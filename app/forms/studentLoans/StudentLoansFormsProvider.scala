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

import forms.AmountForm
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class StudentLoansFormsProvider {

  def uglAmountForm(employerName: String, isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"studentLoans.undergraduateLoanAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = "studentLoans.undergraduateLoanAmount.error.invalidFormat",
    emptyFieldArguments = Seq(employerName)
  )

  def pglAmountForm(employerName: String, isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"studentLoans.postgraduateLoanAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"studentLoans.postgraduateLoanAmount.error.invalidFormat",
    emptyFieldArguments = Seq(employerName)
  )

}
