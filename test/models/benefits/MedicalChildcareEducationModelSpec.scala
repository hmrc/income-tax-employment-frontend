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
import utils.UnitTest

class MedicalChildcareEducationModelSpec extends UnitTest {

  private val employmentId = "some-employment-id"

  "medicalInsuranceSectionFinished" should {
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

  "childcareSectionFinished" should {
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

  "educationalServicesSectionFinished" should {
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

  "beneficialLoanSectionFinished" should {
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

  "isFinished" should {
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

  "clear" should {
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
}
