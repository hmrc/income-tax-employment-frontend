/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views.employment

import config.AppConfig
import controllers.employment.routes.{CheckEmploymentDetailsController, CheckYourBenefitsController, EmploymentSummaryController}
import controllers.studentLoans.routes.StudentLoansCYAController
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.mocks.MockAppConfig
import viewmodels.employment._
import views.html.employment.EmployerInformationView

class EmployerInformationViewSpec extends ViewUnitTest {

  private val employerName = "maggie"
  private val employmentId = "employmentId"

  object Selectors {
    val bannerParagraphSelector: String = ".govuk-notification-banner__heading"
    val bannerLinkSelector: String = ".govuk-notification-banner__link"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val buttonSelector = "#returnToEmploymentSummaryBtn"
    //TODO - change id for links in the view (EmployerInformationView) so that they don't use message keys
    val employmentDetailsLinkSelector: Boolean => String = (welshLang: Boolean) => if (welshLang) "#manylion-cyflogaeth_link" else "#employment-details_link"
    val employmentBenefitsLinkSelector: Boolean => String = (welshLang: Boolean) => if (welshLang) "#buddiannau-cyflogaeth_link" else "#employment-benefits_link"
    val formSelector = "#main-content > div > div > form"

    def studentLoansLinkSelector(welshLang: Boolean): String = if (welshLang) "#benthyciadau-myfyrwyr_link" else "#student-loans_link"

    def summaryListKeySelector(i: Int): String = {
      s"#main-content > div > div > dl:nth-of-type(1) > div:nth-child($i) > dt"
    }

    def summaryListStatusTagsSelector(i: Int): String = {
      s"#main-content > div > div > dl:nth-of-type(1) > div:nth-child($i) > dd"
    }

    def summaryListStatusTagsSelectorEOY(i: Int): String = {
      s"#main-content > div > div > dl > div:nth-child($i) > dd"
    }
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String

    def expectedContent(taxYear: Int): String
  }

  trait CommonExpectedResults {

    def expectedCaption(taxYear: Int): String

    val bannerParagraph: String
    val bannerLinkText: String
    val fieldNames: Seq[String]
    val buttonText: String
    val updated: String
    val toDo: String
    val cannotUpdate: String
    val notStarted: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"PAYE employment for 6 April ${taxYear - 1} to 5 April $taxYear"

    val bannerParagraph: String = "You must add missing employment details."
    val bannerLinkText: String = "add missing employment details."
    val fieldNames: Seq[String] = Seq("Employment details", "Employment benefits", "Student loans")
    val buttonText = "Return to PAYE employment"
    val updated = "Updated"
    val toDo: String = "To do"
    val cannotUpdate = "Cannot update"
    val notStarted = "Not started"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Cyflogaeth TWE ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"

    val bannerParagraph: String = "Mae’n rhaid ychwanegu manylion cyflogaeth sydd ar goll."
    val bannerLinkText: String = "ychwanegu manylion cyflogaeth sydd ar goll."
    val fieldNames: Seq[String] = Seq("Manylion cyflogaeth", "Buddiannau cyflogaeth", "Benthyciadau myfyrwyr")
    val buttonText = "Yn ôl i ‘Cyflogaeth TWE’"
    val updated = "Wedi diweddaru"
    val toDo: String = "I’w gwneud"
    val cannotUpdate = "Ddim yn gallu diweddaru"
    val notStarted = "Heb ddechrau"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employer information"

    def expectedContent(taxYear: Int): String = s"You cannot update your employment information until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employer information"

    def expectedContent(taxYear: Int): String = s"You cannot update your client’s employment information until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Gwybodaeth y cyflogwr"

    def expectedContent(taxYear: Int): String = s"Ni allwch ddiweddaru’ch manylion cyflogaeth tan 6 Ebrill $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Gwybodaeth y cyflogwr"

    def expectedContent(taxYear: Int): String = s"Ni allwch ddiweddaru manylion cyflogaeth eich cleient tan 6 Ebrill $taxYear."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val underTest = inject[EmployerInformationView]

  val updateNotAvailable = false

  userScenarios.foreach { user =>
    import Selectors._
    s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
      "render the page where the status for benefits and student loans is 'Cannot update' when there is no benefits and student loans data in year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val rows = Seq(
          EmployerInformationRow(EmploymentDetails, Updated, Some(CheckEmploymentDetailsController.show(taxYear, employmentId)), updateNotAvailable),
          EmployerInformationRow(EmploymentBenefits, CannotUpdate, None, updateNotAvailable),
          EmployerInformationRow(StudentLoans, CannotUpdate, None, updateNotAvailable)
        )

        val htmlFormat = underTest(employerName, employmentId, rows, taxYear = taxYear, isInYear = true, showNotification = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(user.isWelsh)

        titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
        textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

        "has an employment details section" which {
          linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), CheckEmploymentDetailsController.show(taxYear, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
        }

        "has a benefits section" which {
          textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListKeySelector(2))
          textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(2))
        }

        "has a student loans section" which {
          textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(3))
        }

        buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
        formGetLinkCheck(EmploymentSummaryController.show(taxYear).url, formSelector)
      }

      "render the page where the status for is 'Updated' for benefits and student loans in year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val rows = Seq(
          EmployerInformationRow(EmploymentDetails, Updated, Some(CheckEmploymentDetailsController.show(taxYear, employmentId)), updateNotAvailable),
          EmployerInformationRow(EmploymentBenefits, Updated, Some(CheckYourBenefitsController.show(taxYear, employmentId)), updateNotAvailable),
          EmployerInformationRow(StudentLoans, Updated, Some(StudentLoansCYAController.show(taxYear, employmentId)), updateNotAvailable)
        )

        val htmlFormat = underTest(employerName, employmentId, rows, taxYear = taxYear, isInYear = true, showNotification = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(user.isWelsh)

        titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
        textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

        "has an employment details section" which {
          linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), CheckEmploymentDetailsController.show(taxYear, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
        }

        "has a benefits section" which {
          linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector(user.isWelsh), CheckYourBenefitsController.show(taxYear, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(2))
        }

        "has a student loans section" which {
          linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), StudentLoansCYAController.show(taxYear, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(3))
        }

        buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
        formGetLinkCheck(EmploymentSummaryController.show(taxYear).url, formSelector)
      }

      "render the page where the status for benefits and student loans is 'Not started' when there is no benefits and student loans data for end of year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val rows = Seq(
          EmployerInformationRow(EmploymentDetails, Updated, Some(CheckEmploymentDetailsController.show(taxYearEOY, employmentId)), updateNotAvailable),
          EmployerInformationRow(EmploymentBenefits, NotStarted, Some(CheckYourBenefitsController.show(taxYearEOY, employmentId)), updateNotAvailable),
          EmployerInformationRow(StudentLoans, NotStarted, Some(StudentLoansCYAController.show(taxYearEOY, employmentId)), updateNotAvailable)
        )

        val htmlFormat = underTest(employerName, employmentId, rows, taxYear = taxYearEOY, isInYear = false, showNotification = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))

        "has an employment details section" which {
          linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(1))
        }

        "has a benefits section" which {
          linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector(user.isWelsh), CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.notStarted, summaryListStatusTagsSelectorEOY(2))
        }

        "has a student loans section" which {
          linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), StudentLoansCYAController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.notStarted, summaryListStatusTagsSelectorEOY(3))
        }

        buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
        formGetLinkCheck(EmploymentSummaryController.show(taxYearEOY).url, formSelector)

        welshToggleCheck(user.isWelsh)
      }

      "render the page where the status for benefits and student loans is 'Updated' when there is benefits and student loans data for end of year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val rows = Seq(
          EmployerInformationRow(EmploymentDetails, Updated, Some(CheckEmploymentDetailsController.show(taxYearEOY, employmentId)), updateNotAvailable),
          EmployerInformationRow(EmploymentBenefits, Updated, Some(CheckYourBenefitsController.show(taxYearEOY, employmentId)), updateNotAvailable),
          EmployerInformationRow(StudentLoans, Updated, Some(StudentLoansCYAController.show(taxYearEOY, employmentId)), updateNotAvailable)
        )

        val htmlFormat = underTest(employerName, employmentId, rows, taxYear = taxYearEOY, isInYear = false, showNotification = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))

        "has an employment details section" which {
          linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(1))
        }

        "has a benefits section" which {
          linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector(user.isWelsh), CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(2))
        }

        "has a student loans section" which {
          linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), StudentLoansCYAController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(3))
        }

        buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
        formGetLinkCheck(EmploymentSummaryController.show(taxYearEOY).url, formSelector)

        welshToggleCheck(user.isWelsh)
      }

      "render the page without the Student Loans Row when the feature switch is turned off in year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val appConfig: AppConfig = new MockAppConfig().config(slEnabled = false)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val rows = Seq(
          EmployerInformationRow(EmploymentDetails, Updated, Some(CheckEmploymentDetailsController.show(taxYear, employmentId)), updateNotAvailable),
          EmployerInformationRow(EmploymentBenefits, Updated, Some(CheckYourBenefitsController.show(taxYear, employmentId)), updateNotAvailable),
        )

        val htmlFormat = underTest(employerName, employmentId, rows, taxYear = taxYear, isInYear = true, showNotification = false)(authRequest, messages, appConfig)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
        textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

        "has an employment details section" which {
          linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), CheckEmploymentDetailsController.show(taxYear, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
        }

        "has a benefits section" which {
          linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector(user.isWelsh), CheckYourBenefitsController.show(taxYear, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(2))
        }

        "does not have a student loans section" which {
          elementsNotOnPageCheck(studentLoansLinkSelector(user.isWelsh))
        }

        buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
        formGetLinkCheck(EmploymentSummaryController.show(taxYear).url, formSelector)

        welshToggleCheck(user.isWelsh)
      }

      "render the page without the Student Loans Row when the feature switch is turned off end of year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val appConfig: AppConfig = new MockAppConfig().config(slEnabled = false)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val rows = Seq(
          EmployerInformationRow(EmploymentDetails, Updated, Some(CheckEmploymentDetailsController.show(taxYearEOY, employmentId)), updateNotAvailable),
          EmployerInformationRow(EmploymentBenefits, Updated, Some(CheckYourBenefitsController.show(taxYearEOY, employmentId)), updateNotAvailable)
        )

        val htmlFormat = underTest(employerName, employmentId, rows, taxYear = taxYearEOY, isInYear = false, showNotification = false)(authRequest, messages, appConfig)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))

        "has an employment details section" which {
          linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(1))
        }

        "has a benefits section" which {
          linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector(user.isWelsh), CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(2))
        }

        "does not have a student loans section" which {
          elementsNotOnPageCheck(studentLoansLinkSelector(user.isWelsh))
        }

        buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
        formGetLinkCheck(EmploymentSummaryController.show(taxYearEOY).url, formSelector)

        welshToggleCheck(user.isWelsh)
      }

      "render the end of year view with the notification banner when employment details has missing data, where the status tags for benefits and student loans is 'Cannot Update'" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(user.isAgent)
        implicit val messages: Messages = getMessages(user.isWelsh)

        val rows = Seq(
          EmployerInformationRow(EmploymentDetails, ToDo, Some(CheckEmploymentDetailsController.show(taxYearEOY, employmentId)), updateNotAvailable),
          EmployerInformationRow(EmploymentBenefits, CannotUpdate, None, updateNotAvailable),
          EmployerInformationRow(StudentLoans, CannotUpdate, Some(StudentLoansCYAController.show(taxYearEOY, employmentId)), updateNotAvailable)
        )

        val htmlFormat = underTest(employerName, employmentId, rows, taxYear = taxYearEOY, isInYear = false, showNotification = true)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        "has a Notification banner" which {
          textOnPageCheck(user.commonExpectedResults.bannerParagraph, bannerParagraphSelector)
          linkCheck(user.commonExpectedResults.bannerLinkText, bannerLinkSelector, CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url)
        }

        titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
        h1Check(user.specificExpectedResults.get.expectedH1)
        captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))

        "has an employment details section" which {
          linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagsSelector(1))
        }

        "has a benefits section" which {
          textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListKeySelector(2))
          textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(2))
        }

        "has a student loans section" which {
          linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), StudentLoansCYAController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(3))
        }

        buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
        formGetLinkCheck(EmploymentSummaryController.show(taxYearEOY).url, formSelector)
      }
    }
  }
}
