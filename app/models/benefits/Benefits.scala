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

case class Benefits(accommodation: Option[BigDecimal] = None,
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
                    nonCash: Option[BigDecimal] = None) {

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

  val hasBenefitsPopulated: Boolean =
    vehicleDetailsPopulated || accommodationDetailsPopulated || travelDetailsPopulated ||
      utilitiesDetailsPopulated || medicalDetailsPopulated || incomeTaxDetailsPopulated ||
      reimbursedDetailsPopulated || assetsDetailsPopulated

  def carVanFuelSection(cyaBenefits: Option[BenefitsViewModel] = None): Option[CarVanFuelModel] = {
    if (cyaBenefits.isDefined && cyaBenefits.map(_.carVanFuelModel).isDefined) {
      cyaBenefits.flatMap(_.carVanFuelModel)
    } else {
      Some(CarVanFuelModel(
        Some(vehicleDetailsPopulated),
        carQuestion = Some(car.isDefined),
        car = car,
        carFuelQuestion = Some(carFuel.isDefined),
        carFuel = carFuel,
        vanQuestion = Some(van.isDefined),
        van = van,
        vanFuelQuestion = Some(vanFuel.isDefined),
        vanFuel = vanFuel,
        mileageQuestion = Some(mileage.isDefined),
        mileage = mileage
      ))
    }
  }

  def accommodationRelocationSection(cyaBenefits: Option[BenefitsViewModel] = None): Option[AccommodationRelocationModel] = {
    if (cyaBenefits.isDefined && cyaBenefits.map(_.accommodationRelocationModel).isDefined) {
      cyaBenefits.flatMap(_.accommodationRelocationModel)
    } else {
      Some(AccommodationRelocationModel(
        Some(accommodationDetailsPopulated),
        accommodationQuestion = Some(accommodation.isDefined),
        accommodation = accommodation,
        qualifyingRelocationExpensesQuestion = Some(qualifyingRelocationExpenses.isDefined),
        qualifyingRelocationExpenses = qualifyingRelocationExpenses,
        nonQualifyingRelocationExpensesQuestion = Some(nonQualifyingRelocationExpenses.isDefined),
        nonQualifyingRelocationExpenses = nonQualifyingRelocationExpenses
      ))
    }
  }

  def travelEntertainmentSection(cyaBenefits: Option[BenefitsViewModel] = None): Option[TravelEntertainmentModel] = {
    if (cyaBenefits.isDefined && cyaBenefits.map(_.travelEntertainmentModel).isDefined) {
      cyaBenefits.flatMap(_.travelEntertainmentModel)
    } else {
      Some(TravelEntertainmentModel(
        Some(travelDetailsPopulated),
        travelAndSubsistenceQuestion = Some(travelAndSubsistence.isDefined),
        travelAndSubsistence = travelAndSubsistence,
        personalIncidentalExpensesQuestion = Some(personalIncidentalExpenses.isDefined),
        personalIncidentalExpenses = personalIncidentalExpenses,
        entertainingQuestion = Some(entertaining.isDefined),
        entertaining = entertaining
      ))
    }
  }

  def utilitiesAndServicesSection(cyaBenefits: Option[BenefitsViewModel] = None): Option[UtilitiesAndServicesModel] = {
    if (cyaBenefits.isDefined && cyaBenefits.map(_.utilitiesAndServicesModel).isDefined) {
      cyaBenefits.flatMap(_.utilitiesAndServicesModel)
    } else {
      Some(UtilitiesAndServicesModel(
        Some(utilitiesDetailsPopulated),
        telephoneQuestion = Some(telephone.isDefined),
        telephone = telephone,
        employerProvidedServicesQuestion = Some(employerProvidedServices.isDefined),
        employerProvidedServices = employerProvidedServices,
        employerProvidedProfessionalSubscriptionsQuestion = Some(employerProvidedProfessionalSubscriptions.isDefined),
        employerProvidedProfessionalSubscriptions = employerProvidedProfessionalSubscriptions,
        serviceQuestion = Some(service.isDefined),
        service = service
      ))
    }
  }

  def medicalChildcareEducationModel(cyaBenefits: Option[BenefitsViewModel] = None): Option[MedicalChildcareEducationModel] = {
    if (cyaBenefits.isDefined && cyaBenefits.map(_.medicalChildcareEducationModel).isDefined) {
      cyaBenefits.flatMap(_.medicalChildcareEducationModel)
    } else {
      Some(MedicalChildcareEducationModel(
        medicalChildcareEducationQuestion = Some(medicalDetailsPopulated),
        medicalInsuranceQuestion = Some(medicalInsurance.isDefined),
        medicalInsurance = medicalInsurance,
        nurseryPlacesQuestion = Some(nurseryPlaces.isDefined),
        nurseryPlaces = nurseryPlaces,
        educationalServicesQuestion = Some(educationalServices.isDefined),
        educationalServices = educationalServices,
        beneficialLoanQuestion = Some(beneficialLoan.isDefined),
        beneficialLoan = beneficialLoan
      ))
    }
  }

  def incomeTaxAndCostsModel(cyaBenefits: Option[BenefitsViewModel] = None): Option[IncomeTaxAndCostsModel] = {
    if (cyaBenefits.isDefined && cyaBenefits.map(_.incomeTaxAndCostsModel).isDefined) {
      cyaBenefits.flatMap(_.incomeTaxAndCostsModel)
    } else {
      Some(IncomeTaxAndCostsModel(
        incomeTaxOrCostsQuestion = Some(incomeTaxDetailsPopulated),
        incomeTaxPaidByDirectorQuestion = Some(incomeTaxPaidByDirector.isDefined),
        incomeTaxPaidByDirector = incomeTaxPaidByDirector,
        paymentsOnEmployeesBehalfQuestion = Some(paymentsOnEmployeesBehalf.isDefined),
        paymentsOnEmployeesBehalf = paymentsOnEmployeesBehalf
      ))
    }
  }

  def reimbursedCostsVouchersAndNonCashModel(cyaBenefits: Option[BenefitsViewModel] = None): Option[ReimbursedCostsVouchersAndNonCashModel] = {
    if (cyaBenefits.isDefined && cyaBenefits.map(_.reimbursedCostsVouchersAndNonCashModel).isDefined) {
      cyaBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)
    } else {
      Some(ReimbursedCostsVouchersAndNonCashModel(
        reimbursedCostsVouchersAndNonCashQuestion = Some(reimbursedDetailsPopulated),
        expensesQuestion = Some(expenses.isDefined),
        expenses = expenses,
        taxableExpensesQuestion = Some(taxableExpenses.isDefined),
        taxableExpenses = taxableExpenses,
        vouchersAndCreditCardsQuestion = Some(vouchersAndCreditCards.isDefined),
        vouchersAndCreditCards = vouchersAndCreditCards,
        nonCashQuestion = Some(nonCash.isDefined),
        nonCash = nonCash,
        otherItemsQuestion = Some(otherItems.isDefined),
        otherItems = otherItems
      ))
    }
  }

  def assetsModel(cyaBenefits: Option[BenefitsViewModel] = None): Option[AssetsModel] = {
    if (cyaBenefits.isDefined && cyaBenefits.map(_.assetsModel).isDefined) {
      cyaBenefits.flatMap(_.assetsModel)
    } else {
      Some(AssetsModel(
        sectionQuestion = Some(assetsDetailsPopulated),
        assetsQuestion = Some(assets.isDefined),
        assets = assets,
        assetTransferQuestion = Some(assetTransfer.isDefined),
        assetTransfer = assetTransfer
      ))
    }
  }

  def benefitsPopulated(cyaBenefits: Option[BenefitsViewModel] = None): Boolean = {
    val hasBenefits: Boolean = cyaBenefits.exists(_.isBenefitsReceived)
    hasBenefits || vehicleDetailsPopulated || accommodationDetailsPopulated || travelDetailsPopulated || utilitiesDetailsPopulated ||
      medicalDetailsPopulated || incomeTaxDetailsPopulated || reimbursedDetailsPopulated || assetsDetailsPopulated
  }

  def toBenefitsViewModel(isUsingCustomerData: Boolean, submittedOn: Option[String] = None,
                          cyaBenefits: Option[BenefitsViewModel] = None): BenefitsViewModel = {
    BenefitsViewModel(
      carVanFuelModel = carVanFuelSection(cyaBenefits),
      accommodationRelocationModel = accommodationRelocationSection(cyaBenefits),
      travelEntertainmentModel = travelEntertainmentSection(cyaBenefits),
      utilitiesAndServicesModel = utilitiesAndServicesSection(cyaBenefits),
      medicalChildcareEducationModel = medicalChildcareEducationModel(cyaBenefits),
      incomeTaxAndCostsModel = incomeTaxAndCostsModel(cyaBenefits),
      reimbursedCostsVouchersAndNonCashModel = reimbursedCostsVouchersAndNonCashModel(cyaBenefits),
      assetsModel = assetsModel(cyaBenefits),
      submittedOn = submittedOn,
      isUsingCustomerData = isUsingCustomerData,
      isBenefitsReceived = benefitsPopulated(cyaBenefits)
    )
  }
}

object Benefits {
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

  implicit val format: OFormat[Benefits] = {
    (firstSetOfFields and secondSetOfFields).apply({
      case (
        (accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining,
        expenses, medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses),
        (employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
        vouchersAndCreditCards, nonCash)
        ) =>
        Benefits(
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
