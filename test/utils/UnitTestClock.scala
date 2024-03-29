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

package utils

import java.time.{LocalDateTime, ZoneId}
import support.TaxYearUtils.taxYearEOY

object UnitTestClock extends Clock {
  private val year = taxYearEOY
  private val month = 1
  private val day = 1
  private val hour = 0
  private val minute = 0

  override def now(zone: ZoneId = ZoneId.of("UTC")): LocalDateTime = LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toLocalDateTime
}
