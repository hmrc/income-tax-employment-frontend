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
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class PayeRefControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper  {

  val taxYearEOY = taxYear - 1
  val payeRef: String = "123/AA12345"
  def url (taxYear:Int): String = s"$appUrl/${taxYear.toString}/employer-paye-reference?employmentId=001"

  val continueButtonLink: String = "/income-through-software/return/employment-income/2021/employer-paye-reference?employmentId=001"

  implicit val request = FakeRequest()
  private val userRequest: User[_]=  User(mtditid, None, nino, sessionId, affinityGroup)

  object Selectors {
    val contentSelector = "#main-content > div > div > form > div > label > p"
    val headingSelector = "#main-content > div > div > header > h1"
    val captionSelector = "#main-content > div > div > header > p"
    val hintTestSelector = "#payeRef-hint"
    val inputSelector = "#payeRef"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#payeRef"
    val inputAmountField = "#payeRef"
  }

  val amountInputName = "payeRef"

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedContentNewAccount: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedH1: String
    val expectedContent: String
    val continueButtonText: String
    val hintText: String
    val emptyErrorText: String
    val wrongFormatErrorText: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = (taxYear: Int) => s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedH1: String = "What’s the PAYE reference of maggie?"
    val continueButtonText = "Continue"
    val hintText = "For example, 123/AB456"
    val expectedContent: String = "If the PAYE reference is not 123/AA12345, tell us the correct reference."
    val emptyErrorText: String = "Enter a PAYE reference"
    val wrongFormatErrorText: String = "Enter a PAYE reference in the correct format"

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = (taxYear: Int) => s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedH1: String = "What’s the PAYE reference of maggie?"
    val continueButtonText = "Continue"
    val hintText = "For example, 123/AB456"
    val expectedContent: String = "If the PAYE reference is not 123/AA12345, tell us the correct reference."
    val expectedContentNewAccount: String = "You can find this on your P60 or on letters about PAYE. It may be called ‘Employer PAYE reference’ or ‘PAYE reference’."
    val emptyErrorText: String = "Enter a PAYE reference"
    val wrongFormatErrorText: String = "Enter a PAYE reference in the correct format"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s the PAYE reference of your employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentNewAccount: String = "You can find this on your P60 or on letters about PAYE. It may be called ‘Employer PAYE reference’ or ‘PAYE reference’."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s the PAYE reference of your client’s employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentNewAccount: String = "You can find this on P60 forms or on letters about PAYE. It may be called ‘Employer PAYE reference’ or ‘PAYE reference’."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "What’s the PAYE reference of your employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentNewAccount: String = "You can find this on your P60 or on letters about PAYE. It may be called ‘Employer PAYE reference’ or ‘PAYE reference’."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "What’s the PAYE reference of your client’s employer?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentNewAccount: String = "You can find this on P60 forms or on letters about PAYE. It may be called ‘Employer PAYE reference’ or ‘PAYE reference’."
  }

  object CyaModel {
    val cya = EmploymentUserData (sessionId, mtditid,nino, taxYearEOY, "001", true,
      EmploymentCYAModel(
        EmploymentDetails("maggie", employerRef = Some("123/AA12345"), currentDataIsHmrcHeld = false),
        None
      )
    )

  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {


        "should render What's the PAYE reference of xxx? page with cya payeRef in paragraph text when there is cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, userRequest)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYear))
          textOnPageCheck(expectedContent, contentSelector)
          textOnPageCheck(hintText, hintTestSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck(payeRef, inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render What's the PAYE reference of xxx? page with generic paragraph text when user is adding a new employment" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val newCya = CyaModel.cya.copy(employment = CyaModel.cya.employment.copy
            (employmentDetails = CyaModel.cya.employment.employmentDetails.copy(employerRef = None)))
            insertCyaData(newCya, userRequest)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(hintText, hintTestSelector)
          textOnPageCheck(get.expectedContentNewAccount, contentSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }
        "redirect  to check employment details page when there is no cya data in session" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            urlGet(url(taxYearEOY), follow = false, welsh=user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }


          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/check-employment-details?employmentId=001")
          }
        }

        "redirect  to overview page if the user tries to hit this page with current taxYear" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, userRequest)
            urlGet(url(taxYear), welsh=user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }


          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
          }
        }

      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "should render What's the PAYE reference of xxx? page with empty error text when there no input" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, userRequest)
            urlPost(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck("", inputAmountField)
          errorSummaryCheck(emptyErrorText, expectedErrorHref)
        }

        "should render What's the PAYE reference of xxx? page with wrong format text when input is in incorrect format" which {

          val invalidPaye = "123/abc 001<Q>"

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, userRequest)
            urlPost(url(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("payeRef" -> invalidPaye))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck(invalidPaye, inputAmountField)
          errorSummaryCheck(wrongFormatErrorText, expectedErrorHref)
        }

        "redirect to Overview page when a valid form is submitted" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, userRequest)
            urlPost(url(taxYearEOY), follow=false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("payeRef" -> payeRef))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/check-employment-details?employmentId=001")
          }
        }
      }
    }
  }
}
