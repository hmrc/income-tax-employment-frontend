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

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class BenefitsViewModel(carVanFuelModel: Option[CarVanFuelModel] = None,
                             accommodationRelocationModel: Option[AccommodationRelocationModel] = None,
                             travelEntertainmentModel: Option[TravelEntertainmentModel] = None,
                             utilitiesAndServicesModel: Option[UtilitiesAndServicesModel] = None,
                             medicalChildcareEducationModel: Option[MedicalChildcareEducationModel] = None,
                             incomeTaxAndCostsModel: Option[IncomeTaxAndCostsModel] = None,
                             reimbursedCostsVouchersAndNonCashModel: Option[ReimbursedCostsVouchersAndNonCashModel] = None,
                             assetsModel: Option[AssetsModel] = None,
                             submittedOn: Option[String] = None,
                             isUsingCustomerData: Boolean,
                             isBenefitsReceived: Boolean = false
                            ) {

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
    reimbursedCostsVouchersAndNonCashModel.flatMap(_.expenses).isDefined ||
      reimbursedCostsVouchersAndNonCashModel.flatMap(_.taxableExpenses).isDefined ||
      reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCards).isDefined ||
      reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCash).isDefined ||
      reimbursedCostsVouchersAndNonCashModel.flatMap(_.otherItems).isDefined

  val assetsDetailsPopulated: Boolean =
    assetsModel.flatMap(_.assets).isDefined ||
      assetsModel.flatMap(_.assetTransfer).isDefined

  def toBenefits: Benefits = {
    Benefits(
      accommodationRelocationModel.flatMap(_.accommodation), assetsModel.flatMap(_.assets), assetsModel.flatMap(_.assetTransfer),
      medicalChildcareEducationModel.flatMap(_.beneficialLoan),
      carVanFuelModel.flatMap(_.car), carVanFuelModel.flatMap(_.carFuel), medicalChildcareEducationModel.flatMap(_.educationalServices),
      travelEntertainmentModel.flatMap(_.entertaining), reimbursedCostsVouchersAndNonCashModel.flatMap(_.expenses),
      medicalChildcareEducationModel.flatMap(_.medicalInsurance),
      utilitiesAndServicesModel.flatMap(_.telephone), utilitiesAndServicesModel.flatMap(_.service),
      reimbursedCostsVouchersAndNonCashModel.flatMap(_.taxableExpenses), carVanFuelModel.flatMap(_.van),
      carVanFuelModel.flatMap(_.vanFuel), carVanFuelModel.flatMap(_.mileage), accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses),
      medicalChildcareEducationModel.flatMap(_.nurseryPlaces), reimbursedCostsVouchersAndNonCashModel.flatMap(_.otherItems),
      incomeTaxAndCostsModel.flatMap(_.paymentsOnEmployeesBehalf),
      travelEntertainmentModel.flatMap(_.personalIncidentalExpenses), accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses),
      utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions), utilitiesAndServicesModel.flatMap(_.employerProvidedServices),
      incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirector), travelEntertainmentModel.flatMap(_.travelAndSubsistence),
      reimbursedCostsVouchersAndNonCashModel.flatMap(_.vouchersAndCreditCards), reimbursedCostsVouchersAndNonCashModel.flatMap(_.nonCash)
    )
  }
}

object BenefitsViewModel {

  def clear(isUsingCustomerData: Boolean): BenefitsViewModel =
    BenefitsViewModel(isUsingCustomerData = isUsingCustomerData)

  implicit val format: OFormat[BenefitsViewModel] = Json.format[BenefitsViewModel]

}

case class EncryptedBenefitsViewModel(carVanFuelModel: Option[EncryptedCarVanFuelModel] = None,
                                      accommodationRelocationModel: Option[EncryptedAccommodationRelocationModel] = None,
                                      travelEntertainmentModel: Option[EncryptedTravelEntertainmentModel] = None,
                                      utilitiesAndServicesModel: Option[EncryptedUtilitiesAndServicesModel] = None,
                                      medicalChildcareEducationModel: Option[EncryptedMedicalChildcareEducationModel] = None,
                                      incomeTaxAndCostsModel: Option[EncryptedIncomeTaxAndCostsModel] = None,
                                      reimbursedCostsVouchersAndNonCashModel: Option[EncryptedReimbursedCostsVouchersAndNonCashModel] = None,
                                      assetsModel: Option[EncryptedAssetsModel] = None,
                                      submittedOn: Option[EncryptedValue] = None,
                                      isUsingCustomerData: EncryptedValue,
                                      isBenefitsReceived: EncryptedValue)

object EncryptedBenefitsViewModel {
  implicit val format: OFormat[EncryptedBenefitsViewModel] = Json.format[EncryptedBenefitsViewModel]
}
