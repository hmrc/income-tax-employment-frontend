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

import models.employment.EmploymentDate
import support.builders.models.UserBuilder.aUser
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithDetails}
import support.mocks.MockEmploymentSessionService
import support.{TaxYearProvider, UnitTest}

import scala.concurrent.ExecutionContext

class EmploymentServiceSpec extends UnitTest
  with TaxYearProvider
  with MockEmploymentSessionService {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val employmentId = "some-employment-id"

  private val underTest = new EmploymentService(mockEmploymentSessionService, ec)

  "updateEmployerRef" should {
    "set employerRef" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(employerRef = Some("employerRef"))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEmployerRef(aUser, taxYearEOY, employmentId, givenEmploymentUserData, payeRef = Some("employerRef"))) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateStartDate" should {
    "set startDate and update cessationDate to None" when {
      "cessationDate defined and before startDate" in {
        val givenEmploymentDetails = anEmploymentDetails.copy(cessationDate = Some(s"$taxYearEOY-01-01"))
        val givenEmploymentUserData = anEmploymentUserDataWithDetails(givenEmploymentDetails, isPriorSubmission = false, hasPriorBenefits = false)
        val expectedEmploymentDetails = anEmploymentDetails.copy(cessationDate = None, startDate = Some(s"$taxYearEOY-01-02"))
        val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateStartDate(aUser, taxYearEOY, employmentId, givenEmploymentUserData, startedDate = EmploymentDate("2", "1", s"$taxYearEOY"))) shouldBe
          Right(expectedEmploymentUserData)
      }
    }

    "set startDate and keep cessationDate" when {
      "cessationDate defined and on or after startDate" in {
        val givenEmploymentDetails = anEmploymentDetails.copy(cessationDate = Some(s"$taxYearEOY-01-02"), startDate = Some(s"${taxYearEOY - 1}-01-01"))
        val givenEmploymentUserData = anEmploymentUserDataWithDetails(givenEmploymentDetails, isPriorSubmission = false, hasPriorBenefits = false)
        val expectedEmploymentDetails = anEmploymentDetails.copy(cessationDate = Some(s"$taxYearEOY-01-02"), startDate = Some(s"$taxYearEOY-01-02"))
        val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateStartDate(aUser, taxYearEOY, employmentId, givenEmploymentUserData, startedDate = EmploymentDate("2", "1", s"$taxYearEOY"))) shouldBe
          Right(expectedEmploymentUserData)
      }
    }
  }

  "updatePayrollId" should {
    "set payrollId" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(payrollId = Some("payrollId"))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updatePayrollId(aUser, taxYearEOY, employmentId, givenEmploymentUserData, payrollId = Some("payrollId"))) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateDidYouLeaveQuestion" should {
    "set didYouLeaveQuestion to false and cessationDate value is cleared when false" in {
      val givenEmploymentDetails = anEmploymentDetails.copy(didYouLeaveQuestion = Some(true), cessationDate = Some("some-date"))
      val givenEmploymentUserData = anEmploymentUserDataWithDetails(givenEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(didYouLeaveQuestion = Some(false), cessationDate = None)
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateDidYouLeaveQuestion(aUser, taxYearEOY, employmentId, givenEmploymentUserData, leftEmployer = false)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set didYouLeaveQuestion to true and cessationDate value is preserved when true" in {
      val givenEmploymentDetails = anEmploymentDetails.copy(didYouLeaveQuestion = Some(false), cessationDate = Some("some-date"))
      val givenEmploymentUserData = anEmploymentUserDataWithDetails(givenEmploymentDetails)
      val expectedEmploymentDetails = anEmploymentDetails.copy(didYouLeaveQuestion = Some(true), cessationDate = Some("some-date"))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateDidYouLeaveQuestion(aUser, taxYearEOY, employmentId, givenEmploymentUserData, leftEmployer = true)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateCessationDate" should {
    "set cessationDate" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(cessationDate = Some("some-date"))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCessationDate(aUser, taxYearEOY, employmentId, givenEmploymentUserData, cessationDate = "some-date")) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateTaxablePayToDate" should {
    "set taxablePayToDate amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(taxablePayToDate = Some(123))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTaxablePayToDate(aUser, taxYearEOY, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateTotalTaxToDate" should {
    "set totalTaxToDate amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedEmploymentDetails = anEmploymentDetails.copy(totalTaxToDate = Some(123))
      val expectedEmploymentUserData = anEmploymentUserDataWithDetails(expectedEmploymentDetails).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTotalTaxToDate(aUser, taxYearEOY, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }
}
