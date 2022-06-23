/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.employment

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.DeductionsBuilder.aDeductions
import support.builders.models.employment.EmploymentDataBuilder.anEmploymentData
import support.builders.models.employment.EmploymentFinancialDataBuilder.aHmrcEmploymentFinancialData
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import support.builders.models.employment.PayBuilder.aPay
import support.builders.models.employment.StudentLoansBuilder.aStudentLoans
import utils.PageUrls.{checkYourBenefitsUrl, checkYourDetailsUrl, checkYourStudentLoansUrl, employerInformationUrl, employmentSummaryUrl, fullUrl, overviewUrl}
import utils.{IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class EmployerInformationControllerISpec extends IntegrationTest with ViewHelpers {

  private val employmentId = "employmentId"

  object Selectors {
    val bannerParagraphSelector: String = ".govuk-notification-banner__heading"
    val bannerLinkSelector: String = ".govuk-notification-banner__link"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val buttonSelector = "#returnToEmploymentSummaryBtn"
    val employmentDetailsLinkSelector: Boolean => String = (welshLang: Boolean) => if (welshLang) "#manylion-cyflogaeth_link" else "#employment-details_link"
    val employmentBenefitsLinkSelector = "#employment-benefits_link"
    val formSelector = "#main-content > div > div > form"

    def studentLoansLinkSelector(welshLang: Boolean): String = if (welshLang) "#benthyciadau-myfyrwyr_link" else "#student-loans_link"

    def summaryListKeySelector(i: Int): String = {
      s"#main-content > div > div > dl:nth-child(3) > div:nth-child($i) > dt"
    }

    def summaryListStatusTagsSelector(i: Int): String = {
      s"#main-content > div > div > dl:nth-child(3) > div:nth-child($i) > dd"
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
    val fieldNames = Seq("Employment details", "Employment benefits", "Student loans")
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
    val fieldNames = Seq("Manylion cyflogaeth", "Employment benefits", "Benthyciadau Myfyrwyr")
    val buttonText = "Return to PAYE employment"
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

    def expectedContent(taxYear: Int): String = s"Ni allwch ddiweddaruích manylion cyflogaeth tan 6 Ebrill $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Gwybodaeth y cyflogwr"

    def expectedContent(taxYear: Int): String = s"Ni allwch ddiweddaru manylion cyflogaeth eich cleient tan 6 Ebrill $taxYear."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
    import Selectors._
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the page where total tax to date is None and end of year" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            val employment = aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentData = Some(anEmploymentData.copy(
              deductions = Some(aDeductions.copy(studentLoans = Some(aStudentLoans.copy(uglDeductionAmount = Some(2000), pglDeductionAmount = None)))),
              pay = Some(aPay.copy(totalTaxToDate = None))
            )))))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(employment)))), nino, taxYearEOY)
            urlGet(fullUrl(employerInformationUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has a Notification banner" which {
            textOnPageCheck(user.commonExpectedResults.bannerParagraph, bannerParagraphSelector)
            linkCheck(user.commonExpectedResults.bannerLinkText, bannerLinkSelector, checkYourDetailsUrl(taxYearEOY, employmentId))
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), checkYourDetailsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagsSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListKeySelector(2))
            textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(2))
          }

          "has a student loans section" which {
            linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), checkYourStudentLoansUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(3))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYearEOY), "#main-content > div > div > form")

          welshToggleCheck(user.isWelsh)
        }

        "render the page where the status for benefits is Cannot Update when there is no Benefits data in year and Undergraduate loans" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            val employment = aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None, employmentData = Some(anEmploymentData.copy(
              deductions = Some(aDeductions.copy(studentLoans = Some(aStudentLoans.copy(uglDeductionAmount = Some(2000), pglDeductionAmount = None))))
            )))))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(employment)))), nino, taxYear)
            urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), checkYourDetailsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListKeySelector(2))
            textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(2))
          }

          "has a student loans section" which {
            linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), checkYourStudentLoansUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(3))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYear), "#main-content > div > div > form")

          welshToggleCheck(user.isWelsh)
        }

        "render the page where the status for student loans is Not Started when there is no Student Loans data in year" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            val employment = aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(
              aHmrcEmploymentFinancialData.copy(employmentData = Some(anEmploymentData.copy(deductions = Some(aDeductions.copy(studentLoans = None)))))))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(employment)))), nino, taxYear)
            urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), checkYourDetailsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, checkYourBenefitsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(2))
          }

          "has a student loans section" which {
            textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(3))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYear), "#main-content > div > div > form")

          welshToggleCheck(user.isWelsh)
        }

        "render the page with not ignored employments and Postgraduate loans" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            val employmentOne = aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None)))
            val employmentTwo = aHmrcEmploymentSource.copy(employmentId = "004", employerName = "someName", hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(
              employmentBenefits = None, employmentData = Some(anEmploymentData.copy(
                deductions = Some(aDeductions.copy(studentLoans = Some(aStudentLoans.copy(uglDeductionAmount = None, pglDeductionAmount = Some(2000)))))
              )))))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(employmentOne, employmentTwo)))), nino, taxYear)
            urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), checkYourDetailsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListKeySelector(2))
            textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(2))
          }

          "has a student loans section" which {
            linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), checkYourStudentLoansUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(3))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYear), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page where the status for benefits is Updated when there is Benefits data in year" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), checkYourDetailsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, checkYourBenefitsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(2))
          }

          "has a student loans section" which {
            linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), checkYourStudentLoansUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(3))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYear), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page where the status for benefits is Not Started when there is no Benefits data for end of year" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(
              anAllEmploymentData.copy(hmrcEmploymentData = Seq(
                aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None))))))), nino, taxYear - 1)
            urlGet(fullUrl(employerInformationUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), checkYourDetailsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, checkYourBenefitsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.notStarted, summaryListStatusTagsSelectorEOY(2))
          }

          "has a student loans section" which {
            linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), checkYourStudentLoansUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(3))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYearEOY), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page where the status for benefits is Updated when there is Benefits data for end of year" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            urlGet(fullUrl(employerInformationUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), checkYourDetailsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, checkYourBenefitsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(2))
          }

          "has a student loans section" which {
            linkCheck(user.commonExpectedResults.fieldNames(2), studentLoansLinkSelector(user.isWelsh), checkYourStudentLoansUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(3))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYearEOY), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page without the Student Loans Row when the feature switch is turned off in year" which {
          val headers = if (user.isWelsh) {
            Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear), HeaderNames.ACCEPT_LANGUAGE -> "cy")
          } else {
            Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear))
          }

          val request = FakeRequest("GET", employerInformationUrl(taxYear, employmentId)).withHeaders(headers: _*)

          lazy val result: Future[Result] = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            route(appWithFeatureSwitchesOff, request, "{}").get
          }

          implicit def document: () => Document = () => Jsoup.parse(bodyOf(result))

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), checkYourDetailsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, checkYourBenefitsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(2))
          }

          "does not have a student loans section" which {
            elementsNotOnPageCheck(studentLoansLinkSelector(user.isWelsh))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYear), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page without the Student Loans Row when the feature switch is turned off end of year" which {
          val headers = if (user.isWelsh) {
            Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), HeaderNames.ACCEPT_LANGUAGE -> "cy")
          } else {
            Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
          }

          val request = FakeRequest("GET", employerInformationUrl(taxYearEOY, employmentId)).withHeaders(headers: _*)

          lazy val result: Future[Result] = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            route(appWithFeatureSwitchesOff, request, "{}").get
          }

          implicit def document: () => Document = () => Jsoup.parse(bodyOf(result))

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector(user.isWelsh), checkYourDetailsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, checkYourBenefitsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(2))
          }

          "does not have a student loans section" which {
            elementsNotOnPageCheck(studentLoansLinkSelector(user.isWelsh))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYearEOY), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "redirect to the overview page when there is no data in year" in {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq()))), nino, taxYear)
            urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }

        "render Unauthorised user error page" which {
          lazy val result: WSResponse = {
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), welsh = user.isWelsh)
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }
}
