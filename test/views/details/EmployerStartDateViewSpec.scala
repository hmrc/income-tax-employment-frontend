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
import forms.details.EmploymentDateForm
import models.AuthorisationRequest
import models.details.EmploymentDetails
import models.employment.EmploymentDate
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import views.html.details.EmployerStartDateView

import java.time.LocalDate

class EmployerStartDateViewSpec extends ViewUnitTest {

  private val nino: String = anEmploymentUserData.nino
  private val mtditid: String = anEmploymentUserData.mtdItId
  private val sessionId: String = anEmploymentUserData.sessionId
  private val employerName: String = "HMRC"
  private val employmentStartDay: String = "01"
  private val employmentStartMonth: String = "01"
  private val employmentStartYear: String = s"${taxYearEOY - 1}"
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
    val expectedTitle: String
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
    val tooLongAgoDateError: String
    val tooRecentDateError: String
    val futureDateError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val forExample: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "When did you start working for your employer?"
    val expectedH1 = s"When did you start working at $employerName?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyDayError = "The date you started employment must include a day"
    val emptyMonthError = "The date you started employment must include a month"
    val emptyYearError = "The date you started employment must include a year"
    val emptyDayYearError = "The date you started employment must include a day and year"
    val emptyMonthYearError = "The date you started employment must include a month and year"
    val emptyDayMonthError = "The date you started employment must include a day and month"
    val emptyAllError = "Enter the date your employment started"
    val invalidDateError = "The date you started employment must be a real date"
    val tooLongAgoDateError = "The date you started your employment must be after 1 January 1900"
    val tooRecentDateError = s"The date you started employment must be before 6 April $taxYearEOY"
    val futureDateError = "The date you started employment must be in the past"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Pryd y gwnaethoch ddechrau gweithio i’ch cyflogwr?"
    val expectedH1 = s"Pryd y gwnaethoch ddechrau gweithio yn $employerName?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val emptyDayError = "Mae’n rhaid i’r dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys diwrnod"
    val emptyMonthError = "Mae’n rhaid i’r dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys mis"
    val emptyYearError = "Mae’n rhaid i’r dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys blwyddyn"
    val emptyDayYearError = "Mae’n rhaid i’r dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys diwrnod a blwyddyn"
    val emptyMonthYearError = "Mae’n rhaid i’r dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys mis a blwyddyn"
    val emptyDayMonthError = "Mae’n rhaid i’r dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys diwrnod a mis"
    val emptyAllError = "Nodwch y dyddiad y dechreuodd eich cyflogaeth"
    val invalidDateError = "Mae’n rhaid i’r dyddiad y gwnaethoch ddechrau cyflogaeth fod yn ddyddiad go iawn"
    val tooLongAgoDateError = "Mae’n rhaid i ddyddiad y gwnaethoch ddechrau’ch cyflogaeth fod ar ôl 1 Ionawr 1900"
    val tooRecentDateError = s"Mae’n rhaid i ddyddiad y gwnaethoch ddechrau cyflogaeth fod cyn 6 Ebrill $taxYearEOY"
    val futureDateError = "Mae’n rhaid i’r dyddiad y gwnaethoch ddechrau cyflogaeth fod yn y gorffennol"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "When did your client start working for their employer?"
    val expectedH1 = s"When did your client start working at $employerName?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyDayError = "The date your client started their employment must include a day"
    val emptyMonthError = "The date your client started their employment must include a month"
    val emptyYearError = "The date your client started their employment must include a year"
    val emptyDayYearError = "The date your client started their employment must include a day and year"
    val emptyMonthYearError = "The date your client started their employment must include a month and year"
    val emptyDayMonthError = "The date your client started their employment must include a day and month"
    val emptyAllError = "Enter the date your client’s employment started"
    val invalidDateError = "The date your client started their employment must be a real date"
    val tooLongAgoDateError = "The date your client started their employment must be after 1 January 1900"
    val tooRecentDateError = s"The date your client started their employment must be before 6 April $taxYearEOY"
    val futureDateError = "The date your client started their employment must be in the past"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Pryd y gwnaeth eich cleient ddechrau gweithio i’w gyflogwr?"
    val expectedH1 = s"Pryd y dechreuodd eich cleient weithio yn $employerName?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val emptyDayError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys diwrnod"
    val emptyMonthError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys mis"
    val emptyYearError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys blwyddyn"
    val emptyDayYearError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys diwrnod a blwyddyn"
    val emptyMonthYearError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys mis a blwyddyn"
    val emptyDayMonthError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys diwrnod a mis"
    val emptyAllError = "Nodwch y dyddiad y dechreuodd gyflogaeth eich cleient"
    val invalidDateError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth fod yn ddyddiad go iawn"
    val tooLongAgoDateError = "Mae’n rhaid i ddyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth fod ar ôl 1 Ionawr 1900"
    val tooRecentDateError = s"Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth fod cyn 6 Ebrill $taxYearEOY"
    val futureDateError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth fod yn y gorffennol"
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

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  object CyaModel {
    val cya: EmploymentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = true, hasPriorStudentLoans = true,
      EmploymentCYAModel(
        EmploymentDetails(employerName, startDate = Some(s"${taxYearEOY - 1}-01-01"), currentDataIsHmrcHeld = false),
        None
      )
    )
  }

  private val form = EmploymentDateForm
  private val underTest = inject[EmployerStartDateView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the 'start date' page with an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.employmentStartDateForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
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

        val htmlFormat = underTest(form.employmentStartDateForm.fill(EmploymentDate(employmentStartDay, employmentStartMonth, employmentStartYear)),
          taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> employmentStartYear,
              EmploymentDateForm.month -> employmentStartMonth,
              EmploymentDateForm.day -> ""))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> employmentStartYear,
              EmploymentDateForm.month -> "",
              EmploymentDateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> "",
              EmploymentDateForm.month -> employmentStartMonth,
              EmploymentDateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> employmentStartYear,
              EmploymentDateForm.month -> "",
              EmploymentDateForm.day -> ""))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> "",
              EmploymentDateForm.month -> employmentStartMonth,
              EmploymentDateForm.day -> ""))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> "",
              EmploymentDateForm.month -> "",
              EmploymentDateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> "",
              EmploymentDateForm.month -> "",
              EmploymentDateForm.day -> ""))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> employmentStartYear,
              EmploymentDateForm.month -> employmentStartMonth,
              EmploymentDateForm.day -> "abc"))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> employmentStartYear,
              EmploymentDateForm.month -> "abc",
              EmploymentDateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> "abc",
              EmploymentDateForm.month -> employmentStartMonth,
              EmploymentDateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> employmentStartYear,
              EmploymentDateForm.month -> "13",
              EmploymentDateForm.day -> employmentStartDay))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
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

        "the data is too long ago (must be after 1st January 1900)" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> "1900",
              EmploymentDateForm.month -> "1",
              EmploymentDateForm.day -> "1"))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "1")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "1")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, "1900")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.tooLongAgoDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.tooLongAgoDateError, Some("amount"))
        }

        "the date is a too recent date i.e. after 5thApril" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> taxYearEOY.toString,
              EmploymentDateForm.month -> "04",
              EmploymentDateForm.day -> "06"))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "06")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "04")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.tooRecentDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.tooRecentDateError, Some("amount"))
        }

        "the date is not in the past" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val nowDatePlusOne = LocalDate.now().plusDays(1)
          val filledForm = form.employmentStartDateForm.bind(
            Map(EmploymentDateForm.year -> nowDatePlusOne.getYear.toString,
              EmploymentDateForm.month -> nowDatePlusOne.getMonthValue.toString,
              EmploymentDateForm.day -> nowDatePlusOne.getDayOfMonth.toString))
          val validatedForm = filledForm.copy(errors = EmploymentDateForm.verifyStartDate(
            filledForm.get, taxYearEOY, userScenario.isAgent, EmploymentDateForm.startDate))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, nowDatePlusOne.getDayOfMonth.toString)
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, nowDatePlusOne.getMonthValue.toString)
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, nowDatePlusOne.getYear.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmployerStartDateController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.futureDateError, Selectors.daySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.futureDateError, Some("amount"))
        }
      }
    }
  }
}
