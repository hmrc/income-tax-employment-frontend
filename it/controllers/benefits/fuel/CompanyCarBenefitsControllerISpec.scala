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

import forms.YesNoForm
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithBenefits
import utils.PageUrls.{carBenefitsUrl, fullUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CompanyCarBenefitsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {
    "return a radio button page when not in year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(carBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }
  }

  ".submit" when {
    "return a radio button page when not in year and a bad form submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlPost(fullUrl(carBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
    }
  }
}
