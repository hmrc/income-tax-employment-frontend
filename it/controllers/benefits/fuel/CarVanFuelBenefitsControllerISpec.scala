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

package controllers.benefits.fuel

import forms.YesNoForm
import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{accommodationRelocationBenefitsUrl, carBenefitsUrl, carVanFuelBenefitsUrl, checkYourBenefitsUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CarVanFuelBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  object Selectors {
    val thisIncludesSelector: String = "#main-content > div > div > form > div > fieldset > legend > p.govuk-body"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val thisIncludes: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any car, van or fuel benefits from this company?"
    val expectedH1 = "Did you get any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got car, van or fuel benefits"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get any car, van or fuel benefits from this company?"
    val expectedH1 = "Did you get any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Select yes if you got car, van or fuel benefits"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any car, van or fuel benefits from this company?"
    val expectedH1 = "Did your client get any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got car, van or fuel benefits"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any car, van or fuel benefits from this company?"
    val expectedH1 = "Did your client get any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Select yes if your client got car, van or fuel benefits"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val thisIncludes = "This includes benefits such as company cars or vans, company car or van fuel, and privately owned vehicle mileage allowances."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
    val thisIncludes = "This includes benefits such as company cars or vans, company car or van fuel, and privately owned vehicle mileage allowances."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "Render the 'Did you receive car benefits' page with the correct content with no CarVanFuelQuestion data so no pre-filling" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true)
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          textOnPageCheck(thisIncludes, thisIncludesSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(carVanFuelBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "Render the 'Did you receive car benefits' page with the correct content with cya data and the yes value pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          textOnPageCheck(thisIncludes, thisIncludesSelector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(carVanFuelBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "Redirect the user to the check employment benefits page when theres is session data for that user but no benefits" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        urlGet(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "Redirect the user to the check employment benefits page when theres no session data for that user" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        s"return a BAD_REQUEST($BAD_REQUEST) status" when {
          "the value is empty" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            textOnPageCheck(thisIncludes, thisIncludesSelector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(carVanFuelBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
          }
        }
      }
    }

    "Update the CarVanFuelQuestion to no and wipe the car data when the user chooses no, then redirect to accommodation page when prior benefits exist" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get

        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel).get shouldBe CarVanFuelModel(sectionQuestion = Some(false))
      }
    }

    "Update the CarVanFuelQuestion to no and wipe the car data when the user chooses no," +
      "then redirect to the accommodation section question when its a new submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the accommodation relocation page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the carVanFuelQuestion to false" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel).get shouldBe CarVanFuelModel(sectionQuestion = Some(false))
      }
    }

    "Update the CarVanFuelQuestion to yes when the user chooses yes, then redirect to the company car question page when prior benefits exist" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the company car page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the cya model with carVanFuel to be true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get

        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel).get shouldBe CarVanFuelModel(sectionQuestion = Some(true))
      }
    }

    "Update the CarVanFuelQuestion to yes when the user chooses yes, then redirect to the company car question page when its a new submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the company car page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the cya model with carVanFuel to be true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel).get shouldBe CarVanFuelModel(sectionQuestion = Some(true))
      }
    }

    "Create a new CarVanFuelModel with the value true when there was previously no model if a user chooses yes, then redirect to the company car page" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the company car page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update only the carVanFuelQuestion to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel).get shouldBe CarVanFuelModel(sectionQuestion = Some(true))
      }
    }

    "Redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(carVanFuelBenefitsUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "Redirects to the check employment benefits page when theres no CYA data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carVanFuelBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "doesn't create any benefits data" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits shouldBe None
      }
    }
  }
}