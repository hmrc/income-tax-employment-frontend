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

package models.employment

import java.time.LocalDate

// TODO: Delete when combined start and end date page is removed
case class EmploymentDates(startDate: Option[DateFormData],
                           endDate: Option[DateFormData]) {

  def startDateToLocalDate: Option[LocalDate] =
    startDate.map(employmentDate => LocalDate.of(employmentDate.amountYear.toInt, employmentDate.amountMonth.toInt, employmentDate.amountDay.toInt))

  def endDateToLocalDate: Option[LocalDate] =
    endDate.map(employmentDate => LocalDate.of(employmentDate.amountYear.toInt, employmentDate.amountMonth.toInt, employmentDate.amountDay.toInt))
}

object EmploymentDates {

  def formApply(startDay: String,
                startMonth: String,
                startYear: String,
                endDay: String,
                endMonth: String,
                endYear: String): EmploymentDates = EmploymentDates(
    startDate = Some(DateFormData(startDay, startMonth, startYear)),
    endDate = Some(DateFormData(endDay, endMonth, endYear))
  )

  def formUnapply(form: EmploymentDates): Option[(String, String, String, String, String, String)] = Some(
    (
      form.startDate.map(_.amountDay).getOrElse(""),
      form.startDate.map(_.amountMonth).getOrElse(""),
      form.startDate.map(_.amountYear).getOrElse(""),
      form.endDate.map(_.amountDay).getOrElse(""),
      form.endDate.map(_.amountMonth).getOrElse(""),
      form.endDate.map(_.amountYear).getOrElse("")
    )
  )
}
