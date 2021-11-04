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
import models.employment._
import models.mongo._
import utils.SecureGCMCipher
import javax.inject.Inject
import models.benefits.{AccommodationRelocationModel, BenefitsViewModel, CarVanFuelModel, EncryptedAccommodationRelocationModel, EncryptedBenefitsViewModel, EncryptedCarVanFuelModel, EncryptedTravelEntertainmentModel, EncryptedUtilitiesAndServicesModel, TravelEntertainmentModel, UtilitiesAndServicesModel}

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
      employment = encryptEmployment(userData.employment),
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptEmployment(employment: EmploymentCYAModel)(implicit textAndKey: TextAndKey): EncryptedEmploymentCYAModel = {
    EncryptedEmploymentCYAModel(
      employmentDetails = encryptEmploymentDetails(employment.employmentDetails),
      employmentBenefits = employment.employmentBenefits.map(encryptEmploymentBenefits)
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
      carVanFuelQuestion = carVanFuelModel.carVanFuelQuestion.map(secureGCMCipher.encrypt),
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
      carVanFuelQuestion = carVanFuelModel.carVanFuelQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
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
                                                 (implicit textAndKey: TextAndKey):EncryptedAccommodationRelocationModel = {
    EncryptedAccommodationRelocationModel(
      accommodationRelocationQuestion = accommodationRelocationModel.accommodationRelocationQuestion.map(secureGCMCipher.encrypt),
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
      accommodationRelocationQuestion = accommodationRelocationModel.accommodationRelocationQuestion.map(x =>
        secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      accommodationQuestion = accommodationRelocationModel.accommodationQuestion.map(x =>
        secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      accommodation = accommodationRelocationModel.accommodation.map(x =>
        secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      qualifyingRelocationExpensesQuestion = accommodationRelocationModel.qualifyingRelocationExpensesQuestion.map(x =>
        secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      qualifyingRelocationExpenses = accommodationRelocationModel.qualifyingRelocationExpenses.map(x =>
        secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      nonQualifyingRelocationExpensesQuestion = accommodationRelocationModel.nonQualifyingRelocationExpensesQuestion.map(x =>
        secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      nonQualifyingRelocationExpenses = accommodationRelocationModel.nonQualifyingRelocationExpenses.map(x =>
        secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def encryptTravelEntertainmentModel(travelEntertainmentModel: TravelEntertainmentModel)(implicit textAndKey: TextAndKey):
  EncryptedTravelEntertainmentModel = {
    EncryptedTravelEntertainmentModel(
      travelEntertainmentQuestion = travelEntertainmentModel.travelEntertainmentQuestion.map(secureGCMCipher.encrypt),
      travelAndSubsistenceQuestion = travelEntertainmentModel.travelAndSubsistenceQuestion.map(secureGCMCipher.encrypt),
      travelAndSubsistence = travelEntertainmentModel.travelAndSubsistence.map(secureGCMCipher.encrypt),
      personalIncidentalExpensesQuestion = travelEntertainmentModel.personalIncidentalExpensesQuestion.map(secureGCMCipher.encrypt),
      personalIncidentalExpenses = travelEntertainmentModel.personalIncidentalExpenses.map(secureGCMCipher.encrypt),
      entertainingQuestion = travelEntertainmentModel.entertainingQuestion.map(secureGCMCipher.encrypt),
      entertaining = travelEntertainmentModel.entertaining.map(secureGCMCipher.encrypt)
    )
  }

  private def encryptUtilitiesAndServicesModel(utilitiesAndServicesModel: UtilitiesAndServicesModel)(implicit textAndKey: TextAndKey):
  EncryptedUtilitiesAndServicesModel = {
    EncryptedUtilitiesAndServicesModel(
      utilitiesAndServicesQuestion = utilitiesAndServicesModel.utilitiesAndServicesQuestion.map(secureGCMCipher.encrypt),
      telephoneQuestion = utilitiesAndServicesModel.telephoneQuestion.map(secureGCMCipher.encrypt),
      telephone = utilitiesAndServicesModel.telephone.map(secureGCMCipher.encrypt),
      employerProvidedServicesQuestion = utilitiesAndServicesModel.employerProvidedServicesQuestion.map(secureGCMCipher.encrypt),
      employerProvidedServices = utilitiesAndServicesModel.employerProvidedServices.map(secureGCMCipher.encrypt),
      employerProvidedProfessionalSubscriptionsQuestion = utilitiesAndServicesModel.employerProvidedProfessionalSubscriptionsQuestion.map(
        secureGCMCipher.encrypt),
      employerProvidedProfessionalSubscriptions = utilitiesAndServicesModel.employerProvidedProfessionalSubscriptions.map(secureGCMCipher.encrypt),
      serviceQuestion = utilitiesAndServicesModel.serviceQuestion.map(secureGCMCipher.encrypt),
      service = utilitiesAndServicesModel.service.map(secureGCMCipher.encrypt)
    )
  }

  private def decryptTravelEntertainmentModel(travelEntertainmentModel: EncryptedTravelEntertainmentModel)(implicit textAndKey: TextAndKey):
  TravelEntertainmentModel = {
    TravelEntertainmentModel(
      travelEntertainmentQuestion = travelEntertainmentModel.travelEntertainmentQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      travelAndSubsistenceQuestion = travelEntertainmentModel.travelAndSubsistenceQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      travelAndSubsistence = travelEntertainmentModel.travelAndSubsistence.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      personalIncidentalExpensesQuestion =
        travelEntertainmentModel.personalIncidentalExpensesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      personalIncidentalExpenses = travelEntertainmentModel.personalIncidentalExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      entertainingQuestion = travelEntertainmentModel.entertainingQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      entertaining = travelEntertainmentModel.entertaining.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptUtilitiesAndServicesModel(utilitiesAndServicesModel: EncryptedUtilitiesAndServicesModel)(implicit textAndKey: TextAndKey):
  UtilitiesAndServicesModel = {
    UtilitiesAndServicesModel(
      utilitiesAndServicesQuestion = utilitiesAndServicesModel.utilitiesAndServicesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value,x.nonce)),
      telephoneQuestion = utilitiesAndServicesModel.telephoneQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value,x.nonce)),
      telephone = utilitiesAndServicesModel.telephone.map(x => secureGCMCipher.decrypt[BigDecimal](x.value,x.nonce)),
      employerProvidedServicesQuestion = utilitiesAndServicesModel.employerProvidedServicesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value,x.nonce)),
      employerProvidedServices = utilitiesAndServicesModel.employerProvidedServices.map(x => secureGCMCipher.decrypt[BigDecimal](x.value,x.nonce)),
      employerProvidedProfessionalSubscriptionsQuestion = utilitiesAndServicesModel.employerProvidedProfessionalSubscriptionsQuestion.map(
        x => secureGCMCipher.decrypt[Boolean](x.value,x.nonce)),
      employerProvidedProfessionalSubscriptions = utilitiesAndServicesModel.employerProvidedProfessionalSubscriptions.map(
        x => secureGCMCipher.decrypt[BigDecimal](x.value,x.nonce)),
      serviceQuestion = utilitiesAndServicesModel.serviceQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value,x.nonce)),
      service = utilitiesAndServicesModel.service.map(x => secureGCMCipher.decrypt[BigDecimal](x.value,x.nonce))
    )
  }

  //scalastyle:off
  private def encryptEmploymentBenefits(e: BenefitsViewModel)(implicit textAndKey: TextAndKey): EncryptedBenefitsViewModel = {

    EncryptedBenefitsViewModel(
      carVanFuelModel = e.carVanFuelModel.map(encryptCarVanFuelModel),
      accommodationRelocationModel = e.accommodationRelocationModel.map(encryptAccommodationRelocationModel),
      travelEntertainmentModel = e.travelEntertainmentModel.map(encryptTravelEntertainmentModel),
      utilitiesAndServicesModel = e.utilitiesAndServicesModel.map(encryptUtilitiesAndServicesModel),
      assets = e.assets.map(secureGCMCipher.encrypt),
      assetTransfer = e.assetTransfer.map(secureGCMCipher.encrypt),
      beneficialLoan = e.beneficialLoan.map(secureGCMCipher.encrypt),
      educationalServices = e.educationalServices.map(secureGCMCipher.encrypt),
      expenses = e.expenses.map(secureGCMCipher.encrypt),
      medicalInsurance = e.medicalInsurance.map(secureGCMCipher.encrypt),
      taxableExpenses = e.taxableExpenses.map(secureGCMCipher.encrypt),
      nurseryPlaces = e.nurseryPlaces.map(secureGCMCipher.encrypt),
      otherItems = e.otherItems.map(secureGCMCipher.encrypt),
      paymentsOnEmployeesBehalf = e.paymentsOnEmployeesBehalf.map(secureGCMCipher.encrypt),
      incomeTaxPaidByDirector = e.incomeTaxPaidByDirector.map(secureGCMCipher.encrypt),
      vouchersAndCreditCards = e.vouchersAndCreditCards.map(secureGCMCipher.encrypt),
      nonCash = e.nonCash.map(secureGCMCipher.encrypt),
      assetsQuestion = e.assetsQuestion.map(secureGCMCipher.encrypt),
      assetTransferQuestion = e.assetTransferQuestion.map(secureGCMCipher.encrypt),
      beneficialLoanQuestion = e.beneficialLoanQuestion.map(secureGCMCipher.encrypt),
      educationalServicesQuestion = e.educationalServicesQuestion.map(secureGCMCipher.encrypt),
      expensesQuestion = e.expensesQuestion.map(secureGCMCipher.encrypt),
      medicalInsuranceQuestion = e.medicalInsuranceQuestion.map(secureGCMCipher.encrypt),
      taxableExpensesQuestion = e.taxableExpensesQuestion.map(secureGCMCipher.encrypt),
      nurseryPlacesQuestion = e.nurseryPlacesQuestion.map(secureGCMCipher.encrypt),
      otherItemsQuestion = e.otherItemsQuestion.map(secureGCMCipher.encrypt),
      paymentsOnEmployeesBehalfQuestion = e.paymentsOnEmployeesBehalfQuestion.map(secureGCMCipher.encrypt),
      incomeTaxPaidByDirectorQuestion = e.incomeTaxPaidByDirectorQuestion.map(secureGCMCipher.encrypt),
      vouchersAndCreditCardsQuestion = e.vouchersAndCreditCardsQuestion.map(secureGCMCipher.encrypt),
      nonCashQuestion = e.nonCashQuestion.map(secureGCMCipher.encrypt),
      submittedOn = e.submittedOn.map(secureGCMCipher.encrypt),
      isUsingCustomerData = secureGCMCipher.encrypt(e.isUsingCustomerData),
      isBenefitsReceived = secureGCMCipher.encrypt(e.isBenefitsReceived)
    )
  }

  private def decryptEmploymentBenefits(e: EncryptedBenefitsViewModel)(implicit textAndKey: TextAndKey): BenefitsViewModel ={
    BenefitsViewModel(
      carVanFuelModel = e.carVanFuelModel.map(decryptCarVanFuelModel),
      accommodationRelocationModel = e.accommodationRelocationModel.map(decryptAccommodationRelocationModel),
      travelEntertainmentModel = e.travelEntertainmentModel.map(decryptTravelEntertainmentModel),
      utilitiesAndServicesModel = e.utilitiesAndServicesModel.map(decryptUtilitiesAndServicesModel),
      assets = e.assets.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      assetTransfer = e.assetTransfer.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      beneficialLoan = e.beneficialLoan.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      educationalServices = e.educationalServices.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      expenses = e.expenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      medicalInsurance = e.medicalInsurance.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      taxableExpenses = e.taxableExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      nurseryPlaces = e.nurseryPlaces.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      otherItems = e.otherItems.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      paymentsOnEmployeesBehalf = e.paymentsOnEmployeesBehalf.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      incomeTaxPaidByDirector = e.incomeTaxPaidByDirector.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      vouchersAndCreditCards = e.vouchersAndCreditCards.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      nonCash = e.nonCash.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      assetsQuestion = e.assetsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      assetTransferQuestion = e.assetTransferQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      beneficialLoanQuestion = e.beneficialLoanQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      educationalServicesQuestion = e.educationalServicesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      expensesQuestion = e.expensesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      medicalInsuranceQuestion = e.medicalInsuranceQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      taxableExpensesQuestion = e.taxableExpensesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      nurseryPlacesQuestion = e.nurseryPlacesQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      otherItemsQuestion = e.otherItemsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      paymentsOnEmployeesBehalfQuestion = e.paymentsOnEmployeesBehalfQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      incomeTaxPaidByDirectorQuestion = e.incomeTaxPaidByDirectorQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      vouchersAndCreditCardsQuestion = e.vouchersAndCreditCardsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      nonCashQuestion = e.nonCashQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      submittedOn = e.submittedOn.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      isUsingCustomerData = secureGCMCipher.decrypt[Boolean](e.isUsingCustomerData.value, e.isUsingCustomerData.nonce),
      isBenefitsReceived = secureGCMCipher.decrypt[Boolean](e.isBenefitsReceived.value, e.isBenefitsReceived.nonce)
    )
  }

  def encryptExpenses(userData: ExpensesUserData): EncryptedExpensesUserData = {

    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    val expenses = EncryptedExpenses(
      businessTravelCosts = userData.expensesCya.expenses.businessTravelCosts.map(secureGCMCipher.encrypt),
      jobExpenses = userData.expensesCya.expenses.jobExpenses.map(secureGCMCipher.encrypt),
      flatRateJobExpenses = userData.expensesCya.expenses.flatRateJobExpenses.map(secureGCMCipher.encrypt),
      professionalSubscriptions = userData.expensesCya.expenses.professionalSubscriptions.map(secureGCMCipher.encrypt),
      hotelAndMealExpenses = userData.expensesCya.expenses.hotelAndMealExpenses.map(secureGCMCipher.encrypt),
      otherAndCapitalAllowances = userData.expensesCya.expenses.otherAndCapitalAllowances.map(secureGCMCipher.encrypt),
      vehicleExpenses = userData.expensesCya.expenses.vehicleExpenses.map(secureGCMCipher.encrypt),
      mileageAllowanceRelief = userData.expensesCya.expenses.mileageAllowanceRelief.map(secureGCMCipher.encrypt)
    )

    val expensesCYA = EncryptedExpensesCYAModel(
      expenses = expenses, currentDataIsHmrcHeld = secureGCMCipher.encrypt(userData.expensesCya.currentDataIsHmrcHeld)
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

  def decryptExpenses(userData: EncryptedExpensesUserData): ExpensesUserData = {

    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    val expenses = Expenses(
      businessTravelCosts = userData.expensesCya.expenses.businessTravelCosts.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      jobExpenses = userData.expensesCya.expenses.jobExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      flatRateJobExpenses = userData.expensesCya.expenses.flatRateJobExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      professionalSubscriptions = userData.expensesCya.expenses.professionalSubscriptions.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      hotelAndMealExpenses = userData.expensesCya.expenses.hotelAndMealExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      otherAndCapitalAllowances = userData.expensesCya.expenses.otherAndCapitalAllowances.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      vehicleExpenses = userData.expensesCya.expenses.vehicleExpenses.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      mileageAllowanceRelief = userData.expensesCya.expenses.mileageAllowanceRelief.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )

    val expensesCYA = ExpensesCYAModel(
      expenses = expenses, currentDataIsHmrcHeld = secureGCMCipher.decrypt[Boolean](userData.expensesCya.currentDataIsHmrcHeld.value, userData.expensesCya.currentDataIsHmrcHeld.nonce)
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
      employmentBenefits = employment.employmentBenefits.map(decryptEmploymentBenefits)
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
      employment = decryptEmployment(userData.employment),
      lastUpdated = userData.lastUpdated
    )
  }
}
