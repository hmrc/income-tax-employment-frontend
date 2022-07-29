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

package services

import controllers.benefits.travel.routes.TravelOrEntertainmentBenefitsController
import play.api.mvc.Result
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.mocks.MockBenefitsRedirectService
import support.{TaxYearHelper, UnitTest}

class RedirectsServiceSpec extends UnitTest
  with MockBenefitsRedirectService
  with TaxYearHelper {

  private val employmentId = "employmentId"

  private val underTest = new RedirectsService(mockBenefitsRedirectService)

  ".benefitsSubmitRedirect" should {
    "delegate to benefitsRedirectService and return the result" in {
      val nextPage = TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId)
      val result = mock[Result]

      mockBenefitsSubmitRedirect(taxYear, employmentId, anEmploymentCYAModel, nextPage, result)

      underTest.benefitsSubmitRedirect(taxYear, employmentId, anEmploymentCYAModel, nextPage) shouldBe result
    }
  }
}
