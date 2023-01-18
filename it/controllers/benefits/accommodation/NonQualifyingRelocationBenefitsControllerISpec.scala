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

package controllers.benefits.accommodation

import forms.YesNoForm
import models.benefits.AccommodationRelocationModel
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, nonQualifyingRelocationBenefitsAmountUrl, nonQualifyingRelocationBenefitsUrl, overviewUrl, travelOrEntertainmentBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class NonQualifyingRelocationBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the non-qualifying relocation benefits question page" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect user to check employment benefits page" when {
      "user has no benefits" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "user has no accommodation relocation benefits and prior benefits exist" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = Some(false))))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }

    "redirect user to tax overview page if it's not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" should {
    s"return a BAD_REQUEST($BAD_REQUEST) if no value is submitted" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to non qualifying relocation amount page when user selects Yes and prior benefits exist" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates non-QualifyingRelocation Expenses Question to Yes" in {
        lazy val cya = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpensesQuestion)) shouldBe Some(true)
      }
    }

    "redirect to travel entertainment page and wipe the non-Qualifying Relocation Amount if user selects No" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(travelOrEntertainmentBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates non-QualifyingRelocation Expenses Question to No and removes non-QualifyingRelocation Expenses amount" in {
        lazy val cya = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationQuestion)) shouldBe Some(true)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation)) shouldBe Some(100.00)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpensesQuestion)) shouldBe Some(true)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses)) shouldBe Some(200.00)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpensesQuestion)) shouldBe Some(false)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses)) shouldBe None
      }
    }

    "redirect to non qualifying relocation amount page if valid form is submitted and no prior benefits exist" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        urlPost(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates non-QualifyingRelocation Expenses Question to Yes" in {
        lazy val cya = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpensesQuestion)) shouldBe Some(true)
      }
    }

    "redirect to check employment benefits page when there is no CYA data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlPost(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to tax overview page if it's not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(nonQualifyingRelocationBenefitsUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }
}