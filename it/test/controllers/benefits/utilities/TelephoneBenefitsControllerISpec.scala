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
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.UtilitiesAndServicesModelBuilder.aUtilitiesAndServicesModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{checkYourBenefitsUrl, companyBenefitsUrl, employerProvidedServicesBenefitsUrl, fullUrl, overviewUrl, telephoneBenefitsAmountUrl, telephoneBenefitsUrl, utilitiesOrGeneralServicesBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class TelephoneBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employment = employmentCyaModel)

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render 'Did you get a benefit for using a telephone?' page" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(telephoneQuestion = None)))
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "redirect the user to the check employment benefits page when theres no benefits and prior submission" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true

        }
      }

      "redirect the user to the benefits received page when theres no benefits and not prior submission" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel().copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(companyBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when theres no session data for that user" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when the utilities and services question is false" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(sectionQuestion = Some(false))))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
          urlGet(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true

        }
      }

      "redirect the user to the utilities and services question page when the utilities and services question is empty" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(sectionQuestion = None)))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
          urlGet(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true

        }
      }

      "redirect the user to the overview page when the request is in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = Some(aBenefitsViewModel))))
          urlGet(fullUrl(telephoneBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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


  ".submit" should {

    s"return a BAD_REQUEST($BAD_REQUEST) status" when {
      "the value is empty" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = Some(aBenefitsViewModel))))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {
      "redirect to employer services page, update the telephoneQuestion to no and wipe the telephone amount" +
        " data when the user chooses no" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = None)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), body = form,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the employer services page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(employerProvidedServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.telephoneQuestion)) shouldBe Some(false)
          cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.telephone)) shouldBe None
        }
      }

      "redirect to telephone amount page and update the telephoneQuestion to yes and when the user chooses yes" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel
            .copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(telephoneQuestion = Some(false))))
            .copy(medicalChildcareEducationModel = None)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the telephone amount page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(telephoneBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.telephoneQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.telephone)) shouldBe Some(100)
        }

      }

      "redirect the user to the overview page when it is in year" which {
        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(telephoneBenefitsUrl(taxYear, employmentId)), body = "",
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), body = form,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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

      "redirect the user to the check employment benefits page when the utilities and services question is false" which {

        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(sectionQuestion = Some(false))))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
          urlPost(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), body = form,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true

        }
      }

      "redirect the user to the utilities and services question page when the utilities and services question is empty" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(sectionQuestion = None)))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
          urlPost(fullUrl(telephoneBenefitsUrl(taxYearEOY, employmentId)), body = form,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }
  }
}
