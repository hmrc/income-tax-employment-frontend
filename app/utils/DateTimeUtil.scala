/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.Logging

import java.time.ZonedDateTime

object DateTimeUtil extends Logging{
  def getSubmittedOnDateTime(submittedOn: Option[String]): Option[ZonedDateTime] =
  {
    try {
      submittedOn.map(ZonedDateTime.parse(_))
    } catch {
      case e: Exception =>
        logger.warn(s"Could not parse submittedOn timestamp. SubmittedOn: $submittedOn, Exception: ${e.getMessage}")
        None
    }
  }
}