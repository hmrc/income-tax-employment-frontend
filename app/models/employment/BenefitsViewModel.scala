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

case class BenefitsViewModel(
                        accommodation: Option[BigDecimal] = None,
                        assets: Option[BigDecimal] = None,
                        assetTransfer: Option[BigDecimal] = None,
                        beneficialLoan: Option[BigDecimal] = None,
                        car: Option[BigDecimal] = None,
                        carFuel: Option[BigDecimal] = None,
                        educationalServices: Option[BigDecimal] = None,
                        entertaining: Option[BigDecimal] = None,
                        expenses: Option[BigDecimal] = None,
                        medicalInsurance: Option[BigDecimal] = None,
                        telephone: Option[BigDecimal] = None,
                        service: Option[BigDecimal] = None,
                        taxableExpenses: Option[BigDecimal] = None,
                        van: Option[BigDecimal] = None,
                        vanFuel: Option[BigDecimal] = None,
                        mileage: Option[BigDecimal] = None,
                        nonQualifyingRelocationExpenses: Option[BigDecimal] = None,
                        nurseryPlaces: Option[BigDecimal] = None,
                        otherItems: Option[BigDecimal] = None,
                        paymentsOnEmployeesBehalf: Option[BigDecimal] = None,
                        personalIncidentalExpenses: Option[BigDecimal] = None,
                        qualifyingRelocationExpenses: Option[BigDecimal] = None,
                        employerProvidedProfessionalSubscriptions: Option[BigDecimal] = None,
                        employerProvidedServices: Option[BigDecimal] = None,
                        incomeTaxPaidByDirector: Option[BigDecimal] = None,
                        travelAndSubsistence: Option[BigDecimal] = None,
                        vouchersAndCreditCards: Option[BigDecimal] = None,
                        nonCash: Option[BigDecimal] = None,
                        accommodationQuestion: Option[Boolean] = None,
                        assetsQuestion: Option[Boolean] = None,
                        assetTransferQuestion: Option[Boolean] = None,
                        beneficialLoanQuestion: Option[Boolean] = None,
                        carQuestion: Option[Boolean] = None,
                        carFuelQuestion: Option[Boolean] = None,
                        educationalServicesQuestion: Option[Boolean] = None,
                        entertainingQuestion: Option[Boolean] = None,
                        expensesQuestion: Option[Boolean] = None,
                        medicalInsuranceQuestion: Option[Boolean] = None,
                        telephoneQuestion: Option[Boolean] = None,
                        serviceQuestion: Option[Boolean] = None,
                        taxableExpensesQuestion: Option[Boolean] = None,
                        vanQuestion: Option[Boolean] = None,
                        vanFuelQuestion: Option[Boolean] = None,
                        mileageQuestion: Option[Boolean] = None,
                        nonQualifyingRelocationExpensesQuestion: Option[Boolean] = None,
                        nurseryPlacesQuestion: Option[Boolean] = None,
                        otherItemsQuestion: Option[Boolean] = None,
                        paymentsOnEmployeesBehalfQuestion: Option[Boolean] = None,
                        personalIncidentalExpensesQuestion: Option[Boolean] = None,
                        qualifyingRelocationExpensesQuestion: Option[Boolean] = None,
                        employerProvidedProfessionalSubscriptionsQuestion: Option[Boolean] = None,
                        employerProvidedServicesQuestion: Option[Boolean] = None,
                        incomeTaxPaidByDirectorQuestion: Option[Boolean] = None,
                        travelAndSubsistenceQuestion: Option[Boolean] = None,
                        vouchersAndCreditCardsQuestion: Option[Boolean] = None,
                        nonCashQuestion: Option[Boolean] = None,
                        carVanFuelQuestion: Option[Boolean] = None,
                        submittedOn: Option[String] = None,
                        isUsingCustomerData: Boolean
                       ){

  def toBenefits: Benefits ={
    Benefits(
      accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining, expenses, medicalInsurance,
      telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses, nurseryPlaces, otherItems,
      paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses, employerProvidedProfessionalSubscriptions,
      employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence, vouchersAndCreditCards, nonCash
    )
  }

  val vehicleDetailsPopulated: Boolean =
    car.isDefined || carFuel.isDefined || van.isDefined || vanFuel.isDefined || mileage.isDefined

  val accommodationDetailsPopulated: Boolean =
    accommodation.isDefined || nonQualifyingRelocationExpenses.isDefined || qualifyingRelocationExpenses.isDefined

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

  val benefitsPopulated: Boolean = vehicleDetailsPopulated || accommodationDetailsPopulated || travelDetailsPopulated ||
    utilitiesDetailsPopulated || medicalDetailsPopulated || incomeTaxDetailsPopulated || reimbursedDetailsPopulated || assetsDetailsPopulated
}

object BenefitsViewModel {
  val firstSetOfFields: OFormat[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])] = (
    (__ \ "accommodation").formatNullable[BigDecimal] and
      (__ \ "assets").formatNullable[BigDecimal] and
      (__ \ "assetTransfer").formatNullable[BigDecimal] and
      (__ \ "beneficialLoan").formatNullable[BigDecimal] and
      (__ \ "car").formatNullable[BigDecimal] and
      (__ \ "carFuel").formatNullable[BigDecimal] and
      (__ \ "educationalServices").formatNullable[BigDecimal] and
      (__ \ "entertaining").formatNullable[BigDecimal] and
      (__ \ "expenses").formatNullable[BigDecimal] and
      (__ \ "medicalInsurance").formatNullable[BigDecimal] and
      (__ \ "telephone").formatNullable[BigDecimal] and
      (__ \ "service").formatNullable[BigDecimal] and
      (__ \ "taxableExpenses").formatNullable[BigDecimal] and
      (__ \ "van").formatNullable[BigDecimal] and
      (__ \ "vanFuel").formatNullable[BigDecimal] and
      (__ \ "mileage").formatNullable[BigDecimal] and
      (__ \ "nonQualifyingRelocationExpenses").formatNullable[BigDecimal] and
      (__ \ "nurseryPlaces").formatNullable[BigDecimal] and
      (__ \ "otherItems").formatNullable[BigDecimal] and
      (__ \ "paymentsOnEmployeesBehalf").formatNullable[BigDecimal] and
      (__ \ "personalIncidentalExpenses").formatNullable[BigDecimal] and
      (__ \ "qualifyingRelocationExpenses").formatNullable[BigDecimal]
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
    Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[Boolean], Option[Boolean])] = (
    (__ \ "accommodationQuestion").formatNullable[Boolean] and
      (__ \ "assetsQuestion").formatNullable[Boolean] and
      (__ \ "assetTransferQuestion").formatNullable[Boolean] and
      (__ \ "beneficialLoanQuestion").formatNullable[Boolean] and
      (__ \ "carQuestion").formatNullable[Boolean] and
      (__ \ "carFuelQuestion").formatNullable[Boolean] and
      (__ \ "educationalServicesQuestion").formatNullable[Boolean] and
      (__ \ "entertainingQuestion").formatNullable[Boolean] and
      (__ \ "expensesQuestion").formatNullable[Boolean] and
      (__ \ "medicalInsuranceQuestion").formatNullable[Boolean] and
      (__ \ "telephoneQuestion").formatNullable[Boolean] and
      (__ \ "serviceQuestion").formatNullable[Boolean] and
      (__ \ "taxableExpensesQuestion").formatNullable[Boolean] and
      (__ \ "vanQuestion").formatNullable[Boolean] and
      (__ \ "vanFuelQuestion").formatNullable[Boolean] and
      (__ \ "mileageQuestion").formatNullable[Boolean] and
      (__ \ "nonQualifyingRelocationExpensesQuestion").formatNullable[Boolean] and
      (__ \ "nurseryPlacesQuestion").formatNullable[Boolean] and
      (__ \ "otherItemsQuestion").formatNullable[Boolean] and
      (__ \ "paymentsOnEmployeesBehalfQuestion").formatNullable[Boolean] and
      (__ \ "personalIncidentalExpensesQuestion").formatNullable[Boolean] and
      (__ \ "qualifyingRelocationExpensesQuestion").formatNullable[Boolean]
    ).tupled

  val fourthSetOfFields: OFormat[(Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[Boolean], Option[Boolean], Option[String], Boolean)] = (
    (__ \ "employerProvidedProfessionalSubscriptionsQuestion").formatNullable[Boolean] and
      (__ \ "employerProvidedServicesQuestion").formatNullable[Boolean] and
      (__ \ "incomeTaxPaidByDirectorQuestion").formatNullable[Boolean] and
      (__ \ "travelAndSubsistenceQuestion").formatNullable[Boolean] and
      (__ \ "vouchersAndCreditCardsQuestion").formatNullable[Boolean] and
      (__ \ "nonCashQuestion").formatNullable[Boolean] and
      (__ \ "carVanFuelQuestion").formatNullable[Boolean] and
      (__ \ "submittedOn").formatNullable[String] and
      (__ \ "isUsingCustomerData").format[Boolean]
    ).tupled

  implicit val format: OFormat[BenefitsViewModel] = {
    (firstSetOfFields and secondSetOfFields and thirdSetOfFields and fourthSetOfFields).apply({
      case (
        (accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining,
        expenses, medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses),
        (employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
        vouchersAndCreditCards, nonCash),
        (accommodationQuestion, assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, carQuestion, carFuelQuestion, educationalServicesQuestion,
        entertainingQuestion,
        expensesQuestion, medicalInsuranceQuestion, telephoneQuestion, serviceQuestion, taxableExpensesQuestion, vanQuestion, vanFuelQuestion, mileageQuestion,
        nonQualifyingRelocationExpensesQuestion,
        nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion, personalIncidentalExpensesQuestion, qualifyingRelocationExpensesQuestion),
        (employerProvidedProfessionalSubscriptionsQuestion, employerProvidedServicesQuestion, incomeTaxPaidByDirectorQuestion, travelAndSubsistenceQuestion,
        vouchersAndCreditCardsQuestion, nonCashQuestion, carVanFuelQuestion, submittedOn, isUsingCustomerData)
        ) =>
        BenefitsViewModel(
          accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining, expenses,
          medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
          nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses,
          employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
          vouchersAndCreditCards, nonCash,
          accommodationQuestion, assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, carQuestion, carFuelQuestion, educationalServicesQuestion,
          entertainingQuestion,
          expensesQuestion, medicalInsuranceQuestion, telephoneQuestion, serviceQuestion, taxableExpensesQuestion, vanQuestion, vanFuelQuestion, mileageQuestion,
          nonQualifyingRelocationExpensesQuestion,
          nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion, personalIncidentalExpensesQuestion, qualifyingRelocationExpensesQuestion,
          employerProvidedProfessionalSubscriptionsQuestion, employerProvidedServicesQuestion, incomeTaxPaidByDirectorQuestion, travelAndSubsistenceQuestion,
          vouchersAndCreditCardsQuestion, nonCashQuestion, carVanFuelQuestion, submittedOn, isUsingCustomerData
        )
    }, {
      benefits =>
        (
          (benefits.accommodation, benefits.assets, benefits.assetTransfer, benefits.beneficialLoan, benefits.car, benefits.carFuel,
            benefits.educationalServices, benefits.entertaining, benefits.expenses, benefits.medicalInsurance, benefits.telephone,
            benefits.service, benefits.taxableExpenses, benefits.van, benefits.vanFuel, benefits.mileage,
            benefits.nonQualifyingRelocationExpenses, benefits.nurseryPlaces, benefits.otherItems, benefits.paymentsOnEmployeesBehalf,
            benefits.personalIncidentalExpenses, benefits.qualifyingRelocationExpenses),
          (benefits.employerProvidedProfessionalSubscriptions, benefits.employerProvidedServices, benefits.incomeTaxPaidByDirector,
            benefits.travelAndSubsistence, benefits.vouchersAndCreditCards, benefits.nonCash),
          (benefits.accommodationQuestion, benefits.assetsQuestion, benefits.assetTransferQuestion, benefits.beneficialLoanQuestion, benefits.carQuestion, benefits.carFuelQuestion,
            benefits.educationalServicesQuestion, benefits.entertainingQuestion, benefits.expensesQuestion, benefits.medicalInsuranceQuestion, benefits.telephoneQuestion,
            benefits.serviceQuestion, benefits.taxableExpensesQuestion, benefits.vanQuestion, benefits.vanFuelQuestion, benefits.mileageQuestion,
            benefits.nonQualifyingRelocationExpensesQuestion, benefits.nurseryPlacesQuestion, benefits.otherItemsQuestion, benefits.paymentsOnEmployeesBehalfQuestion,
            benefits.personalIncidentalExpensesQuestion, benefits.qualifyingRelocationExpensesQuestion),
          (benefits.employerProvidedProfessionalSubscriptionsQuestion, benefits.employerProvidedServicesQuestion, benefits.incomeTaxPaidByDirectorQuestion,
            benefits.travelAndSubsistenceQuestion, benefits.vouchersAndCreditCardsQuestion, benefits.nonCashQuestion, benefits.carVanFuelQuestion, benefits.submittedOn, benefits.isUsingCustomerData)
        )
    })
  }
}

