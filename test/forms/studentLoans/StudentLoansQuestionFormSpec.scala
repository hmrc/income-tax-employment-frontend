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

package forms.studentLoans

import forms.studentLoans.StudentLoanQuestionForm._
import models.employment.StudentLoansCYAModel
import play.api.data.{Form, FormError}
import support.UnitTest

class StudentLoansQuestionFormSpec extends UnitTest {

  def theForm(isAgent: Boolean = false): Form[StudentLoansQuestionModel] = {
    StudentLoanQuestionForm.studentLoanForm(isAgent)
  }

  "The Form" should {
    "correctly validate" when {
      "with one input (ugl)" in {
        val expected = StudentLoansQuestionModel(Seq("ugl"))
        val actual = theForm().bind(Map("studentLoans[0]" -> "ugl")).value
        actual shouldBe Some(expected)
      }

      "with one input (pgl)" in {
        val expected = StudentLoansQuestionModel(Seq("pgl"))
        val actual = theForm().bind(Map("studentLoans[0]" -> "pgl")).value
        actual shouldBe Some(expected)
      }

      "with one input (none)" in {
        val expected = StudentLoansQuestionModel(Seq("none"))
        val actual = theForm().bind(Map("studentLoans[0]" -> "none")).value
        actual shouldBe Some(expected)
      }

      "with two inputs (ugl pgl)" in {
        val expected = StudentLoansQuestionModel(Seq("ugl", "pgl"))
        val actual = theForm().bind(Map("studentLoans[0]" -> "ugl", "studentLoans[1]" -> "pgl")).value
        actual shouldBe Some(expected)
      }
    }

    "correctly invalidate" when {
      "with no input individual" in {
        val actual = theForm().bind(Map("" -> ""))
        actual.errors should contain(FormError("", "studentLoansQuestion.checkbox.error.individual"))
      }

      "with max inputs individual" in {
        val actual = theForm().bind(Map("studentLoans[0]" -> "ugl", "studentLoans[1]" -> "pgl", "studentLoans[2]" -> "none"))
        actual.errors should contain(FormError("", "studentLoansQuestion.checkbox.error.individual"))
      }

      "with no input agent" in {
        val actual = theForm(true).bind(Map("" -> ""))
        actual.errors should contain(FormError("", "studentLoansQuestion.checkbox.error.agent"))
      }

      "with max inputs agent" in {
        val actual = theForm(true).bind(Map("studentLoans[0]" -> "ugl", "studentLoans[1]" -> "pgl", "studentLoans[2]" -> "none"))
        actual.errors should contain(FormError("", "studentLoansQuestion.checkbox.error.agent"))
      }
    }
  }

  "StudentLoanQuestionModel.toStudentLoansCyaModel" should {
    "return the correct cyaModel" when {
      "passed previousData and (pgl, ugl)" in {
        val previousData = StudentLoansCYAModel(uglDeduction = false, Some(100.00), pglDeduction = false, Some(120.00))
        StudentLoansQuestionModel(Seq("ugl", "pgl")).toStudentLoansCyaModel(Some(previousData)) shouldBe
          StudentLoansCYAModel(uglDeduction = true, Some(100.00), pglDeduction = true, Some(120.00))
      }

      "passed previousData and (none)" in {
        val previousData = StudentLoansCYAModel(uglDeduction = false, Some(100.00), pglDeduction = false, Some(120.00))
        StudentLoansQuestionModel(Seq("none")).toStudentLoansCyaModel(Some(previousData)) shouldBe
          StudentLoansCYAModel(uglDeduction = false, None, pglDeduction = false, None)
      }

      "without previousData" in {
        StudentLoansQuestionModel(Seq("ugl", "pgl")).toStudentLoansCyaModel(None) shouldBe
          StudentLoansCYAModel(uglDeduction = true, None, pglDeduction = true, None)
      }
    }
  }
}
