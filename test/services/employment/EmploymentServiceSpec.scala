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

import builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithDetails}
import config.MockEmploymentSessionService
import models.employment.EmploymentDate
import utils.UnitTest

class EmploymentServiceSpec extends UnitTest with MockEmploymentSessionService {

  private val taxYear = 2021
  private val employmentId = "some-employment-id"

  private val underTest = new EmploymentService(mockEmploymentSessionService, mockExecutionContext)

  "updateEmployerRef" should {
    "set employerRef" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(employerRef = Some("employerRef"))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEmployerRef(taxYear, employmentId, givenEmploymentUserData, payeRef = "employerRef")) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateStartDate" should {
    "set startDate and update cessationDate to None" when {
      "cessationDate defined and before startDate" in {
        val givenEmploymentDetails = anEmploymentDetails.copy(cessationDate = Some("2021-01-01"))
        val givenEmploymentUserData = anEmploymentUserDataWithDetails(givenEmploymentDetails, isPriorSubmission = false, hasPriorBenefits = false)
        val expectedEmploymentDetails = anEmploymentDetails.copy(cessationDate = None, startDate = Some("2021-01-02"))
        val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

        mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateStartDate(taxYear, employmentId, givenEmploymentUserData, startedDate = EmploymentDate("2", "1", "2021"))) shouldBe
          Right(expectedEmploymentUserData)
      }
    }

    "set startDate and keep cessationDate" when {
      "cessationDate defined and on or after startDate" in {
        val givenEmploymentDetails = anEmploymentDetails.copy(cessationDate = Some("2021-01-02"), startDate = Some("2020-01-01"))
        val givenEmploymentUserData = anEmploymentUserDataWithDetails(givenEmploymentDetails, isPriorSubmission = false, hasPriorBenefits = false)
        val expectedEmploymentDetails = anEmploymentDetails.copy(cessationDate = Some("2021-01-02"), startDate = Some("2021-01-02"))
        val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

        mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateStartDate(taxYear, employmentId, givenEmploymentUserData, startedDate = EmploymentDate("2", "1", "2021"))) shouldBe
          Right(expectedEmploymentUserData)
      }
    }
  }

  "updatePayrollId" should {
    "set payrollId" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(payrollId = Some("payrollId"))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updatePayrollId(taxYear, employmentId, givenEmploymentUserData, payrollId = "payrollId")) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateCessationDateQuestion" should {
    "set cessationDateQuestion to true and cessationDate value is cleared when true" in {
      val givenEmploymentDetails = anEmploymentDetails.copy(cessationDateQuestion = Some(false), cessationDate = Some("some-date"))
      val givenEmploymentUserData = anEmploymentUserDataWithDetails(givenEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(cessationDateQuestion = Some(true), cessationDate = None)
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCessationDateQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set cessationDateQuestion to false and cessationDate value is preserved when false" in {
      val givenEmploymentDetails = anEmploymentDetails.copy(cessationDateQuestion = Some(true), cessationDate = Some("some-date"))
      val givenEmploymentUserData = anEmploymentUserDataWithDetails(givenEmploymentDetails)
      val expectedEmploymentDetails = anEmploymentDetails.copy(cessationDateQuestion = Some(false), cessationDate = Some("some-date"))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCessationDateQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateCessationDate" should {
    "set cessationDate" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(cessationDate = Some("some-date"))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCessationDate(taxYear, employmentId, givenEmploymentUserData, cessationDate = "some-date")) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateTaxablePayToDate" should {
    "set taxablePayToDate amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(taxablePayToDate = Some(123))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTaxablePayToDate(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateTotalTaxToDate" should {
    "set totalTaxToDate amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(totalTaxToDate = Some(123))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, expectedEmploymentUserData,expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTotalTaxToDate(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }
}
