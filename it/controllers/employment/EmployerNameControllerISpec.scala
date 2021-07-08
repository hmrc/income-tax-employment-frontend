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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class EmployerNameControllerISpec extends IntegrationTest with ViewHelpers {

  val employerNamePageUrl = s"$appUrl/2022/employer-name"
  val continueLink = "/income-through-software/return/employment-income/2022/employer-name"

  val testModel = "Google"
  val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > label > header > p"
    val inputFieldSelector: String = "#name"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"

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
    val expectedErrorDuplicateName: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your employer?"
    val expectedH1 = "What’s the name of your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your employer"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your employer?"
    val expectedH1 = "What’s the name of your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your employer"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your client’s employer?"
    val expectedH1 = "What’s the name of your client’s employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your client’s employer"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your client’s employer?"
    val expectedH1 = "What’s the name of your client’s employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your client’s employer"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = "Employment for 6 April 2021 to 5 April 2022"
    val expectedInputName = "name"
    val expectedButtonText = "Continue"
    val expectedErrorCharLimit = "The employer name must be 74 characters or fewer"
    val expectedErrorDuplicateName = "You cannot add 2 employers with the same name"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = "Employment for 6 April 2021 to 5 April 2022"
    val expectedInputName = "name"
    val expectedButtonText = "Continue"
    val expectedErrorCharLimit = "The employer name must be 74 characters or fewer"
    val expectedErrorDuplicateName = "You cannot add 2 employers with the same name"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'name of your employer' page with the correct content" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(employerNamePageUrl, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
          lazy val form: Map[String, Seq[String]] = Map("name" -> Seq("employer name"))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(employerNamePageUrl, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe OK
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "the submitted data is empty" which {
            lazy val form: Map[String, Seq[String]] = Map("name" -> Seq(""))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerNamePageUrl, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Selectors.inputFieldSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, Seq[String]] = Map("name" -> Seq(charLimit))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerNamePageUrl, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedErrorCharLimit, Selectors.inputFieldSelector)
            errorAboveElementCheck(expectedErrorCharLimit)
          }

          "the submitted data is a duplicate name" which {
            lazy val form: Map[String, Seq[String]] = Map("name" -> Seq("Google"))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerNamePageUrl, body = form, follow = false, welsh = user.isWelsh, headers =
                playSessionCookie(user.isAgent, Map(EMPLOYMENT_PRIOR -> Json.toJson(testModel).toString())))
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
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedErrorDuplicateName, Selectors.inputFieldSelector)
            errorAboveElementCheck(expectedErrorDuplicateName)
          }
        }
      }
    }

  }


}
