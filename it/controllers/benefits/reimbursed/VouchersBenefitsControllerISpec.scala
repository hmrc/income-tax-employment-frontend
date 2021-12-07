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
import controllers.benefits.reimbursed.routes.{NonCashBenefitsController, VouchersBenefitsAmountController}
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

class VouchersBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId: String = "001"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId,
      isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  private def cyaModel(benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails("employerName", currentDataIsHmrcHeld = true), benefits)

  private def benefits(reimbursedCostsVouchersAndNonCashModel: ReimbursedCostsVouchersAndNonCashModel): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      isUsingCustomerData = true, isBenefitsReceived = true, medicalChildcareEducationModel = Some(fullMedicalChildcareEducationModel),
      incomeTaxAndCostsModel = Some(fullIncomeTaxAndCostsModel),
      reimbursedCostsVouchersAndNonCashModel = Some(reimbursedCostsVouchersAndNonCashModel))

  private def pageUrl(taxYear: Int) = s"$appUrl/$taxYear/benefits/vouchers-or-credit-cards?employmentId=$employmentId"

  private val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/vouchers-or-credit-cards?employmentId=$employmentId"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val paragraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p"
    val bullet1Selector: String = "#main-content > div > div > form > div > fieldset > legend > ul.govuk-list > li:nth-child(1)"
    val bullet2Selector: String = "#main-content > div > div > form > div > fieldset > legend > ul.govuk-list > li:nth-child(2)"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val paragraph: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedBullet1: String
    val expectedBullet2: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get vouchers or credit cards?"
    val expectedHeading = "Did you get vouchers or credit cards?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got vouchers or credit cards"
    val paragraph = "We only need to know about vouchers you can exchange for:"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get vouchers or credit cards?"
    val expectedHeading = "Did you get vouchers or credit cards?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got vouchers or credit cards"
    val paragraph = "We only need to know about vouchers you can exchange for:"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get vouchers or credit cards?"
    val expectedHeading = "Did your client get vouchers or credit cards?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got vouchers or credit cards"
    val paragraph = "We only need to know about vouchers your client can exchange for:"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get vouchers or credit cards?"
    val expectedHeading = "Did your client get vouchers or credit cards?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got vouchers or credit cards"
    val paragraph = "We only need to know about vouchers your client can exchange for:"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedBullet1 = "goods and services"
    val expectedBullet2 = "a sum of money substantially less than the cost to provide them"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val bullet1 = "goods and services"
    val bullet2 = "a sum of money substantially less than the cost to provide them"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedBullet1 = "goods and services"
    val expectedBullet2 = "a sum of money substantially less than the cost to provide them"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val bullet1 = "goods and services"
    val bullet2 = "a sum of money substantially less than the cost to provide them"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {
    import Selectors._

    userScenarios.foreach { user =>

      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render 'vouchers or credit cards' page with the correct content with no pre-filling" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel(benefits = Some(benefits(fullReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)


          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraphSelector)
          textOnPageCheck(expectedBullet1, bullet1Selector)
          textOnPageCheck(expectedBullet2, bullet2Selector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'vouchers or credit cards' page with the correct content with yes pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel(benefits = Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraphSelector)
          textOnPageCheck(expectedBullet1, bullet1Selector)
          textOnPageCheck(expectedBullet2, bullet2Selector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'vouchers or credit cards' page with the correct content with no pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            val model = fullReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = Some(false))
            insertCyaData(employmentUserData(isPrior = true, cyaModel(benefits = Some(benefits(model)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraphSelector)
          textOnPageCheck(expectedBullet1, bullet1Selector)
          textOnPageCheck(expectedBullet2, bullet2Selector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

      "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(employmentUserData(isPrior = true, cyaModel(benefits = Some(BenefitsViewModel(isUsingCustomerData = true)))), userRequest)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect the user to the check employment benefits page when ReimbursedCostsVouchersAndNonCashQuestion is set to false" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel(benefits = Some(benefits(emptyReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect the user to the check employment benefits page when theres no session data for that user" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect the user to the overview page when the request is in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(employmentUserData(isPrior = true, cyaModel(benefits = Some(BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true)))), userRequest)
          urlGet(pageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel()), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "doesn't create any benefits data" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits shouldBe None
        }
      }
  }

  ".submit" should {

    import Selectors._

    userScenarios.foreach { user =>

      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        s"return a BAD_REQUEST($BAD_REQUEST) status" when {
          "the value is empty" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(pageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraphSelector)
            textOnPageCheck(expectedBullet1, bullet1Selector)
            textOnPageCheck(expectedBullet2, bullet2Selector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText, Some("value"))
          }
        }
      }
    }

      "Update the vouchers or credit cards question to no, and vouchers or credit cards to none when the user chooses no, redirects to CYA if prior submission" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel(Some(fullBenefitsModel))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "updates vouchers or credit cards question to no and vouchers or credit cards to none" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCardsQuestion)) shouldBe Some(false)
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCards)) shouldBe None
        }
      }

      "Update the vouchers or credit cards question to no, and vouchers or credit cards to none when the user chooses no," +
        "redirects to non cash benefits if not prior submission" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the non cash benefits question page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(NonCashBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "updates vouchers or credit cards question to no and vouchers or credit cards to none" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCardsQuestion)) shouldBe Some(false)
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCards)) shouldBe None
        }
      }

      "Update the vouchers or credit cards question to yes, redirects to the vouchers or credit cards amount page" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel(Some(benefits(fullReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = Some(false)))))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the vouchers or credit cards amount page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(VouchersBenefitsAmountController.show(taxYearEOY, employmentId).url)
        }

        "updates vouchers or credit cards question to yes" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCardsQuestion)) shouldBe Some(true)
        }
      }

      "redirect the user to the overview page when it is in year" which {
        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel()), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }

        "doesn't create any benefits data" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits shouldBe None
        }
      }

      "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(employmentUserData(isPrior = true, cyaModel(benefits = Some(BenefitsViewModel(isUsingCustomerData = true)))), userRequest)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect the user to the check employment benefits page when reimbursed costs vouchers and non cash question is set to false" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel(benefits = Some(benefits(emptyReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }
  }
}
