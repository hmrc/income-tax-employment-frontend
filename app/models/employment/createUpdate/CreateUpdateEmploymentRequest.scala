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

package models.employment.createUpdate

import audit._
import models.User
import models.benefits._
import models.employment._
import models.studentLoans.{DecodedAmendStudentLoansPayload, DecodedCreateNewStudentLoansPayload}
import play.api.libs.json.{Json, OFormat}

case class CreateUpdateEmploymentRequest(employmentId: Option[String] = None,
                                         employment: Option[CreateUpdateEmployment] = None,
                                         employmentData: Option[CreateUpdateEmploymentData] = None,
                                         hmrcEmploymentIdToIgnore: Option[String] = None,
                                         isHmrcEmploymentId: Option[Boolean] = None) {

  def toCreateAuditModel(user: User, taxYear: Int, existingEmployments: Seq[PriorEmploymentAuditInfo]): CreateNewEmploymentDetailsAudit = {
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

  def toAmendAuditModel(user: User, employmentId: String, taxYear: Int, priorData: EmploymentSource): AmendEmploymentDetailsUpdateAudit = {
    def currentOrPrior[T](data: Option[T], priorData: Option[T]): Option[T] = {
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
        taxablePayToDate = currentOrPrior(employmentData.map(_.pay.taxablePayToDate), priorData.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))),
        totalTaxToDate = currentOrPrior(employmentData.map(_.pay.totalTaxToDate), priorData.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))),
        payrollId = currentOrPrior(employment.flatMap(_.payrollId), priorData.payrollId)
      )
    )
  }

  def toCreateDecodedPayloadModel(existingEmployments: Seq[DecodedPriorEmploymentInfo]): DecodedCreateNewEmploymentDetailsPayload = {
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

  def toAmendDecodedPayloadModel(employmentId: String, priorData: EmploymentSource): DecodedAmendEmploymentDetailsPayload = {
    def currentOrPrior[T](data: Option[T], priorData: Option[T]): Option[T] = {
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
        taxablePayToDate = currentOrPrior(employmentData.map(_.pay.taxablePayToDate), priorData.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))),
        totalTaxToDate = currentOrPrior(employmentData.map(_.pay.totalTaxToDate), priorData.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))),
        payrollId = currentOrPrior(employment.flatMap(_.payrollId), priorData.payrollId)
      )
    )
  }

  def toCreateDecodedBenefitsPayloadModel(): DecodedCreateNewBenefitsPayload = {
    DecodedCreateNewBenefitsPayload(
      employerName = employment.map(_.employerName),
      employerRef = employment.flatMap(_.employerRef),
      employmentBenefitsData = Benefits(
        accommodation = employmentData.flatMap(_.benefitsInKind.flatMap(_.accommodation)),
        assets = employmentData.flatMap(_.benefitsInKind.flatMap(_.assets)),
        assetTransfer = employmentData.flatMap(_.benefitsInKind.flatMap(_.assetTransfer)),
        beneficialLoan = employmentData.flatMap(_.benefitsInKind.flatMap(_.beneficialLoan)),
        car = employmentData.flatMap(_.benefitsInKind.flatMap(_.car)),
        carFuel = employmentData.flatMap(_.benefitsInKind.flatMap(_.carFuel)),
        educationalServices = employmentData.flatMap(_.benefitsInKind.flatMap(_.educationalServices)),
        entertaining = employmentData.flatMap(_.benefitsInKind.flatMap(_.entertaining)),
        expenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.expenses)),
        medicalInsurance = employmentData.flatMap(_.benefitsInKind.flatMap(_.medicalInsurance)),
        telephone = employmentData.flatMap(_.benefitsInKind.flatMap(_.telephone)),
        service = employmentData.flatMap(_.benefitsInKind.flatMap(_.service)),
        taxableExpenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.taxableExpenses)),
        van = employmentData.flatMap(_.benefitsInKind.flatMap(_.van)),
        mileage = employmentData.flatMap(_.benefitsInKind.flatMap(_.mileage)),
        vanFuel = employmentData.flatMap(_.benefitsInKind.flatMap(_.vanFuel)),
        nonQualifyingRelocationExpenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.nonQualifyingRelocationExpenses)),
        nurseryPlaces = employmentData.flatMap(_.benefitsInKind.flatMap(_.nurseryPlaces)),
        otherItems = employmentData.flatMap(_.benefitsInKind.flatMap(_.otherItems)),
        paymentsOnEmployeesBehalf = employmentData.flatMap(_.benefitsInKind.flatMap(_.paymentsOnEmployeesBehalf)),
        personalIncidentalExpenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.personalIncidentalExpenses)),
        qualifyingRelocationExpenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.qualifyingRelocationExpenses)),
        employerProvidedProfessionalSubscriptions = employmentData.flatMap(_.benefitsInKind.flatMap(_.employerProvidedProfessionalSubscriptions)),
        employerProvidedServices = employmentData.flatMap(_.benefitsInKind.flatMap(_.employerProvidedServices)),
        incomeTaxPaidByDirector = employmentData.flatMap(_.benefitsInKind.flatMap(_.incomeTaxPaidByDirector)),
        travelAndSubsistence = employmentData.flatMap(_.benefitsInKind.flatMap(_.travelAndSubsistence)),
        vouchersAndCreditCards = employmentData.flatMap(_.benefitsInKind.flatMap(_.vouchersAndCreditCards)),
        nonCash = employmentData.flatMap(_.benefitsInKind.flatMap(_.nonCash))
      )
    )
  }

  def toAmendDecodedBenefitsPayloadModel(priorData: EmploymentSource): DecodedAmendBenefitsPayload = {
    DecodedAmendBenefitsPayload(
      priorEmploymentBenefitsData = Benefits(
        accommodation = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.accommodation)),
        assets = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.assets)),
        assetTransfer = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.assetTransfer)),
        beneficialLoan = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.beneficialLoan)),
        car = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.car)),
        carFuel = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.carFuel)),
        educationalServices = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.educationalServices)),
        entertaining = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.entertaining)),
        expenses = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.expenses)),
        medicalInsurance = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.medicalInsurance)),
        telephone = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.telephone)),
        service = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.service)),
        taxableExpenses = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.taxableExpenses)),
        van = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.van)),
        vanFuel = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.vanFuel)),
        mileage = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.mileage)),
        nonQualifyingRelocationExpenses = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.nonQualifyingRelocationExpenses)),
        nurseryPlaces = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.nurseryPlaces)),
        otherItems = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.otherItems)),
        paymentsOnEmployeesBehalf = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.paymentsOnEmployeesBehalf)),
        personalIncidentalExpenses = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.personalIncidentalExpenses)),
        qualifyingRelocationExpenses = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.qualifyingRelocationExpenses)),
        employerProvidedProfessionalSubscriptions = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.employerProvidedProfessionalSubscriptions)),
        employerProvidedServices = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.employerProvidedServices)),
        incomeTaxPaidByDirector = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.incomeTaxPaidByDirector)),
        travelAndSubsistence = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.travelAndSubsistence)),
        vouchersAndCreditCards = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.vouchersAndCreditCards)),
        nonCash = priorData.employmentBenefits.flatMap(_.benefits.flatMap(_.nonCash))
      ),
      employmentBenefitsData = Benefits(
        accommodation = employmentData.flatMap(_.benefitsInKind.flatMap(_.accommodation)),
        assets = employmentData.flatMap(_.benefitsInKind.flatMap(_.assets)),
        assetTransfer = employmentData.flatMap(_.benefitsInKind.flatMap(_.assetTransfer)),
        beneficialLoan = employmentData.flatMap(_.benefitsInKind.flatMap(_.beneficialLoan)),
        car = employmentData.flatMap(_.benefitsInKind.flatMap(_.car)),
        carFuel = employmentData.flatMap(_.benefitsInKind.flatMap(_.carFuel)),
        educationalServices = employmentData.flatMap(_.benefitsInKind.flatMap(_.educationalServices)),
        entertaining = employmentData.flatMap(_.benefitsInKind.flatMap(_.entertaining)),
        expenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.entertaining)),
        medicalInsurance = employmentData.flatMap(_.benefitsInKind.flatMap(_.medicalInsurance)),
        telephone = employmentData.flatMap(_.benefitsInKind.flatMap(_.telephone)),
        service = employmentData.flatMap(_.benefitsInKind.flatMap(_.service)),
        taxableExpenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.taxableExpenses)),
        van = employmentData.flatMap(_.benefitsInKind.flatMap(_.van)),
        vanFuel = employmentData.flatMap(_.benefitsInKind.flatMap(_.vanFuel)),
        mileage = employmentData.flatMap(_.benefitsInKind.flatMap(_.mileage)),
        nonQualifyingRelocationExpenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.nonQualifyingRelocationExpenses)),
        nurseryPlaces = employmentData.flatMap(_.benefitsInKind.flatMap(_.nurseryPlaces)),
        otherItems = employmentData.flatMap(_.benefitsInKind.flatMap(_.otherItems)),
        paymentsOnEmployeesBehalf = employmentData.flatMap(_.benefitsInKind.flatMap(_.paymentsOnEmployeesBehalf)),
        personalIncidentalExpenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.personalIncidentalExpenses)),
        qualifyingRelocationExpenses = employmentData.flatMap(_.benefitsInKind.flatMap(_.qualifyingRelocationExpenses)),
        employerProvidedProfessionalSubscriptions = employmentData.flatMap(_.benefitsInKind.flatMap(_.qualifyingRelocationExpenses)),
        employerProvidedServices = employmentData.flatMap(_.benefitsInKind.flatMap(_.employerProvidedServices)),
        incomeTaxPaidByDirector = employmentData.flatMap(_.benefitsInKind.flatMap(_.incomeTaxPaidByDirector)),
        travelAndSubsistence = employmentData.flatMap(_.benefitsInKind.flatMap(_.travelAndSubsistence)),
        vouchersAndCreditCards = employmentData.flatMap(_.benefitsInKind.flatMap(_.vouchersAndCreditCards)),
        nonCash = employmentData.flatMap(_.benefitsInKind.flatMap(_.nonCash))
      )
    )
  }

  def toCreateDecodedStudentLoansPayloadModel: DecodedCreateNewStudentLoansPayload = {
    DecodedCreateNewStudentLoansPayload(
      employerName = employment.map(_.employerName),
      employerRef = employment.flatMap(_.employerRef),
      Deductions(
        studentLoans = Some(StudentLoans(
          uglDeductionAmount = employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.uglDeductionAmount))),
          pglDeductionAmount = employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.pglDeductionAmount)))
        )
        )))
  }

  def toAmendDecodedStudentLoansPayloadModel(priorData: EmploymentSource): DecodedAmendStudentLoansPayload = {
    DecodedAmendStudentLoansPayload(Deductions(
      studentLoans = Some(StudentLoans(
        uglDeductionAmount = priorData.employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.uglDeductionAmount))),
        pglDeductionAmount = priorData.employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.pglDeductionAmount)))
      )
      )),
      Deductions(
      studentLoans = Some(StudentLoans(
        uglDeductionAmount = employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.uglDeductionAmount))),
        pglDeductionAmount = employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.pglDeductionAmount)))
      )
      )))
  }

  def toCreateStudentLoansAuditModel(user: User, taxYear: Int): CreateNewStudentLoansDeductionsAudit = {
    CreateNewStudentLoansDeductionsAudit(
      taxYear = taxYear,
      userType = user.affinityGroup.toLowerCase,
      nino = user.nino,
      mtditid = user.mtditid,
      deductions = Deductions(
        studentLoans = Some(StudentLoans(
          uglDeductionAmount = employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.uglDeductionAmount))),
          pglDeductionAmount = employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.pglDeductionAmount)))
        )
      ))
    )
  }

  def toAmendStudentLoansAuditModel(user: User, taxYear: Int, priorData: EmploymentSource): AmendStudentLoansDeductionsUpdateAudit = {
    AmendStudentLoansDeductionsUpdateAudit(
      taxYear = taxYear,
      userType = user.affinityGroup.toLowerCase,
      nino = user.nino,
      mtditid = user.mtditid,
      priorStudentLoanDeductionsData = Deductions(
        studentLoans = Some(StudentLoans(
          uglDeductionAmount = priorData.employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.uglDeductionAmount))),
          pglDeductionAmount = priorData.employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.pglDeductionAmount)))
        ))
      ),
      studentLoanDeductionsData = Deductions(
        studentLoans = Some(StudentLoans(
          uglDeductionAmount = employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.uglDeductionAmount))),
          pglDeductionAmount = employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.pglDeductionAmount)))
        ))
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
