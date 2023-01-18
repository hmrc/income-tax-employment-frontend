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

import controllers.benefits.income.routes._
import models.mongo.TextAndKey
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class IncomeTaxAndCostsModel(sectionQuestion: Option[Boolean] = None,
                                  incomeTaxPaidByDirectorQuestion: Option[Boolean] = None,
                                  incomeTaxPaidByDirector: Option[BigDecimal] = None,
                                  paymentsOnEmployeesBehalfQuestion: Option[Boolean] = None,
                                  paymentsOnEmployeesBehalf: Option[BigDecimal] = None) {

  def incomeTaxPaidByDirectorSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    incomeTaxPaidByDirectorQuestion match {
      case Some(true) => if (incomeTaxPaidByDirector.isDefined) None else Some(IncomeTaxBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(IncomeTaxBenefitsController.show(taxYear, employmentId))
    }
  }

  def paymentsOnEmployeesBehalfSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    paymentsOnEmployeesBehalfQuestion match {
      case Some(true) => if (paymentsOnEmployeesBehalf.isDefined) None else Some(IncurredCostsBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(IncurredCostsBenefitsController.show(taxYear, employmentId))
    }
  }

  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    sectionQuestion match {
      case Some(true) => (incomeTaxPaidByDirectorSectionFinished, paymentsOnEmployeesBehalfSectionFinished) match {
        case (call@Some(_), _) => call
        case (_, call@Some(_)) => call
        case _ => None
      }
      case Some(false) => None
      case None => Some(IncomeTaxOrIncurredCostsBenefitsController.show(taxYear, employmentId))
    }
  }

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedIncomeTaxAndCostsModel = EncryptedIncomeTaxAndCostsModel(
    sectionQuestion = sectionQuestion.map(_.encrypted),
    incomeTaxPaidByDirectorQuestion = incomeTaxPaidByDirectorQuestion.map(_.encrypted),
    incomeTaxPaidByDirector = incomeTaxPaidByDirector.map(_.encrypted),
    paymentsOnEmployeesBehalfQuestion = paymentsOnEmployeesBehalfQuestion.map(_.encrypted),
    paymentsOnEmployeesBehalf = paymentsOnEmployeesBehalf.map(_.encrypted)
  )
}

object IncomeTaxAndCostsModel {
  implicit val formats: OFormat[IncomeTaxAndCostsModel] = Json.format[IncomeTaxAndCostsModel]

  def clear: IncomeTaxAndCostsModel = IncomeTaxAndCostsModel(sectionQuestion = Some(false))
}

case class EncryptedIncomeTaxAndCostsModel(sectionQuestion: Option[EncryptedValue] = None,
                                           incomeTaxPaidByDirectorQuestion: Option[EncryptedValue] = None,
                                           incomeTaxPaidByDirector: Option[EncryptedValue] = None,
                                           paymentsOnEmployeesBehalfQuestion: Option[EncryptedValue] = None,
                                           paymentsOnEmployeesBehalf: Option[EncryptedValue] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): IncomeTaxAndCostsModel = IncomeTaxAndCostsModel(
    sectionQuestion = sectionQuestion.map(_.decrypted[Boolean]),
    incomeTaxPaidByDirectorQuestion = incomeTaxPaidByDirectorQuestion.map(_.decrypted[Boolean]),
    incomeTaxPaidByDirector = incomeTaxPaidByDirector.map(_.decrypted[BigDecimal]),
    paymentsOnEmployeesBehalfQuestion = paymentsOnEmployeesBehalfQuestion.map(_.decrypted[Boolean]),
    paymentsOnEmployeesBehalf = paymentsOnEmployeesBehalf.map(_.decrypted[BigDecimal])
  )
}

object EncryptedIncomeTaxAndCostsModel {
  implicit val formats: OFormat[EncryptedIncomeTaxAndCostsModel] = Json.format[EncryptedIncomeTaxAndCostsModel]
}
