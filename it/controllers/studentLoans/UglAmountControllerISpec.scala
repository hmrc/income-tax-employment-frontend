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

package controllers.studentLoans

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithDetails
import models.User
import models.mongo.EmploymentUserData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.PageUrls.{checkYourDetailsUrl, fullUrl, overviewUrl, studentLoansCyaPage, studentLoansUglAmountUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class UglAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  lazy val wsClientFeatureSwitchOff: WSClient = appWithFeatureSwitchesOff.injector.instanceOf[WSClient]

  val employmentId: String = "1234567890-0987654321"

  def url(taxYearUnique: Int): String = fullUrl(studentLoansUglAmountUrl(taxYearUnique, employmentId))

  private val taxYearEOY: Int = taxYear - 1
  private val amount: BigDecimal = 100

  private implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private val userRequest: User[_] = User(mtditid, None, nino, sessionId, affinityGroup)

  object Selectors {
    val contentCheckSelector = "#main-content > div > div > form > div > label > p:nth-child(2)"
    val contentExampleSelector = "#main-content > div > div > form > div > label > p:nth-child(3)"
    val headingSelector = "#main-content > div > div > form > div > label > header > h1"
    val captionSelector = "#main-content > div > div > form > div > label > header > p"
    val hintTestSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#amount"
  }

  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedContentCheck: String
    val emptyErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val expectedContentExample: String
    val hintText: String
    val wrongFormatErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Student Loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £193.54"
    val expectedContentExample = "Undergraduate loan covers courses like undergraduate degrees (BA, BSc), foundation degrees or Certificates of Higher Education (CertHE)."
    val wrongFormatErrorText: String = "Enter the amount of undergraduate loan in the correct format"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Student Loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £193.54"
    val expectedContentExample = "Undergraduate loan covers courses like undergraduate degrees (BA, BSc), foundation degrees or Certificates of Higher Education (CertHE)."
    val wrongFormatErrorText: String = "Enter the amount of undergraduate loan in the correct format"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "How much undergraduate loan did you repay while employed by ABC Digital Ltd?"
    val expectedTitle: String = "How much student loan repayment have you made?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentCheck: String = "Check with the Student Loans Company, your payslips or P60."
    val emptyErrorText: String = "Enter the amount of undergraduate loan you repaid while employed by ABC Digital Ltd"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "Enter the amount of undergraduate loan your client repaid while employed by ABC Digital Ltd"
    val expectedTitle: String = "How much student loan repayment has your client made?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentCheck: String = "Check with the Student Loans Company, your client's payslips or P60."
    val emptyErrorText: String = "Enter the amount of undergraduate loan your client repaid while employed by ABC Digital Ltd"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "How much undergraduate loan did you repay while employed by ABC Digital Ltd?"
    val expectedTitle: String = "How much student loan repayment have you made?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentCheck: String = "Check with the Student Loans Company, your payslips or P60."
    val emptyErrorText: String = "Enter the amount of undergraduate loan you repaid while employed by ABC Digital Ltd"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Enter the amount of undergraduate loan your client repaid while employed by ABC Digital Ltd"
    val expectedTitle: String = "How much student loan repayment has your client made?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContentCheck: String = "Check with the Student Loans Company, your client's payslips or P60."
    val emptyErrorText: String = "Enter the amount of undergraduate loan your client repaid while employed by ABC Digital Ltd"
  }

  def cya(payToDate: Option[BigDecimal] = Some(100), isPriorSubmission: Boolean = true): EmploymentUserData =
    anEmploymentUserDataWithDetails(
      anEmploymentDetails.copy("maggie", taxablePayToDate = payToDate),
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = isPriorSubmission
    )

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val multipleEmployments = anAllEmploymentData.copy(hmrcEmploymentData = Seq(
    anEmploymentSource.copy(employmentBenefits = None),
    anEmploymentSource.copy(employmentId = "002", employmentBenefits = None)
  ))

  ".show" when {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "How much undergraduate loan did you repay while employed by XXX?" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(cya(), User(mtditid, None, nino, sessionId, "agent"))
            urlGet(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContentCheck, contentCheckSelector)
          textOnPageCheck(expectedContentExample, contentExampleSelector)
          textOnPageCheck(hintText, hintTestSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(studentLoansUglAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "How much undergraduate loan did you repay while employed by XXX? 2 " which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(cya(None), User(mtditid, None, nino, sessionId, "agent"))
            urlGet(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(hintText, hintTestSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(studentLoansUglAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

        "The input field" should {

          "be empty" when {
            "there is cya data with pay field empty and no prior" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(payToDate = None, isPriorSubmission = false), User(mtditid, None, nino, sessionId, "agent"))
                urlGet(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputSelector, "")
            }


            "cya data and prior data are the same(i.e. user has clicked on change link)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYearEOY)
                insertCyaData(cya(), User(mtditid, None, nino, sessionId, "agent"))
                urlGet(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputSelector, "")
            }
          }

          "be filled" when {
            "cya data and prior data differ (i.e user has updated their repayment amount)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYearEOY)
                insertCyaData(cya(Some(110.00)), User(mtditid, None, nino, sessionId, "agent"))
                urlGet(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputSelector, "110")
            }

            "cya amount field is filled and prior data is none (i.e user has added undergraduate loan repayment but now want to change it)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(Some(100.00), isPriorSubmission = false), User(mtditid, None, nino, sessionId, "agent"))
                urlGet(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputSelector, "100")
            }
          }
        }
        "redirect to check student loans page when there is no cya data in session" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            urlGet(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }


          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(studentLoansCyaPage(taxYearEOY, employmentId)) shouldBe true
          }
        }

        "redirect  to overview page if the user tries to hit this page with current taxYear" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(), User(mtditid, None, nino, sessionId, "agent"))
            val inYearUrl = s"$appUrl/$taxYear/how-much-pay?employmentId=$employmentId"
            urlGet(inYearUrl, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }


          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(overviewUrl(taxYear)) shouldBe true
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
            insertCyaData(cya(), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
            urlPost(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
        }

        "should render How much did xxx pay you? page with wrong format text when input is in incorrect format" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(), User(mtditid, None, nino, sessionId, "agent"))
            urlPost(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "|"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck(amountInputName, inputSelector, "|")
          errorSummaryCheck(wrongFormatErrorText, expectedErrorHref)
        }

        "should render How much did xxx pay you? page with max error ext when input > 100,000,000,000" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(), User(mtditid, None, nino, sessionId, "agent"))
            urlPost(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)),
              body = Map("amount" -> "9999999999999999999999999999"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck(amountInputName, inputSelector, "9999999999999999999999999999")
        }

        "redirect to Overview page when a valid form is submitted" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(), userRequest)
            urlPost(fullUrl(studentLoansUglAmountUrl(taxYearEOY, employmentId)), follow = false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
          }
        }
      }
    }
  }
}
