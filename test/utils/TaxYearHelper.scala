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

import org.joda.time.DateTime

trait TaxYearHelper {

  private val month = DateTime.now().monthOfYear().get()
  private val dayOfMonth = DateTime.now().dayOfMonth().get()

  val taxYear: Int = if (month >= 4 && dayOfMonth >= 5) DateTime.now().year().get() + 1 else DateTime.now().year().get()
  val taxYearEOY: Int = taxYear - 1
}
