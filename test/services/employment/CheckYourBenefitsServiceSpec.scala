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

package services.employment

import audit.{AmendEmploymentBenefitsUpdateAudit, CreateNewEmploymentBenefitsAudit}
import models.benefits.Benefits
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import support.builders.models.UserBuilder.aUser
import support.builders.models.benefits.BenefitsBuilder.aBenefits
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks.{MockAuditService, MockEmploymentSessionService}
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

import scala.concurrent.ExecutionContext

class CheckYourBenefitsServiceSpec extends UnitTest
  with TaxYearProvider
  with MockEmploymentSessionService
  with MockAuditService {

  private val amendBenefits: Benefits = Benefits(
    accommodation = Some(10),
    assets = Some(10),
    assetTransfer = Some(10),
    beneficialLoan = Some(10),
    car = Some(10),
    carFuel = Some(10),
    educationalServices = Some(10),
    entertaining = Some(10),
    expenses = Some(10),
    medicalInsurance = Some(10),
    telephone = Some(10),
    service = Some(10),
    taxableExpenses = Some(10),
    van = Some(10),
    vanFuel = Some(10),
    mileage = Some(10),
    nonQualifyingRelocationExpenses = Some(10),
    nurseryPlaces = Some(10),
    otherItems = Some(10),
    paymentsOnEmployeesBehalf = Some(10),
    personalIncidentalExpenses = Some(10),
    qualifyingRelocationExpenses = Some(10),
    employerProvidedProfessionalSubscriptions = Some(10),
    employerProvidedServices = Some(10),
    incomeTaxPaidByDirector = Some(10),
    travelAndSubsistence = Some(10),
    vouchersAndCreditCards = Some(10),
    nonCash = Some(10)
  )


  private val underTest = new CheckYourBenefitsService(mockAuditService)

  "performSubmitAudits" should {
    "send an audit from the request model when it's a create" in {
      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        Some(anEmploymentSource.employmentId),
        Some(
          CreateUpdateEmployment(
            Some(anEmploymentSource.employerRef.get),
            anEmploymentSource.employerName,
            anEmploymentSource.startDate.get
          )
        ),
        Some(
          CreateUpdateEmploymentData(
            pay = CreateUpdatePay(
              4354,
              564
            ),
            deductions = Some(
              Deductions(
                Some(StudentLoans(
                  Some(100),
                  Some(100)
                ))
              )
            ),
            Some(aBenefits)
          )
        ),
        Some("employmentId")
      )

      val createNewEmploymentsAudit = CreateNewEmploymentBenefitsAudit(taxYearEOY,
        aUser.affinityGroup.toLowerCase,
        aUser.nino,
        aUser.mtditid,
        anEmploymentSource.employerName,
        Some(anEmploymentSource.employerRef.get),
        aBenefits)

      mockAuditSendEvent(createNewEmploymentsAudit.toAuditModel)

      val customerEmploymentData = anEmploymentSource.copy(employmentBenefits = None)
      val employmentDataWithoutBenefits = anAllEmploymentData.copy(customerEmploymentData = Seq(customerEmploymentData))
      val expected: AuditResult = Success

      val result = underTest.performSubmitAudits(aUser, model, employmentId = anEmploymentSource.employmentId,
        taxYear = taxYearEOY, Some(employmentDataWithoutBenefits))(HeaderCarrier(), ExecutionContext.global).get

      await(result) shouldBe expected
    }

    "send an audit from the request model when it's an amend" in {
      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        Some(anEmploymentSource.employmentId),
        Some(
          CreateUpdateEmployment(
            Some(anEmploymentSource.employerRef.get),
            anEmploymentSource.employerName,
            anEmploymentSource.startDate.get
          )
        ),
        Some(
          CreateUpdateEmploymentData(
            pay = CreateUpdatePay(
              4354,
              564
            ),
            deductions = Some(
              Deductions(
                Some(StudentLoans(
                  Some(100),
                  Some(100)
                ))
              )
            ),
            Some(amendBenefits)
          )
        ),
        Some("employmentId")
      )

      val amendAudit = AmendEmploymentBenefitsUpdateAudit(
        taxYearEOY,
        aUser.affinityGroup.toLowerCase,
        aUser.nino,
        aUser.mtditid,
        aBenefits,
        amendBenefits)
      mockAuditSendEvent(amendAudit.toAuditModel)

      val expected: AuditResult = Success
      val result = underTest.performSubmitAudits(aUser, model, employmentId = anEmploymentSource.employmentId,
        taxYear = taxYearEOY, Some(anAllEmploymentData))(HeaderCarrier(), ExecutionContext.global).get

      await(result) shouldBe expected
    }

    "not send an audit when it cannot find prior data" in {
      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        Some("id"),
        Some(
          CreateUpdateEmployment(
            Some("employerRef"),
            "name",
            "2000-10-10"
          )
        ),
        Some(
          CreateUpdateEmploymentData(
            pay = CreateUpdatePay(
              4354,
              564
            ),
            deductions = Some(
              Deductions(
                Some(StudentLoans(
                  Some(100),
                  Some(100)
                ))
              )
            ),
            Some(aBenefits)
          )
        ),
        Some("001")
      )
      val result = underTest.performSubmitAudits(aUser, model, employmentId = "003", taxYear = taxYearEOY, Some(anAllEmploymentData))(HeaderCarrier(), ExecutionContext.global)
      result shouldBe None
    }
  }
}
