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

package views.details

import controllers.details.routes.EmployerStartDateController
import forms.details.DateForm
import models.AuthorisationRequest
import models.details.EmploymentDetails
import models.employment.DateFormData
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.benefits.pages.EmployerStartDatePageBuilder.anEmployerStartDatePage
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.ViewUtils.dateFormatter
import views.html.details.EmployerStartDateView

import java.time.LocalDate

class EmployerStartDateViewSpec extends ViewUnitTest {

  private val nino: String = anEmploymentUserData.nino
  private val mtditid: String = anEmploymentUserData.mtdItId
  private val sessionId: String = anEmploymentUserData.sessionId
  private val employerName: String = anEmploymentUserData.employment.employmentDetails.employerName
  private val employmentStartDay: String = "01"
  private val employmentStartMonth: String = "01"
  private val employmentStartYear: String = s"${taxYearEOY - 1}"
  private val employmentEndDate = Some(LocalDate.of(taxYearEOY - 2, 1, 1))
  private val employmentId: String = "employmentId"
  private val dayInputName = "amount-day"
  private val monthInputName = "amount-month"
  private val yearInputName = "amount-year"

  object Selectors {
    val daySelector: String = "#amount-day"
    val monthSelector: String = "#amount-month"
    val yearSelector: String = "#amount-year"
    val forExampleSelector: String = "#amount-hint"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedErrorTitle: String
    val emptyDayError: String
    val emptyMonthError: String
    val emptyYearError: String
    val emptyDayYearError: String
    val emptyMonthYearError: String
    val emptyDayMonthError: String
    val emptyAllError: String
    val invalidDateError: String
    val mustBeAfter1900Error: String
    val mustBeBeforeEndDateError: LocalDate => String
    val mustBeBeforeEndOfTaxYearError: String
    val mustHave4DigitYear: String
  }

  trait CommonExpectedResults {
    val expectedButtonText: String
    val forExample: String
  }

  object ExpectedIndividual extends SpecificExpectedResults {
    val expectedH1 = s"When did you start working at $employerName?"
    val expectedErrorTitle = s"Error: $expectedH1"
    val emptyDayError = s"The date you started working at $employerName must include a day"
    val emptyMonthError = s"The date you started working at $employerName must include a month"
    val emptyYearError = s"The date you started working at $employerName must include a year"
    val emptyDayYearError = s"The date you started working at $employerName must include a day and year"
    val emptyMonthYearError = s"The date you started working at $employerName must include a month and year"
    val emptyDayMonthError = s"The date you started working at $employerName must include a day and month"
    val emptyAllError = s"Enter the date you started working at $employerName"
    val invalidDateError = s"The date you started working at $employerName must be a real date"
    val mustBeAfter1900Error = s"The date you started working at $employerName must be after 1 January 1900"
    val mustBeBeforeEndDateError: LocalDate => String = (date: LocalDate) => s"The date you started working at $employerName must be before the date you left, ${dateFormatter(date)}"
    val mustBeBeforeEndOfTaxYearError = s"The date you started working at $employerName must be before 6 April $taxYearEOY"
    val mustHave4DigitYear = s"The year you started working at $employerName must include 4 digits"
  }

  object ExpectedAgent extends SpecificExpectedResults {
    val expectedH1 = s"When did your client start working at $employerName?"
    val expectedErrorTitle = s"Error: $expectedH1"
    val emptyDayError = s"The date your client started working at $employerName must include a day"
    val emptyMonthError = s"The date your client started working at $employerName must include a month"
    val emptyYearError = s"The date your client started working at $employerName must include a year"
    val emptyDayYearError = s"The date your client started working at $employerName must include a day and year"
    val emptyMonthYearError = s"The date your client started working at $employerName must include a month and year"
    val emptyDayMonthError = s"The date your client started working at $employerName must include a day and month"
    val emptyAllError = s"Enter the date your client started working at $employerName"
    val invalidDateError = s"The date your client started working at $employerName must be a real date"
    val mustBeAfter1900Error = s"The date your client started working at $employerName must be after 1 January 1900"
    val mustBeBeforeEndDateError: LocalDate => String = (date: LocalDate) => s"The date your client started working at $employerName must be before the date they left, ${dateFormatter(date)}"
    val mustBeBeforeEndOfTaxYearError = s"The date your client started working at $employerName must be before 6 April $taxYearEOY"
    val mustHave4DigitYear = s"The year your client started working at $employerName must include 4 digits"
  }
  object CommonExpected extends CommonExpectedResults {
    val expectedButtonText = "Continue"
    val forExample = s"For example, 23 11 $taxYearEOY"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpected, Some(ExpectedIndividual)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpected, Some(ExpectedAgent)),
  )

  object CyaModel {
    val cya: EmploymentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = true, hasPriorStudentLoans = true,
      EmploymentCYAModel(
        EmploymentDetails(employerName, startDate = Some(s"${taxYearEOY - 1}-01-01"), currentDataIsHmrcHeld = false),
        None
      )
    )
  }

  private val dateForm = DateForm.dateForm()
  private val underTest = inject[EmployerStartDateView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._

    s"Request is from an ${agentTest(userScenario.isAgent)}" should {
      val page = anEmployerStartDatePage.copy(isAgent = userScenario.isAgent)
      "render the 'start date' page with an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(page)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedH1, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
        textOnPageCheck(forExample, forExampleSelector)
        inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
        inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
        inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the 'start date' page with the correct content and the date prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val pageModel = page.copy(form = dateForm.fill(DateFormData(employmentStartDay, employmentStartMonth, employmentStartYear)))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedH1, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
        textOnPageCheck(forExample, forExampleSelector)
        inputFieldValueCheck(dayInputName, Selectors.daySelector, employmentStartDay)
        inputFieldValueCheck(monthInputName, Selectors.monthSelector, employmentStartMonth)
        inputFieldValueCheck(yearInputName, Selectors.yearSelector, employmentStartYear)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the 'start date' page with an error" when {
        "the day is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> employmentStartYear,
              DateForm.month -> employmentStartMonth,
              DateForm.day -> ""))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, employmentStartMonth)
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, employmentStartYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyDayError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyDayError, Some("amount"))
        }

        "the month is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> employmentStartYear,
              DateForm.month -> "",
              DateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, employmentStartDay)
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, employmentStartYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyMonthError, Selectors.monthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyMonthError, Some("amount"))
        }

        "the year is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> "",
              DateForm.month -> employmentStartMonth,
              DateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "01")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyYearError, Selectors.yearSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyYearError, Some("amount"))
        }

        "the day and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> employmentStartYear,
              DateForm.month -> "",
              DateForm.day -> ""))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, s"${taxYearEOY - 1}")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyDayMonthError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyDayMonthError, Some("amount"))
        }

        "the day and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> "",
              DateForm.month -> employmentStartMonth,
              DateForm.day -> ""))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "01")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyDayYearError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyDayYearError, Some("amount"))
        }

        "the year and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> "",
              DateForm.month -> "",
              DateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyMonthYearError, Selectors.monthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyMonthYearError, Some("amount"))
        }

        "the day, month and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> "",
              DateForm.month -> "",
              DateForm.day -> ""))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyAllError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyAllError, Some("amount"))
        }

        "the day is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> employmentStartYear,
              DateForm.month -> employmentStartMonth,
              DateForm.day -> "abc"))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "abc")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "01")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, s"${taxYearEOY - 1}")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError, Some("amount"))
        }

        "the month is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> employmentStartYear,
              DateForm.month -> "abc",
              DateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "abc")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, s"${taxYearEOY - 1}")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError, Some("amount"))
        }

        "the year is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> "abc",
              DateForm.month -> employmentStartMonth,
              DateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "01")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, "abc")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError, Some("amount"))
        }

        "the date is an invalid date i.e. month is set to 13" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> employmentStartYear,
              DateForm.month -> "13",
              DateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "13")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, s"${taxYearEOY - 1}")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError, Some("amount"))
        }

        "the date is not after 1st January 1900" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> "1900",
              DateForm.month -> "1",
              DateForm.day -> "1"))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "1")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "1")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, "1900")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.mustBeAfter1900Error, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.mustBeAfter1900Error, Some("amount"))
        }

        "the date is after 5th April" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = dateForm.bind(
            Map(DateForm.year -> taxYearEOY.toString,
              DateForm.month -> "04",
              DateForm.day -> "06"))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, employmentEndDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "06")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "04")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.mustBeBeforeEndOfTaxYearError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.mustBeBeforeEndOfTaxYearError, Some("amount"))
        }

        "the date is not before the end date" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val earlierDate = LocalDate.of(taxYearEOY, 2, 2)
          val laterDate = earlierDate.plusDays(1)
          val filledForm = dateForm.bind(
            Map(DateForm.year -> laterDate.getYear.toString,
              DateForm.month -> laterDate.getMonthValue.toString,
              DateForm.day -> laterDate.getDayOfMonth.toString))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, Some(earlierDate)))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, laterDate.getDayOfMonth.toString)
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, laterDate.getMonthValue.toString)
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, laterDate.getYear.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.mustBeBeforeEndDateError(earlierDate), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.mustBeBeforeEndDateError(earlierDate), Some("amount"))
        }

        "the year has fewer than 4 digits" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val startDate = LocalDate.of(999, 1, 1)
          val endDate = startDate.plusDays(1)
          val filledForm = dateForm.bind(
            Map(DateForm.year -> endDate.getYear.toString,
              DateForm.month -> endDate.getMonthValue.toString,
              DateForm.day -> endDate.getDayOfMonth.toString))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, Some(startDate)))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, endDate.getDayOfMonth.toString)
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, endDate.getMonthValue.toString)
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, endDate.getYear.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.mustHave4DigitYear, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.mustHave4DigitYear, Some("amount"))
        }

        "the year has more than 4 digits" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val startDate = LocalDate.of(10_000, 1, 1)
          val endDate = startDate.plusDays(1)
          val filledForm = dateForm.bind(
            Map(DateForm.year -> endDate.getYear.toString,
              DateForm.month -> endDate.getMonthValue.toString,
              DateForm.day -> endDate.getDayOfMonth.toString))
          val validatedForm = filledForm.copy(errors = DateForm.validateStartDate(filledForm.get, taxYearEOY, userScenario.isAgent, employerName, Some(startDate)))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1, isFieldSetH1 = true)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, endDate.getDayOfMonth.toString)
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, endDate.getMonthValue.toString)
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, endDate.getYear.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.mustHave4DigitYear, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.mustHave4DigitYear, Some("amount"))
        }
      }
    }
  }
}
