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

package controllers.benefits.travel

import forms.YesNoForm
import models.benefits.TravelEntertainmentModel
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
import utils.PageUrls.{checkYourBenefitsUrl, entertainmentExpensesBenefitsAmountUrl, entertainmentExpensesBenefitsUrl, fullUrl, overviewUrl, travelOrEntertainmentBenefitsUrl, utilitiesOrGeneralServicesBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EntertainingBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employment = employmentCyaModel)

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the 'Did you get any entertainment benefits?' page" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(entertainingQuestion = None)))
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK($OK) status" in {
        result.status shouldBe OK
      }
    }

    "redirect user to check employment benefits page" when {
      "user has no benefits and it's a prior submission" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = None)))
          urlGet(fullUrl(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "user has no travel or entertainment benefits and it's a prior submission" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel(sectionQuestion = Some(false))))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
          urlGet(fullUrl(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }

    "redirect to the travel or entertainment page when travelEntertainmentQuestion is None and it's not a prior submission" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel(sectionQuestion = None)))
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(travelOrEntertainmentBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect user to tax overview page if it's not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(entertainmentExpensesBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" should {
    "return an error if no value is submitted" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlPost(fullUrl(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an BAD REQUEST($BAD_REQUEST) status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to entertaining amount page when user selects Yes and it's a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val model = aBenefitsViewModel
          .copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(entertainingQuestion = Some(false))))
          .copy(utilitiesAndServicesModel = None)
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(model))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(entertainmentExpensesBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates entertainingQuestion to Yes" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertainingQuestion)) shouldBe Some(true)
      }
    }

    "redirect to utilities or general services benefits page when user selects No and wipe entertaining amount" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = None)
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates entertainingQuestion to No and removes entertaining expenses amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertainingQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertaining)) shouldBe None
      }
    }

    "redirect to entertainment amount page if valid form is submitted and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel
          .copy(travelEntertainmentModel = Some(aTravelEntertainmentModel.copy(entertainingQuestion = Some(false))))
          .copy(utilitiesAndServicesModel = None)
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(entertainmentExpensesBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates entertainingQuestion to Yes" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertainingQuestion)) shouldBe Some(true)
      }
    }

    "redirect to check employment benefits page when there is no CYA data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlPost(fullUrl(entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to tax overview page if it's not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(entertainmentExpensesBenefitsUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }
}
