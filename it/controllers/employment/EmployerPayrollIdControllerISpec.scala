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

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import models.User
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerPayrollIdControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId = "employmentId"
  private val continueButtonLink: String = "/update-and-submit-income-tax-return/employment-income/2021/payroll-id?employmentId=" + employmentId

  private def url(taxYear: Int): String = s"$appUrl/${taxYear.toString}/payroll-id?employmentId=$employmentId"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private val userRequest: User[_] = User(mtditid, None, nino, sessionId, affinityGroup)

  object Selectors {
    val paragraph1Selector = "#main-content > div > div > form > div > label > p:nth-child(2)"
    val paragraph2Selector = "#main-content > div > div > form > div > label > p:nth-child(3)"
    val paragraph3Selector = "#main-content > div > div > form > div > label > p:nth-child(4)"
    val paragraph4Selector = "#main-content > div > div > form > div > label > p:nth-child(5)"
    val hintTextSelector = "#payrollId-hint"
    val inputSelector = "#payrollId"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#payrollId"
    val inputAmountField = "#payrollId"

    def bulletSelector(bulletNumber: Int): String =
      s"#main-content > div > div > form > div > label > ul > li:nth-child($bulletNumber)"
  }

  val inputName: String = "payrollId"

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val emptyErrorText: String
    val wrongFormatErrorText: String
    val tooLongErrorText: String
    val paragraph1: String
    val paragraph2: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val hintText: String
    val bullet1: String
    val bullet2: String
    val bullet3: String
    val previousParagraph: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, 123456"
    val bullet1: String = "upper and lower case letters (a to z)"
    val bullet2: String = "numbers"
    val bullet3: String = "the special characters: .,-()/=!\"%&*;<>'+:\\?"
    val previousParagraph: String = "If the payroll ID is not 123456, tell us the correct ID."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, 123456"
    val bullet1: String = "upper and lower case letters (a to z)"
    val bullet2: String = "numbers"
    val bullet3: String = "the special characters: .,-()/=!\"%&*;<>'+:\\?"
    val previousParagraph: String = "If the payroll ID is not 123456, tell us the correct ID."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your payroll ID for this employment?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "What’s your payroll ID for this employment?"
    val emptyErrorText: String = "Enter your payroll ID"
    val wrongFormatErrorText: String = "Enter your payroll ID in the correct format"
    val tooLongErrorText: String = "Your payroll ID must be 38 characters or fewer"
    val paragraph1: String = "Your payroll ID must be 38 characters or fewer. It can include:"
    val paragraph2: String = "You can find this on your payslip or on your P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your client’s payroll ID for this employment?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "What’s your client’s payroll ID for this employment?"
    val emptyErrorText: String = "Enter your client’s payroll ID"
    val wrongFormatErrorText: String = "Enter your client’s payroll ID in the correct format"
    val tooLongErrorText: String = "Your client’s payroll ID must be 38 characters or fewer"
    val paragraph1: String = "Your client’s payroll ID must be 38 characters or fewer. It can include:"
    val paragraph2: String = "You can find this on your client’s payslip or on their P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your payroll ID for this employment?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "What’s your payroll ID for this employment?"
    val emptyErrorText: String = "Enter your payroll ID"
    val wrongFormatErrorText: String = "Enter your payroll ID in the correct format"
    val tooLongErrorText: String = "Your payroll ID must be 38 characters or fewer"
    val paragraph1: String = "Your payroll ID must be 38 characters or fewer. It can include:"
    val paragraph2: String = "You can find this on your payslip or on your P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your client’s payroll ID for this employment?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "What’s your client’s payroll ID for this employment?"
    val emptyErrorText: String = "Enter your client’s payroll ID"
    val wrongFormatErrorText: String = "Enter your client’s payroll ID in the correct format"
    val tooLongErrorText: String = "Your client’s payroll ID must be 38 characters or fewer"
    val paragraph1: String = "Your client’s payroll ID must be 38 characters or fewer. It can include:"
    val paragraph2: String = "You can find this on your client’s payslip or on their P60. It’s also known as a ‘payroll number’."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render the What's your payrollId? page with the correct content when theres no payrollId in cya" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false), userRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.paragraph1, paragraph1Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph3Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render the What's your payrollId? page with the id pre-filled when theres payrollId data in cya" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val employmentDetails = anEmploymentDetails.copy("maggie", payrollId = Some("123456"), currentDataIsHmrcHeld = false)
            insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentDetails)), userRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(previousParagraph, paragraph1Selector)
          textOnPageCheck(get.paragraph1, paragraph2Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph4Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "123456")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "redirect to check employment details page when there is no cya data in session" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("/update-and-submit-income-tax-return/employment-income/2021/check-employment-details?employmentId=" + employmentId)
          }
        }

        "redirect to overview page if the user tries to hit this page with current taxYear" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(anEmploymentDetails)), userRequest)
            urlGet(url(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("http://localhost:11111/update-and-submit-income-tax-return/2022/view")
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
        "should render the What's your payrollId? page with an error when the payrollId is input as empty" which {
          val payrollId = ""
          val body = Map("payrollId" -> payrollId)
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(anEmploymentUserData, userRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlPost(url(taxYearEOY), body, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has a BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.paragraph1, paragraph1Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph3Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, payrollId)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(get.emptyErrorText)
        }

        "should render the What's your payrollId? page with an error when the payrollId is input as too long" which {
          val payrollId = "123456789012345678901234567890123456789"
          val body = Map("payrollId" -> payrollId)
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(anEmploymentUserData, userRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlPost(url(taxYearEOY), body, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has a BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.paragraph1, paragraph1Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph3Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, payrollId)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.tooLongErrorText, expectedErrorHref)
          errorAboveElementCheck(get.tooLongErrorText)
        }

        "should render the What's your payrollId? page with an error when the payrollId is input as the wrong format" which {
          val payrollId = "$11223"
          val body = Map("payrollId" -> payrollId)
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(anEmploymentUserData, userRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlPost(url(taxYearEOY), body, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has a BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.paragraph1, paragraph1Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph3Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, payrollId)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(get.wrongFormatErrorText)
        }

        "should update the payrollId when a valid payrollId is submitted and redirect to the check your details controller" when {
          val payrollId = "123456"
          val body = Map("payrollId" -> payrollId)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData, userRequest)
            urlPost(url(taxYearEOY), body, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to the Check Employment Details page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some("/update-and-submit-income-tax-return/employment-income/2021/check-employment-details?employmentId=" + employmentId)
          }

          s"update the cya models payroll id to be $payrollId" in {
            lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyaModel.employment.employmentDetails.payrollId shouldBe Some(payrollId)
          }
        }
      }
    }
  }
}
