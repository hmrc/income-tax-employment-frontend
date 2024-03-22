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

package controllers.benefits.travel

import forms.AmountForm
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.TravelEntertainmentModelBuilder.aTravelEntertainmentModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{checkYourBenefitsUrl, entertainmentExpensesBenefitsUrl, fullUrl, incidentalOvernightCostsBenefitsAmountUrl, incidentalOvernightCostsBenefitsUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class IncidentalCostsBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = hasPriorBenefits, hasPriorBenefits = hasPriorBenefits, employment = employmentCyaModel)

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the 'incidental overnight expenses amount' page without pre-filled form" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpenses = None)))
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK($OK) status" in {
        getInputFieldValue() shouldBe ""
        result.status shouldBe OK
      }
    }

    "render the 'incidental overnight expenses amount' page with a pre-filled form" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlGet(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK($OK) status" in {
        getInputFieldValue() shouldBe "200"
        result.status shouldBe OK
      }
    }

    "redirect to the overview page when it's not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel().copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlGet(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the incidental overnight expenses radio button page when the personalIncidentalExpensesQuestion is None " which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = None)))
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(incidentalOvernightCostsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the entertaining question when the personalIncidentalExpensesQuestion is Some(false) and no prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false))))
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when personalIncidentalExpensesQuestion is Some(false) and prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false))))
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }

  ".submit" should {
    "return an error" when {
      "a form is submitted with no entry" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel().copy(employmentBenefits = Some(aBenefitsViewModel))))
          urlPost(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = "",
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has an BAD REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
      }
    }

    "update cya when a valid form is submitted and prior benefits exist" which {
      val newAmount: BigDecimal = 280.35
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val model = aBenefitsViewModel.copy(utilitiesAndServicesModel = None)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel().copy(employmentBenefits = Some(model))))
        urlPost(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to the entertaining page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpenses)) shouldBe Some(newAmount)
      }
    }

    "update cya when a valid form is submitted and no prior benefits exist" which {
      val newAmount: BigDecimal = 534.21
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel
          .copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpenses = None)))
          .copy(utilitiesAndServicesModel = None)
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"redirect to the entertaining question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpenses)) shouldBe Some(newAmount)
      }
    }

    "redirect to the overview page when it's not end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel().copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlGet(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the incidental overnight expenses radio button page when the personalIncidentalExpensesQuestion is None " which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = None)))
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = "",
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(incidentalOvernightCostsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the entertaining question when the personalIncidentalExpensesQuestion is Some(false) and no prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false))))
        insertCyaData(employmentUserData(hasPriorBenefits = false, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when personalIncidentalExpensesQuestion is Some(false) and prior benefits exist" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(personalIncidentalExpensesQuestion = Some(false))))
        insertCyaData(employmentUserData(hasPriorBenefits = true, anEmploymentCYAModel().copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
