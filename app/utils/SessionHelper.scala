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

import play.api.libs.json.{Json, Reads}
import play.api.mvc.Request

trait SessionHelper {

  def sessionIdIsUUID(id: String): Boolean = id.matches("^[A-Za-z0-9\\-\n]{36}$")

  def getFromSession(key: String)(implicit request: Request[_]): Option[String] = {
    request.session.get(key)
  }

  def getModelFromSession[T](key: String)(implicit request: Request[_], reads: Reads[T]): Option[T] = {
    getFromSession(key).flatMap(sessionData => Json.parse(sessionData).asOpt[T])
  }
}
