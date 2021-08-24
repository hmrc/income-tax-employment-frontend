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

import controllers.employment.routes.{CheckEmploymentDetailsController, OtherPaymentsController}
import models.User
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherPaymentsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper{

  val employmentId = "001"
  val otherPaymentsAmountPageUrl = s"$appUrl/2021/amount-of-payments-not-on-p60?employmentId=$employmentId"
  val continueLink = "/income-through-software/return/employment-income/2021/amount-of-payments-not-on-p60?employmentId=001"

  val amount: String = "100"
  val maxLimit: String = "100,000,000,000"

  val taxYearEOY = taxYear-1

  implicit val request = FakeRequest()
  private val userRequest: User[_]=  User(mtditid, None, nino, sessionId, affinityGroup)

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > label > header > p"
    val forExampleSelector: String = "#amount-hint"
    val inputFieldSelector: String = "#amount"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val ifItWasNotSelector: String = "#main-content > div > div > form > div > label > p"
    val inputAmountField:String = "#amount"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedOtherPaymentsPageHeader: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedInputName: String
    val expectedButtonText: String
    val expectedErrorCharLimit: String
    val expectedErrorInvalidAmount: String
    val expectedForExample: String
    val expectedIfItWasNot: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "What is the total amount of payments not included on your P60?"
    val expectedH1 = "What is the total amount of payments not included on your P60?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of payments not included on your P60"
    val expectedOtherPaymentsPageHeader = "Did you receive any payments that are not on your P60?"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "What is the total amount of payments not included on your P60?"
    val expectedH1 = "What is the total amount of payments not included on your P60?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of payments not included on your P60"
    val expectedOtherPaymentsPageHeader = "Did you receive any payments that are not on your P60?"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "What is the total amount of payments not included on your client’s P60?"
    val expectedH1 = "What is the total amount of payments not included on your client’s P60?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of payments not included on your client’s P60"
    val expectedOtherPaymentsPageHeader = "Did your client receive any payments that are not on their P60?"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "What is the total amount of payments not included on your client’s P60?"
    val expectedH1 = "What is the total amount of payments not included on your client’s P60?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of payments not included on your client’s P60"
    val expectedOtherPaymentsPageHeader = "Did your client receive any payments that are not on their P60?"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = "Employment for 6 April 2020 to 5 April 2021"
    val expectedInputName = "amount"
    val expectedButtonText = "Continue"
    val expectedErrorCharLimit = "The amount of payments must be less than £100,000,000,000"
    val expectedErrorInvalidAmount = "Enter the amount of payments in the correct format"
    val expectedForExample = "For example, £600 or £193.54"
    val expectedIfItWasNot = s"If it was not £$amount, tell us the correct amount."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = "Employment for 6 April 2020 to 5 April 2021"
    val expectedInputName = "amount"
    val expectedButtonText = "Continue"
    val expectedErrorCharLimit = "The amount of payments must be less than £100,000,000,000"
    val expectedErrorInvalidAmount = "Enter the amount of payments in the correct format"
    val expectedForExample = "For example, £600 or £193.54"
    val expectedIfItWasNot = s"If it was not £$amount, tell us the correct amount."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  def cya(tipsQuestion:Option[Boolean]=Some(true), tipsAmount:Option[BigDecimal]=None, isPriorSubmission:Boolean=true): EmploymentUserData = {
    EmploymentUserData (sessionId, mtditid,nino, taxYearEOY, "001", isPriorSubmission,
    EmploymentCYAModel(
      EmploymentDetails("maggie", tipsAndOtherPaymentsQuestion = tipsQuestion, tipsAndOtherPayments = tipsAmount, currentDataIsHmrcHeld = false),
      None
    )
    )
  }
  val multipleEmployments = fullEmploymentsModel(Seq(employmentDetailsAndBenefits(employmentId = "002"),
    employmentDetailsAndBenefits(tipsAndOtherPay=Some(100.00))))


  ".show" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'other payments amount' page with the correct content" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(), userRequest)
            urlGet(otherPaymentsAmountPageUrl, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedForExample, forExampleSelector)
          inputFieldCheck(expectedInputName, inputFieldSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'other payments amount' page with the correct content when theres a previous amount" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(tipsAmount=Some(100.00)), userRequest)
            urlGet(otherPaymentsAmountPageUrl, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedIfItWasNot, ifItWasNotSelector)
          textOnPageCheck(expectedForExample, forExampleSelector)
          inputFieldCheck(expectedInputName, inputFieldSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "The input field" should {

          "be empty" when {
            "there is cya data with taxToDate field empty and no prior(i.e. user is adding a new employment)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(tipsAmount = None, isPriorSubmission = false), User(mtditid, None, nino, sessionId, "test")(fakeRequest))
                urlGet(otherPaymentsAmountPageUrl, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck("", Selectors.inputAmountField)

            }


            "cya data and prior data are the same(i.e. user has clicked on change link)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(userData(multipleEmployments), nino, taxYearEOY)
                insertCyaData(cya(tipsAmount = Some(100.00)), User(mtditid, None, nino, sessionId, "test")(fakeRequest))
                urlGet(otherPaymentsAmountPageUrl, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck("", Selectors.inputAmountField)

            }
          }

          "be filled" when {
            "cya data and prior data differ (i.e user has updated their pay)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(userData(multipleEmployments), nino, taxYearEOY)
                insertCyaData(cya(tipsAmount = Some(100.10)), User(mtditid, None, nino, sessionId, "test")(fakeRequest))
                urlGet(otherPaymentsAmountPageUrl, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck("100.10", Selectors.inputAmountField)
            }

            "cya amount field is filled and prior data is none (i.e user has added a new employment and updated their tips but now want to change it)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(tipsQuestion=Some(true), tipsAmount = Some(100.00), isPriorSubmission = false), User(mtditid, None, nino, sessionId, "agent"))
                urlGet(otherPaymentsAmountPageUrl, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck("100", Selectors.inputAmountField)
            }
          }
        }

        "redirect to the CheckYourEmploymentDetails page there is no CYA data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            urlGet(otherPaymentsAmountPageUrl, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to OtherPayments not on P60 page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url)
          }
        }

        "redirect to the 'did you receive any payments that are not on your p60' page when they've previously answered no" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(tipsQuestion = Some(false)), userRequest)
            urlGet(otherPaymentsAmountPageUrl, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to OtherPayments not on P60 page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(OtherPaymentsController.show(taxYearEOY, employmentId).url)
          }
        }

        "redirect to the 'did you receive any payments that are not on your p60' page when they've not answered the previous question" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(tipsQuestion = Some(false)), User(mtditid,None,nino,sessionId,"Individual")(fakeRequest))
            urlGet(otherPaymentsAmountPageUrl, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }
          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to OtherPayments not on P60 page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(OtherPaymentsController.show(taxYearEOY, employmentId).url)
          }
        }
      }
    }
  }

  ".submit" should {

    val validAmountForm = Map("amount" -> Seq("2000.53"))

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "update cya when the previous question has been answered(i.e. journey is valid)" in {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(tipsQuestion = Some(true)), userRequest)
            urlPost(otherPaymentsAmountPageUrl, body = validAmountForm, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))

          lazy val updatedCya = findCyaData(taxYearEOY, employmentId, userRequest)

          updatedCya.get.employment.employmentDetails.tipsAndOtherPayments.get shouldBe 2000.53
        }

        "redirect to check your employment details page when there is no cya data" when{

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            urlPost(otherPaymentsAmountPageUrl, body = validAmountForm, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to Check Employment Details page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url)
          }
        }

        "redirect to the 'did you receive any payments that are not on your p60' page" when {
          "they attempt to submit amount when they've previously answered no" which {
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              insertCyaData(cya(tipsQuestion = Some(false)), userRequest)
              urlPost(otherPaymentsAmountPageUrl, body = validAmountForm, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has an SEE_OTHER status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to OtherPayments not on P60 page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(OtherPaymentsController.show(taxYearEOY, employmentId).url)
            }
          }

          "they attempt to submit amount when they've not answered the payments not on P60 question" which {
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              insertCyaData(cya(tipsQuestion = None), userRequest)
              urlPost(otherPaymentsAmountPageUrl, body = validAmountForm, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has an SEE_OTHER status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to OtherPayments not on P60 page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(OtherPaymentsController.show(2021, employmentId).url)
            }
          }
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "the submitted data is empty" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq(""))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(otherPaymentsAmountPageUrl, body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedForExample, forExampleSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Selectors.inputFieldSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq(maxLimit))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(otherPaymentsAmountPageUrl, body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedForExample, forExampleSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedErrorCharLimit, Selectors.inputFieldSelector)
            errorAboveElementCheck(expectedErrorCharLimit)
          }

          "the submitted data is an invalid format" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq("abc"))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(otherPaymentsAmountPageUrl, body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedForExample, forExampleSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedErrorInvalidAmount, Selectors.inputFieldSelector)
            errorAboveElementCheck(expectedErrorInvalidAmount)
          }
        }
      }
    }
  }
}