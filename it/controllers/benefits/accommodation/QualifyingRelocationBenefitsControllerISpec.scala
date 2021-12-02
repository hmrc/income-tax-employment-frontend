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

import controllers.benefits.accommodation.routes._
import controllers.benefits.travel.routes.TravelOrEntertainmentBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.User
import models.benefits.{AccommodationRelocationModel, BenefitsViewModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class QualifyingRelocationBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val employmentId = "001"
  val taxYearEOY: Int = taxYear - 1
  val urlEOY = s"$appUrl/$taxYearEOY/benefits/qualifying-relocation?employmentId=$employmentId"
  val urlInYear = s"$appUrl/$taxYear/benefits/qualifying-relocation?employmentId=$employmentId"

  val continueButtonLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/qualifying-relocation?employmentId=$employmentId"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val someAmount: Option[BigDecimal] = Some(123.45)
  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }
  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(accommodationModel: AccommodationRelocationModel): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), isUsingCustomerData = true, isBenefitsReceived = true,
      accommodationRelocationModel = Some(accommodationModel))

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = hasPriorBenefits,
      hasPriorBenefits = hasPriorBenefits, employmentCyaModel)

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
    val expectedContent: String
    val expectedExample1: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val contentSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val contentExample1Selector = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(1)"
    val yesSelector = "#value"
    val noSelector = "#value-no"
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

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any qualifying relocation benefits?"
    val expectedH1 = "Did you get any qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got qualifying relocation benefits"
    val expectedContent = "These are costs that your employer has paid to help you with relocation, including bridging loans and legal fees. "
    val expectedExample1 = "This does not include the cost of using the NHS after coming into the UK."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any qualifying relocation benefits?"
    val expectedH1 = "Did your client get any qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got qualifying relocation benefits"
    val expectedContent = "These are costs that their employer has paid to help them with relocation, including bridging loans and legal fees. "
    val expectedExample1 = "This does not include the cost of using the NHS after coming into the UK."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get any qualifying relocation benefits?"
    val expectedH1 = "Did you get any qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got qualifying relocation benefits"
    val expectedContent = "These are costs that your employer has paid to help you with relocation, including bridging loans and legal fees. "
    val expectedExample1 = "This does not include the cost of using the NHS after coming into the UK."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any qualifying relocation benefits?"
    val expectedH1 = "Did your client get any qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got qualifying relocation benefits"
    val expectedContent = "These are costs that their employer has paid to help them with relocation, including bridging loans and legal fees. "
    val expectedExample1 = "This does not include the cost of using the NHS after coming into the UK."
  }

  ".show" when {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'Did you get qualifying relocation benefits?' page with correct content and no radio buttons selected when no cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(qualifyingRelocationExpensesQuestion = None))))), userRequest)
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          textOnPageCheck(user.specificExpectedResults.get.expectedContent + user.specificExpectedResults.get.expectedExample1, contentSelector)

          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
        }

        "render the 'Did you get qualifying relocation benefits?' page with correct content and yes button selected when there cya data" +
          "for the question set as true" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(qualifyingRelocationExpensesQuestion = Some(true)))))), userRequest)
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          textOnPageCheck(user.specificExpectedResults.get.expectedContent + user.specificExpectedResults.get.expectedExample1, contentSelector)

          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }


          "render the 'Did you get qualifying relocation benefits?' page with correct content and yes button selected when the user " +
            "has previously chosen yes but has did not enter a vanFuel amount yet" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
                Some(benefits(fullAccommodationRelocationModel.copy(qualifyingRelocationExpensesQuestion = Some(true)))))), userRequest)
              urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = true)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            textOnPageCheck(user.specificExpectedResults.get.expectedContent + user.specificExpectedResults.get.expectedExample1, contentSelector)
          }

          "render the 'Did you get qualifying relocation benefits?' page with correct content and no button selected when there cya" +
            "data for the question set as false" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
                Some(benefits(fullAccommodationRelocationModel.copy(qualifyingRelocationExpensesQuestion = Some(false)))))), userRequest)
              urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = true)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            textOnPageCheck(user.specificExpectedResults.get.expectedContent + user.specificExpectedResults.get.expectedExample1, contentSelector)
          }
        }
      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlInYear, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
      }
    }
    "redirect to the travel or entertainment page when accommodationRelocationQuestion is Some(false) and no prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
    "redirect to the check your benefits page when accommodationRelocationQuestion is Some(false) and prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
    "redirect to the accommodation or relocation page when accommodationRelocationQuestion is None" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel.copy(sectionQuestion = None))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
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
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
              fullAccommodationRelocationModel)))), userRequest)
            urlPost(urlEOY, body = "", welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an BAD REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)

          textOnPageCheck(user.specificExpectedResults.get.expectedContent + user.specificExpectedResults.get.expectedExample1, contentSelector)
        }
      }
    }

    "update accommodationQuestion to Some(true) when user chooses yes and prior benefits exist" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel.copy(qualifyingRelocationExpensesQuestion = Some(false)))))), userRequest)
        urlPost(urlEOY, body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(QualifyingRelocationBenefitsAmountController.show(taxYearEOY, employmentId).url)
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpensesQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses)) shouldBe Some(200.00)
      }

    }

    "update accommodationQuestion to Some(false) when user chooses no and no prior benefits exist" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel.copy(qualifyingRelocationExpensesQuestion = None))))), userRequest)
        urlPost(urlEOY, body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(NonQualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpensesQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses)) shouldBe None
      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(urlInYear, body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
      }
    }

    "redirect to the accommodation or relocation page when accommodationRelocationQuestion is None" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel.copy(sectionQuestion = None))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(urlEOY, body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the check your benefits page when accommodationRelocationQuestion is Some(false) and prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(urlEOY, body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the travel or entertainment page when accommodationRelocationQuestion is Some(false) and no prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(urlEOY, body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }
}
