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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Json, OFormat, __}

case class DecodedBenefitsData(accommodation: Option[BigDecimal],
                                assets: Option[BigDecimal],
                                assetTransfer: Option[BigDecimal],
                                beneficialLoan: Option[BigDecimal],
                                car: Option[BigDecimal],
                                carFuel: Option[BigDecimal],
                                educationalServices: Option[BigDecimal],
                                entertaining: Option[BigDecimal],
                                expenses: Option[BigDecimal],
                                medicalInsurance: Option[BigDecimal],
                                telephone: Option[BigDecimal],
                                service: Option[BigDecimal],
                                taxableExpenses: Option[BigDecimal],
                                van: Option[BigDecimal],
                                vanFuel: Option[BigDecimal],
                                mileage: Option[BigDecimal],
                                nonQualifyingRelocationExpenses: Option[BigDecimal],
                                nurseryPlaces: Option[BigDecimal],
                                otherItems: Option[BigDecimal],
                                paymentsOnEmployeesBehalf: Option[BigDecimal],
                                personalIncidentalExpenses: Option[BigDecimal],
                                qualifyingRelocationExpenses: Option[BigDecimal],
                                employerProvidedProfessionalSubscriptions: Option[BigDecimal],
                                employerProvidedServices: Option[BigDecimal],
                                incomeTaxPaidByDirector: Option[BigDecimal],
                                travelAndSubsistence: Option[BigDecimal],
                                vouchersAndCreditCards: Option[BigDecimal],
                                nonCash: Option[BigDecimal])

object DecodedBenefitsData {
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

  implicit val format: OFormat[DecodedBenefitsData] = {
    (firstSetOfFields and secondSetOfFields).apply({
      case (
        (accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining,
        expenses, medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses),
        (employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
        vouchersAndCreditCards, nonCash)
        ) =>
        DecodedBenefitsData(
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

case class DecodedAmendNewBenefitsPayload(priorEmploymentBenefitsData: DecodedBenefitsData, employmentBenefitsData: DecodedBenefitsData)

object DecodedAmendNewBenefitsPayload {
  implicit val format: OFormat[DecodedAmendNewBenefitsPayload] = Json.format[DecodedAmendNewBenefitsPayload]
}
