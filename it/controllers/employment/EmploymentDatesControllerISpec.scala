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

import forms.employment.EmploymentDatesForm
import models.mongo.{EmploymentCYAModel, EmploymentDetails}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{checkYourDetailsUrl, employmentDatesUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import java.time.LocalDate

//scalastyle:off
class EmploymentDatesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employerName: String = "HMRC"
  private val employmentId: String = "employmentId"
  private val startDayInputName = "startAmount-day"
  private val startMonthInputName = "startAmount-month"
  private val startYearInputName = "startAmount-year"
  private val endDayInputName = "endAmount-day"
  private val endMonthInputName = "endAmount-month"
  private val endYearInputName = "endAmount-year"

  private def cyaModel(employerName: String): EmploymentCYAModel = EmploymentCYAModel(EmploymentDetails(
    employerName,
    employerRef = Some("123/12345"),
    didYouLeaveQuestion = Some(true),
    currentDataIsHmrcHeld = true,
    payrollId = Some("12345"),
    taxablePayToDate = Some(5),
    totalTaxToDate = Some(5)
  ))

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

  private val employmentDetailsWithCessationDate = anEmploymentUserData.copy(
    employment = anEmploymentCYAModel.copy(employmentDetails = anEmploymentDetails.copy(
      didYouLeaveQuestion = Some(true),
      cessationDate = Some(s"$taxYearEOY-12-12"),
      payrollId = Some("payrollId"))))


  ".show" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'employment dates' page with the correct content and the date prefilled when its already in session" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentDetailsWithCessationDate)
            urlGet(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(forExample, startForExampleSelector, "forStart")
          inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "11")
          inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "11")
          inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, s"${taxYearEOY - 1}")
          textOnPageCheck(forExample, endForExampleSelector, "forEnd")
          inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "12")
          inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "12")
          inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }


        "render the 'employment dates' page with an empty form" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
            authoriseAgentOrIndividual(isAgent = false)
            urlGet(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
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
          formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }
      }
    }


    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employmentDatesUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }


  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        s"return a BAD_REQUEST($BAD_REQUEST) status" when {
          "the start day is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.startEmptyDayError, Selectors.startDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.startEmptyDayError, Some("startAmount"))
          }

          "the start month is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.startEmptyMonthError, Selectors.startMonthSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.startEmptyMonthError, Some("startAmount"))
          }

          "the start year is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> "",
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "")
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.startEmptyYearError, Selectors.startYearSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.startEmptyYearError, Some("startAmount"))
          }

          "the start day and month are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "",
              EmploymentDatesForm.startAmountMonth -> "",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.startEmptyDayMonthError, Selectors.startDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.startEmptyDayMonthError, Some("startAmount"))
          }

          "the start day and year are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> "",
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "")
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.startEmptyDayYearError, Selectors.startDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.startEmptyDayYearError, Some("startAmount"))
          }

          "the start year and month are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "",
              EmploymentDatesForm.startAmountYear -> "",
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "")
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.startEmptyMonthYearError, Selectors.startMonthSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.startEmptyMonthYearError, Some("startAmount"))
          }

          "the start day, month and year are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "",
              EmploymentDatesForm.startAmountMonth -> "",
              EmploymentDatesForm.startAmountYear -> "",
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "")
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.startEmptyAllError, Selectors.startDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.startEmptyAllError, Some("startAmount"))
          }

          "the start day is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "abc",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "abc")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidStartDateError, Selectors.startDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidStartDateError, Some("startAmount"))
          }

          "the start month is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "abc",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "abc")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidStartDateError, Selectors.startDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidStartDateError, Some("startAmount"))
          }

          "the start year is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> "abc",
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "abc")
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidStartDateError, Selectors.startDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidStartDateError, Some("startAmount"))
          }

          "the start date is an invalid date i.e. month is set to 13" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "13",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "13")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidStartDateError, Selectors.startDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidStartDateError, Some("startAmount"))
          }

          "the start date is too long ago (must be after 1st January 1900)" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> "1899",
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, "1899")
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "03")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.startTooLongAgoDateError, Selectors.startDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.startTooLongAgoDateError, Some("startAmount"))
          }

          "the start date and the end dates are too recent i.e. after 5th April" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "06",
              EmploymentDatesForm.startAmountMonth -> "04",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "04",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
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
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            multipleErrorCheck(List((user.specificExpectedResults.get.startTooRecentDateError, Selectors.startDaySelector),
              (user.specificExpectedResults.get.leaveTooRecentDateError, Selectors.endDaySelector)), user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.startTooRecentDateError, Some("startAmount"))
          }

          "the start date and the end date are not in the past" which {
            val nowDatePlusOne = LocalDate.now().plusDays(1)
            val nowDatePlusTwo = LocalDate.now().plusDays(1)

            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> nowDatePlusOne.getDayOfMonth.toString,
              EmploymentDatesForm.startAmountMonth -> nowDatePlusOne.getMonthValue.toString,
              EmploymentDatesForm.startAmountYear -> nowDatePlusOne.getYear.toString,
              EmploymentDatesForm.endAmountDay -> nowDatePlusTwo.getDayOfMonth.toString,
              EmploymentDatesForm.endAmountMonth -> nowDatePlusOne.getMonthValue.toString,
              EmploymentDatesForm.endAmountYear -> nowDatePlusOne.getYear.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
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
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            multipleErrorCheck(List((user.specificExpectedResults.get.startFutureDateError, Selectors.startDaySelector),
              (user.specificExpectedResults.get.leaveFutureDateError, Selectors.endDaySelector)), user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.startFutureDateError, Some("startAmount"))
          }

          "the start date is after the leave date" which {

            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "04",
              EmploymentDatesForm.startAmountMonth -> "04",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "03",
              EmploymentDatesForm.endAmountMonth -> "03",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            "has an OK status" in {
              result.status shouldBe BAD_REQUEST
            }

            titleCheck(expectedErrorTitle, user.isWelsh)
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
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)


            errorSummaryCheck(user.specificExpectedResults.get.leaveBeforeStartDate, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveBeforeStartDate, Some("endAmount"))

          }

          "the end day is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "",
              EmploymentDatesForm.endAmountMonth -> "06",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "06")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveEmptyDayError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveEmptyDayError, Some("endAmount"))
          }

          "the end month is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveEmptyMonthError, Selectors.endMonthSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveEmptyMonthError, Some("endAmount"))
          }

          "the end year is empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "06",
              EmploymentDatesForm.endAmountYear -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "06")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveEmptyYearError, Selectors.endYearSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveEmptyYearError, Some("endAmount"))
          }

          "the end day and month are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "",
              EmploymentDatesForm.endAmountMonth -> "",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveEmptyDayMonthError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveEmptyDayMonthError, Some("endAmount"))
          }

          "the end day and year are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "",
              EmploymentDatesForm.endAmountMonth -> "06",
              EmploymentDatesForm.endAmountYear -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "06")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveEmptyDayYearError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveEmptyDayYearError, Some("endAmount"))
          }

          "the end year and month are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "",
              EmploymentDatesForm.endAmountYear -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveEmptyMonthYearError, Selectors.endMonthSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveEmptyMonthYearError, Some("endAmount"))
          }

          "the end day, month and year are empty" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "",
              EmploymentDatesForm.endAmountMonth -> "",
              EmploymentDatesForm.endAmountYear -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveEmptyAllError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveEmptyAllError, Some("endAmount"))
          }

          "the end day is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "abc",
              EmploymentDatesForm.endAmountMonth -> "06",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "abc")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "06")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidLeaveDateError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidLeaveDateError, Some("endAmount"))
          }

          "the end month is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "abc",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "abc")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidLeaveDateError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidLeaveDateError, Some("endAmount"))
          }

          "the end year is invalid" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "06",
              EmploymentDatesForm.endAmountYear -> "abc")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "06")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "abc")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidLeaveDateError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidLeaveDateError, Some("endAmount"))
          }

          "the end date is an invalid date i.e. month is set to 13" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "13",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "13")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, taxYearEOY.toString)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.invalidLeaveDateError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidLeaveDateError, Some("endAmount"))
          }

          "the end date data is too long ago (must be after 1st January 1900)" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
              EmploymentDatesForm.startAmountMonth -> "01",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "06",
              EmploymentDatesForm.endAmountYear -> "1899")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(forExample, startForExampleSelector)
            inputFieldValueCheck(startDayInputName, Selectors.startDaySelector, "01")
            inputFieldValueCheck(startMonthInputName, Selectors.startMonthSelector, "01")
            inputFieldValueCheck(startYearInputName, Selectors.startYearSelector, taxYearEOY.toString)
            inputFieldValueCheck(endDayInputName, Selectors.endDaySelector, "06")
            inputFieldValueCheck(endMonthInputName, Selectors.endMonthSelector, "06")
            inputFieldValueCheck(endYearInputName, Selectors.endYearSelector, "1899")
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveTooLongAgoDateError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveTooLongAgoDateError, Some("endAmount"))
          }

          "the end date is a too recent date i.e. after 5th April" which {
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "05",
              EmploymentDatesForm.startAmountMonth -> "03",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> "06",
              EmploymentDatesForm.endAmountMonth -> "04",
              EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
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
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveTooRecentDateError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveTooRecentDateError, Some("endAmount"))
          }

          "the end date is not in the past" which {
            val nowDatePlusOne = LocalDate.now().plusDays(1)
            lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "06",
              EmploymentDatesForm.startAmountMonth -> "03",
              EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
              EmploymentDatesForm.endAmountDay -> nowDatePlusOne.getDayOfMonth.toString,
              EmploymentDatesForm.endAmountMonth -> nowDatePlusOne.getMonthValue.toString,
              EmploymentDatesForm.endAmountYear -> nowDatePlusOne.getYear.toString)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentDetailsWithCessationDate)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(expectedErrorTitle, user.isWelsh)
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
            formPostLinkCheck(employmentDatesUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.leaveFutureDateError, Selectors.endDaySelector)
            errorAboveElementCheck(user.specificExpectedResults.get.leaveFutureDateError, Some("endAmount"))
          }

        }
      }
    }

    "redirect to the check details page when the form is fully filled out" which {
      lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
        EmploymentDatesForm.startAmountMonth -> "01",
        EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
        EmploymentDatesForm.endAmountDay -> "06",
        EmploymentDatesForm.endAmountMonth -> "03",
        EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentDetailsWithCessationDate)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employmentDatesUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }
}
