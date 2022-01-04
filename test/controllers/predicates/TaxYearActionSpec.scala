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

package controllers.predicates

import common.SessionValues
import config.AppConfig
import models.User
import play.api.http.Status.SEE_OTHER
import play.api.i18n.MessagesApi
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.UnitTest

class TaxYearActionSpec extends UnitTest {
  val validTaxYear: Int = 2022
  val invalidTaxYear: Int = 3000

  implicit lazy val mockedConfig: AppConfig = mock[AppConfig]
  implicit lazy val cc: MessagesApi = mockControllerComponents.messagesApi

  def taxYearAction(taxYear: Int, reset: Boolean = true): TaxYearAction = new TaxYearAction(taxYear, reset)

  "TaxYearAction.refine" should {

    "return a Right(request)" when {

      "the tax year is within range of allowed years, and matches that in session if the feature switch is on" in {
        lazy val userRequest = User("1234567890", None, "AA123456A",sessionId, AffinityGroup.Individual.toString)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> validTaxYear.toString)
        )

        lazy val result = {
          mockedConfig.defaultTaxYear _ expects() returning validTaxYear
          mockedConfig.taxYearErrorFeature _ expects() returning true

          await(taxYearAction(validTaxYear).refine(userRequest))
        }

        result.isRight shouldBe true
      }

      "the tax year is equal to the session value if the feature switch is off" in {
        lazy val userRequest = User("1234567890", None, "AA123456A",sessionId, AffinityGroup.Individual.toString)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> (validTaxYear + 1).toString)
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false

          await(taxYearAction(validTaxYear + 1).refine(userRequest))
        }

        result.isRight shouldBe true
      }

      "the tax year is different to the session value if the reset variable input is false" in {
        lazy val userRequest = User("1234567890", None, "AA123456A",sessionId, AffinityGroup.Individual.toString)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> (validTaxYear).toString)
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false

          await(taxYearAction(validTaxYear + 1, reset = false).refine(userRequest))
        }

        result.isRight shouldBe true
      }

    }

    "return a Left(result)" when {

      "the tax year is different from that in session and the feature switch is off" which {
        lazy val userRequest = User("1234567890", None, "AA123456A",sessionId, AffinityGroup.Individual.toString)(
          fakeRequest.withSession(SessionValues.TAX_YEAR ->validTaxYear.toString)
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning false
          mockedConfig.incomeTaxSubmissionOverviewUrl _ expects (validTaxYear + 1) returning
            "controllers.routes.StartPageController.show(validTaxYear + 1).url"

          taxYearAction(validTaxYear + 1).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the start page redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe "controllers.routes.StartPageController.show(validTaxYear + 1).url"
        }

        "has an updated tax year session value" in {
          await(result.map(_.left.get)).session.get(SessionValues.TAX_YEAR).get shouldBe (validTaxYear + 1).toString
        }
      }

      "the tax year is outside of the allowed limit while the feature switch is on" which {
        lazy val userRequest = User("1234567890", None, "AA123456A",sessionId, AffinityGroup.Individual.toString)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> validTaxYear.toString)
        )

        lazy val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning true
          mockedConfig.defaultTaxYear _ expects() returning invalidTaxYear twice()

          taxYearAction(validTaxYear).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.get)) shouldBe SEE_OTHER
        }

        "has the overview redirect url" in {
          redirectUrl(result.map(_.left.get)) shouldBe controllers.errors.routes.TaxYearErrorController.show.url
        }
      }
    }
  }

}
