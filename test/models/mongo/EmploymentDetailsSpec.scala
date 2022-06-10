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

package models.mongo

import support.UnitTest
import support.builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails

class EmploymentDetailsSpec extends UnitTest {

  ".isSubmittable" should {
    "return false" when {
      "employerRef is not defined" in {
        val underTest = anEmploymentDetails.copy(employerRef = None)

        underTest.isSubmittable shouldBe false
      }

      "startDate is not defined" in {
        val underTest = anEmploymentDetails.copy(startDate = None)

        underTest.isSubmittable shouldBe false
      }

      "payrollId is not defined" in {
        val underTest = anEmploymentDetails.copy(payrollId = None)

        underTest.isSubmittable shouldBe false
      }

      "taxablePayToDate is not defined" in {
        val underTest = anEmploymentDetails.copy(taxablePayToDate = None)

        underTest.isSubmittable shouldBe false
      }

      "totalTaxToDate is not defined" in {
        val underTest = anEmploymentDetails.copy(totalTaxToDate = None)

        underTest.isSubmittable shouldBe false
      }
    }

    "return true when employerRef, startDate, payrollId, taxablePayToDate and totalTaxToDate are defined" in {
      val underTest = anEmploymentDetails.copy(
        employerRef = Some("123/12345"),
        startDate = Some("2020-11-11"),
        payrollId = Some("12345678"),
        taxablePayToDate = Some(55.99),
        totalTaxToDate = Some(3453453.00)
      )

      underTest.isSubmittable shouldBe true
    }
  }

  ".isFinished" should {
    "return false" when {
      "employerRef is None" in {
        val underTest = anEmploymentDetails.copy(employerRef = None)

        underTest.isFinished shouldBe false
      }

      "startDate is None" in {
        val underTest = anEmploymentDetails.copy(startDate = None)

        underTest.isFinished shouldBe false
      }

      "payrollId is None" in {
        val underTest = anEmploymentDetails.copy(payrollId = None)

        underTest.isFinished shouldBe false
      }

      "didYouLeaveQuestion is true and cessationDate is None" in {
        val underTest = anEmploymentDetails.copy(didYouLeaveQuestion = Some(true), cessationDate = None)

        underTest.isFinished shouldBe false
      }

      "didYouLeaveQuestion is None" in {
        val underTest = anEmploymentDetails.copy(didYouLeaveQuestion = None)

        underTest.isFinished shouldBe false
      }

      "taxablePayToDate is None" in {
        val underTest = anEmploymentDetails.copy(taxablePayToDate = None)

        underTest.isFinished shouldBe false
      }

      "totalTaxToDate is None" in {
        val underTest = anEmploymentDetails.copy(totalTaxToDate = None)

        underTest.isFinished shouldBe false
      }
    }

    "return true" when {
      "all fields are populated and didYouLeaveQuestion is false" in {
        val underTest = anEmploymentDetails.copy(didYouLeaveQuestion = Some(false), cessationDate = None)

        underTest.isFinished shouldBe true
      }

      "all fields are populated" in {
        anEmploymentDetails.isFinished shouldBe true
      }
    }
  }
}
