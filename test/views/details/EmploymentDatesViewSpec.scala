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

package views.details

import controllers.details.routes.EmploymentDatesController
import forms.details.EmploymentDatesForm
import models.AuthorisationRequest
import models.employment.{EmploymentDate, EmploymentDates}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.details.EmploymentDatesView

import java.time.LocalDate

class EmploymentDatesViewSpec extends ViewUnitTest {

  private val employerName: String = "HMRC"
  private val employmentId: String = "employmentId"
  private val startDayInputName = "startAmount-day"
  private val startMonthInputName = "startAmount-month"
  private val startYearInputName = "startAmount-year"
  private val endDayInputName = "endAmount-day"
  private val endMonthInputName = "endAmount-month"
  private val endYearInputName = "endAmount-year"

  private val employmentStartDay: String = "11"
  private val employmentStartMonth: String = "11"
  private val employmentStartYear: String = s"${taxYearEOY - 1}"
  private val employmentEndDay: String = "12"
  private val employmentEndMonth: String = "12"
  private val employmentEndYear: String = s"${taxYearEOY - 1}"

  object Selectors {
    val startDaySelector: String = "#startAmount-day"
    val startMonthSelector: String = "#startAmount-month"
    val startYearSelector: String = "#startAmount-year"
    val startForExampleSelector: String = "#startAmount-hint"
    val endDaySelector: String = "#endAmount-day"
    val endMonthSelector: String = "#endAmount-month"
    val endYearSelector: String = "#endAmount-year"
    val endForExampleSelector: String = "#endAmount-hint"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    val startEmptyDayError: String
    val startEmptyMonthError: String
    val startEmptyYearError: String
    val startEmptyDayYearError: String
    val startEmptyMonthYearError: String
    val startEmptyDayMonthError: String
    val startEmptyAllError: String
    val invalidStartDateError: String
    val startTooLongAgoDateError: String
    val startTooRecentDateError: String
    val startFutureDateError: String
    val leaveBeforeStartDate: String
    val leaveEmptyDayError: String
    val leaveEmptyMonthError: String
    val leaveEmptyYearError: String
    val leaveEmptyDayYearError: String
    val leaveEmptyMonthYearError: String
    val leaveEmptyDayMonthError: String
    val leaveEmptyAllError: String
    val invalidLeaveDateError: String
    val leaveTooLongAgoDateError: String
    val leaveTooRecentDateError: String
    val leaveFutureDateError: String
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedCaption: Int => String
    val expectedButtonText: String
    val forExample: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val startEmptyDayError = "The date you started employment must include a day"
    val startEmptyMonthError = "The date you started employment must include a month"
    val startEmptyYearError = "The date you started employment must include a year"
    val startEmptyDayYearError = "The date you started employment must include a day and year"
    val startEmptyMonthYearError = "The date you started employment must include a month and year"
    val startEmptyDayMonthError = "The date you started employment must include a day and month"
    val startEmptyAllError = "Enter the date your employment started"
    val invalidStartDateError = "The date you started employment must be a real date"
    val startTooLongAgoDateError = "The date you started your employment must be after 1 January 1900"
    val startTooRecentDateError = s"The date you started employment must be before 6 April $taxYearEOY"
    val startFutureDateError = "The date you started employment must be in the past"
    val leaveBeforeStartDate = s"The date you left your employment cannot be before 4 April $taxYearEOY"
    val leaveEmptyDayError = "The date you left your employment must include a day"
    val leaveEmptyMonthError = "The date you left your employment must include a month"
    val leaveEmptyYearError = "The date you left your employment must include a year"
    val leaveEmptyDayYearError = "The date you left your employment must include a day and year"
    val leaveEmptyMonthYearError = "The date you left your employment must include a month and year"
    val leaveEmptyDayMonthError = "The date you left your employment must include a day and month"
    val leaveEmptyAllError = "Enter the date you left your employment"
    val invalidLeaveDateError = "The date you left your employment must be a real date"
    val leaveTooLongAgoDateError = s"The date you left your employment must be after 5 April ${taxYearEOY - 1}"
    val leaveTooRecentDateError = s"The date you left your employment must be before 6 April $taxYearEOY"
    val leaveFutureDateError = "The date you left your employment must be in the past"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val startEmptyDayError = "Maeín rhaid iír dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys diwrnod"
    val startEmptyMonthError = "Maeín rhaid iír dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys mis"
    val startEmptyYearError = "Maeín rhaid iír dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys blwyddyn"
    val startEmptyDayYearError = "Maeín rhaid iír dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys diwrnod a blwyddyn"
    val startEmptyMonthYearError = "Maeín rhaid iír dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys mis a blwyddyn"
    val startEmptyDayMonthError = "Maeín rhaid iír dyddiad y gwnaethoch ddechrau cyflogaeth gynnwys diwrnod a mis"
    val startEmptyAllError = "Nodwch y dyddiad y dechreuodd eich cyflogaeth"
    val invalidStartDateError = "Maeín rhaid iír dyddiad y gwnaethoch ddechrau cyflogaeth fod yn ddyddiad go iawn"
    val startTooLongAgoDateError = "Maeín rhaid i ddyddiad y gwnaethoch ddechrauích cyflogaeth fod ar Ùl 1 Ionawr 1900"
    val startTooRecentDateError = s"Maeín rhaid i ddyddiad y gwnaethoch ddechrau cyflogaeth fod cyn 6 Ebrill $taxYearEOY"
    val startFutureDateError = "Maeín rhaid iír dyddiad y gwnaethoch ddechrau cyflogaeth fod yn y gorffennol"
    val leaveBeforeStartDate = s"Does dim modd i’r dyddiad y gwnaethoch adael eich cyflogaeth fod cyn 4 April $taxYearEOY"
    val leaveEmptyDayError = "Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth gynnwys diwrnod"
    val leaveEmptyMonthError = "Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth gynnwys mis"
    val leaveEmptyYearError = "Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth gynnwys blwyddyn"
    val leaveEmptyDayYearError = "Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth gynnwys diwrnod a blwyddyn"
    val leaveEmptyMonthYearError = "Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth gynnwys mis a blwyddyn"
    val leaveEmptyDayMonthError = "Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth gynnwys diwrnod a mis"
    val leaveEmptyAllError = "Nodwch y dyddiad y gwnaethoch chi adael eich cyflogaeth"
    val invalidLeaveDateError = "Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth fod yn ddyddiad go iawn"
    val leaveTooLongAgoDateError = s"Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth fod ar Ùl 5 Ebrill ${taxYearEOY - 1}"
    val leaveTooRecentDateError = s"Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth fod cyn 6 Ebrill $taxYearEOY"
    val leaveFutureDateError = "Maeín rhaid iír dyddiad y gwnaethoch adael eich cyflogaeth fod yn y gorffennol"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val startEmptyDayError = "The date your client started their employment must include a day"
    val startEmptyMonthError = "The date your client started their employment must include a month"
    val startEmptyYearError = "The date your client started their employment must include a year"
    val startEmptyDayYearError = "The date your client started their employment must include a day and year"
    val startEmptyMonthYearError = "The date your client started their employment must include a month and year"
    val startEmptyDayMonthError = "The date your client started their employment must include a day and month"
    val startEmptyAllError = "Enter the date your client’s employment started"
    val invalidStartDateError = "The date your client started their employment must be a real date"
    val startTooLongAgoDateError = "The date your client started their employment must be after 1 January 1900"
    val startTooRecentDateError = s"The date your client started their employment must be before 6 April $taxYearEOY"
    val startFutureDateError = "The date your client started their employment must be in the past"
    val leaveBeforeStartDate = s"The date your client left their employment cannot be before 4 April $taxYearEOY"
    val leaveEmptyDayError = "The date your client left their employment must include a day"
    val leaveEmptyMonthError = "The date your client left their employment must include a month"
    val leaveEmptyYearError = "The date your client left their employment must include a year"
    val leaveEmptyDayYearError = "The date your client left their employment must include a day and year"
    val leaveEmptyMonthYearError = "The date your client left their employment must include a month and year"
    val leaveEmptyDayMonthError = "The date your client left their employment must include a day and month"
    val leaveEmptyAllError = "Enter the date your client left their employment"
    val invalidLeaveDateError = "The date your client left their employment must be a real date"
    val leaveTooLongAgoDateError = s"The date your client left their employment must be after 5 April ${taxYearEOY - 1}"
    val leaveTooRecentDateError = s"The date your client left their employment must be before 6 April $taxYearEOY"
    val leaveFutureDateError = "The date your client left their employment must be in the past"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val startEmptyDayError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys diwrnod"
    val startEmptyMonthError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys mis"
    val startEmptyYearError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys blwyddyn"
    val startEmptyDayYearError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys diwrnod a blwyddyn"
    val startEmptyMonthYearError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys mis a blwyddyn"
    val startEmptyDayMonthError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth gynnwys diwrnod a mis"
    val startEmptyAllError = "Nodwch y dyddiad y dechreuodd gyflogaeth eich cleient"
    val invalidStartDateError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth fod yn ddyddiad go iawn"
    val startTooLongAgoDateError = "Mae’n rhaid i ddyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth fod ar ôl 1 Ionawr 1900"
    val startTooRecentDateError = s"Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth fod cyn 6 Ebrill $taxYearEOY"
    val startFutureDateError = "Mae’n rhaid i’r dyddiad y gwnaeth eich cleient ddechrau ei gyflogaeth fod yn y gorffennol"
    val leaveBeforeStartDate = s"Does dim modd iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth fod cyn 4 April $taxYearEOY"
    val leaveEmptyDayError = "Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth gynnwys diwrnod"
    val leaveEmptyMonthError = "Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth gynnwys mis"
    val leaveEmptyYearError = "Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth gynnwys blwyddyn"
    val leaveEmptyDayYearError = "Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth gynnwys diwrnod a blwyddyn"
    val leaveEmptyMonthYearError = "Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth gynnwys mis a blwyddyn"
    val leaveEmptyDayMonthError = "Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth gynnwys diwrnod a mis"
    val leaveEmptyAllError = "Nodwch y dyddiad y gwnaeth eich cleient adael ei gyflogaeth"
    val invalidLeaveDateError = "Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth fod yn ddyddiad go iawn"
    val leaveTooLongAgoDateError = s"Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth fod ar neu ar Ùl 5 Ebrill ${taxYearEOY - 1}"
    val leaveTooRecentDateError = s"Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth fod cyn 6 Ebrill $taxYearEOY"
    val leaveFutureDateError = "Maeín rhaid iír dyddiad y gwnaeth eich cleient adael ei gyflogaeth fod yn y gorffennol"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Employment dates"
    val expectedH1 = "Employment dates"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val forExample = "For example, 12 11 2007"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "Dyddiadau cyflogaeth"
    val expectedH1 = "Dyddiadau cyflogaeth"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedCaption: Int => String = (taxYear: Int) => s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val forExample = "Er enghraifft, 12 11 2007"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val form = EmploymentDatesForm
  private val underTest = inject[EmploymentDatesView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the 'employment dates' page with an empty form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.employmentDatesForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(forExample, startForExampleSelector, "forStart")
        inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "")
        inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "")
        inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "")
        textOnPageCheck(forExample, endForExampleSelector, "forEnd")
        inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "")
        inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "")
        inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "")
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the 'employment dates' page with the correct content and the date prefilled" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form.employmentDatesForm.fill(EmploymentDates(
          Some(EmploymentDate(employmentStartDay, employmentStartMonth, employmentStartYear)),
          Some(EmploymentDate(employmentEndDay, employmentEndMonth, employmentEndYear)))),
          taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(forExample, startForExampleSelector, "forStart")
        inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
        inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
        inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
        textOnPageCheck(forExample, endForExampleSelector, "forEnd")
        inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
        inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
        inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
        buttonCheck(expectedButtonText, continueButtonSelector)
        formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the 'employment dates' page with an error" when {

        "the start day is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> "",
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.startEmptyDayError, Selectors.startDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startEmptyDayError, Some("startAmount"))
        }

        "the start month is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> "",
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.startEmptyMonthError, Selectors.startMonthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startEmptyMonthError, Some("startAmount"))
        }

        "the start year is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> "",
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "")
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.startEmptyYearError, Selectors.startYearSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startEmptyYearError, Some("startAmount"))
        }

        "the start day and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> "",
              EmploymentDatesForm.startAmountDay -> "",
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.startEmptyDayMonthError, Selectors.startDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startEmptyDayMonthError, Some("startAmount"))
        }

        "the start day and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> "",
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> "",
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "")
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.startEmptyDayYearError, Selectors.startDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startEmptyDayYearError, Some("startAmount"))
        }

        "the start year and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> "",
              EmploymentDatesForm.startAmountMonth -> "",
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "")
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.startEmptyMonthYearError, Selectors.startMonthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startEmptyMonthYearError, Some("startAmount"))
        }

        "the start day, month and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> "",
              EmploymentDatesForm.startAmountMonth -> "",
              EmploymentDatesForm.startAmountDay -> "",
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "")
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.startEmptyAllError, Selectors.startDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startEmptyAllError, Some("startAmount"))
        }

        "the start day is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> "abc",
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "abc")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidStartDateError, Selectors.startDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidStartDateError, Some("startAmount"))
        }

        "the start month is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> "abc",
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "abc")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidStartDateError, Selectors.startDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidStartDateError, Some("startAmount"))
        }

        "the start year is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> "abc",
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "abc")
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidStartDateError, Selectors.startDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidStartDateError, Some("startAmount"))
        }

        "the start date is an invalid date i.e. month is set to 13" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> "13",
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "13")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidStartDateError, Selectors.startDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidStartDateError, Some("startAmount"))
        }

        "the start date is too long ago (must be after 1st January 1900)" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> "1899",
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "1899")
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.startTooLongAgoDateError, Selectors.startDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startTooLongAgoDateError, Some("startAmount"))
        }

        "the start date and the end dates are too recent i.e. after 5th April" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.startAmountMonth -> "04",
              EmploymentDatesForm.startAmountDay -> "06",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountMonth -> "04",
              EmploymentDatesForm.endAmountDay -> "06"
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "06")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "04")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "04")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          multipleErrorCheck(List((userScenario.specificExpectedResults.get.startTooRecentDateError, Selectors.startDaySelector),
            (userScenario.specificExpectedResults.get.leaveTooRecentDateError, Selectors.endDaySelector)), userScenario.isWelsh)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startTooRecentDateError, Some("startAmount"))
        }

        "the start date and the end date are not in the past" which {
          val nowDatePlusOne = LocalDate.now().plusDays(1)
          val nowDatePlusTwo = LocalDate.now().plusDays(1)

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountDay -> nowDatePlusOne.getDayOfMonth.toString,
              EmploymentDatesForm.startAmountMonth -> nowDatePlusOne.getMonthValue.toString,
              EmploymentDatesForm.startAmountYear -> nowDatePlusOne.getYear.toString,
              EmploymentDatesForm.endAmountDay -> nowDatePlusTwo.getDayOfMonth.toString,
              EmploymentDatesForm.endAmountMonth -> nowDatePlusOne.getMonthValue.toString,
              EmploymentDatesForm.endAmountYear -> nowDatePlusOne.getYear.toString
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, nowDatePlusOne.getDayOfMonth.toString)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, nowDatePlusOne.getMonthValue.toString)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, nowDatePlusOne.getYear.toString)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, nowDatePlusTwo.getDayOfMonth.toString)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, nowDatePlusTwo.getMonthValue.toString)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, nowDatePlusTwo.getYear.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          multipleErrorCheck(List((userScenario.specificExpectedResults.get.startFutureDateError, Selectors.startDaySelector),
            (userScenario.specificExpectedResults.get.leaveFutureDateError, Selectors.endDaySelector)), userScenario.isWelsh)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.startFutureDateError, Some("startAmount"))
        }

        "the start date is after the leave date" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.startAmountMonth -> "04",
              EmploymentDatesForm.startAmountDay -> "04",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountDay -> "03"
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector, "forStart")
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "04")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "04")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
          textOnPageCheck(forExample, endForExampleSelector, "forEnd")
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "03")
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveBeforeStartDate, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveBeforeStartDate, Some("endAmount"))
        }

        "the end day is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> ""
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "")
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveEmptyDayError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveEmptyDayError, Some("endAmount"))
        }

        "the end month is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> "",
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveEmptyMonthError, Selectors.endMonthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveEmptyMonthError, Some("endAmount"))
        }

        "the end year is empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> "",
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveEmptyYearError, Selectors.endYearSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveEmptyYearError, Some("endAmount"))
        }

        "the end day and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> "",
              EmploymentDatesForm.endAmountDay -> ""
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "")
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveEmptyDayMonthError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveEmptyDayMonthError, Some("endAmount"))
        }

        "the end day and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> "",
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> ""
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "")
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveEmptyDayYearError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveEmptyDayYearError, Some("endAmount"))
        }

        "the end year and month are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> "",
              EmploymentDatesForm.endAmountMonth -> "",
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveEmptyMonthYearError, Selectors.endMonthSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveEmptyMonthYearError, Some("endAmount"))
        }

        "the end day, month and year are empty" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> "",
              EmploymentDatesForm.endAmountMonth -> "",
              EmploymentDatesForm.endAmountDay -> ""
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "")
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveEmptyAllError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveEmptyAllError, Some("endAmount"))
        }

        "the end day is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> "abc"
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "abc")
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidLeaveDateError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidLeaveDateError, Some("endAmount"))
        }

        "the end month is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> "abc",
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "abc")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidLeaveDateError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidLeaveDateError, Some("endAmount"))
        }

        "the end year is invalid" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> "abc",
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "abc")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidLeaveDateError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidLeaveDateError, Some("endAmount"))
        }

        "the end date is an invalid date i.e. month is set to 13" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> employmentEndYear,
              EmploymentDatesForm.endAmountMonth -> "13",
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "13")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, employmentEndYear)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.invalidLeaveDateError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.invalidLeaveDateError, Some("endAmount"))
        }

        "the end date data is too long ago (must be after 1st January 1900)" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> employmentStartYear,
              EmploymentDatesForm.startAmountMonth -> employmentStartMonth,
              EmploymentDatesForm.startAmountDay -> employmentStartDay,
              EmploymentDatesForm.endAmountYear -> "1899",
              EmploymentDatesForm.endAmountMonth -> employmentEndMonth,
              EmploymentDatesForm.endAmountDay -> employmentEndDay
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, employmentStartDay)
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, employmentStartMonth)
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, employmentStartYear)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, employmentEndDay)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, employmentEndMonth)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "1899")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveTooLongAgoDateError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveTooLongAgoDateError, Some("endAmount"))
        }

        "the end date is a too recent date i.e. after 5th April" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.startAmountMonth -> "03",
              EmploymentDatesForm.startAmountDay -> "05",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountMonth -> "04",
              EmploymentDatesForm.endAmountDay -> "06"
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "05")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "03")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "04")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveTooRecentDateError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveTooRecentDateError, Some("endAmount"))
        }

        "the end date is not in the past" which {
          val nowDatePlusOne = LocalDate.now().plusDays(1)
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val filledForm = form.employmentDatesForm.bind(
            Map(EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.startAmountMonth -> "03",
              EmploymentDatesForm.startAmountDay -> "06",
              EmploymentDatesForm.endAmountYear -> nowDatePlusOne.getYear.toString,
              EmploymentDatesForm.endAmountMonth -> nowDatePlusOne.getMonthValue.toString,
              EmploymentDatesForm.endAmountDay -> nowDatePlusOne.getDayOfMonth.toString
            ))
          val validatedForm = filledForm.copy(errors = EmploymentDatesForm.verifyDates(
            filledForm.get, taxYearEOY, userScenario.isAgent))

          val htmlFormat = underTest(validatedForm, taxYear = taxYearEOY, employmentId = employmentId, employerName = employerName)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector)
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "06")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "03")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, nowDatePlusOne.getDayOfMonth.toString)
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, nowDatePlusOne.getMonthValue.toString)
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, nowDatePlusOne.getYear.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(EmploymentDatesController.submit(taxYearEOY, employmentId).url, continueButtonFormSelector)
          welshToggleCheck(userScenario.isWelsh)

          errorSummaryCheck(userScenario.specificExpectedResults.get.leaveFutureDateError, Selectors.endDaySelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.leaveFutureDateError, Some("endAmount"))
        }
      }
    }
  }
}
