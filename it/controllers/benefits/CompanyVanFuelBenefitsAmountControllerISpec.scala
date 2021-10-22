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

import controllers.employment.routes.CheckYourBenefitsController
import models.User
import models.employment.{BenefitsViewModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CompanyVanFuelBenefitsAmountControllerISpec  extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId = "001"

  def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/van-fuel-amount?employmentId=$employmentId"

  val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/van-fuel-amount?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

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
    val expectedContent: String
    val expectedErrorTitle: String
    val wrongFormatErrorText: String
    val emptyErrorText: String
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
    val expectedTitle: String = "How much was your total company van fuel benefit?"
    val expectedHeading: String = "How much was your total company van fuel benefit?"
    val expectedContent: String = "You can find this information on your P11D form in section G, box 10."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your company van fuel benefit amount in the correct format"
    val emptyErrorText: String = "Enter your company van fuel benefit amount"
    val maxAmountErrorText: String = "Your company van fuel benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much was your total company van fuel benefit?"
    val expectedHeading: String = "How much was your total company van fuel benefit?"
    val expectedContent: String = "You can find this information on your P11D form in section G, box 10."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your company van fuel benefit amount in the correct format"
    val emptyErrorText: String = "Enter your company van fuel benefit amount"
    val maxAmountErrorText: String = "Your company van fuel benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much was your client’s total company van fuel benefit?"
    val expectedHeading: String = "How much was your client’s total company van fuel benefit?"
    val expectedContent: String = "You can find this information on your client’s P11D form in section G, box 10."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your client’s company van fuel benefit amount in the correct format"
    val emptyErrorText: String = "Enter your client’s company van fuel benefit amount"
    val maxAmountErrorText: String = "Your client’s company van fuel benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "How much was your client’s total company van fuel benefit?"
    val expectedHeading: String = "How much was your client’s total company van fuel benefit?"
    val expectedContent: String = "You can find this information on your client’s P11D form in section G, box 10."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val wrongFormatErrorText: String = "Enter your client’s company van fuel benefit amount in the correct format"
    val emptyErrorText: String = "Enter your client’s company van fuel benefit amount"
    val maxAmountErrorText: String = "Your client’s company van fuel benefit must be less than £100,000,000,000"
  }

  def benefits(carModel: CarVanFuelModel): BenefitsViewModel = BenefitsViewModel(Some(carModel), isUsingCustomerData = true, isBenefitsReceived = true)

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the company van fuel benefits amount page without pre-filled form" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(_.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(mileage = None))))))))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanFuel = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputSelector)

          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the company van fuel benefits amount page with pre-filled form" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(_.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(mileage = None))))))))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          textOnPageCheck(previousExpectedContent + " " + get.expectedContent, contentSelector)
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

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "Redirect user to the check your benefits page with no cya data" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(user.isAgent)
        urlGet(url(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "Redirect user to the tax overview page when in year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(_.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(mileage = None))))))))), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(van = None))))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlGet(url(taxYear), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }

    "redirect to the van fuel question page when benefits has carVanFuelQuestion set to true but van fuel question empty" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanFuelQuestion = None))))), userRequest)
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/van-fuel?employmentId=$employmentId")
      }
    }

    "redirect to the mileage page when benefits has vanFuelQuestion set to false and not prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanFuelQuestion = Some(false)))))), userRequest)
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/mileage?employmentId=$employmentId")
      }
    }

    "redirect to the check employment benefits page when benefits has vanFuelQuestion set to false and prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanFuelQuestion = Some(false)))))), userRequest)
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to the mileage page when benefits has vanQuestion set to false and not prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanQuestion = Some(false)))))), userRequest)
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/mileage?employmentId=$employmentId")
      }
    }

    "redirect to the check employment benefits page when benefits has vanQuestion set to false and prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanQuestion = Some(false)))))), userRequest)
        urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to the check employment benefits page when benefits has carVanFuelQuestion set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(BenefitsViewModel(None, isUsingCustomerData = true)))), userRequest)
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
      import Selectors._
      import user.specificExpectedResults._

        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

          "should render How much was your company van fuel benefit? page with empty error text when there no input" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
              urlPost(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an BAD_REQUEST status" in {
              result.status shouldBe BAD_REQUEST
            }

            titleCheck(get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(user.commonExpectedResults.expectedCaption)
            textOnPageCheck(user.commonExpectedResults.previousExpectedContent + " " + get.expectedContent, contentSelector)
            textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck("", inputSelector)

            buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
            errorAboveElementCheck(get.emptyErrorText)
          }

          "should render How much was your company van fuel benefit? page with wrong format text when input is in incorrect format" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
              urlPost(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "abc"))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an BAD_REQUEST status" in {
              result.status shouldBe BAD_REQUEST
            }

            titleCheck(get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(user.commonExpectedResults.expectedCaption)
            textOnPageCheck(user.commonExpectedResults.previousExpectedContent + " " + get.expectedContent, contentSelector)
            textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldValueCheck("abc", inputSelector)

            buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
            errorAboveElementCheck(get.wrongFormatErrorText)
          }

          "should render How much was your company van fuel benefit? page with max error when input > 100,000,000,000" which {

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
              urlPost(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)),
                body = Map("amount" -> "9999999999999999999999999999"))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an BAD_REQUEST status" in {
              result.status shouldBe BAD_REQUEST
            }

            titleCheck(get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(user.commonExpectedResults.expectedCaption)
            textOnPageCheck(user.commonExpectedResults.previousExpectedContent + " " + get.expectedContent, contentSelector)
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

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "redirect to mileage benefits page when a valid form is submitted and a prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
        urlPost(url(taxYearEOY), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/benefits/mileage?employmentId=001")
      }

      "updates the CYA model with the new value" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        val vanFuelAmount: Option[BigDecimal] = cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel))
        vanFuelAmount shouldBe Some(100)
      }
    }

    "redirect to mileage benefit question page when a valid form is submitted and not a prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
        urlPost(url(taxYearEOY), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/benefits/mileage?employmentId=001")
      }

      "updates the CYA model with the new value" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        val vanFuelAmount: Option[BigDecimal] = cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel))
        vanFuelAmount shouldBe Some(100)
      }
    }

    "redirect user to the check your benefits page with no cya data" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(url(taxYearEOY), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))            }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect user to the tax overview page when in year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(_.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(mileage = None))))))))), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(van = None))))), userRequest)
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(url(taxYear), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))            }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }

    "redirect to the van fuel question page when benefits has carVanFuelQuestion set to true but van fuel question empty" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanFuelQuestion = None))))), userRequest)
        urlPost(url(taxYearEOY), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))            }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/van-fuel?employmentId=$employmentId")
      }
    }

    "redirect to the mileage page when benefits has vanFuelQuestion set to false and not prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanFuelQuestion = Some(false)))))), userRequest)
        urlPost(url(taxYearEOY), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))            }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/mileage?employmentId=$employmentId")
      }
    }

    "redirect to the check employment benefits page when benefits has vanFuelQuestion set to false and prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanFuelQuestion = Some(false)))))), userRequest)
        urlPost(url(taxYearEOY), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))            }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to the mileage page when benefits has vanQuestion set to false and not prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanQuestion = Some(false)))))), userRequest)
        urlPost(url(taxYearEOY), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))            }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/mileage?employmentId=$employmentId")
      }
    }

    "redirect to the check employment benefits page when benefits has vanQuestion set to false and prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(vanQuestion = Some(false)))))), userRequest)
        urlPost(url(taxYearEOY), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))            }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to the check employment benefits page when benefits has carVanFuelQuestion set to false" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true, Some(BenefitsViewModel(None, isUsingCustomerData = true)))), userRequest)
        urlPost(url(taxYearEOY), follow=false,
          welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))            }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }
  }
}
