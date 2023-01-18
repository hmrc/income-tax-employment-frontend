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
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, otherServicesBenefitsUrl, overviewUrl, professionalFeesOrSubscriptionsBenefitsAmountUrl, professionalFeesOrSubscriptionsBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ProfessionalSubscriptionsBenefitsControllerISpec extends IntegrationTest with EmploymentDatabaseHelper with ViewHelpers {

  private val employmentId: String = "employmentId"

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits, employment = employmentCyaModel)

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the employer professional subscriptions page with no pre-filled radio buttons" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptionsQuestion = None)))
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK($OK) status" in {
        result.status shouldBe OK
      }
    }

    "render the employer professional subscriptions page with the yes radio button pre-filled and the user has cya data and prior benefits" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYearEOY, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK($OK) status" in {
        result.status shouldBe OK
      }
    }

    "render the employer professional subscriptions page with the no radio button pre-filled and the user has cya data but no prior benefits" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false))))
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYearEOY, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK($OK) status" in {
        result.status shouldBe OK
      }
    }

    "redirect to the overview page when the tax year isn't end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check your benefits page when there is no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }

  ".submit" should {
    "return an error when a form is submitted with no entry" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYearEOY, employmentId)), body = "",
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an BAD REQUEST($BAD_REQUEST) status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "successfully submit a form when a user answers no and has prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = None)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"update the utilitiesAndServicesModel model and redirect to other services benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(otherServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptionsQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions)) shouldBe None
      }
    }

    "successfully submit a form when a user answers yes and doesn't have prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel
          .copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptionsQuestion = None)))
          .copy(medicalChildcareEducationModel = None)
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"update the utilitiesAndServicesModel model and redirect to the professional subscriptions amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptionsQuestion)) shouldBe Some(true)
      }

    }

    "redirect to the overview page when the tax year isn't end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYear, employmentId)), body = "", follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }

    }

    "redirect to the check your benefits page when there is no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
