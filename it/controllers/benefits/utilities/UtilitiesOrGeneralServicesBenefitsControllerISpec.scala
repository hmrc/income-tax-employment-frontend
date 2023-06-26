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

package controllers.benefits.utilities

import forms.YesNoForm
import models.benefits.UtilitiesAndServicesModel
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, medicalDentalChildcareLoansBenefitsUrl, overviewUrl, telephoneBenefitsUrl, utilitiesOrGeneralServicesBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class UtilitiesOrGeneralServicesBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the 'Did you get utility or general service employment benefits' page" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val model = aBenefitsViewModel.copy(utilitiesAndServicesModel = None)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = Some(model))))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          urlGet(fullUrl(utilitiesOrGeneralServicesBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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

      "redirect the user to the check employment benefits page when theres no benefits and prior benefits exist" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = None)))
          urlGet(fullUrl(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true

        }
      }
    }
  }

  ".submit" should {
    "should render qualifying relocation benefits amount page with empty error text when there no input" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)), body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to telephone benefits page and update the UtilitiesAndServicesQuestion to yes and when the user chooses yes" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(UtilitiesAndServicesModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the telephone benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(telephoneBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        val utilitiesAndServicesData = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get.employment.employmentBenefits.get.utilitiesAndServicesModel.get
        utilitiesAndServicesData shouldBe UtilitiesAndServicesModel(sectionQuestion = Some(true))
      }
    }

    "redirect to medical dental childcare benefits page when 'Utilities or general services' is false" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = None)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        val utilitiesAndServicesData = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get.employment.employmentBenefits.get.utilitiesAndServicesModel.get
        utilitiesAndServicesData shouldBe UtilitiesAndServicesModel(sectionQuestion = Some(false))
      }
    }

    "redirect to overview page if the user tries to hit this page with current taxYear" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(utilitiesOrGeneralServicesBenefitsUrl(taxYear, employmentId)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
  }
}
