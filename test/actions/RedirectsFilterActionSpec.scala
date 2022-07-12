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

import controllers.benefits.fuel.routes.CarVanFuelBenefitsController
import models.redirects.ConditionalRedirect
import play.api.mvc.Results.Redirect
import support.UnitTest
import support.builders.models.UserSessionDataRequestBuilder.aUserSessionDataRequest
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.mocks.MockRedirectsMatcherUtils

import scala.concurrent.ExecutionContext

class RedirectsFilterActionSpec extends UnitTest
  with MockRedirectsMatcherUtils {

  private val anyTaxYear = 2020
  private val anyEmploymentId = "any-employment-id"

  private val executionContext = ExecutionContext.global

  ".executionContext" should {
    "return the given execution context" in {
      val underTest = RedirectsFilterAction(mockRedirectsMatcherUtils, controllerName = "some-controller-name", anyTaxYear, anyEmploymentId)(executionContext)

      underTest.executionContext shouldBe executionContext
    }
  }

  ".filter" should {
    "return a redirect when conditional redirect with hasPrior None and condition is true" in {
      val redirectCall = CarVanFuelBenefitsController.show(anyTaxYear, anyEmploymentId)
      val resultRedirects: Seq[ConditionalRedirect] = Seq(ConditionalRedirect(condition = true, redirect = redirectCall, hasPrior = None))

      mockMatchToRedirects(controllerName = "some-controller-name", anyTaxYear, anyEmploymentId, anEmploymentCYAModel, resultRedirects)

      val underTest = RedirectsFilterAction(mockRedirectsMatcherUtils, controllerName = "some-controller-name", anyTaxYear, anyEmploymentId)(executionContext)

      await(underTest.filter(aUserSessionDataRequest)) shouldBe Some(Redirect(redirectCall))
    }

    "return a redirect when conditional redirect with hasPrior" in {
      val redirectCall = CarVanFuelBenefitsController.show(anyTaxYear, anyEmploymentId)
      val resultRedirects: Seq[ConditionalRedirect] = Seq(ConditionalRedirect(condition = true, redirect = redirectCall, hasPrior = Some(true)))

      mockMatchToRedirects(controllerName = "some-controller-name", anyTaxYear, anyEmploymentId, anEmploymentCYAModel, resultRedirects)

      val underTest = RedirectsFilterAction(mockRedirectsMatcherUtils, controllerName = "some-controller-name", anyTaxYear, anyEmploymentId)(executionContext)

      await(underTest.filter(aUserSessionDataRequest)) shouldBe Some(Redirect(redirectCall))
    }

    "None when no conditional redirects" in {
      mockMatchToRedirects(controllerName = "some-controller-name", anyTaxYear, anyEmploymentId, anEmploymentCYAModel, Seq.empty)

      val underTest = RedirectsFilterAction(mockRedirectsMatcherUtils, controllerName = "some-controller-name", anyTaxYear, anyEmploymentId)(executionContext)

      await(underTest.filter(aUserSessionDataRequest)) shouldBe None
    }
  }
}
