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

package models.benefits

import controllers.benefits.reimbursed.routes._
import controllers.employment.routes.CheckYourBenefitsController
import utils.UnitTest

class ReimbursedCostsVouchersAndNonCashModelSpec extends UnitTest {

  private val employmentId = "id"
  private val taxYear = 2022
  private val model = ReimbursedCostsVouchersAndNonCashModel(
    sectionQuestion = Some(true),
    expensesQuestion = Some(true),
    expenses = Some(55.55),
    taxableExpensesQuestion = Some(true),
    taxableExpenses = Some(55.55),
    vouchersAndCreditCardsQuestion = Some(true),
    vouchersAndCreditCards = Some(55.55),
    nonCashQuestion = Some(true),
    nonCash = Some(55.55),
    otherItemsQuestion = Some(true),
    otherItems = Some(55.55)
  )

  "isFinished" should {
    "return reimbursedCostsVouchersAndNonCash yes no page" in {
      model.copy(sectionQuestion = None).isFinished(taxYear, employmentId) shouldBe
        Some(ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYear, employmentId))
    }
    "return none when section is finished" in {
      model.copy(sectionQuestion = Some(false)).isFinished(taxYear, "employmentId") shouldBe None
      model.isFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "expensesSectionFinished" should {
    "return expenses yes no page" in {
      model.copy(expensesQuestion = None).expensesSectionFinished(taxYear, employmentId) shouldBe
        Some(controllers.benefits.reimbursed.routes.NonTaxableCostsBenefitsController.show(taxYear, employmentId))
    }

    "return expenses amount page" in {
      model.copy(expenses = None).expensesSectionFinished(taxYear, employmentId) shouldBe
        Some(controllers.benefits.reimbursed.routes.NonTaxableCostsBenefitsAmountController.show(taxYear, employmentId))
      Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      model.copy(expensesQuestion = Some(false)).expensesSectionFinished(taxYear, "employmentId") shouldBe None
      model.expensesSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "taxableExpensesSectionFinished" should {
    "return taxableExpenses yes no page" in {
      model.copy(taxableExpensesQuestion = None).taxableExpensesSectionFinished(taxYear, employmentId) shouldBe
        Some(TaxableCostsBenefitsController.show(taxYear, employmentId))
    }

    "return taxableExpenses amount page" in {
      model.copy(taxableExpenses = None).taxableExpensesSectionFinished(taxYear, employmentId) shouldBe
        Some(TaxableCostsBenefitsAmountController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      model.copy(taxableExpensesQuestion = Some(false)).taxableExpensesSectionFinished(taxYear, "employmentId") shouldBe None
      model.taxableExpensesSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "vouchersAndCreditCardsSectionFinished" should {
    "return vouchersAndCreditCards yes no page" in {
      model.copy(vouchersAndCreditCardsQuestion = None).vouchersAndCreditCardsSectionFinished(taxYear, employmentId) shouldBe
        Some(VouchersBenefitsController.show(taxYear, employmentId))
    }

    "return vouchersAndCreditCards amount page" in {
      model.copy(vouchersAndCreditCards = None).vouchersAndCreditCardsSectionFinished(taxYear, employmentId) shouldBe
        Some(VouchersBenefitsAmountController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      model.copy(vouchersAndCreditCardsQuestion = Some(false)).vouchersAndCreditCardsSectionFinished(taxYear, "employmentId") shouldBe None
      model.vouchersAndCreditCardsSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "nonCashSectionFinished" should {
    "return nonCash yes no page" in {
      model.copy(nonCashQuestion = None).nonCashSectionFinished(taxYear, employmentId) shouldBe
        Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }

    "return nonCash amount page" in {
      model.copy(nonCash = None).nonCashSectionFinished(taxYear, employmentId) shouldBe
        Some(NonCashBenefitsAmountController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      model.copy(nonCashQuestion = Some(false)).nonCashSectionFinished(taxYear, "employmentId") shouldBe None
      model.nonCashSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "otherItemsSectionFinished" should {
    "return otherItems yes no page" in {
      model.copy(otherItemsQuestion = None).otherItemsSectionFinished(taxYear, employmentId) shouldBe
        Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }

    "return otherItems amount page" in {
      model.copy(otherItems = None).otherItemsSectionFinished(taxYear, employmentId) shouldBe
        Some(OtherBenefitsAmountController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      model.copy(otherItemsQuestion = Some(false)).otherItemsSectionFinished(taxYear, "employmentId") shouldBe None
      model.otherItemsSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "clear" should {
    "clear the model" in {
      ReimbursedCostsVouchersAndNonCashModel.clear shouldBe ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))
    }
  }
}
