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

package models

import play.api.mvc.PathBindable
import enumeratum._

sealed abstract class Journey(name: String) extends EnumEntry {
  override def toString: String = name
}

object Journey extends Enum[Journey] with PlayJsonEnum[Journey] {

  implicit def pathBindable(implicit strBinder: PathBindable[String]): PathBindable[Journey] = new PathBindable[Journey] {

    override def bind(key: String, value: String): Either[String, Journey] =
      strBinder.bind(key, value).flatMap { stringValue =>
        Journey.withNameOption(stringValue) match {
          case Some(journeyName) => Right(journeyName)
          case None => Left(s"$stringValue Invalid journey name")
        }
      }

    override def unbind(key: String, journey: Journey): String =
      strBinder.unbind(key, journey.entryName)
  }

  val values: IndexedSeq[Journey] = findValues

  // Employment
  //TODO: Temporary name until journeys split out to non-linear
  case object Employment extends Journey("employment-summary")

}
