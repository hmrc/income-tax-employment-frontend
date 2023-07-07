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

package viewmodels.employment

import play.mvc.Call

case class EmployerInformationRow(
                                   labelMessageKey: LabelMessageKey,
                                   status: Status,
                                   maybeAction: Option[Call],
                                   updateAvailable: Boolean
                                 )

sealed trait Status

case object CannotUpdate extends Status {
  override def toString: String = "common.status.cannotUpdate"
}

case object NotStarted extends Status {
  override def toString: String = "common.status.notStarted"
}

case object Updated extends Status {
  override def toString: String = "common.status.updated"
}

case object ToDo extends Status {
  override def toString: String = "common.status.toDo"
}

sealed trait LabelMessageKey

case object EmploymentDetails extends LabelMessageKey {
  override def toString: String = "common.employmentDetails"
}

case object EmploymentBenefits extends LabelMessageKey {
  override def toString: String = "common.employmentBenefits"
}

case object StudentLoans extends LabelMessageKey {
  override def toString: String = "common.studentLoans"
}

case object TaxableLumpSums extends LabelMessageKey {
  override def toString: String = "common.taxableLumpSums"
}
