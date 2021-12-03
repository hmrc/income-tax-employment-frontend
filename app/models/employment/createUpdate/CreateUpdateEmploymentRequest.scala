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

package models.employment.createUpdate

import audit.{AmendEmploymentDetailsUpdateAudit, AuditEmploymentData, AuditNewEmploymentData, CreateNewEmploymentDetailsAudit, PriorEmploymentAuditInfo}
import models.User
import models.benefits.Benefits
import models.employment.{DecodedAmendEmploymentDetailsPayload, DecodedCreateNewEmploymentDetailsPayload, DecodedEmploymentData, DecodedNewEmploymentData, DecodedPriorEmploymentInfo, Deductions, EmploymentSource}
import play.api.libs.json.{Json, OFormat}

case class CreateUpdateEmploymentRequest(employmentId: Option[String] = None,
                                         employment: Option[CreateUpdateEmployment] = None,
                                         employmentData: Option[CreateUpdateEmploymentData] = None,
                                         hmrcEmploymentIdToIgnore: Option[String] = None){

  def toCreateAuditModel(taxYear: Int, existingEmployments: Seq[PriorEmploymentAuditInfo])(implicit user: User[_]): CreateNewEmploymentDetailsAudit = {

    CreateNewEmploymentDetailsAudit(
      taxYear = taxYear,
      userType = user.affinityGroup.toLowerCase,
      nino = user.nino,
      mtditid = user.mtditid,
      employmentData = AuditNewEmploymentData(
        employerName = employment.map(_.employerName),
        employerRef = employment.flatMap(_.employerRef),
        startDate = employment.map(_.startDate),
        cessationDate = employment.flatMap(_.cessationDate),
        taxablePayToDate = employmentData.map(_.pay.taxablePayToDate),
        totalTaxToDate = employmentData.map(_.pay.totalTaxToDate),
        payrollId = employment.flatMap(_.payrollId)
      ),
      existingEmployments = existingEmployments
    )
  }

  def toAmendAuditModel(employmentId: String, taxYear: Int, priorData: EmploymentSource)(implicit user: User[_]): AmendEmploymentDetailsUpdateAudit = {

    def currentOrPrior[T](data: Option[T], priorData: Option[T]): Option[T] ={
      (data, priorData) match {
        case (data@Some(_), _) => data
        case (_, priorData@Some(_)) => priorData
        case _ => None
      }
    }

    AmendEmploymentDetailsUpdateAudit(
      taxYear = taxYear,
      userType = user.affinityGroup.toLowerCase,
      nino = user.nino,
      mtditid = user.mtditid,
      priorEmploymentData = AuditEmploymentData(
        employerName = priorData.employerName,
        employerRef = priorData.employerRef,
        employmentId = priorData.employmentId,
        startDate = priorData.startDate,
        cessationDate = priorData.cessationDate,
        taxablePayToDate = priorData.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)),
        totalTaxToDate = priorData.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)),
        payrollId = priorData.payrollId
      ),
      employmentData = AuditEmploymentData(
        employerName = employment.map(_.employerName).getOrElse(priorData.employerName),
        employerRef = currentOrPrior(employment.flatMap(_.employerRef), priorData.employerRef),
        employmentId = employmentId,
        startDate = currentOrPrior(employment.map(_.startDate), priorData.startDate),
        cessationDate = currentOrPrior(employment.flatMap(_.cessationDate), priorData.cessationDate),
        taxablePayToDate =  currentOrPrior(employmentData.map(_.pay.taxablePayToDate), priorData.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))),
        totalTaxToDate = currentOrPrior(employmentData.map(_.pay.totalTaxToDate), priorData.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))),
        payrollId = currentOrPrior(employment.flatMap(_.payrollId), priorData.payrollId)
      )
    )
  }

  def toCreateDecodedPayloadModel(existingEmployments: Seq[DecodedPriorEmploymentInfo])(implicit user: User[_]): DecodedCreateNewEmploymentDetailsPayload = {

    DecodedCreateNewEmploymentDetailsPayload(
      employmentData = DecodedNewEmploymentData(
        employerName = employment.map(_.employerName),
        employerRef = employment.flatMap(_.employerRef),
        startDate = employment.map(_.startDate),
        cessationDate = employment.flatMap(_.cessationDate),
        taxablePayToDate = employmentData.map(_.pay.taxablePayToDate),
        totalTaxToDate = employmentData.map(_.pay.totalTaxToDate),
        payrollId = employment.flatMap(_.payrollId)
      ),
      existingEmployments = existingEmployments
    )
  }

  def toAmendDecodedPayloadModel(employmentId: String, priorData: EmploymentSource)(implicit user: User[_]): DecodedAmendEmploymentDetailsPayload = {

    def currentOrPrior[T](data: Option[T], priorData: Option[T]): Option[T] ={
      (data, priorData) match {
        case (data@Some(_), _) => data
        case (_, priorData@Some(_)) => priorData
        case _ => None
      }
    }

    DecodedAmendEmploymentDetailsPayload(
      priorEmploymentData = DecodedEmploymentData(
        employerName = priorData.employerName,
        employerRef = priorData.employerRef,
        employmentId = priorData.employmentId,
        startDate = priorData.startDate,
        cessationDate = priorData.cessationDate,
        taxablePayToDate = priorData.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)),
        totalTaxToDate = priorData.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)),
        payrollId = priorData.payrollId
      ),
      employmentData = DecodedEmploymentData(
        employerName = employment.map(_.employerName).getOrElse(priorData.employerName),
        employerRef = currentOrPrior(employment.flatMap(_.employerRef), priorData.employerRef),
        employmentId = employmentId,
        startDate = currentOrPrior(employment.map(_.startDate), priorData.startDate),
        cessationDate = currentOrPrior(employment.flatMap(_.cessationDate), priorData.cessationDate),
        taxablePayToDate =  currentOrPrior(employmentData.map(_.pay.taxablePayToDate), priorData.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))),
        totalTaxToDate = currentOrPrior(employmentData.map(_.pay.totalTaxToDate), priorData.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))),
        payrollId = currentOrPrior(employment.flatMap(_.payrollId), priorData.payrollId)
      )
    )
  }
}

object CreateUpdateEmploymentRequest {
  implicit val formats: OFormat[CreateUpdateEmploymentRequest] = Json.format[CreateUpdateEmploymentRequest]
}

case class CreateUpdateEmployment(employerRef: Option[String],
                                  employerName: String,
                                  startDate: String,
                                  cessationDate: Option[String] = None,
                                  payrollId: Option[String] = None)

object CreateUpdateEmployment {
  implicit val formats: OFormat[CreateUpdateEmployment] = Json.format[CreateUpdateEmployment]
}

case class CreateUpdateEmploymentData(pay: CreateUpdatePay,
                                      deductions: Option[Deductions] = None,
                                      benefitsInKind: Option[Benefits] = None)

object CreateUpdateEmploymentData {
  implicit val formats: OFormat[CreateUpdateEmploymentData] = Json.format[CreateUpdateEmploymentData]
}

case class CreateUpdatePay(taxablePayToDate: BigDecimal,
                           totalTaxToDate: BigDecimal)

object CreateUpdatePay {
  implicit val formats: OFormat[CreateUpdatePay] = Json.format[CreateUpdatePay]
}
