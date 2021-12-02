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

package controllers.benefits.fuel

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

class CarVanFuelBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId: String = "001"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  private def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  private def benefits(carModel: CarVanFuelModel): BenefitsViewModel = BenefitsViewModel(carVanFuelModel =
    Some(carModel), isUsingCustomerData = true, isBenefitsReceived = true)

  private def carVanFuelBenefitsPage(taxYear: Int) = s"$appUrl/$taxYear/benefits/car-van-fuel?employmentId=$employmentId"

  val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/car-van-fuel?employmentId=$employmentId"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val thisIncludesSelector: String = "#main-content > div > div > form > div > fieldset > legend > p.govuk-body"
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
    val thisIncludes: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you receive any car, van or fuel benefits from this company?"
    val expectedH1 = "Did you receive any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got car, van or fuel benefits"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you receive any car, van or fuel benefits from this company?"
    val expectedH1 = "Did you receive any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got car, van or fuel benefits"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client receive any car, van or fuel benefits from this company?"
    val expectedH1 = "Did your client receive any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got car, van or fuel benefits"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client receive any car, van or fuel benefits from this company?"
    val expectedH1 = "Did your client receive any car, van or fuel benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got car, van or fuel benefits"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val thisIncludes = "This includes benefits such as company cars or vans, company cars or van fuel, and privately owned vehicle mileage allowances."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val thisIncludes = "This includes benefits such as company cars or vans, company cars or van fuel, and privately owned vehicle mileage allowances."
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

        "Render the 'Did you receive car benefits' page with the correct content with no CarVanFuelQuestion data so no pre-filling" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              benefits = Some(BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(carVanFuelBenefitsPage(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          textOnPageCheck(thisIncludes, thisIncludesSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "Render the 'Did you receive car benefits' page with the correct content with cya data and the yes value pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(carVanFuelBenefitsPage(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          textOnPageCheck(thisIncludes, thisIncludesSelector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "Redirect the user to the check employment benefits page when theres is session data for that user but no benefits" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(user.isAgent)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlGet(carVanFuelBenefitsPage(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")

      }
    }

    "Redirect the user to the check employment benefits page when theres no session data for that user" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(user.isAgent)
        urlGet(carVanFuelBenefitsPage(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")

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
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(carVanFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            textOnPageCheck(thisIncludes, thisIncludesSelector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
          }
        }
      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "Update the CarVanFuelQuestion to no and wipe the car data when the user chooses no, then redirect to accommodation page when prior benefits exist" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carVanFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/accommodation-relocation?employmentId=$employmentId")
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.sectionQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.van)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe None
      }

    }

    "Update the CarVanFuelQuestion to no and wipe the car data when the user chooses no," +
      "then redirect to the accommodation section question when its a new submission" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carVanFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the accommodation relocation page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/accommodation-relocation?employmentId=$employmentId")
      }

      "updates the carVanFuelQuestion to false" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.sectionQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.van)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe None
      }

    }

    "Update the CarVanFuelQuestion to yes when the user chooses yes, then redirect to the company car question page when prior benefits exist" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(emptyCarVanFuelModel)))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carVanFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the company car page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/company-car?employmentId=$employmentId")
      }

      "updates the cya model with carVanFuel to be true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.van)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe None
      }

    }

    "Update the CarVanFuelQuestion to yes when the user chooses yes, then redirect to the company car question page when its a new submission" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true, Some(benefits(emptyCarVanFuelModel)))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carVanFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the company car page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/company-car?employmentId=$employmentId")
      }

      "updates the cya model with carVanFuel to be true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.van)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe None
      }

    }

    "Create a new CarVanFuelModel with the value true when there was previously no model if a user chooses yes, then redirect to the company car page" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          benefits = Some(BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true)))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carVanFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the company car page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/company-car?employmentId=$employmentId")
      }

      "update only the carVanFuelQuestion to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.van)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe None
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe None
      }
    }

    "Redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carVanFuelBenefitsPage(taxYear), body = "", user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
      }
    }

    "Redirects to the check employment benefits page when theres no CYA data" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(carVanFuelBenefitsPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }

      "doesn't create any benefits data" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits shouldBe None
      }
    }

  }
}

