/*
 * Copyright 2022 HM Revenue & Customs
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

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.benefits.AssetsModelBuilder.anAssetsModel
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import forms.YesNoForm
import models.benefits.AssetsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{assetsToKeepBenefitsAmountUrl, assetsToKeepBenefitsUrl, checkYourBenefitsUrl, fullUrl, overviewUrl}
import org.scalatest.DoNotDiscover

@DoNotDiscover
class AssetTransfersBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId: String = "employmentId"
  private val assetsSoFar: AssetsModel = AssetsModel(sectionQuestion = Some(true), assetsQuestion = Some(true), assets = Some(100.00), None, None)

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val paragraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraph: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your employer give you any assets to keep?"
    val expectedHeading = "Did your employer give you any assets to keep?"
    val expectedParagraph = "You became the owner of these assets."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your employer gave you assets to keep"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did your employer give you any assets to keep?"
    val expectedHeading = "Did your employer give you any assets to keep?"
    val expectedParagraph = "You became the owner of these assets."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your employer gave you assets to keep"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer give them any assets to keep?"
    val expectedHeading = "Did your client’s employer give them any assets to keep?"
    val expectedParagraph = "Your client became the owner of these assets."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client’s employer gave them assets to keep"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer give them any assets to keep?"
    val expectedHeading = "Did your client’s employer give them any assets to keep?"
    val expectedParagraph = "Your client became the owner of these assets."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client’s employer gave them assets to keep"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    import Selectors._

    userScenarios.foreach { user =>
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render 'asset transfers' yes/no page with the correct content with no pre-filling" which {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransferQuestion = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
            urlGet(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(assetsToKeepBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'asset transfers' yes/no page with the correct content with yes pre-filled" which {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransferQuestion = Some(true))))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
            urlGet(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(assetsToKeepBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'asset transfers' yes/no page with the correct content with no pre-filled" which {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransferQuestion = Some(false))))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
            urlGet(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(assetsToKeepBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(isBenefitsReceived = false)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
        urlGet(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the check employment benefits page when theres no session data for that user" which {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the overview page when the request is in year" which {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData, aUserRequest)
        urlGet(fullUrl(assetsToKeepBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check employment benefits page when theres no CYA data" which {
      lazy val result = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "doesn't create any benefits data" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
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
            val form = Map(YesNoForm.yesNo -> "")
            lazy val result = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(anEmploymentUserData, aUserRequest)
              urlPost(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(assetsToKeepBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText, Some("value"))
          }
        }
      }
    }

    "Update the asset transfers question to no, clear assets transfers amount when the user chooses no and redirects to CYA when prior submission" which {
      val form = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates asset transfers question to no and asset transfers amount to none" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.assetsModel) shouldBe Some(anAssetsModel.copy(assetTransferQuestion = Some(false), assetTransfer = None))
      }
    }

    "Update the asset transfers question to no, clear assets transfers amount when the user chooses no and redirects to CYA when not prior submission" which {
      val form = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(assetsSoFar))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates asset transfers question to no and asset transfers amount to none" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.assetsModel).flatMap(_.assetTransferQuestion) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.assetsModel).flatMap(_.assetTransfer) shouldBe None
      }
    }

    "Update asset transfers question to yes, redirects to the asset transfers amount page when user chooses yes" which {
      val form = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(assetsSoFar))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the asset transfers amount page page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates asset transfers question to yes" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assetTransferQuestion)) shouldBe Some(true)
      }
    }

    "redirect the user to the overview page when it is in year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsToKeepBenefitsUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check employment benefits page when theres no CYA data" which {
      lazy val form = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "doesn't create any benefits data" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits shouldBe None
      }
    }

    "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
      val form = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(isBenefitsReceived = false)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
        urlPost(fullUrl(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
