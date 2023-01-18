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

import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import support.TaxYearUtils.taxYear
import support.UnitTest
import support.builders.models.benefits.BenefitsBuilder.aBenefits
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher}

class BenefitsViewModelSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val mockCarVanFuelModel = mock[CarVanFuelModel]
  private val mockAccommodationRelocationModel = mock[AccommodationRelocationModel]
  private val mockTravelEntertainmentModel = mock[TravelEntertainmentModel]
  private val mockUtilitiesAndServicesModel = mock[UtilitiesAndServicesModel]
  private val mockMedicalChildcareEducationModel = mock[MedicalChildcareEducationModel]
  private val mockIncomeTaxAndCostsModel = mock[IncomeTaxAndCostsModel]
  private val mockReimbursedCostsVouchersAndNonCashModel = mock[ReimbursedCostsVouchersAndNonCashModel]
  private val mockAssetsModel = mock[AssetsModel]

  private val encryptedCarVanFuelModel = mock[EncryptedCarVanFuelModel]
  private val encryptedAccommodationRelocationModel = mock[EncryptedAccommodationRelocationModel]
  private val encryptedTravelEntertainmentModel = mock[EncryptedTravelEntertainmentModel]
  private val encryptedUtilitiesAndServicesModel = mock[EncryptedUtilitiesAndServicesModel]
  private val encryptedMedicalChildcareEducationModel = mock[EncryptedMedicalChildcareEducationModel]
  private val encryptedIncomeTaxAndCostsModel = mock[EncryptedIncomeTaxAndCostsModel]
  private val encryptedReimbursedCostsVouchersAndNonCashModel = mock[EncryptedReimbursedCostsVouchersAndNonCashModel]
  private val encryptedAssetsModel = mock[EncryptedAssetsModel]
  private val encryptedSubmittedOn = EncryptedValue("encryptedSubmittedOn", "some-nonce")
  private val encryptedIsUsingCustomerData = EncryptedValue("encryptedIsUsingCustomerData", "some-nonce")
  private val encryptedIsBenefitsReceived = EncryptedValue("encryptedIsBenefitsReceived", "some-nonce")

  "BenefitsViewModel.vehicleDetailsPopulated" should {
    "return true when all everything in the vehicle section is defined" in {
      aBenefitsViewModel.vehicleDetailsPopulated shouldBe true
    }

    "return true when an element in the vehicle section is not defined" in {
      aBenefitsViewModel.copy(carVanFuelModel = None).vehicleDetailsPopulated shouldBe false
    }
  }

  "BenefitsViewModel.accommodationDetailsPopulated" should {
    "return true when all everything in the accommodation section is defined" in {
      aBenefitsViewModel.accommodationDetailsPopulated shouldBe true
    }

    "return true when an element in the accommodation section is not defined" in {
      aBenefitsViewModel.copy(accommodationRelocationModel = None).accommodationDetailsPopulated shouldBe false
    }
  }

  "BenefitsViewModel.travelDetailsPopulated" should {
    "return true when all everything in the travel section is defined" in {
      aBenefitsViewModel.travelDetailsPopulated shouldBe true
    }

    "return true when an element in the travel section is not defined" in {
      aBenefitsViewModel.copy(travelEntertainmentModel = None).travelDetailsPopulated shouldBe false
    }
  }

  "BenefitsViewModel.utilitiesDetailsPopulated" should {
    "return true when all everything in the utilities section is defined" in {
      aBenefitsViewModel.utilitiesDetailsPopulated shouldBe true
    }

    "return true when an element in the utilities section is not defined" in {
      aBenefitsViewModel.copy(utilitiesAndServicesModel = None).utilitiesDetailsPopulated shouldBe false
    }
  }

  "BenefitsViewModel.medicalDetailsPopulated" should {
    "return true when all everything in the medical section is defined" in {
      aBenefitsViewModel.medicalDetailsPopulated shouldBe true
    }

    "return true when an element in the medical section is not defined" in {
      aBenefitsViewModel.copy(medicalChildcareEducationModel = None).medicalDetailsPopulated shouldBe false
    }
  }

  "BenefitsViewModel.incomeTaxDetailsPopulated" should {
    "return true when all everything in the incomeTax section is defined" in {
      aBenefitsViewModel.incomeTaxDetailsPopulated shouldBe true
    }

    "return true when an element in the incomeTax section is not defined" in {
      aBenefitsViewModel.copy(incomeTaxAndCostsModel = None).incomeTaxDetailsPopulated shouldBe false
    }
  }

  "BenefitsViewModel.reimbursedDetailsPopulated" should {
    "return true when all everything in the reimbursed section is defined" in {
      aBenefitsViewModel.reimbursedDetailsPopulated shouldBe true
    }

    "return true when an element in the reimbursed section is not defined" in {
      aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = None).reimbursedDetailsPopulated shouldBe false
    }
  }

  "BenefitsViewModel.assetsDetailsPopulated" should {
    "return true when all everything in the assets section is defined" in {
      aBenefitsViewModel.assetsDetailsPopulated shouldBe true
    }

    "return true when an element in the assets section is not defined" in {
      aBenefitsViewModel.copy(assetsModel = None).assetsDetailsPopulated shouldBe false
    }
  }

  "BenefitsViewModel.toBenefits" should {
    "create a Benefits model based on the data from the BenefitsViewModel" in {
      aBenefitsViewModel.asBenefits shouldBe
        aBenefits.copy(accommodation = Some(100), assets = Some(100), assetTransfer = Some(200), beneficialLoan = Some(400),
          car = Some(100), carFuel = Some(200), educationalServices = Some(300), entertaining = Some(300), expenses = Some(100),
          medicalInsurance = Some(100), telephone = Some(100), Some(400), Some(200), van = Some(300), vanFuel = Some(400), mileage = Some(500),
          nonQualifyingRelocationExpenses = Some(300), nurseryPlaces = Some(200), otherItems = Some(500), paymentsOnEmployeesBehalf = Some(255),
          personalIncidentalExpenses = Some(200), qualifyingRelocationExpenses = Some(200), employerProvidedProfessionalSubscriptions = Some(300),
          employerProvidedServices = Some(200), incomeTaxPaidByDirector = Some(255), travelAndSubsistence = Some(100), vouchersAndCreditCards = Some(300),
          nonCash = Some(400))
    }
  }

  "BenefitsViewModel.encrypted" should {
    "return EncryptedBenefitsViewModel instance" in {
      val underTest = BenefitsViewModel(
        carVanFuelModel = Some(mockCarVanFuelModel),
        accommodationRelocationModel = Some(mockAccommodationRelocationModel),
        travelEntertainmentModel = Some(mockTravelEntertainmentModel),
        utilitiesAndServicesModel = Some(mockUtilitiesAndServicesModel),
        medicalChildcareEducationModel = Some(mockMedicalChildcareEducationModel),
        incomeTaxAndCostsModel = Some(mockIncomeTaxAndCostsModel),
        reimbursedCostsVouchersAndNonCashModel = Some(mockReimbursedCostsVouchersAndNonCashModel),
        assetsModel = Some(mockAssetsModel),
        submittedOn = Some("some-date"),
        isUsingCustomerData = true,
        isBenefitsReceived = true
      )

      (mockCarVanFuelModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedCarVanFuelModel)
      (mockAccommodationRelocationModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedAccommodationRelocationModel)
      (mockTravelEntertainmentModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedTravelEntertainmentModel)
      (mockUtilitiesAndServicesModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedUtilitiesAndServicesModel)
      (mockMedicalChildcareEducationModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedMedicalChildcareEducationModel)
      (mockIncomeTaxAndCostsModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedIncomeTaxAndCostsModel)
      (mockReimbursedCostsVouchersAndNonCashModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedReimbursedCostsVouchersAndNonCashModel)
      (mockAssetsModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedAssetsModel)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.submittedOn.get, textAndKey).returning(encryptedSubmittedOn)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.isUsingCustomerData, textAndKey).returning(encryptedIsUsingCustomerData)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.isBenefitsReceived, textAndKey).returning(encryptedIsBenefitsReceived)

      underTest.encrypted shouldBe EncryptedBenefitsViewModel(
        carVanFuelModel = Some(encryptedCarVanFuelModel),
        accommodationRelocationModel = Some(encryptedAccommodationRelocationModel),
        travelEntertainmentModel = Some(encryptedTravelEntertainmentModel),
        utilitiesAndServicesModel = Some(encryptedUtilitiesAndServicesModel),
        medicalChildcareEducationModel = Some(encryptedMedicalChildcareEducationModel),
        incomeTaxAndCostsModel = Some(encryptedIncomeTaxAndCostsModel),
        reimbursedCostsVouchersAndNonCashModel = Some(encryptedReimbursedCostsVouchersAndNonCashModel),
        assetsModel = Some(encryptedAssetsModel),
        submittedOn = Some(encryptedSubmittedOn),
        isUsingCustomerData = encryptedIsUsingCustomerData,
        isBenefitsReceived = encryptedIsBenefitsReceived
      )
    }
  }

  "EncryptedBenefitsViewModel.decrypted" should {
    "return BenefitsViewModel instance" in {
      val underTest = EncryptedBenefitsViewModel(
        carVanFuelModel = Some(encryptedCarVanFuelModel),
        accommodationRelocationModel = Some(encryptedAccommodationRelocationModel),
        travelEntertainmentModel = Some(encryptedTravelEntertainmentModel),
        utilitiesAndServicesModel = Some(encryptedUtilitiesAndServicesModel),
        medicalChildcareEducationModel = Some(encryptedMedicalChildcareEducationModel),
        incomeTaxAndCostsModel = Some(encryptedIncomeTaxAndCostsModel),
        reimbursedCostsVouchersAndNonCashModel = Some(encryptedReimbursedCostsVouchersAndNonCashModel),
        assetsModel = Some(encryptedAssetsModel),
        submittedOn = Some(encryptedSubmittedOn),
        isUsingCustomerData = encryptedIsUsingCustomerData,
        isBenefitsReceived = encryptedIsBenefitsReceived
      )

      (encryptedCarVanFuelModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(aBenefitsViewModel.carVanFuelModel.get)
      (encryptedAccommodationRelocationModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(aBenefitsViewModel.accommodationRelocationModel.get)
      (encryptedTravelEntertainmentModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(aBenefitsViewModel.travelEntertainmentModel.get)
      (encryptedUtilitiesAndServicesModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(aBenefitsViewModel.utilitiesAndServicesModel.get)
      (encryptedMedicalChildcareEducationModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(aBenefitsViewModel.medicalChildcareEducationModel.get)
      (encryptedIncomeTaxAndCostsModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(aBenefitsViewModel.incomeTaxAndCostsModel.get)
      (encryptedReimbursedCostsVouchersAndNonCashModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(aBenefitsViewModel.reimbursedCostsVouchersAndNonCashModel.get)
      (encryptedAssetsModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(aBenefitsViewModel.assetsModel.get)
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedSubmittedOn.value, encryptedSubmittedOn.nonce, textAndKey, *).returning(value = s"$taxYear-03-11")
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedIsUsingCustomerData.value, encryptedIsUsingCustomerData.nonce, textAndKey, *).returning(value = aBenefitsViewModel.isUsingCustomerData)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedIsBenefitsReceived.value, encryptedIsBenefitsReceived.nonce, textAndKey, *).returning(value = aBenefitsViewModel.isBenefitsReceived)

      underTest.decrypted shouldBe aBenefitsViewModel.copy(submittedOn = Some(s"$taxYear-03-11"))
    }
  }
}
