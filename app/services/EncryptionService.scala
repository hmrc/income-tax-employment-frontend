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

package services

import config.AppConfig
import javax.inject.Inject
import models.benefits._
import models.employment.{EncryptedStudentLoansCYAModel, StudentLoansCYAModel}
import models.expenses.{EncryptedExpensesViewModel, ExpensesViewModel}
import models.mongo._
import utils.SecureGCMCipher

class EncryptionService @Inject()(secureGCMCipher: SecureGCMCipher, appConfig: AppConfig) {

  def encryptUserData(userData: EmploymentUserData): EncryptedEmploymentUserData = {
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    EncryptedEmploymentUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      employmentId = userData.employmentId,
      isPriorSubmission = userData.isPriorSubmission,
      hasPriorBenefits = userData.hasPriorBenefits,
      hasPriorStudentLoans = userData.hasPriorStudentLoans,
      employment = encryptEmployment(userData.employment),
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptEmployment(employment: EmploymentCYAModel)(implicit textAndKey: TextAndKey): EncryptedEmploymentCYAModel = {
    EncryptedEmploymentCYAModel(
      employmentDetails = encryptEmploymentDetails(employment.employmentDetails),
      employmentBenefits = employment.employmentBenefits.map(encryptEmploymentBenefits),
      studentLoansCYAModel = employment.studentLoans.map(encryptStudentLoansCYAModel)
    )
  }

  private def encryptEmploymentDetails(e: EmploymentDetails)(implicit textAndKey: TextAndKey): EncryptedEmploymentDetails = {
    EncryptedEmploymentDetails(
      employerName = secureGCMCipher.encrypt(e.employerName),
      employerRef = e.employerRef.map(secureGCMCipher.encrypt),
      startDate = e.startDate.map(secureGCMCipher.encrypt),
      payrollId = e.payrollId.map(secureGCMCipher.encrypt),
      cessationDateQuestion = e.cessationDateQuestion.map(secureGCMCipher.encrypt),
      cessationDate = e.cessationDate.map(secureGCMCipher.encrypt),
      dateIgnored = e.dateIgnored.map(secureGCMCipher.encrypt),
      employmentSubmittedOn = e.employmentSubmittedOn.map(secureGCMCipher.encrypt),
      employmentDetailsSubmittedOn = e.employmentDetailsSubmittedOn.map(secureGCMCipher.encrypt),
      taxablePayToDate = e.taxablePayToDate.map(secureGCMCipher.encrypt),
      totalTaxToDate = e.totalTaxToDate.map(secureGCMCipher.encrypt),
      currentDataIsHmrcHeld = secureGCMCipher.encrypt(e.currentDataIsHmrcHeld)
    )
  }

  private def encryptCarVanFuelModel(carVanFuelModel: CarVanFuelModel)(implicit textAndKey: TextAndKey): EncryptedCarVanFuelModel = {
    EncryptedCarVanFuelModel(
      sectionQuestion = carVanFuelModel.sectionQuestion.map(secureGCMCipher.encrypt),
      carQuestion = carVanFuelModel.carQuestion.map(secureGCMCipher.encrypt),
      car = carVanFuelModel.car.map(secureGCMCipher.encrypt),
      carFuelQuestion = carVanFuelModel.carFuelQuestion.map(secureGCMCipher.encrypt),
      carFuel = carVanFuelModel.carFuel.map(secureGCMCipher.encrypt),
      vanQuestion = carVanFuelModel.vanQuestion.map(secureGCMCipher.encrypt),
      van = carVanFuelModel.van.map(secureGCMCipher.encrypt),
      vanFuelQuestion = carVanFuelModel.vanFuelQuestion.map(secureGCMCipher.encrypt),
      vanFuel = carVanFuelModel.vanFuel.map(secureGCMCipher.encrypt),
      mileageQuestion = carVanFuelModel.mileageQuestion.map(secureGCMCipher.encrypt),
      mileage = carVanFuelModel.mileage.map(secureGCMCipher.encrypt)
    )
  }

  private def decryptCarVanFuelModel(carVanFuelModel: EncryptedCarVanFuelModel)(implicit textAndKey: TextAndKey): CarVanFuelModel = {
    CarVanFuelModel(
      sectionQuestion = carVanFuelModel.sectionQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      carQuestion = carVanFuelModel.carQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      car = carVanFuelModel.car.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      carFuelQuestion = carVanFuelModel.carFuelQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      carFuel = carVanFuelModel.carFuel.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      vanQuestion = carVanFuelModel.vanQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      van = carVanFuelModel.van.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      vanFuelQuestion = carVanFuelModel.vanFuelQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      vanFuel = carVanFuelModel.vanFuel.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      mileageQuestion = carVanFuelModel.mileageQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      mileage = carVanFuelModel.mileage.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def encryptAccommodationRelocationModel(accommodationRelocationModel: AccommodationRelocationModel)
                                                 (implicit textAndKey: TextAndKey): EncryptedAccommodationRelocationModel = {
    EncryptedAccommodationRelocationModel(
      sectionQuestion = accommodationRelocationModel.sectionQuestion.map(secureGCMCipher.encrypt),
      accommodationQuestion = accommodationRelocationModel.accommodationQuestion.map(secureGCMCipher.encrypt),
      accommodation = accommodationRelocationModel.accommodation.map(secureGCMCipher.encrypt),
      qualifyingRelocationExpensesQuestion = accommodationRelocationModel.qualifyingRelocationExpensesQuestion.map(secureGCMCipher.encrypt),
      qualifyingRelocationExpenses = accommodationRelocationModel.qualifyingRelocationExpenses.map(secureGCMCipher.encrypt),
      nonQualifyingRelocationExpensesQuestion = accommodationRelocationModel.nonQualifyingRelocationExpensesQuestion.map(secureGCMCipher.encrypt),
      nonQualifyingRelocationExpenses = accommodationRelocationModel.nonQualifyingRelocationExpenses.map(secureGCMCipher.encrypt)
    )
  }

  private def decryptAccommodationRelocationModel(accommodationRelocationModel: EncryptedAccommodationRelocationModel)
                                                 (implicit textAndKey: TextAndKey): AccommodationRelocationModel = {
    AccommodationRelocationModel(
      sectionQuestion = accommodationRelocationModel.sectionQuestion
        .map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      accommodationQuestion = accommodationRelocationModel.accommodationQuestion
        .map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      accommodation = accommodationRelocationModel.accommodation
        .map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      qualifyingRelocationExpensesQuestion = accommodationRelocationModel.qualifyingRelocationExpensesQuestion
        .map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      qualifyingRelocationExpenses = accommodationRelocationModel.qualifyingRelocationExpenses
        .map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      nonQualifyingRelocationExpensesQuestion = accommodationRelocationModel.nonQualifyingRelocationExpensesQuestion
        .map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      nonQualifyingRelocationExpenses = accommodationRelocationModel.nonQualifyingRelocationExpenses
        .map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def encryptTravelEntertainmentModel(travelEntertainmentModel: TravelEntertainmentModel)
                                             (implicit textAndKey: TextAndKey): EncryptedTravelEntertainmentModel = {
    EncryptedTravelEntertainmentModel(
      sectionQuestion = travelEntertainmentModel.sectionQuestion.map(secureGCMCipher.encrypt),
      travelAndSubsistenceQuestion = travelEntertainmentModel.travelAndSubsistenceQuestion.map(secureGCMCipher.encrypt),
      travelAndSubsistence = travelEntertainmentModel.travelAndSubsistence.map(secureGCMCipher.encrypt),
      personalIncidentalExpensesQuestion = travelEntertainmentModel.personalIncidentalExpensesQuestion.map(secureGCMCipher.encrypt),
      personalIncidentalExpenses = travelEntertainmentModel.personalIncidentalExpenses.map(secureGCMCipher.encrypt),
      entertainingQuestion = travelEntertainmentModel.entertainingQuestion.map(secureGCMCipher.encrypt),
      entertaining = travelEntertainmentModel.entertaining.map(secureGCMCipher.encrypt)
    )
  }

  private def encryptUtilitiesAndServicesModel(utilitiesAndServicesModel: UtilitiesAndServicesModel)
                                              (implicit textAndKey: TextAndKey): EncryptedUtilitiesAndServicesModel = {
    EncryptedUtilitiesAndServicesModel(
      sectionQuestion = utilitiesAndServicesModel.sectionQuestion.map(secureGCMCipher.encrypt),
      telephoneQuestion = utilitiesAndServicesModel.telephoneQuestion.map(secureGCMCipher.encrypt),
      telephone = utilitiesAndServicesModel.telephone.map(secureGCMCipher.encrypt),
      employerProvidedServicesQuestion = utilitiesAndServicesModel.employerProvidedServicesQuestion.map(secureGCMCipher.encrypt),
      employerProvidedServices = utilitiesAndServicesModel.employerProvidedServices.map(secureGCMCipher.encrypt),
      employerProvidedProfessionalSubscriptionsQuestion = utilitiesAndServicesModel.employerProvidedProfessionalSubscriptionsQuestion
        .map(secureGCMCipher.encrypt),
      employerProvidedProfessionalSubscriptions = utilitiesAndServicesModel.employerProvidedProfessionalSubscriptions.map(secureGCMCipher.encrypt),
      serviceQuestion = utilitiesAndServicesModel.serviceQuestion.map(secureGCMCipher.encrypt),
      service = utilitiesAndServicesModel.service.map(secureGCMCipher.encrypt)
    )
  }

  private def encryptMedicalChildcareEducationModel(medicalChildcareEducationModel: MedicalChildcareEducationModel)
                                                   (implicit textAndKey: TextAndKey): EncryptedMedicalChildcareEducationModel = {
    EncryptedMedicalChildcareEducationModel(
      sectionQuestion = medicalChildcareEducationModel.sectionQuestion.map(secureGCMCipher.encrypt),
      medicalInsuranceQuestion = medicalChildcareEducationModel.medicalInsuranceQuestion.map(secureGCMCipher.encrypt),
      medicalInsurance = medicalChildcareEducationModel.medicalInsurance.map(secureGCMCipher.encrypt),
      nurseryPlacesQuestion = medicalChildcareEducationModel.nurseryPlacesQuestion.map(secureGCMCipher.encrypt),
      nurseryPlaces = medicalChildcareEducationModel.nurseryPlaces.map(secureGCMCipher.encrypt),
      educationalServicesQuestion = medicalChildcareEducationModel.educationalServicesQuestion.map(secureGCMCipher.encrypt),
      educationalServices = medicalChildcareEducationModel.educationalServices.map(secureGCMCipher.encrypt),
      beneficialLoanQuestion = medicalChildcareEducationModel.beneficialLoanQuestion.map(secureGCMCipher.encrypt),
      beneficialLoan = medicalChildcareEducationModel.beneficialLoan.map(secureGCMCipher.encrypt)
    )
  }

  private def encryptIncomeTaxAndCostsModel(incomeTaxAndCostsModel: IncomeTaxAndCostsModel)
                                           (implicit textAndKey: TextAndKey): EncryptedIncomeTaxAndCostsModel = {
    EncryptedIncomeTaxAndCostsModel(
      sectionQuestion = incomeTaxAndCostsModel.sectionQuestion.map(secureGCMCipher.encrypt),
      incomeTaxPaidByDirectorQuestion = incomeTaxAndCostsModel.incomeTaxPaidByDirectorQuestion.map(secureGCMCipher.encrypt),
      incomeTaxPaidByDirector = incomeTaxAndCostsModel.incomeTaxPaidByDirector.map(secureGCMCipher.encrypt),
      paymentsOnEmployeesBehalfQuestion = incomeTaxAndCostsModel.paymentsOnEmployeesBehalfQuestion.map(secureGCMCipher.encrypt),
      paymentsOnEmployeesBehalf = incomeTaxAndCostsModel.paymentsOnEmployeesBehalf.map(secureGCMCipher.encrypt)
    )
  }
  private def encryptReimbursedCostsVouchersAndNonCashModel(reimbursedCostsVouchersAndNonCashModel: ReimbursedCostsVouchersAndNonCashModel)
                                                           (implicit textAndKey: TextAndKey): EncryptedReimbursedCostsVouchersAndNonCashModel = {
    EncryptedReimbursedCostsVouchersAndNonCashModel(
      sectionQuestion = reimbursedCostsVouchersAndNonCashModel.sectionQuestion.map(secureGCMCipher.encrypt),
      expensesQuestion = reimbursedCostsVouchersAndNonCashModel.expensesQuestion.map(secureGCMCipher.encrypt),
      expenses = reimbursedCostsVouchersAndNonCashModel.expenses.map(secureGCMCipher.encrypt),
      taxableExpensesQuestion = reimbursedCostsVouchersAndNonCashModel.taxableExpensesQuestion.map(secureGCMCipher.encrypt),
      taxableExpenses = reimbursedCostsVouchersAndNonCashModel.taxableExpenses.map(secureGCMCipher.encrypt),
      vouchersAndCreditCardsQuestion = reimbursedCostsVouchersAndNonCashModel.vouchersAndCreditCardsQuestion.map(secureGCMCipher.encrypt),
      vouchersAndCreditCards = reimbursedCostsVouchersAndNonCashModel.vouchersAndCreditCards.map(secureGCMCipher.encrypt),
      nonCashQuestion = reimbursedCostsVouchersAndNonCashModel.nonCashQuestion.map(secureGCMCipher.encrypt),
      nonCash = reimbursedCostsVouchersAndNonCashModel.nonCash.map(secureGCMCipher.encrypt),
      otherItemsQuestion = reimbursedCostsVouchersAndNonCashModel.otherItemsQuestion.map(secureGCMCipher.encrypt),
      otherItems = reimbursedCostsVouchersAndNonCashModel.otherItems.map(secureGCMCipher.encrypt)
    )
  }
  private def encryptAssetsModel(assetsModel: AssetsModel)
                                (implicit textAndKey: TextAndKey): EncryptedAssetsModel = {
    EncryptedAssetsModel(
      sectionQuestion = assetsModel.sectionQuestion.map(secureGCMCipher.encrypt),
      assetsQuestion = assetsModel.assetsQuestion.map(secureGCMCipher.encrypt),
      assets = assetsModel.assets.map(secureGCMCipher.encrypt),
      assetTransferQuestion = assetsModel.assetTransferQuestion.map(secureGCMCipher.encrypt),
      assetTransfer = assetsModel.assetTransfer.map(secureGCMCipher.encrypt)
    )
  }

  private def decryptReimbursedCostsVouchersAndNonCashModel(reimbursedCostsVouchersAndNonCashModel: EncryptedReimbursedCostsVouchersAndNonCashModel)
                                                           (implicit textAndKey: TextAndKey): ReimbursedCostsVouchersAndNonCashModel = {
    ReimbursedCostsVouchersAndNonCashModel(
      sectionQuestion = reimbursedCostsVouchersAndNonCashModel.sectionQuestion.map(
        x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      expensesQuestion = reimbursedCostsVouchersAndNonCashModel.expensesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      expenses = reimbursedCostsVouchersAndNonCashModel.expenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      taxableExpensesQuestion = reimbursedCostsVouchersAndNonCashModel.taxableExpensesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      taxableExpenses = reimbursedCostsVouchersAndNonCashModel.taxableExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      vouchersAndCreditCardsQuestion = reimbursedCostsVouchersAndNonCashModel.vouchersAndCreditCardsQuestion.map(
        x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      vouchersAndCreditCards = reimbursedCostsVouchersAndNonCashModel.vouchersAndCreditCards.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      nonCashQuestion = reimbursedCostsVouchersAndNonCashModel.nonCashQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      nonCash = reimbursedCostsVouchersAndNonCashModel.nonCash.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      otherItemsQuestion = reimbursedCostsVouchersAndNonCashModel.otherItemsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      otherItems = reimbursedCostsVouchersAndNonCashModel.otherItems.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }
  private def decryptAssetsModel(assetsModel: EncryptedAssetsModel)
                                (implicit textAndKey: TextAndKey): AssetsModel = {
    AssetsModel(
      sectionQuestion = assetsModel.sectionQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      assetsQuestion = assetsModel.assetsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      assets = assetsModel.assets.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      assetTransferQuestion = assetsModel.assetTransferQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      assetTransfer = assetsModel.assetTransfer.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptTravelEntertainmentModel(travelEntertainmentModel: EncryptedTravelEntertainmentModel)
                                             (implicit textAndKey: TextAndKey): TravelEntertainmentModel = {
    TravelEntertainmentModel(
      sectionQuestion = travelEntertainmentModel.sectionQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      travelAndSubsistenceQuestion = travelEntertainmentModel.travelAndSubsistenceQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      travelAndSubsistence = travelEntertainmentModel.travelAndSubsistence.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      personalIncidentalExpensesQuestion = travelEntertainmentModel.personalIncidentalExpensesQuestion
        .map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      personalIncidentalExpenses = travelEntertainmentModel.personalIncidentalExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      entertainingQuestion = travelEntertainmentModel.entertainingQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      entertaining = travelEntertainmentModel.entertaining.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptUtilitiesAndServicesModel(utilitiesAndServicesModel: EncryptedUtilitiesAndServicesModel)
                                              (implicit textAndKey: TextAndKey): UtilitiesAndServicesModel = {
    UtilitiesAndServicesModel(
      sectionQuestion = utilitiesAndServicesModel.sectionQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      telephoneQuestion = utilitiesAndServicesModel.telephoneQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      telephone = utilitiesAndServicesModel.telephone.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      employerProvidedServicesQuestion = utilitiesAndServicesModel.employerProvidedServicesQuestion
        .map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      employerProvidedServices = utilitiesAndServicesModel.employerProvidedServices.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      employerProvidedProfessionalSubscriptionsQuestion = utilitiesAndServicesModel.employerProvidedProfessionalSubscriptionsQuestion
        .map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      employerProvidedProfessionalSubscriptions = utilitiesAndServicesModel.employerProvidedProfessionalSubscriptions
        .map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      serviceQuestion = utilitiesAndServicesModel.serviceQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      service = utilitiesAndServicesModel.service.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptMedicalChildcareEducationModel(medicalChildcareEducationModel: EncryptedMedicalChildcareEducationModel)
                                                   (implicit textAndKey: TextAndKey): MedicalChildcareEducationModel = {
    MedicalChildcareEducationModel(
      sectionQuestion = medicalChildcareEducationModel.sectionQuestion
        .map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      medicalInsuranceQuestion = medicalChildcareEducationModel.medicalInsuranceQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      medicalInsurance = medicalChildcareEducationModel.medicalInsurance.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      nurseryPlacesQuestion = medicalChildcareEducationModel.nurseryPlacesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      nurseryPlaces = medicalChildcareEducationModel.nurseryPlaces.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      educationalServicesQuestion = medicalChildcareEducationModel.educationalServicesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      educationalServices = medicalChildcareEducationModel.educationalServices.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      beneficialLoanQuestion = medicalChildcareEducationModel.beneficialLoanQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      beneficialLoan = medicalChildcareEducationModel.beneficialLoan.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptIncomeTaxAndCostsModel(incomeTaxAndCostsModel: EncryptedIncomeTaxAndCostsModel)
                                           (implicit textAndKey: TextAndKey): IncomeTaxAndCostsModel = {
    IncomeTaxAndCostsModel(
      sectionQuestion = incomeTaxAndCostsModel.sectionQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      incomeTaxPaidByDirectorQuestion = incomeTaxAndCostsModel.incomeTaxPaidByDirectorQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      incomeTaxPaidByDirector = incomeTaxAndCostsModel.incomeTaxPaidByDirector.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      paymentsOnEmployeesBehalfQuestion = incomeTaxAndCostsModel.paymentsOnEmployeesBehalfQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      paymentsOnEmployeesBehalf = incomeTaxAndCostsModel.paymentsOnEmployeesBehalf.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def encryptEmploymentBenefits(e: BenefitsViewModel)(implicit textAndKey: TextAndKey): EncryptedBenefitsViewModel = {
    EncryptedBenefitsViewModel(
      carVanFuelModel = e.carVanFuelModel.map(encryptCarVanFuelModel),
      accommodationRelocationModel = e.accommodationRelocationModel.map(encryptAccommodationRelocationModel),
      travelEntertainmentModel = e.travelEntertainmentModel.map(encryptTravelEntertainmentModel),
      utilitiesAndServicesModel = e.utilitiesAndServicesModel.map(encryptUtilitiesAndServicesModel),
      medicalChildcareEducationModel = e.medicalChildcareEducationModel.map(encryptMedicalChildcareEducationModel),
      incomeTaxAndCostsModel = e.incomeTaxAndCostsModel.map(encryptIncomeTaxAndCostsModel),
      reimbursedCostsVouchersAndNonCashModel = e.reimbursedCostsVouchersAndNonCashModel.map(encryptReimbursedCostsVouchersAndNonCashModel),
      assetsModel = e.assetsModel.map(encryptAssetsModel),
      submittedOn = e.submittedOn.map(secureGCMCipher.encrypt),
      isUsingCustomerData = secureGCMCipher.encrypt(e.isUsingCustomerData),
      isBenefitsReceived = secureGCMCipher.encrypt(e.isBenefitsReceived)
    )
  }

  private def decryptEmploymentBenefits(e: EncryptedBenefitsViewModel)(implicit textAndKey: TextAndKey): BenefitsViewModel = {
    BenefitsViewModel(
      carVanFuelModel = e.carVanFuelModel.map(decryptCarVanFuelModel),
      accommodationRelocationModel = e.accommodationRelocationModel.map(decryptAccommodationRelocationModel),
      travelEntertainmentModel = e.travelEntertainmentModel.map(decryptTravelEntertainmentModel),
      utilitiesAndServicesModel = e.utilitiesAndServicesModel.map(decryptUtilitiesAndServicesModel),
      medicalChildcareEducationModel = e.medicalChildcareEducationModel.map(decryptMedicalChildcareEducationModel),
      incomeTaxAndCostsModel = e.incomeTaxAndCostsModel.map(decryptIncomeTaxAndCostsModel),
      reimbursedCostsVouchersAndNonCashModel = e.reimbursedCostsVouchersAndNonCashModel.map(decryptReimbursedCostsVouchersAndNonCashModel),
      assetsModel = e.assetsModel.map(decryptAssetsModel),
      submittedOn = e.submittedOn.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      isUsingCustomerData = secureGCMCipher.decrypt[Boolean](e.isUsingCustomerData.value, e.isUsingCustomerData.nonce),
      isBenefitsReceived = secureGCMCipher.decrypt[Boolean](e.isBenefitsReceived.value, e.isBenefitsReceived.nonce)
    )
  }

  def encryptExpenses(userData: ExpensesUserData): EncryptedExpensesUserData = {

    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    val expenses = EncryptedExpensesViewModel(
      businessTravelCosts = userData.expensesCya.expenses.businessTravelCosts.map(secureGCMCipher.encrypt),
      jobExpenses = userData.expensesCya.expenses.jobExpenses.map(secureGCMCipher.encrypt),
      flatRateJobExpenses = userData.expensesCya.expenses.flatRateJobExpenses.map(secureGCMCipher.encrypt),
      professionalSubscriptions = userData.expensesCya.expenses.professionalSubscriptions.map(secureGCMCipher.encrypt),
      hotelAndMealExpenses = userData.expensesCya.expenses.hotelAndMealExpenses.map(secureGCMCipher.encrypt),
      otherAndCapitalAllowances = userData.expensesCya.expenses.otherAndCapitalAllowances.map(secureGCMCipher.encrypt),
      vehicleExpenses = userData.expensesCya.expenses.vehicleExpenses.map(secureGCMCipher.encrypt),
      mileageAllowanceRelief = userData.expensesCya.expenses.mileageAllowanceRelief.map(secureGCMCipher.encrypt),
      jobExpensesQuestion = userData.expensesCya.expenses.jobExpensesQuestion.map(secureGCMCipher.encrypt),
      flatRateJobExpensesQuestion = userData.expensesCya.expenses.flatRateJobExpensesQuestion.map(secureGCMCipher.encrypt),
      professionalSubscriptionsQuestion = userData.expensesCya.expenses.professionalSubscriptionsQuestion.map(secureGCMCipher.encrypt),
      otherAndCapitalAllowancesQuestion = userData.expensesCya.expenses.otherAndCapitalAllowancesQuestion.map(secureGCMCipher.encrypt),
      submittedOn = userData.expensesCya.expenses.submittedOn.map(secureGCMCipher.encrypt),
      isUsingCustomerData = secureGCMCipher.encrypt(userData.expensesCya.expenses.isUsingCustomerData),
      claimingEmploymentExpenses = secureGCMCipher.encrypt(userData.expensesCya.expenses.claimingEmploymentExpenses)
    )

    val expensesCYA = EncryptedExpensesCYAModel(
      expenses = expenses
    )

    EncryptedExpensesUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      isPriorSubmission = userData.isPriorSubmission,
      hasPriorExpenses = userData.hasPriorExpenses,
      expensesCya = expensesCYA,
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptStudentLoansCYAModel(studentloans: StudentLoansCYAModel)
                                                   (implicit textAndKey: TextAndKey): EncryptedStudentLoansCYAModel ={
    EncryptedStudentLoansCYAModel(
      secureGCMCipher.encrypt[Boolean](studentloans.uglDeduction),
      studentloans.uglDeductionAmount.map(x => secureGCMCipher.encrypt[BigDecimal](x)),
      secureGCMCipher.encrypt[Boolean](studentloans.pglDeduction),
      studentloans.pglDeductionAmount.map(x => secureGCMCipher.encrypt[BigDecimal](x))
    )
  }

  private def decryptStudentLoansCYAModel(studentloans: EncryptedStudentLoansCYAModel)
                                                   (implicit textAndKey: TextAndKey): StudentLoansCYAModel ={
    StudentLoansCYAModel(
      secureGCMCipher.decrypt[Boolean](studentloans.uglDeduction.value, studentloans.uglDeduction.nonce),
      studentloans.uglDeductionAmount.map(x => secureGCMCipher.decrypt[BigDecimal](x.value,x.nonce)),
      secureGCMCipher.decrypt[Boolean](studentloans.pglDeduction.value, studentloans.pglDeduction.nonce),
      studentloans.pglDeductionAmount.map(x => secureGCMCipher.decrypt[BigDecimal](x.value,x.nonce))
    )
  }


  def decryptExpenses(userData: EncryptedExpensesUserData): ExpensesUserData = {

    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    val expenses = ExpensesViewModel(claimingEmploymentExpenses = secureGCMCipher
      .decrypt[Boolean](userData.expensesCya.expenses.claimingEmploymentExpenses.value, userData.expensesCya.expenses.claimingEmploymentExpenses.nonce),
      jobExpensesQuestion = userData.expensesCya.expenses.jobExpensesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      jobExpenses = userData.expensesCya.expenses.jobExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      flatRateJobExpensesQuestion = userData.expensesCya.expenses.flatRateJobExpensesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      flatRateJobExpenses = userData.expensesCya.expenses.flatRateJobExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      professionalSubscriptionsQuestion =
        userData.expensesCya.expenses.professionalSubscriptionsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      professionalSubscriptions = userData.expensesCya.expenses.professionalSubscriptions.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      otherAndCapitalAllowancesQuestion =
        userData.expensesCya.expenses.otherAndCapitalAllowancesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      otherAndCapitalAllowances = userData.expensesCya.expenses.otherAndCapitalAllowances.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      businessTravelCosts = userData.expensesCya.expenses.businessTravelCosts.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      hotelAndMealExpenses = userData.expensesCya.expenses.hotelAndMealExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      vehicleExpenses = userData.expensesCya.expenses.vehicleExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      mileageAllowanceRelief = userData.expensesCya.expenses.mileageAllowanceRelief.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      submittedOn = userData.expensesCya.expenses.submittedOn.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      isUsingCustomerData =
        secureGCMCipher.decrypt[Boolean](userData.expensesCya.expenses.isUsingCustomerData.value, userData.expensesCya.expenses.isUsingCustomerData.nonce))

    val expensesCYA = ExpensesCYAModel(
      expenses = expenses
    )

    ExpensesUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      isPriorSubmission = userData.isPriorSubmission,
      hasPriorExpenses = userData.hasPriorExpenses,
      expensesCya = expensesCYA,
      lastUpdated = userData.lastUpdated
    )
  }

  private def decryptEmploymentDetails(e: EncryptedEmploymentDetails)(implicit textAndKey: TextAndKey): EmploymentDetails = {
    EmploymentDetails(
      employerName = secureGCMCipher.decrypt[String](e.employerName.value, e.employerName.nonce),
      employerRef = e.employerRef.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      startDate = e.startDate.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      payrollId = e.payrollId.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      cessationDateQuestion = e.cessationDateQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      cessationDate = e.cessationDate.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      dateIgnored = e.dateIgnored.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      employmentSubmittedOn = e.employmentSubmittedOn.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      employmentDetailsSubmittedOn = e.employmentDetailsSubmittedOn.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      taxablePayToDate = e.taxablePayToDate.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      totalTaxToDate = e.totalTaxToDate.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      currentDataIsHmrcHeld = secureGCMCipher.decrypt[Boolean](e.currentDataIsHmrcHeld.value, e.currentDataIsHmrcHeld.nonce)
    )
  }

  private def decryptEmployment(employment: EncryptedEmploymentCYAModel)(implicit textAndKey: TextAndKey): EmploymentCYAModel = {
    EmploymentCYAModel(
      employmentDetails = decryptEmploymentDetails(employment.employmentDetails),
      employmentBenefits = employment.employmentBenefits.map(decryptEmploymentBenefits),
      studentLoans = employment.studentLoansCYAModel.map(decryptStudentLoansCYAModel)
    )
  }

  def decryptUserData(userData: EncryptedEmploymentUserData): EmploymentUserData = {

    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    EmploymentUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      employmentId = userData.employmentId,
      isPriorSubmission = userData.isPriorSubmission,
      hasPriorBenefits = userData.hasPriorBenefits,
      hasPriorStudentLoans = userData.hasPriorStudentLoans,
      employment = decryptEmployment(userData.employment),
      lastUpdated = userData.lastUpdated
    )
  }
}
