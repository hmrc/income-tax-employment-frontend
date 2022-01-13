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

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithDetails
import models.User
import models.employment.AllEmploymentData
import models.mongo.EmploymentUserData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.PageUrls.{checkYourDetailsUrl, employerPayeReferenceUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class PayeRefControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val payeRef: String = "123/AA12345"
  private val employmentId = "employmentId"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private val userRequest: User[_] = User(mtditid, None, nino, sessionId, affinityGroup)

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
    val expectedCaption: String
    val expectedH1: String
    val expectedContent: String
    val continueButtonText: String
    val hintText: String
    val emptyErrorText: String
    val wrongFormatErrorText: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedH1: String = "What’s the PAYE reference of maggie?"
    val continueButtonText = "Continue"
    val hintText = "For example, 123/AB456"
    val expectedContent: String = "If the PAYE reference is not 123/AA12345, tell us the correct reference."
    val emptyErrorText: String = "Enter a PAYE reference"
    val wrongFormatErrorText: String = "Enter a PAYE reference in the correct format"

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
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

  private def cya(paye: Option[String] = Some(payeRef), isPriorSubmission: Boolean = true): EmploymentUserData = anEmploymentUserDataWithDetails(
    employmentDetails = anEmploymentDetails.copy("maggie", employerRef = paye),
    isPriorSubmission = isPriorSubmission,
    hasPriorBenefits = isPriorSubmission
  )

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val multipleEmployments: AllEmploymentData = anAllEmploymentData.copy(Seq(
    anEmploymentSource.copy(employmentId = "002", employmentBenefits = None),
    anEmploymentSource.copy(employerRef = Some(payeRef), employmentBenefits = None)
  ))

  ".show" when {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render What's the PAYE reference of xxx? page with cya payeRef in paragraph text when there is cya data" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(cya(), userRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedContent, contentSelector)
          textOnPageCheck(hintText, hintTestSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "123/AA12345")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(employerPayeReferenceUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render What's the PAYE reference of xxx? page with generic paragraph text when user is adding a new employment" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(None), userRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(hintText, hintTestSelector)
          textOnPageCheck(get.expectedContentNewAccount, contentSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(employerPayeReferenceUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

        "The input field" should {

          "be empty" when {
            "there is cya data with PAYE ref field empty but no prior(i.e. user is adding a new employment)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(None, isPriorSubmission = false), aUserRequest)
                urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputSelector, "")
            }


            "cya data and prior data are the same(i.e. user has clicked on change link)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYearEOY)
                insertCyaData(cya(), aUserRequest)
                urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputSelector, "")
            }
          }

          "be filled" when {
            "cya data and prior data differ (i.e user has updated their pay)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYearEOY)
                insertCyaData(cya(Some("123/BB124")), aUserRequest)
                urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputSelector, "123/BB124")
            }

            "cya amount field is filled and prior data is none (i.e user has added a new employment and updated their payeRef but now want to change it)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(Some("123/BB124"), isPriorSubmission = false), User(mtditid, None, nino, sessionId, "agent"))
                urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputSelector, "123/BB124")
            }
          }
        }
      }
    }

    "redirect  to check employment details page when there is no cya data in session" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect  to overview page if the user tries to hit this page with current taxYear" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya(), userRequest)
        urlGet(fullUrl(employerPayeReferenceUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
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
            insertCyaData(cya(), userRequest)
            urlPost(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          errorSummaryCheck(emptyErrorText, expectedErrorHref)
        }

        "should render What's the PAYE reference of xxx? page with wrong format text when input is in incorrect format" which {

          val invalidPaye = "123/abc " + employmentId + "<Q>"

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(), userRequest)
            urlPost(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("payeRef" -> invalidPaye))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          inputFieldValueCheck(amountInputName, inputSelector, invalidPaye)
          errorSummaryCheck(wrongFormatErrorText, expectedErrorHref)
        }

      }
    }

    "redirect to Overview page when a valid form is submitted" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya(), userRequest)
        urlPost(fullUrl(employerPayeReferenceUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("payeRef" -> payeRef))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
