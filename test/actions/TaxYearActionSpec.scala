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

package actions

import common.SessionValues
import config.AppConfig
import models.{AuthorisationRequest, User}
import play.api.http.Status.SEE_OTHER
import play.api.test.FakeRequest
import support.ServiceUnitTest
import uk.gov.hmrc.auth.core.AffinityGroup

class TaxYearActionSpec extends ServiceUnitTest  {

  implicit lazy val mockedConfig: AppConfig = mock[AppConfig]

  def taxYearAction(taxYear: Int, reset: Boolean = true): TaxYearAction = new TaxYearAction(taxYear, reset)

  "TaxYearAction.refine" should {
    "return a Right(request)" when {
      "the tax year is within the list of valid tax years, and matches that in session if the feature switch is on" in {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning true

          await(taxYearAction(taxYear).refine(userRequest))
        }

        result.isRight shouldBe true
      }

      "the tax year is within the list of valid tax years, and matches that in session if the feature switch is off" in {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString,
            SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false

          await(taxYearAction(taxYearEOY).refine(userRequest))
        }

        result.isRight shouldBe true
      }

      "the tax year is different to the session value if the reset variable input is false" in {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false

          await(taxYearAction(taxYearEOY, reset = false).refine(userRequest))
        }

        result.isRight shouldBe true
      }

    }

    "return a Left(result)" when {

      "the VALID_TAX_YEARS session value is not present" which {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          FakeRequest().withHeaders("X-Session-ID" -> sessionId)
        )

        lazy val result = {
          mockedConfig.incomeTaxSubmissionStartUrl _ expects (taxYear) returning
            "controllers.routes.StartPageController.show(taxYear).url"

          taxYearAction(taxYear).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the start page redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe "controllers.routes.StartPageController.show(taxYear).url"
        }

      }

      "the tax year is outside of validTaxYearList while the feature switch is on" which {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning true

          taxYearAction(invalidTaxYear).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the TaxYearError redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe controllers.errors.routes.TaxYearErrorController.show.url
        }
      }

      "the tax year is within the validTaxYearList but the missing tax year reset is true" which {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning true
          mockedConfig.incomeTaxSubmissionOverviewUrl _ expects (taxYearEOY) returning
            "controllers.routes.OverviewPageController.show(taxYearEOY).url"

          taxYearAction(taxYearEOY).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the Overview page redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe "controllers.routes.OverviewPageController.show(taxYearEOY).url"
        }

        "has the updated TAX_YEAR session value" in {
          await(result.map(_.left.get)).session.get(SessionValues.TAX_YEAR).get shouldBe (taxYearEOY).toString
        }
      }
    }
  }
}