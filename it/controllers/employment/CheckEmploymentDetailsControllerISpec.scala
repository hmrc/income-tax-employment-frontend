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

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.employment.PayBuilder.aPay
import builders.models.employment.StudentLoansBuilder.aStudentLoans
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import controllers.employment.routes._
import models.User
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Call
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CheckEmploymentDetailsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"
  private val url = s"$appUrl/$taxYear/check-employment-details?employmentId=$employmentId"

  object Selectors {
    val headingSelector = "#main-content > div > div > header > h1"
    val captionSelector = "#main-content > div > div > header > p"
    val contentTextSelector = "#main-content > div > div > p"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val summaryListSelector = "#main-content > div > div > dl"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val returnToEmploymentSummarySelector = "#returnToEmploymentSummaryBtn"
    val returnToEmployerSelector = "#returnToEmployerBtn"

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
    val changeEmploymentDatesHiddenText: String
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
    val stillWorkingForEmployerField1: String
    val employmentDatesField: String
    val payeReferenceField2: String
    val payReceivedField3: String
    val taxField4: String
    val payrollIdField: String
    val payrollIdHiddenText: String
    val changeEmployerNameHiddenText: String
    val returnToEmploymentSummaryText: String
    val returnToEmploymentSummaryLink: String
    val returnToEmployerText: String
    val returnToEmployerLink: String
  }

  object ContentValues {
    val employerName = "maggie"
    val employmentStartDate = "21 April 2019"
    val employmentEndDate = "11 March 2020"
    val employmentDates = s"$employmentStartDate to $employmentEndDate"
    val payeRef = "223/AB12399"
    val payReceived = "£100"
    val payReceivedB = "£34234.50"
    val taxTakenFromPay = "£200"
    val taxTakenFromPayB = "£6782.90"
    val stillWorkingYes = "Yes"
    val stillWorkingNo = "No"
    val payrollId = "12345678"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val continueButtonLink: String = "/update-and-submit-income-tax-return/employment-income/2021/check-employment-details?employmentId=" + employmentId
    val taxButtonLink: String = "/update-and-submit-income-tax-return/employment-income/2021/uk-tax?employmentId=" + employmentId
    val employerNameField1 = "Employer"
    val employmentStartDateField1 = "Employment start date"
    val stillWorkingForEmployerField1 = "Still working for your employer"
    val employmentDatesField = "Employment dates"
    val payeReferenceField2 = "PAYE reference"
    val payReceivedField3 = "Pay received"
    val taxField4 = "UK tax taken from pay"
    val changeEmployerNameHiddenText: String = "Change the name of this employer"
    val payrollIdField: String = "Payroll ID"
    val payrollIdHiddenText: String = "Change the payroll ID for this employment"
    val returnToEmploymentSummaryText: String = "Return to employment summary"
    val returnToEmploymentSummaryLink: String = "/update-and-submit-income-tax-return/employment-income/2022/employment-summary"
    val returnToEmployerText: String = "Return to employer"
    val returnToEmployerLink: String = "/update-and-submit-income-tax-return/employment-income/2022/employer-details-and-benefits?employmentId=" + employmentId
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val continueButtonLink: String = "/update-and-submit-income-tax-return/employment-income/2021/check-employment-details?employmentId=" + employmentId
    val taxButtonLink: String = "/update-and-submit-income-tax-return/employment-income/2021/uk-tax?employmentId=" + employmentId
    val employerNameField1 = "Employer"
    val employmentStartDateField1 = "Employment start date"
    val stillWorkingForEmployerField1 = "Still working for your employer"
    val employmentDatesField = "Employment dates"
    val payeReferenceField2 = "PAYE reference"
    val payReceivedField3 = "Pay received"
    val taxField4 = "UK tax taken from pay"
    val changeEmployerNameHiddenText: String = "Change the name of this employer"
    val payrollIdField: String = "Payroll ID"
    val payrollIdHiddenText: String = "Change the payroll ID for this employment"
    val returnToEmploymentSummaryText: String = "Return to employment summary"
    val returnToEmploymentSummaryLink: String = "/update-and-submit-income-tax-return/employment-income/2022/employment-summary"
    val returnToEmployerText: String = "Return to employer"
    val returnToEmployerLink: String = "/update-and-submit-income-tax-return/employment-income/2022/employer-details-and-benefits?employmentId=" + employmentId
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your P60"
    val employeeFieldName8 = "Amount of payments not on your P60"
    val changeEmploymentStartDateHiddenText = s"Change your start date for ${ContentValues.employerName}"
    val changeEmploymentDatesHiddenText = "Change your employment dates"
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
    val changeEmploymentDatesHiddenText = "Change your client’s employment dates"
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
    val changeEmploymentDatesHiddenText = "Change your employment dates"
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
    val changeEmploymentDatesHiddenText = "Change your client’s employment dates"
    val changePAYERefHiddenText: String = "Change your client’s PAYE reference number"
    val changePayReceivedHiddenText: String = s"Change the amount of pay your client got from ${ContentValues.employerName}"
    val taxTakenFromPayHiddenText: String = "Change the amount of tax your client paid"
    val paymentsNotOnP60HiddenText: String = "Change if your client got payments that are not on their P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "Change the amount of payments that were not on your client’s P60"
    val changeStillWorkingForEmployerHiddenText = "Change if your client is still working for their employer"
    val paymentsNotOnYourP60: String = "Payments not on your client’s P60"
  }

  object ChangeLinks {
    val employerPayAmountControllerHref: Call = EmployerPayAmountController.show(taxYear - 1, employmentId)
    val employerNameHref: Call = EmployerNameController.show(taxYear - 1, employmentId)
    val payeRefHref: Call = PayeRefController.show(taxYear - 1, employmentId)
    val changeEmploymentStartDateHref: Call = EmployerStartDateController.show(taxYear - 1, employmentId)
    val changeEmploymentDatesHref: Call = CheckEmploymentDetailsController.show(taxYear - 1, employmentId)
    val payrollIdHref: Call = EmployerPayrollIdController.show(taxYear - 1, employmentId)
    val changeStillWorkingForEmployerHref: Call = StillWorkingForEmployerController.show(taxYear - 1, employmentId)
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  object MinModel {
    val miniData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(
        EmploymentSource(
          employmentId = employmentId,
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
            pay = Some(aPay),
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
          employmentId = employmentId,
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
          employmentId = employmentId,
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
            pay = Some(Pay(Some(100), Some(200), None, None, None, None)),
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
        val specific = user.specificExpectedResults.get
        val common = user.commonExpectedResults

        "for end of year return a fully populated page when cya data exists" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(EmploymentUserData(
              sessionId,
              "1234567890",
              "AA123456A",
              2021,
              employmentId,
              isPriorSubmission = true,
              hasPriorBenefits = true,
              EmploymentCYAModel(
                anEmploymentSource.toEmploymentDetails(isUsingCustomerData = false).copy(cessationDateQuestion = Some(false)),
                None
              )
            ), User(mtditid, if (user.isAgent) Some("12345678") else None, nino, sessionId, if (user.isAgent) "Agent" else "Individual")(FakeRequest()))
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, 2021)
            urlGet(s"$appUrl/2021/check-employment-details?employmentId=$employmentId", follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(common.expectedCaption(2021), captionSelector)
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(common.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.stillWorkingNo, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${common.changeLinkExpected} ${specific.changeStillWorkingForEmployerHiddenText}", cyaChangeLink(3), changeStillWorkingForEmployerHref.url)
          textOnPageCheck(common.employmentDatesField, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employmentDates, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.payrollId, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(7))
        }

        "for in year return a fully populated page when all the fields are populated" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(common.expectedCaption(taxYear), captionSelector)
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          textOnPageCheck(specific.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(common.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.stillWorkingNo, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(common.employmentDatesField, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employmentDates, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.payrollId, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(7))
          buttonCheck(user.commonExpectedResults.returnToEmploymentSummaryText, Selectors.returnToEmploymentSummarySelector)
        }

        "for in year with multiple employment sources, return a fully populated page when all fields are populated" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val multipleSources = Seq(
              anEmploymentSource,
              anEmploymentSource.copy(
                employmentId = "002",
                employerName = "dave",
                payrollId = Some("12345693"),
                startDate = Some("2018-04-18"),
              ))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = multipleSources))), nino, taxYear)
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
          textOnPageCheck(user.commonExpectedResults.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.stillWorkingNo, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.employmentDatesField, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employmentDates, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(user.commonExpectedResults.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.payrollId, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(user.commonExpectedResults.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(user.commonExpectedResults.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(7))
          buttonCheck(user.commonExpectedResults.returnToEmployerText, Selectors.returnToEmployerSelector)
        }

        "for end of year return a fully populated page, with change links, when all the fields are populated" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            urlGet(s"$appUrl/${taxYear - 1}/check-employment-details?employmentId=$employmentId", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(common.expectedCaption(2021), captionSelector)
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          linkCheck(s"${common.changeLinkExpected} ${common.changeEmployerNameHiddenText}", cyaChangeLink(1), employerNameHref.url, Some(cyaHiddenChangeLink(1)))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          linkCheck(s"${common.changeLinkExpected} ${specific.changePAYERefHiddenText}", cyaChangeLink(2), payeRefHref.url, Some(cyaHiddenChangeLink(2)))
          textOnPageCheck(common.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.stillWorkingNo, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${common.changeLinkExpected} ${specific.changeStillWorkingForEmployerHiddenText}", cyaChangeLink(3), changeStillWorkingForEmployerHref.url, Some(cyaHiddenChangeLink(3)))
          textOnPageCheck(common.employmentDatesField, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employmentDates, summaryListRowFieldAmountSelector(4))
          linkCheck(s"${common.changeLinkExpected} ${specific.changeEmploymentDatesHiddenText}", cyaChangeLink(4), changeEmploymentDatesHref.url, Some(cyaHiddenChangeLink(4)))
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.payrollId, summaryListRowFieldAmountSelector(5))
          linkCheck(s"${common.changeLinkExpected} ${common.payrollIdHiddenText}", cyaChangeLink(5), payrollIdHref.url, Some(cyaHiddenChangeLink(5)))
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(6))
          linkCheck(s"${common.changeLinkExpected} ${specific.changePayReceivedHiddenText}", cyaChangeLink(6), employerPayAmountControllerHref.url, Some(cyaHiddenChangeLink(6)))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(7))
          linkCheck(s"${common.changeLinkExpected} ${specific.taxTakenFromPayHiddenText}", cyaChangeLink(7), EmploymentTaxController.show(taxYear - 1, employmentId).url, Some(cyaHiddenChangeLink(7)))
        }

        "for end of year return a fully populated page, with change links when minimum data is returned" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(MinModel.miniData)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(common.expectedCaption(taxYear), captionSelector)
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          textOnPageCheck(specific.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(2), "for payee reference")
          textOnPageCheck(common.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.stillWorkingYes, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(common.employmentStartDateField1, summaryListRowFieldNameSelector(4))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(4), "for start date")
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(5), "for payroll")
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(7))
        }

        "for end of year return customer employment data if there is both HMRC and customer Employment Data " +
          "and render page without filtering when minimum data is returned" when {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(CustomerMinModel.miniData)), nino, taxYear - 1)
            urlGet(s"$appUrl/${taxYear - 1}/check-employment-details?employmentId=$employmentId", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(common.expectedCaption(2021), captionSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          linkCheck(s"${common.changeLinkExpected} ${common.changeEmployerNameHiddenText}", cyaChangeLink(1), employerNameHref.url, Some(cyaHiddenChangeLink(1)))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(2), "for payee reference")
          linkCheck(s"${common.changeLinkExpected} ${specific.changePAYERefHiddenText}", cyaChangeLink(2), payeRefHref.url, Some(cyaHiddenChangeLink(2)))
          textOnPageCheck(common.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.stillWorkingYes, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${common.changeLinkExpected} ${specific.changeStillWorkingForEmployerHiddenText}", cyaChangeLink(3), changeStillWorkingForEmployerHref.url)
          textOnPageCheck(common.employmentStartDateField1, summaryListRowFieldNameSelector(4))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(4), "for start date")
          linkCheck(s"${common.changeLinkExpected} ${specific.changeEmploymentStartDateHiddenText}", cyaChangeLink(4), changeEmploymentStartDateHref.url, Some(cyaHiddenChangeLink(4)))
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(5), "for payroll")
          linkCheck(s"${common.changeLinkExpected} ${common.payrollIdHiddenText}", cyaChangeLink(5), payrollIdHref.url, Some(cyaHiddenChangeLink(5)))
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceivedB, summaryListRowFieldAmountSelector(6))
          linkCheck(s"${common.changeLinkExpected} ${specific.changePayReceivedHiddenText}", cyaChangeLink(6), employerPayAmountControllerHref.url, Some(cyaHiddenChangeLink(6)))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPayB, summaryListRowFieldAmountSelector(7))
          linkCheck(s"${common.changeLinkExpected} ${specific.taxTakenFromPayHiddenText}", cyaChangeLink(7), EmploymentTaxController.show(taxYear - 1, employmentId).url, Some(cyaHiddenChangeLink(7)))

          buttonCheck(common.continueButtonText, continueButtonSelector)
          formPostLinkCheck(common.continueButtonLink, continueButtonFormSelector)
        }

        "handle a model with an Invalid date format returned" when {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(SomeModelWithInvalidDateFormat.invalidData)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(common.expectedCaption(taxYear), captionSelector)
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          textOnPageCheck(specific.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(2), "for payee reference")
          textOnPageCheck(common.stillWorkingForEmployerField1, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.stillWorkingYes, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(common.employmentStartDateField1, summaryListRowFieldNameSelector(4))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(4), "for start date")
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(5), "for payroll")
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(7))
        }
      }
    }

    "redirect to the overview page when prior data exists but not matching the id" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, 2021)
        urlGet(s"$appUrl/2021/check-employment-details?employmentId=001", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(2021))
      }
    }

    "redirect to the overview page when no data exists" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        noUserDataStub(nino, 2021)
        urlGet(s"$appUrl/2021/check-employment-details?employmentId=employmentId", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(2021))
      }
    }

    "for end of year return a redirect when cya data exists but not finished when its a new employment" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(EmploymentUserData(
          sessionId,
          "1234567890",
          "AA123456A",
          2021,
          employmentId,
          isPriorSubmission = false,
          hasPriorBenefits = true,
          EmploymentCYAModel(
            anEmploymentSource.toEmploymentDetails(false).copy(employerRef = None),
            None
          )
        ), User(mtditid, None, nino, sessionId, "Individual")(FakeRequest()))
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, 2021)
        urlGet(s"$appUrl/2021/check-employment-details?employmentId=$employmentId", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
      }

      "has a redirect url of employer reference page" in {
        result.header("location") shouldBe Some(PayeRefController.show(2021, employmentId).url)
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        unauthorisedAgentOrIndividual(isAgent = false)
        urlGet(url)
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

    "redirect to overview page when theres no details" in {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq.empty))), nino, taxYear)
        urlGet(url, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }

  ".submit" when {
    "redirect when in year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, 2022)
        urlPost(s"$appUrl/2022/check-employment-details?employmentId=employmentId", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect when at the end of the year when no cya data" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, 2021)
        urlPost(s"$appUrl/2021/check-employment-details?employmentId=$employmentId", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckEmploymentDetailsController.show(2021, employmentId).url)
      }
    }

    "create the model to update the data and return the correct redirect" which {
      val employmentData = anEmploymentCYAModel.copy(employmentBenefits = None)
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = employmentData).copy(employmentId = employmentId), aUserRequest)
        val customerEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(customerEmploymentData = customerEmploymentData))), nino, 2021)

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
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
              deductions = Some(Deductions(Some(aStudentLoans)))
            )
          )
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2021", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(s"$appUrl/2021/check-employment-details?employmentId=$employmentId", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EmploymentSummaryController.show(taxYear - 1).url)
        findCyaData(taxYear, employmentId, aUserRequest) shouldBe None
      }
    }

    "create the model to update the data and return the correct redirect when there is a hmrc employment to ignore" which {
      val employmentData: EmploymentCYAModel = anEmploymentCYAModel.copy(employmentBenefits = None)
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = employmentData).copy(employmentId = employmentId), aUserRequest)
        val hmrcEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = hmrcEmploymentData))), nino, 2021)

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
              deductions = Some(Deductions(Some(aStudentLoans)))
            )
          ),
          Some(employmentId)
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2021", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(s"$appUrl/2021/check-employment-details?employmentId=$employmentId", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EmploymentSummaryController.show(taxYear - 1).url)
        findCyaData(taxYear, employmentId, aUserRequest) shouldBe None
      }
    }

    "create the model to create the data and return the correct redirect" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData, aUserRequest)
        noUserDataStub(nino, 2021)

        val model = CreateUpdateEmploymentRequest(
          None,
          Some(CreateUpdateEmployment(
            anEmploymentCYAModel.employmentDetails.employerRef,
            anEmploymentCYAModel.employmentDetails.employerName,
            anEmploymentCYAModel.employmentDetails.startDate.get
          )),
          Some(CreateUpdateEmploymentData(
            pay = CreateUpdatePay(
              anEmploymentCYAModel.employmentDetails.taxablePayToDate.get,
              anEmploymentCYAModel.employmentDetails.totalTaxToDate.get,
            )
          ))
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2021", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(s"$appUrl/2021/check-employment-details?employmentId=employmentId", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(EmploymentSummaryController.show(taxYear - 1).url)
        findCyaData(taxYear, employmentId, aUserRequest) shouldBe None
      }
    }

    "create the model to update the data and return the correct redirect when not all data is present" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData, aUserRequest)
        userDataStub(anIncomeTaxUserData, nino, 2021)
        urlPost(s"$appUrl/2021/check-employment-details?employmentId=001", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckEmploymentDetailsController.show(2021, "001").url)
      }
    }
  }
}