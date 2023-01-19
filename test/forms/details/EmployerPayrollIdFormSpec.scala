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

package forms.details

import play.api.data.FormError
import support.UnitTest

class EmployerPayrollIdFormSpec extends UnitTest {

  private val anyBoolean = true

  ".employerPayrollIdForm" should {
    "allow empty form" in {
      val emptyFormData = Map[String, String]().empty
      val underTest = EmployerPayrollIdForm.employerPayrollIdForm(anyBoolean)

      underTest.bind(emptyFormData).errors shouldBe Seq.empty
    }

    "allow for correct data format" in {
      val formData = Map(EmployerPayrollIdForm.payrollId -> "some-correct-value")
      val underTest = EmployerPayrollIdForm.employerPayrollIdForm(anyBoolean)

      underTest.bind(formData).errors shouldBe Seq.empty
    }

    "contain error when more than 38 characters" when {
      val payrollWithMoreThan38Characters = "a" * 39
      val formData = Map(EmployerPayrollIdForm.payrollId -> payrollWithMoreThan38Characters)

      "for agent" in {
        val underTest = EmployerPayrollIdForm.employerPayrollIdForm(isAgent = true)

        underTest.bind(formData).errors should contain(FormError(EmployerPayrollIdForm.payrollId, "employment.payrollId.error.tooMany.agent"))
      }

      "for individual" in {
        val underTest = EmployerPayrollIdForm.employerPayrollIdForm(isAgent = false)

        underTest.bind(formData).errors should contain(FormError(EmployerPayrollIdForm.payrollId, "employment.payrollId.error.tooMany.individual"))
      }
    }

    "contain error when payroll Id is in the wrong format" when {
      val wrongFormat = "payrollId-with-forbidden-character-#"
      val formData = Map(EmployerPayrollIdForm.payrollId -> wrongFormat)

      "for agent" in {
        val underTest = EmployerPayrollIdForm.employerPayrollIdForm(isAgent = true)

        underTest.bind(formData).errors should contain(FormError(EmployerPayrollIdForm.payrollId, "employment.payrollId.error.incorrect.agent"))
      }

      "for individual" in {
        val underTest = EmployerPayrollIdForm.employerPayrollIdForm(isAgent = false)

        underTest.bind(formData).errors should contain(FormError(EmployerPayrollIdForm.payrollId, "employment.payrollId.error.incorrect.individual"))
      }
    }
  }
}
