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

package models

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OFormat, __}

case class EmploymentBenefitsModel(
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
                                    nonCash: Option[BigDecimal] = None
                        ){

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
}

object EmploymentBenefitsModel {
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

  implicit val format: OFormat[EmploymentBenefitsModel] = {
    (firstSetOfFields and secondSetOfFields).apply({
      case (
        (accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining,
        expenses, medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses),
        (employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
        vouchersAndCreditCards, nonCash)
        ) =>
        EmploymentBenefitsModel(
          accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining, expenses,
          medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
          nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses,
          employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
          vouchersAndCreditCards, nonCash
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
            benefits.travelAndSubsistence, benefits.vouchersAndCreditCards, benefits.nonCash)
        )
    })
  }
}
