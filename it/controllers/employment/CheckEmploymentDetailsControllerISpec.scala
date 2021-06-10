/*
 * Copyright 2021 HM Revenue & Customs
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

import models.employment.{AllEmploymentData, EmploymentData, EmploymentSource, Pay}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class CheckEmploymentDetailsControllerISpec extends IntegrationTest with ViewHelpers {

  val url = s"$appUrl/$taxYear/check-employment-details?employmentId=001"

  object Selectors {
    val headingSelector = "#main-content > div > div > header > h1"
    val captionSelector = "#main-content > div > div > header > p"
    val contentTextSelector = "#main-content > div > div > p"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val summaryListSelector = "#main-content > div > div > dl"
    def summaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dt"
    def summaryListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dd"
  }

  trait ExpectedResultsForLanguage {
    val expectedH1: String
    val expectedTitle: String
    val expectedContent: String
    val expectedInsetText: String
    val employeeFieldName7: String
  }

  trait CommonExpectedResult {
    val expectedCaption: String
    val employeeFieldName1: String
    val employeeFieldName2: String
    val employeeFieldName3: String
    val employeeFieldName4: String
    val employeeFieldName5: String
    val employeeFieldName6: String
  }

  object ContentValues {
    val employeeFieldValue1 = "maggie"
    val employeeFieldValue2 = "223/AB12399"
    val employeeFieldValue3 = "12 February 2020"
    val employeeFieldValue4 = "No"
    val employeeFieldValue4a = "Yes"
    val employeeFieldValue5 = "£34234.15"
    val employeeFieldValue6 = "£6782.92"
    val employeeFieldValue7 = "£67676"
  }

  object CommonExpectedEN extends CommonExpectedResult {
    val expectedCaption = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val employeeFieldName1 = "Employer"
    val employeeFieldName2 = "PAYE reference"
    val employeeFieldName3 = "Director role end date"
    val employeeFieldName4 = "Close company"
    val employeeFieldName5 = "Pay received"
    val employeeFieldName6 = "UK tax taken from pay"
  }

  object CommonExpectedCY extends CommonExpectedResult {
    val expectedCaption = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val employeeFieldName1 = "Employer"
    val employeeFieldName2 = "PAYE reference"
    val employeeFieldName3 = "Director role end date"
    val employeeFieldName4 = "Close company"
    val employeeFieldName5 = "Pay received"
    val employeeFieldName6 = "UK tax taken from pay"
  }

  object ExpectedIndividualEN extends ExpectedResultsForLanguage {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your P60"
  }

  object ExpectedAgentEN extends ExpectedResultsForLanguage {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on P60"
  }

  object ExpectedIndividualCY extends ExpectedResultsForLanguage {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your P60"
  }

  object ExpectedAgentCY extends ExpectedResultsForLanguage {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on P60"
  }

  val userScenarios: Seq[UserScenario[ExpectedResultsForLanguage, CommonExpectedResult]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, ExpectedIndividualEN, CommonExpectedEN),
      UserScenario(isWelsh = false, isAgent = true, ExpectedAgentEN, CommonExpectedEN),
      UserScenario(isWelsh = true, isAgent = false, ExpectedIndividualCY, CommonExpectedCY),
      UserScenario(isWelsh = true, isAgent = true, ExpectedAgentCY, CommonExpectedCY))
  }

  object MinModel {
    val miniData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(
        EmploymentSource(
          employmentId = "001",
          employerName = "maggie",
          employerRef = None,
          payrollId = None,
          startDate = None,
          cessationDate = None,
          dateIgnored = None,
          submittedOn = None,
          employmentData = Some(EmploymentData(
            submittedOn = "2020-02-12",
            employmentSequenceNumber = None,
            companyDirector = None,
            closeCompany = None,
            directorshipCeasedDate = None,
            occPen = None,
            disguisedRemuneration = None,
            pay = Pay(34234.15, 6782.92, None, None, None, None, None)
          )),
          None
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  object SomeModelWithInvalidDateFormat {
    val invalidData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(
        EmploymentSource(
          employmentId = "001",
          employerName = "maggie",
          employerRef = None,
          payrollId = None,
          startDate = None,
          cessationDate = None,
          dateIgnored = None,
          submittedOn = None,
          employmentData = Some(EmploymentData(
            submittedOn = "2020-02-12",
            employmentSequenceNumber = None,
            companyDirector = Some(true),
            closeCompany = Some(true),
            directorshipCeasedDate = Some("14/07/1990"),
            occPen = None,
            disguisedRemuneration = None,
            pay = Pay(34234.15, 6782.92, None, None, None, None, None)
          )),
          None
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return a fully populated page when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.expectedResultsForLanguage.expectedTitle)
          h1Check(user.expectedResultsForLanguage.expectedH1)
          textOnPageCheck(user.commonExpectedResult.expectedCaption, captionSelector)
          textOnPageCheck(user.expectedResultsForLanguage.expectedContent, contentTextSelector)
          textOnPageCheck(user.expectedResultsForLanguage.expectedInsetText, insetTextSelector)
          welshToggleCheck(if(user.isWelsh) WELSH else ENGLISH)
          textOnPageCheck(user.commonExpectedResult.employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.employeeFieldValue2, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employeeFieldValue3, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employeeFieldValue4, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName5, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName6, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(user.expectedResultsForLanguage.employeeFieldName7, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.employeeFieldValue7, summaryListRowFieldAmountSelector(7))
        }

        "redirect to overview page when theres no details" in {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(
              fullEmploymentsModel(None).copy(hmrcEmploymentData = Seq.empty)
            ), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }

        "return a filtered list on page when minimum data is returned" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(MinModel.miniData), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.expectedResultsForLanguage.expectedTitle)
          h1Check(user.expectedResultsForLanguage.expectedH1)
          textOnPageCheck(user.commonExpectedResult.expectedCaption, captionSelector)
          textOnPageCheck(user.expectedResultsForLanguage.expectedContent, contentTextSelector)
          textOnPageCheck(user.expectedResultsForLanguage.expectedInsetText, insetTextSelector)
          welshToggleCheck(if(user.isWelsh) WELSH else ENGLISH)
          textOnPageCheck(user.commonExpectedResult.employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))
        }

        "handle a model with an Invalid date format returned" when {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(SomeModelWithInvalidDateFormat.invalidData), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.expectedResultsForLanguage.expectedTitle)
          h1Check(user.expectedResultsForLanguage.expectedH1)
          textOnPageCheck(user.commonExpectedResult.expectedCaption, captionSelector)
          textOnPageCheck(user.expectedResultsForLanguage.expectedContent, contentTextSelector)
          textOnPageCheck(user.expectedResultsForLanguage.expectedInsetText, insetTextSelector)
          welshToggleCheck(if(user.isWelsh) WELSH else ENGLISH)
          textOnPageCheck(user.commonExpectedResult.employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName4, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.employeeFieldValue4a, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName5, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResult.employeeFieldName6, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(4))
        }

        "returns an action when auth call fails" which {
          lazy val result: WSResponse = {
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(url,welsh = user.isWelsh)
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }
}