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

package models.benefits

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OFormat, __}
import utils.EncryptedValue

case class BenefitsViewModel(
                              carVanFuelModel: Option[CarVanFuelModel] = None,
                              accommodationRelocationModel: Option[AccommodationRelocationModel] = None,
                              travelEntertainmentModel: Option[TravelEntertainmentModel] = None,
                              utilitiesAndServicesModel: Option[UtilitiesAndServicesModel] = None,
                              medicalChildcareEducationModel: Option[MedicalChildcareEducationModel] = None,
                              incomeTaxAndCostsModel: Option[IncomeTaxAndCostsModel] = None,
                              assets: Option[BigDecimal] = None,
                              assetTransfer: Option[BigDecimal] = None,
                              expenses: Option[BigDecimal] = None,
                              taxableExpenses: Option[BigDecimal] = None,
                              otherItems: Option[BigDecimal] = None,
                              vouchersAndCreditCards: Option[BigDecimal] = None,
                              nonCash: Option[BigDecimal] = None,
                              assetsQuestion: Option[Boolean] = None,
                              assetTransferQuestion: Option[Boolean] = None,
                              expensesQuestion: Option[Boolean] = None,
                              taxableExpensesQuestion: Option[Boolean] = None,
                              otherItemsQuestion: Option[Boolean] = None,
                              vouchersAndCreditCardsQuestion: Option[Boolean] = None,
                              nonCashQuestion: Option[Boolean] = None,
                              submittedOn: Option[String] = None,
                              isUsingCustomerData: Boolean,
                              isBenefitsReceived: Boolean = false
                            ) {

  def toBenefits: Benefits = {
    Benefits(
      accommodationRelocationModel.flatMap(_.accommodation), assets, assetTransfer, medicalChildcareEducationModel.flatMap(_.beneficialLoan),
      carVanFuelModel.flatMap(_.car), carVanFuelModel.flatMap(_.carFuel), medicalChildcareEducationModel.flatMap(_.educationalServices),
      travelEntertainmentModel.flatMap(_.entertaining), expenses, medicalChildcareEducationModel.flatMap(_.medicalInsurance),
      utilitiesAndServicesModel.flatMap(_.telephone), utilitiesAndServicesModel.flatMap(_.service), taxableExpenses, carVanFuelModel.flatMap(_.van),
      carVanFuelModel.flatMap(_.vanFuel), carVanFuelModel.flatMap(_.mileage), accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses),
      medicalChildcareEducationModel.flatMap(_.nurseryPlaces), otherItems, incomeTaxAndCostsModel.flatMap(_.paymentsOnEmployeesBehalf),
      travelEntertainmentModel.flatMap(_.personalIncidentalExpenses), accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses),
      utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions), utilitiesAndServicesModel.flatMap(_.employerProvidedServices),
      incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirector), travelEntertainmentModel.flatMap(_.travelAndSubsistence), vouchersAndCreditCards, nonCash
    )
  }

  val vehicleDetailsPopulated: Boolean =
    carVanFuelModel.flatMap(_.car).isDefined ||
      carVanFuelModel.flatMap(_.carFuel).isDefined ||
      carVanFuelModel.flatMap(_.van).isDefined ||
      carVanFuelModel.flatMap(_.vanFuel).isDefined ||
      carVanFuelModel.flatMap(_.mileage).isDefined

  val accommodationDetailsPopulated: Boolean =
    accommodationRelocationModel.flatMap(_.accommodation).isDefined ||
      accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses).isDefined ||
      accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses).isDefined

  val travelDetailsPopulated: Boolean =
    travelEntertainmentModel.flatMap(_.travelAndSubsistence).isDefined ||
      travelEntertainmentModel.flatMap(_.personalIncidentalExpenses).isDefined ||
      travelEntertainmentModel.flatMap(_.entertaining).isDefined

  val utilitiesDetailsPopulated: Boolean =
    utilitiesAndServicesModel.flatMap(_.telephone).isDefined ||
      utilitiesAndServicesModel.flatMap(_.employerProvidedServices).isDefined ||
      utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions).isDefined ||
      utilitiesAndServicesModel.flatMap(_.service).isDefined

  val medicalDetailsPopulated: Boolean =
    medicalChildcareEducationModel.flatMap(_.medicalInsurance).isDefined ||
      medicalChildcareEducationModel.flatMap(_.nurseryPlaces).isDefined ||
      medicalChildcareEducationModel.flatMap(_.beneficialLoan).isDefined ||
      medicalChildcareEducationModel.flatMap(_.educationalServices).isDefined

  val incomeTaxDetailsPopulated: Boolean =
    incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirector).isDefined ||
      incomeTaxAndCostsModel.flatMap(_.paymentsOnEmployeesBehalf).isDefined

  val reimbursedDetailsPopulated: Boolean =
    expenses.isDefined ||
      taxableExpenses.isDefined ||
      vouchersAndCreditCards.isDefined ||
      nonCash.isDefined ||
      otherItems.isDefined

  val assetsDetailsPopulated: Boolean =
    assets.isDefined ||
      assetTransfer.isDefined
}

object BenefitsViewModel {

  def clear(isUsingCustomerData: Boolean): BenefitsViewModel =
    BenefitsViewModel(isUsingCustomerData = isUsingCustomerData)

  val firstSetOfFields: OFormat[(Option[CarVanFuelModel], Option[AccommodationRelocationModel], Option[TravelEntertainmentModel],
    Option[UtilitiesAndServicesModel], Option[MedicalChildcareEducationModel], Option[IncomeTaxAndCostsModel],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])] = (
    (__ \ "carVanFuelModel").formatNullable[CarVanFuelModel] and
      (__ \ "accommodationRelocationModel").formatNullable[AccommodationRelocationModel] and
      (__ \ "travelEntertainmentModel").formatNullable[TravelEntertainmentModel] and
      (__ \ "utilitiesAndServicesModel").formatNullable[UtilitiesAndServicesModel] and
      (__ \ "medicalChildcareEducationModel").formatNullable[MedicalChildcareEducationModel] and
      (__ \ "incomeTaxAndCostsModel").formatNullable[IncomeTaxAndCostsModel] and
      (__ \ "assets").formatNullable[BigDecimal] and
      (__ \ "assetTransfer").formatNullable[BigDecimal] and
      (__ \ "expenses").formatNullable[BigDecimal] and
      (__ \ "taxableExpenses").formatNullable[BigDecimal] and
      (__ \ "otherItems").formatNullable[BigDecimal]
    ).tupled

  val secondSetOfFields: OFormat[(Option[BigDecimal], Option[BigDecimal])] = (
    (__ \ "vouchersAndCreditCards").formatNullable[BigDecimal] and
      (__ \ "nonCash").formatNullable[BigDecimal]
    ).tupled

  val thirdSetOfFields: OFormat[(Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean])] = (
    (__ \ "assetsQuestion").formatNullable[Boolean] and
      (__ \ "assetTransferQuestion").formatNullable[Boolean] and
      (__ \ "expensesQuestion").formatNullable[Boolean] and
      (__ \ "taxableExpensesQuestion").formatNullable[Boolean] and
      (__ \ "otherItemsQuestion").formatNullable[Boolean]
    ).tupled

  val fourthSetOfFields: OFormat[(Option[Boolean], Option[Boolean], Option[String], Boolean, Boolean)] = (
    (__ \ "vouchersAndCreditCardsQuestion").formatNullable[Boolean] and
      (__ \ "nonCashQuestion").formatNullable[Boolean] and
      (__ \ "submittedOn").formatNullable[String] and
      (__ \ "isUsingCustomerData").format[Boolean] and
      (__ \ "isBenefitsReceived").format[Boolean]
    ).tupled

  implicit val format: OFormat[BenefitsViewModel] = {
    (firstSetOfFields and secondSetOfFields and thirdSetOfFields and fourthSetOfFields).apply({
      case (
        (carVanFuelModel, accommodationRelocationModel, travelEntertainmentModel, utilitiesAndServicesModel, medicalChildcareEducationModel,
        incomeTaxAndCostsModel, assets, assetTransfer, expenses, taxableExpenses, otherItems),
        (vouchersAndCreditCards, nonCash),
        (assetsQuestion, assetTransferQuestion, expensesQuestion, taxableExpensesQuestion, otherItemsQuestion),
        (vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived)
        ) =>
        BenefitsViewModel(
          carVanFuelModel, accommodationRelocationModel, travelEntertainmentModel, utilitiesAndServicesModel, medicalChildcareEducationModel,
          incomeTaxAndCostsModel, assets, assetTransfer, expenses, taxableExpenses, otherItems, vouchersAndCreditCards, nonCash, assetsQuestion,
          assetTransferQuestion, expensesQuestion, taxableExpensesQuestion, otherItemsQuestion, vouchersAndCreditCardsQuestion, nonCashQuestion,
          submittedOn, isUsingCustomerData, isBenefitsReceived
        )
    }, {
      benefits =>
        (
          (benefits.carVanFuelModel, benefits.accommodationRelocationModel, benefits.travelEntertainmentModel, benefits.utilitiesAndServicesModel,
            benefits.medicalChildcareEducationModel, benefits.incomeTaxAndCostsModel, benefits.assets, benefits.assetTransfer, benefits.expenses,
            benefits.taxableExpenses, benefits.otherItems),
          (benefits.vouchersAndCreditCards, benefits.nonCash),
          (benefits.assetsQuestion, benefits.assetTransferQuestion, benefits.expensesQuestion, benefits.taxableExpensesQuestion,
            benefits.otherItemsQuestion),
          (benefits.vouchersAndCreditCardsQuestion, benefits.nonCashQuestion,
            benefits.submittedOn, benefits.isUsingCustomerData, benefits.isBenefitsReceived)
        )
    })
  }
}

case class EncryptedBenefitsViewModel(
                                       carVanFuelModel: Option[EncryptedCarVanFuelModel] = None,
                                       accommodationRelocationModel: Option[EncryptedAccommodationRelocationModel] = None,
                                       travelEntertainmentModel: Option[EncryptedTravelEntertainmentModel] = None,
                                       utilitiesAndServicesModel: Option[EncryptedUtilitiesAndServicesModel] = None,
                                       medicalChildcareEducationModel: Option[EncryptedMedicalChildcareEducationModel] = None,
                                       incomeTaxAndCostsModel: Option[EncryptedIncomeTaxAndCostsModel] = None,
                                       assets: Option[EncryptedValue] = None,
                                       assetTransfer: Option[EncryptedValue] = None,
                                       expenses: Option[EncryptedValue] = None,
                                       taxableExpenses: Option[EncryptedValue] = None,
                                       otherItems: Option[EncryptedValue] = None,
                                       vouchersAndCreditCards: Option[EncryptedValue] = None,
                                       nonCash: Option[EncryptedValue] = None,
                                       assetsQuestion: Option[EncryptedValue] = None,
                                       assetTransferQuestion: Option[EncryptedValue] = None,
                                       expensesQuestion: Option[EncryptedValue] = None,
                                       taxableExpensesQuestion: Option[EncryptedValue] = None,
                                       otherItemsQuestion: Option[EncryptedValue] = None,
                                       vouchersAndCreditCardsQuestion: Option[EncryptedValue] = None,
                                       nonCashQuestion: Option[EncryptedValue] = None,
                                       submittedOn: Option[EncryptedValue] = None,
                                       isUsingCustomerData: EncryptedValue,
                                       isBenefitsReceived: EncryptedValue
                                     )

object EncryptedBenefitsViewModel {

  val firstSetOfFields: OFormat[(Option[EncryptedCarVanFuelModel], Option[EncryptedAccommodationRelocationModel],
    Option[EncryptedTravelEntertainmentModel], Option[EncryptedUtilitiesAndServicesModel], Option[EncryptedMedicalChildcareEducationModel],
    Option[EncryptedIncomeTaxAndCostsModel], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue])] = (
    (__ \ "carVanFuelModel").formatNullable[EncryptedCarVanFuelModel] and
      (__ \ "accommodationRelocationModel").formatNullable[EncryptedAccommodationRelocationModel] and
      (__ \ "travelEntertainmentModel").formatNullable[EncryptedTravelEntertainmentModel] and
      (__ \ "utilitiesAndServicesModel").formatNullable[EncryptedUtilitiesAndServicesModel] and
      (__ \ "medicalChildcareEducationModel").formatNullable[EncryptedMedicalChildcareEducationModel] and
      (__ \ "incomeTaxAndCostsModel").formatNullable[EncryptedIncomeTaxAndCostsModel] and
      (__ \ "assets").formatNullable[EncryptedValue] and
      (__ \ "assetTransfer").formatNullable[EncryptedValue] and
      (__ \ "expenses").formatNullable[EncryptedValue] and
      (__ \ "taxableExpenses").formatNullable[EncryptedValue] and
      (__ \ "otherItems").formatNullable[EncryptedValue]
    ).tupled

  val secondSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue])] = (
    (__ \ "vouchersAndCreditCards").formatNullable[EncryptedValue] and
      (__ \ "nonCash").formatNullable[EncryptedValue]
    ).tupled

  val thirdSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue])] = (
    (__ \ "assetsQuestion").formatNullable[EncryptedValue] and
      (__ \ "assetTransferQuestion").formatNullable[EncryptedValue] and
      (__ \ "expensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "taxableExpensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "otherItemsQuestion").formatNullable[EncryptedValue]
    ).tupled

  val fourthSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], EncryptedValue, EncryptedValue)] = (
    (__ \ "vouchersAndCreditCardsQuestion").formatNullable[EncryptedValue] and
      (__ \ "nonCashQuestion").formatNullable[EncryptedValue] and
      (__ \ "submittedOn").formatNullable[EncryptedValue] and
      (__ \ "isUsingCustomerData").format[EncryptedValue] and
      (__ \ "isBenefitsReceived").format[EncryptedValue]
    ).tupled

  implicit val format: OFormat[EncryptedBenefitsViewModel] = {
    (firstSetOfFields and secondSetOfFields and thirdSetOfFields and fourthSetOfFields).apply({
      case (
        (carVanFuelModel, accommodationRelocationModel, travelEntertainmentModel, utilitiesAndServicesModel, medicalChildcareEducationModel,
        incomeTaxAndCostsModel, assets, assetTransfer, expenses, taxableExpenses, otherItems),
        (vouchersAndCreditCards, nonCash),
        (assetsQuestion, assetTransferQuestion, expensesQuestion, taxableExpensesQuestion, otherItemsQuestion),
        (vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived)
        ) =>
        EncryptedBenefitsViewModel(
          carVanFuelModel, accommodationRelocationModel, travelEntertainmentModel, utilitiesAndServicesModel, medicalChildcareEducationModel,
          incomeTaxAndCostsModel, assets, assetTransfer, expenses, taxableExpenses, otherItems, vouchersAndCreditCards, nonCash,
          assetsQuestion, assetTransferQuestion, expensesQuestion, taxableExpensesQuestion, otherItemsQuestion,
          vouchersAndCreditCardsQuestion, nonCashQuestion, submittedOn, isUsingCustomerData, isBenefitsReceived
        )
    }, {
      benefits =>
        (
          (benefits.carVanFuelModel, benefits.accommodationRelocationModel, benefits.travelEntertainmentModel, benefits.utilitiesAndServicesModel,
            benefits.medicalChildcareEducationModel, benefits.incomeTaxAndCostsModel, benefits.assets, benefits.assetTransfer, benefits.expenses,
            benefits.taxableExpenses, benefits.otherItems),
          (benefits.vouchersAndCreditCards, benefits.nonCash),
          (benefits.assetsQuestion, benefits.assetTransferQuestion, benefits.expensesQuestion, benefits.taxableExpensesQuestion, benefits.otherItemsQuestion),
          (benefits.vouchersAndCreditCardsQuestion, benefits.nonCashQuestion, benefits.submittedOn,
            benefits.isUsingCustomerData, benefits.isBenefitsReceived)
        )
    })
  }
}