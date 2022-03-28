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

import common.EmploymentToRemove.{all, hmrcHeld, customer}
import models.benefits.BenefitsViewModel
import models.employment.createUpdate.CreateUpdateEmployment
import models.mongo.EmploymentDetails
import org.joda.time.DateTime
import play.api.Logging
import play.api.libs.json.{Json, OFormat}

case class HmrcEmploymentSource(employmentId: String,
                                employerName: String,
                                employerRef: Option[String],
                                payrollId: Option[String],
                                startDate: Option[String],
                                cessationDate: Option[String],
                                dateIgnored: Option[String],
                                submittedOn: Option[String],
                                hmrcEmploymentFinancialData: Option[EmploymentFinancialData],
                                customerEmploymentFinancialData: Option[EmploymentFinancialData]) extends Logging {

  def toRemove: String ={
    (hmrcEmploymentFinancialData, customerEmploymentFinancialData) match {
      case (_, Some(_)) => all
      case _ => hmrcHeld
    }
  }

  private def parseDate(submittedOn: String): Option[DateTime] = {
    try {
      Some(DateTime.parse(submittedOn))
    } catch {
      case e: Exception =>
        logger.error(s"[HmrcEmploymentSource][parseDate] Couldn't parse submitted on to DateTime - ${e.getMessage}")
        None
    }
  }

  def getLatestEmploymentFinancialData: Option[EmploymentFinancialData] = {

    (hmrcEmploymentFinancialData, customerEmploymentFinancialData) match {
      case (None, None) => None
      case (Some(hmrc), None) => Some(hmrc)
      case (None, Some(customer)) => Some(customer)
      case (Some(hmrc), Some(customer)) =>

        val hmrcSubmittedOn: Option[DateTime] = hmrc.employmentData.map(_.submittedOn).flatMap(parseDate)
        val customerSubmittedOn: Option[DateTime] = customer.employmentData.map(_.submittedOn).flatMap(parseDate)

        Some((hmrcSubmittedOn,customerSubmittedOn) match {
          case (Some(hmrcSubmittedOn), Some(customerSubmittedOn)) => if(hmrcSubmittedOn.isAfter(customerSubmittedOn)) hmrc else customer
          case (Some(_), None) => hmrc
          case (None, Some(_)) => customer
          case (None, None) => customer
        })
    }
  }

  def toEmploymentSource: EmploymentSource = {

    val latestData = getLatestEmploymentFinancialData

    EmploymentSource(
      employmentId = employmentId,
      employerName = employerName,
      employerRef = employerRef,
      payrollId = payrollId,
      startDate = startDate,
      cessationDate = cessationDate,
      dateIgnored = dateIgnored,
      submittedOn = submittedOn,
      employmentData = latestData.flatMap(_.employmentData),
      employmentBenefits = latestData.flatMap(_.employmentBenefits)
    )
  }
}

object HmrcEmploymentSource {
  implicit val format: OFormat[HmrcEmploymentSource] = Json.format[HmrcEmploymentSource]
}

case class EmploymentFinancialData(employmentData: Option[EmploymentData], employmentBenefits: Option[EmploymentBenefits])

object EmploymentFinancialData {
  implicit val format: OFormat[EmploymentFinancialData] = Json.format[EmploymentFinancialData]
}

case class EmploymentSource(employmentId: String,
                            employerName: String,
                            employerRef: Option[String],
                            payrollId: Option[String],
                            startDate: Option[String],
                            cessationDate: Option[String],
                            dateIgnored: Option[String],
                            submittedOn: Option[String],
                            employmentData: Option[EmploymentData],
                            employmentBenefits: Option[EmploymentBenefits]
                           ) extends Logging {

  def toEmployerView: Employer = Employer(employmentId, employerName, startDate, cessationDate)

  def hasPriorBenefits: Boolean =
    employmentBenefits.exists(_.benefits.exists(_.hasBenefitsPopulated))

  def hasPriorStudentLoans: Boolean =
    employmentData.exists(_.deductions.exists(_.studentLoans.exists(sl => sl.pglDeductionAmount.isDefined || sl.uglDeductionAmount.isDefined)))

  def dataHasNotChanged(createUpdateEmployment: CreateUpdateEmployment): Boolean = {
    employerRef == createUpdateEmployment.employerRef &&
      employerName == createUpdateEmployment.employerName &&
      startDate.contains(createUpdateEmployment.startDate) &&
      cessationDate == createUpdateEmployment.cessationDate &&
      payrollId == createUpdateEmployment.payrollId
  }

  def toBenefitsViewModel(isUsingCustomerData: Boolean): Option[BenefitsViewModel] = {
    val submittedOn: Option[String] = employmentBenefits.map(_.submittedOn)
    employmentBenefits.flatMap(_.benefits.map(_.toBenefitsViewModel(isUsingCustomerData, submittedOn)))
  }

  def toEmploymentDetails(isUsingCustomerData: Boolean): EmploymentDetails = {
    EmploymentDetails(
      employerName = employerName,
      employerRef = employerRef,
      startDate = startDate,
      payrollId = payrollId,
      cessationDateQuestion = Some(cessationDate.isEmpty),
      cessationDate = cessationDate,
      dateIgnored = dateIgnored,
      employmentSubmittedOn = submittedOn,
      employmentDetailsSubmittedOn = employmentData.map(_.submittedOn),
      taxablePayToDate = employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)),
      totalTaxToDate = employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)),
      currentDataIsHmrcHeld = !isUsingCustomerData
    )
  }

  def toStudentLoansCYAModel: Option[StudentLoansCYAModel] = {
    employmentData.flatMap(_.deductions.flatMap(_.studentLoans.map(data =>
      StudentLoansCYAModel(
        data.uglDeductionAmount.isDefined,
        data.uglDeductionAmount,
        data.pglDeductionAmount.isDefined,
        data.pglDeductionAmount)
    )))
  }


  def toEmploymentDetailsViewModel(isUsingCustomerData: Boolean): EmploymentDetailsViewModel = {
    EmploymentDetailsViewModel(
      employerName,
      employerRef,
      payrollId,
      employmentId,
      startDate,
      Some(cessationDate.isEmpty),
      cessationDate,
      employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)),
      employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)),
      isUsingCustomerData
    )
  }
}

object EmploymentSource {
  implicit val format: OFormat[EmploymentSource] = Json.format[EmploymentSource]
}

