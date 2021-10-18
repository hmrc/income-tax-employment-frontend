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

import models.benefits.{EncryptedUtilitiesAndServicesModel, UtilitiesAndServicesModel}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OFormat, __}
import utils.EncryptedValue

case class BenefitsViewModel(
                              carVanFuelModel: Option[CarVanFuelModel] = None,
                              accommodationRelocationModel: Option[AccommodationRelocationModel] = None,
                              travelEntertainmentModel: Option[TravelEntertainmentModel] = None,
                              utilitiesAndServicesModel: Option[UtilitiesAndServicesModel]= None,
                              assets: Option[BigDecimal] = None,
                              assetTransfer: Option[BigDecimal] = None,
                              beneficialLoan: Option[BigDecimal] = None,
                              educationalServices: Option[BigDecimal] = None,
                              expenses: Option[BigDecimal] = None,
                              medicalInsurance: Option[BigDecimal] = None,
                              taxableExpenses: Option[BigDecimal] = None,
                              nurseryPlaces: Option[BigDecimal] = None,
                              otherItems: Option[BigDecimal] = None,
                              paymentsOnEmployeesBehalf: Option[BigDecimal] = None,
                              incomeTaxPaidByDirector: Option[BigDecimal] = None,
                              vouchersAndCreditCards: Option[BigDecimal] = None,
                              nonCash: Option[BigDecimal] = None,
                              assetsQuestion: Option[Boolean] = None,
                              assetTransferQuestion: Option[Boolean] = None,
                              beneficialLoanQuestion: Option[Boolean] = None,
                              educationalServicesQuestion: Option[Boolean] = None,
                              expensesQuestion: Option[Boolean] = None,
                              medicalInsuranceQuestion: Option[Boolean] = None,
                              taxableExpensesQuestion: Option[Boolean] = None,
                              nurseryPlacesQuestion: Option[Boolean] = None,
                              otherItemsQuestion: Option[Boolean] = None,
                              paymentsOnEmployeesBehalfQuestion: Option[Boolean] = None,
                              incomeTaxPaidByDirectorQuestion: Option[Boolean] = None,
                              vouchersAndCreditCardsQuestion: Option[Boolean] = None,
                              nonCashQuestion: Option[Boolean] = None,
                              submittedOn: Option[String] = None,
                              isUsingCustomerData: Boolean,
                              isBenefitsReceived: Boolean = false
                       ){

  def toBenefits: Benefits ={
    Benefits(
      accommodationRelocationModel.flatMap(_.accommodation), assets, assetTransfer, beneficialLoan, carVanFuelModel.flatMap(_.car),
      carVanFuelModel.flatMap(_.carFuel), educationalServices, travelEntertainmentModel.flatMap(_.entertaining), expenses, medicalInsurance,
      utilitiesAndServicesModel.flatMap(_.telephone), utilitiesAndServicesModel.flatMap(_.service),
      taxableExpenses, carVanFuelModel.flatMap(_.van), carVanFuelModel.flatMap(_.vanFuel),
      carVanFuelModel.flatMap(_.mileage), accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses), nurseryPlaces, otherItems,
      paymentsOnEmployeesBehalf, travelEntertainmentModel.flatMap(_.personalIncidentalExpenses),
      accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses),
        utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions),
      utilitiesAndServicesModel.flatMap(_.employerProvidedServices), incomeTaxPaidByDirector,
      travelEntertainmentModel.flatMap(_.travelAndSubsistence), vouchersAndCreditCards, nonCash
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
    travelEntertainmentModel.flatMap(_.travelAndSubsistence).isDefined || travelEntertainmentModel.flatMap(_.personalIncidentalExpenses).isDefined ||
      travelEntertainmentModel.flatMap(_.entertaining).isDefined

  val utilitiesDetailsPopulated: Boolean =
    utilitiesAndServicesModel.flatMap(_.telephone).isDefined || utilitiesAndServicesModel.flatMap(_.employerProvidedServices).isDefined ||
      utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions).isDefined || utilitiesAndServicesModel.flatMap(_.service).isDefined

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

  val firstSetOfFields: OFormat[(Option[CarVanFuelModel], Option[AccommodationRelocationModel], Option[TravelEntertainmentModel],
    Option[UtilitiesAndServicesModel],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])] = (
      (__ \ "carVanFuelModel").formatNullable[CarVanFuelModel] and
      (__ \ "accommodationRelocationModel").formatNullable[AccommodationRelocationModel] and
      (__ \ "travelEntertainmentModel").formatNullable[TravelEntertainmentModel] and
      (__ \ "utilitiesAndServicesModel").formatNullable[UtilitiesAndServicesModel] and
      (__ \ "assets").formatNullable[BigDecimal] and
      (__ \ "assetTransfer").formatNullable[BigDecimal] and
      (__ \ "beneficialLoan").formatNullable[BigDecimal] and
      (__ \ "educationalServices").formatNullable[BigDecimal] and
      (__ \ "expenses").formatNullable[BigDecimal] and
      (__ \ "medicalInsurance").formatNullable[BigDecimal] and
      (__ \ "taxableExpenses").formatNullable[BigDecimal] and
      (__ \ "nurseryPlaces").formatNullable[BigDecimal] and
      (__ \ "otherItems").formatNullable[BigDecimal] and
      (__ \ "paymentsOnEmployeesBehalf").formatNullable[BigDecimal]
    ).tupled

  val secondSetOfFields: OFormat[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])] = (
      (__ \ "incomeTaxPaidByDirector").formatNullable[BigDecimal] and
      (__ \ "vouchersAndCreditCards").formatNullable[BigDecimal] and
      (__ \ "nonCash").formatNullable[BigDecimal]
    ).tupled

  val thirdSetOfFields: OFormat[(Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[Boolean], Option[Boolean])] = (
    (__ \ "assetsQuestion").formatNullable[Boolean] and
      (__ \ "assetTransferQuestion").formatNullable[Boolean] and
      (__ \ "beneficialLoanQuestion").formatNullable[Boolean] and
      (__ \ "educationalServicesQuestion").formatNullable[Boolean] and
      (__ \ "expensesQuestion").formatNullable[Boolean] and
      (__ \ "medicalInsuranceQuestion").formatNullable[Boolean] and
      (__ \ "taxableExpensesQuestion").formatNullable[Boolean] and
      (__ \ "nurseryPlacesQuestion").formatNullable[Boolean] and
      (__ \ "otherItemsQuestion").formatNullable[Boolean] and
      (__ \ "paymentsOnEmployeesBehalfQuestion").formatNullable[Boolean]
    ).tupled

  val fourthSetOfFields: OFormat[(Option[Boolean], Option[Boolean],
    Option[Boolean], Option[String], Boolean, Boolean)] = (
      (__ \ "incomeTaxPaidByDirectorQuestion").formatNullable[Boolean] and
      (__ \ "vouchersAndCreditCardsQuestion").formatNullable[Boolean] and
      (__ \ "nonCashQuestion").formatNullable[Boolean] and
      (__ \ "submittedOn").formatNullable[String] and
      (__ \ "isUsingCustomerData").format[Boolean] and
      (__ \ "isBenefitsReceived").format[Boolean]
    ).tupled

  implicit val format: OFormat[BenefitsViewModel] = {
    (firstSetOfFields and secondSetOfFields and thirdSetOfFields and fourthSetOfFields).apply({
      case (
        (carVanFuelModel, accommodationRelocationModel, travelEntertainmentModel, utilitiesAndServicesModel,
        assets, assetTransfer, beneficialLoan, educationalServices,
        expenses, medicalInsurance, taxableExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf),
        (incomeTaxPaidByDirector,
        vouchersAndCreditCards, nonCash),
        (assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, educationalServicesQuestion,
        expensesQuestion, medicalInsuranceQuestion, taxableExpensesQuestion,
        nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion),
        (incomeTaxPaidByDirectorQuestion,
        vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived)
        ) =>
        BenefitsViewModel(
          carVanFuelModel, accommodationRelocationModel, travelEntertainmentModel, utilitiesAndServicesModel,
          assets, assetTransfer, beneficialLoan, educationalServices, expenses,
          medicalInsurance,taxableExpenses,
          nurseryPlaces, otherItems, paymentsOnEmployeesBehalf,
          incomeTaxPaidByDirector,
          vouchersAndCreditCards, nonCash,
          assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, educationalServicesQuestion,
          expensesQuestion, medicalInsuranceQuestion, taxableExpensesQuestion,
          nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion, incomeTaxPaidByDirectorQuestion,
          vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived
        )
    }, {
      benefits =>
        (
          (benefits.carVanFuelModel, benefits.accommodationRelocationModel, benefits.travelEntertainmentModel,
            benefits.utilitiesAndServicesModel, benefits.assets,
            benefits.assetTransfer, benefits.beneficialLoan, benefits.educationalServices, benefits.expenses, benefits.medicalInsurance,
            benefits.taxableExpenses, benefits.nurseryPlaces, benefits.otherItems, benefits.paymentsOnEmployeesBehalf
            ),
          (benefits.incomeTaxPaidByDirector,
            benefits.vouchersAndCreditCards, benefits.nonCash),
          (benefits.assetsQuestion, benefits.assetTransferQuestion, benefits.beneficialLoanQuestion,
            benefits.educationalServicesQuestion, benefits.expensesQuestion,
            benefits.medicalInsuranceQuestion, benefits.taxableExpensesQuestion, benefits.nurseryPlacesQuestion,
            benefits.otherItemsQuestion, benefits.paymentsOnEmployeesBehalfQuestion
            ),
          (benefits.incomeTaxPaidByDirectorQuestion,
            benefits.vouchersAndCreditCardsQuestion, benefits.nonCashQuestion,
            benefits.submittedOn, benefits.isUsingCustomerData, benefits.isBenefitsReceived)
        )
    })
  }
}

case class EncryptedBenefitsViewModel(
                        carVanFuelModel: Option[EncryptedCarVanFuelModel] = None,
                        accommodationRelocationModel: Option[EncryptedAccommodationRelocationModel] = None,
                        travelEntertainmentModel: Option[EncryptedTravelEntertainmentModel] = None,
                        utilitiesAndServicesModel: Option[EncryptedUtilitiesAndServicesModel]= None,
                        assets: Option[EncryptedValue] = None,
                        assetTransfer: Option[EncryptedValue] = None,
                        beneficialLoan: Option[EncryptedValue] = None,
                        educationalServices: Option[EncryptedValue] = None,
                        expenses: Option[EncryptedValue] = None,
                        medicalInsurance: Option[EncryptedValue] = None,
                        taxableExpenses: Option[EncryptedValue] = None,
                        nurseryPlaces: Option[EncryptedValue] = None,
                        otherItems: Option[EncryptedValue] = None,
                        paymentsOnEmployeesBehalf: Option[EncryptedValue] = None,
                        incomeTaxPaidByDirector: Option[EncryptedValue] = None,
                        vouchersAndCreditCards: Option[EncryptedValue] = None,
                        nonCash: Option[EncryptedValue] = None,
                        assetsQuestion: Option[EncryptedValue] = None,
                        assetTransferQuestion: Option[EncryptedValue] = None,
                        beneficialLoanQuestion: Option[EncryptedValue] = None,
                        educationalServicesQuestion: Option[EncryptedValue] = None,
                        expensesQuestion: Option[EncryptedValue] = None,
                        medicalInsuranceQuestion: Option[EncryptedValue] = None,
                        taxableExpensesQuestion: Option[EncryptedValue] = None,
                        nurseryPlacesQuestion: Option[EncryptedValue] = None,
                        otherItemsQuestion: Option[EncryptedValue] = None,
                        paymentsOnEmployeesBehalfQuestion: Option[EncryptedValue] = None,
                        incomeTaxPaidByDirectorQuestion: Option[EncryptedValue] = None,
                        vouchersAndCreditCardsQuestion: Option[EncryptedValue] = None,
                        nonCashQuestion: Option[EncryptedValue] = None,
                        submittedOn: Option[EncryptedValue] = None,
                        isUsingCustomerData: EncryptedValue,
                        isBenefitsReceived: EncryptedValue
                       )

object EncryptedBenefitsViewModel {

  val firstSetOfFields: OFormat[(Option[EncryptedCarVanFuelModel], Option[EncryptedAccommodationRelocationModel],
    Option[EncryptedTravelEntertainmentModel],Option[EncryptedUtilitiesAndServicesModel], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue]
    )] = (
      (__ \ "carVanFuelModel").formatNullable[EncryptedCarVanFuelModel] and
      (__ \ "accommodationRelocationModel").formatNullable[EncryptedAccommodationRelocationModel] and
      (__ \ "travelEntertainmentModel").formatNullable[EncryptedTravelEntertainmentModel] and
      (__ \ "utilitiesAndServicesModel").formatNullable[EncryptedUtilitiesAndServicesModel]and
      (__ \ "assets").formatNullable[EncryptedValue] and
      (__ \ "assetTransfer").formatNullable[EncryptedValue] and
      (__ \ "beneficialLoan").formatNullable[EncryptedValue] and
      (__ \ "educationalServices").formatNullable[EncryptedValue] and
      (__ \ "expenses").formatNullable[EncryptedValue] and
      (__ \ "medicalInsurance").formatNullable[EncryptedValue] and
      (__ \ "taxableExpenses").formatNullable[EncryptedValue] and
      (__ \ "nurseryPlaces").formatNullable[EncryptedValue] and
      (__ \ "otherItems").formatNullable[EncryptedValue] and
      (__ \ "paymentsOnEmployeesBehalf").formatNullable[EncryptedValue]
    ).tupled

  val secondSetOfFields: OFormat[(Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue])] = (
      (__ \ "incomeTaxPaidByDirector").formatNullable[EncryptedValue] and
      (__ \ "vouchersAndCreditCards").formatNullable[EncryptedValue] and
      (__ \ "nonCash").formatNullable[EncryptedValue]
    ).tupled

  val thirdSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue])] = (
    (__ \ "assetsQuestion").formatNullable[EncryptedValue] and
      (__ \ "assetTransferQuestion").formatNullable[EncryptedValue] and
      (__ \ "beneficialLoanQuestion").formatNullable[EncryptedValue] and
      (__ \ "educationalServicesQuestion").formatNullable[EncryptedValue] and
      (__ \ "expensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "medicalInsuranceQuestion").formatNullable[EncryptedValue] and
      (__ \ "taxableExpensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "nurseryPlacesQuestion").formatNullable[EncryptedValue] and
      (__ \ "otherItemsQuestion").formatNullable[EncryptedValue] and
      (__ \ "paymentsOnEmployeesBehalfQuestion").formatNullable[EncryptedValue]
    ).tupled

  val fourthSetOfFields: OFormat[(Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], EncryptedValue, EncryptedValue)] = (
      (__ \ "incomeTaxPaidByDirectorQuestion").formatNullable[EncryptedValue] and
      (__ \ "vouchersAndCreditCardsQuestion").formatNullable[EncryptedValue] and
      (__ \ "nonCashQuestion").formatNullable[EncryptedValue] and
      (__ \ "submittedOn").formatNullable[EncryptedValue] and
      (__ \ "isUsingCustomerData").format[EncryptedValue] and
      (__ \ "isBenefitsReceived").format[EncryptedValue]
    ).tupled

  implicit val format: OFormat[EncryptedBenefitsViewModel] = {
    (firstSetOfFields and secondSetOfFields and thirdSetOfFields and fourthSetOfFields).apply({
      case (
        (carVanFuelModel, accommodationRelocationModel, travelEntertainmentModel,
        utilitiesAndServicesModel, assets, assetTransfer, beneficialLoan, educationalServices,
        expenses, medicalInsurance, taxableExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf),
        (incomeTaxPaidByDirector,
        vouchersAndCreditCards, nonCash),
        (assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, educationalServicesQuestion,
        expensesQuestion, medicalInsuranceQuestion, taxableExpensesQuestion,
        nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion),
        (incomeTaxPaidByDirectorQuestion,
        vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived)
        ) =>
        EncryptedBenefitsViewModel(
          carVanFuelModel, accommodationRelocationModel, travelEntertainmentModel, utilitiesAndServicesModel,
          assets, assetTransfer, beneficialLoan, educationalServices, expenses,
          medicalInsurance, taxableExpenses,
          nurseryPlaces, otherItems, paymentsOnEmployeesBehalf,
          incomeTaxPaidByDirector,
          vouchersAndCreditCards, nonCash,
          assetsQuestion, assetTransferQuestion, beneficialLoanQuestion, educationalServicesQuestion,
          expensesQuestion, medicalInsuranceQuestion, taxableExpensesQuestion,
          nurseryPlacesQuestion, otherItemsQuestion, paymentsOnEmployeesBehalfQuestion,
          incomeTaxPaidByDirectorQuestion,
          vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived
        )
    }, {
      benefits =>
        (
          (benefits.carVanFuelModel, benefits.accommodationRelocationModel, benefits.travelEntertainmentModel, benefits.utilitiesAndServicesModel,
            benefits.assets, benefits.assetTransfer,
            benefits.beneficialLoan, benefits.educationalServices, benefits.expenses, benefits.medicalInsurance,benefits.taxableExpenses,
            benefits.nurseryPlaces, benefits.otherItems, benefits.paymentsOnEmployeesBehalf
            ),
          (benefits.incomeTaxPaidByDirector,
            benefits.vouchersAndCreditCards, benefits.nonCash),
          ( benefits.assetsQuestion, benefits.assetTransferQuestion, benefits.beneficialLoanQuestion,
            benefits.educationalServicesQuestion,
            benefits.expensesQuestion, benefits.medicalInsuranceQuestion,benefits.taxableExpensesQuestion,
            benefits.nurseryPlacesQuestion,
            benefits.otherItemsQuestion, benefits.paymentsOnEmployeesBehalfQuestion
            ),
          (benefits.incomeTaxPaidByDirectorQuestion,
            benefits.vouchersAndCreditCardsQuestion,
            benefits.nonCashQuestion, benefits.submittedOn, benefits.isUsingCustomerData, benefits.isBenefitsReceived)
        )
    })
  }
}

