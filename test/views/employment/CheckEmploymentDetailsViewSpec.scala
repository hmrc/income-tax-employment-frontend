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

package views.employment

import controllers.details.routes._
import controllers.employment.routes._
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
    val contentTextSelector = "#main-content > div > div > p"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val returnToEmploymentSummarySelector = "#returnToEmploymentSummaryBtn"
    val returnToEmployerSelector = "#returnToEmployerBtn"

    def summaryListRowFieldNameSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dt"

    def summaryListRowFieldValueSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__value"

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
    val didYouLeaveHiddenText: String

    val changePayReceivedHiddenText: String
    val employmentStartDateAddHiddenText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
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

    val employerHiddenText: String
    val payeRefHiddenText: String
    val startDateHiddenText: String
    val payrollIdHiddenText: String
    val payReceivedHiddenText: String
    val totalTaxToDateHiddenText: String

    val returnToEmployerText: String
    val employmentStartDate: String
    val employmentEndDate: String
    val employmentDates: String
    val didYouLeaveNo: String
    val didYouLeaveYes: String
    val notProvided: String
  }

  object ContentValues {
    val employerName = "maggie"
    val payeRef = "223/AB12399"
    val payReceived = "£100"
    val taxTakenFromPay = "£200"
    val payrollId = "12345678"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val employerNameField1 = "Employer"
    val employmentStartDateField1 = "Employment start date"
    val didYouLeaveEmployerField = "Left employer"
    val employmentDatesField = "Employment dates"
    val payeReferenceField2 = "PAYE reference"
    val payReceivedField3 = "Pay received"
    val taxField4 = "UK tax taken from pay"
    val payrollIdField: String = "Payroll ID"

    val employerHiddenText: String = "Change the name of this employer"
    val payeRefHiddenText: String = "Change the PAYE reference number"
    val startDateHiddenText: String = "Change the employment start date"
    val payrollIdHiddenText: String = "Change the payroll ID for this employment"
    val payReceivedHiddenText: String = "Change the amount of pay received"
    val totalTaxToDateHiddenText: String = "Change the amount of UK tax taken from pay"


    val returnToEmployerText: String = "Return to employer"
    val employmentStartDate = "21 April 2019"
    val employmentEndDate = s"11 March ${taxYearEOY - 1}"
    val employmentDates = s"$employmentStartDate to $employmentEndDate"
    val didYouLeaveYes = "Yes"
    val didYouLeaveNo = "No"
    val notProvided = "Not provided"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val changeLinkExpected = "Newid"
    val continueButtonText = "Cadw ac yn eich blaen"
    val employerNameField1 = "Cyflogwr"
    val employmentStartDateField1 = "Dyddiad dechrau’r gyflogaeth"
    val didYouLeaveEmployerField = "Wedi gadael y cyflogwr"
    val employmentDatesField = "Dyddiadau cyflogaeth"
    val payeReferenceField2 = "Cyfeirnod TWE"
    val payReceivedField3 = "Tal a gafwyd"
    val taxField4 = "Treth y DU a dynnwyd o’r cyflog"
    val payrollIdField: String = "ID y gyflogres"

    val employerHiddenText: String = "Newid enw’r cyflogwr hwn"
    val payeRefHiddenText: String = "Newid y cyfeirnod TWE"
    val startDateHiddenText: String = "Newid dyddiad dechrau’r gyflogaeth"
    val payrollIdHiddenText: String = "Newid ID y gyflogres ar gyfer y gyflogaeth hon"
    val payReceivedHiddenText: String = "Newid swm y cyflog a gafwyd"
    val totalTaxToDateHiddenText: String = "Newid swm y dreth yn y DU a ddidynnwyd oddi wrth y cyflog"

    val returnToEmployerText: String = "Dychwelyd at y cyflogwr"
    val employmentStartDate = "21 Ebrill 2019"
    val employmentEndDate = s"11 Mawrth ${taxYearEOY - 1}"
    val employmentDates = s"$employmentStartDate i $employmentEndDate"
    val didYouLeaveYes = "Iawn"
    val didYouLeaveNo = "Na"
    val notProvided = "Heb ddarparu"
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
    val changePayReceivedHiddenText: String = "Change the amount of pay received"
    val employmentStartDateAddHiddenText = "Add employment start date"

    val didYouLeaveHiddenText: String = "Change if you left the employer in this tax year"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "Your client’s employment details are based on the information we already hold about them."
    val expectedInsetText = s"You cannot update your client’s employment details until 6 April $taxYear."
    val changeEmploymentStartDateHiddenText: String => String = (employerName: String) => s"Change your client’s start date for $employerName"
    val changeEmploymentDatesHiddenText = "Change your client’s employment dates"
    val changePayReceivedHiddenText: String = "Change the amount of pay received"
    val employmentStartDateAddHiddenText = "Add employment start date"

    val didYouLeaveHiddenText: String = "Change if your client left the employer in this tax year"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Gwiriwch eich manylion cyflogaeth"
    val expectedTitle = "Gwiriwch eich manylion cyflogaeth"
    val expectedContent = "Mae’ch manylion cyflogaeth yn seiliedig ar yr wybodaeth sydd eisoes gennym amdanoch."
    val expectedInsetText = s"Ni allwch ddiweddaru’ch manylion cyflogaeth tan 6 Ebrill $taxYear."
    val changeEmploymentStartDateHiddenText: String => String = (employerName: String) => s"Newidiwch eich dyddiad dechrau ar gyfer $employerName"
    val changeEmploymentDatesHiddenText = "Newidiwch ddyddiadau’ch cyflogaeth chi"
    val changePayReceivedHiddenText: String = "Newid swm y cyflog a gafwyd"
    val employmentStartDateAddHiddenText = "Ychwanegu dyddiad dechrau’r gyflogaeth"

    val didYouLeaveHiddenText: String = "Newidiwch os gwnaethoch adael y cyflogwr yn ystod y flwyddyn dreth hon"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Gwiriwch fanylion cyflogaeth eich cleient"
    val expectedTitle = "Gwiriwch fanylion cyflogaeth eich cleient"
    val expectedContent = "Mae manylion cyflogaeth eich cleient yn seiliedig ar yr wybodaeth sydd eisoes gennym amdano."
    val expectedInsetText = s"Ni allwch ddiweddaru manylion cyflogaeth eich cleient tan 6 Ebrill $taxYear."
    val employeeFieldName7 = "Taliadau sydd ddim ar P60 eich cleient"
    val changeEmploymentStartDateHiddenText: String => String = (employerName: String) => s"Newidiwch ddyddiad dechrau eich cleient ar gyfer $employerName"
    val changeEmploymentDatesHiddenText = "Newidiwch ddyddiadau cyflogaeth eich cleient"

    val changePayReceivedHiddenText: String = "Newid swm y cyflog a gafwyd"
    val employmentStartDateAddHiddenText = "Ychwanegu dyddiad dechrau’r gyflogaeth"

    val didYouLeaveHiddenText: String = "Newidiwch os gwnaeth eich cleient adael y cyflogwr yn ystod y flwyddyn dreth hon"
  }


  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val underTest = inject[CheckEmploymentDetailsView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      import Selectors._
      val specific = userScenario.specificExpectedResults.get
      val common = userScenario.commonExpectedResults

      "for in year return a fully populated page when all the fields are populated" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(anEmploymentDetailsViewModel, taxYear = taxYear, isInYear = true)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYear))
        textOnPageCheck(specific.expectedContent, contentTextSelector)
        textOnPageCheck(specific.expectedInsetText, insetTextSelector)
        welshToggleCheck(userScenario.isWelsh)
        textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(1))
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.payeRef, summaryListRowFieldValueSelector(2))
        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.didYouLeaveYes, summaryListRowFieldValueSelector(3))
        textOnPageCheck(common.employmentDatesField, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.employmentDates, summaryListRowFieldValueSelector(4))
        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
        textOnPageCheck(ContentValues.payrollId, summaryListRowFieldValueSelector(5))
        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(6))
        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(7))
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
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(2), "for payee reference")
        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.didYouLeaveNo, summaryListRowFieldValueSelector(3))
        textOnPageCheck(common.employmentStartDateField1, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4), "for start date")
        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5), "for payroll")
        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(6))
        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(7))
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
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.payeRef, summaryListRowFieldValueSelector(2))
        linkCheck(s"${common.changeLinkExpected} ${common.payeRefHiddenText}", cyaChangeLink(2), PayeRefController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(2)))
        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.didYouLeaveYes, summaryListRowFieldValueSelector(3))
        linkCheck(s"${common.changeLinkExpected} ${specific.didYouLeaveHiddenText}", cyaChangeLink(3),
          DidYouLeaveEmployerController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(3)))
        textOnPageCheck(common.employmentDatesField, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.employmentDates, summaryListRowFieldValueSelector(4))
        linkCheck(s"${common.changeLinkExpected} ${specific.changeEmploymentDatesHiddenText}", cyaChangeLink(4),
          EmploymentDatesController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(4)))
        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
        textOnPageCheck(ContentValues.payrollId, summaryListRowFieldValueSelector(5))
        linkCheck(s"${common.changeLinkExpected} ${common.payrollIdHiddenText}", cyaChangeLink(5), EmployerPayrollIdController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(5)))
        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(6))
        linkCheck(s"${common.changeLinkExpected} ${specific.changePayReceivedHiddenText}",
          cyaChangeLink(6), EmployerPayAmountController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(6)))
        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(7))
        linkCheck(s"${common.changeLinkExpected} ${common.totalTaxToDateHiddenText}", cyaChangeLink(7), EmploymentTaxController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(7)))
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
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(2), "for payee reference")
        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.didYouLeaveNo, summaryListRowFieldValueSelector(3))
        textOnPageCheck(common.employmentStartDateField1, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4), "for start date")
        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5), "for payroll")
        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(6))
        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(7))
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
          isUsingCustomerData = true
        ), taxYear = taxYearEOY, isInYear = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)


        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYearEOY))
        welshToggleCheck(userScenario.isWelsh)
        textOnPageCheck(common.employerNameField1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(anEmploymentDetailsViewModel.employerName, summaryListRowFieldValueSelector(1))
        linkCheck(s"${common.changeLinkExpected} ${common.employerHiddenText}", cyaChangeLink(1), EmployerNameController.show(taxYearEOY, employmentId).url)
        textOnPageCheck(common.payeReferenceField2, summaryListRowFieldNameSelector(2))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(2), "paye ref")

        textOnPageCheck(common.didYouLeaveEmployerField, summaryListRowFieldNameSelector(3))
        textOnPageCheck(common.didYouLeaveNo, summaryListRowFieldValueSelector(3))
        linkCheck(s"${common.changeLinkExpected} ${specific.didYouLeaveHiddenText}",
          cyaChangeLink(3), DidYouLeaveEmployerController.show(taxYearEOY, employmentId).url)
        textOnPageCheck(common.employmentStartDateField1, summaryListRowFieldNameSelector(4))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4), "employment start date")

        textOnPageCheck(common.payrollIdField, summaryListRowFieldNameSelector(5))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5), "payroll id")

        textOnPageCheck(common.payReceivedField3, summaryListRowFieldNameSelector(6))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(6), "pay received")

        textOnPageCheck(common.taxField4, summaryListRowFieldNameSelector(7))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(7), "tax taken from pay")

        buttonCheck(common.continueButtonText, continueButtonSelector)
        formPostLinkCheck(CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }
    }
  }
}
