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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.EncryptedValue
import controllers.benefits.routes._
import controllers.employment.routes._

case class UtilitiesAndServicesModel(utilitiesAndServicesQuestion: Option[Boolean] = None,
                                     telephoneQuestion: Option[Boolean] = None,
                                     telephone: Option[BigDecimal] = None,
                                     employerProvidedServicesQuestion: Option[Boolean] = None,
                                     employerProvidedServices: Option[BigDecimal] = None,
                                     employerProvidedProfessionalSubscriptionsQuestion: Option[Boolean] = None,
                                     employerProvidedProfessionalSubscriptions: Option[BigDecimal] = None,
                                     serviceQuestion: Option[Boolean] = None,
                                     service: Option[BigDecimal] = None){

  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={

    utilitiesAndServicesQuestion match {
      case Some(true) =>
        (telephoneSectionFinished,employerProvidedServicesSectionFinished,
          employerProvidedProfessionalSubscriptionsSectionFinished, serviceSectionFinished) match {
          case (call@Some(_), _, _, _) => call
          case (_, call@Some(_), _, _) => call
          case (_, _, call@Some(_), _) => call
          case (_, _, _, call@Some(_)) => call
          case _ => None
        }
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }
  }

  //scalastyle:off
  def telephoneSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={
    telephoneQuestion match {
      case Some(true) => if(telephone.isDefined) None else Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO telephone amount page
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO telephone yes no page
    }
  }

  def employerProvidedServicesSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] ={
    employerProvidedServicesQuestion match {
      case Some(true) => if(employerProvidedServices.isDefined) None else Some(CheckYourBenefitsController.show(taxYear, employmentId)) // TODO employerProvidedServices amount page
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO employerProvidedServices yes no page
    }
  }

  def employerProvidedProfessionalSubscriptionsSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    employerProvidedProfessionalSubscriptionsQuestion match {
      case Some(true) => if(employerProvidedProfessionalSubscriptions.isDefined) None else Some(CheckYourBenefitsController.show(taxYear,employmentId)) // TODO employerProvidedProfessionalSubscriptions amount page
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO employerProvidedProfessionalSubscriptions yes no page
    }
  }

  def serviceSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    serviceQuestion match {
      case Some(true) => if(service.isDefined) None else Some(CheckYourBenefitsController.show(taxYear,employmentId)) // TODO service amount page
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO service yes no page
    }
  }
  //scalastyle:on

}

object UtilitiesAndServicesModel{
  implicit val formats: OFormat[UtilitiesAndServicesModel] = Json.format[UtilitiesAndServicesModel]

  def clear: UtilitiesAndServicesModel = UtilitiesAndServicesModel(utilitiesAndServicesQuestion = Some(false))
}


case class EncryptedUtilitiesAndServicesModel(utilitiesAndServicesQuestion: Option[EncryptedValue] = None,
                                              telephoneQuestion: Option[EncryptedValue] = None,
                                              telephone: Option[EncryptedValue] = None,
                                              employerProvidedServicesQuestion: Option[EncryptedValue] = None,
                                              employerProvidedServices: Option[EncryptedValue] = None,
                                              employerProvidedProfessionalSubscriptionsQuestion: Option[EncryptedValue] = None,
                                              employerProvidedProfessionalSubscriptions: Option[EncryptedValue] = None,
                                              serviceQuestion: Option[EncryptedValue] = None,
                                              service: Option[EncryptedValue] = None)

object EncryptedUtilitiesAndServicesModel{
  implicit val formats: OFormat[EncryptedUtilitiesAndServicesModel] = Json.format[EncryptedUtilitiesAndServicesModel]
}