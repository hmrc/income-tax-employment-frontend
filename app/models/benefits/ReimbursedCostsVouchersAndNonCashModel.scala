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

package models.benefits

import controllers.benefits.reimbursed.routes._
import models.mongo.TextAndKey
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class ReimbursedCostsVouchersAndNonCashModel(sectionQuestion: Option[Boolean] = None,
                                                  expensesQuestion: Option[Boolean] = None,
                                                  expenses: Option[BigDecimal] = None,
                                                  taxableExpensesQuestion: Option[Boolean] = None,
                                                  taxableExpenses: Option[BigDecimal] = None,
                                                  vouchersAndCreditCardsQuestion: Option[Boolean] = None,
                                                  vouchersAndCreditCards: Option[BigDecimal] = None,
                                                  nonCashQuestion: Option[Boolean] = None,
                                                  nonCash: Option[BigDecimal] = None,
                                                  otherItemsQuestion: Option[Boolean] = None,
                                                  otherItems: Option[BigDecimal] = None) {

  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    sectionQuestion match {
      case Some(true) =>
        (expensesSectionFinished, taxableExpensesSectionFinished, vouchersAndCreditCardsSectionFinished,
          nonCashSectionFinished, otherItemsSectionFinished) match {
          case (call@Some(_), _, _, _, _) => call
          case (_, call@Some(_), _, _, _) => call
          case (_, _, call@Some(_), _, _) => call
          case (_, _, _, call@Some(_), _) => call
          case (_, _, _, _, call@Some(_)) => call
          case _ => None
        }
      case Some(false) => None
      case None => Some(ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYear, employmentId))
    }
  }

  def expensesSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    expensesQuestion match {
      case Some(true) => if (expenses.isDefined) None else Some(NonTaxableCostsBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(NonTaxableCostsBenefitsController.show(taxYear, employmentId))
    }
  }

  def taxableExpensesSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    taxableExpensesQuestion match {
      case Some(true) => if (taxableExpenses.isDefined) None else Some(TaxableCostsBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(TaxableCostsBenefitsController.show(taxYear, employmentId))
    }
  }

  def vouchersAndCreditCardsSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    vouchersAndCreditCardsQuestion match {
      case Some(true) => if (vouchersAndCreditCards.isDefined) None else Some(
        VouchersBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(VouchersBenefitsController.show(taxYear, employmentId))
    }
  }

  def nonCashSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    nonCashQuestion match {
      case Some(true) => if (nonCash.isDefined) None else Some(NonCashBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(NonCashBenefitsController.show(taxYear, employmentId))
    }
  }

  def otherItemsSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    otherItemsQuestion match {
      case Some(true) => if (otherItems.isDefined) None else Some(OtherBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(OtherBenefitsController.show(taxYear, employmentId))
    }
  }

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher,
                  textAndKey: TextAndKey): EncryptedReimbursedCostsVouchersAndNonCashModel = EncryptedReimbursedCostsVouchersAndNonCashModel(
    sectionQuestion = sectionQuestion.map(_.encrypted),
    expensesQuestion = expensesQuestion.map(_.encrypted),
    expenses = expenses.map(_.encrypted),
    taxableExpensesQuestion = taxableExpensesQuestion.map(_.encrypted),
    taxableExpenses = taxableExpenses.map(_.encrypted),
    vouchersAndCreditCardsQuestion = vouchersAndCreditCardsQuestion.map(_.encrypted),
    vouchersAndCreditCards = vouchersAndCreditCards.map(_.encrypted),
    nonCashQuestion = nonCashQuestion.map(_.encrypted),
    nonCash = nonCash.map(_.encrypted),
    otherItemsQuestion = otherItemsQuestion.map(_.encrypted),
    otherItems = otherItems.map(_.encrypted)
  )
}

object ReimbursedCostsVouchersAndNonCashModel {
  implicit val formats: OFormat[ReimbursedCostsVouchersAndNonCashModel] = Json.format[ReimbursedCostsVouchersAndNonCashModel]

  def clear: ReimbursedCostsVouchersAndNonCashModel = ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))
}

case class EncryptedReimbursedCostsVouchersAndNonCashModel(sectionQuestion: Option[EncryptedValue] = None,
                                                           expensesQuestion: Option[EncryptedValue] = None,
                                                           expenses: Option[EncryptedValue] = None,
                                                           taxableExpensesQuestion: Option[EncryptedValue] = None,
                                                           taxableExpenses: Option[EncryptedValue] = None,
                                                           vouchersAndCreditCardsQuestion: Option[EncryptedValue] = None,
                                                           vouchersAndCreditCards: Option[EncryptedValue] = None,
                                                           nonCashQuestion: Option[EncryptedValue] = None,
                                                           nonCash: Option[EncryptedValue] = None,
                                                           otherItemsQuestion: Option[EncryptedValue] = None,
                                                           otherItems: Option[EncryptedValue] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher,
                  textAndKey: TextAndKey): ReimbursedCostsVouchersAndNonCashModel = ReimbursedCostsVouchersAndNonCashModel(
    sectionQuestion = sectionQuestion.map(_.decrypted[Boolean]),
    expensesQuestion = expensesQuestion.map(_.decrypted[Boolean]),
    expenses = expenses.map(_.decrypted[BigDecimal]),
    taxableExpensesQuestion = taxableExpensesQuestion.map(_.decrypted[Boolean]),
    taxableExpenses = taxableExpenses.map(_.decrypted[BigDecimal]),
    vouchersAndCreditCardsQuestion = vouchersAndCreditCardsQuestion.map(_.decrypted[Boolean]),
    vouchersAndCreditCards = vouchersAndCreditCards.map(_.decrypted[BigDecimal]),
    nonCashQuestion = nonCashQuestion.map(_.decrypted[Boolean]),
    nonCash = nonCash.map(_.decrypted[BigDecimal]),
    otherItemsQuestion = otherItemsQuestion.map(_.decrypted[Boolean]),
    otherItems = otherItems.map(_.decrypted[BigDecimal])
  )
}

object EncryptedReimbursedCostsVouchersAndNonCashModel {
  implicit val formats: OFormat[EncryptedReimbursedCostsVouchersAndNonCashModel] = Json.format[EncryptedReimbursedCostsVouchersAndNonCashModel]
}
