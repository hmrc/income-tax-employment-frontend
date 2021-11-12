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

package controllers.benefits.utilities

import controllers.employment.routes.CheckYourBenefitsController
import models.User
import models.benefits.{BenefitsViewModel, UtilitiesAndServicesModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherServicesBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId = "001"

  private def pageUrl(taxYear: Int): String = s"$appUrl/$taxYear/benefits/other-services-amount?employmentId=$employmentId"

  val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/other-services-amount?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId,
      isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  object Selectors {
    val contentSelector = "#main-content > div > div > form > div > label > p"
    val hintTextSelector = "#amount-hint"
    val inputSelector = "#amount"
    val poundPrefixSelector = ".govuk-input__prefix"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val subheading = "#main-content > div > div > form > div > label > header > p"
    val expectedErrorHref = "#amount"
  }


  val poundPrefixText = "£"
  val amountInputName = "amount"

  trait CommonExpectedResults {
    val expectedCaption: String
    val amountHint: String
    val continue: String
    val previousExpectedContent: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val amountHint: String = "For example, £600 or £193.54"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
    val previousExpectedContent: String = "If it was not £400, tell us the correct amount."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "For example, £600 or £193.54"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
    val previousExpectedContent: String = "If it was not £400, tell us the correct amount."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for other services?"
    val expectedHeading: String = "How much did you get in total for other services?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount you got for other services"
    val invalidFormatErrorText: String = "Enter the amount you got for other services in the correct format"
    val maxAmountErrorText: String = "The amount you got for other services must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for other services?"
    val expectedHeading: String = "How much did you get in total for other services?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount you got for other services"
    val invalidFormatErrorText: String = "Enter the amount you got for other services in the correct format"
    val maxAmountErrorText: String = "The amount you got for other services must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for other services?"
    val expectedHeading: String = "How much did your client get in total for other services?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount your client got for other services"
    val invalidFormatErrorText: String = "Enter the amount your client got for other services in the correct format"
    val maxAmountErrorText: String = "The amount your client got for other services must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for other services?"
    val expectedHeading: String = "How much did your client get in total for other services?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount your client got for other services"
    val invalidFormatErrorText: String = "Enter the amount your client got for other services in the correct format"
    val maxAmountErrorText: String = "The amount your client got for other services must be less than £100,000,000,000"
  }

  def benefits(utilitiesModel: UtilitiesAndServicesModel): BenefitsViewModel =
    BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true, utilitiesAndServicesModel = Some(utilitiesModel))

  val benefitsWithNoBenefitsReceived: Option[BenefitsViewModel] = Some(BenefitsViewModel(isUsingCustomerData = true))

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {
    userScenarios.foreach { user =>


      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the other services amount page without pre-filled form and without replay text" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(_.copy(
              benefits = fullBenefits.flatMap(_.benefits.map(_.copy(entertaining = None))))))))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel.copy(service = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          elementNotOnPageCheck(contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the other services amount page with pre-filled cy data" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(
              _.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(telephone = None))))))))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("400", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the other services amount page with the amount field pre-filled with prior submitted data" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("400", inputSelector)
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {

      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))

      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(pageUrl(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "Redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(
            fullBenefits.map(_.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(telephone = None))))))))), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            benefitsWithNoBenefitsReceived)), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(pageUrl(taxYear), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
        }
      }

      "redirect to the other services question page when there is a service amount but the other serviceQuestion has not been answered" +
        " but entertainment benefit question empty" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            Some(benefits(fullUtilitiesAndServicesModel.copy(serviceQuestion = None))))), userRequest)
          urlGet(pageUrl(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe
            Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/other-services?employmentId=$employmentId")
        }
      }

      "redirect to the check employment benefits page when benefits has serviceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
            Some(benefits(fullUtilitiesAndServicesModel.copy(serviceQuestion = Some(false)))))), userRequest)
          urlGet(pageUrl(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe
            Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
        }
      }

      "redirect the user to the check employment benefits page when there's no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            benefitsWithNoBenefitsReceived)), userRequest)

          urlGet(pageUrl(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe
            Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
        }
      }

    }


  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "should render the other services amount amount page with empty error text when there no input" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
            urlPost(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(get.emptyErrorText)
        }

        "should render the entertainment benefit question amount page with invalid format text when input is in incorrect format" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
            urlPost(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "abc"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("abc", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(get.invalidFormatErrorText)
        }

        "should render the entertainment benefit question page with max error when input > 100,000,000,000" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
            urlPost(pageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)),
              body = Map("amount" -> "9999999999999999999999999999"))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("9999999999999999999999999999", inputSelector)

          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(get.maxAmountErrorText)
        }

      }
    }

    "redirect to another page when a valid request is made and then" should {
      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))

      "redirect to medical dental childcare benefits page when a valid form is submitted and a prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
          urlPost(pageUrl(taxYearEOY), follow = false,
            welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.23"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/benefits/medical-dental-childcare-education-loans?employmentId=001")
        }

        "updates the CYA model with the new value" in {
          lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get

          val otherServicesAmount: Option[BigDecimal] =
            cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.service))

          otherServicesAmount shouldBe Some(100.23)
        }
      }

      "redirect to the utility general service next question page when a valid form is submitted and not prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)

          urlPost(pageUrl(taxYearEOY), follow = false,
            welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.11"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe
            Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/medical-dental-childcare-education-loans?employmentId=$employmentId")
        }

        "updates the CYA model with the new value" in {
          lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get

          val otherServicesAmount: Option[BigDecimal] =
            cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.service))

          otherServicesAmount shouldBe Some(100.11)
        }
      }

      "redirect user to the check your benefits page with no cya data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(pageUrl(taxYearEOY), follow = false,
            welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(
            _.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(mileage = None))))))))), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
            Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(pageUrl(taxYear), follow = false,
            welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.22"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
        }
      }


      "redirect to the other services question page when there is a service amount but the other serviceQuestion has not been answered" +
        " but entertainment benefit question empty" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            Some(benefits(fullUtilitiesAndServicesModel.copy(serviceQuestion = None))))), userRequest)
          urlPost(pageUrl(taxYearEOY), follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe
            Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/other-services?employmentId=$employmentId")
        }
      }

      "redirect to the check employment benefits page when benefits has serviceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
            Some(benefits(fullUtilitiesAndServicesModel.copy(serviceQuestion = Some(false)))))), userRequest)
          urlPost(pageUrl(taxYearEOY), follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe
            Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
        }
      }

      "redirect the user to the check employment benefits page when theres no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            benefitsWithNoBenefitsReceived)), userRequest)

          urlPost(pageUrl(taxYearEOY), follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
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
