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

import controllers.benefits.medical.routes._
import controllers.employment.routes.CheckYourBenefitsController
import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.benefits.MedicalChildcareEducationModelBuilder.aMedicalChildcareEducationModel
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher, TaxYearHelper}

class MedicalChildcareEducationModelSpec extends UnitTest
  with TaxYearHelper
  with MockFactory {

  private val employmentId = "some-employment-id"

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedSectionQuestion = EncryptedValue("encryptedSectionQuestion", "some-nonce")
  private val encryptedMedicalInsuranceQuestion = EncryptedValue("encryptedMedicalInsuranceQuestion", "some-nonce")
  private val encryptedMedicalInsurance = EncryptedValue("encryptedMedicalInsurance", "some-nonce")
  private val encryptedNurseryPlacesQuestion = EncryptedValue("encryptedNurseryPlacesQuestion", "some-nonce")
  private val encryptedNurseryPlaces = EncryptedValue("encryptedNurseryPlaces", "some-nonce")
  private val encryptedEducationalServicesQuestion = EncryptedValue("encryptedEducationalServicesQuestion", "some-nonce")
  private val encryptedEducationalServices = EncryptedValue("encryptedEducationalServices", "some-nonce")
  private val encryptedBeneficialLoanQuestion = EncryptedValue("encryptedBeneficialLoanQuestion", "some-nonce")
  private val encryptedBeneficialLoan = EncryptedValue("encryptedBeneficialLoan", "some-nonce")

  "MedicalChildcareEducationModel.medicalInsuranceSectionFinished" should {
    "return None when medicalInsuranceQuestion is true and medicalInsurance amount is defined" in {
      val underTest = MedicalChildcareEducationModel(medicalInsuranceQuestion = Some(true), medicalInsurance = Some(1))
      underTest.medicalInsuranceSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Medical insurance amount page' when medicalInsuranceQuestion is true and medicalInsurance amount not defined" in {
      val underTest = MedicalChildcareEducationModel(medicalInsuranceQuestion = Some(true), medicalInsurance = None)
      underTest.medicalInsuranceSectionFinished(taxYearEOY, employmentId) shouldBe Some(MedicalOrDentalBenefitsAmountController.show(taxYearEOY, employmentId))
    }

    "return None when medicalInsuranceQuestion is false" in {
      val underTest = MedicalChildcareEducationModel(medicalInsuranceQuestion = Some(false))
      underTest.medicalInsuranceSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Medical insurance yes/no page' when medicalInsuranceQuestion is None" in {
      val underTest = MedicalChildcareEducationModel(medicalInsuranceQuestion = None)
      underTest.medicalInsuranceSectionFinished(taxYearEOY, employmentId) shouldBe Some(MedicalDentalBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "MedicalChildcareEducationModel.childcareSectionFinished" should {
    "return None when childcareQuestion is true and childcare amount is defined" in {
      val underTest = MedicalChildcareEducationModel(nurseryPlacesQuestion = Some(true), nurseryPlaces = Some(1))
      underTest.childcareSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Childcare amount page' when childcareQuestion is true and childcare amount not defined" in {
      val underTest = MedicalChildcareEducationModel(nurseryPlacesQuestion = Some(true), nurseryPlaces = None)
      underTest.childcareSectionFinished(taxYearEOY, employmentId) shouldBe Some(ChildcareBenefitsAmountController.show(taxYearEOY, employmentId))
    }

    "return None when childcareQuestion is false" in {
      val underTest = MedicalChildcareEducationModel(nurseryPlacesQuestion = Some(false))
      underTest.childcareSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Childcare yes/no page' when childcareQuestion is None" in {
      val underTest = MedicalChildcareEducationModel(nurseryPlacesQuestion = None)
      underTest.childcareSectionFinished(taxYearEOY, employmentId) shouldBe Some(ChildcareBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "MedicalChildcareEducationModel.educationalServicesSectionFinished" should {
    "return None when educationalServicesQuestion is true and educationalServices amount is defined" in {
      val underTest = MedicalChildcareEducationModel(educationalServicesQuestion = Some(true), educationalServices = Some(1))
      underTest.educationalServicesSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Educational services amount page' when educationalServicesQuestion is true and educationalServices amount not defined" in {
      val underTest = MedicalChildcareEducationModel(educationalServicesQuestion = Some(true), educationalServices = None)

      underTest.educationalServicesSectionFinished(taxYearEOY, employmentId) shouldBe Some(EducationalServicesBenefitsAmountController.show(taxYearEOY, employmentId))
    }

    "return None when educationalServicesQuestion is false" in {
      val underTest = MedicalChildcareEducationModel(educationalServicesQuestion = Some(false))
      underTest.educationalServicesSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Educational services yes/no page' when educationalServicesQuestion is None" in {
      val underTest = MedicalChildcareEducationModel(educationalServicesQuestion = None)
      //TODO Educational services yes/no page
      underTest.educationalServicesSectionFinished(taxYearEOY, employmentId) shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "MedicalChildcareEducationModel.beneficialLoanSectionFinished" should {
    "return None when beneficialLoansQuestion is true and beneficialLoans amount is defined" in {
      val underTest = MedicalChildcareEducationModel(beneficialLoanQuestion = Some(true), beneficialLoan = Some(1))
      underTest.beneficialLoanSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Beneficial loans amount page' when beneficialLoansQuestion is true and beneficialLoans amount not defined" in {
      val underTest = MedicalChildcareEducationModel(beneficialLoanQuestion = Some(true), beneficialLoan = None)
      underTest.beneficialLoanSectionFinished(taxYearEOY, employmentId) shouldBe Some(BeneficialLoansAmountController.show(taxYearEOY, employmentId))
    }

    "return None when beneficialLoansQuestion is false" in {
      val underTest = MedicalChildcareEducationModel(beneficialLoanQuestion = Some(false))
      underTest.beneficialLoanSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Beneficial loans yes/no page' when beneficialLoansQuestion is None" in {
      val underTest = MedicalChildcareEducationModel(beneficialLoanQuestion = None)
      underTest.beneficialLoanSectionFinished(taxYearEOY, employmentId) shouldBe Some(BeneficialLoansBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "MedicalChildcareEducationModel.isFinished" should {
    "return result of medicalInsuranceSectionFinished when medicalChildcareEducationQuestion is true and medicalInsuranceQuestion is not None" in {
      val underTest = MedicalChildcareEducationModel(sectionQuestion = Some(true), medicalInsuranceQuestion = None)

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.medicalInsuranceSectionFinished(taxYearEOY, employmentId)
    }

    "return result of childcareSectionFinished when medicalChildcareEducationQuestion is true and medicalInsuranceQuestion is false" in {
      val underTest = MedicalChildcareEducationModel(sectionQuestion = Some(true), medicalInsuranceQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.childcareSectionFinished(taxYearEOY, employmentId)
    }

    "return result of educationalServicesSectionFinished when medicalChildcareEducationQuestion is true and " +
      "medicalInsuranceQuestion is false and childcareQuestion is false" in {
      val underTest = MedicalChildcareEducationModel(sectionQuestion = Some(true),
        medicalInsuranceQuestion = Some(false),
        nurseryPlacesQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.educationalServicesSectionFinished(taxYearEOY, employmentId)
    }

    "return result of beneficialLoanSectionFinished when medicalChildcareEducationQuestion is true and " +
      "medicalInsuranceQuestion is false and childcareQuestion is false and educationalServicesQuestion is false" in {
      val underTest = MedicalChildcareEducationModel(sectionQuestion = Some(true),
        medicalInsuranceQuestion = Some(false),
        nurseryPlacesQuestion = Some(false),
        educationalServicesQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.beneficialLoanSectionFinished(taxYearEOY, employmentId)
    }

    "return None when medicalChildcareEducationQuestion is true and medicalInsuranceQuestion, " +
      "childcareQuestion, educationalServicesQuestion, beneficialLoansQuestion are false" in {
      val underTest = MedicalChildcareEducationModel(sectionQuestion = Some(true),
        medicalInsuranceQuestion = Some(false),
        nurseryPlacesQuestion = Some(false),
        educationalServicesQuestion = Some(false),
        beneficialLoanQuestion = Some(false)
      )

      underTest.isFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return None when medicalChildcareEducationQuestion is false" in {
      val underTest = MedicalChildcareEducationModel(sectionQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Medical childcare education section yes/no page' when medicalChildcareEducationQuestion is None" in {
      val underTest = MedicalChildcareEducationModel(sectionQuestion = None)

      underTest.isFinished(taxYearEOY, employmentId) shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "MedicalChildcareEducationModel.clear" should {
    "return empty MedicalChildcareEducationModel with main question set to false" in {
      MedicalChildcareEducationModel.clear shouldBe MedicalChildcareEducationModel(
        sectionQuestion = Some(false),
        medicalInsuranceQuestion = None,
        medicalInsurance = None,
        nurseryPlacesQuestion = None,
        nurseryPlaces = None,
        educationalServicesQuestion = None,
        educationalServices = None,
        beneficialLoanQuestion = None,
        beneficialLoan = None
      )
    }
  }

  "MedicalChildcareEducationModel.encrypted" should {
    "return EncryptedMedicalChildcareEducationModel instance" in {
      val underTest = aMedicalChildcareEducationModel

      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.sectionQuestion.get, textAndKey).returning(encryptedSectionQuestion)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.medicalInsuranceQuestion.get, textAndKey).returning(encryptedMedicalInsuranceQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.medicalInsurance.get, textAndKey).returning(encryptedMedicalInsurance)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.nurseryPlacesQuestion.get, textAndKey).returning(encryptedNurseryPlacesQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.nurseryPlaces.get, textAndKey).returning(encryptedNurseryPlaces)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.educationalServicesQuestion.get, textAndKey).returning(encryptedEducationalServicesQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.educationalServices.get, textAndKey).returning(encryptedEducationalServices)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.beneficialLoanQuestion.get, textAndKey).returning(encryptedBeneficialLoanQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.beneficialLoan.get, textAndKey).returning(encryptedBeneficialLoan)

      underTest.encrypted shouldBe EncryptedMedicalChildcareEducationModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        medicalInsuranceQuestion = Some(encryptedMedicalInsuranceQuestion),
        medicalInsurance = Some(encryptedMedicalInsurance),
        nurseryPlacesQuestion = Some(encryptedNurseryPlacesQuestion),
        nurseryPlaces = Some(encryptedNurseryPlaces),
        educationalServicesQuestion = Some(encryptedEducationalServicesQuestion),
        educationalServices = Some(encryptedEducationalServices),
        beneficialLoanQuestion = Some(encryptedBeneficialLoanQuestion),
        beneficialLoan = Some(encryptedBeneficialLoan)
      )
    }
  }

  "EncryptedMedicalChildcareEducationModel.decrypted" should {
    "return MedicalChildcareEducationModel instance" in {
      val underTest = EncryptedMedicalChildcareEducationModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        medicalInsuranceQuestion = Some(encryptedMedicalInsuranceQuestion),
        medicalInsurance = Some(encryptedMedicalInsurance),
        nurseryPlacesQuestion = Some(encryptedNurseryPlacesQuestion),
        nurseryPlaces = Some(encryptedNurseryPlaces),
        educationalServicesQuestion = Some(encryptedEducationalServicesQuestion),
        educationalServices = Some(encryptedEducationalServices),
        beneficialLoanQuestion = Some(encryptedBeneficialLoanQuestion),
        beneficialLoan = Some(encryptedBeneficialLoan)
      )

      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedSectionQuestion.value, encryptedSectionQuestion.nonce, textAndKey, *).returning(value = aMedicalChildcareEducationModel.sectionQuestion.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedMedicalInsuranceQuestion.value, encryptedMedicalInsuranceQuestion.nonce, textAndKey, *).returning(value = aMedicalChildcareEducationModel.medicalInsuranceQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedMedicalInsurance.value, encryptedMedicalInsurance.nonce, textAndKey, *).returning(value = aMedicalChildcareEducationModel.medicalInsurance.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedNurseryPlacesQuestion.value, encryptedNurseryPlacesQuestion.nonce, textAndKey, *).returning(value = aMedicalChildcareEducationModel.nurseryPlacesQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedNurseryPlaces.value, encryptedNurseryPlaces.nonce, textAndKey, *).returning(value = aMedicalChildcareEducationModel.nurseryPlaces.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedEducationalServicesQuestion.value, encryptedEducationalServicesQuestion.nonce, textAndKey, *)
        .returning(value = aMedicalChildcareEducationModel.educationalServicesQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedEducationalServices.value, encryptedEducationalServices.nonce, textAndKey, *).returning(value = aMedicalChildcareEducationModel.educationalServices.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedBeneficialLoanQuestion.value, encryptedBeneficialLoanQuestion.nonce, textAndKey, *).returning(value = aMedicalChildcareEducationModel.beneficialLoanQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedBeneficialLoan.value, encryptedBeneficialLoan.nonce, textAndKey, *).returning(value = aMedicalChildcareEducationModel.beneficialLoan.get)

      underTest.decrypted shouldBe aMedicalChildcareEducationModel
    }
  }
}
