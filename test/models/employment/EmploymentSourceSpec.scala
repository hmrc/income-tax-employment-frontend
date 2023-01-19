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

package models.employment

import support.UnitTest
import support.builders.models.employment.EmploymentDataBuilder.anEmploymentData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.employment.PayBuilder.aPay

class EmploymentSourceSpec extends UnitTest {

  ".employmentDetailsSubmittable" should {
    "return false" when {
      "startDate not defined" in {
        val underTest = anEmploymentSource.copy(startDate = None)

        underTest.employmentDetailsSubmittable shouldBe false
      }

      "employmentData not defined" in {
        val underTest = anEmploymentSource.copy(employmentData = None)

        underTest.employmentDetailsSubmittable shouldBe false
      }

      "pay not defined" in {
        val underTest = anEmploymentSource.copy(employmentData = Some(anEmploymentData.copy(pay = None)))

        underTest.employmentDetailsSubmittable shouldBe false
      }

      "taxablePayToDate not defined" in {
        val underTest = anEmploymentSource.copy(employmentData = Some(anEmploymentData.copy(pay = Some(aPay.copy(taxablePayToDate = None)))))

        underTest.employmentDetailsSubmittable shouldBe false
      }

      "totalTaxToDate not defined" in {
        val underTest = anEmploymentSource.copy(employmentData = Some(anEmploymentData.copy(pay = Some(aPay.copy(totalTaxToDate = None)))))

        underTest.employmentDetailsSubmittable shouldBe false
      }
    }

    "return true when startDate, taxablePayToDate and totalTaxToDate are defined" in {
      val underTest = anEmploymentSource.copy(employmentData = Some(anEmploymentData.copy(pay = Some(aPay.copy(taxablePayToDate = Some(100), totalTaxToDate = Some(200))))))

      underTest.employmentDetailsSubmittable shouldBe true
    }
  }
}
