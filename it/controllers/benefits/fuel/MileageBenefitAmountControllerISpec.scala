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

package controllers.benefits.fuel

import forms.AmountForm
import models.IncomeTaxUserData
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{accommodationRelocationBenefitsUrl, carBenefitsUrl, carFuelBenefitsUrl, checkYourBenefitsUrl, fullUrl, mileageBenefitsAmountUrl, mileageBenefitsUrl, overviewUrl, vanBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class MileageBenefitAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the mileage amount page with no pre-filled amount" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileage = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe ""
        result.status shouldBe OK
      }
    }

    "render the mileage amount page with pre-filled amount" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        getInputFieldValue() shouldBe "500"
        result.status shouldBe OK
      }
    }

    "redirect" when {
      "redirect to accommodation relocation page when the mileage question is no" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileageQuestion = Some(false))))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "mileage question is empty" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileageQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(mileageBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "car section isn't finished" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(carBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "van section isn't finished" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(vanBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

      }
      "car fuel section isn't finished" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carFuelQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(carFuelBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "there is no data in session for that user" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "it is not EOY" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYear)
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    "return an error where there is no entry" which {
      val form: Map[String, String] = Map[String, String]()
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect" when {
      "there is no cya data in session for that user" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel) shouldBe None
        }
      }

      "redirect to accommodation relocation page when the mileage question is no" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")
        lazy val result: WSResponse = {
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileageQuestion = Some(false))))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "it isn't end of year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(mileageBenefitsAmountUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }
    }

    "update mileage amount to 200 when the user submits and prior benefits exist, redirects to accommodation page" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the accommodation page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the mileage benefit to be 200" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe Some(200.00)
      }
    }

    "update mileage amount to 200 when the user submits and no prior benefits exist, redirects to the accommodation relocation page" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the accommodation relocation controller page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the mileage benefit to be 200" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe Some(200.00)
      }
    }
  }
}