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

import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import models.employment.AllEmploymentData.employmentIdExists
import utils.UnitTest

class AllEmploymentDataSpec extends UnitTest {

  private val employmentId = "some-employment-id"

  "employmentIdExists" should {
    "return true if the employmentId exists in HMRC Data only" in {
      val hmrcEmploymentSource = anEmploymentSource.copy(employmentId = employmentId)
      val employmentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(hmrcEmploymentSource))

      employmentIdExists(employmentData, Some(employmentId)) shouldBe true
    }

    "return true if the employmentId exists in Customer Data only" in {
      val customerEmploymentSource = anEmploymentSource.copy(employmentId = employmentId)
      val employmentData = anAllEmploymentData.copy(customerEmploymentData = Seq(customerEmploymentSource))

      employmentIdExists(employmentData, Some(employmentId)) shouldBe true
    }

    "return false if the employmentId does not exists in Customer and HMRC Data" in {
      val hmrcEmploymentSource = anEmploymentSource.copy(employmentId = "different-employment-Id")
      val customerEmploymentSource = anEmploymentSource.copy(employmentId = "different-employment-Id")
      val employmentData = anAllEmploymentData.copy(
        hmrcEmploymentData = Seq(hmrcEmploymentSource),
        customerEmploymentData = Seq(customerEmploymentSource)
      )

      employmentIdExists(employmentData, Some(employmentId)) shouldBe false
    }

    "return false if employmentId is not defined" in {
      val customerEmploymentSource = anEmploymentSource.copy(employmentId = employmentId)
      val employmentData = anAllEmploymentData.copy(customerEmploymentData = Seq(customerEmploymentSource))

      employmentIdExists(employmentData, None) shouldBe false
    }
  }
}
