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

package controllers.benefits.income

import forms.AmountForm
import models.benefits.{BenefitsViewModel, IncomeTaxAndCostsModel}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.IncomeTaxAndCostsModelBuilder.anIncomeTaxAndCostsModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, incomeTaxBenefitsAmountUrl, incurredCostsBenefitsUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class IncomeTaxBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the how much of your client’s Income Tax did their employer pay?' page with an empty amount field" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirector = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        insertCyaData(anEmploymentUserData)
        urlGet(fullUrl(incomeTaxBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlGet(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the income tax benefits question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirectorQuestion = Some(false), incomeTaxPaidByDirector = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the income tax or incurred costs question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(IncomeTaxAndCostsModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the benefits received question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = true)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }

  ".submit" should {
    "render page with error when a form is submitted with no entry" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), body = "",
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an BAD REQUEST($BAD_REQUEST) status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "Update the incomeTaxPaidByDirector value when a user submits a valid form and has prior benefits, redirects to CYA" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates incomeTaxPaidByDirector value to the new amount" in {
        lazy val cyaData = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaData.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirectorQuestion)) shouldBe Some(true)
        cyaData.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirector)) shouldBe Some(newAmount)
      }
    }

    "Update the incomeTaxPaidByDirector value when a user submits a valid form and has no prior benefits, redirects to incurred costs" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        urlPost(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the incurred costs question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(incurredCostsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates incomeTaxPaidByDirector value to the new amount" in {
        lazy val cyaData = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaData.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirectorQuestion)) shouldBe Some(true)
        cyaData.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirector)) shouldBe Some(newAmount)
      }
    }

    "redirect the user to the overview page when it is in year" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(incomeTaxBenefitsAmountUrl(taxYear, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check your benefits page when there is no cya data found" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = true)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the income tax benefits question is set to false" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirectorQuestion = Some(false), incomeTaxPaidByDirector = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the check employment benefits page when incomeTaxOrCostsQuestion is set to false" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirectorQuestion = Some(false), incomeTaxPaidByDirector = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
