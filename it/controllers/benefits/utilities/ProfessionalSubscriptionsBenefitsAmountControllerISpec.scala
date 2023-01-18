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
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, otherServicesBenefitsUrl, overviewUrl, professionalFeesOrSubscriptionsBenefitsAmountUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ProfessionalSubscriptionsBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employment = employmentCyaModel)

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the professional subscriptions benefits amount page with no pre-filled form" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptions = None)))
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe ""
        result.status shouldBe OK
      }
    }

    "render the professional subscriptions benefits amount page with a pre-filled form" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe "300"
        result.status shouldBe OK
      }
    }

    "render the professional subscriptions benefits amount page with pre-filled form" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect user to check employment benefits page when there is no cya data" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to tax overview page if it is not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlGet(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" should {
    "render the professional subscriptions benefits amount page with empty input error when no input is submitted" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> ""))
      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to other services page" when {
      "valid form is submitted and it's a prior submission" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = None)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
          urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "100"))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(otherServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "updates employerProvidedProfessionalSubscriptions to the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions)) shouldBe Some(100.0)
        }
      }

      "there is no cya data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }

    "redirect to other services page when valid form is submitted and not a prior submission" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = None)
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "100"))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(otherServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates employerProvidedProfessionalSubscriptions to the new value" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions)) shouldBe Some(100.0)
      }
    }

    "redirect to tax overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(fullUrl(professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYear, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }
}


