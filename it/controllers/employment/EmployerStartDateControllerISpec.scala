/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.employment.EmploymentStartDateForm
import models.User
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import java.time.LocalDate

class EmployerStartDateControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear-1

  val employerName: String = "HMRC"
  val employmentStartDate: String = "2020-01-01"
  val updatedEmployerName: String = "Microsoft"
  val employmentId: String = "001"

  val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, employmentCyaModel)


  def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel = EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc))

  private def employerStartDatePageUrl(taxYear: Int) = s"$appUrl/$taxYear/employment-start-date?employmentId=$employmentId"

  val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/employment-start-date?employmentId=$employmentId"

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
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val day: String
    val month: String
    val year: String
    val forExample: String
    val emptyDayError: String
    val emptyMonthError: String
    val emptyYearError: String
    val invalidDayError: String
    val invalidMonthError: String
    val invalidYearError: String
    val invalidDateError: String
    val tooRecentDateError: String
    val futureDateError: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "When did you start working for your employer?"
    val expectedH1 = s"When did you start working at $employerName?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "When did you start working for your employer?"
    val expectedH1 = s"When did you start working at $employerName?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "When did your client start working for their employer?"
    val expectedH1 = s"When did your client start working at $employerName?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "When did your client start working for their employer?"
    val expectedH1 = s"When did your client start working at $employerName?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val day = "day"
    val month = "month"
    val year = "year"
    val forExample = "For example, 12 11 2007"
    val emptyDayError = "The date must include a day"
    val emptyMonthError = "The date must include a month"
    val emptyYearError = "The date must include a year"
    val invalidDayError = "Enter the day in the correct format"
    val invalidMonthError = "Enter the month in the correct format"
    val invalidYearError = "Enter the year in the correct format"
    val invalidDateError = "Enter the date in the correct format"
    val tooRecentDateError = s"The date must be before 5 April $taxYearEOY"
    val futureDateError = "The date must be in the past"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val day = "day"
    val month = "month"
    val year = "year"
    val forExample = "For example, 12 11 2007"
    val emptyDayError = "The date must include a day"
    val emptyMonthError = "The date must include a month"
    val emptyYearError = "The date must include a year"
    val invalidDayError = "Enter the day in the correct format"
    val invalidMonthError = "Enter the month in the correct format"
    val invalidYearError = "Enter the year in the correct format"
    val invalidDateError = "Enter the date in the correct format"
    val tooRecentDateError = s"The date must be before 5 April $taxYearEOY"
    val futureDateError = "The date must be in the past"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  object CyaModel {
    val cya: EmploymentUserData = EmploymentUserData (sessionId, mtditid,nino, taxYearEOY, employmentId, isPriorSubmission = true,
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
            insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
          inputFieldCheck(s"amount-$day", Selectors.daySelector)
          inputFieldCheck(s"amount-$month", Selectors.monthSelector)
          inputFieldCheck(s"amount-$year", Selectors.yearSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the 'start date' page with the correct content and the date prefilled when its already in session" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, userRequest)
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
          inputFieldValueCheck("1", Selectors.daySelector)
          inputFieldValueCheck("1", Selectors.monthSelector)
          inputFieldValueCheck("2020", Selectors.yearSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

        "redirect the user to the overview page when it is not end of year" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(employerStartDatePageUrl(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
          }

        }
      }
    }
  }


  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect the user to the overview page when it is not end of year" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(employerStartDatePageUrl(taxYear), body = "", user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
          }
        }

        "create a new cya model with the employer start date" which {

          lazy val form: Map[String, String] = Map(EmploymentStartDateForm.startDateYear -> "2020", EmploymentStartDateForm.startDateMonth -> "01",
            EmploymentStartDateForm.startDateDay -> "01")

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(employerStartDatePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the check your details page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-details?employmentId=$employmentId")
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyamodel.employment.employmentDetails.startDate shouldBe Some(employmentStartDate)
          }

        }

        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "the day is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentStartDateForm.startDateYear -> "2020", EmploymentStartDateForm.startDateMonth -> "01",
              EmploymentStartDateForm.startDateDay -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
            inputFieldValueCheck("", Selectors.daySelector)
            inputFieldValueCheck("01", Selectors.monthSelector)
            inputFieldValueCheck("2020", Selectors.yearSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.commonExpectedResults.emptyDayError, Selectors.daySelector)
            errorAboveElementCheck(user.commonExpectedResults.emptyDayError, Some("amount"))
          }

          "the month is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentStartDateForm.startDateYear -> "2020", EmploymentStartDateForm.startDateMonth -> "",
              EmploymentStartDateForm.startDateDay -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
            inputFieldValueCheck("01", Selectors.daySelector)
            inputFieldValueCheck("", Selectors.monthSelector)
            inputFieldValueCheck("2020", Selectors.yearSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.commonExpectedResults.emptyMonthError, Selectors.monthSelector)
            errorAboveElementCheck(user.commonExpectedResults.emptyMonthError, Some("amount"))
          }

          "the year is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentStartDateForm.startDateYear -> "", EmploymentStartDateForm.startDateMonth -> "01",
              EmploymentStartDateForm.startDateDay -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
            inputFieldValueCheck("01", Selectors.daySelector)
            inputFieldValueCheck("01", Selectors.monthSelector)
            inputFieldValueCheck("", Selectors.yearSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.commonExpectedResults.emptyYearError, Selectors.yearSelector)
            errorAboveElementCheck(user.commonExpectedResults.emptyYearError, Some("amount"))
          }

          "the day is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentStartDateForm.startDateYear -> "2020", EmploymentStartDateForm.startDateMonth -> "01",
              EmploymentStartDateForm.startDateDay -> "abc")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
            inputFieldValueCheck("abc", Selectors.daySelector)
            inputFieldValueCheck("01", Selectors.monthSelector)
            inputFieldValueCheck("2020", Selectors.yearSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.commonExpectedResults.invalidDayError, Selectors.daySelector)
            errorAboveElementCheck(user.commonExpectedResults.invalidDayError, Some("amount"))
          }

          "the month is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentStartDateForm.startDateYear -> "2020", EmploymentStartDateForm.startDateMonth -> "abc",
              EmploymentStartDateForm.startDateDay -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
            inputFieldValueCheck("01", Selectors.daySelector)
            inputFieldValueCheck("abc", Selectors.monthSelector)
            inputFieldValueCheck("2020", Selectors.yearSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.commonExpectedResults.invalidMonthError, Selectors.monthSelector)
            errorAboveElementCheck(user.commonExpectedResults.invalidMonthError, Some("amount"))
          }

          "the year is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentStartDateForm.startDateYear -> "abc", EmploymentStartDateForm.startDateMonth -> "01",
              EmploymentStartDateForm.startDateDay -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
            inputFieldValueCheck("01", Selectors.daySelector)
            inputFieldValueCheck("01", Selectors.monthSelector)
            inputFieldValueCheck("abc", Selectors.yearSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.commonExpectedResults.invalidYearError, Selectors.yearSelector)
            errorAboveElementCheck(user.commonExpectedResults.invalidYearError, Some("amount"))
          }

          "the date is an invalid date" which {
            lazy val form: Map[String, String] = Map(EmploymentStartDateForm.startDateYear -> "2020", EmploymentStartDateForm.startDateMonth -> "13",
              EmploymentStartDateForm.startDateDay -> "01")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
            inputFieldValueCheck("01", Selectors.daySelector)
            inputFieldValueCheck("13", Selectors.monthSelector)
            inputFieldValueCheck("2020", Selectors.yearSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.commonExpectedResults.invalidDateError, Selectors.daySelector)
            errorAboveElementCheck(user.commonExpectedResults.invalidDateError, Some("amount"))
          }

          "the date is a too recent date i.e. after 5thApril" which {
            lazy val form: Map[String, String] = Map(EmploymentStartDateForm.startDateYear -> taxYearEOY.toString, EmploymentStartDateForm.startDateMonth -> "06",
              EmploymentStartDateForm.startDateDay -> "09")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
            inputFieldValueCheck("09", Selectors.daySelector)
            inputFieldValueCheck("06", Selectors.monthSelector)
            inputFieldValueCheck(taxYearEOY.toString, Selectors.yearSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.commonExpectedResults.tooRecentDateError, Selectors.daySelector)
            errorAboveElementCheck(user.commonExpectedResults.tooRecentDateError, Some("amount"))
          }

          "the date is not in the past" which {
            val nowDatePlusOne = LocalDate.now().plusDays(1)
            lazy val form: Map[String, String] = Map(
              EmploymentStartDateForm.startDateYear -> nowDatePlusOne.getYear.toString,
              EmploymentStartDateForm.startDateMonth -> nowDatePlusOne.getMonthValue.toString,
              EmploymentStartDateForm.startDateDay -> nowDatePlusOne.getDayOfMonth.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
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
            inputFieldValueCheck(nowDatePlusOne.getDayOfMonth.toString, Selectors.daySelector)
            inputFieldValueCheck(nowDatePlusOne.getMonthValue.toString, Selectors.monthSelector)
            inputFieldValueCheck(nowDatePlusOne.getYear.toString, Selectors.yearSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.commonExpectedResults.futureDateError, Selectors.daySelector)
            errorAboveElementCheck(user.commonExpectedResults.futureDateError, Some("amount"))
          }
        }
      }
    }
  }
}

