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

import builders.models.UserBuilder.aUserRequest
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import forms.employment.EmploymentDateForm
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{checkYourDetailsUrl, employmentEndDateUrl, overviewUrl}

import java.time.LocalDate

class EmployerStartDateControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employerName: String = "HMRC"
  private val employmentStartDate: String = "2020-01-01"
  private val employmentId: String = "employmentId"
  private val dayInputName = "amount-day"
  private val monthInputName = "amount-month"
  private val yearInputName = "amount-year"

  private def cyaModel(employerName: String, hmrc: Boolean) = EmploymentCYAModel(EmploymentDetails(
    employerName,
    employerRef = Some("123/12345"),
    cessationDateQuestion = Some(true),
    currentDataIsHmrcHeld = hmrc,
    payrollId = Some("12345"),
    taxablePayToDate = Some(5),
    totalTaxToDate = Some(5)
  ))

  private def employerStartDatePageUrl(taxYear: Int) = s"$appUrl/$taxYear/employment-start-date?employmentId=$employmentId"

  val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/employment-start-date?employmentId=$employmentId"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
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
    val day: String
    val month: String
    val year: String
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

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val day = "day"
    val month = "month"
    val year = "year"
    val forExample = "For example, 12 11 2007"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val day = "day"
    val month = "month"
    val year = "year"
    val forExample = "For example, 12 11 2007"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  object CyaModel {
    val cya: EmploymentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = true,
      EmploymentCYAModel(
        EmploymentDetails(employerName, startDate = Some("2020-01-01"), currentDataIsHmrcHeld = false),
        None
      )
    )
  }

  ".show" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'start date' page with the correct content" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(employerStartDatePageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the 'start date' page with the correct content and the date prefilled when its already in session" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, aUserRequest)
            urlGet(employerStartDatePageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(forExample, forExampleSelector)
          inputFieldValueCheck(dayInputName, Selectors.daySelector, "1")
          inputFieldValueCheck(monthInputName, Selectors.monthSelector, "1")
          inputFieldValueCheck(yearInputName, Selectors.yearSelector, "2020")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(employerStartDatePageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe overviewUrl(taxYear)
      }

    }
  }


  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "the day is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "2020", EmploymentDateForm.month -> "01",
              EmploymentDateForm.day -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "01")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "2020")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.emptyDayError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyDayError, Some("amount"))
          }

          "the month is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "2020", EmploymentDateForm.month -> "",
              EmploymentDateForm.day -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "2020")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.emptyMonthError, Selectors.monthSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyMonthError, Some("amount"))
          }

          "the year is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "", EmploymentDateForm.month -> "01",
              EmploymentDateForm.day -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "01")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.emptyYearError, Selectors.yearSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyYearError, Some("amount"))
          }

          "the day and month are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "2020", EmploymentDateForm.month -> "",
              EmploymentDateForm.day -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "2020")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.emptyDayMonthError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyDayMonthError, Some("amount"))
          }

          "the day and year are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "", EmploymentDateForm.month -> "01",
              EmploymentDateForm.day -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "01")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.emptyDayYearError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyDayYearError, Some("amount"))
          }

          "the year and month are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "", EmploymentDateForm.month -> "",
              EmploymentDateForm.day -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.emptyMonthYearError, Selectors.monthSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyMonthYearError, Some("amount"))
          }

          "the day, month and year are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "", EmploymentDateForm.month -> "",
              EmploymentDateForm.day -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.emptyAllError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyAllError, Some("amount"))
          }

          "the day is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "2020", EmploymentDateForm.month -> "01",
              EmploymentDateForm.day -> "abc")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "abc")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "01")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "2020")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidDateError, Some("amount"))
          }

          "the month is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "2020", EmploymentDateForm.month -> "abc",
              EmploymentDateForm.day -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "abc")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "2020")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidDateError, Some("amount"))
          }

          "the year is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "abc", EmploymentDateForm.month -> "01",
              EmploymentDateForm.day -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "01")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "abc")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidDateError, Some("amount"))
          }

          "the date is an invalid date i.e. month is set to 13" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "2020", EmploymentDateForm.month -> "13",
              EmploymentDateForm.day -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "01")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "13")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "2020")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidDateError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidDateError, Some("amount"))
          }

          "the data is too long ago (must be after 1st January 1900)" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "1900", EmploymentDateForm.month -> "1",
              EmploymentDateForm.day -> "1")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "1")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "1")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, "1900")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.tooLongAgoDateError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.tooLongAgoDateError, Some("amount"))
          }

          "the date is a too recent date i.e. after 5thApril" which {
            lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> taxYearEOY.toString, EmploymentDateForm.month -> "06",
              EmploymentDateForm.day -> "09")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, "09")
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, "06")
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.tooRecentDateError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.tooRecentDateError, Some("amount"))
          }

          "the date is not in the past" which {
            val nowDatePlusOne = LocalDate.now().plusDays(1)
            lazy val form: Map[String, String] = Map(
              EmploymentDateForm.year -> nowDatePlusOne.getYear.toString,
              EmploymentDateForm.month -> nowDatePlusOne.getMonthValue.toString,
              EmploymentDateForm.day -> nowDatePlusOne.getDayOfMonth.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(forExample, forExampleSelector)
            inputFieldValueCheck(dayInputName, Selectors.daySelector, nowDatePlusOne.getDayOfMonth.toString)
            inputFieldValueCheck(monthInputName, Selectors.monthSelector, nowDatePlusOne.getMonthValue.toString)
            inputFieldValueCheck(yearInputName, Selectors.yearSelector, nowDatePlusOne.getYear.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.futureDateError, Selectors.daySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.futureDateError, Some("amount"))
          }

        }
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(employerStartDatePageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe overviewUrl(taxYear)
      }
    }

    "redirect when the date is now before the end date" which {
      val cya = cyaModel(employerName, hmrc = true)
      lazy val form: Map[String, String] = Map(
        EmploymentDateForm.year -> (taxYearEOY - 1).toString,
        EmploymentDateForm.month -> "11",
        EmploymentDateForm.day -> "11")

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = cya.copy(employmentDetails = cya.employmentDetails.copy(
          cessationDateQuestion = Some(false), cessationDate = Some(s"${taxYearEOY - 1}-11-10")))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has the correct status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe employmentEndDateUrl(taxYearEOY, employmentId)

      }
    }

    "create a new cya model with the employer start date" which {

      lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "2020", EmploymentDateForm.month -> "01",
        EmploymentDateForm.day -> "01")

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe checkYourDetailsUrl(taxYearEOY, employmentId)
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentDetails.startDate shouldBe Some(employmentStartDate)
      }

    }
  }
}

