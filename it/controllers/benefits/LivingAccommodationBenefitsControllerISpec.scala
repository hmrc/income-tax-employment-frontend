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

package controllers.benefits

import controllers.employment.routes.CheckYourBenefitsController
import controllers.benefits.routes.AccommodationRelocationBenefitsController
import forms.YesNoForm
import models.User
import models.employment.{AccommodationRelocationModel, BenefitsViewModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class LivingAccommodationBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = 2021
  val employmentId: String = "001"

  def livingAccommodationPageUrl(taxYear: Int): String = s"$appUrl/$taxYear/benefits/living-accommodation?employmentId=$employmentId"

  val continueButtonLink: String = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/living-accommodation?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(accommodationModel: AccommodationRelocationModel): BenefitsViewModel =
    BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true, accommodationRelocationModel = Some(accommodationModel))

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
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodationQuestion = None))))), userRequest)
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
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
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
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodationQuestion = Some(true)))))), userRequest)
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
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, formSelector)
          textOnPageCheck(expectedDetailsTitle, detailsTitleSelector)
          textOnPageCheck(expectedDetailsText1, detailsText1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsText2, detailsText2Selector)
          textOnPageCheck(expectedDetailsText3, detailsText3Selector)
          welshToggleCheck(user.isWelsh)

        }

        "render the 'Did you get any accommodation benefits?' page with the 'no' radio button pre-filled and it's not a prior submission" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodationQuestion = Some(false)))))), userRequest)
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
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(true))
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
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(livingAccommodationPageUrl(taxYear), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }

    }

    "redirect to the accommodation or relocation page when accommodationRelocationQuestion is None" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel.copy(accommodationRelocationQuestion = None))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(livingAccommodationPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the check your benefits page when accommodationRelocationQuestion is Some(false) and it's a prior submission" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(livingAccommodationPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the travel or entertainment page when accommodationRelocationQuestion is Some(false) and it's not a prior submission" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(livingAccommodationPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        //TODO - change to the first page of travel section
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
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
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(
              fullAccommodationRelocationModel)))), userRequest)
            urlPost(livingAccommodationPageUrl(taxYearEOY), body = "", welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an BAD REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, yesSelector)
          formPostLinkCheck(continueButtonLink, formSelector)

        }
      }
    }

    "update accommodationQuestion to Some(true) when user chooses yes and it's a prior submission" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel)))), userRequest)
        urlPost(livingAccommodationPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationRelocationQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation)) shouldBe Some(100.00)
      }

    }

    "update accommodationQuestion to Some(false) when user chooses no and it's not a prior submission" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel.copy(accommodationQuestion = None))))), userRequest)
        urlPost(livingAccommodationPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationRelocationQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationQuestion)) shouldBe Some(false)
        cyamodel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation)) shouldBe None
      }

    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(livingAccommodationPageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }

      "redirect to the accommodation or relocation page when accommodationRelocationQuestion is None" which {

        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            Some(benefits(emptyAccommodationRelocationModel.copy(accommodationRelocationQuestion = None))))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(livingAccommodationPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
        }

      }

      "redirect to the check your benefits page when accommodationRelocationQuestion is Some(false) and it's a prior submission" which {

        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(livingAccommodationPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

      }

      "redirect to the travel or entertainment page when accommodationRelocationQuestion is Some(false) and it's not a prior submission" which {

        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
            Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(livingAccommodationPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          //TODO - change to the first page of travel section
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

      }

    }

  }

}
