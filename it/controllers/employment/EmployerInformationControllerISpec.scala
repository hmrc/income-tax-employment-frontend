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

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.PageUrls.{checkYourBenefitsUrl, checkYourDetailsUrl, employerInformationUrl, employmentSummaryUrl, fullUrl, overviewUrl}
import utils.{IntegrationTest, ViewHelpers}

class EmployerInformationControllerISpec extends IntegrationTest with ViewHelpers {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId = "employmentId"

  object Selectors {
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val buttonSelector = "#returnToEmploymentSummaryBtn"
    val employmentDetailsLinkSelector = "#employment-details_link"
    val employmentBenefitsLinkSelector = "#employment-benefits_link"
    val formSelector = "#main-content > div > div > form"

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

    val fieldNames: Seq[String]
    val buttonText: String
    val updated: String
    val cannotUpdate: String
    val notStarted: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"PAYE employment for 6 April ${taxYear - 1} to 5 April $taxYear"

    val fieldNames = Seq("Employment details", "Employment benefits")
    val buttonText = "Return to PAYE employment"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
    val notStarted = "Not started"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"PAYE employment for 6 April ${taxYear - 1} to 5 April $taxYear"

    val fieldNames = Seq("Employment details", "Employment benefits")
    val buttonText = "Return to PAYE employment"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
    val notStarted = "Not started"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employer information"

    def expectedContent(taxYear: Int): String = s"You cannot change your employment information until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employer information"

    def expectedContent(taxYear: Int): String = s"You cannot change your client’s employment information until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employer information"

    def expectedContent(taxYear: Int): String = s"You cannot change your employment information until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "maggie"
    val expectedTitle: String = "Employer information"

    def expectedContent(taxYear: Int): String = s"You cannot change your client’s employment information until 6 April $taxYear."
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
        "render the page where the status for benefits is Cannot Update when there is no Benefits data in year" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            val employment = anEmploymentSource.copy(employmentBenefits = None)
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(employment)))), nino, taxYear)
            urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, checkYourDetailsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListKeySelector(2))
            textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(2))
          }
          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYear), "#main-content > div > div > form")

          welshToggleCheck(user.isWelsh)
        }

        "render the page with not ignored employments " which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            val employmentOne = anEmploymentSource.copy(employmentBenefits = None)
            val employmentTwo = anEmploymentSource.copy(employmentId = "004", employerName = "someName", employmentBenefits = None)
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(employmentOne, employmentTwo)))), nino, taxYear)
            urlGet(fullUrl(employerInformationUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, checkYourDetailsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
          }

          "has a benefits section" which {
            textOnPageCheck(user.commonExpectedResults.fieldNames(1), summaryListKeySelector(2))
            textOnPageCheck(user.commonExpectedResults.cannotUpdate, summaryListStatusTagsSelector(2))
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

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent(taxYear), insetTextSelector)

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, checkYourDetailsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, checkYourBenefitsUrl(taxYear, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelector(2))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)
          formGetLinkCheck(employmentSummaryUrl(taxYear), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page where the status for benefits is Not Started when there is no Benefits data for end of year" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None))))), nino, taxYear - 1)
            urlGet(fullUrl(employerInformationUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, checkYourDetailsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, checkYourBenefitsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.notStarted, summaryListStatusTagsSelectorEOY(2))
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

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear - 1))

          "has an employment details section" which {
            linkCheck(user.commonExpectedResults.fieldNames.head, employmentDetailsLinkSelector, checkYourDetailsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(1))
          }

          "has a benefits section" which {
            linkCheck(user.commonExpectedResults.fieldNames(1), employmentBenefitsLinkSelector, checkYourBenefitsUrl(taxYearEOY, employmentId))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagsSelectorEOY(2))
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
