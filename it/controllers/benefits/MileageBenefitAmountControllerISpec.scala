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

import controllers.benefits.routes._
import controllers.employment.routes.CheckYourBenefitsController
import forms.AmountForm
import models.employment.{BenefitsViewModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import models.{IncomeTaxUserData, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class MileageBenefitAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId = "001"

  val poundPrefixText = "£"
  val amountField = "#amount"

  def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/mileage-amount?employmentId=$employmentId"

  val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/mileage-amount?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  override def fullCarVanFuelModel: CarVanFuelModel =
    CarVanFuelModel(
      carVanFuelQuestion = Some(true),
      carQuestion = Some(true),
      car = Some(100.00),
      carFuelQuestion = Some(true),
      carFuel = Some(200.00),
      vanQuestion = Some(true),
      van = Some(300.00),
      vanFuelQuestion = Some(true),
      vanFuel = Some(350.00),
      mileageQuestion = Some(true),
      mileage = Some(400.00)
    )

  override def emptyCarVanFuelModel: CarVanFuelModel = CarVanFuelModel(None)

  def emptyMileageModel: CarVanFuelModel = fullCarVanFuelModel.copy(mileageQuestion = None, mileage = None)

  def benefits(carModel: CarVanFuelModel): BenefitsViewModel = BenefitsViewModel(isBenefitsReceived = true, carVanFuelModel = Some(carModel), isUsingCustomerData = true)

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val contentSelector = "#main-content > div > div > form > div > label > p"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String
    val continueButtonText: String
    val hintText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraph: String
    val expectedParagraphWithPrefill: String
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedWrongFormatErrorMessage: String
    val expectedMaxErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £600 or £193.54"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £600 or £193.54"
    val continueButtonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much mileage benefit did you get in total for using your own car?"
    val expectedHeading = "How much mileage benefit did you get in total for using your own car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of mileage benefit you got for using your own car"
    val expectedParagraph: String = "You can find this information on your P11D form in section E, box 12."
    val expectedParagraphWithPrefill: String = "If it was not £400.0, tell us the correct amount. You can find this information on your P11D form in section E, box 12."
    val expectedWrongFormatErrorMessage: String = "Enter the amount of mileage benefit you got in the correct format"
    val expectedMaxErrorMessage: String = "Your mileage benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much mileage benefit did you get in total for using your own car?"
    val expectedHeading = "How much mileage benefit did you get in total for using your own car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of mileage benefit you got for using your own car"
    val expectedParagraph: String = "You can find this information on your P11D form in section E, box 12."
    val expectedParagraphWithPrefill: String = "If it was not £400.0, tell us the correct amount. You can find this information on your P11D form in section E, box 12."
    val expectedWrongFormatErrorMessage: String = "Enter the amount of mileage benefit you got in the correct format"
    val expectedMaxErrorMessage: String = "Your mileage benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much mileage benefit did your client get in total for using their own car?"
    val expectedHeading = "How much mileage benefit did your client get in total for using their own car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of mileage benefit your client got for using their own car"
    val expectedParagraph: String = "You can find this information on your client’s P11D form in section E, box 12."
    val expectedParagraphWithPrefill: String = "If it was not £400.0, tell us the correct amount. You can find this information on your client’s P11D form in section E, box 12."
    val expectedWrongFormatErrorMessage: String = "Enter the amount of mileage benefit your client got in the correct format"
    val expectedMaxErrorMessage: String = "Your client’s mileage benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much mileage benefit did your client get in total for using their own car?"
    val expectedHeading = "How much mileage benefit did your client get in total for using their own car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of mileage benefit your client got for using their own car"
    val expectedParagraph: String = "You can find this information on your client’s P11D form in section E, box 12."
    val expectedParagraphWithPrefill: String = "If it was not £400.0, tell us the correct amount. You can find this information on your client’s P11D form in section E, box 12."
    val expectedWrongFormatErrorMessage: String = "Enter the amount of mileage benefit your client got in the correct format"
    val expectedMaxErrorMessage: String = "Your client’s mileage benefit must be less than £100,000,000,000"
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
        "render the mileage amount page with no pre-filled amount" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(_.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(mileage = None))))))))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(mileage = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, contentSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("", amountField)
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the mileage amount page with amount pre-filled" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphWithPrefill, contentSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("400", amountField)
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)

        }
      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "redirect" when {

      "redirect to accommodation relocation page when the mileage question is no" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(mileageQuestion = Some(false)))))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/benefits/accommodation-relocation?employmentId=001")
        }

      }
      "mileage question is empty" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(mileageQuestion = None))))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ReceiveOwnCarMileageBenefitController.show(taxYearEOY, employmentId).url)
        }

      }
      "car section isn't finished" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(carQuestion = None))))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CompanyCarBenefitsController.show(taxYearEOY, employmentId).url)
        }

      }
      "van section isn't finished" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanQuestion = None))))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CompanyVanBenefitsController.show(taxYearEOY, employmentId).url)
        }

      }
      "car fuel section isn't finished" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(carFuelQuestion = None))))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CompanyCarFuelBenefitsController.show(taxYearEOY, employmentId).url)
        }

      }
      "there is no data in session for that user" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

      }

      "it is not EOY" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYear)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
        }
      }
    }

  }

  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error where there is no entry" which {

          val form: Map[String, String] = Map[String, String]()

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, amountField)
          inputFieldValueCheck("", amountField)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when it is the wrong format" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> "fgfggffg")

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          errorSummaryCheck(user.specificExpectedResults.get.expectedWrongFormatErrorMessage, amountField)
          inputFieldValueCheck("fgfggffg", amountField)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedWrongFormatErrorMessage)

          welshToggleCheck(user.isWelsh)
        }

        "return an error when the value is too large" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> "2353453425345234")

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          errorSummaryCheck(user.specificExpectedResults.get.expectedMaxErrorMessage, amountField)
          inputFieldValueCheck("2353453425345234", amountField)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedMaxErrorMessage)

          welshToggleCheck(user.isWelsh)
        }
      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "redirect" when {

      "there is no cya data in session for that user" which {

        lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
          lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel) shouldBe None
        }
      }
      "redirect to accommodation relocation page when the mileage question is no" which {

        lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(mileageQuestion = Some(false)))))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/benefits/accommodation-relocation?employmentId=001")
        }
      }

      "it isn't end of year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(url(taxYear), body = "", user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
        }

      }

    }

    "update mileage amount to 200 when the user submits and prior benefits exist, redirects to accommodation page" which {

      lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the accommodation page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "updates the mileage benefit to be 200" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe Some(200.00)
      }

    }

    "update mileage amount to 200 when the user submits and no prior benefits exist, redirects to the accommodation relocation page" which {

      lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the accommodation relocation controller page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "updates the mileage benefit to be 200" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe Some(200.00)
      }

    }
  }
}

