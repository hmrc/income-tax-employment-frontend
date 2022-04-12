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

import common.SessionValues
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentFinancialDataBuilder.aHmrcEmploymentFinancialData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import support.builders.models.employment.PayBuilder.aPay
import support.builders.models.employment.StudentLoansBuilder.aStudentLoans
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CheckEmploymentDetailsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  object Selectors {
    var notificationBanner: String = ".govuk-notification-banner"
    var bannerEmployerRefSelector: String = "#paye-ref-link"
    var bannerEmployerStartDateSelector: String = "#employer-start-date-link"
    var bannerEmploymentDatesSelector: String = "#employment-dates-link"
    var bannerPayrollIdSelector: String = "#employer-payroll-id-link"
    var bannerTaxablePayToDateSelector: String = "#employer-pay-amount-link"
    var bannerTotalTaxToDateSelector: String = "#employment-tax-link"
    val contentTextSelector = "#main-content > div > div > p"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
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
    val changeEmploymentStartDateHiddenText: String => String
    val changeEmploymentDatesHiddenText: String

    def changeLeftEmployerHiddenText(name: String): String

    val paymentsNotOnYourP60: String
    val changePAYERefHiddenText: String
    val changePayReceivedHiddenText: String => String
    val taxTakenFromPayHiddenText: String
    val paymentsNotOnP60HiddenText: String
    val amountOfPaymentsNotOnP60HiddenText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val addLinkExpected: String
    val changeLinkExpected: String
    val continueButtonText: String
    val employerNameField1: String
    val employmentStartDateField1: String
    val didYouLeaveEmployerField: String
    val employmentDatesField: String
    val payeReferenceField2: String
    val payReceivedField3: String
    val taxField4: String
    val payrollIdField: String
    val payrollIdHiddenText: String
    val changeEmployerNameHiddenText: String
    val returnToEmploymentSummaryText: String
    val returnToEmployerText: String
    val employmentStartDate: String
    val employmentEndDate: String
    val employmentDates: String
    val didYouLeaveNo: String
    val didYouLeaveYes: String
  }

  object ContentValues {
    val employerName = "maggie"
    val payeRef = "223/AB12399"
    val payReceived = "£100"
    val payReceivedB = "£34,234.50"
    val taxTakenFromPay = "£200"
    val taxTakenFromPayB = "£6,782.90"
    val payrollId = "12345678"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val addLinkExpected = "Add"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val employerNameField1 = "Employer"
    val employmentStartDateField1 = "Employment start date"
    val didYouLeaveEmployerField = "Left employer"
    val employmentDatesField = "Employment dates"
    val payeReferenceField2 = "PAYE reference"
    val payReceivedField3 = "Pay received"
    val taxField4 = "UK tax taken from pay"
    val changeEmployerNameHiddenText: String = "Change the name of this employer"
    val payrollIdField: String = "Payroll ID"
    val payrollIdHiddenText: String = "Change the payroll ID for this employment"
    val returnToEmploymentSummaryText: String = "Return to employment summary"
    val returnToEmployerText: String = "Return to employer"
    val employmentStartDate = "21 April 2019"
    val employmentEndDate = s"11 March ${taxYearEOY - 1}"
    val employmentDates = s"$employmentStartDate to $employmentEndDate"
    val didYouLeaveNo = "No"
    val didYouLeaveYes = "Yes"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val addLinkExpected = "Add"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val employerNameField1 = "Employer"
    val employmentStartDateField1 = "Employment start date"
    val didYouLeaveEmployerField = "Left employer"
    val employmentDatesField = "Employment dates"
    val payeReferenceField2 = "PAYE reference"
    val payReceivedField3 = "Pay received"
    val taxField4 = "UK tax taken from pay"
    val changeEmployerNameHiddenText: String = "Change the name of this employer"
    val payrollIdField: String = "Payroll ID"
    val payrollIdHiddenText: String = "Change the payroll ID for this employment"
    val returnToEmploymentSummaryText: String = "Return to employment summary"
    val returnToEmployerText: String = "Return to employer"
    val employmentStartDate = "21 April 2019"
    val employmentEndDate = s"11 March ${taxYearEOY - 1}"
    val employmentDates = s"$employmentStartDate to $employmentEndDate"
    val didYouLeaveNo = "No"
    val didYouLeaveYes = "Yes"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your P60"
    val employeeFieldName8 = "Amount of payments not on your P60"
    val changeEmploymentStartDateHiddenText: String => String = (employerName: String) => s"Change your start date for $employerName"
    val changeEmploymentDatesHiddenText = "Change your employment dates"
    val changePAYERefHiddenText: String = "Change your PAYE reference number"
    val changePayReceivedHiddenText: String => String = (employerName: String) => s"Change the amount of pay you got from $employerName"
    val taxTakenFromPayHiddenText: String = "Change the amount of tax you paid"
    val paymentsNotOnP60HiddenText: String = "Change if you got payments that are not on your P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "Change the amount of payments that were not on your P60"

    def changeLeftEmployerHiddenText(name: String): String = s"Change if you left $name in the tax year"

    val paymentsNotOnYourP60: String = "Payments not on your P60"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val changeEmploymentStartDateHiddenText: String => String = (employerName: String) => s"Change your client’s start date for $employerName"
    val changeEmploymentDatesHiddenText = "Change your client’s employment dates"
    val changePAYERefHiddenText: String = "Change your client’s PAYE reference number"
    val changePayReceivedHiddenText: String => String = (employerName: String) => s"Change the amount of pay your client got from $employerName"
    val taxTakenFromPayHiddenText: String = "Change the amount of tax your client paid"
    val paymentsNotOnP60HiddenText: String = "Change if your client got payments that are not on their P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "Change the amount of payments that were not on your client’s P60"

    def changeLeftEmployerHiddenText(name: String): String = s"Change if your client left $name in the tax year"

    val paymentsNotOnYourP60: String = "Payments not on your client’s P60"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val changeEmploymentStartDateHiddenText: String => String = (employerName: String) => s"Change your start date for $employerName"
    val changeEmploymentDatesHiddenText = "Change your employment dates"
    val changePAYERefHiddenText: String = "Change your PAYE reference number"
    val changePayReceivedHiddenText: String => String = (employerName: String) => s"Change the amount of pay you got from $employerName"
    val taxTakenFromPayHiddenText: String = "Change the amount of tax you paid"
    val paymentsNotOnP60HiddenText: String = "Change if you got payments that are not on your P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "Change the amount of payments that were not on your P60"

    def changeLeftEmployerHiddenText(name: String): String = s"Change if you left $name in the tax year"

    val paymentsNotOnYourP60: String = "Payments not on your P60"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your client’s P60"
    val employeeFieldName8 = "Amount of payments not on your client’s P60"
    val changeEmploymentStartDateHiddenText: String => String = (employerName: String) => s"Change your client’s start date for $employerName"
    val changeEmploymentDatesHiddenText = "Change your client’s employment dates"
    val changePAYERefHiddenText: String = "Change your client’s PAYE reference number"
    val changePayReceivedHiddenText: String => String = (employerName: String) => s"Change the amount of pay your client got from $employerName"
    val taxTakenFromPayHiddenText: String = "Change the amount of tax your client paid"
    val paymentsNotOnP60HiddenText: String = "Change if your client got payments that are not on their P60"
    val amountOfPaymentsNotOnP60HiddenText: String = "Change the amount of payments that were not on your client’s P60"

    def changeLeftEmployerHiddenText(name: String): String = s"Change if your client left $name in the tax year"

    val paymentsNotOnYourP60: String = "Payments not on your client’s P60"
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
        HmrcEmploymentSource(
          employmentId = employmentId,
          employerName = "maggie",
          employerRef = None,
          payrollId = None,
          startDate = None,
          cessationDate = None,
          dateIgnored = None,
          submittedOn = None,
          hmrcEmploymentFinancialData = Some(
            EmploymentFinancialData(
              employmentData = Some(EmploymentData(
                submittedOn = s"${taxYearEOY - 1}-02-12",
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
            submittedOn = s"${taxYearEOY - 1}-02-12",
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
        HmrcEmploymentSource(
          employmentId = employmentId,
          employerName = "maggie",
          employerRef = None,
          payrollId = None,
          startDate = None,
          cessationDate = None,
          dateIgnored = None,
          submittedOn = None,
          hmrcEmploymentFinancialData = Some(
            EmploymentFinancialData(
              employmentData = Some(EmploymentData(
                submittedOn = s"${taxYearEOY - 1}-02-12",
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
        val specific = user.specificExpectedResults.get
        val common = user.commonExpectedResults

        "for end of year when data not submittable" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            val employmentDetails = anEmploymentDetails.copy(employerRef = None, startDate = None, payrollId = None, didYouLeaveQuestion = Some(false), taxablePayToDate = None, totalTaxToDate = None)
            insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentDetails = employmentDetails)))
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          "has a Notification banner with links" which {
            textOnPageCheck(user.commonExpectedResults.payeReferenceField2, Selectors.bannerEmployerRefSelector)
            textOnPageCheck(user.commonExpectedResults.employmentStartDateField1, Selectors.bannerEmployerStartDateSelector)
            textOnPageCheck(user.commonExpectedResults.payrollIdField, Selectors.bannerPayrollIdSelector)
            textOnPageCheck(user.commonExpectedResults.payReceivedField3, Selectors.bannerTaxablePayToDateSelector)
            textOnPageCheck(user.commonExpectedResults.taxField4, Selectors.bannerTotalTaxToDateSelector)
          }

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYearEOY))
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(anEmploymentDetails.employerName, summaryListRowFieldAmountSelector(1))
          linkCheck(s"${common.changeLinkExpected} ${common.changeEmployerNameHiddenText}", cyaChangeLink(1), employerNameUrl(taxYearEOY, employmentId))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(2), "paye ref")
          linkCheck(s"${common.addLinkExpected} ${specific.changePAYERefHiddenText}", cyaChangeLink(2), employerPayeReferenceUrl(taxYearEOY, employmentId))
          textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
          textOnPageCheck("No", summaryListRowFieldAmountSelector(3))
          linkCheck(s"${common.changeLinkExpected} ${specific.changeLeftEmployerHiddenText(anEmploymentDetails.employerName)}", cyaChangeLink(3), didYouLeaveUrl(taxYearEOY, employmentId))
          textOnPageCheck(common.employmentStartDateField1, summaryListRowFieldNameSelector(4))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(4), "employment start date")
          linkCheck(s"${common.addLinkExpected} ${specific.changeEmploymentStartDateHiddenText(anEmploymentDetails.employerName)}", cyaChangeLink(4), employmentStartDateUrl(taxYearEOY, employmentId))
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(5), "payroll id")
          linkCheck(s"${common.addLinkExpected} ${common.payrollIdHiddenText}", cyaChangeLink(5), payrollIdUrl(taxYearEOY, employmentId))
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(6), "pay received")
          linkCheck(s"${common.addLinkExpected} ${specific.changePayReceivedHiddenText(anEmploymentDetails.employerName)}", cyaChangeLink(6), howMuchPayUrl(taxYearEOY, employmentId))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(7), "tax taken from pay")
          linkCheck(s"${common.addLinkExpected} ${specific.taxTakenFromPayHiddenText}", cyaChangeLink(7), howMuchTaxUrl(taxYearEOY, employmentId))
        }

        "for end of year return a fully populated page when cya data exists" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(EmploymentUserData(
              sessionId,
              "1234567890",
              "AA123456A",
              taxYearEOY,
              employmentId,
              isPriorSubmission = true,
              hasPriorBenefits = true,
              hasPriorStudentLoans = true,
              EmploymentCYAModel(
                anEmploymentSource.toEmploymentDetails(isUsingCustomerData = false).copy(didYouLeaveQuestion = Some(true)),
                None
              )
            ))
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYearEOY))
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
          textOnPageCheck(common.didYouLeaveYes, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${common.changeLinkExpected} ${specific.changeLeftEmployerHiddenText("maggie")}", cyaChangeLink(3), didYouLeaveUrl(taxYearEOY, employmentId))
          textOnPageCheck(common.employmentDatesField, summaryListRowFieldNameSelector(4))
          textOnPageCheck(common.employmentDates, summaryListRowFieldAmountSelector(4))
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
            urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          "has no Notification banner" in {
            elementExist(notificationBanner) shouldBe false
          }

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYear))
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          textOnPageCheck(specific.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
          textOnPageCheck(common.didYouLeaveYes, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(common.employmentDatesField, summaryListRowFieldNameSelector(4))
          textOnPageCheck(common.employmentDates, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.payrollId, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(7))
          buttonCheck(user.commonExpectedResults.returnToEmployerText, Selectors.returnToEmployerSelector)
        }

        "for in year with multiple employment sources, return a fully populated page when all fields are populated" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val multipleSources: Seq[HmrcEmploymentSource] = Seq(
              aHmrcEmploymentSource,
              aHmrcEmploymentSource.copy(
                employmentId = "002",
                employerName = "dave",
                payrollId = Some("12345693"),
                startDate = Some("2018-04-18"),
              ))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = multipleSources))), nino, taxYear)
            urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(common.expectedCaption(taxYear))
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, contentTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(user.commonExpectedResults.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(user.commonExpectedResults.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
          textOnPageCheck(common.didYouLeaveYes, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(user.commonExpectedResults.employmentDatesField, summaryListRowFieldNameSelector(4))
          textOnPageCheck(common.employmentDates, summaryListRowFieldAmountSelector(4))
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
            urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYearEOY))
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          linkCheck(s"${common.changeLinkExpected} ${common.changeEmployerNameHiddenText}", cyaChangeLink(1), employerNameUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(1)))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.payeRef, summaryListRowFieldAmountSelector(2))
          linkCheck(s"${common.changeLinkExpected} ${specific.changePAYERefHiddenText}", cyaChangeLink(2), employerPayeReferenceUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(2)))
          textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
          textOnPageCheck(common.didYouLeaveYes, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${common.changeLinkExpected} ${specific.changeLeftEmployerHiddenText("maggie")}", cyaChangeLink(3),
            didYouLeaveUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(3)))
          textOnPageCheck(common.employmentDatesField, summaryListRowFieldNameSelector(4))
          textOnPageCheck(common.employmentDates, summaryListRowFieldAmountSelector(4))
          linkCheck(s"${common.changeLinkExpected} ${specific.changeEmploymentDatesHiddenText}", cyaChangeLink(4), employmentDatesUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(4)))
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.payrollId, summaryListRowFieldAmountSelector(5))
          linkCheck(s"${common.changeLinkExpected} ${common.payrollIdHiddenText}", cyaChangeLink(5), payrollIdUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(5)))
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceived, summaryListRowFieldAmountSelector(6))
          linkCheck(s"${common.changeLinkExpected} ${specific.changePayReceivedHiddenText(ContentValues.employerName)}",
            cyaChangeLink(6), howMuchPayUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(6)))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldAmountSelector(7))
          linkCheck(s"${common.changeLinkExpected} ${specific.taxTakenFromPayHiddenText}", cyaChangeLink(7), howMuchTaxUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(7)))
        }

        "for end of year return a fully populated page, with change links when minimum data is returned" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(MinModel.miniData)), nino, taxYear)
            urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYear))
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          textOnPageCheck(specific.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(2), "for payee reference")
          textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
          textOnPageCheck(common.didYouLeaveNo, summaryListRowFieldAmountSelector(3))
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
            userDataStub(anIncomeTaxUserData.copy(Some(CustomerMinModel.miniData)), nino, taxYearEOY)
            urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYearEOY))
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          linkCheck(s"${common.changeLinkExpected} ${common.changeEmployerNameHiddenText}", cyaChangeLink(1), employerNameUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(1)))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(2), "for payee reference")
          linkCheck(s"${common.addLinkExpected} ${specific.changePAYERefHiddenText}", cyaChangeLink(2), employerPayeReferenceUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(2)))
          textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
          textOnPageCheck(common.didYouLeaveNo, summaryListRowFieldAmountSelector(3))
          linkCheck(s"${common.changeLinkExpected} ${specific.changeLeftEmployerHiddenText("maggie")}", cyaChangeLink(3), didYouLeaveUrl(taxYearEOY, employmentId))
          textOnPageCheck(common.employmentStartDateField1, summaryListRowFieldNameSelector(4))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(4), "for start date")
          linkCheck(s"${common.addLinkExpected} ${specific.changeEmploymentStartDateHiddenText(ContentValues.employerName)}", cyaChangeLink(4),
            employmentStartDateUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(4)))
          textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(5), "for payroll")
          linkCheck(s"${common.addLinkExpected} ${common.payrollIdHiddenText}", cyaChangeLink(5), payrollIdUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(5)))
          textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.payReceivedB, summaryListRowFieldAmountSelector(6))
          linkCheck(s"${common.changeLinkExpected} ${specific.changePayReceivedHiddenText(ContentValues.employerName)}",
            cyaChangeLink(6), howMuchPayUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(6)))
          textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.taxTakenFromPayB, summaryListRowFieldAmountSelector(7))
          linkCheck(s"${common.changeLinkExpected} ${specific.taxTakenFromPayHiddenText}", cyaChangeLink(7), howMuchTaxUrl(taxYearEOY, employmentId), Some(cyaHiddenChangeLink(7)))

          buttonCheck(common.continueButtonText, continueButtonSelector)
          formPostLinkCheck(checkYourDetailsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
        }

        "handle a model with an Invalid date format returned" when {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(Some(SomeModelWithInvalidDateFormat.invalidData)), nino, taxYear)
            urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYear))
          textOnPageCheck(specific.expectedContent, contentTextSelector)
          textOnPageCheck(specific.expectedInsetText, insetTextSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employerName, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
          textOnPageCheck("Not provided", summaryListRowFieldAmountSelector(2), "for payee reference")
          textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
          textOnPageCheck(common.didYouLeaveNo, summaryListRowFieldAmountSelector(3))
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
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, "001")), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the overview page when no data exists" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        noUserDataStub(nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYearEOY)) shouldBe true
      }
    }

    "for end of year return a redirect when cya data exists but not finished when its a new employment" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(EmploymentUserData(
          sessionId,
          "1234567890",
          "AA123456A",
          taxYearEOY,
          employmentId,
          isPriorSubmission = false,
          hasPriorBenefits = true, hasPriorStudentLoans = false,
          EmploymentCYAModel(
            anEmploymentSource.toEmploymentDetails(false).copy(employerRef = None),
            None
          )
        ))
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
      }

      "has a redirect url of employer reference page" in {
        result.header("location").contains(employerPayeReferenceUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        unauthorisedAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)))
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
        urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }
  }

  ".submit" when {
    "redirect when in year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, 2022)
        urlPost(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect when at the end of the year when no cya data" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "create the model to update the data and return the correct redirect when there is a hmrc employment to ignore" which {
      val employmentData: EmploymentCYAModel = anEmploymentCYAModel.copy(employmentBenefits = None)
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = employmentData).copy(employmentId = employmentId, hasPriorBenefits = false))
        val hmrcEmploymentData = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None))))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = hmrcEmploymentData))), nino, taxYearEOY)

        val model = CreateUpdateEmploymentRequest(
          None,
          Some(
            CreateUpdateEmployment(
              employmentData.employmentDetails.employerRef,
              employmentData.employmentDetails.employerName,
              employmentData.employmentDetails.startDate.get,
              payrollId = anEmploymentCYAModel.employmentDetails.payrollId
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

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", CREATED,
          Json.toJson(model).toString(), """{"employmentId":"id"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerInformationUrl(taxYearEOY, "id")) shouldBe true
        findCyaData(taxYear, employmentId, anAuthorisationRequest) shouldBe None
      }
    }
    "create the model to update the data and return the correct redirect and return to employer information page" which {
      val employmentData: EmploymentCYAModel = anEmploymentCYAModel.copy(employmentBenefits = None)
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = employmentData).copy(employmentId = employmentId, hasPriorBenefits = false))
        val hmrcEmploymentData = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None))))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = hmrcEmploymentData))), nino, taxYearEOY)

        val model = CreateUpdateEmploymentRequest(
          None,
          Some(
            CreateUpdateEmployment(
              employmentData.employmentDetails.employerRef,
              employmentData.employmentDetails.employerName,
              employmentData.employmentDetails.startDate.get,
              payrollId = anEmploymentCYAModel.employmentDetails.payrollId
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

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerInformationUrl(taxYearEOY, employmentId)) shouldBe true
        findCyaData(taxYear, employmentId, anAuthorisationRequest) shouldBe None
      }
    }

    "create the model to create the data and return the correct redirect" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(hasPriorBenefits = false))
        noUserDataStub(nino, taxYearEOY)

        val model = CreateUpdateEmploymentRequest(
          None,
          Some(CreateUpdateEmployment(
            anEmploymentCYAModel.employmentDetails.employerRef,
            anEmploymentCYAModel.employmentDetails.employerName,
            anEmploymentCYAModel.employmentDetails.startDate.get,
            payrollId = anEmploymentCYAModel.employmentDetails.payrollId
          )),
          Some(CreateUpdateEmploymentData(
            pay = CreateUpdatePay(
              anEmploymentCYAModel.employmentDetails.taxablePayToDate.get,
              anEmploymentCYAModel.employmentDetails.totalTaxToDate.get,
            )
          ))
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", CREATED,
          Json.toJson(model).toString(), """{"employmentId": "id"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY,
          extraData = Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, "id")) shouldBe true
        findCyaData(taxYear, employmentId, anAuthorisationRequest) shouldBe None
      }
    }

    "create the model to update the data and return the correct redirect when not all data is present" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, "001")), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, "001")) shouldBe true
      }
    }
  }
}