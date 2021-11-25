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

package models

import models.employment.{AllEmploymentData, EmploymentData, EmploymentSource}
import play.api.libs.json.{Json, OFormat}

case class IncomeTaxUserData(employment: Option[AllEmploymentData] = None)

object IncomeTaxUserData {

  implicit val formats: OFormat[IncomeTaxUserData] = Json.format[IncomeTaxUserData]

  def excludePensionIncome(incomeTaxUserData: IncomeTaxUserData): IncomeTaxUserData = {
    incomeTaxUserData.copy(employment = incomeTaxUserData.employment.map(
      allEmploymentData => allEmploymentData.copy(hmrcEmploymentData = excludePensionIncome(allEmploymentData.hmrcEmploymentData))
    ))
  }

  private def excludePensionIncome(employmentData: Seq[EmploymentSource]): Seq[EmploymentSource] = {
    employmentData.map(employmentSource => employmentSource.copy(employmentData = employmentSource.employmentData.map(excludePensionIncome)))
  }

  private def excludePensionIncome(employmentData: EmploymentData): EmploymentData = {
    val pay = employmentData.occPen match {
      case Some(true) => None
      case _ => employmentData.pay
    }

    employmentData.copy(pay = pay)
  }
}
