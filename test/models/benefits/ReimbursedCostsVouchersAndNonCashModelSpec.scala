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

package models.benefits

import controllers.benefits.reimbursed.routes._
import controllers.employment.routes.CheckYourBenefitsController
import org.scalamock.scalatest.MockFactory
import support.TaxYearUtils.taxYear
import support.UnitTest
import support.builders.models.benefits.ReimbursedCostsVouchersAndNonCashModelBuilder.aReimbursedCostsVouchersAndNonCashModel
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

class ReimbursedCostsVouchersAndNonCashModelSpec extends UnitTest
  with MockFactory {

  private val employmentId = "employmentId"

  private implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val encryptedSectionQuestion = EncryptedValue("encryptedSectionQuestion", "some-nonce")
  private val encryptedExpensesQuestion = EncryptedValue("encryptedExpensesQuestion", "some-nonce")
  private val encryptedExpenses = EncryptedValue("encryptedExpenses", "some-nonce")
  private val encryptedTaxableExpensesQuestion = EncryptedValue("encryptedTaxableExpensesQuestion", "some-nonce")
  private val encryptedTaxableExpenses = EncryptedValue("encryptedTaxableExpenses", "some-nonce")
  private val encryptedVouchersAndCreditCardsQuestion = EncryptedValue("encryptedVouchersAndCreditCardsQuestion", "some-nonce")
  private val encryptedVouchersAndCreditCards = EncryptedValue("encryptedVouchersAndCreditCards", "some-nonce")
  private val encryptedNonCashQuestion = EncryptedValue("encryptedNonCashQuestion", "some-nonce")
  private val encryptedNonCash = EncryptedValue("encryptedNonCash", "some-nonce")
  private val encryptedOtherItemsQuestion = EncryptedValue("encryptedOtherItemsQuestion", "some-nonce")
  private val encryptedOtherItems = EncryptedValue("encryptedOtherItems", "some-nonce")

  "ReimbursedCostsVouchersAndNonCashModel.isFinished" should {
    "return reimbursedCostsVouchersAndNonCash yes no page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(sectionQuestion = None).isFinished(taxYear, employmentId) shouldBe
        Some(ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYear, employmentId))
    }
    "return none when section is finished" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(sectionQuestion = Some(false)).isFinished(taxYear, "employmentId") shouldBe None
      aReimbursedCostsVouchersAndNonCashModel.isFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "ReimbursedCostsVouchersAndNonCashModel.expensesSectionFinished" should {
    "return expenses yes no page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = None).expensesSectionFinished(taxYear, employmentId) shouldBe
        Some(controllers.benefits.reimbursed.routes.NonTaxableCostsBenefitsController.show(taxYear, employmentId))
    }

    "return expenses amount page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(expenses = None).expensesSectionFinished(taxYear, employmentId) shouldBe
        Some(controllers.benefits.reimbursed.routes.NonTaxableCostsBenefitsAmountController.show(taxYear, employmentId))
      Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(false)).expensesSectionFinished(taxYear, "employmentId") shouldBe None
      aReimbursedCostsVouchersAndNonCashModel.expensesSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "ReimbursedCostsVouchersAndNonCashModel.taxableExpensesSectionFinished" should {
    "return taxableExpenses yes no page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = None).taxableExpensesSectionFinished(taxYear, employmentId) shouldBe
        Some(TaxableCostsBenefitsController.show(taxYear, employmentId))
    }

    "return taxableExpenses amount page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpenses = None).taxableExpensesSectionFinished(taxYear, employmentId) shouldBe
        Some(TaxableCostsBenefitsAmountController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = Some(false)).taxableExpensesSectionFinished(taxYear, "employmentId") shouldBe None
      aReimbursedCostsVouchersAndNonCashModel.taxableExpensesSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "ReimbursedCostsVouchersAndNonCashModel.vouchersAndCreditCardsSectionFinished" should {
    "return vouchersAndCreditCards yes no page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = None).vouchersAndCreditCardsSectionFinished(taxYear, employmentId) shouldBe
        Some(VouchersBenefitsController.show(taxYear, employmentId))
    }

    "return vouchersAndCreditCards amount page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCards = None).vouchersAndCreditCardsSectionFinished(taxYear, employmentId) shouldBe
        Some(VouchersBenefitsAmountController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = Some(false)).vouchersAndCreditCardsSectionFinished(taxYear, "employmentId") shouldBe None
      aReimbursedCostsVouchersAndNonCashModel.vouchersAndCreditCardsSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "ReimbursedCostsVouchersAndNonCashModel.nonCashSectionFinished" should {
    "return nonCash yes no page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = None).nonCashSectionFinished(taxYear, employmentId) shouldBe
        Some(NonCashBenefitsController.show(taxYear, employmentId))
    }

    "return nonCash amount page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(nonCash = None).nonCashSectionFinished(taxYear, employmentId) shouldBe
        Some(NonCashBenefitsAmountController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(false)).nonCashSectionFinished(taxYear, "employmentId") shouldBe None
      aReimbursedCostsVouchersAndNonCashModel.nonCashSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "ReimbursedCostsVouchersAndNonCashModel.otherItemsSectionFinished" should {
    "return otherItems yes no page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = None).otherItemsSectionFinished(taxYear, employmentId) shouldBe
        Some(OtherBenefitsController.show(taxYear, employmentId))
    }

    "return otherItems amount page" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(otherItems = None).otherItemsSectionFinished(taxYear, employmentId) shouldBe
        Some(OtherBenefitsAmountController.show(taxYear, employmentId))
    }

    "return none when section is finished" in {
      aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(false)).otherItemsSectionFinished(taxYear, "employmentId") shouldBe None
      aReimbursedCostsVouchersAndNonCashModel.otherItemsSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "ReimbursedCostsVouchersAndNonCashModel.clear" should {
    "clear the model" in {
      ReimbursedCostsVouchersAndNonCashModel.clear shouldBe ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))
    }
  }

  "ReimbursedCostsVouchersAndNonCashModel.encrypted" should {
    "return EncryptedReimbursedCostsVouchersAndNonCashModel instance" in {
      val underTest = aReimbursedCostsVouchersAndNonCashModel

      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.sectionQuestion.get.toString, associatedText).returning(encryptedSectionQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.expensesQuestion.get.toString, associatedText).returning(encryptedExpensesQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.expenses.get.toString(), associatedText).returning(encryptedExpenses)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.taxableExpensesQuestion.get.toString, associatedText).returning(encryptedTaxableExpensesQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.taxableExpenses.get.toString(), associatedText).returning(encryptedTaxableExpenses)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.vouchersAndCreditCardsQuestion.get.toString, associatedText).returning(encryptedVouchersAndCreditCardsQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.vouchersAndCreditCards.get.toString(), associatedText).returning(encryptedVouchersAndCreditCards)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.nonCashQuestion.get.toString, associatedText).returning(encryptedNonCashQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.nonCash.get.toString(), associatedText).returning(encryptedNonCash)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.otherItemsQuestion.get.toString, associatedText).returning(encryptedOtherItemsQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.otherItems.get.toString(), associatedText).returning(encryptedOtherItems)

      underTest.encrypted shouldBe EncryptedReimbursedCostsVouchersAndNonCashModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        expensesQuestion = Some(encryptedExpensesQuestion),
        expenses = Some(encryptedExpenses),
        taxableExpensesQuestion = Some(encryptedTaxableExpensesQuestion),
        taxableExpenses = Some(encryptedTaxableExpenses),
        vouchersAndCreditCardsQuestion = Some(encryptedVouchersAndCreditCardsQuestion),
        vouchersAndCreditCards = Some(encryptedVouchersAndCreditCards),
        nonCashQuestion = Some(encryptedNonCashQuestion),
        nonCash = Some(encryptedNonCash),
        otherItemsQuestion = Some(encryptedOtherItemsQuestion),
        otherItems = Some(encryptedOtherItems)
      )
    }
  }

  "EncryptedReimbursedCostsVouchersAndNonCashModel.decrypted" should {
    "return ReimbursedCostsVouchersAndNonCashModel instance" in {
      val underTest = EncryptedReimbursedCostsVouchersAndNonCashModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        expensesQuestion = Some(encryptedExpensesQuestion),
        expenses = Some(encryptedExpenses),
        taxableExpensesQuestion = Some(encryptedTaxableExpensesQuestion),
        taxableExpenses = Some(encryptedTaxableExpenses),
        vouchersAndCreditCardsQuestion = Some(encryptedVouchersAndCreditCardsQuestion),
        vouchersAndCreditCards = Some(encryptedVouchersAndCreditCards),
        nonCashQuestion = Some(encryptedNonCashQuestion),
        nonCash = Some(encryptedNonCash),
        otherItemsQuestion = Some(encryptedOtherItemsQuestion),
        otherItems = Some(encryptedOtherItems)
      )

      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedSectionQuestion, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.sectionQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedExpensesQuestion, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.expensesQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedExpenses, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.expenses.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedTaxableExpensesQuestion, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.taxableExpensesQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedTaxableExpenses, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.taxableExpenses.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedVouchersAndCreditCardsQuestion, associatedText)
        .returning(value = aReimbursedCostsVouchersAndNonCashModel.vouchersAndCreditCardsQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedVouchersAndCreditCards, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.vouchersAndCreditCards.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedNonCashQuestion, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.nonCashQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedNonCash, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.nonCash.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedOtherItemsQuestion, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.otherItemsQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedOtherItems, associatedText).returning(value = aReimbursedCostsVouchersAndNonCashModel.otherItems.get.toString())

      underTest.decrypted shouldBe aReimbursedCostsVouchersAndNonCashModel
    }
  }
}
