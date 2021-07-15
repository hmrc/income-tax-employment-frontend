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

import models.User
import models.mongo.EmploymentUserData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class AmountOfTipsOnP60ControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper{

  val amountOfTipsOnP60PageUrl = s"$appUrl/2021/amount-of-payments-not-on-p60?employmentId=employmentId"
  val continueLink = "/income-through-software/return/employment-income/2021/amount-of-payments-not-on-p60?employmentId=employmentId"

  val amount: String = "100"
  val maxLimit: String = "100,000,000,000"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > label > header > p"
    val forExampleSelector: String = "#amount-hint"
    val inputFieldSelector: String = "#amount"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val ifItWasNotSelector: String = "#main-content > div > div > form > div > label > p"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
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
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "What is the total amount of payments not included on your P60?"
    val expectedH1 = "What is the total amount of payments not included on your P60?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of payments not included on your P60"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "What is the total amount of payments not included on your client’s P60?"
    val expectedH1 = "What is the total amount of payments not included on your client’s P60?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of payments not included on your client’s P60"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "What is the total amount of payments not included on your client’s P60?"
    val expectedH1 = "What is the total amount of payments not included on your client’s P60?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of payments not included on your client’s P60"
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

  val employmentUserDataWithP60s: EmploymentUserData = employmentUserData.copy(employment = employmentUserData.employment.copy(employmentUserData.employment.employmentDetails.copy(tipsAndOtherPayments = Some(100.00))))

  ".show" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'amount of tips on p60' page with the correct content" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData, User(mtditid,if(user.isAgent) Some("12345678") else None,nino,sessionId,if(user.isAgent) "Agent" else "Individual")(fakeRequest))
            urlGet(amountOfTipsOnP60PageUrl, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
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

        "render the 'amount of tips on p60' page with the correct content when theres a previous amount" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserDataWithP60s, User(mtditid,if(user.isAgent) Some("12345678") else None,nino,sessionId,if(user.isAgent) "Agent" else "Individual")(fakeRequest))
            urlGet(amountOfTipsOnP60PageUrl, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(2021)))
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
      }
    }
  }

  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an OK" in {
          lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq(amount))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(amountOfTipsOnP60PageUrl, body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "the submitted data is empty" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq(""))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(amountOfTipsOnP60PageUrl, body = form, follow = false, welsh = user.isWelsh,
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
              urlPost(amountOfTipsOnP60PageUrl, body = form, follow = false, welsh = user.isWelsh,
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
              urlPost(amountOfTipsOnP60PageUrl, body = form, follow = false, welsh = user.isWelsh,
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