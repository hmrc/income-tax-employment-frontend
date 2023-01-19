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

import controllers.benefits.medical.routes._
import controllers.employment.routes._
import models.mongo.TextAndKey
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class MedicalChildcareEducationModel(sectionQuestion: Option[Boolean] = None,
                                          medicalInsuranceQuestion: Option[Boolean] = None,
                                          medicalInsurance: Option[BigDecimal] = None,
                                          nurseryPlacesQuestion: Option[Boolean] = None,
                                          nurseryPlaces: Option[BigDecimal] = None,
                                          educationalServicesQuestion: Option[Boolean] = None,
                                          educationalServices: Option[BigDecimal] = None,
                                          beneficialLoanQuestion: Option[Boolean] = None,
                                          beneficialLoan: Option[BigDecimal] = None) {

  def medicalInsuranceSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    medicalInsuranceQuestion match {
      case Some(true) => if (medicalInsurance.isDefined) None else Some(MedicalOrDentalBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(MedicalDentalBenefitsController.show(taxYear, employmentId))
    }
  }

  def childcareSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    nurseryPlacesQuestion match {
      case Some(true) => if (nurseryPlaces.isDefined) None else Some(ChildcareBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(ChildcareBenefitsController.show(taxYear, employmentId))
    }
  }

  def educationalServicesSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    educationalServicesQuestion match {
      case Some(true) => if (educationalServices.isDefined) None else Some(EducationalServicesBenefitsAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO Educational services yes/no page
    }
  }

  def beneficialLoanSectionFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    beneficialLoanQuestion match {
      case Some(true) => if (beneficialLoan.isDefined) None else Some(BeneficialLoansAmountController.show(taxYear, employmentId))
      case Some(false) => None
      case None => Some(BeneficialLoansBenefitsController.show(taxYear, employmentId))
    }
  }

  def isFinished(implicit taxYear: Int, employmentId: String): Option[Call] = {
    sectionQuestion match {
      case Some(true) => (medicalInsuranceSectionFinished, childcareSectionFinished, educationalServicesSectionFinished, beneficialLoanSectionFinished) match {
        case (call@Some(_), _, _, _) => call
        case (_, call@Some(_), _, _) => call
        case (_, _, call@Some(_), _) => call
        case (_, _, _, call@Some(_)) => call
        case _ => None
      }
      case Some(false) => None
      case None => Some(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO Medical childcare education section yes/no page
    }
  }

  def encrypted(implicit secureGCMCipher: SecureGCMCipher,
                textAndKey: TextAndKey): EncryptedMedicalChildcareEducationModel = EncryptedMedicalChildcareEducationModel(
    sectionQuestion = sectionQuestion.map(_.encrypted),
    medicalInsuranceQuestion = medicalInsuranceQuestion.map(_.encrypted),
    medicalInsurance = medicalInsurance.map(_.encrypted),
    nurseryPlacesQuestion = nurseryPlacesQuestion.map(_.encrypted),
    nurseryPlaces = nurseryPlaces.map(_.encrypted),
    educationalServicesQuestion = educationalServicesQuestion.map(_.encrypted),
    educationalServices = educationalServices.map(_.encrypted),
    beneficialLoanQuestion = beneficialLoanQuestion.map(_.encrypted),
    beneficialLoan = beneficialLoan.map(_.encrypted)
  )
}

object MedicalChildcareEducationModel {
  implicit val formats: OFormat[MedicalChildcareEducationModel] = Json.format[MedicalChildcareEducationModel]

  def clear: MedicalChildcareEducationModel = MedicalChildcareEducationModel(sectionQuestion = Some(false))
}

case class EncryptedMedicalChildcareEducationModel(sectionQuestion: Option[EncryptedValue] = None,
                                                   medicalInsuranceQuestion: Option[EncryptedValue] = None,
                                                   medicalInsurance: Option[EncryptedValue] = None,
                                                   nurseryPlacesQuestion: Option[EncryptedValue] = None,
                                                   nurseryPlaces: Option[EncryptedValue] = None,
                                                   educationalServicesQuestion: Option[EncryptedValue] = None,
                                                   educationalServices: Option[EncryptedValue] = None,
                                                   beneficialLoanQuestion: Option[EncryptedValue] = None,
                                                   beneficialLoan: Option[EncryptedValue] = None) {

  def decrypted(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): MedicalChildcareEducationModel = MedicalChildcareEducationModel(
    sectionQuestion = sectionQuestion.map(_.decrypted[Boolean]),
    medicalInsuranceQuestion = medicalInsuranceQuestion.map(_.decrypted[Boolean]),
    medicalInsurance = medicalInsurance.map(_.decrypted[BigDecimal]),
    nurseryPlacesQuestion = nurseryPlacesQuestion.map(_.decrypted[Boolean]),
    nurseryPlaces = nurseryPlaces.map(_.decrypted[BigDecimal]),
    educationalServicesQuestion = educationalServicesQuestion.map(_.decrypted[Boolean]),
    educationalServices = educationalServices.map(_.decrypted[BigDecimal]),
    beneficialLoanQuestion = beneficialLoanQuestion.map(_.decrypted[Boolean]),
    beneficialLoan = beneficialLoan.map(_.decrypted[BigDecimal])
  )
}

object EncryptedMedicalChildcareEducationModel {
  implicit val formats: OFormat[EncryptedMedicalChildcareEducationModel] = Json.format[EncryptedMedicalChildcareEducationModel]
}
