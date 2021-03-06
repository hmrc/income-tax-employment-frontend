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

package services.benefits

import models.mongo.EmploymentCYAModel
import play.api.mvc.{Call, Result}
import services.RedirectService

import javax.inject.{Inject, Singleton}

// TODO: Test should be probably copied and reviewed from RedirectServiceSpec.
// TODO: Copy and refactor implementation
@Singleton
class BenefitsRedirectService @Inject()() {

  def benefitsSubmitRedirect(taxYear: Int,
                             employmentId: String,
                             employmentCYAModel: EmploymentCYAModel,
                             nextPage: Call): Result = {
    RedirectService.benefitsSubmitRedirect(employmentCYAModel, nextPage)(taxYear, employmentId)
  }
}
