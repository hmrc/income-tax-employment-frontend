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

package models

import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentDataBuilder.anEmploymentData
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.employment.PayBuilder.aPay
import models.IncomeTaxUserData.excludePensionIncome
import models.employment.AllEmploymentData
import utils.UnitTest

class IncomeTaxUserDataSpec extends UnitTest {

  private val employmentDataWithOccPenTrueAndPay = anEmploymentData.copy(occPen = Some(true), pay = Some(aPay))
  private val employmentDataWithOccPenTrueAndNoPay = anEmploymentData.copy(occPen = Some(true), pay = None)
  private val employmentDataWithOccPenFalseAndWithPay = anEmploymentData.copy(occPen = Some(false), pay = Some(aPay))
  private val employmentDataWithOccPenFalseAndNoPay = anEmploymentData.copy(occPen = Some(false), pay = None)
  private val employmentDataWithNoOccPenAndWithPay = anEmploymentData.copy(occPen = None, pay = Some(aPay))
  private val employmentDataWithNoOccPenAndNoPay = anEmploymentData.copy(occPen = None, pay = None)

  "stripPensionIncome" should {
    "only strip pay when occupational pension is true" in {
      val allEmploymentData: AllEmploymentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(
        anEmploymentSource.copy(employmentData = Some(employmentDataWithOccPenTrueAndPay)),
        anEmploymentSource.copy(employmentData = Some(employmentDataWithOccPenTrueAndNoPay)),
        anEmploymentSource.copy(employmentData = Some(employmentDataWithOccPenFalseAndWithPay)),
        anEmploymentSource.copy(employmentData = Some(employmentDataWithOccPenFalseAndNoPay)),
        anEmploymentSource.copy(employmentData = Some(employmentDataWithNoOccPenAndWithPay)),
        anEmploymentSource.copy(employmentData = Some(employmentDataWithNoOccPenAndNoPay))
      ))

      excludePensionIncome(IncomeTaxUserData(Some(allEmploymentData))) shouldBe
        IncomeTaxUserData(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(
          anEmploymentSource.copy(employmentData = Some(employmentDataWithOccPenTrueAndNoPay)),
          anEmploymentSource.copy(employmentData = Some(employmentDataWithOccPenTrueAndNoPay)),
          anEmploymentSource.copy(employmentData = Some(employmentDataWithOccPenFalseAndWithPay)),
          anEmploymentSource.copy(employmentData = Some(employmentDataWithOccPenFalseAndNoPay)),
          anEmploymentSource.copy(employmentData = Some(employmentDataWithNoOccPenAndWithPay)),
          anEmploymentSource.copy(employmentData = Some(employmentDataWithNoOccPenAndNoPay))
        ))))
    }
  }
}