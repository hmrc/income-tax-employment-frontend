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

import models.benefits.BenefitsViewModel
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
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, medicalDentalChildcareLoansBenefitsUrl, otherServicesBenefitsAmountUrl, otherServicesBenefitsUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherServicesBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employment = employmentCyaModel)

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val benefitsWithNoBenefitsReceived: Option[BenefitsViewModel] = Some(BenefitsViewModel(isUsingCustomerData = true))

  ".show" should {
    "render the other services amount page without a pre-filled form" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(service = None)))
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe ""
        result.status shouldBe OK
      }
    }

    "render the other services amount page with a pre-filled form" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe "400"
        result.status shouldBe OK
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "Redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = benefitsWithNoBenefitsReceived)))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(otherServicesBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the other services question page when there is a service amount but the other serviceQuestion has not been answered" +
        " but entertainment benefit question empty" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(serviceQuestion = None)))
          insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
          urlGet(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(otherServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when benefits has serviceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(serviceQuestion = Some(false))))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
          urlGet(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when there's no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = benefitsWithNoBenefitsReceived)))
          urlGet(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    "should render the other services amount amount page with empty error text when there no input" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlPost(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to another page when a valid request is made and then" should {
      "redirect to medical dental childcare benefits page when a valid form is submitted and a prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = None)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
          urlPost(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.23"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "updates the CYA model with the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          val otherServicesAmount = cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.service))
          otherServicesAmount shouldBe Some(100.23)
        }
      }

      "redirect to the utility general service next question page when a valid form is submitted and not prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = None)
          insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
          urlPost(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.11"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "updates the CYA model with the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          val otherServicesAmount = cyaModel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.service))
          otherServicesAmount shouldBe Some(100.11)
        }
      }

      "redirect user to the check your benefits page with no cya data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(otherServicesBenefitsAmountUrl(taxYear, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.22"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the other services question page when there is a service amount but the other serviceQuestion has not been answered" +
        " but entertainment benefit question empty" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(serviceQuestion = None)))
          insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
          urlPost(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(otherServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when benefits has serviceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(serviceQuestion = Some(false))))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
          urlPost(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when theres no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = benefitsWithNoBenefitsReceived)))
          urlPost(fullUrl(otherServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }
  }
}
