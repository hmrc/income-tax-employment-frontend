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

package actions

import common.SessionValues
import config.AppConfig
import models.{AuthorisationRequest, User}
import org.scalamock.scalatest.MockFactory
import play.api.http.Status.SEE_OTHER
import play.api.i18n.MessagesApi
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import support.{ControllerUnitTest, TaxYearProvider}
import uk.gov.hmrc.auth.core.AffinityGroup

class TaxYearActionSpec extends ControllerUnitTest with TaxYearProvider with MockFactory {

  private val sessionId: String = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"

  implicit lazy val mockedConfig: AppConfig = mock[AppConfig]
  implicit lazy val messagesApi: MessagesApi = cc.messagesApi

  val invalidTaxYear: Int = taxYear + 999

  def taxYearAction(taxYear: Int, reset: Boolean = true): TaxYearAction = new TaxYearAction(taxYear, reset)

  "TaxYearAction.refine" should {
    "return a Right(request)" when {
      "the tax year is within the list of valid tax years, and matches that in session if the feature switch is on" in {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        val result = {
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

        val result = {
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

        val result = {
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

        val result = {
          mockedConfig.incomeTaxSubmissionStartUrl _ expects (taxYear) returning
            "controllers.routes.StartPageController.show(taxYear).url"

          val request = taxYearAction(taxYear).refine(userRequest)
          await(request.map(_.left.get))
        }

        "has a status of SEE_OTHER (303)" in {
          result.header.status shouldBe SEE_OTHER
        }

        "has the start page redirect url" in {
          result.header.headers("Location") shouldBe "controllers.routes.StartPageController.show(taxYear).url"
        }

      }

      "the tax year is outside of validTaxYearList while the feature switch is on" which {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning true

          val request = taxYearAction(invalidTaxYear).refine(userRequest)
          await(request.map(_.left.get))
        }

        "has a status of SEE_OTHER (303)" in {
          result.header.status shouldBe SEE_OTHER
        }

        "has the TaxYearError redirect url" in {
          result.header.headers("Location") shouldBe controllers.errors.routes.TaxYearErrorController.show.url
        }
      }

      "the tax year is within the validTaxYearList but the missing tax year reset is true" which {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        val result = {
          mockedConfig.taxYearErrorFeature _ expects() returning true
          mockedConfig.incomeTaxSubmissionOverviewUrl _ expects (taxYearEOY) returning
            "controllers.routes.OverviewPageController.show(taxYearEOY).url"

          val request = taxYearAction(taxYearEOY).refine(userRequest)
          await(request.map(_.left.get))
        }

        "has a status of SEE_OTHER (303)" in {
          result.header.status shouldBe SEE_OTHER
        }

        "has the Overview page redirect url" in {
          result.header.headers("Location") shouldBe "controllers.routes.OverviewPageController.show(taxYearEOY).url"
        }

        "has the updated TAX_YEAR session value" in {
          implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
            new AuthorisationRequest[AnyContent](models.User("1234567890", None, "AA123456A", "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
              fakeRequest)
          result.session.get(SessionValues.TAX_YEAR).get shouldBe (taxYearEOY).toString
        }
      }
    }
  }
}