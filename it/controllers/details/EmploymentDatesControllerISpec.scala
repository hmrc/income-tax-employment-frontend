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

import forms.details.EmploymentDatesForm
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{checkYourDetailsUrl, employmentDatesUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmploymentDatesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  private val employmentId: String = "employmentId"

  private val employmentDetailsWithCessationDate = anEmploymentUserData.copy(
    employment = anEmploymentCYAModel.copy(employmentDetails = anEmploymentDetails.copy(
      didYouLeaveQuestion = Some(true),
      cessationDate = Some(s"$taxYearEOY-12-12"),
      payrollId = Some("payrollId"))))

  ".show" should {
    "render the 'employment dates' page with the correct content" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentDetailsWithCessationDate)
        urlGet(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employmentDatesUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" should {
    s"return a BAD_REQUEST($BAD_REQUEST) status" when {
      "there is an invalid input" which {
        lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "",
          EmploymentDatesForm.startAmountMonth -> "01",
          EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
          EmploymentDatesForm.endAmountDay -> "06",
          EmploymentDatesForm.endAmountMonth -> "03",
          EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentDetailsWithCessationDate)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }
    }

    "redirect to the check details page when the form is fully filled out" which {
      lazy val form: Map[String, String] = Map(EmploymentDatesForm.startAmountDay -> "01",
        EmploymentDatesForm.startAmountMonth -> "01",
        EmploymentDatesForm.startAmountYear -> taxYearEOY.toString,
        EmploymentDatesForm.endAmountDay -> "06",
        EmploymentDatesForm.endAmountMonth -> "03",
        EmploymentDatesForm.endAmountYear -> taxYearEOY.toString)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentDetailsWithCessationDate)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employmentDatesUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(employmentDatesUrl(taxYear, employmentId)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }
}
