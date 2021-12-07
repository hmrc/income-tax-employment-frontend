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

import builders.models.UserBuilder.aUserRequest
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.employment.EmployerNameForm
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerNameControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1

  val employerName: String = "HMRC"
  val updatedEmployerName: String = "Microsoft"
  val employmentId: String = "001"
  val amountInputName = "name"

  val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uD3vujYg00ZtHwo3s0kyOOCEoAK9vuPiP374QKOe9o"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)


  def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel = EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc))

  private def employerNamePageUrl(taxYear: Int) = s"$appUrl/$taxYear/employer-name?employmentId=$employmentId"

  val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/employer-name?employmentId=$employmentId"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > label > header > p"
    val inputSelector: String = "#name"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val paragraphTextSelector: String = "#main-content > div > div > form > div > label > p"
    val formatListSelector1: String = "#main-content > div > div > form > div > label > ul > li:nth-child(1)"
    val formatListSelector2: String = "#main-content > div > div > form > div > label > ul > li:nth-child(2)"
    val formatListSelector3: String = "#main-content > div > div > form > div > label > ul > li:nth-child(3)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val checkEmploymentDetailsHeading: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val expectedErrorCharLimit: String
    val expectedErrorDuplicateName: String
    val paragraphText: String
    val formatList1: String
    val formatList2: String
    val formatList3: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your employer?"
    val expectedH1 = "What’s the name of your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your employer"
    val checkEmploymentDetailsHeading = "Check your employment details"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your employer?"
    val expectedH1 = "What’s the name of your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your employer"
    val checkEmploymentDetailsHeading = "Check your employment details"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your client’s employer?"
    val expectedH1 = "What’s the name of your client’s employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your client’s employer"
    val checkEmploymentDetailsHeading = "Check your client’s employment details"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your client’s employer?"
    val expectedH1 = "What’s the name of your client’s employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your client’s employer"
    val checkEmploymentDetailsHeading = "Check your client’s employment details"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedErrorCharLimit = "The employer name must be 74 characters or fewer"
    val expectedErrorDuplicateName = "You cannot add 2 employers with the same name"
    val paragraphText = "The employer name must be 74 characters or fewer. It can include:"
    val formatList1 = "upper and lower case letters (a to z)"
    val formatList2 = "numbers"
    val formatList3 = "the special characters: & : ’ \\ , . ( ) -"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedErrorCharLimit = "The employer name must be 74 characters or fewer"
    val expectedErrorDuplicateName = "You cannot add 2 employers with the same name"
    val paragraphText = "The employer name must be 74 characters or fewer. It can include:"
    val formatList1 = "upper and lower case letters (a to z)"
    val formatList2 = "numbers"
    val formatList3 = "the special characters: & : ’ \\ , . ( ) -"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'name of your employer' page with the correct content" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(employerNamePageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(paragraphText, paragraphTextSelector)
          textOnPageCheck(formatList1, formatListSelector1)
          textOnPageCheck(formatList2, formatListSelector2)
          textOnPageCheck(formatList3, formatListSelector3)
        }

        "render the 'name of your employer' page with the correct content and pre-popped input field" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), aUserRequest)
            urlGet(employerNamePageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          inputFieldValueCheck(amountInputName, inputSelector, employerName)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(paragraphText, paragraphTextSelector)
          textOnPageCheck(formatList1, formatListSelector1)
          textOnPageCheck(formatList2, formatListSelector2)
          textOnPageCheck(formatList3, formatListSelector3)
        }

        "redirect the user to the overview page when it is not end of year" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(employerNamePageUrl(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
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
            urlPost(employerNamePageUrl(taxYear), body = "", user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
          }
        }

        "create a new cya model with the employer name (not prior submission)" which {

          lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> employerName)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(employerNamePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the next question page (PAYE reference)" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("/update-and-submit-income-tax-return/employment-income/2021/employer-paye-reference?employmentId=001")
            lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
            cyaModel.employment.employmentDetails.employerName shouldBe employerName
          }

        }

        "update a recently created cya model with the employer name (not prior submission)" which {

          lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> employerName)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName, hmrc = false)), aUserRequest)
            urlPost(employerNamePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the next question page (PAYE reference)" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("/update-and-submit-income-tax-return/employment-income/2021/employer-paye-reference?employmentId=001")
            lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
            cyaModel.employment.employmentDetails.employerName shouldBe employerName
          }

        }

        "update existing cya model with the new employer name" which {
          lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> updatedEmployerName)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), aUserRequest)
            urlPost(employerNamePageUrl(taxYearEOY), body = form, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to employment details CYA page" in {
            result.status shouldBe SEE_OTHER
            result.header(HeaderNames.LOCATION) shouldBe Some(CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url)
            lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
            cyaModel.employment.employmentDetails.employerName shouldBe updatedEmployerName

          }

        }

        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "the submitted data is empty" which {
            lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerNamePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Selectors.inputSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> charLimit)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(employerNamePageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            inputFieldValueCheck(amountInputName, inputSelector, charLimit)
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedErrorCharLimit, Selectors.inputSelector)
            errorAboveElementCheck(expectedErrorCharLimit)
          }
        }
      }
    }
  }

}

