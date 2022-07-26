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
import controllers.employment.routes.CheckYourBenefitsController
import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import play.api.mvc.Call
import support.UnitTest
import support.builders.models.benefits.UtilitiesAndServicesModelBuilder.aUtilitiesAndServicesModel
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher, TaxYearHelper}

class UtilitiesAndServicesModelSpec extends UnitTest
  with TaxYearHelper
  with MockFactory {

  private val employmentId = "employmentId"

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedSectionQuestion = EncryptedValue("encryptedSectionQuestion", "some-nonce")
  private val encryptedTelephoneQuestion = EncryptedValue("encryptedTelephoneQuestion", "some-nonce")
  private val encryptedTelephone = EncryptedValue("encryptedTelephone", "some-nonce")
  private val encryptedEmployerProvidedServicesQuestion = EncryptedValue("encryptedEmployerProvidedServicesQuestion", "some-nonce")
  private val encryptedEmployerProvidedServices = EncryptedValue("encryptedEmployerProvidedServices", "some-nonce")
  private val encryptedEmployerProvidedProfessionalSubscriptionsQuestion = EncryptedValue("encryptedEmployerProvidedProfessionalSubscriptionsQuestion", "some-nonce")
  private val encryptedEmployerProvidedProfessionalSubscriptions = EncryptedValue("encryptedEmployerProvidedProfessionalSubscriptions", "some-nonce")
  private val encryptedServiceQuestion = EncryptedValue("encryptedServiceQuestion", "some-nonce")
  private val encryptedService = EncryptedValue("encryptedService", "some-nonce")

  private def result(url: String): Option[Call] = Some(Call("GET", url))

  private val underTest = aUtilitiesAndServicesModel

  "UtilitiesAndServicesModel.isFinished" should {
    "return utilities and services yes no page" in {
      underTest.copy(sectionQuestion = None).isFinished(taxYear, employmentId) shouldBe
        result(UtilitiesOrGeneralServicesBenefitsController.show(taxYear, employmentId).url)
    }

    "return none when section is finished" in {
      underTest.copy(sectionQuestion = Some(false)).isFinished(taxYear, employmentId) shouldBe None
      underTest.isFinished(taxYear, employmentId) shouldBe None
    }
  }

  "UtilitiesAndServicesModel.telephoneSectionFinished" should {
    "return telephone yes no page" in {
      underTest.copy(telephoneQuestion = None).telephoneSectionFinished(taxYear, employmentId) shouldBe
        result(TelephoneBenefitsController.show(taxYear, employmentId).url)
    }

    "return telephone amount page" in {
      underTest.copy(telephone = None).telephoneSectionFinished(taxYear, employmentId) shouldBe result(CheckYourBenefitsController.show(taxYear, employmentId).url)
    }

    "return none when section is finished" in {
      underTest.copy(telephoneQuestion = Some(false)).telephoneSectionFinished(taxYear, employmentId) shouldBe None
      underTest.telephoneSectionFinished(taxYear, employmentId) shouldBe None
    }
  }

  "UtilitiesAndServicesModel.employerProvidedServicesSectionFinished" should {
    "return employerProvidedServices yes no page" in {
      underTest.copy(employerProvidedServicesQuestion = None).employerProvidedServicesSectionFinished(taxYear, employmentId) shouldBe
        result(EmployerProvidedServicesBenefitsController.show(taxYear, employmentId).url)
    }

    "return employerProvidedServices amount page" in {
      underTest.copy(employerProvidedServices = None).employerProvidedServicesSectionFinished(taxYear, employmentId) shouldBe
        result(EmployerProvidedServicesBenefitsAmountController.show(taxYear, employmentId).url)
    }

    "return none when section is finished" in {
      underTest.copy(employerProvidedServicesQuestion = Some(false)).employerProvidedServicesSectionFinished(taxYear, employmentId) shouldBe None
      underTest.employerProvidedServicesSectionFinished(taxYear, employmentId) shouldBe None
    }
  }

  "UtilitiesAndServicesModel.employerProvidedProfessionalSubscriptionsSectionFinished" should {
    "return employerProvidedProfessionalSubscriptions yes no page" in {
      underTest.copy(employerProvidedProfessionalSubscriptionsQuestion = None).employerProvidedProfessionalSubscriptionsSectionFinished(taxYear, employmentId) shouldBe
        result(ProfessionalSubscriptionsBenefitsController.show(taxYear, employmentId).url)
    }

    "return employerProvidedProfessionalSubscriptions amount page" in {
      underTest.copy(employerProvidedProfessionalSubscriptions = None).employerProvidedProfessionalSubscriptionsSectionFinished(taxYear, employmentId) shouldBe
        result(ProfessionalSubscriptionsBenefitsAmountController.show(taxYear, employmentId).url)
    }

    "return none when section is finished" in {
      underTest.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false)).employerProvidedProfessionalSubscriptionsSectionFinished(taxYear, employmentId) shouldBe None
      underTest.employerProvidedProfessionalSubscriptionsSectionFinished(taxYear, employmentId) shouldBe None
    }
  }

  "UtilitiesAndServicesModel.serviceSectionFinished" should {
    "return service yes no page" in {
      underTest.copy(serviceQuestion = None).serviceSectionFinished(taxYear, employmentId) shouldBe
        result(OtherServicesBenefitsController.show(taxYear, employmentId).url)
    }

    "return service amount page" in {
      underTest.copy(service = None).serviceSectionFinished(taxYear, employmentId) shouldBe
        result(CheckYourBenefitsController.show(taxYear, employmentId).url)
    }

    "return none when section is finished" in {
      underTest.copy(serviceQuestion = Some(false)).serviceSectionFinished(taxYear, employmentId) shouldBe None
      underTest.serviceSectionFinished(taxYear, employmentId) shouldBe None
    }
  }

  "UtilitiesAndServicesModel.clear" should {
    "clear the model" in {
      UtilitiesAndServicesModel.clear shouldBe UtilitiesAndServicesModel(sectionQuestion = Some(false))
    }
  }

  "UtilitiesAndServicesModel.encrypted" should {
    "return EncryptedUtilitiesAndServicesModel instance" in {
      val underTest = aUtilitiesAndServicesModel

      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.sectionQuestion.get, textAndKey).returning(encryptedSectionQuestion)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.telephoneQuestion.get, textAndKey).returning(encryptedTelephoneQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.telephone.get, textAndKey).returning(encryptedTelephone)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.employerProvidedServicesQuestion.get, textAndKey).returning(encryptedEmployerProvidedServicesQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.employerProvidedServices.get, textAndKey).returning(encryptedEmployerProvidedServices)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.employerProvidedProfessionalSubscriptionsQuestion.get, textAndKey)
        .returning(encryptedEmployerProvidedProfessionalSubscriptionsQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.employerProvidedProfessionalSubscriptions.get, textAndKey).returning(encryptedEmployerProvidedProfessionalSubscriptions)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.serviceQuestion.get, textAndKey).returning(encryptedServiceQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.service.get, textAndKey).returning(encryptedService)

      underTest.encrypted shouldBe EncryptedUtilitiesAndServicesModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        telephoneQuestion = Some(encryptedTelephoneQuestion),
        telephone = Some(encryptedTelephone),
        employerProvidedServicesQuestion = Some(encryptedEmployerProvidedServicesQuestion),
        employerProvidedServices = Some(encryptedEmployerProvidedServices),
        employerProvidedProfessionalSubscriptionsQuestion = Some(encryptedEmployerProvidedProfessionalSubscriptionsQuestion),
        employerProvidedProfessionalSubscriptions = Some(encryptedEmployerProvidedProfessionalSubscriptions),
        serviceQuestion = Some(encryptedServiceQuestion),
        service = Some(encryptedService)
      )
    }
  }

  "EncryptedUtilitiesAndServicesModel.decrypted" should {
    "return UtilitiesAndServicesModel instance" in {
      val underTest = EncryptedUtilitiesAndServicesModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        telephoneQuestion = Some(encryptedTelephoneQuestion),
        telephone = Some(encryptedTelephone),
        employerProvidedServicesQuestion = Some(encryptedEmployerProvidedServicesQuestion),
        employerProvidedServices = Some(encryptedEmployerProvidedServices),
        employerProvidedProfessionalSubscriptionsQuestion = Some(encryptedEmployerProvidedProfessionalSubscriptionsQuestion),
        employerProvidedProfessionalSubscriptions = Some(encryptedEmployerProvidedProfessionalSubscriptions),
        serviceQuestion = Some(encryptedServiceQuestion),
        service = Some(encryptedService)
      )

      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedSectionQuestion.value, encryptedSectionQuestion.nonce, textAndKey, *).returning(value = aUtilitiesAndServicesModel.sectionQuestion.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedTelephoneQuestion.value, encryptedTelephoneQuestion.nonce, textAndKey, *).returning(value = aUtilitiesAndServicesModel.telephoneQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedTelephone.value, encryptedTelephone.nonce, textAndKey, *).returning(value = aUtilitiesAndServicesModel.telephone.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedEmployerProvidedServicesQuestion.value, encryptedEmployerProvidedServicesQuestion.nonce, textAndKey, *)
        .returning(value = aUtilitiesAndServicesModel.employerProvidedServicesQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedEmployerProvidedServices.value, encryptedEmployerProvidedServices.nonce, textAndKey, *).returning(value = aUtilitiesAndServicesModel.employerProvidedServices.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedEmployerProvidedProfessionalSubscriptionsQuestion.value, encryptedEmployerProvidedProfessionalSubscriptionsQuestion.nonce, textAndKey, *)
        .returning(value = aUtilitiesAndServicesModel.employerProvidedProfessionalSubscriptionsQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedEmployerProvidedProfessionalSubscriptions.value, encryptedEmployerProvidedProfessionalSubscriptions.nonce, textAndKey, *)
        .returning(value = aUtilitiesAndServicesModel.employerProvidedProfessionalSubscriptions.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedServiceQuestion.value, encryptedServiceQuestion.nonce, textAndKey, *).returning(value = aUtilitiesAndServicesModel.serviceQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedService.value, encryptedService.nonce, textAndKey, *).returning(value = aUtilitiesAndServicesModel.service.get)

      underTest.decrypted shouldBe aUtilitiesAndServicesModel
    }
  }
}
