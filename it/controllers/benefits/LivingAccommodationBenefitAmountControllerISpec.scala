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

import controllers.benefits.routes.{AccommodationRelocationBenefitsController, LivingAccommodationBenefitsController}
import controllers.employment.routes.CheckYourBenefitsController
import forms.AmountForm
import models.User
import models.employment.{AccommodationRelocationModel, BenefitsViewModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class LivingAccommodationBenefitAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper  {

  val employmentId = "001"
  val taxYearEOY: Int = taxYear - 1
  val urlEOY = s"$appUrl/$taxYearEOY/benefits/living-accommodation-amount?employmentId=$employmentId"
  val urlInYear = s"$appUrl/$taxYear/benefits/living-accommodation-amount?employmentId=$employmentId"

  val continueButtonLink: String = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/living-accommodation-amount?employmentId=$employmentId"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private val userRequest: User[_]=  User(mtditid, None, nino, sessionId, affinityGroup)


  object Selectors {
    val headingSelector = "#main-content > div > div > header > h1"
    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#amount"
    val inputAmountField = "#amount"
  }

  val poundPrefixText = "£"
  val amountInputName = "amount"

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedContent: String
    val emptyErrorText: String
    val wrongFormatErrorText: String
    val maxAmountErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val hintText: String
    val optionalText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £600 or £193.54"
    val optionalText = s"If it was not £${livingAccommodationBenefitAmount.get}, tell us the correct amount."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £600 or £193.54"
    val optionalText = s"If it was not £${livingAccommodationBenefitAmount.get}, tell us the correct amount."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "How much was your total living accommodation benefit?"
    val expectedTitle: String = "How much was your total living accommodation benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your P11D form in section D, box 14."
    val emptyErrorText: String = "Enter your living accommodation benefit amount"
    val wrongFormatErrorText: String = "Enter your living accommodation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your living accommodation benefit amount must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "How much was your client’s total living accommodation benefit?"
    val expectedTitle: String = "How much was your client’s total living accommodation benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your client’s P11D form in section D, box 14."
    val emptyErrorText: String = "Enter your client’s living accommodation benefit amount"
    val wrongFormatErrorText: String = "Enter your client’s living accommodation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s living accommodation benefit amount must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "How much was your total living accommodation benefit?"
    val expectedTitle: String = "How much was your total living accommodation benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your P11D form in section D, box 14."
    val emptyErrorText: String = "Enter your living accommodation benefit amount"
    val wrongFormatErrorText: String = "Enter your living accommodation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your living accommodation benefit amount must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "How much was your client’s total living accommodation benefit?"
    val expectedTitle: String = "How much was your client’s total living accommodation benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your client’s P11D form in section D, box 14."
    val emptyErrorText: String = "Enter your client’s living accommodation benefit amount"
    val wrongFormatErrorText: String = "Enter your client’s living accommodation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s living accommodation benefit amount must be less than £100,000,000,000"
  }

  val livingAccommodationBenefitAmount: Option[BigDecimal] = Some(123.45)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(accommodationModel: AccommodationRelocationModel): BenefitsViewModel =
    BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true, accommodationRelocationModel = Some(accommodationModel))


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
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "should render How much was your total living accommodation benefit? page with no value when theres no cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None))))), userRequest)
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, paragraphTextSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render How much was your total living accommodation benefit? page with prefilling when there is cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodation = livingAccommodationBenefitAmount))))), userRequest)
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(optionalText, paragraphTextSelector(2))
          textOnPageCheck(get.expectedContent, paragraphTextSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("123.45", inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render How much was your How much was your total living accommodation benefit? page with prefilling when there is cya data and no prior benefits" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodation = livingAccommodationBenefitAmount))))), userRequest)
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(optionalText, paragraphTextSelector(2))
          textOnPageCheck(get.expectedContent, paragraphTextSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck(livingAccommodationBenefitAmount.get.toString(), inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlInYear, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
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
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the living accommodation page when accommodationQuestion is None and prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = None))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when accommodationQuestion is Some(false) and prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = Some(false)))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when accommodationQuestion is Some(false) and no prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = Some(false)))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

  }

  ".submit" when {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "should render the How much was your Living accommodation benefit? page with an error when theres no input" which {

          val errorAmount = ""
          val errorForm: Map[String, String] = Map(AmountForm.amount -> errorAmount)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None))))), userRequest)
            urlPost(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = errorForm)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, paragraphTextSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck(errorAmount, inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(get.emptyErrorText)
        }

        "should render the How much was your total living accommodation benefit? page with an error when the amount is invalid" which {

          val errorAmount = "abc"
          val errorForm: Map[String, String] = Map(AmountForm.amount -> errorAmount)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None))))), userRequest)
            urlPost(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = errorForm)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, paragraphTextSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck(errorAmount, inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(get.wrongFormatErrorText)
        }

        "should render the How much was your total living accommodation benefit? page with an error when the amount is too big" which {

          val errorAmount = "100,000,000,000"
          val errorForm: Map[String, String] = Map(AmountForm.amount -> errorAmount)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None))))), userRequest)
            urlPost(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = errorForm)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, paragraphTextSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck(errorAmount, inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(get.maxAmountErrorText)
        }

        "redirect to CYA page and update the living accommodation amount when a valid form is submitted and prior benefits exist" when {

          val newAmount = 100

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None))))), userRequest)
            urlPost(urlEOY, follow=false,
              welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> newAmount.toString))
          }

          "redirects to the check your benefits page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
          }

          "updates the CYA model with the new value" in {
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            val livingAccommodationAmount: Option[BigDecimal] = cyamodel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation))
            livingAccommodationAmount shouldBe Some(newAmount)
          }
        }
      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlInYear, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
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
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the living accommodation benefits page when accommodationQuestion is None and prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = None))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when accommodationQuestion is Some(false) and prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = Some(false)))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the check your benefits page when accommodationQuestion is Some(false) and no prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = Some(false)))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(urlEOY, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }
}
