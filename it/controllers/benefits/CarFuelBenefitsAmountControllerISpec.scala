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

import forms.AmountForm
import models.User
import models.employment.{Benefits, BenefitsViewModel, CarVanFuelModel, EmploymentBenefits}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CarFuelBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper  {

  val employmentId = "001"
  val taxYearEOY: Int = taxYear - 1
  val urlEOY = s"$appUrl/$taxYearEOY/benefits/car-fuel-amount?employmentId=$employmentId"
  val urlInYear = s"$appUrl/$taxYear/benefits/car-fuel-amount?employmentId=$employmentId"

  val continueButtonLink: String = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/car-fuel-amount?employmentId=$employmentId"

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
    val optionalText = s"If it was not £${carFuelAmount.get}, tell us the correct amount."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £600 or £193.54"
    val optionalText = s"If it was not £${carFuelAmount.get}, tell us the correct amount."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "How much was your total company car fuel benefit?"
    val expectedTitle: String = "How much was your total company car fuel benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your P11D form in section F, box 10."
    val emptyErrorText: String = "Enter your company car fuel benefit amount"
    val wrongFormatErrorText: String = "Enter your company car fuel benefit amount in the correct format"
    val maxAmountErrorText: String = "Your company car fuel benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "How much was your client’s total company car fuel benefit?"
    val expectedTitle: String = "How much was your client’s total company car fuel benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your client’s P11D form in section F, box 10."
    val emptyErrorText: String = "Enter your client’s company car fuel benefit amount"
    val wrongFormatErrorText: String = "Enter your client’s company car fuel benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s company car fuel benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "How much was your total company car fuel benefit?"
    val expectedTitle: String = "How much was your total company car fuel benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your P11D form in section F, box 10."
    val emptyErrorText: String = "Enter your company car fuel benefit amount"
    val wrongFormatErrorText: String = "Enter your company car fuel benefit amount in the correct format"
    val maxAmountErrorText: String = "Your company car fuel benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "How much was your client’s total company car fuel benefit?"
    val expectedTitle: String = "How much was your client’s total company car fuel benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your client’s P11D form in section F, box 10."
    val emptyErrorText: String = "Enter your client’s company car fuel benefit amount"
    val wrongFormatErrorText: String = "Enter your client’s company car fuel benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s company car fuel benefit must be less than £100,000,000,000"
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

  val benefitsWithNoCarFuel: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true), carFuelQuestion = Some(true))),
    isUsingCustomerData = true))

  val benefitsWithCarFuel: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true), carFuelQuestion = Some(true),
      carFuel = carFuelAmount)), isUsingCustomerData = true))

  def cya(isPriorSubmission: Boolean = true, benefits: Option[BenefitsViewModel]):
  EmploymentUserData = EmploymentUserData (sessionId, mtditid,nino, taxYearEOY, employmentId, isPriorSubmission,isPriorSubmission,
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
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "should render How much was your company car fuel benefit? page with no value when theres no cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarFuel), User(mtditid, None, nino, sessionId, "agent"))
            urlGet(urlEOY, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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

        "should render How much was your company car fuel benefit? page with prefilling when there is cya data" which {

          val benefits = EmploymentBenefits("2020-01-04T05:01:01Z", Some(Benefits(carFuel = carFuelAmount)))
          val newModel = fullEmploymentsModel().copy(hmrcEmploymentData = Seq(employmentDetailsAndBenefits(Some(benefits))))

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(newModel), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithCarFuel), User(mtditid, None, nino, sessionId, "agent"))
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
          inputFieldValueCheck("", inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render How much was your company car fuel benefit? page with prefilling when there is cya data and no prior data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithCarFuel), User(mtditid, None, nino, sessionId, "agent"))
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
          inputFieldValueCheck(carFuelAmount.get.toString(), inputAmountField)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "redirect to check employment benefits page when there is no cya data in session" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        urlGet(urlEOY, follow = false, welsh=user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to the car fuel question page when benefits has carFuelQuestion set to true but car fuel question empty" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(urlEOY, follow = false, welsh=user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/car-fuel?employmentId=$employmentId")
      }
    }

    "redirect to the company van question page when benefits has carFuelQuestion set to false and not prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithFalseCarFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(urlEOY, follow = false, welsh=user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-van?employmentId=$employmentId")
      }
    }

    "redirect to the check your benefits page when benefits has carFuelQuestion set to false and prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = true, benefitsWithFalseCarFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(urlEOY, follow = false, welsh=user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to accommodation relocation page when benefits has carVanFuelQuestion set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithFalseCarVanFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(urlEOY, follow = false, welsh=user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/accommodation-relocation?employmentId=$employmentId")
      }
    }

    "redirect to check employment benefits page when benefits has benefitsReceived set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoBenefitsReceived), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(urlEOY, follow = false, welsh=user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarFuel), User(mtditid, None, nino, sessionId, "agent"))
        val inYearUrl =s"$appUrl/$taxYear/how-much-pay?employmentId=$employmentId"
        urlGet(inYearUrl, welsh=user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "should render the How much was your company car fuel benefit? page with an error when theres no input" which {

          val errorAmount = ""
          val errorForm: Map[String, String] = Map(AmountForm.amount -> errorAmount)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarFuel), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
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

        "should render the How much was your company car fuel benefit? page with an error when the amount is invalid" which {

          val errorAmount = "abc"
          val errorForm: Map[String, String] = Map(AmountForm.amount -> errorAmount)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarFuel), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
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

        "should render the How much was your company car fuel benefit? page with an error when the amount is too big" which {

          val errorAmount = "100,000,000,000"
          val errorForm: Map[String, String] = Map(AmountForm.amount -> errorAmount)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarFuel), User(mtditid, None, nino, sessionId, agentTest(user.isAgent)))
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

      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "redirect to company van page and update the car fuel amount when a valid form is submitted and prior submission" when {

      val newAmount = 100

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = true, benefitsWithNoCarFuel), userRequest)
        urlPost(urlEOY, follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> newAmount.toString))
      }

      "redirects to the check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-van?employmentId=$employmentId")
      }

      "updates the CYA model with the new value" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        val carFuelAmount: Option[BigDecimal] = cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel))
        carFuelAmount shouldBe Some(newAmount)
      }
    }

    "redirect to company van question page and update the car fuel amount when a valid form is submitted and not prior submission" when {

      val newAmount = 100

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarFuel), userRequest)
        urlPost(urlEOY, follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> newAmount.toString))
      }

      "redirects to the check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-van?employmentId=$employmentId")
      }

      "updates the CYA model with the new value" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        val carFuelAmount: Option[BigDecimal] = cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel))
        carFuelAmount shouldBe Some(newAmount)
      }
    }

    "redirect to company van question page when benefits has carFuelQuestion set to false and not prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithFalseCarFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlPost(urlEOY, follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-van?employmentId=$employmentId")
      }
    }

    "redirect to the car fuel question page when benefits has carFuelQuestion set to true but car fuel question empty" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlPost(urlEOY, follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))          }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/car-fuel?employmentId=$employmentId")
      }
    }

    "redirect to check employment benefits page when benefits has carFuelQuestion set to false and prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = true, benefitsWithFalseCarFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlPost(urlEOY, follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to accommodation relocation page when benefits has carVanFuelQuestion set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithFalseCarVanFuelQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlPost(urlEOY, follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))          }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/accommodation-relocation?employmentId=$employmentId")
      }
    }

    "redirect to check employment benefits page when benefits has benefitsReceived set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoBenefitsReceived), User(mtditid, None, nino, sessionId, "agent"))
        urlPost(urlEOY, follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))          }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }
  }
}
