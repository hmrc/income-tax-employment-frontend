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

import controllers.employment.routes.EmploymentSummaryController
import models.User
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.employment._
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Call
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CheckEmploymentDetailsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val url = s"$appUrl/$taxYear/check-employment-details?employmentId=001"
  val employmentId = "001"

  object Selectors {
    val headingSelector = "#main-content > div > div > header > h1"
    val captionSelector = "#main-content > div > div > header > p"
    val contentTextSelector = "#main-content > div > div > p"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val summaryListSelector = "#main-content > div > div > dl"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"

    def summaryListRowFieldNameSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dt"

    def summaryListRowFieldAmountSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__value"

    def cyaChangeLink(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__actions > a"
    def cyaHiddenChangeLink(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__actions > a > span.govuk-visually-hidden"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedContent: String
    val expectedInsetText: String
    val changeEmploymentStartDateHiddenText: String
    val changeEmploymentEndDateHiddenText: String
    val changeStillWorkingForEmployerHiddenText: String
    val paymentsNotOnYourP60: String
    val changePAYERefHiddenText: String
    val changePayReceivedHiddenText: String
    val taxTakenFromPayHiddenText: String
    val paymentsNotOnP60HiddenText: String
    val amountOfPaymentsNotOnP60HiddenText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val changeLinkExpected: String
    val continueButtonText: String
    val continueButtonLink: String
    val taxButtonLink: String
    val employerNameField1: String
    val employmentStartDateField1: String
    val employmentEndDateField1: String
    val stillWorkingForEmployerField1: String
    val payeReferenceField2: String
    val payReceivedField3: String
    val taxField4: String
    val payrollIdField: String
    val payrollIdHiddenText: String
    val changeEmployerNameHiddenText: String
  }

  object ContentValues {
    val employerName = "maggie"
    val employmentStartDate = "21 April 2019"
    val employmentEndDate = "11 March 2020"
    val payeRef = "223/AB12399"
    val payReceived = "£34234.15"
    val payReceivedB = "£34234.50"
    val taxTakenFromPay = "£6782.92"
    val taxTakenFromPayB = "£6782.90"
    val stillWorkingYes = "Yes"
    val stillWorkingNo = "No"
    val payrollId = "12345678"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val continueButtonLink = "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=001"
    val taxButtonLink = "/income-through-software/return/employment-income/2021/uk-tax?employmentId=001"
    val employerNameField1 = "Employer"
    val employmentStartDateField1 = "Employment start date"
    val employmentEndDateField1 = "Employment end date"
    val stillWorkingForEmployerField1 = "Still working for your employer"
    val payeReferenceField2 = "PAYE reference"
    val payReceivedField3 = "Pay received"
    val taxField4 = "UK tax taken from pay"
    val changeEmployerNameHiddenText: String = "Change the name of this employer"
    val payrollIdField: String = "Payroll ID"
    val payrollIdHiddenText: String = "Change the payroll ID for this employment"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val continueButtonLink = "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=001"
    val taxButtonLink = "/income-through-software/return/employment-income/2021/uk-tax?employmentId=001"
    val employerNameField1 = "Employer"
    val employmentStartDateField1 = "Employment start date"
    val employmentEndDateField1 = "Employment end date"
    val stillWorkingForEmployerField1 = "Still working for your employer"
    val payeReferenceField2 = "PAYE reference"
    val payReceivedField3 = "Pay received"
    val taxField4 = "UK tax taken from pay"
    val changeEmployerNameHiddenText: String = "Change the name of this employer"
    val payrollIdField: String = "Payroll ID"
    val payrollIdHiddenText: String = "Change the payroll ID for this employment"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your P60"
    val employeeFieldName8 = "Amount of payments not on your P60"
    val changeEmploymentStartDateHiddenText = s"Change your start date for ${ContentValues.employerName}"
    val changeEmploymentEndDateHiddenText = s"Change your end date for ${ContentValues.employerName}"
    val changePAYERefHiddenText: String = "Change your PAYE reference number"
    val changePayReceivedHiddenText: String = s"Change the amount of pay you got from ${ContentValues.employerName}"
    val taxTakenFromPayHiddenText: String = "Change the amount of tax you paid"
    val paymentsNotOnP60HiddenText: String = "Change if you got payments that are not on your P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "Change the amount of payments that were not on your P60"
    val changeStillWorkingForEmployerHiddenText = "Change if you are still working for your employer"
    val paymentsNotOnYourP60: String = "Payments not on your P60"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val changeEmploymentStartDateHiddenText = s"Change your client’s start date for ${ContentValues.employerName}"
    val changeEmploymentEndDateHiddenText = s"Change your client’s end date for ${ContentValues.employerName}"
    val changePAYERefHiddenText: String = "Change your client’s PAYE reference number"
    val changePayReceivedHiddenText: String = s"Change the amount of pay your client got from ${ContentValues.employerName}"
    val taxTakenFromPayHiddenText: String = "Change the amount of tax your client paid"
    val paymentsNotOnP60HiddenText: String = "Change if your client got payments that are not on their P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "Change the amount of payments that were not on your client’s P60"
    val changeStillWorkingForEmployerHiddenText = "Change if your client is still working for their employer"
    val paymentsNotOnYourP60: String = "Payments not on your client’s P60"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val changeEmploymentStartDateHiddenText = s"Change your start date for ${ContentValues.employerName}"
    val changeEmploymentEndDateHiddenText = s"Change your end date for ${ContentValues.employerName}"
    val changePAYERefHiddenText: String = "Change your PAYE reference number"
    val changePayReceivedHiddenText: String = s"Change the amount of pay you got from ${ContentValues.employerName}"
    val taxTakenFromPayHiddenText: String = "Change the amount of tax you paid"
    val paymentsNotOnP60HiddenText: String = "Change if you got payments that are not on your P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "Change the amount of payments that were not on your P60"
    val changeStillWorkingForEmployerHiddenText = "Change if you are still working for your employer"
    val paymentsNotOnYourP60: String = "Payments not on your P60"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your client’s P60"
    val employeeFieldName8 = "Amount of payments not on your client’s P60"
    val changeEmploymentStartDateHiddenText = s"Change your client’s start date for ${ContentValues.employerName}"
    val changeEmploymentEndDateHiddenText = s"Change your client’s end date for ${ContentValues.employerName}"
    val changePAYERefHiddenText: String = "Change your client’s PAYE reference number"
    val changePayReceivedHiddenText: String = s"Change the amount of pay your client got from ${ContentValues.employerName}"
    val taxTakenFromPayHiddenText: String = "Change the amount of tax your client paid"
    val paymentsNotOnP60HiddenText: String = "Change if your client got payments that are not on their P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "Change the amount of payments that were not on your client’s P60"
    val changeStillWorkingForEmployerHiddenText = "Change if your client is still working for their employer"
    val paymentsNotOnYourP60: String = "Payments not on your client’s P60"
  }

  object ChangeLinks {
    val employerPayAmountControllerHref: Call = controllers.employment.routes.EmployerPayAmountController.show(taxYear-1, employmentId)
    val employerNameHref: Call = controllers.employment.routes.EmployerNameController.show(taxYear-1, employmentId)
    val payeRefHref: Call = controllers.employment.routes.PayeRefController.show(taxYear-1, employmentId)
    val changeEmploymentStartDateHref: Call = controllers.employment.routes.EmployerStartDateController.show(taxYear-1, employmentId)
    val changeEmploymentEndDateHref: Call = controllers.employment.routes.EmployerLeaveDateController.show(taxYear-1, employmentId)
    val payrollIdHref: Call = controllers.employment.routes.EmployerPayrollIdController.show(taxYear-1, employmentId)
    val changeStillWorkingForEmployerHref: Call = controllers.employment.routes.StillWorkingForEmployerController.show(taxYear - 1, employmentId)
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
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
            pay = Some(Pay(Some(34234.15), Some(6782.92), None, None, None, None)),
            Some(Deductions(
              studentLoans = Some(StudentLoans(
                uglDeductionAmount = Some(100.00),
                pglDeductionAmount = Some(100.00)
              ))
            ))
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
            pay = Some(Pay(Some(34234.50), Some(6782.90), None, None, None, None)),
            Some(Deductions(
              studentLoans = Some(StudentLoans(
                uglDeductionAmount = Some(100.00),
                pglDeductionAmount = Some(100.00)
              ))
            ))
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
            pay = Some(Pay(Some(34234.15), Some(6782.92), None, None, None, None)),
            Some(Deductions(
              studentLoans = Some(StudentLoans(
                uglDeductionAmount = Some(100.00),
                pglDeductionAmount = Some(100.00)
              ))
            ))
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
    import ChangeLinks._
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        //noinspection ScalaStyle
        "redirect to the overview page when prior data exists but not matching the id" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, 2021)
            urlGet(s"$appUrl/2021/check-employment-details?employmentId=employmentId", follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2021/view")
          }
        }
        //noinspection ScalaStyle
        "redirect to the overview page when no data exists" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            noUserDataStub(nino, 2021)
            urlGet(s"$appUrl/2021/check-employment-details?employmentId=employmentId", follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2021/view")
          }
        }
        //noinspection ScalaStyle
        "for end of year return a fully populated page when cya data exists" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(EmploymentUserData(
              sessionId,
              "1234567890",
              "AA123456A",
              2021,
              "001",
              isPriorSubmission = true,
              hasPriorBenefits = true,
              EmploymentCYAModel(
                fullEmploymentsModel().hmrcEmploymentData.head.toEmploymentDetails(false).copy(cessationDateQuestion = Some(false)),
                None
              )
            ), User(mtditid, if (user.isAgent) Some("12345678") else None, nino, sessionId, if (user.isAgent) "Agent" else "Individual")(fakeRequest))
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, 2021)
            urlGet(s"$appUrl/2021/check-employment-details?employmentId=001", follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption(2021), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.employmentStartDateField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employmentStartDate, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.stillWorkingNo, summaryListRowFieldAmountSelector(4))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changeStillWorkingForEmployerHiddenText}",
            cyaChangeLink(4), changeStillWorkingForEmployerHref.url)
          textOnPageCheck(user.commonExpectedResults.employmentEndDateField1, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.employmentEndDate, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(user.commonExpectedResults.payrollIdField, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payrollId, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(user.commonExpectedResults.payReceivedField3, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(7))
          textOnPageCheck(user.commonExpectedResults.taxField4, summaryListRowFieldNameSelector(8))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(8))
        }
        //noinspection ScalaStyle
        "for end of year return a redirect when cya data exists but not finished when its a new employment" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(EmploymentUserData(
              sessionId,
              "1234567890",
              "AA123456A",
              2021,
              "001",
              isPriorSubmission = false,
              hasPriorBenefits =  true,
              EmploymentCYAModel(
                fullEmploymentsModel().hmrcEmploymentData.head.toEmploymentDetails(false).copy(employerRef = None),
                None
              )
            ), User(mtditid, if (user.isAgent) Some("12345678") else None, nino, sessionId, if (user.isAgent) "Agent" else "Individual")(fakeRequest))
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, 2021)
            urlGet(s"$appUrl/2021/check-employment-details?employmentId=001", follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
          }

          "has a redirect url of employer reference page" in {
            result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/employer-paye-reference?employmentId=001")
          }
        }
        //noinspection ScalaStyle
        "for in year return a fully populated page when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
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
          textOnPageCheck(user.commonExpectedResults.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.employmentStartDateField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employmentStartDate, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.stillWorkingNo, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(user.commonExpectedResults.employmentEndDateField1, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.employmentEndDate, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(user.commonExpectedResults.payrollIdField, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payrollId, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(user.commonExpectedResults.payReceivedField3, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(7))
          textOnPageCheck(user.commonExpectedResults.taxField4, summaryListRowFieldNameSelector(8))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(8))
        }
        //noinspection ScalaStyle
        "for end of year return a fully populated page, with change links, when all the fields are populated" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear - 1)
            urlGet(s"$appUrl/${taxYear - 1}/check-employment-details?employmentId=$employmentId", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          val taxHref = "/income-through-software/return/employment-income/2021/uk-tax?employmentId=001"

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption(2021), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.commonExpectedResults.changeEmployerNameHiddenText}",
            cyaChangeLink(1), employerNameHref.url, Some(cyaHiddenChangeLink(1)))
          textOnPageCheck(user.commonExpectedResults.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changePAYERefHiddenText}",
            cyaChangeLink(2), payeRefHref.url, Some(cyaHiddenChangeLink(2)))
          textOnPageCheck(user.commonExpectedResults.employmentStartDateField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employmentStartDate, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changeEmploymentStartDateHiddenText}",
            cyaChangeLink(3), changeEmploymentStartDateHref.url, Some(cyaHiddenChangeLink(3)))
          textOnPageCheck(user.commonExpectedResults.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.stillWorkingNo, summaryListRowFieldAmountSelector(4))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changeStillWorkingForEmployerHiddenText}",
            cyaChangeLink(4), changeStillWorkingForEmployerHref.url, Some(cyaHiddenChangeLink(4)))
          textOnPageCheck(user.commonExpectedResults.employmentEndDateField1, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.employmentEndDate, summaryListRowFieldAmountSelector(5))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changeEmploymentEndDateHiddenText}",
            cyaChangeLink(5), changeEmploymentEndDateHref.url, Some(cyaHiddenChangeLink(5)))
          textOnPageCheck(user.commonExpectedResults.payrollIdField, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payrollId, summaryListRowFieldAmountSelector(6))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.commonExpectedResults.payrollIdHiddenText}",
            cyaChangeLink(6), payrollIdHref.url, Some(cyaHiddenChangeLink(6)))
          textOnPageCheck(user.commonExpectedResults.payReceivedField3, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(7))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changePayReceivedHiddenText}",
            cyaChangeLink(7), employerPayAmountControllerHref.url, Some(cyaHiddenChangeLink(7)))
          textOnPageCheck(user.commonExpectedResults.taxField4, summaryListRowFieldNameSelector(8))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(8))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.taxTakenFromPayHiddenText}",
            cyaChangeLink(8), taxHref, Some(cyaHiddenChangeLink(8)))
        }

        "for in year return a filtered list on page when minimum data is returned" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
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
          textOnPageCheck(user.commonExpectedResults.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.stillWorkingYes, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.payReceivedField3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.taxField4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(4))
        }
        //noinspection ScalaStyle
        "for end of year return customer employment data if there is both HMRC and customer Employment Data " +
          "and correctly render a filtered list on page when minimum data is returned" when {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(CustomerMinModel.miniData), nino, taxYear - 1)
            urlGet(s"$appUrl/${taxYear - 1}/check-employment-details?employmentId=$employmentId", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          val taxHref = "/income-through-software/return/employment-income/2021/uk-tax?employmentId=001"

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption(2021), captionSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.commonExpectedResults.changeEmployerNameHiddenText}",
            cyaChangeLink(1), employerNameHref.url, Some(cyaHiddenChangeLink(1)))
          textOnPageCheck(user.commonExpectedResults.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.stillWorkingYes, summaryListRowFieldAmountSelector(2))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changeStillWorkingForEmployerHiddenText}",
            cyaChangeLink(2), changeStillWorkingForEmployerHref.url)
          textOnPageCheck(user.commonExpectedResults.payReceivedField3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.payReceivedB, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.changePayReceivedHiddenText}",
            cyaChangeLink(3), employerPayAmountControllerHref.url, Some(cyaHiddenChangeLink(3)))
          textOnPageCheck(user.commonExpectedResults.taxField4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.taxTakenFromPayB, summaryListRowFieldAmountSelector(4))
          linkCheck(s"${user.commonExpectedResults.changeLinkExpected} ${user.specificExpectedResults.get.taxTakenFromPayHiddenText}",
            cyaChangeLink(4), taxHref, Some(cyaHiddenChangeLink(4)))

          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(user.commonExpectedResults.continueButtonLink, continueButtonFormSelector)
        }
        //noinspection ScalaStyle
        "handle a model with an Invalid date format returned" when {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
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
          textOnPageCheck(user.commonExpectedResults.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.stillWorkingYes, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.payReceivedField3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.taxField4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(4))
        }

        "returns an action when auth call fails" which {
          lazy val result: WSResponse = {
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh)
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }

        "redirect to overview page when theres no details" in {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(
              fullEmploymentsModel().copy(hmrcEmploymentData = Seq.empty)
            ), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }
      }
    }
  }


  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        //noinspection ScalaStyle
        "redirect when in year" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, 2022)
            urlPost(s"$appUrl/2022/check-employment-details?employmentId=employmentId", follow = false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
          }
        }
        //noinspection ScalaStyle
        "redirect when at the end of the year when no cya data" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, 2021)
            urlPost(s"$appUrl/2021/check-employment-details?employmentId=employmentId", follow = false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId")
          }
        }
        //noinspection ScalaStyle
        "create the model to update the data and return the correct redirect" which {

          val employmentData: EmploymentCYAModel = {
            employmentUserData.employment.copy(employmentDetails = employmentUserData.employment.employmentDetails.copy(
              employerRef = Some(
                "123/12345"
              ),
              startDate = Some("2020-11-11"),
              taxablePayToDate = Some(55.99),
              totalTaxToDate = Some(3453453.00),
              currentDataIsHmrcHeld = false
            ))
          }

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData.copy(employment = employmentData).copy(employmentId = "001"),userRequest)
            userDataStub(userData(fullEmploymentsModel().copy(customerEmploymentData = fullEmploymentsModel().hmrcEmploymentData)), nino, 2021)

            val model = CreateUpdateEmploymentRequest(
              Some("001"),
              Some(
                CreateUpdateEmployment(
                  employmentData.employmentDetails.employerRef,
                  employmentData.employmentDetails.employerName,
                  employmentData.employmentDetails.startDate.get
                )
              ),
              Some(
                CreateUpdateEmploymentData(
                  pay = CreateUpdatePay(
                    employmentData.employmentDetails.taxablePayToDate.get,
                    employmentData.employmentDetails.totalTaxToDate.get,
                  ),
                  deductions = Some(
                    Deductions(
                      Some(StudentLoans(
                        Some(100),
                        Some(100)
                      ))
                    )
                  )
                )
              )
            )

            stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2021", NO_CONTENT,
              Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

            urlPost(s"$appUrl/2021/check-employment-details?employmentId=001", follow = false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)),body = "{}")
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(EmploymentSummaryController.show(taxYear-1).url)
            findCyaData(taxYear,employmentId,userRequest) shouldBe None
          }
        }
        //noinspection ScalaStyle
        "create the model to update the data and return the correct redirect when there is a hmrc employment to ignore" which {

          val employmentData: EmploymentCYAModel = {
            employmentUserData.employment.copy(employmentDetails = employmentUserData.employment.employmentDetails.copy(
              employerRef = Some(
                "123/12345"
              ),
              startDate = Some("2020-11-11"),
              taxablePayToDate= Some(55.99),
              totalTaxToDate= Some(3453453.00),
              currentDataIsHmrcHeld = false
            ))
          }

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData.copy(employment = employmentData).copy(employmentId = "001"),userRequest)
            userDataStub(userData(fullEmploymentsModel()), nino, 2021)

            val model = CreateUpdateEmploymentRequest(
              None,
              Some(
                CreateUpdateEmployment(
                  employmentData.employmentDetails.employerRef,
                  employmentData.employmentDetails.employerName,
                  employmentData.employmentDetails.startDate.get
                )
              ),
              Some(
                CreateUpdateEmploymentData(
                  pay = CreateUpdatePay(
                    employmentData.employmentDetails.taxablePayToDate.get,
                    employmentData.employmentDetails.totalTaxToDate.get,
                  ),
                  deductions = Some(
                    Deductions(
                      Some(StudentLoans(
                        Some(100),
                        Some(100)
                      ))
                    )
                  )
                )
              ),
              Some("001")
            )

            stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2021", NO_CONTENT,
              Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

            urlPost(s"$appUrl/2021/check-employment-details?employmentId=001", follow = false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)),body = "{}")
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(EmploymentSummaryController.show(taxYear-1).url)
            findCyaData(taxYear,employmentId,userRequest) shouldBe None
          }
        }
        //noinspection ScalaStyle
        "create the model to create the data and return the correct redirect" which {

          val employmentData: EmploymentCYAModel = {
            employmentUserData.employment.copy(employmentDetails = employmentUserData.employment.employmentDetails.copy(
              employerRef = Some(
                "123/12345"
              ),
              startDate = Some("2020-11-11"),
              taxablePayToDate= Some(55.99),
              totalTaxToDate= Some(3453453.00),
              currentDataIsHmrcHeld = false
            ))
          }

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData.copy(employment = employmentData),userRequest)
            noUserDataStub(nino, 2021)

            val model = CreateUpdateEmploymentRequest(
              None,
              Some(
                CreateUpdateEmployment(
                  employmentData.employmentDetails.employerRef,
                  employmentData.employmentDetails.employerName,
                  employmentData.employmentDetails.startDate.get
                )
              ),
              Some(
                CreateUpdateEmploymentData(
                  pay = CreateUpdatePay(
                    employmentData.employmentDetails.taxablePayToDate.get,
                    employmentData.employmentDetails.totalTaxToDate.get,
                  )
                )
              )
            )

            stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2021", NO_CONTENT,
              Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

            urlPost(s"$appUrl/2021/check-employment-details?employmentId=employmentId", follow = false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(EmploymentSummaryController.show(taxYear - 1).url)
            findCyaData(taxYear, employmentId, userRequest) shouldBe None
          }
        }
        //noinspection ScalaStyle
        "create the model to update the data and return the correct redirect when not all data is present" which {

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData, userRequest)
            userDataStub(userData(fullEmploymentsModel()), nino, 2021)
            urlPost(s"$appUrl/2021/check-employment-details?employmentId=employmentId", follow = false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId")
          }
        }
      }
    }
  }
}