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

import forms.AmountForm
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.MedicalChildcareEducationModelBuilder.aMedicalChildcareEducationModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{checkYourBenefitsUrl, childcareBenefitsUrl, fullUrl, medicalDentalBenefitsAmountUrl, medicalDentalBenefitsUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class MedicalOrDentalBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"
  private val newAmount: BigDecimal = 250

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the medical or dental benefits amount page with no pre-filled amount" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsurance = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe ""
        result.status shouldBe OK
      }
    }

    "render the medical or dental benefits amount page with pre-filled amount" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe "100"
        result.status shouldBe OK
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "Redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(medicalDentalBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the medical insurance question page when there is a medical insurance amount but no medicalInsuranceQuestion" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          urlGet(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(medicalDentalBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when benefits has medicalInsuranceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = Some(false), medicalInsurance = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          urlGet(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the medical insurance question page when there's no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None, medicalInsurance = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          urlGet(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(medicalDentalBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    "return an error when the medical or dental benefits amount is too large" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> "2353453425345234")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsurance = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to another page when request is valid" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      "Redirect to child care page when all is valid in previous data flow" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(childcareBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsuranceQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance)) shouldBe Some(newAmount)
        }
      }

      "Redirect to child care page when all is valid in new data flow" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(childcareBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsuranceQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance)) shouldBe Some(newAmount)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(medicalDentalBenefitsAmountUrl(taxYear, employmentId)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the medical insurance question page when there is a medical insurance amount but no medicalInsuranceQuestion" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          urlPost(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(medicalDentalBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when benefits has medicalInsuranceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = Some(false), medicalInsurance = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          urlPost(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the medical insurance question page when there's no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None, medicalInsurance = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          urlPost(fullUrl(medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(medicalDentalBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }
  }
}
