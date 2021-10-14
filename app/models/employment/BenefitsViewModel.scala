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

package models.employment

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OFormat, __}
import utils.EncryptedValue

case class BenefitsViewModel(
                        carVanFuelModel: Option[CarVanFuelModel] = None,
                        accommodationRelocationModel: Option[AccommodationRelocationModel] = None,
                        assets: Option[BigDecimal] = None,
                        assetTransfer: Option[BigDecimal] = None,
                        beneficialLoan: Option[BigDecimal] = None,
                        educationalServices: Option[BigDecimal] = None,
                        entertaining: Option[BigDecimal] = None,
                        expenses: Option[BigDecimal] = None,
                        medicalInsurance: Option[BigDecimal] = None,
                        telephone: Option[BigDecimal] = None,
                        service: Option[BigDecimal] = None,
                        taxableExpenses: Option[BigDecimal] = None,
                        nurseryPlaces: Option[BigDecimal] = None,
                        otherItems: Option[BigDecimal] = None,
                        paymentsOnEmployeesBehalf: Option[BigDecimal] = None,
                        personalIncidentalExpenses: Option[BigDecimal] = None,
                        employerProvidedProfessionalSubscriptions: Option[BigDecimal] = None,
                        employerProvidedServices: Option[BigDecimal] = None,
                        incomeTaxPaidByDirector: Option[BigDecimal] = None,
                        travelAndSubsistence: Option[BigDecimal] = None,
                        vouchersAndCreditCards: Option[BigDecimal] = None,
                        nonCash: Option[BigDecimal] = None,
                        assetsQuestion: Option[Boolean] = None,
                        assetTransferQuestion: Option[Boolean] = None,
                        beneficialLoanQuestion: Option[Boolean] = None,
                        educationalServicesQuestion: Option[Boolean] = None,
                        entertainingQuestion: Option[Boolean] = None,
                        expensesQuestion: Option[Boolean] = None,
                        medicalInsuranceQuestion: Option[Boolean] = None,
                        telephoneQuestion: Option[Boolean] = None,
                        serviceQuestion: Option[Boolean] = None,
                        taxableExpensesQuestion: Option[Boolean] = None,
                        nurseryPlacesQuestion: Option[Boolean] = None,
                        otherItemsQuestion: Option[Boolean] = None,
                        paymentsOnEmployeesBehalfQuestion: Option[Boolean] = None,
                        personalIncidentalExpensesQuestion: Option[Boolean] = None,
                        employerProvidedProfessionalSubscriptionsQuestion: Option[Boolean] = None,
                        employerProvidedServicesQuestion: Option[Boolean] = None,
                        incomeTaxPaidByDirectorQuestion: Option[Boolean] = None,
                        travelAndSubsistenceQuestion: Option[Boolean] = None,
                        vouchersAndCreditCardsQuestion: Option[Boolean] = None,
                        nonCashQuestion: Option[Boolean] = None,
                        submittedOn: Option[String] = None,
                        isUsingCustomerData: Boolean,
                        isBenefitsReceived: Boolean = false
                       ){

  def toBenefits: Benefits ={
    Benefits(
      accommodationRelocationModel.flatMap(_.accommodation), assets, assetTransfer, beneficialLoan, carVanFuelModel.flatMap(_.car),
      carVanFuelModel.flatMap(_.carFuel), educationalServices, entertaining, expenses, medicalInsurance,
      telephone, service, taxableExpenses, carVanFuelModel.flatMap(_.van), carVanFuelModel.flatMap(_.vanFuel),
      carVanFuelModel.flatMap(_.mileage), accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses), nurseryPlaces, otherItems,
      paymentsOnEmployeesBehalf, personalIncidentalExpenses, accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses),
      employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence, vouchersAndCreditCards, nonCash
    )
  }

  val vehicleDetailsPopulated: Boolean =
    carVanFuelModel.flatMap(_.car).isDefined || carVanFuelModel.flatMap(_.carFuel).isDefined ||
    carVanFuelModel.flatMap(_.van).isDefined || carVanFuelModel.flatMap(_.vanFuel).isDefined ||
    carVanFuelModel.flatMap(_.mileage).isDefined

  val accommodationDetailsPopulated: Boolean =
    accommodationRelocationModel.flatMap(_.accommodation).isDefined ||
    accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses).isDefined ||
    accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses).isDefined

  val travelDetailsPopulated: Boolean =
    travelAndSubsistence.isDefined || personalIncidentalExpenses.isDefined || entertaining.isDefined

  val utilitiesDetailsPopulated: Boolean =
    telephone.isDefined || employerProvidedServices.isDefined || employerProvidedProfessionalSubscriptions.isDefined || service.isDefined

  val medicalDetailsPopulated: Boolean =
    medicalInsurance.isDefined || nurseryPlaces.isDefined || beneficialLoan.isDefined || educationalServices.isDefined

  val incomeTaxDetailsPopulated: Boolean =
    incomeTaxPaidByDirector.isDefined || paymentsOnEmployeesBehalf.isDefined

  val reimbursedDetailsPopulated: Boolean =
    expenses.isDefined || taxableExpenses.isDefined || vouchersAndCreditCards.isDefined || nonCash.isDefined || otherItems.isDefined

  val assetsDetailsPopulated: Boolean =
    assets.isDefined || assetTransfer.isDefined

}

object BenefitsViewModel {

  def clear(isUsingCustomerData:Boolean):BenefitsViewModel =
    BenefitsViewModel(isBenefitsReceived = false, isUsingCustomerData = isUsingCustomerData)

  val firstSetOfFields: OFormat[(Option[CarVanFuelModel], Option[AccommodationRelocationModel], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal])] = (
      (__ \ "carVanFuel").formatNullable[CarVanFuelModel] and
      (__ \ "accommodationRelocationModel").formatNullable[AccommodationRelocationModel] and
      (__ \ "assets").formatNullable[BigDecimal] and
      (__ \ "assetTransfer").formatNullable[BigDecimal] and
      (__ \ "beneficialLoan").formatNullable[BigDecimal] and
      (__ \ "educationalServices").formatNullable[BigDecimal] and
      (__ \ "entertaining").formatNullable[BigDecimal] and
      (__ \ "expenses").formatNullable[BigDecimal] and
      (__ \ "medicalInsurance").formatNullable[BigDecimal] and
      (__ \ "telephone").formatNullable[BigDecimal] and
      (__ \ "service").formatNullable[BigDecimal] and
      (__ \ "taxableExpenses").formatNullable[BigDecimal] and
      (__ \ "nurseryPlaces").formatNullable[BigDecimal] and
      (__ \ "otherItems").formatNullable[BigDecimal] and
      (__ \ "paymentsOnEmployeesBehalf").formatNullable[BigDecimal] and
      (__ \ "personalIncidentalExpenses").formatNullable[BigDecimal]
    ).tupled

  val secondSetOfFields: OFormat[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal])] = (
    (__ \ "employerProvidedProfessionalSubscriptions").formatNullable[BigDecimal] and
      (__ \ "employerProvidedServices").formatNullable[BigDecimal] and
      (__ \ "incomeTaxPaidByDirector").formatNullable[BigDecimal] and
      (__ \ "travelAndSubsistence").formatNullable[BigDecimal] and
      (__ \ "vouchersAndCreditCards").formatNullable[BigDecimal] and
      (__ \ "nonCash").formatNullable[BigDecimal]
    ).tupled

  val thirdSetOfFields: OFormat[(Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean])] = (
    (__ \ "assetsQuestion").formatNullable[Boolean] and
      (__ \ "assetTransferQuestion").formatNullable[Boolean] and
      (__ \ "beneficialLoanQuestion").formatNullable[Boolean] and
      (__ \ "educationalServicesQuestion").formatNullable[Boolean] and
      (__ \ "entertainingQuestion").formatNullable[Boolean] and
      (__ \ "expensesQuestion").formatNullable[Boolean] and
      (__ \ "medicalInsuranceQuestion").formatNullable[Boolean] and
      (__ \ "telephoneQuestion").formatNullable[Boolean] and
      (__ \ "serviceQuestion").formatNullable[Boolean] and
      (__ \ "taxableExpensesQuestion").formatNullable[Boolean] and
      (__ \ "nurseryPlacesQuestion").formatNullable[Boolean] and
      (__ \ "otherItemsQuestion").formatNullable[Boolean] and
      (__ \ "paymentsOnEmployeesBehalfQuestion").formatNullable[Boolean] and
      (__ \ "personalIncidentalExpensesQuestion").formatNullable[Boolean]
    ).tupled

  val fourthSetOfFields: OFormat[(Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[Boolean], Option[String], Boolean, Boolean)] = (
    (__ \ "employerProvidedProfessionalSubscriptionsQuestion").formatNullable[Boolean] and
      (__ \ "employerProvidedServicesQuestion").formatNullable[Boolean] and
      (__ \ "incomeTaxPaidByDirectorQuestion").formatNullable[Boolean] and
      (__ \ "travelAndSubsistenceQuestion").formatNullable[Boolean] and
      (__ \ "vouchersAndCreditCardsQuestion").formatNullable[Boolean] and
      (__ \ "nonCashQuestion").formatNullable[Boolean] and
      (__ \ "submittedOn").formatNullable[String] and
      (__ \ "isUsingCustomerData").format[Boolean] and
      (__ \ "isBenefitsReceived").format[Boolean]
    ).tupled

  implicit val format: OFormat[BenefitsViewModel] = {
    (firstSetOfFields and secondSetOfFields and thirdSetOfFields and fourthSetOfFields).apply({
      case (
        (carVanFuelModel, accommodationRelocationModel,assets, assetTransfer, beneficialLoan, educationalServices, entertaining,
        expenses, medicalInsurance, telephone, service, taxableExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses),
        (employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
        vouchersAndCreditCards, nonCash),
        (assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, educationalServicesQuestion,
        entertainingQuestion,
        expensesQuestion, medicalInsuranceQuestion, telephoneQuestion, serviceQuestion, taxableExpensesQuestion,
        nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion, personalIncidentalExpensesQuestion),
        (employerProvidedProfessionalSubscriptionsQuestion, employerProvidedServicesQuestion, incomeTaxPaidByDirectorQuestion, travelAndSubsistenceQuestion,
        vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived)
        ) =>
        BenefitsViewModel(
          carVanFuelModel,accommodationRelocationModel, assets, assetTransfer, beneficialLoan, educationalServices, entertaining, expenses,
          medicalInsurance, telephone, service, taxableExpenses,
          nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses,
          employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
          vouchersAndCreditCards, nonCash,
          assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, educationalServicesQuestion,
          entertainingQuestion,
          expensesQuestion, medicalInsuranceQuestion, telephoneQuestion, serviceQuestion, taxableExpensesQuestion,
          nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion,
          personalIncidentalExpensesQuestion,
          employerProvidedProfessionalSubscriptionsQuestion, employerProvidedServicesQuestion, incomeTaxPaidByDirectorQuestion, travelAndSubsistenceQuestion,
          vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived
        )
    }, {
      benefits =>
        (
          (benefits.carVanFuelModel, benefits.accommodationRelocationModel, benefits.assets, benefits.assetTransfer, benefits.beneficialLoan,
            benefits.educationalServices, benefits.entertaining, benefits.expenses, benefits.medicalInsurance, benefits.telephone,
            benefits.service, benefits.taxableExpenses, benefits.nurseryPlaces, benefits.otherItems, benefits.paymentsOnEmployeesBehalf,
            benefits.personalIncidentalExpenses),
          (benefits.employerProvidedProfessionalSubscriptions, benefits.employerProvidedServices, benefits.incomeTaxPaidByDirector,
            benefits.travelAndSubsistence, benefits.vouchersAndCreditCards, benefits.nonCash),
          (benefits.assetsQuestion, benefits.assetTransferQuestion, benefits.beneficialLoanQuestion,
            benefits.educationalServicesQuestion, benefits.entertainingQuestion, benefits.expensesQuestion,
            benefits.medicalInsuranceQuestion, benefits.telephoneQuestion,
            benefits.serviceQuestion, benefits.taxableExpensesQuestion, benefits.nurseryPlacesQuestion,
            benefits.otherItemsQuestion, benefits.paymentsOnEmployeesBehalfQuestion,
            benefits.personalIncidentalExpensesQuestion),
          (benefits.employerProvidedProfessionalSubscriptionsQuestion, benefits.employerProvidedServicesQuestion, benefits.incomeTaxPaidByDirectorQuestion,
            benefits.travelAndSubsistenceQuestion, benefits.vouchersAndCreditCardsQuestion, benefits.nonCashQuestion,
            benefits.submittedOn, benefits.isUsingCustomerData, benefits.isBenefitsReceived)
        )
    })
  }
}

case class EncryptedBenefitsViewModel(
                        carVanFuelModel: Option[EncryptedCarVanFuelModel] = None,
                        accommodationRelocationModel: Option[EncryptedAccommodationRelocationModel] = None,
                        assets: Option[EncryptedValue] = None,
                        assetTransfer: Option[EncryptedValue] = None,
                        beneficialLoan: Option[EncryptedValue] = None,
                        educationalServices: Option[EncryptedValue] = None,
                        entertaining: Option[EncryptedValue] = None,
                        expenses: Option[EncryptedValue] = None,
                        medicalInsurance: Option[EncryptedValue] = None,
                        telephone: Option[EncryptedValue] = None,
                        service: Option[EncryptedValue] = None,
                        taxableExpenses: Option[EncryptedValue] = None,
                        nurseryPlaces: Option[EncryptedValue] = None,
                        otherItems: Option[EncryptedValue] = None,
                        paymentsOnEmployeesBehalf: Option[EncryptedValue] = None,
                        personalIncidentalExpenses: Option[EncryptedValue] = None,
                        employerProvidedProfessionalSubscriptions: Option[EncryptedValue] = None,
                        employerProvidedServices: Option[EncryptedValue] = None,
                        incomeTaxPaidByDirector: Option[EncryptedValue] = None,
                        travelAndSubsistence: Option[EncryptedValue] = None,
                        vouchersAndCreditCards: Option[EncryptedValue] = None,
                        nonCash: Option[EncryptedValue] = None,
                        assetsQuestion: Option[EncryptedValue] = None,
                        assetTransferQuestion: Option[EncryptedValue] = None,
                        beneficialLoanQuestion: Option[EncryptedValue] = None,
                        educationalServicesQuestion: Option[EncryptedValue] = None,
                        entertainingQuestion: Option[EncryptedValue] = None,
                        expensesQuestion: Option[EncryptedValue] = None,
                        medicalInsuranceQuestion: Option[EncryptedValue] = None,
                        telephoneQuestion: Option[EncryptedValue] = None,
                        serviceQuestion: Option[EncryptedValue] = None,
                        taxableExpensesQuestion: Option[EncryptedValue] = None,
                        nurseryPlacesQuestion: Option[EncryptedValue] = None,
                        otherItemsQuestion: Option[EncryptedValue] = None,
                        paymentsOnEmployeesBehalfQuestion: Option[EncryptedValue] = None,
                        personalIncidentalExpensesQuestion: Option[EncryptedValue] = None,
                        employerProvidedProfessionalSubscriptionsQuestion: Option[EncryptedValue] = None,
                        employerProvidedServicesQuestion: Option[EncryptedValue] = None,
                        incomeTaxPaidByDirectorQuestion: Option[EncryptedValue] = None,
                        travelAndSubsistenceQuestion: Option[EncryptedValue] = None,
                        vouchersAndCreditCardsQuestion: Option[EncryptedValue] = None,
                        nonCashQuestion: Option[EncryptedValue] = None,
                        submittedOn: Option[EncryptedValue] = None,
                        isUsingCustomerData: EncryptedValue,
                        isBenefitsReceived: EncryptedValue
                       )

object EncryptedBenefitsViewModel {

  val firstSetOfFields: OFormat[(Option[EncryptedCarVanFuelModel], Option[EncryptedAccommodationRelocationModel],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue])] = (
      (__ \ "carVanFuel").formatNullable[EncryptedCarVanFuelModel] and
      (__ \ "accommodationRelocation").formatNullable[EncryptedAccommodationRelocationModel] and
      (__ \ "assets").formatNullable[EncryptedValue] and
      (__ \ "assetTransfer").formatNullable[EncryptedValue] and
      (__ \ "beneficialLoan").formatNullable[EncryptedValue] and
      (__ \ "educationalServices").formatNullable[EncryptedValue] and
      (__ \ "entertaining").formatNullable[EncryptedValue] and
      (__ \ "expenses").formatNullable[EncryptedValue] and
      (__ \ "medicalInsurance").formatNullable[EncryptedValue] and
      (__ \ "telephone").formatNullable[EncryptedValue] and
      (__ \ "service").formatNullable[EncryptedValue] and
      (__ \ "taxableExpenses").formatNullable[EncryptedValue] and
      (__ \ "nurseryPlaces").formatNullable[EncryptedValue] and
      (__ \ "otherItems").formatNullable[EncryptedValue] and
      (__ \ "paymentsOnEmployeesBehalf").formatNullable[EncryptedValue] and
      (__ \ "personalIncidentalExpenses").formatNullable[EncryptedValue]
    ).tupled

  val secondSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue])] = (
    (__ \ "employerProvidedProfessionalSubscriptions").formatNullable[EncryptedValue] and
      (__ \ "employerProvidedServices").formatNullable[EncryptedValue] and
      (__ \ "incomeTaxPaidByDirector").formatNullable[EncryptedValue] and
      (__ \ "travelAndSubsistence").formatNullable[EncryptedValue] and
      (__ \ "vouchersAndCreditCards").formatNullable[EncryptedValue] and
      (__ \ "nonCash").formatNullable[EncryptedValue]
    ).tupled

  val thirdSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue])] = (
    (__ \ "assetsQuestion").formatNullable[EncryptedValue] and
      (__ \ "assetTransferQuestion").formatNullable[EncryptedValue] and
      (__ \ "beneficialLoanQuestion").formatNullable[EncryptedValue] and
      (__ \ "educationalServicesQuestion").formatNullable[EncryptedValue] and
      (__ \ "entertainingQuestion").formatNullable[EncryptedValue] and
      (__ \ "expensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "medicalInsuranceQuestion").formatNullable[EncryptedValue] and
      (__ \ "telephoneQuestion").formatNullable[EncryptedValue] and
      (__ \ "serviceQuestion").formatNullable[EncryptedValue] and
      (__ \ "taxableExpensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "nurseryPlacesQuestion").formatNullable[EncryptedValue] and
      (__ \ "otherItemsQuestion").formatNullable[EncryptedValue] and
      (__ \ "paymentsOnEmployeesBehalfQuestion").formatNullable[EncryptedValue] and
      (__ \ "personalIncidentalExpensesQuestion").formatNullable[EncryptedValue]
    ).tupled

  val fourthSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], EncryptedValue, EncryptedValue)] = (
    (__ \ "employerProvidedProfessionalSubscriptionsQuestion").formatNullable[EncryptedValue] and
      (__ \ "employerProvidedServicesQuestion").formatNullable[EncryptedValue] and
      (__ \ "incomeTaxPaidByDirectorQuestion").formatNullable[EncryptedValue] and
      (__ \ "travelAndSubsistenceQuestion").formatNullable[EncryptedValue] and
      (__ \ "vouchersAndCreditCardsQuestion").formatNullable[EncryptedValue] and
      (__ \ "nonCashQuestion").formatNullable[EncryptedValue] and
      (__ \ "submittedOn").formatNullable[EncryptedValue] and
      (__ \ "isUsingCustomerData").format[EncryptedValue] and
      (__ \ "isBenefitsReceived").format[EncryptedValue]
    ).tupled

  implicit val format: OFormat[EncryptedBenefitsViewModel] = {
    (firstSetOfFields and secondSetOfFields and thirdSetOfFields and fourthSetOfFields).apply({
      case (
        (carVanFuelModel, accommodationRelocationModel, assets, assetTransfer, beneficialLoan, educationalServices, entertaining,
        expenses, medicalInsurance, telephone, service, taxableExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses),
        (employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
        vouchersAndCreditCards, nonCash),
        (assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, educationalServicesQuestion,
        entertainingQuestion,
        expensesQuestion, medicalInsuranceQuestion, telephoneQuestion, serviceQuestion, taxableExpensesQuestion,
        nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion, personalIncidentalExpensesQuestion),
        (employerProvidedProfessionalSubscriptionsQuestion, employerProvidedServicesQuestion, incomeTaxPaidByDirectorQuestion, travelAndSubsistenceQuestion,
        vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived)
        ) =>
        EncryptedBenefitsViewModel(
          carVanFuelModel, accommodationRelocationModel, assets, assetTransfer, beneficialLoan, educationalServices, entertaining, expenses,
          medicalInsurance, telephone, service, taxableExpenses,
          nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses,
          employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
          vouchersAndCreditCards, nonCash,
          assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, educationalServicesQuestion,
          entertainingQuestion,
          expensesQuestion, medicalInsuranceQuestion, telephoneQuestion, serviceQuestion, taxableExpensesQuestion,
          nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion,
          personalIncidentalExpensesQuestion,
          employerProvidedProfessionalSubscriptionsQuestion, employerProvidedServicesQuestion, incomeTaxPaidByDirectorQuestion, travelAndSubsistenceQuestion,
          vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived
        )
    }, {
      benefits =>
        (
          (benefits.carVanFuelModel, benefits.accommodationRelocationModel, benefits.assets, benefits.assetTransfer, benefits.beneficialLoan,
            benefits.educationalServices, benefits.entertaining, benefits.expenses, benefits.medicalInsurance, benefits.telephone,
            benefits.service, benefits.taxableExpenses,
            benefits.nurseryPlaces, benefits.otherItems, benefits.paymentsOnEmployeesBehalf,
            benefits.personalIncidentalExpenses),
          (benefits.employerProvidedProfessionalSubscriptions, benefits.employerProvidedServices, benefits.incomeTaxPaidByDirector,
            benefits.travelAndSubsistence, benefits.vouchersAndCreditCards, benefits.nonCash),
          ( benefits.assetsQuestion, benefits.assetTransferQuestion, benefits.beneficialLoanQuestion,
            benefits.educationalServicesQuestion, benefits.entertainingQuestion,
            benefits.expensesQuestion, benefits.medicalInsuranceQuestion, benefits.telephoneQuestion,
            benefits.serviceQuestion, benefits.taxableExpensesQuestion,
            benefits.nurseryPlacesQuestion,
            benefits.otherItemsQuestion, benefits.paymentsOnEmployeesBehalfQuestion,
            benefits.personalIncidentalExpensesQuestion),
          (benefits.employerProvidedProfessionalSubscriptionsQuestion, benefits.employerProvidedServicesQuestion, benefits.incomeTaxPaidByDirectorQuestion,
            benefits.travelAndSubsistenceQuestion, benefits.vouchersAndCreditCardsQuestion,
            benefits.nonCashQuestion, benefits.submittedOn, benefits.isUsingCustomerData, benefits.isBenefitsReceived)
        )
    })
  }
}

