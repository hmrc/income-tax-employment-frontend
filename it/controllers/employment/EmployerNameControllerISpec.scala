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

import common.SessionValues
import forms.employment.EmployerNameForm
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import utils.PageUrls.{checkYourDetailsUrl, employerNameUrl, employerPayeReferenceUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerNameControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employerName: String = "HMRC"
  private val updatedEmployerName: String = "Microsoft"
  private val employmentId: String = "001"
  private val amountInputName = "name"

  private val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uD3vujYg00ZtHwo3s0kyOOCEoAK9vuPiP374QKOe9o"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employmentCyaModel)

  private def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel = EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc))

  object Selectors {
    val inputSelector: String = "#name"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val paragraphTextSelector: String = "#main-content > div > div > p.govuk-body"
    val formatListSelector1: String = "#main-content > div > div > ul > li:nth-child(1)"
    val formatListSelector2: String = "#main-content > div > div > ul > li:nth-child(2)"
    val formatListSelector3: String = "#main-content > div > div > ul > li:nth-child(3)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedErrorWrongFormat: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val expectedErrorCharLimit: String
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
    val expectedErrorWrongFormat = "Enter your employer name in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Beth oedd enwích cyflogwr?"
    val expectedH1 = "Beth oedd enwích cyflogwr?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch enwích cyflogwr"
    val expectedErrorWrongFormat = "Nodwch enwích cyflogwr yn y fformat cywir"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "What’s the name of your client’s employer?"
    val expectedH1 = "What’s the name of your client’s employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the name of your client’s employer"
    val expectedErrorWrongFormat = "Enter your client’s employer name in the correct format"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Beth yw enw cyflogwr eich cleient?"
    val expectedH1 = "Beth yw enw cyflogwr eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch enw cyflogwr eich cleient"
    val expectedErrorWrongFormat = "Nodwch enw cyflogwr eich cleient yn y fformat cywir"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedErrorCharLimit = "The employer name must be 74 characters or fewer"
    val paragraphText = "The employer name must be 74 characters or fewer. It can include:"
    val formatList1 = "upper and lower case letters (a to z)"
    val formatList2 = "numbers"
    val formatList3 = "the special characters: & : ’ \\ , . ( ) -"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val expectedErrorCharLimit = "Maeín rhaid i enwír cyflogwr fod yn 74 o gymeriadau neu lai"
    val paragraphText = "Maeín rhaid i enwír cyflogwr fod yn 74 o gymeriadau neu lai. Gall gynnwys y canlynol:"
    val formatList1 = "llythrennau mawr a bach (a i z)"
    val formatList2 = "rhifau"
    val formatList3 = "y cymeriadau arbennig: & : ’ \\ , . ( ) -"
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
            urlGet(fullUrl(employerNameUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY,
              Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(employerNameUrl(taxYearEOY, employmentId), continueButtonFormSelector)
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
            insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)))
            urlGet(fullUrl(employerNameUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          inputFieldValueCheck(amountInputName, inputSelector, employerName)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(employerNameUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(paragraphText, paragraphTextSelector)
          textOnPageCheck(formatList1, formatListSelector1)
          textOnPageCheck(formatList2, formatListSelector2)
          textOnPageCheck(formatList3, formatListSelector3)
        }
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employerNameUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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

          "the submitted data is empty" which {
            lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            inputFieldValueCheck(amountInputName, inputSelector, "")
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, inputSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry)
          }

          "the submitted data is in the wrong format" which {
            lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> "~name~")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            inputFieldValueCheck(amountInputName, inputSelector, "~name~")
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorWrongFormat, inputSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorWrongFormat)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> charLimit)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            inputFieldValueCheck(amountInputName, inputSelector, charLimit)
            buttonCheck(expectedButtonText, continueButtonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedErrorCharLimit, inputSelector)
            errorAboveElementCheck(expectedErrorCharLimit)
          }
        }
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerNameUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "create a new cya model with the employer name (not prior submission)" which {

      lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> employerName)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the next question page (PAYE reference)" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerPayeReferenceUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.employerName shouldBe employerName
      }

    }

    "update a recently created cya model with the employer name (not prior submission)" which {

      lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> employerName)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName, hmrc = false)))
        urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the next question page (PAYE reference)" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerPayeReferenceUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.employerName shouldBe employerName
      }

    }

    "update existing cya model with the new employer name" which {
      lazy val form: Map[String, String] = Map(EmployerNameForm.employerName -> updatedEmployerName)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)))
        urlPost(fullUrl(employerNameUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to employment details CYA page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.employerName shouldBe updatedEmployerName

      }

    }
  }

}

