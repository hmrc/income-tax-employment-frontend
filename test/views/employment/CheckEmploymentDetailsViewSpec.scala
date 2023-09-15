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
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val returnToEmploymentSummarySelector = "#returnToEmploymentSummaryBtn"
    val returnToEmployerSelector = "#returnToEmployerBtn"
    val subHeading1 = "#main-content > div > div > h2"
    val subHeading2 = "#main-content > div > div > h3"
    val subHeading3 = "#main-content > div > div > h4"
    val content = "#main-content > div > div > p"

    def summaryListRowFieldNameSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dt"

    def summaryListRowFieldValueSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd.govuk-summary-list__value"

    def cyaChangeLink(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd.govuk-summary-list__actions > a"

    def cyaHiddenChangeLink(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd.govuk-summary-list__actions > a " +
      s"> span.govuk-visually-hidden"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedContent: String
    val offPayrollWorkingStatusField: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedCaptionEOY: Int => String
    val changeLinkExpected: String
    val continueButtonText: String

    val employerNameField: String
    val payeReferenceField: String
    val employmentStartDateField: String
    val payReceivedField: String
    val taxTakenField: String

    val leftEmployerField: String

    val subHeading1: String
    val subHeading2: String
    val subHeading3: String

    val employerHiddenText: String
    val payeRefHiddenText: String
    val startDateHiddenText: String
    val payReceivedHiddenText: String
    val totalTaxToDateHiddenText: String
    val offPayrollWorkingStatusHiddenText: String
    val leftEmployerHiddenText: String

    val employmentStartDate: String
    val employmentEndDate: String
    val notProvided: String
  }

  object ContentValues {
    val employerName = "maggie"
    val payeRef = "223/AB12399"
    val payReceived = "£100"
    val taxTakenFromPay = "£200"
    val yes = "Yes"
    val no = "No"
  }

  object CommonExpected extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYearEOY} to 5 April $taxYear"
    val expectedCaptionEOY: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYearEndOfYearMinusOne} to 5 April $taxYearEOY"
    val changeLinkExpected = "Change"
    val continueButtonText = "Save and continue"
    val employerNameField = "Employer"
    val payeReferenceField = "PAYE reference"
    val employmentStartDateField = "Employment start date"
    val payReceivedField = "Pay received"
    val taxTakenField = "UK tax taken from pay"
    val leftEmployerField = "Left employer"

    val subHeading1 = "Employment details"
    val subHeading2 = "Off-payroll working (IR35)"
    val subHeading3 = "End of employment details"

    val employerHiddenText: String = "Change employment details"
    val payeRefHiddenText: String = "Change PAYE reference"
    val startDateHiddenText: String = "Change employment start date"
    val payReceivedHiddenText: String = "Change pay received"
    val totalTaxToDateHiddenText: String = "Change UK tax taken from pay"
    val offPayrollWorkingStatusHiddenText: String = "Change off-payroll working (IR35) status"
    val leftEmployerHiddenText: String = "Change left employer"

    val employmentStartDate = "21 April 2019"
    val employmentEndDate = s"11 March ${taxYearEOY - 1}"
    val notProvided = "Not provided"
  }

  object ExpectedIndividual extends SpecificExpectedResults {
    val expectedH1 = "Check your employment details"
    val expectedTitle = "Check your employment details"
    val expectedContent = "ABC Digital Ltd treated you as an employee for tax purposes and deducted Income Tax and National Insurance contributions from your fees"
    val offPayrollWorkingStatusField: String = "Do you agree?"

  }

  object ExpectedAgent extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s employment details"
    val expectedTitle = "Check your client’s employment details"
    val expectedContent = "ABC Digital Ltd treated your client as an employee for tax purposes and deducted Income Tax and National Insurance contributions from their fees"
    val offPayrollWorkingStatusField: String = "Does your client agree?"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpected, Some(ExpectedIndividual)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpected, Some(ExpectedAgent))
  )

  private val underTest = inject[CheckEmploymentDetailsView]

  userScenarios.foreach { userScenario =>
    s"Request is from an ${agentTest(userScenario.isAgent)}" should {
      import Selectors._
      val specific = userScenario.specificExpectedResults.get
      val common = userScenario.commonExpectedResults

      "for in year return a fully populated page when all the fields in employment details are populated" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val htmlFormat = underTest(anEmploymentDetailsViewModel, taxYear = taxYear, isInYear = true)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        // in year (2024) cannot fill in employment until april (notification banner) but existing data still displays so displays text but no change links

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYear))
        textOnPageCheck(common.subHeading1, subHeading1)
        textOnPageCheck(common.subHeading2, subHeading2)
        textOnPageCheck(common.subHeading3, subHeading3)
        textOnPageCheck(specific.expectedContent, content)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(common.employerNameField, summaryListRowFieldNameSelector(5, 1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(5, 1))
        textOnPageCheck(common.payeReferenceField, summaryListRowFieldNameSelector(5, 2))
        textOnPageCheck(ContentValues.payeRef, summaryListRowFieldValueSelector(5, 2))
        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(5, 3))
        textOnPageCheck(common.employmentStartDate, summaryListRowFieldValueSelector(5, 3))
        textOnPageCheck(common.payReceivedField, summaryListRowFieldNameSelector(5, 4))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(5, 4))
        textOnPageCheck(common.taxTakenField, summaryListRowFieldNameSelector(5, 5))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(5, 5))
        textOnPageCheck(specific.offPayrollWorkingStatusField, summaryListRowFieldNameSelector(8, 1))
        textOnPageCheck(ContentValues.no, summaryListRowFieldValueSelector(8, 1), "for off-payroll working status")
        textOnPageCheck(common.leftEmployerField, summaryListRowFieldNameSelector(10, 1))
        textOnPageCheck(ContentValues.yes, summaryListRowFieldValueSelector(10, 1), "for left employer")
      }


      "render the in year page with the minimum data items returned" when {
        import Selectors._
        val specific = userScenario.specificExpectedResults.get
        val common = userScenario.commonExpectedResults
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(anEmploymentDetailsViewModel.copy(
          payrollId = None,
          employerRef = None,
          startDate = None,
          taxablePayToDate = None,
          totalTaxToDate = None,
          offPayrollWorkingStatus = Some(false)
        ), taxYear = taxYear, isInYear = true)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYear))
        textOnPageCheck(specific.expectedContent, content)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(common.employerNameField, summaryListRowFieldNameSelector(5, 1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(5, 1))
        textOnPageCheck(common.payeReferenceField, summaryListRowFieldNameSelector(5, 2))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5, 2), "for PAYE ref")
        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(5, 3))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5, 3), "for start date")
        textOnPageCheck(common.payReceivedField, summaryListRowFieldNameSelector(5, 4))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5, 4), "for pay recieved")
        textOnPageCheck(common.taxTakenField, summaryListRowFieldNameSelector(5, 5))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(5, 5), "for tax taken")
        textOnPageCheck(specific.offPayrollWorkingStatusField, summaryListRowFieldNameSelector(8, 1))
        textOnPageCheck(ContentValues.no, summaryListRowFieldValueSelector(8, 1), "for off-payroll working status")
        textOnPageCheck(common.leftEmployerField, summaryListRowFieldNameSelector(10, 1))
        textOnPageCheck(ContentValues.yes, summaryListRowFieldValueSelector(10, 1), "for left employer")
      }

      "for end of year return a fully populated page, with change links, when all the fields are populated" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        import Selectors._
        val specific = userScenario.specificExpectedResults.get
        val common = userScenario.commonExpectedResults

        val htmlFormat = underTest(anEmploymentDetailsViewModel, taxYear = taxYearEOY, isInYear = false)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaptionEOY(taxYearEOY))
        textOnPageCheck(common.subHeading1, subHeading1)
        textOnPageCheck(common.subHeading2, subHeading2)
        textOnPageCheck(common.subHeading3, subHeading3)
        textOnPageCheck(specific.expectedContent, content)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(common.employerNameField, summaryListRowFieldNameSelector(4, 1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(4, 1))
        linkCheck(s"${common.changeLinkExpected} ${common.employerHiddenText}", cyaChangeLink(4,1), EmployerNameController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(4,1)))

        textOnPageCheck(common.payeReferenceField, summaryListRowFieldNameSelector(4, 2))
        textOnPageCheck(ContentValues.payeRef, summaryListRowFieldValueSelector(4, 2))
        linkCheck(s"${common.changeLinkExpected} ${common.payeRefHiddenText}", cyaChangeLink(4,2), PayeRefController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(4,2)))

        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(4, 3))
        textOnPageCheck(common.employmentStartDate, summaryListRowFieldValueSelector(4, 3))
        linkCheck(s"${common.changeLinkExpected} ${common.startDateHiddenText}", cyaChangeLink(4,3), EmployerStartDateController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(4,3)))

        textOnPageCheck(common.payReceivedField, summaryListRowFieldNameSelector(4, 4))
        textOnPageCheck(ContentValues.payReceived, summaryListRowFieldValueSelector(4, 4))
        linkCheck(s"${common.changeLinkExpected} ${common.payReceivedHiddenText}", cyaChangeLink(4,4), EmployerPayAmountController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(4,4)))

        textOnPageCheck(common.taxTakenField, summaryListRowFieldNameSelector(4, 5))
        textOnPageCheck(ContentValues.taxTakenFromPay, summaryListRowFieldValueSelector(4, 5))
        linkCheck(s"${common.changeLinkExpected} ${common.totalTaxToDateHiddenText}", cyaChangeLink(4,5), EmploymentTaxController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(4,5)))


        "for in year return a fully populated page when off payroll work status field is populated" which {
          textOnPageCheck(specific.offPayrollWorkingStatusField, summaryListRowFieldNameSelector(7, 1))
          textOnPageCheck(ContentValues.no, summaryListRowFieldValueSelector(7, 1))
          linkCheck(s"${common.changeLinkExpected} ${common.offPayrollWorkingStatusHiddenText}", cyaChangeLink(7,1), EmployerOffPayrollWorkingController.show(taxYearEOY).url,
            Some(cyaHiddenChangeLink(7,1)))

        }

        "for in year return a fully populated page when left employer field is populated" which {
          textOnPageCheck(common.leftEmployerField, summaryListRowFieldNameSelector(9, 1))
          textOnPageCheck(ContentValues.yes, summaryListRowFieldValueSelector(9, 1))
          linkCheck(s"${common.changeLinkExpected} ${common.leftEmployerHiddenText}", cyaChangeLink(9,1), DidYouLeaveEmployerController.show(taxYearEOY, employmentId).url,
            Some(cyaHiddenChangeLink(9,1)))

        }

      }

      "for end of year return a fully populated page, with change links when minimum data is returned" when {
        import Selectors._
        val specific = userScenario.specificExpectedResults.get
        val common = userScenario.commonExpectedResults
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(anEmploymentDetailsViewModel.copy(
          payrollId = None,
          employerRef = None,
          startDate = None,
          taxablePayToDate = None,
          totalTaxToDate = None,
          offPayrollWorkingStatus = Some(false)
        ), taxYear = taxYearEOY, isInYear = false)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaptionEOY(taxYearEOY))
        textOnPageCheck(specific.expectedContent, content)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(common.employerNameField, summaryListRowFieldNameSelector(4, 1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(4, 1))
        textOnPageCheck(common.payeReferenceField, summaryListRowFieldNameSelector(4, 2))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4, 2), "for PAYE ref")
        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(4, 3))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4, 3), "for start date")
        textOnPageCheck(common.payReceivedField, summaryListRowFieldNameSelector(4, 4))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4, 4), "for pay recieved")
        textOnPageCheck(common.taxTakenField, summaryListRowFieldNameSelector(4, 5))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4, 5), "for tax taken")
        textOnPageCheck(specific.offPayrollWorkingStatusField, summaryListRowFieldNameSelector(7, 1))
        textOnPageCheck(ContentValues.no, summaryListRowFieldValueSelector(7, 1), "for off-payroll working status")
        textOnPageCheck(common.leftEmployerField, summaryListRowFieldNameSelector(9, 1))
        textOnPageCheck(ContentValues.yes, summaryListRowFieldValueSelector(9, 1), "for left employer")
      }

      "render the end of year page with no notification banner when there are data items missing" when {
        import Selectors._
        val specific = userScenario.specificExpectedResults.get
        val common = userScenario.commonExpectedResults
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(anEmploymentDetailsViewModel.copy(
          payrollId = None,
          employerRef = None,
          startDate = None,
          taxablePayToDate = None,
          totalTaxToDate = None,
          offPayrollWorkingStatus = Some(false),
          isUsingCustomerData = true
        ), taxYear = taxYearEOY, isInYear = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaptionEOY(taxYearEOY))
        textOnPageCheck(specific.expectedContent, content)
        welshToggleCheck(userScenario.isWelsh)

        textOnPageCheck(common.employerNameField, summaryListRowFieldNameSelector(4, 1))
        textOnPageCheck(ContentValues.employerName, summaryListRowFieldValueSelector(4, 1))
        linkCheck(s"${common.changeLinkExpected} ${common.employerHiddenText}", cyaChangeLink(4,1), EmployerNameController.show(taxYearEOY, employmentId).url, Some(cyaHiddenChangeLink(4,1)))

        textOnPageCheck(common.payeReferenceField, summaryListRowFieldNameSelector(4, 2))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4, 2), "for PAYE ref")
        textOnPageCheck(common.employmentStartDateField, summaryListRowFieldNameSelector(4, 3))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4, 3), "for start date")
        textOnPageCheck(common.payReceivedField, summaryListRowFieldNameSelector(4, 4))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4, 4), "for pay recieved")
        textOnPageCheck(common.taxTakenField, summaryListRowFieldNameSelector(4, 5))
        textOnPageCheck(common.notProvided, summaryListRowFieldValueSelector(4, 5), "for tax taken")

        textOnPageCheck(specific.offPayrollWorkingStatusField, summaryListRowFieldNameSelector(7, 1))
        textOnPageCheck(ContentValues.no, summaryListRowFieldValueSelector(7, 1), "for off-payroll working status")
        linkCheck(s"${common.changeLinkExpected} ${common.offPayrollWorkingStatusHiddenText}", cyaChangeLink(7, 1), EmployerOffPayrollWorkingController.show(taxYearEOY).url,
          Some(cyaHiddenChangeLink(7, 1)))

        textOnPageCheck(common.leftEmployerField, summaryListRowFieldNameSelector(9, 1))
        textOnPageCheck(ContentValues.yes, summaryListRowFieldValueSelector(9, 1), "for left employer")
        linkCheck(s"${common.changeLinkExpected} ${common.leftEmployerHiddenText}", cyaChangeLink(9, 1), DidYouLeaveEmployerController.show(taxYearEOY, employmentId).url,
          Some(cyaHiddenChangeLink(9, 1)))

        buttonCheck(common.continueButtonText, continueButtonSelector)
        formPostLinkCheck(CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url, continueButtonFormSelector)
      }

    }
    }
}