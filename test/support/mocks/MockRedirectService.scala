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

package support.mocks

import models.mongo.EmploymentCYAModel
import models.redirects.ConditionalRedirect
import org.scalamock.handlers.{CallHandler3, CallHandler4}
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{Call, Result}
import services.RedirectService

trait MockRedirectService extends MockFactory {

  protected val mockRedirectService: RedirectService = mock[RedirectService]

  def mockCommonAccommodationBenefitsRedirects(employmentCYAModel: EmploymentCYAModel,
                                               taxYear: Int,
                                               employmentId: String,
                                               result: Seq[ConditionalRedirect]): CallHandler3[EmploymentCYAModel, Int, String, Seq[ConditionalRedirect]] = {
    (mockRedirectService.commonAccommodationBenefitsRedirects(_: EmploymentCYAModel, _: Int, _: String))
      .expects(employmentCYAModel, taxYear, employmentId)
      .returns(result)
  }

  def mockAccommodationRelocationBenefitsRedirects(employmentCYAModel: EmploymentCYAModel,
                                                   taxYear: Int,
                                                   employmentId: String,
                                                   result: Seq[ConditionalRedirect]): CallHandler3[EmploymentCYAModel, Int, String, Seq[ConditionalRedirect]] = {
    (mockRedirectService.accommodationRelocationBenefitsRedirects(_: EmploymentCYAModel, _: Int, _: String))
      .expects(employmentCYAModel, taxYear, employmentId)
      .returns(result)
  }

  def mockAccommodationBenefitsAmountRedirects(employmentCYAModel: EmploymentCYAModel,
                                               taxYear: Int,
                                               employmentId: String,
                                               result: Seq[ConditionalRedirect]): CallHandler3[EmploymentCYAModel, Int, String, Seq[ConditionalRedirect]] = {
    (mockRedirectService.accommodationBenefitsAmountRedirects(_: EmploymentCYAModel, _: Int, _: String))
      .expects(employmentCYAModel, taxYear, employmentId)
      .returns(result)
  }

  def mockQualifyingRelocationBenefitsAmountRedirects(employmentCYAModel: EmploymentCYAModel,
                                                      taxYear: Int,
                                                      employmentId: String,
                                                      result: Seq[ConditionalRedirect]): CallHandler3[EmploymentCYAModel, Int, String, Seq[ConditionalRedirect]] = {
    (mockRedirectService.qualifyingRelocationBenefitsAmountRedirects(_: EmploymentCYAModel, _: Int, _: String))
      .expects(employmentCYAModel, taxYear, employmentId)
      .returns(result)
  }

  def mockQualifyingRelocationBenefitsRedirects(employmentCYAModel: EmploymentCYAModel,
                                                      taxYear: Int,
                                                      employmentId: String,
                                                      result: Seq[ConditionalRedirect]): CallHandler3[EmploymentCYAModel, Int, String, Seq[ConditionalRedirect]] = {
    (mockRedirectService.qualifyingRelocationBenefitsRedirects(_: EmploymentCYAModel, _: Int, _: String))
      .expects(employmentCYAModel, taxYear, employmentId)
      .returns(result)
  }

  def mockCommonBenefitsRedirects(employmentCYAModel: EmploymentCYAModel,
                                  taxYear: Int,
                                  employmentId: String,
                                  result: Seq[ConditionalRedirect]): CallHandler3[EmploymentCYAModel, Int, String, Seq[ConditionalRedirect]] = {
    (mockRedirectService.commonBenefitsRedirects(_: EmploymentCYAModel, _: Int, _: String))
      .expects(employmentCYAModel, taxYear, employmentId)
      .returns(result)
  }

  def mockBenefitsSubmitRedirect(employmentCYAModel: EmploymentCYAModel,
                                 nextPage: Call,
                                 taxYear: Int,
                                 employmentId: String,
                                 result: Result): CallHandler4[EmploymentCYAModel, Call, Int, String, Result] = {
    (mockRedirectService.benefitsSubmitRedirect(_: EmploymentCYAModel, _: Call)(_: Int, _: String))
      .expects(employmentCYAModel, nextPage, taxYear, employmentId)
      .returns(result)
  }
}
