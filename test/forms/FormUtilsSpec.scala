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

package forms

import forms.AmountForm.amountForm
import play.api.data.Form
import utils.UnitTest

class FormUtilsSpec extends UnitTest {

  object Test extends FormUtils

  def theForm(): Form[BigDecimal] = {
    amountForm("nothing to see here", "this not good", "too big")
  }

  "The form" should {
    "decide whether to fill the form based on CYA & prior" when {
      "its looking for the mileage amount and there's an amount in CYA that differs from the prior amount" in {
        val employmentId = "001"
        val cya: Option[BigDecimal] = Some(166.66)
        val prior = Some(employmentsModel)

        val form = Test.fillFormFromPriorAndCYA(theForm(), prior, cya, employmentId) {
          source =>
            source.employmentBenefits.flatMap(_.benefits.flatMap(_.mileage))
        }

        val result = form.value
        result shouldBe Some(166.66)
      }

      "its looking for the mileage amount and there's an amount in CYA that's the same as the prior amount" in {
        val employmentId = "001"
        val cya: Option[BigDecimal] = Some(5.00)
        val prior = Some(employmentsModel)

        val form = Test.fillFormFromPriorAndCYA(theForm(), prior, cya, employmentId) {
          source => source.employmentBenefits.flatMap(_.benefits.flatMap(_.mileage))
        }

        val result = form.value
        result shouldBe None
      }

      "its looking for the mileage amount and there's an amount in CYA but no prior amount" in {
        val employmentId = "001"
        val cya: Option[BigDecimal] = Some(5.00)
        val prior = None

        val form = Test.fillFormFromPriorAndCYA(theForm(), prior, cya, employmentId) {
          source =>
            source.employmentBenefits.flatMap(_.benefits.flatMap(_.mileage))
        }

        val result = form.value
        result shouldBe Some(5.00)
      }

      "its looking for the mileage amount and there's no amount in CYA and no prior amount" in {
        val employmentId = "001"
        val cya: Option[BigDecimal] = None
        val prior = None

        val form = Test.fillFormFromPriorAndCYA(theForm(), prior, cya, employmentId) {
          source =>
            source.employmentBenefits.flatMap(_.benefits.flatMap(_.mileage))
        }

        val result = form.value
        result shouldBe None
      }
    }
  }
}