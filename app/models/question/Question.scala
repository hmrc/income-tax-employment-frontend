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

package models.question

import play.api.mvc.{Call, Results}

sealed trait Question {
  val expectedPage: Call
  def isValid: Boolean
}

object Question extends Results {

  final case class WithoutDependency(question: Option[Boolean], expectedPage: Call) extends Question {
    override def isValid: Boolean = true
  }

  final case class WithDependency(page: Option[Any], dependency: Option[Boolean], expectedPage: Call, redirectPage: Call) extends Question {
    override def isValid: Boolean = {
      (dependency, page) match {
        case (Some(true), _) => true
        case _ => false
      }
    }
  }
}
