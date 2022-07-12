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

package utils

import models.mongo.EmploymentCYAModel
import models.redirects.ConditionalRedirect
import services.RedirectService.accommodationRelocationBenefitsRedirects

import java.security.InvalidParameterException
import javax.inject.Singleton

@Singleton
class RedirectsMatcherUtils() {

  // TODO: (Hristo) Test me
  def matchToRedirects(controllerName: String,
                       taxYear: Int,
                       employmentId: String,
                       employmentCYAModel: EmploymentCYAModel): Seq[ConditionalRedirect] = controllerName match {
    case "AccommodationRelocationBenefitsController" => accommodationRelocationBenefitsRedirects(employmentCYAModel, taxYear, employmentId)
    case _ => throw new InvalidParameterException(s"$controllerName could not be matched with redirects.")
  }
}
