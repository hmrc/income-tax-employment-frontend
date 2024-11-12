/*
 * Copyright 2024 HM Revenue & Customs
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

package models.mongo

import play.api.libs.json._

sealed abstract class JourneyStatus(status: String) {
  override def toString: String = status
}

object JourneyStatus {
  val values: Seq[JourneyStatus] = Seq(NotStarted, InProgress, Completed)

  case object NotStarted extends JourneyStatus("notStarted")
  case object InProgress extends JourneyStatus("inProgress")
  case object Completed extends JourneyStatus("completed")

  implicit val format: Format[JourneyStatus] = new Format[JourneyStatus] {
    override def writes(js: JourneyStatus): JsValue = JsString(js.toString)

    override def reads(json: JsValue): JsResult[JourneyStatus] = json match {
      case JsString("inProgress") => JsSuccess(InProgress)
      case JsString("completed") => JsSuccess(Completed)
      case error => JsError(s"Unable to read JourneyStatus: $error")
    }
  }
}
