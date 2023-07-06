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

import play.api.libs.json._


sealed abstract class ShareOptionSchemePlanType

object ShareOptionSchemePlanType {

  case object EMI extends ShareOptionSchemePlanType
  case object CSOP extends ShareOptionSchemePlanType
  case object SAYE extends ShareOptionSchemePlanType
  case object Other extends ShareOptionSchemePlanType

  implicit val format: Format[ShareOptionSchemePlanType] = new Format[ShareOptionSchemePlanType] {
    def writes(schemePlanType: ShareOptionSchemePlanType): JsValue = schemePlanType match {
      case EMI => JsString("EMI")
      case CSOP => JsString("CSOP")
      case SAYE => JsString("SAYE")
      case Other => JsString("Other")
    }

    def reads(json: JsValue): JsResult[ShareOptionSchemePlanType] = json match {
      case JsString("EMI") => JsSuccess(EMI)
      case JsString("CSOP") => JsSuccess(CSOP)
      case JsString("SAYE") => JsSuccess(SAYE)
      case JsString("Other") => JsSuccess(Other)
      case other => JsError(s"Invalid ShareOptionSchemePlanType: $other")
    }
  }
}
