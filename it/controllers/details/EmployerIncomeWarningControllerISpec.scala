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

package controllers.details

import models.mongo.EmploymentUserData
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithDetails}
import utils.PageUrls.{employerIncomeWarningUrl, fullUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerIncomeWarningControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {


  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  private val employmentId: String = anEmploymentUserData.employmentId

  private def cya(isPriorSubmission: Boolean = true): EmploymentUserData =
    anEmploymentUserDataWithDetails(
      anEmploymentDetails.copy("HMRC"),
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = isPriorSubmission
    )

  ".show" when {
    "render the correct view for an individual" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(cya())
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employerIncomeWarningUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "has OK status" in {
        result.status shouldBe OK
      }

      "render the correct view for an agent" which {
        implicit lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(cya())
          authoriseAgentOrIndividual(isAgent = true)
          urlGet(fullUrl(employerIncomeWarningUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }
        "has OK status" in {
          result.status shouldBe OK
        }
      }
    }

  }

}