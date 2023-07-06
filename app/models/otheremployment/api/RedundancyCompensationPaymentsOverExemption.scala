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

package models.otheremployment.api

import play.api.libs.json.{Json, OFormat}

case class RedundancyCompensationPaymentsOverExemption(amount: BigDecimal,
                                                       taxPaid: Option[BigDecimal] = None,
                                                       taxTakenOffInEmployment: Option[Boolean] = None)

object RedundancyCompensationPaymentsOverExemption {
  implicit val format: OFormat[RedundancyCompensationPaymentsOverExemption] = Json.format[RedundancyCompensationPaymentsOverExemption]
}
