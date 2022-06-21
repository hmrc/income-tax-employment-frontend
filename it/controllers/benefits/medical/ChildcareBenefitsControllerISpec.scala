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

package controllers.benefits.medical

import forms.YesNoForm
import models.benefits.{BenefitsViewModel, MedicalChildcareEducationModel}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.MedicalChildcareEducationModelBuilder.aMedicalChildcareEducationModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{checkYourBenefitsUrl, childcareBenefitsAmountUrl, childcareBenefitsUrl, educationalServicesBenefitsUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ChildcareBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the 'Did you get childcare benefits' page with the correct content without pre-filled form" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(nurseryPlacesQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "redirect to overview page if the user tries to hit this page with current taxYear" when {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(childcareBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(isPriorSubmission = true, hasPriorBenefits = true, employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "doesn't create any benefits data" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          cyaModel.employment.employmentBenefits shouldBe None
        }
      }

      "redirect the user to the check employment benefits page when the medicalChildcareEducation question is false" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(MedicalChildcareEducationModel(sectionQuestion = Some(false))))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          urlGet(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = true)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          urlGet(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    "should render 'Did you get childcare benefits' page with empty error text when there no input" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to child care amount page and update the NurseryPlacesQuestion to yes when the user chooses yes, has prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the child care amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(childcareBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update the NurseryPlacesQuestion to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlacesQuestion)) shouldBe Some(true)
      }
    }

    "redirect to educational services page and update NurseryPlacesQuestion to no when user chooses no, has prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the educational services page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(educationalServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update the NurseryPlacesQuestion to false, and nurseryPlaces to None" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlacesQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlaces)) shouldBe None
      }
    }

    "redirect to the check employment benefits page when a valid form is submitted and no prior benefits" when {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        urlPost(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(childcareBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to overview page if the user tries to hit this page with current taxYear" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(childcareBenefitsUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check employment benefits page when theres no CYA data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "doesn't create any benefits data" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits shouldBe None
      }
    }

    "redirect the user to the check employment benefits page when the medicalChildcareEducation question is false" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(MedicalChildcareEducationModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "redirects to the check employment benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = true)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(childcareBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "redirects to the check employment benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
