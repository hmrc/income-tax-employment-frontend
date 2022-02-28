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

package services.studentLoans

import support.builders.models.employment.StudentLoansBuilder.aStudentLoans
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithStudentLoans}
import support.mocks.MockEmploymentSessionService
import utils.UnitTest

class StudentLoansServiceSpec extends UnitTest with MockEmploymentSessionService {

  private val employmentId = "some-employment-id"

  private val underTest = new StudentLoansService(mockEmploymentSessionService)

  "updateUglQuestion" should {
    "set uglDeductions to false and uglDeductionsAmount to None" when {
      "passed false" in {
        val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
        val expectedStudentLoans = aStudentLoans.toStudentLoansCYAModel().copy(uglDeduction = false, uglDeductionAmount = None)
        val expectedEmploymentUserData = anEmploymentUserDataWithStudentLoans(expectedStudentLoans).copy(isPriorSubmission = false, hasPriorBenefits = false)

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateUglQuestion(authorisationRequest.user, taxYearEOY, employmentId, givenEmploymentUserData, ugl = false)) shouldBe Right(expectedEmploymentUserData)
      }
    }
    "set uglDeductions to true and uglDeductionsAmount to None" when {
      "passed true" in {
        val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
        val expectedStudentLoans = aStudentLoans.toStudentLoansCYAModel().copy(uglDeduction = true)
        val expectedEmploymentUserData = anEmploymentUserDataWithStudentLoans(expectedStudentLoans).copy(isPriorSubmission = false, hasPriorBenefits = false)

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updateUglQuestion(authorisationRequest.user, taxYearEOY, employmentId, givenEmploymentUserData, ugl = true)) shouldBe Right(expectedEmploymentUserData)
      }
    }
  }

  "updatePglQuestion" should {
    "set pglDeductions to false and pglDeductionsAmount to None" when {
      "passed false" in {
        val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
        val expectedStudentLoans = aStudentLoans.toStudentLoansCYAModel().copy(pglDeduction = false, pglDeductionAmount = None)
        val expectedEmploymentUserData = anEmploymentUserDataWithStudentLoans(expectedStudentLoans).copy(isPriorSubmission = false, hasPriorBenefits = false)

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updatePglQuestion(authorisationRequest.user, taxYearEOY, employmentId, givenEmploymentUserData, pgl = false)) shouldBe Right(expectedEmploymentUserData)
      }
    }
    "set pglDeductions to true and pglDeductionsAmount to None" when {
      "passed true" in {
        val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
        val expectedStudentLoans = aStudentLoans.toStudentLoansCYAModel().copy(pglDeduction = true)
        val expectedEmploymentUserData = anEmploymentUserDataWithStudentLoans(expectedStudentLoans).copy(isPriorSubmission = false, hasPriorBenefits = false)

        mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

        await(underTest.updatePglQuestion(authorisationRequest.user, taxYearEOY, employmentId, givenEmploymentUserData, pgl = true)) shouldBe Right(expectedEmploymentUserData)
      }
    }
  }

  "updateUglDeductionAmount" should {
    "set UglDeductionsAmount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedStudentLoans = aStudentLoans.toStudentLoansCYAModel().copy(uglDeductionAmount = Some(500.00))
      val expectedEmploymentUserData = anEmploymentUserDataWithStudentLoans(expectedStudentLoans).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateUglDeductionAmount(authorisationRequest.user, taxYearEOY, employmentId, givenEmploymentUserData, uglAmount = 500.00)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updatePglDeductionAmount" should {
    "set pglDeductionsAmount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedStudentLoans = aStudentLoans.toStudentLoansCYAModel().copy(pglDeductionAmount = Some(500.00))
      val expectedEmploymentUserData = anEmploymentUserDataWithStudentLoans(expectedStudentLoans).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updatePglDeductionAmount(authorisationRequest.user, taxYearEOY, employmentId, givenEmploymentUserData, pglAmount = 500.00)) shouldBe Right(expectedEmploymentUserData)
    }
  }
}
