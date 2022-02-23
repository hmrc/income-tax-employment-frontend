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

package services.employment

import audit.{AmendEmploymentBenefitsUpdateAudit, CreateNewEmploymentBenefitsAudit}
import models.benefits.{Benefits, DecodedAmendBenefitsPayload, DecodedCreateNewBenefitsPayload}
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import support.builders.models.benefits.BenefitsBuilder.aBenefits
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks.{MockAuditService, MockEmploymentSessionService, MockNrsService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import utils.UnitTest

class CheckYourBenefitsServiceSpec extends UnitTest with MockEmploymentSessionService with MockNrsService with MockAuditService {

  private val underTest = new CheckYourBenefitsService(mockNrsService, mockAuditService)

  "performSubmitNrsPayload" should {
    "send the event from the model when it's a create" in {
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
            Some(allBenefits)
          )
        ),
        Some("001")
      )

      verifySubmitEvent(DecodedCreateNewBenefitsPayload(Some("name"), Some("employerRef"),
        Benefits(accommodation = allBenefits.accommodation,
          assets = allBenefits.assets,
          assetTransfer = allBenefits.assetTransfer,
          beneficialLoan = allBenefits.beneficialLoan,
          car = allBenefits.car,
          carFuel = allBenefits.carFuel,
          educationalServices = allBenefits.educationalServices,
          entertaining = allBenefits.entertaining,
          expenses = allBenefits.expenses,
          medicalInsurance = allBenefits.medicalInsurance,
          telephone = allBenefits.telephone,
          service = allBenefits.service,
          taxableExpenses = allBenefits.taxableExpenses,
          van = allBenefits.van,
          vanFuel = allBenefits.vanFuel,
          mileage = allBenefits.mileage,
          nonQualifyingRelocationExpenses = allBenefits.nonQualifyingRelocationExpenses,
          nurseryPlaces = allBenefits.nurseryPlaces,
          otherItems = allBenefits.otherItems,
          paymentsOnEmployeesBehalf = allBenefits.paymentsOnEmployeesBehalf,
          personalIncidentalExpenses = allBenefits.personalIncidentalExpenses,
          qualifyingRelocationExpenses = allBenefits.qualifyingRelocationExpenses,
          employerProvidedProfessionalSubscriptions = allBenefits.employerProvidedProfessionalSubscriptions,
          employerProvidedServices = allBenefits.employerProvidedServices,
          incomeTaxPaidByDirector = allBenefits.incomeTaxPaidByDirector,
          travelAndSubsistence = allBenefits.travelAndSubsistence,
          vouchersAndCreditCards = allBenefits.vouchersAndCreditCards,
          nonCash = allBenefits.nonCash
        )))

      await(underTest.performSubmitNrsPayload(authorisationRequest.user, model, "001", prior = None)) shouldBe Right()

    }

    "send the event from the model when it's an amend" in {

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
            benefitsInKind = Some(amendBenefits)
          )
        ),
        Some("001")
      )

      val employmentSource1 = EmploymentSource(
        employmentId = "001",
        employerName = "Mishima Zaibatsu",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = None,
        submittedOn = Some("2020-01-04T05:01:01Z"),
        employmentData = Some(EmploymentData(
          submittedOn = "2020-02-12",
          employmentSequenceNumber = Some("123456789999"),
          companyDirector = Some(true),
          closeCompany = Some(false),
          directorshipCeasedDate = Some("2020-02-12"),
          occPen = Some(false),
          disguisedRemuneration = Some(false),
          pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
          Some(Deductions(
            studentLoans = Some(StudentLoans(
              uglDeductionAmount = Some(100.00),
              pglDeductionAmount = Some(100.00)
            ))
          ))
        )),
        employmentBenefits = Some(EmploymentBenefits("2020-02-15", Some(allBenefits)))
      )

      val priorData: AllEmploymentData = AllEmploymentData(
        hmrcEmploymentData = Seq(employmentSource1),
        hmrcExpenses = None,
        customerEmploymentData = Seq(),
        customerExpenses = None
      )

      verifySubmitEvent(DecodedAmendBenefitsPayload(
        Benefits(
          accommodation = allBenefits.accommodation,
          assets = allBenefits.assets,
          assetTransfer = allBenefits.assetTransfer,
          beneficialLoan = allBenefits.beneficialLoan,
          car = allBenefits.car,
          carFuel = allBenefits.carFuel,
          educationalServices = allBenefits.educationalServices,
          entertaining = allBenefits.entertaining,
          expenses = allBenefits.expenses,
          medicalInsurance = allBenefits.medicalInsurance,
          telephone = allBenefits.telephone,
          service = allBenefits.service,
          taxableExpenses = allBenefits.taxableExpenses,
          van = allBenefits.van,
          vanFuel = allBenefits.vanFuel,
          mileage = allBenefits.mileage,
          nonQualifyingRelocationExpenses = allBenefits.nonQualifyingRelocationExpenses,
          nurseryPlaces = allBenefits.nurseryPlaces,
          otherItems = allBenefits.otherItems,
          paymentsOnEmployeesBehalf = allBenefits.paymentsOnEmployeesBehalf,
          personalIncidentalExpenses = allBenefits.personalIncidentalExpenses,
          qualifyingRelocationExpenses = allBenefits.qualifyingRelocationExpenses,
          employerProvidedProfessionalSubscriptions = allBenefits.employerProvidedProfessionalSubscriptions,
          employerProvidedServices = allBenefits.employerProvidedServices,
          incomeTaxPaidByDirector = allBenefits.incomeTaxPaidByDirector,
          travelAndSubsistence = allBenefits.travelAndSubsistence,
          vouchersAndCreditCards = allBenefits.vouchersAndCreditCards,
          nonCash = allBenefits.nonCash
        ),
        employmentBenefitsData = Benefits(
          accommodation = amendBenefits.accommodation,
          assets = amendBenefits.assets,
          assetTransfer = amendBenefits.assetTransfer,
          beneficialLoan = amendBenefits.beneficialLoan,
          car = amendBenefits.car,
          carFuel = amendBenefits.carFuel,
          educationalServices = amendBenefits.educationalServices,
          entertaining = amendBenefits.entertaining,
          expenses = amendBenefits.expenses,
          medicalInsurance = amendBenefits.medicalInsurance,
          telephone = amendBenefits.telephone,
          service = amendBenefits.service,
          taxableExpenses = amendBenefits.taxableExpenses,
          van = amendBenefits.van,
          vanFuel = amendBenefits.vanFuel,
          mileage = amendBenefits.mileage,
          nonQualifyingRelocationExpenses = amendBenefits.nonQualifyingRelocationExpenses,
          nurseryPlaces = amendBenefits.nurseryPlaces,
          otherItems = amendBenefits.otherItems,
          paymentsOnEmployeesBehalf = amendBenefits.paymentsOnEmployeesBehalf,
          personalIncidentalExpenses = amendBenefits.personalIncidentalExpenses,
          qualifyingRelocationExpenses = amendBenefits.nonQualifyingRelocationExpenses,
          employerProvidedProfessionalSubscriptions = amendBenefits.employerProvidedProfessionalSubscriptions,
          employerProvidedServices = amendBenefits.employerProvidedServices,
          incomeTaxPaidByDirector = amendBenefits.incomeTaxPaidByDirector,
          travelAndSubsistence = amendBenefits.travelAndSubsistence,
          vouchersAndCreditCards = amendBenefits.vouchersAndCreditCards,
          nonCash = amendBenefits.nonCash
        )
      ))

      await(underTest.performSubmitNrsPayload(authorisationRequest.user, model, "001", Some(priorData))) shouldBe Right()
    }
  }

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
            Some(allBenefits)
          )
        ),
        Some("employmentId")
      )

      val createNewEmploymentsAudit = CreateNewEmploymentBenefitsAudit(2021,
        authorisationRequest.user.affinityGroup.toLowerCase,
        authorisationRequest.user.nino,
        authorisationRequest.user.mtditid,
        anEmploymentSource.employerName,
        Some(anEmploymentSource.employerRef.get),
        allBenefits)

      mockAuditSendEvent(createNewEmploymentsAudit.toAuditModel)

      val customerEmploymentData = anEmploymentSource.copy(employmentBenefits = None)
      val employmentDataWithoutBenefits = anAllEmploymentData.copy(customerEmploymentData = Seq(customerEmploymentData))
      val expected: AuditResult = Success

      val result = underTest.performSubmitAudits(authorisationRequest.user, model, employmentId = anEmploymentSource.employmentId, taxYear = 2021, Some(employmentDataWithoutBenefits)).get
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
        2021,
        authorisationRequest.user.affinityGroup.toLowerCase,
        authorisationRequest.user.nino,
        authorisationRequest.user.mtditid,
        aBenefits,
        amendBenefits)
      mockAuditSendEvent(amendAudit.toAuditModel)

      val expected: AuditResult = Success
      val result = underTest.performSubmitAudits(authorisationRequest.user, model, employmentId = anEmploymentSource.employmentId, taxYear = 2021, Some(anAllEmploymentData)).get
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
            Some(allBenefits)
          )
        ),
        Some("001")
      )
      val result = underTest.performSubmitAudits(authorisationRequest.user, model, employmentId = "003", taxYear = 2021, Some(employmentsModel))
      result shouldBe None
    }
  }
}
