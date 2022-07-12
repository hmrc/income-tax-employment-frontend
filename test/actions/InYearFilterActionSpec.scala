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

import play.api.mvc.Results.Redirect
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.mocks.MockAppConfig
import support.{TaxYearProvider, UnitTest}
import utils.InYearUtil

import scala.concurrent.ExecutionContext

class InYearFilterActionSpec extends UnitTest
  with TaxYearProvider {

  private val appConfig = new MockAppConfig().config()
  private val inYearUtil = new InYearUtil()(appConfig)
  private val executionContext = ExecutionContext.global

  ".executionContext" should {
    "return the given execution context" in {
      val underTest = InYearFilterAction(taxYear = taxYear, inYearUtil = inYearUtil, appConfig = appConfig)(executionContext)

      underTest.executionContext shouldBe executionContext
    }
  }

  ".filter" should {
    "return a redirect to Income Tax Submission Overview when taxYear is end of year" in {
      val underTest = InYearFilterAction(taxYear = taxYearEOY, inYearUtil = inYearUtil, appConfig = appConfig)(executionContext)

      await(underTest.filter(anAuthorisationRequest)) shouldBe Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }

    "return None when taxYear is in year" in {
      val underTest = InYearFilterAction(taxYear = taxYear, inYearUtil = inYearUtil, appConfig = appConfig)(executionContext)

      await(underTest.filter(anAuthorisationRequest)) shouldBe None
    }
  }
}
