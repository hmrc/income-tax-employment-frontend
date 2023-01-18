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

package controllers.benefits.income

import forms.YesNoForm
import models.benefits.{BenefitsViewModel, IncomeTaxAndCostsModel}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{checkYourBenefitsUrl, fullUrl, incomeTaxBenefitsUrl, incomeTaxOrIncurredCostsBenefitsUrl, overviewUrl, reimbursedCostsBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class IncomeTaxOrIncurredCostsBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "Render the Income Tax or Incurred Costs Benefits Question page with no pre-filled data" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "Redirect to the Check Employment Benefits page when there is no in-session data for the user" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "Redirect to the Check Employment Benefits page when the benefits received flag is false" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = true)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        urlGet(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "Redirect to the overview page if it is not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }
      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" should {
    "Render the Income Tax or Incurred Costs Benefits Question page with an error when no value is submitted" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "returns a bad request" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "Redirect to overview page if it is not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYear, employmentId)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "Update the Income Tax Or Costs Question to 'Yes' when user selects yes and there is previous cya data, redirects to income tax question" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(IncomeTaxAndCostsModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the income tax question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(incomeTaxBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update the income tax or incurred costs question to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel).flatMap(_.sectionQuestion) shouldBe Some(true)
      }
    }

    "Create a new IncomeTaxAndCostsModel and redirect to income tax question page when user selects 'yes' and no prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the income tax question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(incomeTaxBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "creates a new incomeTaxAndCostsModel with the sectionQuestion as true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel).get shouldBe IncomeTaxAndCostsModel(sectionQuestion = Some(true))
      }
    }

    "Update the Income Tax Or Costs Question to 'No' and remove income tax data when user selects no, redirects to cya if prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the Income tax or costs question to false and wipes the other values" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel).get shouldBe IncomeTaxAndCostsModel(sectionQuestion = Some(false))
      }
    }

    "Update the Income Tax Or Costs Question to 'No' and remove income tax data when user selects no, redirects to reimbursed section if no prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the reimbursed section page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reimbursedCostsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the Income tax or costs question to false and wipes the other values" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel).get shouldBe IncomeTaxAndCostsModel(sectionQuestion = Some(false))
      }
    }
  }
}
