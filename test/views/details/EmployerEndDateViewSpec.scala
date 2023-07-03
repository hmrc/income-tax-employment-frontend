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


import forms.details.DateForm._
import forms.details.{DateForm, EmploymentDetailsFormsProvider}
import models.AuthorisationRequest
import models.employment.DateFormData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.benefits.pages.EmployerEndDatePageBuilder.anEmployerEndDatePage
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import utils.ViewUtils.dateFormatter
import views.html.details.EmployerEndDateView

import java.time.LocalDate

class EmployerEndDateViewSpec extends ViewUnitTest {

  object Selectors {
    val daySelector: String = "#amount-day"
    val monthSelector: String = "#amount-month"
    val yearSelector: String = "#amount-year"
    val forExampleSelector: String = "#amount-hint"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String => String
    val expectedH1: String => String
    val expectedErrorTitle: String => String
    val emptyDayError: String => String
    val emptyMonthError: String => String
    val emptyYearError: String => String
    val emptyDayYearError: String => String
    val emptyMonthYearError: String => String
    val emptyDayMonthError: String => String
    val emptyAllError: String => String
    val invalidDateError: String => String
    val mustBeAfterStartDateError: (String, LocalDate) => String
    val mustBeBeforeEndOfTaxYearError: String => String
    val mustHave4DigitYear: String => String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val forExample: String
  }

  object ExpectedIndividual extends SpecificExpectedResults {
    val expectedTitle: String => String = (employerName: String) => s"When did you leave $employerName?"
    val expectedH1: String => String = (employerName: String) => expectedTitle(employerName)
    val expectedErrorTitle: String => String = (employerName: String) => s"Error: ${expectedTitle(employerName)}"
    val emptyDayError: String => String = (employerName: String) => s"The date you left $employerName must include a day"
    val emptyMonthError: String => String = (employerName: String) => s"The date you left $employerName must include a month"
    val emptyYearError: String => String = (employerName: String) => s"The date you left $employerName must include a year"
    val emptyDayYearError: String => String = (employerName: String) => s"The date you left $employerName must include a day and year"
    val emptyMonthYearError: String => String = (employerName: String) => s"The date you left $employerName must include a month and year"
    val emptyDayMonthError: String => String = (employerName: String) => s"The date you left $employerName must include a day and month"
    val emptyAllError: String => String = (employerName: String) => s"Enter the date you left $employerName"
    val invalidDateError: String => String = (employerName: String) => s"The date you left $employerName must be a real date"
    val mustBeAfterStartDateError: (String, LocalDate) => String = (employerName: String, date: LocalDate) =>
      s"The date you left $employerName must be after the date you started, ${dateFormatter(date)}"
    val mustBeBeforeEndOfTaxYearError: String => String = (employerName: String) => s"The date you left $employerName must be the same as or before 5 April $taxYearEOY"
    val mustHave4DigitYear: String => String = (employerName: String) => s"The year you left $employerName must include 4 digits"
  }

  object ExpectedAgent extends SpecificExpectedResults {
    val expectedTitle: String => String = (employerName: String) => s"When did your client leave $employerName?"
    val expectedH1: String => String = (employerName: String) => expectedTitle(employerName)
    val expectedErrorTitle: String => String = (employerName: String) => s"Error: ${expectedTitle(employerName)}"
    val emptyDayError: String => String = (employerName: String) => s"The date your client left $employerName must include a day"
    val emptyMonthError: String => String = (employerName: String) => s"The date your client left $employerName must include a month"
    val emptyYearError: String => String = (employerName: String) => s"The date your client left $employerName must include a year"
    val emptyDayYearError: String => String = (employerName: String) => s"The date your client left $employerName must include a day and year"
    val emptyMonthYearError: String => String = (employerName: String) => s"The date your client left $employerName must include a month and year"
    val emptyDayMonthError: String => String = (employerName: String) => s"The date your client left $employerName must include a day and month"
    val emptyAllError: String => String = (employerName: String) => s"Enter the date your client left $employerName"
    val invalidDateError: String => String = (employerName: String) => s"The date your client left $employerName must be a real date"
    val mustBeAfterStartDateError: (String, LocalDate) => String = (employerName: String, date: LocalDate) =>
      s"The date your client left $employerName must be after the date they started, ${dateFormatter(date)}"
    val mustBeBeforeEndOfTaxYearError: String => String = (employerName: String) => s"The date your client left $employerName must be the same as or before 5 April $taxYearEOY"
    val mustHave4DigitYear: String => String = (employerName: String) => s"The year your client left $employerName must include 4 digits"
  }

  object CommonExpected extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val forExample = s"For example, 23 11 $taxYearEOY"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpected, Some(ExpectedIndividual)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpected, Some(ExpectedAgent))
  )

  private val formsProvider = new EmploymentDetailsFormsProvider()
  private val dateForm = DateForm.dateForm()
  private val startDate = LocalDate.parse(anEmploymentDetails.startDate.get)
  private val underTest = inject[EmployerEndDateView]

  userScenarios.foreach { userScenario =>
    s"Request is from an ${agentTest(userScenario.isAgent)}" should {
      val page = anEmployerEndDatePage.copy(isAgent = userScenario.isAgent)
      "render page with an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(page)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        inputFieldValueCheck(day, Selectors.daySelector, "")
        inputFieldValueCheck(month, Selectors.monthSelector, "")
        inputFieldValueCheck(year, Selectors.yearSelector, "")
      }

      "render page with prefilled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = page.copy(form = dateForm.fill(DateFormData("1", "1", taxYearEOY.toString)))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        inputFieldValueCheck(day, Selectors.daySelector, "1")
        inputFieldValueCheck(month, Selectors.monthSelector, "1")
        inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)

      }

      "render the 'start date' page with an error" which {
        "the day is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "", amountMonth = "1", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyDayError(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyDayError(pageModel.employerName), Some("amount"))
        }

        "the month is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyMonthError(pageModel.employerName), Selectors.monthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyMonthError(pageModel.employerName), Some("amount"))
        }

        "the year is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "1", amountYear = "")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, "")

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyYearError(pageModel.employerName), Selectors.yearSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyYearError(pageModel.employerName), Some("amount"))
        }

        "the day and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "", amountMonth = "", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "")
          inputFieldValueCheck(month, Selectors.monthSelector, "")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyDayMonthError(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyDayMonthError(pageModel.employerName), Some("amount"))
        }

        "the day and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "", amountMonth = "1", amountYear = "")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, "")

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyDayYearError(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyDayYearError(pageModel.employerName), Some("amount"))
        }

        "the year and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "", amountYear = "")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "")
          inputFieldValueCheck(year, Selectors.yearSelector, "")

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyMonthYearError(pageModel.employerName), Selectors.monthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyMonthYearError(pageModel.employerName), Some("amount"))
        }

        "the day, month and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "", amountMonth = "", amountYear = "")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "")
          inputFieldValueCheck(month, Selectors.monthSelector, "")
          inputFieldValueCheck(year, Selectors.yearSelector, "")

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyAllError(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyAllError(pageModel.employerName), Some("amount"))
        }

        "the day is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "abc", amountMonth = "1", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "abc")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError(pageModel.employerName), Some("amount"))
        }

        "the month is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "abc", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "abc")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError(pageModel.employerName), Some("amount"))
        }

        "the year is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "1", amountYear = "abc")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, "abc")

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError(pageModel.employerName), Some("amount"))
        }

        "the date is an invalid date i.e. month is set to 13" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "13", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "13")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError(pageModel.employerName), Some("amount"))
        }

        "the date is not before the start date" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val startDate = LocalDate.of(taxYearEOY, 1, 1)
          val formData = DateFormData(amountDay = "1", amountMonth = "1", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.mustBeAfterStartDateError(pageModel.employerName, startDate), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.mustBeAfterStartDateError(pageModel.employerName, startDate), Some("amount"))
        }

        "the date is after the tax year end" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "6", amountMonth = "4", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, "6")
          inputFieldValueCheck(month, Selectors.monthSelector, "4")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.mustBeBeforeEndOfTaxYearError(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.mustBeBeforeEndOfTaxYearError(pageModel.employerName), Some("amount"))
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
          val validatedForm = filledForm.copy(errors = DateForm.validateEndDate(filledForm.get, taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, endDate.getDayOfMonth.toString)
          inputFieldValueCheck(month, Selectors.monthSelector, endDate.getMonthValue.toString)
          inputFieldValueCheck(year, Selectors.yearSelector, endDate.getYear.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.mustHave4DigitYear(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.mustHave4DigitYear(pageModel.employerName), Some("amount"))
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
          val validatedForm = filledForm.copy(errors = DateForm.validateEndDate(filledForm.get, taxYearEOY, userScenario.isAgent, page.employerName, startDate))
          val pageModel = page.copy(form = validatedForm)
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          inputFieldValueCheck(day, Selectors.daySelector, endDate.getDayOfMonth.toString)
          inputFieldValueCheck(month, Selectors.monthSelector, endDate.getMonthValue.toString)
          inputFieldValueCheck(year, Selectors.yearSelector, endDate.getYear.toString)

          errorSummaryCheck(userScenario.specificExpectedResults.get.mustHave4DigitYear(pageModel.employerName), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.mustHave4DigitYear(pageModel.employerName), Some("amount"))
        }
      }
    }
  }
}
