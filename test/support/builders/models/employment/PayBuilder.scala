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

package support.builders.models.employment

import models.employment.Pay
import support.TaxYearUtils.taxYearEOY

object PayBuilder {

  val aPay: Pay = Pay(
    taxablePayToDate = Some(100),
    totalTaxToDate = Some(200),
    payFrequency = Some("CALENDAR MONTHLY"),
    paymentDate = Some(s"${taxYearEOY - 1}-04-23"),
    taxWeekNo = Some(1),
    taxMonthNo = Some(1)
  )
}
