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

import controllers.employment.routes._
import controllers.benefits.income.routes._
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.EncryptedValue

case class IncomeTaxAndCostsModel(incomeTaxOrCostsQuestion: Option[Boolean] = None,
                                  incomeTaxPaidByDirectorQuestion: Option[Boolean] = None,
                                  incomeTaxPaidByDirector: Option[BigDecimal] = None,
                                  paymentsOnEmployeesBehalfQuestion: Option[Boolean] = None,
                                  paymentsOnEmployeesBehalf: Option[BigDecimal] = None) {

  //scalastyle:off
  def incomeTaxPaidByDirectorSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    incomeTaxPaidByDirectorQuestion match {
      case Some(true) => if (incomeTaxPaidByDirector.isDefined) None else Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO Income tax paid by director amount page (Amount of Income Tax paid by employer)
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO Income tax paid by director yes/no page
    }
  }

  def paymentsOnEmployeesBehalfSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    paymentsOnEmployeesBehalfQuestion match {
      case Some(true) => if (paymentsOnEmployeesBehalf.isDefined) None else Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO Payments on employees behalf amount page (Amount of incurred costs paid by employer)
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO Payments on employees behalf yes/no page (Incurred costs paid by employer)
    }
  }
  //scalastyle:on

  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    incomeTaxOrCostsQuestion match {
      case Some(true) => (incomeTaxPaidByDirectorSectionFinished, paymentsOnEmployeesBehalfSectionFinished) match {
        case (call@Some(_), _) => call
        case (_, call@Some(_)) => call
        case _ => None
      }
      case Some(false) => None
      case None => Some(IncomeTaxOrIncurredCostsBenefitsController.show(taxYear, employmentId))
    }
  }
}

object IncomeTaxAndCostsModel {
  implicit val formats: OFormat[IncomeTaxAndCostsModel] = Json.format[IncomeTaxAndCostsModel]

  def clear: IncomeTaxAndCostsModel = IncomeTaxAndCostsModel(incomeTaxOrCostsQuestion = Some(false))
}

case class EncryptedIncomeTaxAndCostsModel(incomeTaxOrCostsQuestion: Option[EncryptedValue] = None,
                                           incomeTaxPaidByDirectorQuestion: Option[EncryptedValue] = None,
                                           incomeTaxPaidByDirector: Option[EncryptedValue] = None,
                                           paymentsOnEmployeesBehalfQuestion: Option[EncryptedValue] = None,
                                           paymentsOnEmployeesBehalf: Option[EncryptedValue] = None)

object EncryptedIncomeTaxAndCostsModel {
  implicit val formats: OFormat[EncryptedIncomeTaxAndCostsModel] = Json.format[EncryptedIncomeTaxAndCostsModel]
}
