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

import controllers.details.routes.EmployerEndDateController
import forms.details.DateForm._
import forms.details.{DateForm, EmploymentDetailsFormsProvider}
import models.AuthorisationRequest
import models.benefits.pages.EmployerEndDatePage
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
    val emptyDayError: String
    val emptyMonthError: String
    val emptyYearError: String
    val emptyDayYearError: String
    val emptyMonthYearError: String
    val emptyDayMonthError: String
    val emptyAllError: String
    val invalidDateError: String
    val tooLongAgoDateError: String
    val tooRecentDateError: String
    val beforeStartDateError: LocalDate => String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val forExample: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String => String = (employerName: String) => s"When did you leave $employerName?"
    val expectedH1: String => String = (employerName: String) => expectedTitle(employerName)
    val expectedErrorTitle: String => String = (employerName: String) => s"Error: ${expectedTitle(employerName)}"
    val emptyDayError = "The date you left must include a day"
    val emptyMonthError = "The date you left must include a month"
    val emptyYearError = "The date you left must include a year"
    val emptyDayYearError = "The date you left must include a day and year"
    val emptyMonthYearError = "The date you left must include a month and year"
    val emptyDayMonthError = "The date you left must include a day and month"
    val emptyAllError = "Enter the date your employment ended"
    val invalidDateError = "The date you left must be a real date"
    val tooLongAgoDateError = s"The date you left must be the same as or after 6 April ${taxYearEOY - 1}"
    val tooRecentDateError = s"The date you left must be the same as or before 5 April $taxYearEOY"
    val beforeStartDateError: LocalDate => String = (date: LocalDate) => s"The date you left must be after the date you started, ${dateFormatter(date)}"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String => String = (employerName: String) => s"When did you leave $employerName?"
    val expectedH1: String => String = (employerName: String) => expectedTitle(employerName)
    val expectedErrorTitle: String => String = (employerName: String) => s"Gwall: ${expectedTitle(employerName)}"
    val emptyDayError = "The date you left must include a day"
    val emptyMonthError = "The date you left must include a month"
    val emptyYearError = "The date you left must include a year"
    val emptyDayYearError = "The date you left must include a day and year"
    val emptyMonthYearError = "The date you left must include a month and year"
    val emptyDayMonthError = "The date you left must include a day and month"
    val emptyAllError = "Enter the date your employment ended"
    val invalidDateError = "The date you left must be a real date"
    val tooLongAgoDateError = s"The date you left must be the same as or after 6 April ${taxYearEOY - 1}"
    val tooRecentDateError = s"The date you left must be the same as or before 5 April $taxYearEOY"
    val beforeStartDateError: LocalDate => String = (date: LocalDate) => s"The date you left must be after the date you started, ${dateFormatter(date)}"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String => String = (employerName: String) => s"When did your client leave $employerName?"
    val expectedH1: String => String = (employerName: String) => expectedTitle(employerName)
    val expectedErrorTitle: String => String = (employerName: String) => s"Error: ${expectedTitle(employerName)}"
    val emptyDayError = "The date your client left must include a day"
    val emptyMonthError = "The date your client left must include a month"
    val emptyYearError = "The date your client left must include a year"
    val emptyDayYearError = "The date your client left must include a day and year"
    val emptyMonthYearError = "The date your client left must include a month and year"
    val emptyDayMonthError = "The date your client left must include a day and month"
    val emptyAllError = "Enter the date your client’s employment ended"
    val invalidDateError = "The date your client left must be a real date"
    val tooLongAgoDateError = s"The date your client left must be the same as or after 6 April ${taxYearEOY - 1}"
    val tooRecentDateError = s"The date your client left must be the same as or before 5 April $taxYearEOY"
    val beforeStartDateError: LocalDate => String = (date: LocalDate) => s"The date your client left must be after the date your client started, ${dateFormatter(date)}"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String => String = (employerName: String) => s"When did your client leave $employerName?"
    val expectedH1: String => String = (employerName: String) => expectedTitle(employerName)
    val expectedErrorTitle: String => String = (employerName: String) => s"Gwall: ${expectedTitle(employerName)}"
    val emptyDayError = "The date your client left must include a day"
    val emptyMonthError = "The date your client left must include a month"
    val emptyYearError = "The date your client left must include a year"
    val emptyDayYearError = "The date your client left must include a day and year"
    val emptyMonthYearError = "The date your client left must include a month and year"
    val emptyDayMonthError = "The date your client left must include a day and month"
    val emptyAllError = "Enter the date your client’s employment ended"
    val invalidDateError = "The date your client left must be a real date"
    val tooLongAgoDateError = s"The date your client left must be the same as or after 6 April ${taxYearEOY - 1}"
    val tooRecentDateError = s"The date your client left must be the same as or before 5 April $taxYearEOY"
    val beforeStartDateError: LocalDate => String = (date: LocalDate) => s"The date your client left must be after the date your client started, ${dateFormatter(date)}"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val forExample = "For example, 12 11 2007"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val forExample = "Er enghraifft, 12 11 2007"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val formsProvider = new EmploymentDetailsFormsProvider()
  private val dateForm = DateForm.dateForm()
  private val startDate = LocalDate.parse(anEmploymentDetails.startDate.get)
  private val underTest = inject[EmployerEndDateView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      val page = anEmployerEndDatePage.copy(isAgent = userScenario.isAgent)
      "render page with an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(page)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle(page.employerName), userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1(page.employerName), isFieldSetH1 = true)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(forExample, forExampleSelector)
        inputFieldValueCheck(day, Selectors.daySelector, "")
        inputFieldValueCheck(month, Selectors.monthSelector, "")
        inputFieldValueCheck(year, Selectors.yearSelector, "")
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, page.employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with prefilled data" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = page.copy(form = dateForm.fill(DateFormData("1", "1", taxYearEOY.toString)))
        val htmlFormat = underTest(pageModel)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle(page.employerName), userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1(page.employerName), isFieldSetH1 = true)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(forExample, forExampleSelector)
        inputFieldValueCheck(day, Selectors.daySelector, "1")
        inputFieldValueCheck(month, Selectors.monthSelector, "1")
        inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, page.employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the 'start date' page with an error" which {
        "the day is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "", amountMonth = "1", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyDayError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyDayError, Some("amount"))
        }

        "the month is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyMonthError, Selectors.monthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyMonthError, Some("amount"))
        }

        "the year is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "1", amountYear = "")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyYearError, Selectors.yearSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyYearError, Some("amount"))
        }

        "the day and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "", amountMonth = "", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "")
          inputFieldValueCheck(month, Selectors.monthSelector, "")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyDayMonthError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyDayMonthError, Some("amount"))
        }

        "the day and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "", amountMonth = "1", amountYear = "")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyDayYearError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyDayYearError, Some("amount"))
        }

        "the year and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "", amountYear = "")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "")
          inputFieldValueCheck(year, Selectors.yearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyMonthYearError, Selectors.monthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyMonthYearError, Some("amount"))
        }

        "the day, month and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "", amountMonth = "", amountYear = "")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "")
          inputFieldValueCheck(month, Selectors.monthSelector, "")
          inputFieldValueCheck(year, Selectors.yearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.emptyAllError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.emptyAllError, Some("amount"))
        }

        "the day is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "abc", amountMonth = "1", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "abc")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError, Some("amount"))
        }

        "the month is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "abc", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "abc")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError, Some("amount"))
        }

        "the year is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "1", amountYear = "abc")
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, "abc")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError, Some("amount"))
        }

        "the date is an invalid date i.e. month is set to 13" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "13", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "13")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidDateError, Some("amount"))
        }

        "the date is before the start of the tax year" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "1", amountMonth = "1", amountYear = (taxYearEOY - 1).toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, (taxYearEOY - 1).toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.tooLongAgoDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.tooLongAgoDateError, Some("amount"))
        }

        "the date is not before the start date" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val startDate = LocalDate.of(taxYearEOY, 1, 1)
          val formData = DateFormData(amountDay = "1", amountMonth = "1", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "1")
          inputFieldValueCheck(month, Selectors.monthSelector, "1")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.beforeStartDateError(startDate), Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.beforeStartDateError(startDate), Some("amount"))
        }

        "the date is after the tax year end" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val formData = DateFormData(amountDay = "6", amountMonth = "4", amountYear = taxYearEOY.toString)
          val pageModel = page.copy(form = formsProvider.validatedEndDateForm(dateForm.fill(formData), taxYearEOY, userScenario.isAgent, EmployerEndDatePage.pageNameKey, startDate))
          val htmlFormat = underTest(pageModel)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle(pageModel.employerName), userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1(pageModel.employerName), isFieldSetH1 = true)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(day, Selectors.daySelector, "6")
          inputFieldValueCheck(month, Selectors.monthSelector, "4")
          inputFieldValueCheck(year, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerEndDateController.submit(taxYearEOY, pageModel.employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.tooRecentDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.tooRecentDateError, Some("amount"))
        }
      }
    }
  }
}
