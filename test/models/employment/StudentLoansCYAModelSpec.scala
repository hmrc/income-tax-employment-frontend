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

import models.benefits.Benefits
import models.expenses.Expenses
import play.api.libs.json.{JsValue, Json}
import utils.UnitTest

class StudentLoansCYAModelSpec extends UnitTest {

  "toDeductions" when {
    "passed a valid student loans cya model" should {
      "produce a deductions model" in {

        StudentLoansCYAModel(uglDeduction = false, None, pglDeduction = true, Some(44)).toDeductions shouldBe Some(
          Deductions(
            Some(StudentLoans(
              None, Some(44)
            ))
          )
        )
      }
      "produce a ugl deductions model" in {

        StudentLoansCYAModel(uglDeduction = true, Some(67), pglDeduction = false, None).toDeductions shouldBe Some(
          Deductions(
            Some(StudentLoans(
              Some(67), None
            ))
          )
        )
      }
      "produce a deductions model with both values" in {

        StudentLoansCYAModel(uglDeduction = true, Some(55), pglDeduction = true, Some(44)).toDeductions shouldBe Some(
          Deductions(
            Some(StudentLoans(
              Some(55), Some(44)
            ))
          )
        )
      }
      "return empty deductions" in {
        StudentLoansCYAModel(uglDeduction = false, None, pglDeduction = false, None).toDeductions shouldBe Some(Deductions(None))
      }
    }
  }

}
