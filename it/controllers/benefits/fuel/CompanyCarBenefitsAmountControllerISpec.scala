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

import controllers.benefits.fuel.routes.CompanyCarFuelBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import models.{IncomeTaxUserData, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CompanyCarBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId = "001"

  val carAmount: BigDecimal = 100
  val newAmount: BigDecimal = 250
  val maxLimit: String = "100,000,000,000"

  def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/company-car-amount?employmentId=$employmentId"

  val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-car-amount?employmentId=$employmentId"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  val benefitsWithNoBenefitsReceived: Option[BenefitsViewModel] = Some(BenefitsViewModel(isUsingCustomerData = true))

  val benefitsWithFalseCarVanFuelQuestion: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(false))), isUsingCustomerData = true))

  val benefitsWithFalseCarQuestion: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true), carQuestion = Some(false))),
    isUsingCustomerData = true))

  val benefitsWithNoCarQuestion: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true))), isUsingCustomerData = true))

  val benefitsWithNoCarAmount: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true), carQuestion = Some(true))),
    isUsingCustomerData = true))

  val benefitsWithCarAmount: Option[BenefitsViewModel] = Some(BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(carVanFuelQuestion = Some(true), carQuestion = Some(true),
      car = Some(carAmount))), isUsingCustomerData = true))

  def cya(isPriorSubmission: Boolean = true, benefits: Option[BenefitsViewModel]):
  EmploymentUserData = EmploymentUserData (sessionId, mtditid,nino, taxYearEOY, employmentId, isPriorSubmission, hasPriorBenefits = isPriorSubmission,
    EmploymentCYAModel(
      EmploymentDetails("maggie", currentDataIsHmrcHeld = false),
      benefits
    )
  )

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"
    val hintTextSelector = "#amount-hint"
    val amountFieldSelector = "#amount"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String
    val hintText: String
    val amount: String
    val continueButtonText: String
    val optionalText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraphText: String
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedInvalidFormatErrorMessage: String
    val expectedMaxLengthErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £600 or £193.54"
    val amount = "amount"
    val continueButtonText = "Continue"
    val optionalText = s"If it was not £$carAmount, tell us the correct amount."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £600 or £193.54"
    val amount = "amount"
    val continueButtonText = "Continue"
    val optionalText = s"If it was not £$carAmount, tell us the correct amount."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your total company car benefit?"
    val expectedHeading = "How much was your total company car benefit?"
    val expectedParagraphText = "You can find this information on your P11D form in section F, box 9."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your company car benefit amount"
    val expectedInvalidFormatErrorMessage = "Enter your company car benefit amount in the correct format"
    val expectedMaxLengthErrorMessage = "Your company car benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much was your total company car benefit?"
    val expectedHeading = "How much was your total company car benefit?"
    val expectedParagraphText = "You can find this information on your P11D form in section F, box 9."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your company car benefit amount"
    val expectedInvalidFormatErrorMessage = "Enter your company car benefit amount in the correct format"
    val expectedMaxLengthErrorMessage = "Your company car benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s total company car benefit?"
    val expectedHeading = "How much was your client’s total company car benefit?"
    val expectedParagraphText = "You can find this information on your client’s P11D form in section F, box 9."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your client’s company car benefit amount"
    val expectedInvalidFormatErrorMessage = "Enter your client’s company car benefit amount in the correct format"
    val expectedMaxLengthErrorMessage = "Your client’s company car benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s total company car benefit?"
    val expectedHeading = "How much was your client’s total company car benefit?"
    val expectedParagraphText = "You can find this information on your client’s P11D form in section F, box 9."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your client’s company car benefit amount"
    val expectedInvalidFormatErrorMessage = "Enter your client’s company car benefit amount in the correct format"
    val expectedMaxLengthErrorMessage = "Your client’s company car benefit must be less than £100,000,000,000"
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

        "render the company car benefits amount page with no-prefilled amount box" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarAmount), User(mtditid, None, nino, sessionId, "agent"))
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector(2))
          textOnPageCheck(user.commonExpectedResults.hintText, hintTextSelector)
          inputFieldCheck(user.commonExpectedResults.amount, amountFieldSelector)
          inputFieldValueCheck("", amountFieldSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the company car benefits amount page with the amount field pre-filled with cya data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(cya(isPriorSubmission = false, benefitsWithCarAmount), User(mtditid, None, nino, sessionId, "agent"))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          textOnPageCheck(user.commonExpectedResults.optionalText, paragraphTextSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector(3))
          textOnPageCheck(user.commonExpectedResults.hintText, hintTextSelector)
          inputFieldCheck(user.commonExpectedResults.amount, amountFieldSelector)
          inputFieldValueCheck(carAmount.toString(), amountFieldSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the company car benefits amount page with the amount field pre-filled with prior CYA data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(cya(isPriorSubmission = false, benefitsWithCarAmount), User(mtditid, None, nino, sessionId, "agent"))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          textOnPageCheck(user.commonExpectedResults.optionalText, paragraphTextSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector(3))
          textOnPageCheck(user.commonExpectedResults.hintText, hintTextSelector)
          inputFieldCheck(user.commonExpectedResults.amount, amountFieldSelector)
          inputFieldValueCheck(carAmount.toString(), amountFieldSelector)
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)

        }
      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "redirect to the overview page when it is not EOY" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithCarAmount), User(mtditid, None, nino, sessionId, "agent"))
        authoriseAgentOrIndividual(user.isAgent)
        urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }

    "redirect to the check your benefits page when there is no cya data in session" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
        authoriseAgentOrIndividual(user.isAgent)
        urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to the car question page when benefits has carVanFuelQuestion set to true but car question empty" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-car?employmentId=$employmentId")
      }
    }

    "redirect to the company van question page when benefits has carQuestion set to false and no prior benefits" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithFalseCarQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-van?employmentId=$employmentId")
      }
    }

    "redirect to the check your benefits page when benefits has carQuestion set to false and prior benefits exist" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = true, benefitsWithFalseCarQuestion), User(mtditid, None, nino, sessionId, "agent"))
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }
  }

  ".submit" should {

    userScenarios.foreach { user =>

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {

          "there is no entry" which {

            lazy val form: Map[String, String] = Map("amount" -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarAmount), User(mtditid, None, nino, sessionId, "agent"))
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, amountFieldSelector)
            inputFieldValueCheck("", amountFieldSelector)

            welshToggleCheck(user.isWelsh)
          }

          "the entry has an invalid format" which {

            lazy val form: Map[String, String] = Map("amount" -> "abc")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarAmount), User(mtditid, None, nino, sessionId, "agent"))
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            errorSummaryCheck(user.specificExpectedResults.get.expectedInvalidFormatErrorMessage, amountFieldSelector)
            inputFieldValueCheck("abc", amountFieldSelector)

            welshToggleCheck(user.isWelsh)
          }

          "the amount is larger than the maximum limit" which {

            lazy val form: Map[String, String] = Map("amount" -> maxLimit)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarAmount), User(mtditid, None, nino, sessionId, "agent"))
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            errorSummaryCheck(user.specificExpectedResults.get.expectedMaxLengthErrorMessage, amountFieldSelector)
            inputFieldValueCheck(maxLimit, amountFieldSelector)

            welshToggleCheck(user.isWelsh)
          }

        }

      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "update car model with submitted amount when there is existing cya data" which {

      lazy val form: Map[String, String] = Map("amount" -> newAmount.toString())

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = true, benefitsWithCarAmount), User(mtditid, None, nino, sessionId, "agent"))
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CompanyCarFuelBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car)) shouldBe Some(newAmount)
      }

    }

    "update car model with submitted amount when prior benefits exist and go to the car fuel page" which {

      lazy val form: Map[String, String] = Map("amount" -> carAmount.toString())

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = true, benefitsWithNoCarAmount), User(mtditid, None, nino, sessionId, "agent"))
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CompanyCarFuelBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car)) shouldBe Some(carAmount)
      }
    }

    "update car model with submitted amount when no prior benefits exist and go to the check your benefits section" which {

      lazy val form: Map[String, String] = Map("amount" -> carAmount.toString())

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarAmount), User(mtditid, None, nino, sessionId, "agent"))
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CompanyCarFuelBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car)) shouldBe Some(carAmount)
      }
    }

    "redirect to the overview page when it is not EOY" which {

      lazy val form: Map[String, String] = Map("amount" -> "123")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya(isPriorSubmission = false, benefitsWithNoCarAmount), User(mtditid, None, nino, sessionId, "agent"))
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(url(taxYear), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }

    "there is no cya data in session for that user" which {

      lazy val form: Map[String, String] = Map("amount" -> "123")

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }

}
