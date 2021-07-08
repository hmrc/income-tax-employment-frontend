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
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    def summaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dt"
    def summaryListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__value"
    def cyaChangeLink(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__actions > a"

  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedContent: String
    val expectedInsetText: String
    val employeeFieldName7: String
    val employeeFieldName8: String

    val changePAYERefHiddenText:String
    val changePayReceivedHiddenText:String
    val taxTakenFromPayHiddenText:String
    val paymentsNotOnP60HiddenText:String
    val amountOfPaymentsNotOnP60HiddenText:String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val changeLinkExpected:String
    val continueButtonText: String
    val continueButtonLink: String
    val employeeFieldName1: String
    val employeeFieldName2: String
    val employeeFieldName3: String
    val employeeFieldName4: String
    val employeeFieldName5: String
    val employeeFieldName6: String

    val changeEmployerNameHiddenText:String
  }

  object ContentValues {
    val employeeFieldValue1 = "maggie"
    val employeeFieldValue2 = "223/AB12399"
    val employeeFieldValue3 = "12 February 2020"
    val employeeFieldValue4 = "No"
    val employeeFieldValue4a = "Yes"
    val employeeFieldValue5 = "£34234.15"
    val employeeFieldValue5a = "£34234.50"
    val employeeFieldValue6 = "£6782.92"
    val employeeFieldValue6a = "£6782.90"
    val employeeFieldValue7 = "No"
    val employeeFieldValue7a = "Yes"
    val employeeFieldValue8 = "£67676"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = (taxYear:Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val continueButtonLink = "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=001"
    val employeeFieldName1 = "Employer"
    val employeeFieldName2 = "PAYE reference"
    val employeeFieldName3 = "Director role end date"
    val employeeFieldName4 = "Close company"
    val employeeFieldName5 = "Pay received"
    val employeeFieldName6 = "UK tax taken from pay"

    val changeEmployerNameHiddenText: String =  "the name of this employer"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = (taxYear:Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val continueButtonLink = "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=001"
    val employeeFieldName1 = "Employer"
    val employeeFieldName2 = "PAYE reference"
    val employeeFieldName3 = "Director role end date"
    val employeeFieldName4 = "Close company"
    val employeeFieldName5 = "Pay received"
    val employeeFieldName6 = "UK tax taken from pay"

    val changeEmployerNameHiddenText: String =  "the name of this employer"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your P60"
    val employeeFieldName8 = "Amount of payments not on your P60"
    val changePAYERefHiddenText: String = "your PAYE reference number"
    val changePayReceivedHiddenText: String = "the amount of pay you got from this employer"
    val taxTakenFromPayHiddenText: String = "the amount of tax you paid"
    val paymentsNotOnP60HiddenText: String = "if you got payments that are not on your P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "the amount of payments that were not on your P60"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your client’s P60"
    val employeeFieldName8 = "Amount of payments not on your client’s P60"
    val changePAYERefHiddenText: String = "your client’s PAYE reference number"
    val changePayReceivedHiddenText: String = "the amount of pay your client got from this employer"
    val taxTakenFromPayHiddenText: String = "the amount of tax your client paid"
    val paymentsNotOnP60HiddenText: String = "if your client got payments that are not on their P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "the amount of payments that were not on your client’s P60"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your P60"
    val employeeFieldName8 = "Amount of payments not on your P60"
    val changePAYERefHiddenText: String = "your PAYE reference number"
    val changePayReceivedHiddenText: String = "the amount of pay you got from this employer"
    val taxTakenFromPayHiddenText: String = "the amount of tax you paid"
    val paymentsNotOnP60HiddenText: String = "if you got payments that are not on your P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "the amount of payments that were not on your P60"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your client’s P60"
    val employeeFieldName8 = "Amount of payments not on your client’s P60"
    val changePAYERefHiddenText: String = "your client’s PAYE reference number"
    val changePayReceivedHiddenText: String = "the amount of pay your client got from this employer"
    val taxTakenFromPayHiddenText: String = "the amount of tax your client paid"
    val paymentsNotOnP60HiddenText: String = "if your client got payments that are not on their P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "the amount of payments that were not on your client’s P60"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
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
            pay = Some(Pay(Some(34234.15), Some(6782.92), None, None, None, None, None))
          )),
          None
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  object CustomerMinModel {
    val miniData: AllEmploymentData = AllEmploymentData(MinModel.miniData.hmrcEmploymentData, None,
      customerEmploymentData = Seq(
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
            pay = Some(Pay(Some(34234.50), Some(6782.90), None, None, None, None, None))
          )),
          None
        )
      ),
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
            pay = Some(Pay(Some(34234.15), Some(6782.92), None, None, None, None, None))
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

        //noinspection ScalaStyle
        "for in year return a fully populated page when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption(taxYear), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.employeeFieldValue2, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employeeFieldValue3, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employeeFieldValue4, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName5, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName6, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(user.specificExpectedResults.get.employeeFieldName8, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.employeeFieldValue8, summaryListRowFieldAmountSelector(7))

        }
        //noinspection ScalaStyle
        "for end of year return a fully populated page, with change links, when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(None)), nino, taxYear-1)
            urlGet(s"$appUrl/${taxYear-1}/check-employment-details?employmentId=001", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          val dummyHref =  "/income-through-software/return/employment-income/2021/check-employment-expenses"
          val employerNameHref =  "/income-through-software/return/employment-income/2021/employer-name"

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption(2021), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.commonExpectedResults.changeEmployerNameHiddenText}",
            cyaChangeLink(1), employerNameHref)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.employeeFieldValue2, summaryListRowFieldAmountSelector(2))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changePAYERefHiddenText}",
            cyaChangeLink(2), dummyHref)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName5, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changePayReceivedHiddenText}",
            cyaChangeLink(3), dummyHref)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName6, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(4))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.taxTakenFromPayHiddenText}",
            cyaChangeLink(4), dummyHref)
          textOnPageCheck(user.specificExpectedResults.get.employeeFieldName7, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.employeeFieldValue7a, summaryListRowFieldAmountSelector(5))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.paymentsNotOnP60HiddenText}",
            cyaChangeLink(5), dummyHref)
          textOnPageCheck(user.specificExpectedResults.get.employeeFieldName8, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.employeeFieldValue8, summaryListRowFieldAmountSelector(6))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.amountOfPaymentsNotOnP60HiddenText}",
            cyaChangeLink(6), dummyHref)
        }


        "for in year return a filtered list on page when minimum data is returned" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(MinModel.miniData), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption(taxYear), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))
        }
        //noinspection ScalaStyle
        "for end of year return customer employment data if there is both HMRC and customer Employment Data " +
          "and correctly render a filtered list on page when minimum data is returned" when {

          implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          userDataStub(userData(CustomerMinModel.miniData), nino, taxYear-1)
          urlGet(s"$appUrl/${taxYear-1}/check-employment-details?employmentId=001", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          val dummyHref =  "/income-through-software/return/employment-income/2021/check-employment-expenses"
          val employerNameHref =  "/income-through-software/return/employment-income/2021/employer-name"

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption(2021), captionSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.commonExpectedResults.changeEmployerNameHiddenText}",
            cyaChangeLink(1), employerNameHref)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.employeeFieldValue5a, summaryListRowFieldAmountSelector(2))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changePayReceivedHiddenText}",
            cyaChangeLink(2), dummyHref)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employeeFieldValue6a, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.taxTakenFromPayHiddenText}",
            cyaChangeLink(3), dummyHref)
          textOnPageCheck(user.specificExpectedResults.get.employeeFieldName7, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employeeFieldValue7, summaryListRowFieldAmountSelector(4))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.paymentsNotOnP60HiddenText}",
            cyaChangeLink(4), dummyHref)

          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(user.commonExpectedResults. continueButtonLink, continueButtonFormSelector)
        }

        //noinspection ScalaStyle
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
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption(taxYear), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName4, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.employeeFieldValue4a, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName5, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.employeeFieldName6, summaryListRowFieldNameSelector(4))
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
      }
    }
  }
}