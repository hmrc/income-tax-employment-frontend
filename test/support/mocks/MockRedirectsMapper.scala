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
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import utils.RedirectsMapper

trait MockRedirectsMapper extends MockFactory {

  protected val mockRedirectsMapper: RedirectsMapper = mock[RedirectsMapper]

  def mockMatchToRedirects(clazz: Class[_],
                           taxYear: Int,
                           employmentId: String,
                           employmentCYAModel: EmploymentCYAModel,
                           result: Seq[ConditionalRedirect]): CallHandler4[Class[_], Int, String, EmploymentCYAModel, Seq[ConditionalRedirect]] = {
    (mockRedirectsMapper.mapToRedirects(_: Class[_], _: Int, _: String, _: EmploymentCYAModel))
      .expects(clazz, taxYear, employmentId, employmentCYAModel)
      .returns(result)
  }
}
