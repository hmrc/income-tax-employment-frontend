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

import controllers.details.routes._
import controllers.employment.routes._
import controllers.offPayrollWorking.routes._
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.employment.EmploymentDetailsViewModelBuilder.anEmploymentDetailsViewModel

import views.html.employment.CheckEmploymentDetailsView
class CheckEmploymentDetailsViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"

  object Selectors {
    val contentTextSelector = "#employmentInfoParagraph"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val returnToEmploymentSummarySelector = "#returnToEmploymentSummaryBtn"
    val returnToEmployerSelector = "#returnToEmployerBtn"
    val offPayrollWorkingSubHeading = "#main-content > div > div > h2"
    val offPayrollWorkingParagraph = "#offPayrollWorkingP1"
    val saveAndContinueButtonSelector = "#saveAndContinueBtn"


    def opwsummaryListRowFieldNameSelector(i: Int): String = s"#opwSummaryList > div > dt"
    def opwsummaryListRowValuedNameSelector(i: Int): String = s"#opwSummaryList > div > dd.govuk-summary-list__value"

    def summaryListRowFieldNameSelector(i: Int): String = s"#employmentDetailsSummaryList > div:nth-child($i) > dt"

    def summaryListRowFieldValueSelector(i: Int): String = s"#employmentDetailsSummaryList > div:nth-child($i) > dd.govuk-summary-list__value"
    def cyaChangeLink(i: Int): String = s"#employmentDetailsSummaryList > div:nth-child($i) > dd.govuk-summary-list__actions > a"
    def cyaHiddenChangeLink(i: Int): String = s"#employmentDetailsSummaryList > div:nth-child($i) > dd.govuk-summary-list__actions > a > span.govuk-visually-hidden"
    def opwHiddenChangeLink(i: Int): String = s"#opwSummaryList > div > dd.govuk-summary-list__actions > a > span.govuk-visually-hidden"
    def opwCyaChangeLink(i: Int): String = s"#opwSummaryList > div > dd.govuk-summary-list__actions > a"

  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedContent: String
    val expectedInsetText: String
    val didYouLeaveHiddenText: String
    val offPayrollWorkingParagraph : String
    val changePayReceivedHiddenText: String
    val offPayrollWorkingField: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val changeLinkExpected: String
    val continueButtonText: String
    val employerNameField1: String
    val employmentStartDateField: String
    val didYouLeaveEmployerField: String
    val employmentEndDateField: String
    val payeReferenceField2: String
    val payReceivedField3: String
    val taxField4: String
    val payrollIdField: String
    val offPayrollWorkingSubHeading: String
    val employmentStartDateAddHiddenText: String


    val employerHiddenText: String
    val payeRefHiddenText: String
    val startDateHiddenText: String
    val endDateHiddenText: String
    val payrollIdHiddenText: String
    val payReceivedHiddenText: String
    val totalTaxToDateHiddenText: String
    val offPayrollWorkingHiddenText: String

    val returnToEmployerText: String
    val employmentStartDate: String
    val employmentEndDate: String
    val no: String
    val yes: String
    val notProvided: String
  }

  object ContentValues {
    val employerName = "maggie"
    val payeRef = "223/AB12399"
    val payReceived = "£100"
    val taxTakenFromPay = "£200"
    val payrollId = "12345678"
  }

  object CommonExpected extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val employerNameField1 = "Employer"
    val employmentStartDateField = "Employment start date"
    val didYouLeaveEmployerField = "Left employer"
    val employmentEndDateField = "Employment end date"
    val payeReferenceField2 = "PAYE reference"
    val payReceivedField3 = "Pay received"
    val taxField4 = "UK tax taken from pay"
    val payrollIdField: String = "Payroll ID"
    val offPayrollWorkingSubHeading: String = "Off-payroll working (IR35)"


    val employerHiddenText: String = "Change the name of this employer"
    val payeRefHiddenText: String = "Change the PAYE reference number"
    val startDateHiddenText: String = "Change the employment start date"
    val endDateHiddenText: String = "Change the employment end date"
    val payrollIdHiddenText: String = "Change the payroll ID for this employment"
    val payReceivedHiddenText: String = "Change the amount of pay received"
    val totalTaxToDateHiddenText: String = "Change the amount of UK tax taken from pay"
    val offPayrollWorkingHiddenText: String = "Change off-payroll working (IR35) status"
    val employmentStartDateAddHiddenText = "Add employment start date"


    val returnToEmployerText: String = "Return to employer"
    val employmentStartDate = "21 April 2019"
    val employmentEndDate = s"11 March ${taxYearEOY - 1}"
    val yes = "Yes"
    val no = "No"
    val notProvided = "Not provided"
  }

  object ExpectedIndividual extends SpecificExpectedResults {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "Your employment details are based on the information we already hold about you."
    val expectedInsetText = s"You cannot update your employment details until 6 April $taxYear."
    val employeeFieldName7 = "Payments not on your P60"
    val employeeFieldName8 = "Amount of payments not on your P60"
    val changePayReceivedHiddenText: String = "Change the amount of pay received"
    val offPayrollWorkingField: String = "Do you agree?"
    val offPayrollWorkingParagraph: String = "ABC Digital Ltd treated you as an employee for tax purposes and deducted Income Tax and National Insurance contributions from your fees"


    val didYouLeaveHiddenText: String = "Change if you left the employer in this tax year"
  }

  object ExpectedAgent extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val changePayReceivedHiddenText: String = "Change the amount of pay received"
    val offPayrollWorkingField: String = "Does your client agree?"
    val offPayrollWorkingParagraph: String = "ABC Digital Ltd treated your client as an employee for tax purposes and deducted Income Tax and National Insurance contributions from their fees"
    val didYouLeaveHiddenText: String = "Change if your client left the employer in this tax year"
  }

  object CommonExpectedCY extends CommonExpectedResults {

    val expectedCaption: Int => String = (taxYear: Int) => s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val changeLinkExpected = "Newid"
    val continueButtonText = "Cadw ac yn eich blaen"
    val employerNameField1 = "Cyflogwr"
    val employmentStartDateField = "Dyddiad dechrau’r gyflogaeth"
    val didYouLeaveEmployerField = "Wedi gadael y cyflogwr"
    val employmentEndDateField = "Dyddiad dod i ben y gyflogaeth"
    val payeReferenceField2 = "Cyfeirnod TWE"
    val payReceivedField3 = "Tal a gafwyd"
    val taxField4 = "Treth y DU a dynnwyd o’r cyflog"
    val payrollIdField: String = "ID y gyflogres"
    val offPayrollWorkingSubHeading: String = "Gweithio oddi ar y gyflogres (IR35)"
    val employmentStartDateAddHiddenText = "Ychwanegu dyddiad dechrau’r gyflogaeth"


    val employerHiddenText: String = "Newid enw’r cyflogwr hwn"
    val payeRefHiddenText: String = "Newid y cyfeirnod TWE"
    val startDateHiddenText: String = "Newid dyddiad dechrau’r gyflogaeth"
    val endDateHiddenText: String = "Newid dyddiad dod i ben y gyflogaeth"
    val payrollIdHiddenText: String = "Newid ID y gyflogres ar gyfer y gyflogaeth hon"
    val payReceivedHiddenText: String = "Newid swm y cyflog a gafwyd"
    val totalTaxToDateHiddenText: String = "Newid swm y dreth yn y DU a ddidynnwyd oddi wrth y cyflog"
    val offPayrollWorkingHiddenText: String = "Newid statws gweithio oddi ar y gyflogres (IR35)"

    val returnToEmployerText: String = "Dychwelyd at y cyflogwr"
    val employmentStartDate = "21 Ebrill 2019"
    val employmentEndDate = s"11 Mawrth ${taxYearEOY - 1}"
    val yes = "Iawn"
    val no = "Na"
    val notProvided = "Heb ddarparu"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Gwiriwch eich manylion cyflogaeth"
    val expectedTitle = "Gwiriwch eich manylion cyflogaeth"
    val expectedContent = "Mae’ch manylion cyflogaeth yn seiliedig ar yr wybodaeth sydd eisoes gennym amdanoch."
    val expectedInsetText = s"Ni allwch ddiweddaru’ch manylion cyflogaeth tan 6 Ebrill $taxYear."
    val employeeFieldName7 = "Taliadau sydd ddim ar eich P60"
    val employeeFieldName8 = "Swm y taliadau sydd ddim ar eich P60"
    val changePayReceivedHiddenText: String = "Newid swm y cyflog a gafwyd"
    val offPayrollWorkingField: String = "A ydych yn cytuno?"
    val offPayrollWorkingParagraph: String = "Gwnaeth ABC Digital Ltd eich trin fel cyflogai at ddibenion treth, a didynnodd Treth Incwm a chyfraniadau Yswiriant Gwladol o’ch ffioedd"
    val didYouLeaveHiddenText: String = "Newidiwch os gwnaethoch adael y cyflogwr yn ystod y flwyddyn dreth hon"

  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Gwiriwch fanylion cyflogaeth eich cleient"
    val expectedTitle = "Gwiriwch fanylion cyflogaeth eich cleient"
    val expectedContent = "Mae manylion cyflogaeth eich cleient yn seiliedig ar yr wybodaeth sydd eisoes gennym amdano."
    val expectedInsetText = s"Ni allwch ddiweddaru manylion cyflogaeth eich cleient tan 6 Ebrill $taxYear."
    val changePayReceivedHiddenText: String = "Newid swm y cyflog a gafwyd"
    val offPayrollWorkingField: String = "A yw’ch cleient yn cytuno?"
    val offPayrollWorkingParagraph: String = "Gwnaeth ABC Digital Ltd drin eich cleient fel cyflogai at ddibenion treth, a didynnodd Treth Incwm a chyfraniadau Yswiriant Gwladol o’i ffioedd"
    val didYouLeaveHiddenText: String = "Newidiwch os gwnaeth eich cleient adael y cyflogwr yn ystod y flwyddyn dreth hon"

  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpected, Some(ExpectedIndividual)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpected, Some(ExpectedAgent)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val underTest = inject[CheckEmploymentDetailsView]

  userScenarios.foreach { userScenario =>
    s"Language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      import Selectors._
      val specific = userScenario.specificExpectedResults.get
      val common = userScenario.commonExpectedResults

      "for in year return a fully populated page when all the fields are populated" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(anEmploymentDetailsViewModel, taxYear = taxYear, isInYear = true)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(1))
        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(2))
        textOnPageCheck(common.employmentStartDate, summaryListRowFieldValueSelector(2))
        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.yes, summaryListRowFieldValueSelector(3))
        textOnPageCheck(common.employmentEndDateField, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.employmentEndDate, summaryListRowFieldValueSelector(4))
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(5))
        textOnPageCheck(ContentValues.payeRef, summaryListRowFieldValueSelector(5))
        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.payrollId, summaryListRowFieldValueSelector(6))
        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(7))
        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(8))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(8))

        textOnPageCheck(common.offPayrollWorkingSubHeading, offPayrollWorkingSubHeading)
        textOnPageCheck(specific.offPayrollWorkingParagraph, offPayrollWorkingParagraph)

        textOnPageCheck(specific.offPayrollWorkingField, opwsummaryListRowFieldNameSelector(6))
        textOnPageCheck(common.no, opwsummaryListRowValuedNameSelector(6),"for off payroll working")

        buttonCheck(userScenario.commonExpectedResults.returnToEmployerText, Selectors.returnToEmployerSelector)
      }

      "render the in year page with the minimum data items returned" when {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(anEmploymentDetailsViewModel.copy(
          employerRef = None,
          payrollId = None,
          didYouLeaveQuestion = Some(false),
          startDate = None,
          cessationDate = None,
        ), taxYear = taxYear, isInYear = true)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYear))
        textOnPageCheck(specific.expectedContent, contentTextSelector)
        textOnPageCheck(specific.expectedInsetText, insetTextSelector)
        welshToggleCheck(userScenario.isWelsh)
        textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(1))
        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(2))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(2), "for start date")
        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.no, summaryListRowFieldValueSelector(3))
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4), "for payee reference")
        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5), "for payroll")
        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(6))
        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(7))

        textOnPageCheck(specific.offPayrollWorkingField, opwsummaryListRowFieldNameSelector(10))
        textOnPageCheck(common.no, opwsummaryListRowValuedNameSelector(10), "for off payroll working")

      }

      "for end of year return a fully populated page, with change links, when all the fields are populated" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(anEmploymentDetailsViewModel, taxYear = taxYearEOY, isInYear = false)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYearEOY))
        textOnPageCheck(specific.expectedContent, contentTextSelector)
        welshToggleCheck(userScenario.isWelsh)
        textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(1))
        linkCheck(s"${common.changeLinkExpected} ${common.employerHiddenText}", cyaChangeLink(1), EmployerNameController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(1)))
        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(2))
        textOnPageCheck(common.employmentStartDate, summaryListRowFieldValueSelector(2))
        linkCheck(s"${common.changeLinkExpected} ${common.startDateHiddenText}", cyaChangeLink(2),
          EmployerStartDateController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(2)))
        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.yes, summaryListRowFieldValueSelector(3))
        linkCheck(s"${common.changeLinkExpected} ${specific.didYouLeaveHiddenText}", cyaChangeLink(3),
          DidYouLeaveEmployerController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(3)))
        textOnPageCheck(common.employmentEndDateField, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.employmentEndDate, summaryListRowFieldValueSelector(4))
        linkCheck(s"${common.changeLinkExpected} ${common.endDateHiddenText}", cyaChangeLink(4),
          EmployerEndDateController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(4)))
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(5))
        textOnPageCheck(ContentValues.payeRef, summaryListRowFieldValueSelector(5))
        linkCheck(s"${common.changeLinkExpected} ${common.payeRefHiddenText}", cyaChangeLink(5), PayeRefController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(5)))
        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.payrollId, summaryListRowFieldValueSelector(6))
        linkCheck(s"${common.changeLinkExpected} ${common.payrollIdHiddenText}", cyaChangeLink(6), EmployerPayrollIdController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(6)))
        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(7))
        linkCheck(s"${common.changeLinkExpected} ${specific.changePayReceivedHiddenText}",
          cyaChangeLink(7), EmployerPayAmountController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(7)))
        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(8))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(8))
        linkCheck(s"${common.changeLinkExpected} ${common.totalTaxToDateHiddenText}", cyaChangeLink(8), EmploymentTaxController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(8)))

        textOnPageCheck(specific.offPayrollWorkingField, opwsummaryListRowFieldNameSelector(10))
        textOnPageCheck(common.no, opwsummaryListRowValuedNameSelector(10), "for off payroll working")
        linkCheck(s"${common.changeLinkExpected} ${common.offPayrollWorkingHiddenText}", opwCyaChangeLink(1), EmployerOffPayrollWorkingController.show(taxYearEOY).url,
          Some(opwHiddenChangeLink(1)))
      }

      "for end of year return a fully populated page, with change links when minimum data is returned" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(anEmploymentDetailsViewModel.copy(
          employerRef = None,
          payrollId = None,
          didYouLeaveQuestion = Some(false),
          startDate = None,
          cessationDate = None,
        ), taxYear = taxYearEOY, isInYear = false)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYearEOY))
        textOnPageCheck(specific.expectedContent, contentTextSelector)
        welshToggleCheck(userScenario.isWelsh)
        textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(1))
        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(2))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(2), "for start date")
        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.no, summaryListRowFieldValueSelector(3))
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4), "for payee reference")
        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5), "for payroll")
        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(6))
        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(7))

        textOnPageCheck(specific.offPayrollWorkingField, opwsummaryListRowFieldNameSelector(10))
        textOnPageCheck(common.no, opwsummaryListRowValuedNameSelector(10),"for off payroll working")
      }

      "render the end of year page with no notification banner when there are data items missing" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(anEmploymentDetailsViewModel.copy(
          employerRef = None,
          startDate = None,
          payrollId = None,
          didYouLeaveQuestion = Some(false),
          taxablePayToDate = None,
          totalTaxToDate = None,
          isUsingCustomerData = true,
          offPayrollWorkingStatus = Some(false)
        ), taxYear = taxYearEOY, isInYear = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYearEOY))
        welshToggleCheck(userScenario.isWelsh)
        textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(anEmploymentDetailsViewModel.employerName, summaryListRowFieldValueSelector(1))
        linkCheck(s"${common.changeLinkExpected} ${common.employerHiddenText}", cyaChangeLink(1), EmployerNameController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(2))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(2), "employment start date")
        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.no, summaryListRowFieldValueSelector(3))
        linkCheck(s"${common.changeLinkExpected} ${specific.didYouLeaveHiddenText}",
          cyaChangeLink(3), DidYouLeaveEmployerController.show(taxYearEOY, employmentId).url)
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4), "paye ref")

        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5), "payroll id")

        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(6), "pay received")

        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(7), "tax taken from pay")

        textOnPageCheck(specific.offPayrollWorkingField, opwsummaryListRowFieldNameSelector(10))
        textOnPageCheck(common.no, opwsummaryListRowValuedNameSelector(10), "for off payroll working")
        linkCheck(s"${common.changeLinkExpected} ${common.offPayrollWorkingHiddenText}", opwCyaChangeLink(10), EmployerOffPayrollWorkingController.show(taxYearEOY).url,
          Some(opwHiddenChangeLink(10)))

        buttonCheck(common.continueButtonText, saveAndContinueButtonSelector)
        formPostLinkCheck(CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }
    }
  }
}