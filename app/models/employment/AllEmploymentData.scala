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

package models.employment

import play.api.Logging
import play.api.libs.json.{Json, OFormat}

case class AllEmploymentData(hmrcEmploymentData: Seq[HmrcEmploymentSource],
                             hmrcExpenses: Option[EmploymentExpenses],
                             customerEmploymentData: Seq[EmploymentSource],
                             customerExpenses: Option[EmploymentExpenses]) extends Logging {

  def latestInYearEmployments: Seq[EmploymentSource] = {
    hmrcEmploymentData.map(_.toEmploymentSource).sorted(Ordering.by((_: EmploymentSource).submittedOn).reverse)
  }

  def latestEOYEmployments: Seq[EmploymentSource] = {
    val hmrcData = hmrcEmploymentData.filter(_.dateIgnored.isEmpty).map(_.toEmploymentSource)
    val customerData = customerEmploymentData

    (hmrcData ++ customerData).sorted(Ordering.by((_: EmploymentSource).submittedOn).reverse)
  }

  def isLastEOYEmployment: Boolean = latestEOYEmployments.length == 1

  def isLastInYearEmployment: Boolean = latestInYearEmployments.length == 1

  def latestInYearExpenses: Option[LatestExpensesOrigin] = hmrcExpenses.map(LatestExpensesOrigin(_, isCustomerData = false))

  def latestEOYExpenses: Option[LatestExpensesOrigin] = {
    val hmrcExp = hmrcExpenses.filter(_.dateIgnored.isEmpty)

    // TODO: This logging should be moved to wherever the object is read from, e.g. connector or repository
    if (hmrcExp.isDefined && customerExpenses.isDefined) {
      logger.warn("[AllEmploymentData][latestEOYExpenses] Hmrc expenses and customer expenses exist but hmrc expenses have not been ignored")
    }

    lazy val default = hmrcExpenses.map(LatestExpensesOrigin(_, isCustomerData = false))
    customerExpenses.fold(default)(customerExpenses => Some(LatestExpensesOrigin(customerExpenses, isCustomerData = true)))
  }

  def ignoredEmployments: Seq[EmploymentSource] ={
    hmrcEmploymentData.filter(_.dateIgnored.isDefined).map(_.toEmploymentSource)
  }

  def inYearEmploymentSourceWith(employmentId: String): Option[EmploymentSourceOrigin] = hmrcEmploymentData
    .find(source => source.employmentId.equals(employmentId))
    .map(hmrcSource => EmploymentSourceOrigin(hmrcSource.toEmploymentSource, isCustomerData = false))

  def eoyEmploymentSourceWith(employmentId: String): Option[EmploymentSourceOrigin] = {
    val hmrcRecord = hmrcEmploymentData.find(source => source.employmentId.equals(employmentId) && source.dateIgnored.isEmpty)
    val customerRecord = customerEmploymentData.find(source => source.employmentId.equals(employmentId))
    lazy val default = hmrcRecord.map(hmrcSource => EmploymentSourceOrigin(hmrcSource.toEmploymentSource, isCustomerData = false))
    customerRecord.fold(default)(customerRecord => Some(EmploymentSourceOrigin(customerRecord, isCustomerData = true)))
  }
}

object AllEmploymentData {
  implicit val format: OFormat[AllEmploymentData] = Json.format[AllEmploymentData]

  def employmentIdExists(allEmploymentData: AllEmploymentData, employmentId: Option[String]): Boolean = {
    employmentId.exists { employmentId =>
      val idExistsInHMRCData: Boolean = allEmploymentData.hmrcEmploymentData.exists(_.employmentId == employmentId)
      val idExistsInCustomerData: Boolean = allEmploymentData.customerEmploymentData.exists(_.employmentId == employmentId)

      idExistsInHMRCData || idExistsInCustomerData
    }
  }
}