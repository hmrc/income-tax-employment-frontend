/*
 * Copyright 2021 HM Revenue & Customs
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

package forms

import models.employment.{AllEmploymentData, EmploymentExpenses, EmploymentSource}
import play.api.data.Form
import services.EmploymentSessionService

trait FormUtils {

  val employmentSessionService: EmploymentSessionService

  def fillFormFromPriorAndCYA(form: Form[BigDecimal], prior: Option[AllEmploymentData],
                              cya: Option[BigDecimal], employmentId: String)(f: EmploymentSource => Option[BigDecimal]): Form[BigDecimal] ={

    val priorAmount = prior.flatMap { priorEmp =>
      employmentSessionService.employmentSourceToUse(priorEmp, employmentId, isInYear = false).flatMap {
        employmentSource =>
          f(employmentSource._1)
      }
    }

    fillForm(form,priorAmount,cya)
  }

  def fillExpensesFormFromPriorAndCYA(form: Form[BigDecimal], prior: Option[AllEmploymentData],
                                      cya: Option[BigDecimal])(f: EmploymentExpenses => Option[BigDecimal]): Form[BigDecimal] ={

    val priorAmount = prior.flatMap { priorEmp =>
      employmentSessionService.getLatestExpenses(priorEmp, isInYear = false).flatMap {
        expenses =>
          f(expenses._1)
      }
    }

    fillForm(form,priorAmount,cya)
  }

  private def fillForm(form: Form[BigDecimal], prior: Option[BigDecimal], cya: Option[BigDecimal]): Form[BigDecimal] ={
    cya.fold(form)(cya => if(prior.contains(cya)) form else form.fill(cya))
  }
}
