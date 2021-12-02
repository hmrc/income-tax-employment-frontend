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

package controllers.benefits.reimbursed

import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.User
import models.benefits.{BenefitsViewModel, ReimbursedCostsVouchersAndNonCashModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ReimbursedCostsVouchersAndNonCashBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId: String = "001"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/reimbursed-costs-vouchers-non-cash-benefits?employmentId=$employmentId"

  val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/reimbursed-costs-vouchers-non-cash-benefits?employmentId=$employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails("employerName", currentDataIsHmrcHeld = true), benefits)

  def benefits(reimbursedModel: Option[ReimbursedCostsVouchersAndNonCashModel]): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      medicalChildcareEducationModel = Some(fullMedicalChildcareEducationModel),
      incomeTaxAndCostsModel = Some(fullIncomeTaxOrIncurredCostsModel),
      reimbursedCostsVouchersAndNonCashModel = reimbursedModel,
      isBenefitsReceived = true, isUsingCustomerData = true)

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
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any reimbursed costs, vouchers or non-cash benefits from your employer?"
    val expectedH1 = "Did you get any reimbursed costs, vouchers or non-cash benefits from your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got reimbursed costs, vouchers or non-cash benefits from your employer"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get any reimbursed costs, vouchers or non-cash benefits from your employer?"
    val expectedH1 = "Did you get any reimbursed costs, vouchers or non-cash benefits from your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got reimbursed costs, vouchers or non-cash benefits from your employer"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any reimbursed costs, vouchers or non-cash benefits from their employer?"
    val expectedH1 = "Did your client get any reimbursed costs, vouchers or non-cash benefits from their employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got reimbursed costs, vouchers or non-cash benefits from their employer"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any reimbursed costs, vouchers or non-cash benefits from their employer?"
    val expectedH1 = "Did your client get any reimbursed costs, vouchers or non-cash benefits from their employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got reimbursed costs, vouchers or non-cash benefits from their employer"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
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

        "Render the 'Reimbursed Costs Vouchers And Non Cash Benefits' question page with no pre-filled data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(None)))), userRequest)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          radioButtonCheck(yesText, 1,checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

        "Render the 'Reimbursed Costs Vouchers And Non Cash Benefits' question page with 'yes' as pre-filled data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "Render the 'Reimbursed Costs Vouchers And Non Cash Benefits' question page with 'no' as pre-filled data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(Some(emptyReimbursedCostsVouchersAndNonCashModel))))), userRequest)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "Redirect to the Check Employment Benefits page when there is no in-session data for the user" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "Redirect to the Check Employment Benefits page when the benefits received flag is false" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel(benefits = Some(BenefitsViewModel(isUsingCustomerData = true)))), userRequest)
        urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "Redirect to the overview page if it is not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(url(taxYear), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }
      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some("http://localhost:11111/update-and-submit-income-tax-return/2022/view")
      }
    }

  }


    ".submit" should {
      userScenarios.foreach { user =>
        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

          "Render the 'Reimbursed Costs Vouchers And Non Cash Benefits' question page with an error when no value is submitted" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "returns a bad request" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
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

      "Redirect to overview page if it is not EOY" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url(taxYear), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
        }
        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/update-and-submit-income-tax-return/2022/view")
        }
      }

      "Update the reimbursedCostsVouchersAndNonCashQuestion to true when user selects yes and there is previous cya data" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(Some(emptyReimbursedCostsVouchersAndNonCashModel))))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your benefits page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "update the reimbursedCostsVouchersAndNonCashQuestion to true" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.sectionQuestion) shouldBe Some(true)
        }
      }

      "Update the reimbursedCostsVouchersAndNonCashQuestion to false and remove reimbursed section data when user selects no" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(Some(fullReimbursedCostsVouchersAndNonCashModel))))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your benefits page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "updates the reimbursedCostsVouchersAndNonCashQuestion to false and wipes the other values" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.sectionQuestion) shouldBe Some(false)
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.expensesQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.expenses) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.taxableExpensesQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.taxableExpenses) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.vouchersAndCreditCardsQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.vouchersAndCreditCards) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.nonCashQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.nonCash) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.otherItemsQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.otherItems) shouldBe None
        }

      }

      "Create a new ReimbursedCostsVouchersAndNonCashModel and redirect to Check Employment Benefits page when user selects 'yes' and no prior benefits" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(None)))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your benefits page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "creates the ReimbursedCostsVouchersAndNonCashModel with reimbursedCostsVouchersAndNonCashQuestion set to true" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.sectionQuestion) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.expensesQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.expenses) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.taxableExpensesQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.taxableExpenses) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.vouchersAndCreditCardsQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.vouchersAndCreditCards) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.nonCashQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.nonCash) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.otherItemsQuestion) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.otherItems) shouldBe None
        }
      }
    }
}
