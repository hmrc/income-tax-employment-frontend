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

package controllers.benefits.carVanFuel

import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.User
import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CompanyCarFuelBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear-1
  val employmentId: String = "001"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(carModel: CarVanFuelModel): BenefitsViewModel = BenefitsViewModel(carVanFuelModel=Some(carModel),
    isUsingCustomerData = true, isBenefitsReceived = true)

  private def carFuelBenefitsPage(taxYear: Int) = s"$appUrl/$taxYear/benefits/car-fuel?employmentId=$employmentId"

  val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/car-fuel?employmentId=$employmentId"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
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
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get fuel benefit for a company car?"
    val expectedH1 = "Did you get fuel benefit for a company car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got fuel benefit for a company car"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get fuel benefit for a company car?"
    val expectedH1 = "Did you get fuel benefit for a company car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got fuel benefit for a company car"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get fuel benefit for a company car?"
    val expectedH1 = "Did your client get fuel benefit for a company car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got fuel benefit for a company car"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get fuel benefit for a company car?"
    val expectedH1 = "Did your client get fuel benefit for a company car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got fuel benefit for a company car"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
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

        "Render the 'Did you receive car fuel benefits' page with the correct content with no benefits data so no pre-filling" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(carFuelQuestion = None)).copy(isBenefitsReceived = true)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(carFuelBenefitsPage(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "Render the 'Did you receive car fuel benefits' page with the correct content with cya data and the yes value pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel).copy(isBenefitsReceived = true)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(carFuelBenefitsPage(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "Redirect user to the check your benefits page when theres no CYA data" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(user.isAgent)
        urlGet(carFuelBenefitsPage(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }


  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "Redirect to check your benefits page" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(carFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a SEE_OTHER($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
          }

        }
        "return an error where there is no entry" which {

          val form: Map[String, String] = Map[String, String]()

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(carFuelQuestion = None)).copy(isBenefitsReceived = true)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(carFuelBenefitsPage(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))

          welshToggleCheck(user.isWelsh)

        }
      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "Redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carFuelBenefitsPage(taxYear), body = "", user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }

    "Update the CarFuelQuestion to no and wipe the car data when the user chooses no, redirect to company van page when prior benefits exist" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel).copy(isBenefitsReceived = true)))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-van?employmentId=$employmentId")
      }

      "updates the carFuelQuestion to false and carFuel to None" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) shouldBe Some(false)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel)) shouldBe None
      }

    }

    "Update the CarFuelQuestion to no and wipe the car data when the user chooses no, redirect to van question when no prior benefits" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel).copy(isBenefitsReceived = true)))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-van?employmentId=$employmentId")
      }

      "updates the carFuelQuestion to false and carFuel to None" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) shouldBe Some(false)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel)) shouldBe None
      }

    }

    "Update the CarFuelQuestion to yes and when the user chooses yes, redirect to the car fuel amount page when prior benefits exist" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullCarVanFuelModel.copy(carFuelQuestion = Some(false)))))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the car fuel amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/car-fuel-amount?employmentId=$employmentId")
      }

      "updates the car fuel question to be true" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) shouldBe Some(true)
      }

    }

    "Update the CarFuelQuestion to yes and when the user chooses yes, redirect to the car fuel amount page when no prior benefits" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(fullCarVanFuelModel.copy(carFuelQuestion = Some(false)))))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the car fuel amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/car-fuel-amount?employmentId=$employmentId")
      }

      "updates the car fuel question to be true" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) shouldBe Some(true)
      }

    }
  }
}

