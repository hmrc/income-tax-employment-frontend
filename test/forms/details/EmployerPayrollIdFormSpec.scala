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

  private val formProvider = new EmploymentDetailsFormsProvider()

  ".employerPayrollIdForm" should {
    "allow empty form" in {
      val emptyFormData = Map[String, String]().empty

      formProvider.employerPayrollIdForm().bind(emptyFormData).errors shouldBe Seq.empty
    }

    "allow for correct data format" in {
      val formData = Map(EmployerPayrollIdForm.payrollId -> "some-correct-value")

      formProvider.employerPayrollIdForm().bind(formData).errors shouldBe Seq.empty
    }

    "contain error when more than 38 characters" in {
      val payrollWithMoreThan38Characters = "a" * 39
      val formData = Map(EmployerPayrollIdForm.payrollId -> payrollWithMoreThan38Characters)

      formProvider.employerPayrollIdForm().bind(formData).errors should
        contain(FormError(EmployerPayrollIdForm.payrollId, "employment.payrollId.error.tooManyCharacters"))
    }

    "contain error when payroll Id is in the wrong format" in {
      val wrongFormat = "payrollId-with-forbidden-character-#"
      val formData = Map(EmployerPayrollIdForm.payrollId -> wrongFormat)

      formProvider.employerPayrollIdForm().bind(formData).errors should
        contain(FormError(EmployerPayrollIdForm.payrollId, "employment.payrollId.error.invalidCharacters", List("#")))
    }
  }
}
