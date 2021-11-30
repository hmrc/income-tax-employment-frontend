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

package controllers.benefits.assets

import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.User
import models.benefits.{AssetsModel, BenefitsViewModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class AssetsBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = 2021
  private val employmentId: String = "001"

  private def assetsBenefitsPageUrl(taxYear: Int): String = s"$appUrl/$taxYear/benefits/assets-available-for-use?employmentId=$employmentId"

  private val formPostLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/assets-available-for-use?employmentId=$employmentId"

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val youCanUseTextSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val continueText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val youCanUseText: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your employer make any assets available for your use?"
    val expectedHeading = "Did your employer make any assets available for your use?"
    val youCanUseText = "You can use these assets but you do not own them."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your employer made assets available for your use"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did your employer make any assets available for your use?"
    val expectedHeading = "Did your employer make any assets available for your use?"
    val youCanUseText = "You can use these assets but you do not own them."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your employer made assets available for your use"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer make any assets available for their use?"
    val expectedHeading = "Did your client’s employer make any assets available for their use?"
    val youCanUseText = "They can use these assets but they do not own them."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client’s employer made assets available for their use"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer make any assets available for their use?"
    val expectedHeading = "Did your client’s employer make any assets available for their use?"
    val youCanUseText = "They can use these assets but they do not own them."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client’s employer made assets available for their use"
  }

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits, employmentCyaModel)

  def cyaModel(benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName = "employerName", currentDataIsHmrcHeld = true), benefits)

  def benefits(assetsModel: Option[AssetsModel]): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      medicalChildcareEducationModel = Some(fullMedicalChildcareEducationModel), incomeTaxAndCostsModel = Some(fullIncomeTaxOrIncurredCostsModel),
      reimbursedCostsVouchersAndNonCashModel = Some(fullReimbursedCostsVouchersAndNonCashModel),
      assetsModel = assetsModel,
      isBenefitsReceived = true, isUsingCustomerData = true)

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the assets benefits page without pre-filled radio buttons and the user doesn't have prior benefits" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(
              Some(fullAssetsModel.copy(assetsQuestion = None)))))), userRequest)
            urlGet(assetsBenefitsPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.youCanUseText, youCanUseTextSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(formPostLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the assets benefits page with the 'yes' radio button prefilled and the user has prior benefits" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(Some(fullAssetsModel))))), userRequest)
            urlGet(assetsBenefitsPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.youCanUseText, youCanUseTextSelector)
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(formPostLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the assets benefits page with the 'no' radio button prefilled and the user doesn't have prior benefits" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(
              Some(fullAssetsModel.copy(assetsQuestion = Some(false))))))), userRequest)
            urlGet(assetsBenefitsPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.youCanUseText, youCanUseTextSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(true))
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(formPostLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }
      }

    }

    "redirect to check your benefits page there is no cya data for found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(assetsBenefitsPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to check your benefits page when assetsAndAssetsTransferQuestion is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(Some(emptyAssetsModel))))), userRequest)
        urlGet(assetsBenefitsPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the overview page when it's not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(Some(fullAssetsModel))))), userRequest)
        urlGet(assetsBenefitsPageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error when a user submits an empty form" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")


          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(Some(fullAssetsModel))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(assetsBenefitsPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a BAD REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.youCanUseText, youCanUseTextSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(formPostLink, formSelector)

          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText, Some("value"))
        }
      }
    }

    "update cya with 'yes' and a user chooses yes and doesn't have prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(Some(
          fullAssetsModel.copy(assetsQuestion = None)))))), userRequest)
        urlPost(assetsBenefitsPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the assets amount page" in {
        result.status shouldBe SEE_OTHER
        //TODO - change to assets amount page when available
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "update assetsQuestion to Some(true) in assetsModel" in {
        lazy val model = findCyaData(taxYearEOY, employmentId, userRequest).get
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assetsQuestion)) shouldBe Some(true)
      }
    }

    "update cya with 'no' when a user choose no and has prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(Some(fullAssetsModel))))), userRequest)
        urlPost(assetsBenefitsPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "update assetsQuestion to Some(false) and assets to None" in {
        lazy val model = findCyaData(taxYearEOY, employmentId, userRequest).get
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assetsQuestion)) shouldBe Some(false)
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assets)) shouldBe None
      }
    }

    "redirect to check your benefits page there is no cya data for found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(assetsBenefitsPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "redirect to check your benefits page when assetsAndAssetsTransferQuestion is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel(Some(benefits(Some(emptyAssetsModel))))), userRequest)
        urlPost(assetsBenefitsPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the overview page when it's not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel(Some(benefits(Some(fullAssetsModel))))), userRequest)
        urlPost(assetsBenefitsPageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

  }

}
