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

import controllers.benefits.utilities.routes._
import controllers.employment.routes._
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.EncryptedValue

case class UtilitiesAndServicesModel(sectionQuestion: Option[Boolean] = None,
                                     telephoneQuestion: Option[Boolean] = None,
                                     telephone: Option[BigDecimal] = None,
                                     employerProvidedServicesQuestion: Option[Boolean] = None,
                                     employerProvidedServices: Option[BigDecimal] = None,
                                     employerProvidedProfessionalSubscriptionsQuestion: Option[Boolean] = None,
                                     employerProvidedProfessionalSubscriptions: Option[BigDecimal] = None,
                                     serviceQuestion: Option[Boolean] = None,
                                     service: Option[BigDecimal] = None) {

  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    sectionQuestion match {
      case Some(true) =>
        (telephoneSectionFinished, employerProvidedServicesSectionFinished,
          employerProvidedProfessionalSubscriptionsSectionFinished, serviceSectionFinished) match {
          case (call@Some(_), _, _, _) => call
          case (_, call@Some(_), _, _) => call
          case (_, _, call@Some(_), _) => call
          case (_, _, _, call@Some(_)) => call
          case _ => None
        }
      case Some(false) => None
      case None => Some(UtilitiesOrGeneralServicesBenefitsController.show(taxYear, employmentId))
    }
  }

  //scalastyle:off
  def telephoneSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    telephoneQuestion match {
      case Some(true) => if (telephone.isDefined) None else Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO telephone amount page
      case Some(false) => None
      case None => Some(TelephoneBenefitsController.show(taxYear, employmentId))
    }
  }

  def employerProvidedServicesSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    employerProvidedServicesQuestion match {
      case Some(true) => if (employerProvidedServices.isDefined) None else Some(EmployerProvidedServicesBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(EmployerProvidedServicesBenefitsController.show(taxYear, employmentId))
    }
  }

  def employerProvidedProfessionalSubscriptionsSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    employerProvidedProfessionalSubscriptionsQuestion match {
      case Some(true) => if (employerProvidedProfessionalSubscriptions.isDefined) None else Some(ProfessionalSubscriptionsBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(ProfessionalSubscriptionsBenefitsController.show(taxYear, employmentId))
    }
  }

  def serviceSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    serviceQuestion match {
      case Some(true) => if (service.isDefined) None else Some(CheckYourBenefitsController.show(taxYear, employmentId)) // TODO service amount page
      case Some(false) => None
      case None => Some(OtherServicesBenefitsController.show(taxYear, employmentId))
    }
  }
  //scalastyle:on
}

object UtilitiesAndServicesModel {
  implicit val formats: OFormat[UtilitiesAndServicesModel] = Json.format[UtilitiesAndServicesModel]

  def clear: UtilitiesAndServicesModel = UtilitiesAndServicesModel(sectionQuestion = Some(false))
}


case class EncryptedUtilitiesAndServicesModel(sectionQuestion: Option[EncryptedValue] = None,
                                              telephoneQuestion: Option[EncryptedValue] = None,
                                              telephone: Option[EncryptedValue] = None,
                                              employerProvidedServicesQuestion: Option[EncryptedValue] = None,
                                              employerProvidedServices: Option[EncryptedValue] = None,
                                              employerProvidedProfessionalSubscriptionsQuestion: Option[EncryptedValue] = None,
                                              employerProvidedProfessionalSubscriptions: Option[EncryptedValue] = None,
                                              serviceQuestion: Option[EncryptedValue] = None,
                                              service: Option[EncryptedValue] = None)

object EncryptedUtilitiesAndServicesModel {
  implicit val formats: OFormat[EncryptedUtilitiesAndServicesModel] = Json.format[EncryptedUtilitiesAndServicesModel]
}
