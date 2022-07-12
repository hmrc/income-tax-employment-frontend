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

package controllers.details

import models.mongo.EmploymentUserData
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithDetails
import utils.PageUrls.{checkYourDetailsUrl, fullUrl, howMuchPayUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerPayAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {
  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val employmentId = "employmentId"

  private def cya(payToDate: Option[BigDecimal] = Some(100), isPriorSubmission: Boolean = true): EmploymentUserData =
    anEmploymentUserDataWithDetails(
      anEmploymentDetails.copy("maggie", taxablePayToDate = payToDate),
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = isPriorSubmission
    )

  ".show" should {
    "should render How much did xxx pay you? page" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(cya())
        urlGet(fullUrl(howMuchPayUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect  to check employment details page when there is no cya data in session" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(howMuchPayUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect  to overview page if the user tries to hit this page with current taxYear" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        val inYearUrl = s"$appUrl/$taxYear/details/how-much-pay?employmentId=$employmentId"
        urlGet(inYearUrl, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }

  ".submit" should {
    "return a bad request when an empty form is submitted" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlPost(fullUrl(howMuchPayUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
      }
      "has an BAD_REQUEST status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to check employment details page when a valid form is submitted" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlPost(fullUrl(howMuchPayUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
