/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.YesNoForm
import models.benefits.AssetsModel
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.AssetsModelBuilder.anAssetsModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{assetsForUseBenefitsAmountUrl, assetsForUseBenefitsUrl, assetsToKeepBenefitsUrl, checkYourBenefitsUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class AssetsBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  private val assetsSoFar: AssetsModel = AssetsModel(sectionQuestion = Some(true), None, None, None, None)

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the assets benefits page without pre-filled radio buttons and the user doesn't have prior benefits" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetsQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        urlGet(fullUrl(assetsForUseBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK($OK) status" in {
        result.status shouldBe OK
      }
    }

    "redirect to check your benefits page there is no cya data for found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(assetsForUseBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to check your benefits page when assetsAndAssetsTransferQuestion is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(assetsForUseBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the overview page when it's not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
        urlGet(fullUrl(assetsForUseBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" should {
    "return an error when a user submits an empty form" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsForUseBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a BAD REQUEST($BAD_REQUEST) status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "update cya with 'yes' when a user chooses yes and doesn't have prior benefits, redirects to assets amount page" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(assetsSoFar))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        urlPost(fullUrl(assetsForUseBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the assets amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(assetsForUseBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update assetsQuestion to Some(true) in assetsModel" in {
        lazy val model = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assetsQuestion)) shouldBe Some(true)
      }
    }

    "update cya with 'no' when a user choose no and has prior benefits, redirects to CYA" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(assetsForUseBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update assetsQuestion to Some(false) and assets to None" in {
        lazy val model = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assetsQuestion)) shouldBe Some(false)
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assets)) shouldBe None
      }
    }

    "update cya with 'no' when a user choose no and has no prior benefits, redirects to asset transfers question" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(assetsSoFar))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        urlPost(fullUrl(assetsForUseBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the assets transfers page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(assetsToKeepBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update assetsQuestion to Some(false) and assets to None" in {
        lazy val model = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assetsQuestion)) shouldBe Some(false)
        model.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assets)) shouldBe None
      }
    }

    "redirect to check your benefits page there is no cya data for found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlPost(fullUrl(assetsForUseBenefitsUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to check your benefits page when assetsAndAssetsTransferQuestion is false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(assetsForUseBenefitsUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the overview page when it's not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
        urlPost(fullUrl(assetsForUseBenefitsUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }
}
