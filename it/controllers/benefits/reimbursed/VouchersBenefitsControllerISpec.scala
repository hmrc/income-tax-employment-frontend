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

package controllers.benefits.reimbursed

import forms.YesNoForm
import models.benefits.{BenefitsViewModel, ReimbursedCostsVouchersAndNonCashModel}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.ReimbursedCostsVouchersAndNonCashModelBuilder.aReimbursedCostsVouchersAndNonCashModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, nonCashBenefitsUrl, overviewUrl, vouchersOrCreditCardsBenefitsAmountUrl, vouchersOrCreditCardsBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class VouchersBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employment = employmentCyaModel)

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render 'vouchers or credit cards' page with the correct content with no pre-filling" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = None)))
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(BenefitsViewModel(isUsingCustomerData = true)))))
        urlGet(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the check employment benefits page when ReimbursedCostsVouchersAndNonCashQuestion is set to false" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))))
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the check employment benefits page when theres no session data for that user" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the overview page when the request is in year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlGet(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check employment benefits page when theres no CYA data" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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

  ".submit" should {
    s"return a BAD_REQUEST($BAD_REQUEST) status" when {
      "the value is empty" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }
    }

    "Update the vouchers or credit cards question to no, and vouchers or credit cards to none when the user chooses no, redirects to CYA if prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates vouchers or credit cards question to no and vouchers or credit cards to none" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCardsQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCards)) shouldBe None
      }
    }

    "Update the vouchers or credit cards question to no, and vouchers or credit cards to none when the user chooses no," +
      "redirects to non cash benefits if not prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = None)))
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the non cash benefits question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(nonCashBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates vouchers or credit cards question to no and vouchers or credit cards to none" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCardsQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCards)) shouldBe None
      }
    }

    "Update the vouchers or credit cards question to yes, redirects to the vouchers or credit cards amount page" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCards = None)))
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the vouchers or credit cards amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vouchersOrCreditCardsBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates vouchers or credit cards question to yes" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCardsQuestion)) shouldBe Some(true)
      }
    }

    "redirect the user to the overview page when it is in year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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

    "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(BenefitsViewModel(isUsingCustomerData = true)))))
        urlPost(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the check employment benefits page when reimbursed costs vouchers and non cash question is set to false" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))))
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
