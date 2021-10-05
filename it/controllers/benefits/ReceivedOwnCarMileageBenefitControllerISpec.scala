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

import forms.YesNoForm
import models.User
import models.employment.{BenefitsViewModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ReceivedOwnCarMileageBenefitControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val employmentId = "001"
  val mileageAmount: Option[BigDecimal] = Some(BigDecimal(4.9))
  val taxYearEOY: Int = taxYear - 1
  val urlEOY = s"$appUrl/$taxYearEOY/benefits/mileage?employmentId=$employmentId"
  val urlInYear = s"$appUrl/$taxYear/benefits/mileage?employmentId=$employmentId"

  val continueButtonLink: String = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/mileage?employmentId=$employmentId"
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private val userRequest: User[_] = User(mtditid, None, nino, sessionId, affinityGroup)

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val p1Selector = "#main-content > div > div > form > div > fieldset > legend > p.govuk-body"
    val p2Selector = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(3)"
  }

  val poundPrefixText = "£"
  val amountInputName = "amount"

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedP1: String
    val expectedP2: String
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
    val expectedTitle = "Did you get a mileage benefit for using your own car for work?"
    val expectedH1 = "Did you get a mileage benefit for using your own car for work?"
    val expectedP1 = "We only need to know about payments made above our ‘approved amount‘. If you have payments above the ‘approved amount‘, they should be recorded in section E of your P11D form."
    val expectedP2 = "Check with your employer if you are unsure."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got a mileage benefit for using your own car for work"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a mileage benefit for using their own car for work?"
    val expectedH1 = "Did your client get a mileage benefit for using their own car for work?"
    val expectedP1 = "We only need to know about payments made above our ‘approved amount‘. If your client has payments above the ‘approved amount‘, they should be recorded in section E of their P11D form."
    val expectedP2 = "Check with your client’s employer if you are unsure."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got a mileage benefit for using your own car for work"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get a mileage benefit for using your own car for work?"
    val expectedH1 = "Did you get a mileage benefit for using your own car for work?"
    val expectedP1 = "We only need to know about payments made above our ‘approved amount‘. If you have payments above the ‘approved amount‘, they should be recorded in section E of your P11D form."
    val expectedP2 = "Check with your employer if you are unsure."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got a mileage benefit for using your own car for work"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a mileage benefit for using their own car for work?"
    val expectedH1 = "Did your client get a mileage benefit for using their own car for work?"
    val expectedP1 = "We only need to know about payments made above our ‘approved amount‘. If your client has payments above the ‘approved amount‘, they should be recorded in section E of their P11D form."
    val expectedP2 = "Check with your client’s employer if you are unsure."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got a mileage benefit for using your own car for work"
  }

  val carFuelAmount: Option[BigDecimal] = Some(123.45)

  val benefitsWithNoBenefitsReceived: Option[BenefitsViewModel] = Some(BenefitsViewModel(isUsingCustomerData = true))

  val benefitsWithFalseCarVanFuelQuestion: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(false))),
    isUsingCustomerData = true))

  val benefitsWithFalseCarFuelQuestion: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true), carFuelQuestion = Some(false))),
    isUsingCustomerData = true))

  val benefitsWithNoCarFuelQuestion: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true))),
    isUsingCustomerData = true))

  val benefitsWithEmptyMileage: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true), carFuelQuestion = Some(true),
      mileageQuestion = None, mileage = None)), isUsingCustomerData = true))

  def benefitsWithMileageYes(mileageAmount: Option[BigDecimal] = mileageAmount): Option[BenefitsViewModel] =
    Some(BenefitsViewModel(isBenefitsReceived = true,
      carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true), carFuelQuestion = Some(true),
        carFuel = carFuelAmount, mileageQuestion = Some(true), mileage = mileageAmount)), isUsingCustomerData = true))

  val benefitsWithMileageNo: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true), carFuelQuestion = Some(true),
      carFuel = carFuelAmount, mileageQuestion = Some(false), mileage = None)), isUsingCustomerData = true))

  val benefitsWithMileageAndNoCarVanFuelQuestion: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = None, carFuelQuestion = Some(true),
      carFuel = carFuelAmount, mileageQuestion = Some(true), mileage = mileageAmount)), isUsingCustomerData = true))

  def cya(isPriorSubmission: Boolean = true, benefits: Option[BenefitsViewModel]):
  EmploymentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission,
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

        "render the 'Did you get a mileage benefit?' page with correct content and no radio buttons selected when no cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithEmptyMileage), User(mtditid, None, nino, sessionId, affinityGroup))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get a mileage benefit?' page with correct content and yes button selected when there cya data for the question set as true" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithMileageYes()), User(mtditid, None, nino, sessionId, affinityGroup))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get a mileage benefit?' page with correct content and yes button selected when the user has previously chosen yes" +
          " but has did not entere a mileage amount yet" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithMileageYes(mileageAmount = None)), User(mtditid, None, nino, sessionId, affinityGroup))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get a mileage benefit?' page with correct content and no button selected when there cya data for the question set as false" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithMileageNo), User(mtditid, None, nino, sessionId, affinityGroup))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
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
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
          }
        }

        "redirect to the company van fuel question page when benefits has carFuelQuestion set to false and not prior submission" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithMileageAndNoCarVanFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
            urlGet(urlEOY, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/car-van-fuel?employmentId=$employmentId")
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
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
          }
        }

        "redirect to check employment benefits page when benefits has carVanFuelQuestion set to false" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithFalseCarVanFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
            urlGet(urlEOY, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
          }
        }

        "redirect to overview page if the user tries to hit this page with current taxYear" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithMileageNo), User(mtditid, None, nino, sessionId, "agent"))
            val inYearUrl = s"$appUrl/$taxYear/how-much-pay?employmentId=$employmentId"
            urlGet(inYearUrl, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
          }
        }
      }
    }
  }

  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "Update the mileageQuestion to no and wipe out the mileage amount when the no radio button has been chosen" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithEmptyMileage), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
            urlPost(urlEOY, body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(urlEOY, body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the check your details page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(false)
            cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe None
          }

        }

        "Update the mileageQuestion to yes when the user chooses yes" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithEmptyMileage), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
            urlPost(urlEOY, body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(urlEOY, body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the check your benefits page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
          }

        }

        "display an error when no radio button is selected" which {

          val form: Map[String, String] = Map[String, String]()

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithEmptyMileage), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
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

        "redirect to the company van fuel question page when benefits has carFuelQuestion set to false and not prior submission" when {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithMileageAndNoCarVanFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
            urlPost(urlEOY, follow = false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
          }


          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/car-van-fuel?employmentId=$employmentId")
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
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
          }
        }

        "redirect to check employment benefits page when benefits has carVanFuelQuestion set to false" when {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithFalseCarVanFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
            urlPost(urlEOY, follow = false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
          }
        }

      }
    }
  }

}

