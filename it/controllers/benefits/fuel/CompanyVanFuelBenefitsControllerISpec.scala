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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CompanyVanFuelBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val employmentId = "001"
  val taxYearEOY: Int = taxYear - 1
  val urlEOY = s"$appUrl/$taxYearEOY/benefits/van-fuel?employmentId=$employmentId"
  val urlInYear = s"$appUrl/$taxYear/benefits/van-fuel?employmentId=$employmentId"

  val continueButtonLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/van-fuel?employmentId=$employmentId"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private val userRequest: User[_] = User(mtditid, None, nino, sessionId, affinityGroup)

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
    val expectedTitle = "Did you get fuel benefit for a company van?"
    val expectedH1 = "Did you get fuel benefit for a company van?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got fuel benefit for a company van"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get fuel benefit for a company van?"
    val expectedH1 = "Did your client get fuel benefit for a company van?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got fuel benefit for a company van"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get fuel benefit for a company van?"
    val expectedH1 = "Did you get fuel benefit for a company van?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got fuel benefit for a company van"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get fuel benefit for a company van?"
    val expectedH1 = "Did your client get fuel benefit for a company van?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got fuel benefit for a company van"
  }

  val someAmount: Option[BigDecimal] = Some(123.45)

  val allSectionsFinishedCarVanFuelModel: CarVanFuelModel = CarVanFuelModel(sectionQuestion = Some(true), carQuestion = Some(true), car = someAmount,
    carFuelQuestion = Some(true), carFuel = someAmount, vanQuestion = Some(true), van = someAmount, vanFuelQuestion = Some(true),
    vanFuel = someAmount, mileageQuestion = Some(true), mileage = someAmount)

  val benefitsWithEmptyVanFuel: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(vanFuelQuestion = None, vanFuel = None)), isUsingCustomerData = true))

  def benefitsWithVanFuelYes(vanFuelAmount: Option[BigDecimal] = someAmount): Option[BenefitsViewModel] =
    Some(BenefitsViewModel(isBenefitsReceived = true,
      carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(vanFuelQuestion = Some(true), vanFuel = vanFuelAmount)), isUsingCustomerData = true))

  val benefitsWithNoBenefitsReceived: Option[BenefitsViewModel] = Some(BenefitsViewModel(isUsingCustomerData = true))

  def benefitsWithNoCarVanFuelQuestion(carVanFuelQuestion: Option[Boolean] = Some(false)): Option[BenefitsViewModel] =
    Some(BenefitsViewModel(isBenefitsReceived = true,
      carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(sectionQuestion = carVanFuelQuestion)),
      isUsingCustomerData = true))

  val benefitsWithVanFuelNo: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(vanFuelQuestion = Some(false), vanFuel = None)), isUsingCustomerData = true))


  def cya(isPriorSubmission: Boolean = true, benefits: Option[BenefitsViewModel]):
  EmploymentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission, hasPriorBenefits = isPriorSubmission,
    EmploymentCYAModel(
      EmploymentDetails("maggie", currentDataIsHmrcHeld = false),
      benefits
    )
  )

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'Did you get fuel benefit for a company van?' page with correct content and no radio buttons selected when no cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithEmptyVanFuel), User(mtditid, None, nino, sessionId, affinityGroup))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get fuel benefit for a company van?' page with correct content and yes button selected when there cya data" +
          "for the question set as true" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithVanFuelYes()), User(mtditid, None, nino, sessionId, affinityGroup))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
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
        }

        "render the 'Did you get fuel benefit for a company van?' page with correct content and yes button selected when the user " +
          "has previously chosen yes but has did not enter a vanFuel amount yet" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithVanFuelYes(vanFuelAmount = None)), User(mtditid, None, nino, sessionId, affinityGroup))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
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
        }

        "render the 'Did you get fuel benefit for a company van?' page with correct content and no button selected when there cya" +
          "data for the question set as false" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithVanFuelNo), User(mtditid, None, nino, sessionId, affinityGroup))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
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
        }
      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "redirect to overview page if the user tries to hit this page with current taxYear" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithVanFuelNo), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(urlInYear, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
      }
    }

    "redirect to check employment benefits page when there is no cya data in session" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        urlGet(urlEOY, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to check employment benefits page when benefits has benefitsReceived set to false" when {

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoBenefitsReceived), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(urlEOY, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }
  }

  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "display an error when no radio button is selected" which {

          val form: Map[String, String] = Map[String, String]()

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithEmptyVanFuel), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(urlEOY, body = form, user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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

    "Update the vanFuelQuestion to no and vanfuel to none when no radio button has been chosen, redirect to mileage page when prior benefits exist" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = true, benefitsWithEmptyVanFuel), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(urlEOY, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the mileage page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/mileage?employmentId=$employmentId")
      }

      "update the vanFuelQuestion to false and vanFuel to none" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel)) shouldBe None
      }

    }

    "Update the vanFuelQuestion to no and vanfuel to none when no radio button has been chosen, redirect to mileage benefit when no prior benefits" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithEmptyVanFuel), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(urlEOY, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the mileage benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/mileage?employmentId=$employmentId")
      }

      "update the vanFuelQuestion to false and vanFuel to none" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel)) shouldBe None
      }

    }

    "Update the vanFuelQuestion to yes when the user chooses yes, redirect to the van fuel benefits amount page when no prior benefits" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithEmptyVanFuel), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(urlEOY, body = form, follow = false, welsh = user.isWelsh,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the van fuel amount benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/van-fuel-amount?employmentId=$employmentId")
      }

      "update the vanFuelQuestion to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion)) shouldBe Some(true)
      }

    }

    "Update the vanFuelQuestion to yes when the user chooses yes, redirect to the van fuel benefits amount page when prior benefits exist" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = true, benefitsWithEmptyVanFuel), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(urlEOY, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the van fuel amount benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/van-fuel-amount?employmentId=$employmentId")
      }

      "update the vanFuelQuestion to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion)) shouldBe Some(true)
      }

    }

    "redirect to check employment benefits page when there is no cya data in session" when {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        urlPost(urlEOY, follow = false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to overview page if the user tries to hit this page with current taxYear" when {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithVanFuelNo), User(mtditid, None, nino, sessionId, "agent"))
        urlPost(urlInYear, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = form)
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
      }
    }

    "redirect to check employment benefits page when benefits has benefitsReceived set to false" when {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoBenefitsReceived), User(mtditid, None, nino, sessionId, "agent"))
        urlPost(urlEOY, follow = false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

  }

}


