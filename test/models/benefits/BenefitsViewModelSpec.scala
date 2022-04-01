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

import support.UnitTest
import support.builders.models.benefits.BenefitsBuilder.aBenefits
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel

class BenefitsViewModelSpec extends UnitTest {

  ".vehicleDetailsPopulated" should {
    "return true when all everything in the vehicle section is defined" in {
      aBenefitsViewModel.vehicleDetailsPopulated shouldBe true
    }

    "return true when an element in the vehicle section is not defined" in {
      aBenefitsViewModel.copy(carVanFuelModel = None).vehicleDetailsPopulated shouldBe false
    }
  }

  ".accommodationDetailsPopulated" should {
    "return true when all everything in the accommodation section is defined" in {
      aBenefitsViewModel.accommodationDetailsPopulated shouldBe true
    }

    "return true when an element in the accommodation section is not defined" in {
      aBenefitsViewModel.copy(accommodationRelocationModel = None).accommodationDetailsPopulated shouldBe false
    }
  }

  ".travelDetailsPopulated" should {
    "return true when all everything in the travel section is defined" in {
      aBenefitsViewModel.travelDetailsPopulated shouldBe true
    }

    "return true when an element in the travel section is not defined" in {
      aBenefitsViewModel.copy(travelEntertainmentModel = None).travelDetailsPopulated shouldBe false
    }
  }

  ".utilitiesDetailsPopulated" should {
    "return true when all everything in the utilities section is defined" in {
      aBenefitsViewModel.utilitiesDetailsPopulated shouldBe true
    }

    "return true when an element in the utilities section is not defined" in {
      aBenefitsViewModel.copy(utilitiesAndServicesModel = None).utilitiesDetailsPopulated shouldBe false
    }
  }

  ".medicalDetailsPopulated" should {
    "return true when all everything in the medical section is defined" in {
      aBenefitsViewModel.medicalDetailsPopulated shouldBe true
    }

    "return true when an element in the medical section is not defined" in {
      aBenefitsViewModel.copy(medicalChildcareEducationModel = None).medicalDetailsPopulated shouldBe false
    }
  }

  ".incomeTaxDetailsPopulated" should {
    "return true when all everything in the incomeTax section is defined" in {
      aBenefitsViewModel.incomeTaxDetailsPopulated shouldBe true
    }

    "return true when an element in the incomeTax section is not defined" in {
      aBenefitsViewModel.copy(incomeTaxAndCostsModel = None).incomeTaxDetailsPopulated shouldBe false
    }
  }

  ".reimbursedDetailsPopulated" should {
    "return true when all everything in the reimbursed section is defined" in {
      aBenefitsViewModel.reimbursedDetailsPopulated shouldBe true
    }

    "return true when an element in the reimbursed section is not defined" in {
      aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = None).reimbursedDetailsPopulated shouldBe false
    }
  }

  ".assetsDetailsPopulated" should {
    "return true when all everything in the assets section is defined" in {
      aBenefitsViewModel.assetsDetailsPopulated shouldBe true
    }

    "return true when an element in the assets section is not defined" in {
      aBenefitsViewModel.copy(assetsModel = None).assetsDetailsPopulated shouldBe false
    }
  }

  ".toBenefits" should {
    "create a Benefits model based on the data from the BenefitsViewModel" in {
      aBenefitsViewModel.toBenefits shouldBe
        aBenefits.copy(accommodation = Some(100),assets = Some(100), assetTransfer = Some(200), beneficialLoan = Some(400),
          car = Some(100), carFuel = Some(200), educationalServices = Some(300), entertaining = Some(300), expenses = Some(100),
          medicalInsurance = Some(100), telephone = Some(100), Some(400), Some(200), van = Some(300), vanFuel = Some(400), mileage = Some(500),
          nonQualifyingRelocationExpenses = Some(300), nurseryPlaces = Some(200), otherItems = Some(500), paymentsOnEmployeesBehalf = Some(255),
          personalIncidentalExpenses = Some(200), qualifyingRelocationExpenses = Some(200), employerProvidedProfessionalSubscriptions = Some(300),
          employerProvidedServices = Some(200), incomeTaxPaidByDirector = Some(255), travelAndSubsistence = Some(100), vouchersAndCreditCards = Some(300),
          nonCash = Some(400))
    }
  }

}
