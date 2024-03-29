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

package forms.employment

import common.SessionValues
import filters.InputFilters
import play.api.data.Forms.single
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms}

import javax.inject.Singleton

@Singleton
class SelectEmployerForm extends InputFilters {

  private val employer = "value"

  private val missingInputError = "employment.unignoreEmployment.error"

  def employerListForm(ignoredEmployments: Seq[String]): Form[String] = Form(
    single(employer -> Forms.of(formatter(missingInputError, ignoredEmployments)).transform[String](filter, identity))
  )

  private def formatter(missingInputError: String, ignoredEmployments: Seq[String]): Formatter[String] = new Formatter[String] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(employmentId) if ignoredEmployments.contains(employmentId) || employmentId == SessionValues.ADD_A_NEW_EMPLOYER => Right(employmentId)
        case _ => Left(Seq(FormError(key, missingInputError)))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = {
      Map(
        key -> value
      )
    }
  }
}
