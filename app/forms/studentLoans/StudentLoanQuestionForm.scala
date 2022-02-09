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

import forms.validation.utils.ConstraintUtil.{ConstraintUtil, constraint}
import models.employment.StudentLoansCYAModel
import play.api.data.Form
import play.api.data.Forms.{mapping, seq, text}
import play.api.data.validation.{Constraint, Invalid, Valid}

object StudentLoanQuestionForm {

  case class StudentLoansQuestionModel(studentLoans: Seq[String]){

    val containsUgl: Boolean = studentLoans.contains("ugl")
    val containsPgl: Boolean = studentLoans.contains("pgl")
    val containsNone: Boolean = studentLoans.contains("none")

    def toStudentLoansCyaModel(previousStudentLoans: Option[StudentLoansCYAModel]): StudentLoansCYAModel = {
      previousStudentLoans.fold(StudentLoansCYAModel(uglDeduction = containsUgl, pglDeduction = containsPgl))(previous => previous.copy(
          uglDeduction = containsUgl,
          uglDeductionAmount = if(containsUgl) previous.uglDeductionAmount else None,
          pglDeduction = containsPgl,
          pglDeductionAmount = if(containsPgl) previous.pglDeductionAmount else None
        ))
    }
  }

  val studentLoans : String = "studentLoans"

  val allEmpty: String => Constraint[StudentLoansQuestionModel] = msgKey => constraint[StudentLoansQuestionModel](
      studentLoans => {if (studentLoans.studentLoans.isEmpty) Invalid(msgKey) else Valid}
  )

  val allChecked: String => Constraint[StudentLoansQuestionModel] = msgKey => constraint[StudentLoansQuestionModel](
    studentLoans => if ((studentLoans.containsUgl || studentLoans.containsPgl) && studentLoans.containsNone) Invalid(msgKey) else Valid
  )

  def studentLoanForm(isAgent: Boolean): Form[StudentLoansQuestionModel] = Form[StudentLoansQuestionModel](
    mapping(
      studentLoans -> seq(text)
    )(StudentLoansQuestionModel.apply)(StudentLoansQuestionModel.unapply).verifying(
      allEmpty(s"studentLoansQuestion.checkbox.error.${if(isAgent) "agent" else "individual"}")
        andThen allChecked(s"studentLoansQuestion.checkbox.error.${if(isAgent) "agent" else "individual"}"))
  )

}
