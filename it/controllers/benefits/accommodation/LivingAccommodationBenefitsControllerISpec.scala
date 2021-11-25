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

package controllers.benefits.accommodation

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import controllers.benefits.accommodation.routes.{AccommodationRelocationBenefitsController, LivingAccommodationBenefitAmountController, QualifyingRelocationBenefitsController}
import controllers.benefits.travel.routes.TravelOrEntertainmentBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.benefits.AccommodationRelocationModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class LivingAccommodationBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = 2021
  private val employmentId: String = "employmentId"
  private val continueButtonLink: String = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/living-accommodation?employmentId=$employmentId"

  private def livingAccommodationPageUrl(taxYear: Int): String = s"$appUrl/$taxYear/benefits/living-accommodation?employmentId=$employmentId"

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val paragraphSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val continueButtonSelector = "#continue"
    val detailsTitleSelector = "#main-content > div > div > form > details > summary > span"
    val detailsText1Selector = "#main-content > div > div > form > details > div > p:nth-child(1)"
    val detailsText2Selector = "#main-content > div > div > form > details > div > p:nth-child(2)"
    val detailsText3Selector = "#main-content > div > div > form > details > div > p:nth-child(3)"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedParagraphText: String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedDetailsTitle: String
    val expectedDetailsText1: String
    val expectedDetailsText3: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
    val expectedDetailsText2: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedParagraphText: String = "Living accommodation is any accommodation that you can live in, whether you live there all " +
      "the time or only occasionally. It includes houses, flats, houseboats, holiday homes and apartments."
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetailsTitle = "More information about living accommodation"
    val expectedDetailsText1: String = "Living accommodation doesn’t include hotel rooms or board and lodgings, where you’re " +
      "dependent on someone else for cooking, cleaning or laundry."
    val expectedDetailsText3: String = "If you think all or part of this amount should be exempt from tax, refer to HS202 Living " +
      "accommodation and follow the working sheet."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedParagraphText: String = "Living accommodation is any accommodation that you can live in, whether you live there all " +
      "the time or only occasionally. It includes houses, flats, houseboats, holiday homes and apartments."
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetailsTitle = "More information about living accommodation"
    val expectedDetailsText1: String = "Living accommodation doesn’t include hotel rooms or board and lodgings, where you’re " +
      "dependent on someone else for cooking, cleaning or laundry."
    val expectedDetailsText3: String = "If you think all or part of this amount should be exempt from tax, refer to HS202 Living " +
      "accommodation and follow the working sheet."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any living accommodation benefits?"
    val expectedHeading = "Did you get any living accommodation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you got living accommodation benefits"
    val expectedDetailsText2: String = "Your employment income should include the value of any living accommodation you or your " +
      "relations get because of your employment."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get any living accommodation benefits?"
    val expectedHeading = "Did you get any living accommodation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you got living accommodation benefits"
    val expectedDetailsText2: String = "Your employment income should include the value of any living accommodation you or your " +
      "relations get because of your employment."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any living accommodation benefits?"
    val expectedHeading = "Did your client get any living accommodation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client got living accommodation benefits"
    val expectedDetailsText2: String = "Your client’s employment income should include the value of any living accommodation they " +
      "or their relations get because of their employment."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any living accommodation benefits?"
    val expectedHeading = "Did your client get any living accommodation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client got living accommodation benefits"
    val expectedDetailsText2: String = "Your client’s employment income should include the value of any living accommodation they " +
      "or their relations get because of their employment."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the 'Did you get any accommodation benefits?' page with no pre-filled radio buttons" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodationQuestion = None)))
            insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
            urlGet(livingAccommodationPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, formSelector)
          textOnPageCheck(expectedDetailsTitle, detailsTitleSelector)
          textOnPageCheck(expectedDetailsText1, detailsText1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsText2, detailsText2Selector)
          textOnPageCheck(expectedDetailsText3, detailsText3Selector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get any accommodation benefits?' page with the 'yes' radio button pre-filled and cya data" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodationQuestion = Some(true))))
            insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
            urlGet(livingAccommodationPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, formSelector)
          textOnPageCheck(expectedDetailsTitle, detailsTitleSelector)
          textOnPageCheck(expectedDetailsText1, detailsText1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsText2, detailsText2Selector)
          textOnPageCheck(expectedDetailsText3, detailsText3Selector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get any accommodation benefits?' page with the 'no' radio button pre-filled and no prior benefits exist" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodationQuestion = Some(false))))
            val employmentUserData = anEmploymentUserData
              .copy(isPriorSubmission = false, hasPriorBenefits = false)
              .copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel)))
            insertCyaData(employmentUserData, aUserRequest)
            urlGet(livingAccommodationPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, formSelector)
          textOnPageCheck(expectedDetailsTitle, detailsTitleSelector)
          textOnPageCheck(expectedDetailsText1, detailsText1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsText2, detailsText2Selector)
          textOnPageCheck(expectedDetailsText3, detailsText3Selector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(livingAccommodationPageUrl(taxYear), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
      }
    }

    "redirect to the accommodation or relocation page when accommodationRelocationQuestion is None" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = None)))
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(livingAccommodationPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when accommodationRelocationQuestion is Some(false) and no prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(livingAccommodationPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the travel or entertainment page when accommodationRelocationQuestion is Some(false) and no prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = Some(false))))
        val employmentUserData = anEmploymentUserData
          .copy(isPriorSubmission = false, hasPriorBenefits = false)
          .copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel)))
        insertCyaData(employmentUserData, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(livingAccommodationPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error when a form is submitted with no entry" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel))
            insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
            urlPost(livingAccommodationPageUrl(taxYearEOY), body = "", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an BAD REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))
          formPostLinkCheck(continueButtonLink, formSelector)
        }
      }
    }

    "update accommodationQuestion to Some(true) when user chooses yes and prior benefits exist" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel
          .copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodationQuestion = Some(false))))
          .copy(travelEntertainmentModel = None)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        urlPost(livingAccommodationPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(LivingAccommodationBenefitAmountController.show(taxYearEOY, employmentId).url)
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation)) shouldBe Some(100.00)
      }
    }

    "update accommodationQuestion to Some(false) when user chooses no and no prior benefits exist" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel
          .copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodationQuestion = None)))
          .copy(travelEntertainmentModel = None)
        val employmentUserData = anEmploymentUserData
          .copy(isPriorSubmission = false, hasPriorBenefits = false)
          .copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel)))
        insertCyaData(employmentUserData, aUserRequest)
        urlPost(livingAccommodationPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(QualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation)) shouldBe None
      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(livingAccommodationPageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
      }
    }

    "redirect to the accommodation or relocation page when accommodationRelocationQuestion is None" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = None)))
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(livingAccommodationPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when accommodationRelocationQuestion is Some(false) and prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(livingAccommodationPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the travel or entertainment page when accommodationRelocationQuestion is Some(false) and no prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = Some(false))))
        val employmentUserData = anEmploymentUserData
          .copy(isPriorSubmission = false, hasPriorBenefits = false)
          .copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel)))
        insertCyaData(employmentUserData, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(livingAccommodationPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }
}
