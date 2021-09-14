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

package services

import config.AppConfig
import javax.inject.Inject
import models.employment.{BenefitsViewModel, CarVanFuelModel, EncryptedBenefitsViewModel, EncryptedCarVanFuelModel, EncryptedExpenses, Expenses}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData, EncryptedEmploymentCYAModel, EncryptedEmploymentDetails,
  EncryptedEmploymentUserData, EncryptedExpensesCYAModel, EncryptedExpensesUserData, ExpensesCYAModel, ExpensesUserData, TextAndKey}
import utils.SecureGCMCipher

class EncryptionService @Inject()(encryptionService: SecureGCMCipher, appConfig: AppConfig) {

  def encryptUserData(userData: EmploymentUserData): EncryptedEmploymentUserData ={
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId,appConfig.encryptionKey)

    EncryptedEmploymentUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      employmentId = userData.employmentId,
      isPriorSubmission = userData.isPriorSubmission,
      employment = encryptEmployment(userData.employment),
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptEmployment(employment: EmploymentCYAModel)(implicit textAndKey: TextAndKey): EncryptedEmploymentCYAModel ={

    EncryptedEmploymentCYAModel(
      employmentDetails = encryptEmploymentDetails(employment.employmentDetails),
      employmentBenefits = employment.employmentBenefits.map(encryptEmploymentBenefits)
    )
  }

  private def encryptEmploymentDetails(e: EmploymentDetails)(implicit textAndKey: TextAndKey): EncryptedEmploymentDetails ={

    EncryptedEmploymentDetails(
      employerName = encryptionService.encrypt(e.employerName),
      employerRef = e.employerRef.map(encryptionService.encrypt),
      startDate = e.startDate.map(encryptionService.encrypt),
      payrollId = e.payrollId.map(encryptionService.encrypt),
      cessationDateQuestion = e.cessationDateQuestion.map(encryptionService.encrypt),
      cessationDate = e.cessationDate.map(encryptionService.encrypt),
      dateIgnored = e.dateIgnored.map(encryptionService.encrypt),
      employmentSubmittedOn = e.employmentSubmittedOn.map(encryptionService.encrypt),
      employmentDetailsSubmittedOn = e.employmentDetailsSubmittedOn.map(encryptionService.encrypt),
      taxablePayToDate = e.taxablePayToDate.map(encryptionService.encrypt),
      totalTaxToDate = e.totalTaxToDate.map(encryptionService.encrypt),
      currentDataIsHmrcHeld = encryptionService.encrypt(e.currentDataIsHmrcHeld)
    )
  }

  private def encryptCarVanFuelModel(carVanFuelModel: CarVanFuelModel)(implicit textAndKey: TextAndKey): EncryptedCarVanFuelModel ={
    EncryptedCarVanFuelModel(
      carVanFuelQuestion = carVanFuelModel.carVanFuelQuestion.map(encryptionService.encrypt),
      carQuestion = carVanFuelModel.carQuestion.map(encryptionService.encrypt),
      car = carVanFuelModel.car.map(encryptionService.encrypt),
      carFuelQuestion = carVanFuelModel.carFuelQuestion.map(encryptionService.encrypt),
      carFuel = carVanFuelModel.carFuel.map(encryptionService.encrypt),
      vanQuestion = carVanFuelModel.vanQuestion.map(encryptionService.encrypt),
      van = carVanFuelModel.van.map(encryptionService.encrypt),
      vanFuelQuestion = carVanFuelModel.vanFuelQuestion.map(encryptionService.encrypt),
      vanFuel = carVanFuelModel.vanFuel.map(encryptionService.encrypt),
      mileageQuestion = carVanFuelModel.mileageQuestion.map(encryptionService.encrypt),
      mileage = carVanFuelModel.mileage.map(encryptionService.encrypt)
    )
  }

  private def decryptCarVanFuelModel(carVanFuelModel: EncryptedCarVanFuelModel)(implicit textAndKey: TextAndKey): CarVanFuelModel ={
    CarVanFuelModel(
      carVanFuelQuestion = carVanFuelModel.carVanFuelQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      carQuestion = carVanFuelModel.carQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      car = carVanFuelModel.car.map(x => encryptionService.decrypt(x.value,x.nonce)),
      carFuelQuestion = carVanFuelModel.carFuelQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      carFuel = carVanFuelModel.carFuel.map(x => encryptionService.decrypt(x.value,x.nonce)),
      vanQuestion = carVanFuelModel.vanQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      van = carVanFuelModel.van.map(x => encryptionService.decrypt(x.value,x.nonce)),
      vanFuelQuestion = carVanFuelModel.vanFuelQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      vanFuel = carVanFuelModel.vanFuel.map(x => encryptionService.decrypt(x.value,x.nonce)),
      mileageQuestion = carVanFuelModel.mileageQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      mileage = carVanFuelModel.mileage.map(x => encryptionService.decrypt(x.value,x.nonce))
    )
  }

  //scalastyle:off
  private def encryptEmploymentBenefits(e: BenefitsViewModel)(implicit textAndKey: TextAndKey): EncryptedBenefitsViewModel ={

    EncryptedBenefitsViewModel(
      carVanFuelModel = e.carVanFuelModel.map(encryptCarVanFuelModel),
      accommodation = e.accommodation.map(encryptionService.encrypt),
      assets = e.assets.map(encryptionService.encrypt),
      assetTransfer = e.assetTransfer.map(encryptionService.encrypt),
      beneficialLoan = e.beneficialLoan.map(encryptionService.encrypt),
      educationalServices = e.educationalServices.map(encryptionService.encrypt),
      entertaining = e.entertaining.map(encryptionService.encrypt),
      expenses = e.expenses.map(encryptionService.encrypt),
      medicalInsurance = e.medicalInsurance.map(encryptionService.encrypt),
      telephone = e.telephone.map(encryptionService.encrypt),
      service = e.service.map(encryptionService.encrypt),
      taxableExpenses = e.taxableExpenses.map(encryptionService.encrypt),
      nonQualifyingRelocationExpenses = e.nonQualifyingRelocationExpenses.map(encryptionService.encrypt),
      nurseryPlaces = e.nurseryPlaces.map(encryptionService.encrypt),
      otherItems = e.otherItems.map(encryptionService.encrypt),
      paymentsOnEmployeesBehalf = e.paymentsOnEmployeesBehalf.map(encryptionService.encrypt),
      personalIncidentalExpenses = e.personalIncidentalExpenses.map(encryptionService.encrypt),
      qualifyingRelocationExpenses = e.qualifyingRelocationExpenses.map(encryptionService.encrypt),
      employerProvidedProfessionalSubscriptions = e.employerProvidedProfessionalSubscriptions.map(encryptionService.encrypt),
      employerProvidedServices = e.employerProvidedServices.map(encryptionService.encrypt),
      incomeTaxPaidByDirector = e.incomeTaxPaidByDirector.map(encryptionService.encrypt),
      travelAndSubsistence = e.travelAndSubsistence.map(encryptionService.encrypt),
      vouchersAndCreditCards = e.vouchersAndCreditCards.map(encryptionService.encrypt),
      nonCash = e.nonCash.map(encryptionService.encrypt),
      accommodationQuestion = e.accommodationQuestion.map(encryptionService.encrypt),
      assetsQuestion = e.assetsQuestion.map(encryptionService.encrypt),
      assetTransferQuestion = e.assetTransferQuestion.map(encryptionService.encrypt),
      beneficialLoanQuestion = e.beneficialLoanQuestion.map(encryptionService.encrypt),
      educationalServicesQuestion = e.educationalServicesQuestion.map(encryptionService.encrypt),
      entertainingQuestion = e.entertainingQuestion.map(encryptionService.encrypt),
      expensesQuestion = e.expensesQuestion.map(encryptionService.encrypt),
      medicalInsuranceQuestion = e.medicalInsuranceQuestion.map(encryptionService.encrypt),
      telephoneQuestion = e.telephoneQuestion.map(encryptionService.encrypt),
      serviceQuestion = e.serviceQuestion.map(encryptionService.encrypt),
      taxableExpensesQuestion = e.taxableExpensesQuestion.map(encryptionService.encrypt),
      nonQualifyingRelocationExpensesQuestion = e.nonQualifyingRelocationExpensesQuestion.map(encryptionService.encrypt),
      nurseryPlacesQuestion = e.nurseryPlacesQuestion.map(encryptionService.encrypt),
      otherItemsQuestion = e.otherItemsQuestion.map(encryptionService.encrypt),
      paymentsOnEmployeesBehalfQuestion = e.paymentsOnEmployeesBehalfQuestion.map(encryptionService.encrypt),
      personalIncidentalExpensesQuestion = e.personalIncidentalExpensesQuestion.map(encryptionService.encrypt),
      qualifyingRelocationExpensesQuestion = e.qualifyingRelocationExpensesQuestion.map(encryptionService.encrypt),
      employerProvidedProfessionalSubscriptionsQuestion = e.employerProvidedProfessionalSubscriptionsQuestion.map(encryptionService.encrypt),
      employerProvidedServicesQuestion = e.employerProvidedServicesQuestion.map(encryptionService.encrypt),
      incomeTaxPaidByDirectorQuestion = e.incomeTaxPaidByDirectorQuestion.map(encryptionService.encrypt),
      travelAndSubsistenceQuestion = e.travelAndSubsistenceQuestion.map(encryptionService.encrypt),
      vouchersAndCreditCardsQuestion = e.vouchersAndCreditCardsQuestion.map(encryptionService.encrypt),
      nonCashQuestion = e.nonCashQuestion.map(encryptionService.encrypt),
      submittedOn = e.submittedOn.map(encryptionService.encrypt),
      isUsingCustomerData = encryptionService.encrypt(e.isUsingCustomerData),
      isBenefitsReceived = encryptionService.encrypt(e.isBenefitsReceived)
    )
  }

  private def decryptEmploymentBenefits(e: EncryptedBenefitsViewModel)(implicit textAndKey: TextAndKey): BenefitsViewModel ={
    BenefitsViewModel(
      carVanFuelModel = e.carVanFuelModel.map(decryptCarVanFuelModel),
      accommodation = e.accommodation.map(x => encryptionService.decrypt(x.value,x.nonce)),
      assets = e.assets.map(x => encryptionService.decrypt(x.value,x.nonce)),
      assetTransfer = e.assetTransfer.map(x => encryptionService.decrypt(x.value,x.nonce)),
      beneficialLoan = e.beneficialLoan.map(x => encryptionService.decrypt(x.value,x.nonce)),
      educationalServices = e.educationalServices.map(x => encryptionService.decrypt(x.value,x.nonce)),
      entertaining = e.entertaining.map(x => encryptionService.decrypt(x.value,x.nonce)),
      expenses = e.expenses.map(x => encryptionService.decrypt(x.value,x.nonce)),
      medicalInsurance = e.medicalInsurance.map(x => encryptionService.decrypt(x.value,x.nonce)),
      telephone = e.telephone.map(x => encryptionService.decrypt(x.value,x.nonce)),
      service = e.service.map(x => encryptionService.decrypt(x.value,x.nonce)),
      taxableExpenses = e.taxableExpenses.map(x => encryptionService.decrypt(x.value,x.nonce)),
      nonQualifyingRelocationExpenses = e.nonQualifyingRelocationExpenses.map(x => encryptionService.decrypt(x.value,x.nonce)),
      nurseryPlaces = e.nurseryPlaces.map(x => encryptionService.decrypt(x.value,x.nonce)),
      otherItems = e.otherItems.map(x => encryptionService.decrypt(x.value,x.nonce)),
      paymentsOnEmployeesBehalf = e.paymentsOnEmployeesBehalf.map(x => encryptionService.decrypt(x.value,x.nonce)),
      personalIncidentalExpenses = e.personalIncidentalExpenses.map(x => encryptionService.decrypt(x.value,x.nonce)),
      qualifyingRelocationExpenses = e.qualifyingRelocationExpenses.map(x => encryptionService.decrypt(x.value,x.nonce)),
      employerProvidedProfessionalSubscriptions = e.employerProvidedProfessionalSubscriptions.map(x => encryptionService.decrypt(x.value,x.nonce)),
      employerProvidedServices = e.employerProvidedServices.map(x => encryptionService.decrypt(x.value,x.nonce)),
      incomeTaxPaidByDirector = e.incomeTaxPaidByDirector.map(x => encryptionService.decrypt(x.value,x.nonce)),
      travelAndSubsistence = e.travelAndSubsistence.map(x => encryptionService.decrypt(x.value,x.nonce)),
      vouchersAndCreditCards = e.vouchersAndCreditCards.map(x => encryptionService.decrypt(x.value,x.nonce)),
      nonCash = e.nonCash.map(x => encryptionService.decrypt(x.value,x.nonce)),
      accommodationQuestion = e.accommodationQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      assetsQuestion = e.assetsQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      assetTransferQuestion = e.assetTransferQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      beneficialLoanQuestion = e.beneficialLoanQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      educationalServicesQuestion = e.educationalServicesQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      entertainingQuestion = e.entertainingQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      expensesQuestion = e.expensesQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      medicalInsuranceQuestion = e.medicalInsuranceQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      telephoneQuestion = e.telephoneQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      serviceQuestion = e.serviceQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      taxableExpensesQuestion = e.taxableExpensesQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      nonQualifyingRelocationExpensesQuestion = e.nonQualifyingRelocationExpensesQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      nurseryPlacesQuestion = e.nurseryPlacesQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      otherItemsQuestion = e.otherItemsQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      paymentsOnEmployeesBehalfQuestion = e.paymentsOnEmployeesBehalfQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      personalIncidentalExpensesQuestion = e.personalIncidentalExpensesQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      qualifyingRelocationExpensesQuestion = e.qualifyingRelocationExpensesQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      employerProvidedProfessionalSubscriptionsQuestion = e.employerProvidedProfessionalSubscriptionsQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      employerProvidedServicesQuestion = e.employerProvidedServicesQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      incomeTaxPaidByDirectorQuestion = e.incomeTaxPaidByDirectorQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      travelAndSubsistenceQuestion = e.travelAndSubsistenceQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      vouchersAndCreditCardsQuestion = e.vouchersAndCreditCardsQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      nonCashQuestion = e.nonCashQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      submittedOn = e.submittedOn.map(x => encryptionService.decrypt(x.value,x.nonce)),
      isUsingCustomerData = encryptionService.decrypt(e.isUsingCustomerData.value, e.isUsingCustomerData.nonce),
      isBenefitsReceived = encryptionService.decrypt(e.isBenefitsReceived.value, e.isBenefitsReceived.nonce)
    )
  }

  private def encryptExpenses(userData: ExpensesUserData)(implicit textAndKey: TextAndKey): EncryptedExpensesUserData ={

    val expenses = EncryptedExpenses(
      businessTravelCosts = userData.expensesCya.expenses.businessTravelCosts.map(encryptionService.encrypt),
      jobExpenses = userData.expensesCya.expenses.jobExpenses.map(encryptionService.encrypt),
      flatRateJobExpenses = userData.expensesCya.expenses.flatRateJobExpenses.map(encryptionService.encrypt),
      professionalSubscriptions = userData.expensesCya.expenses.professionalSubscriptions.map(encryptionService.encrypt),
      hotelAndMealExpenses = userData.expensesCya.expenses.hotelAndMealExpenses.map(encryptionService.encrypt),
      otherAndCapitalAllowances = userData.expensesCya.expenses.otherAndCapitalAllowances.map(encryptionService.encrypt),
      vehicleExpenses = userData.expensesCya.expenses.vehicleExpenses.map(encryptionService.encrypt),
      mileageAllowanceRelief = userData.expensesCya.expenses.mileageAllowanceRelief.map(encryptionService.encrypt)
    )

    val expensesCYA = EncryptedExpensesCYAModel(
      expenses = expenses, currentDataIsHmrcHeld = encryptionService.encrypt(userData.expensesCya.currentDataIsHmrcHeld)
    )

    EncryptedExpensesUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      isPriorSubmission = userData.isPriorSubmission,
      expensesCya = expensesCYA,
      lastUpdated = userData.lastUpdated
    )
  }

  private def decryptExpenses(userData: EncryptedExpensesUserData)(implicit textAndKey: TextAndKey): ExpensesUserData ={

    val expenses = Expenses(
      businessTravelCosts = userData.expensesCya.expenses.businessTravelCosts.map(x => encryptionService.decrypt(x.value,x.nonce)),
      jobExpenses = userData.expensesCya.expenses.jobExpenses.map(x => encryptionService.decrypt(x.value,x.nonce)),
      flatRateJobExpenses = userData.expensesCya.expenses.flatRateJobExpenses.map(x => encryptionService.decrypt(x.value,x.nonce)),
      professionalSubscriptions = userData.expensesCya.expenses.professionalSubscriptions.map(x => encryptionService.decrypt(x.value,x.nonce)),
      hotelAndMealExpenses = userData.expensesCya.expenses.hotelAndMealExpenses.map(x => encryptionService.decrypt(x.value,x.nonce)),
      otherAndCapitalAllowances = userData.expensesCya.expenses.otherAndCapitalAllowances.map(x => encryptionService.decrypt(x.value,x.nonce)),
      vehicleExpenses = userData.expensesCya.expenses.vehicleExpenses.map(x => encryptionService.decrypt(x.value,x.nonce)),
      mileageAllowanceRelief = userData.expensesCya.expenses.mileageAllowanceRelief.map(x => encryptionService.decrypt(x.value,x.nonce))
    )

    val expensesCYA = ExpensesCYAModel(
      expenses = expenses, currentDataIsHmrcHeld = encryptionService.decrypt(userData.expensesCya.currentDataIsHmrcHeld.value, userData.expensesCya.currentDataIsHmrcHeld.nonce)
    )

    ExpensesUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      isPriorSubmission = userData.isPriorSubmission,
      expensesCya = expensesCYA,
      lastUpdated = userData.lastUpdated
    )
  }

  private def decryptEmploymentDetails(e: EncryptedEmploymentDetails)(implicit textAndKey: TextAndKey): EmploymentDetails ={
    EmploymentDetails(
      employerName = encryptionService.decrypt(e.employerName.value, e.employerName.nonce),
      employerRef = e.employerRef.map(x => encryptionService.decrypt(x.value,x.nonce)),
      startDate = e.startDate.map(x => encryptionService.decrypt(x.value,x.nonce)),
      payrollId = e.payrollId.map(x => encryptionService.decrypt(x.value,x.nonce)),
      cessationDateQuestion = e.cessationDateQuestion.map(x => encryptionService.decrypt(x.value,x.nonce)),
      cessationDate = e.cessationDate.map(x => encryptionService.decrypt(x.value,x.nonce)),
      dateIgnored = e.dateIgnored.map(x => encryptionService.decrypt(x.value,x.nonce)),
      employmentSubmittedOn = e.employmentSubmittedOn.map(x => encryptionService.decrypt(x.value,x.nonce)),
      employmentDetailsSubmittedOn = e.employmentDetailsSubmittedOn.map(x => encryptionService.decrypt(x.value,x.nonce)),
      taxablePayToDate = e.taxablePayToDate.map(x => encryptionService.decrypt(x.value,x.nonce)),
      totalTaxToDate = e.totalTaxToDate.map(x => encryptionService.decrypt(x.value,x.nonce)),
      currentDataIsHmrcHeld = encryptionService.decrypt(e.currentDataIsHmrcHeld.value, e.currentDataIsHmrcHeld.nonce)
    )
  }

  private def decryptEmployment(employment: EncryptedEmploymentCYAModel)(implicit textAndKey: TextAndKey): EmploymentCYAModel ={

    EmploymentCYAModel(
      employmentDetails = decryptEmploymentDetails(employment.employmentDetails),
      employmentBenefits = employment.employmentBenefits.map(decryptEmploymentBenefits)
    )
  }

  def decryptUserData(userData: EncryptedEmploymentUserData ): EmploymentUserData ={

    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId,appConfig.encryptionKey)

    EmploymentUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      employmentId = userData.employmentId,
      isPriorSubmission = userData.isPriorSubmission,
      employment = decryptEmployment(userData.employment),
      lastUpdated = userData.lastUpdated
    )
  }
}
