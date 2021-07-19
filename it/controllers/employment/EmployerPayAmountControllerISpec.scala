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
import org.eclipse.jetty.http.HttpParser.RequestHandler
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerPayAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper  {

  val taxYearEOY = taxYear - 1
  val amount: BigDecimal = 34234.15
  val urlEOY = s"$appUrl/2021/how-much-pay?employmentId=001"

  val continueButtonLink: String = "/income-through-software/return/employment-income/2021/how-much-pay?employmentId=001"

  implicit val request = FakeRequest()
  val userRequest: User[_]=  User(mtditid, None, nino, sessionId, affinityGroup)

  object Selectors {
    val contentSelector = "#main-content > div > div > form > div > label > p"
    val headingSelector = "#main-content > div > div > header > h1"
    val captionSelector = "#main-content > div > div > header > p"
    val hintTestSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#amount"
    val inputAmountField = "#amount"
  }

  val poundPrefixText = "£"
  val amountInputName = "amount"

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedContent: String
    val expectedContentNewAccount: String
    val emptyErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val continueButtonText: String
    val hintText: String
    val wrongFormatErrorText: String
    val maxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = (taxYear: Int) => s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £600 or £193.54"
    val wrongFormatErrorText: String = "Enter the amount paid in the correct format"
    val maxAmountErrorText: String = "The amount paid must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = (taxYear: Int) => s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £600 or £193.54"
    val wrongFormatErrorText: String = "Enter the amount paid in the correct format"
    val maxAmountErrorText: String = "The amount paid must be less than £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "How much did maggie pay you?"
    val expectedTitle: String = "How much did your employer pay you?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = s"If you were not paid £$amount, tell us the correct amount."
    val expectedContentNewAccount: String = "Enter the gross amount. This can usually be found on your P60."
    val emptyErrorText: String = "Enter the amount you were paid"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "How much did maggie pay your client?"
    val expectedTitle: String = "How much did your client’s employer pay them?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "If your client was not paid £34234.15, tell us the correct amount."
    val expectedContentNewAccount: String = "Enter the gross amount. This can usually be found on your client’s P60."
    val emptyErrorText: String = "Enter the amount your client was paid"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "How much did maggie pay you?"
    val expectedTitle: String = "How much did your employer pay you?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = s"If you were not paid £$amount, tell us the correct amount."
    val expectedContentNewAccount: String = "Enter the gross amount. This can usually be found on your P60."
    val emptyErrorText: String = "Enter the amount you were paid"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "How much did maggie pay your client?"
    val expectedTitle: String = "How much did your client’s employer pay them?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "If your client was not paid £34234.15, tell us the correct amount."
    val expectedContentNewAccount: String = "Enter the gross amount. This can usually be found on your client’s P60."
    val emptyErrorText: String = "Enter the amount your client was paid"
  }

  object CyaModel {
    val cya = EmploymentUserData (sessionId, mtditid,nino, taxYearEOY, "001", true,
      EmploymentCYAModel(
        EmploymentDetails("maggie", taxablePayToDate = Some(34234.15), currentDataIsHmrcHeld = false),
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


        "should render How much did xxx pay you? page with cya amount in paragraph text when there is cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, User(mtditid, None, nino, sessionId, "agent"))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption(taxYear))
          textOnPageCheck(get.expectedContent, contentSelector)
          textOnPageCheck(hintText, hintTestSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck(amount.toString, inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render How much did xxx pay you? page with generic paragraph text when user is adding a new employment" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val newCya = CyaModel.cya.copy(employment = CyaModel.cya.employment.copy
            (employmentDetails = CyaModel.cya.employment.employmentDetails.copy(taxablePayToDate = None)))
            insertCyaData(newCya, User(mtditid, None, nino, sessionId, "agent"))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(hintText, hintTestSelector)
          textOnPageCheck(get.expectedContentNewAccount, contentSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
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
            urlGet(urlEOY, follow = false, welsh=user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
            insertCyaData(CyaModel.cya, User(mtditid, None, nino, sessionId, "agent"))
            val inYearUrl =s"$appUrl/$taxYear/how-much-pay?employmentId=001"
            urlGet(inYearUrl, welsh=user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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

        "should render How much did xxx pay you? page with empty error text when there no input" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
            urlPost(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck("", inputAmountField)
          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
        }

        "should render How much did xxx pay you? page with wrong format text when input is in incorrect format" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, User(mtditid, None, nino, sessionId, "agent"))
            urlPost(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "|"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck("", inputAmountField)
          errorSummaryCheck(wrongFormatErrorText, expectedErrorHref)
        }

        "should render How much did xxx pay you? page with max error ext when input > 100,000,000,000" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, User(mtditid, None, nino, sessionId, "agent"))
            urlPost(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)),
              body = Map("amount" -> "9999999999999999999999999999"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck("9999999999999999999999999999", inputAmountField)
          errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
        }

        "redirect to Overview page when a valid form is submitted" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(CyaModel.cya, userRequest)
            urlPost(urlEOY, follow=false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
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
