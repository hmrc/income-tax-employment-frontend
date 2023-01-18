/*
 * Copyright 2023 HM Revenue & Customs
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

package views.employment

import controllers.benefits.accommodation.routes._
import controllers.benefits.assets.routes._
import controllers.benefits.fuel.routes._
import controllers.benefits.income.routes._
import controllers.benefits.medical.routes._
import controllers.benefits.reimbursed.routes._
import controllers.benefits.routes.ReceiveAnyBenefitsController
import controllers.benefits.travel.routes._
import controllers.benefits.utilities.routes._
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import views.html.employment.CheckYourBenefitsView

class CheckYourBenefitsViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"

  object Selectors {
    val bannerParagraphSelector: String = ".govuk-notification-banner__heading"
    val bannerLinkSelector: String = ".govuk-notification-banner__link"
    val p1 = "#main-content > div > div > p.govuk-body"
    val p2 = "#main-content > div > div > div.govuk-inset-text"
    val returnToEmployerSelector = "#returnToEmployerBtn"
    val changeLinkCssSelector = ".govuk-summary-list__actions"

    def fieldNameSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dt"

    def fieldAmountSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd.govuk-summary-list__value"

    def fieldHeaderSelector(i: Int): String = s"#main-content > div > div > h2:nth-child($i)"
  }

  trait SpecificExpectedResults {
    def expectedP2(year: Int = taxYear): String

    val expectedH1: String
    val expectedTitle: String
    val expectedP1: String
    val companyCarHiddenText: String
    val fuelForCompanyCarHiddenText: String
    val companyVanHiddenText: String
    val fuelForCompanyVanHiddenText: String
    val mileageBenefitHiddenText: String
    val accommodationHiddenText: String
    val qualifyingRelocationCostsHiddenText: String
    val nonQualifyingRelocationCostsHiddenText: String
    val travelAndSubsistenceHiddenText: String
    val personalCostsHiddenText: String
    val entertainmentHiddenText: String
    val telephoneHiddenText: String
    val servicesProvidedHiddenText: String
    val profSubscriptionsHiddenText: String
    val otherServicesHiddenText: String
    val medicalInsHiddenText: String
    val nurseryHiddenText: String
    val beneficialLoansHiddenText: String
    val educationalHiddenText: String
    val incomeTaxPaidHiddenText: String
    val incurredCostsPaidHiddenText: String
    val nonTaxableHiddenText: String
    val taxableCostsHiddenText: String
    val vouchersHiddenText: String
    val nonCashHiddenText: String
    val otherBenefitsHiddenText: String
    val assetsHiddenText: String
    val assetTransfersHiddenText: String
    val companyCarAmountHiddenText: String
    val fuelForCompanyCarAmountHiddenText: String
    val companyVanAmountHiddenText: String
    val fuelForCompanyVanAmountHiddenText: String
    val mileageBenefitAmountHiddenText: String
    val accommodationAmountHiddenText: String
    val qualifyingRelocationCostsAmountHiddenText: String
    val nonQualifyingRelocationCostsAmountHiddenText: String
    val travelAndSubsistenceAmountHiddenText: String
    val personalCostsAmountHiddenText: String
    val entertainmentAmountHiddenText: String
    val telephoneAmountHiddenText: String
    val servicesProvidedAmountHiddenText: String
    val profSubscriptionsAmountHiddenText: String
    val otherServicesAmountHiddenText: String
    val medicalInsAmountHiddenText: String
    val nurseryAmountHiddenText: String
    val beneficialLoansAmountHiddenText: String
    val educationalAmountHiddenText: String
    val incomeTaxPaidAmountHiddenText: String
    val incurredCostsPaidAmountHiddenText: String
    val nonTaxableAmountHiddenText: String
    val taxableCostsAmountHiddenText: String
    val vouchersAmountHiddenText: String
    val nonCashAmountHiddenText: String
    val otherBenefitsAmountHiddenText: String
    val assetsAmountHiddenText: String
    val assetTransfersAmountHiddenText: String
    val carSubheadingHiddenText: String
    val accommodationSubheadingHiddenText: String
    val travelSubheadingHiddenText: String
    val utilitiesSubheadingHiddenText: String
    val medicalSubheadingHiddenText: String
    val incomeTaxSubheadingHiddenText: String
    val reimbursedSubheadingHiddenText: String
    val assetsSubheadingHiddenText: String
    val benefitsReceivedHiddenText: String
  }

  trait CommonExpectedResults {

    def expectedCaption(year: Int = taxYear): String

    val bannerParagraph: String
    val bannerLinkText: String
    val employerName: String
    val changeText: String
    val vehicleHeader: String
    val companyCar: String
    val fuelForCompanyCar: String
    val companyVan: String
    val fuelForCompanyVan: String
    val mileageBenefit: String
    val accommodationHeader: String
    val accommodation: String
    val qualifyingRelocationCosts: String
    val nonQualifyingRelocationCosts: String
    val travelHeader: String
    val travelAndSubsistence: String
    val personalCosts: String
    val entertainment: String
    val utilitiesHeader: String
    val telephone: String
    val servicesProvided: String
    val profSubscriptions: String
    val otherServices: String
    val medicalHeader: String
    val medicalIns: String
    val nursery: String
    val beneficialLoans: String
    val educational: String
    val incomeTaxHeader: String
    val incomeTaxPaid: String
    val incurredCostsPaid: String
    val reimbursedHeader: String
    val nonTaxable: String
    val taxableCosts: String
    val vouchers: String
    val nonCash: String
    val otherBenefits: String
    val assetsHeader: String
    val assets: String
    val assetTransfers: String
    val companyCarAmount: String
    val fuelForCompanyCarAmount: String
    val companyVanAmount: String
    val fuelForCompanyVanAmount: String
    val mileageBenefitAmount: String
    val accommodationAmount: String
    val qualifyingRelocationCostsAmount: String
    val nonQualifyingRelocationCostsAmount: String
    val travelAndSubsistenceAmount: String
    val personalCostsAmount: String
    val entertainmentAmount: String
    val telephoneAmount: String
    val servicesProvidedAmount: String
    val profSubscriptionsAmount: String
    val otherServicesAmount: String
    val medicalInsAmount: String
    val nurseryAmount: String
    val beneficialLoansAmount: String
    val educationalAmount: String
    val incomeTaxPaidAmount: String
    val incurredCostsPaidAmount: String
    val nonTaxableAmount: String
    val taxableCostsAmount: String
    val vouchersAmount: String
    val nonCashAmount: String
    val otherBenefitsAmount: String
    val assetsAmount: String
    val assetTransfersAmount: String
    val carSubheading: String
    val accommodationSubheading: String
    val travelSubheading: String
    val utilitiesSubheading: String
    val medicalSubheading: String
    val incomeTaxSubheading: String
    val reimbursedSubheading: String
    val assetsSubheading: String
    val yes: String
    val no: String
    val benefitsReceived: String
    val saveAndContinue: String
    val returnToEmployerText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(year: Int = taxYear): String = s"Employment benefits for 6 April ${year - 1} to 5 April $year"

    val bannerParagraph: String = "You cannot update employment benefits until you add missing employment details."
    val bannerLinkText: String = "add missing employment details."
    val employerName: String = "maggie"
    val changeText: String = "Change"
    val vehicleHeader = "Vehicles, fuel and mileage"
    val companyCar = "Company car"
    val fuelForCompanyCar = "Fuel for company car"
    val companyVan = "Company van"
    val fuelForCompanyVan = "Fuel for company van"
    val mileageBenefit = "Mileage benefit"
    val accommodationHeader = "Accommodation and relocation"
    val accommodation = "Living accommodation"
    val qualifyingRelocationCosts = "Qualifying relocation"
    val nonQualifyingRelocationCosts = "Non-qualifying relocation"
    val travelHeader = "Travel and entertainment"
    val travelAndSubsistence = "Travel and subsistence"
    val personalCosts = "Incidental overnight costs"
    val entertainment = "Entertainment"
    val utilitiesHeader = "Utilities and general services"
    val telephone = "Telephone"
    val servicesProvided = "Services provided by employer"
    val profSubscriptions = "Professional fees or subscriptions"
    val otherServices = "Other services"
    val medicalHeader = "Medical, dental, childcare, education benefits and loans"
    val medicalIns = "Medical or dental insurance"
    val nursery = "Childcare"
    val beneficialLoans = "Beneficial loans"
    val educational = "Educational services"
    val incomeTaxHeader = "Income Tax and incurred costs"
    val incomeTaxPaid = "Income Tax paid by employer"
    val incurredCostsPaid = "Incurred costs paid by employer"
    val reimbursedHeader = "Reimbursed costs, vouchers and non-cash benefits"
    val nonTaxable = "Non-taxable costs reimbursed by employer"
    val taxableCosts = "Taxable costs reimbursed by employer"
    val vouchers = "Vouchers or credit cards"
    val nonCash = "Non-cash benefits"
    val otherBenefits = "Other benefits"
    val assetsHeader = "Assets"
    val assets = "Assets to use"
    val assetTransfers = "Assets to keep"
    val companyCarAmount = "Amount for company car"
    val fuelForCompanyCarAmount = "Amount of company car fuel"
    val companyVanAmount = "Amount for company van"
    val fuelForCompanyVanAmount = "Amount for company van fuel"
    val mileageBenefitAmount = "Amount for mileage benefit"
    val accommodationAmount = "Amount for living accommodation"
    val qualifyingRelocationCostsAmount = "Amount for qualifying relocation"
    val nonQualifyingRelocationCostsAmount = "Amount for non-qualifying relocation"
    val travelAndSubsistenceAmount = "Amount for travel and subsistence"
    val personalCostsAmount = "Amount for incidental overnight costs"
    val entertainmentAmount = "Amount for entertainment"
    val telephoneAmount = "Amount for telephone"
    val servicesProvidedAmount = "Amount for services provided by employer"
    val profSubscriptionsAmount = "Amount for professional fees or subscriptions"
    val otherServicesAmount = "Amount for other services"
    val medicalInsAmount = "Amount for medical or dental insurance"
    val nurseryAmount = "Amount for childcare"
    val beneficialLoansAmount = "Amount for beneficial loans"
    val educationalAmount = "Amount for educational services"
    val incomeTaxPaidAmount = "Amount of Income Tax paid by employer"
    val incurredCostsPaidAmount = "Amount of incurred costs paid by employer"
    val nonTaxableAmount = "Amount of non-taxable costs reimbursed by employer"
    val taxableCostsAmount = "Amount of taxable costs reimbursed by employer"
    val vouchersAmount = "Amount for vouchers or credit cards"
    val nonCashAmount = "Amount for non-cash benefits"
    val otherBenefitsAmount = "Amount for other benefits"
    val assetsAmount = "Amount for assets to use"
    val assetTransfersAmount = "Amount for assets to keep"
    val carSubheading: String = "Car, van or fuel"
    val accommodationSubheading: String = "Accommodation or relocation"
    val travelSubheading: String = "Travel or entertainment"
    val utilitiesSubheading: String = "Utilities or general services"
    val medicalSubheading: String = "Medical, dental, childcare, education benefits or loans"
    val incomeTaxSubheading: String = "Income Tax or incurred costs"
    val reimbursedSubheading: String = "Reimbursed costs, vouchers or non-cash benefits"
    val assetsSubheading: String = "Assets"
    val yes: String = "Yes"
    val no: String = "No"
    val benefitsReceived = "Benefits received"
    val saveAndContinue: String = "Save and continue"
    val returnToEmployerText: String = "Return to employer"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(year: Int = taxYear): String = s"Buddiannau cyflogaeth ar gyfer 6 Ebrill ${year - 1} i 5 Ebrill $year"

    val bannerParagraph: String = "Ni allwch ddiweddaru buddiannau cyflogaeth hyd nes eich bod yn ychwanegu manylion cyflogaeth sydd ar goll."
    val bannerLinkText: String = "ychwanegu manylion cyflogaeth sydd ar goll."
    val employerName: String = "maggie"
    val changeText: String = "Newid"
    val vehicleHeader = "Cerbydau, tanwydd a chostau milltiroedd"
    val companyCar = "Car cwmni"
    val fuelForCompanyCar = "Tanwydd ar gyfer ceir cwmni"
    val companyVan = "Fan cwmni"
    val fuelForCompanyVan = "Tanwydd ar gyfer faniau cwmni"
    val mileageBenefit = "Buddiant milltiroedd"
    val accommodationHeader = "Llety ac adleoli"
    val accommodation = "Llety byw"
    val qualifyingRelocationCosts = "Costau adleoli cymwys"
    val nonQualifyingRelocationCosts = "Costau adleoli anghymwys"
    val travelHeader = "Teithio a gwesteia"
    val travelAndSubsistence = "Teithio a chynhaliaeth"
    val personalCosts = "Mân gostau dros nos"
    val entertainment = "Gwesteia"
    val utilitiesHeader = "Cyfleustodau a gwasanaethau cyffredinol"
    val telephone = "Ffôn"
    val servicesProvided = "Gwasanaethau a ddarparwyd gan y cyflogwr"
    val profSubscriptions = "Ffioedd neu danysgrifiadau proffesiynol"
    val otherServices = "Gwasanaethau eraill"
    val medicalHeader = "Buddiannau neu fenthyciadau meddygol, deintyddol, gofal plant neu addysg"
    val medicalIns = "Yswiriant meddygol neu ddeintyddol"
    val nursery = "Gofal Plant"
    val beneficialLoans = "Benthyciad buddiannol"
    val educational = "Gwasanaethau addysg"
    val incomeTaxHeader = "Treth Incwm a chostau a ysgwyddwyd"
    val incomeTaxPaid = "Treth Incwm a dalwyd gan y cyflogwr"
    val incurredCostsPaid = "Costau a ysgwyddwyd sydd wedi’u talu gan y cyflogwr"
    val reimbursedHeader = "Costau a ad-dalwyd, talebau a buddiannau sydd ddim yn arian parod"
    val nonTaxable = "Costau anhrethadwy a ad-dalwyd gan y cyflogwr"
    val taxableCosts = "Costau trethadwy a ad-dalwyd gan y cyflogwr"
    val vouchers = "Talebau neu gardiau credyd"
    val nonCash = "Buddiannau sydd ddim yn arian parod"
    val otherBenefits = "Buddiannau eraill"
    val assetsHeader = "Asedion"
    val assets = "Asedion i’w defnyddio"
    val assetTransfers = "Asedion i’w cadw"
    val companyCarAmount = "Swm ar gyfer car cwmni"
    val fuelForCompanyCarAmount = "Swm y tanwydd car cwmni"
    val companyVanAmount = "Swm ar gyfer fan cwmni"
    val fuelForCompanyVanAmount = "Swm ar gyfer tanwydd fan cwmni"
    val mileageBenefitAmount = "Swm ar gyfer buddiant milltiroedd"
    val accommodationAmount = "Swm ar gyfer llety byw"
    val qualifyingRelocationCostsAmount = "Swm ar gyfer adleoli cymwys"
    val nonQualifyingRelocationCostsAmount = "Swm ar gyfer costau adleoli anghymwys"
    val travelAndSubsistenceAmount = "Swm ar gyfer costau teithio a chynhaliaeth"
    val personalCostsAmount = "Swm ar gyfer mân gostau dros nos"
    val entertainmentAmount = "Swm ar gyfer gwesteia"
    val telephoneAmount = "Swm ar gyfer y Ffôn"
    val servicesProvidedAmount = "Swm ar gyfer y gwasanaethau a ddarparwyd gan y cyflogwr"
    val profSubscriptionsAmount = "Swm ar gyfer ffioedd neu danysgrifiadau proffesiynol"
    val otherServicesAmount = "Swm ar gyfer gwasanaethau eraill"
    val medicalInsAmount = "Swm ar gyfer yswiriant meddygol neu ddeintyddol"
    val nurseryAmount = "Swm ar gyfer gofal plant"
    val beneficialLoansAmount = "Swm ar gyfer benthyciadau buddiannol"
    val educationalAmount = "Swm ar gyfer gwasanaethau addysgol"
    val incomeTaxPaidAmount = "Swm y Dreth Incwm a dalwyd gan y cyflogwr"
    val incurredCostsPaidAmount = "Y swm y cafodd ei dalu gan y cyflogwr am gostau a ysgwyddwyd"
    val nonTaxableAmount = "Swm y costau anhrethadwy a ad-dalwyd gan y cyflogwr"
    val taxableCostsAmount = "Swm y costau trethadwy a ad-dalwyd gan y cyflogwr"
    val vouchersAmount = "Swm ar gyfer talebau neu gardiau credyd"
    val nonCashAmount = "Swm ar gyfer buddiannau sydd ddim yn arian parod"
    val otherBenefitsAmount = "Swm ar gyfer buddiannau eraill"
    val assetsAmount = "Swm ar gyfer asedion i’w defnyddio"
    val assetTransfersAmount = "Swm ar gyfer asedion i’w cadw"
    val carSubheading: String = "Car, fan neu danwydd"
    val accommodationSubheading: String = "Llety neu adleoli"
    val travelSubheading: String = "Teithio neu westeia"
    val utilitiesSubheading: String = "Cyfleustodau neu wasanaethau cyffredinol"
    val medicalSubheading: String = "Buddiannau neu fenthyciadau meddygol, deintyddol, gofal plant neu addysg"
    val incomeTaxSubheading: String = "Treth Incwm neu gostau a ysgwyddwyd"
    val reimbursedSubheading: String = "Costau a ad-dalwyd, talebau a buddiannau sydd ddim yn arian parod"
    val assetsSubheading: String = "Asedion"
    val yes: String = "Iawn"
    val no: String = "Na"
    val benefitsReceived = "Buddiannau a gafwyd"
    val saveAndContinue: String = "Cadw ac yn eich blaen"
    val returnToEmployerText: String = "Dychwelyd at y cyflogwr"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    def expectedP2(year: Int = taxYear): String = s"You cannot update your employment benefits until 6 April $year."

    val expectedH1: String = "Check your employment benefits"
    val expectedTitle: String = "Check your employment benefits"
    val expectedP1: String = "Your employment benefits are based on the information we already hold about you."
    val companyCarHiddenText: String = "Change if you got a company car as an employment benefit from this company"
    val fuelForCompanyCarHiddenText: String = "Change if you got a company car fuel as an employment benefit from this company"
    val companyVanHiddenText: String = "Change if you got a company van as an employment benefit from this company"
    val fuelForCompanyVanHiddenText: String = "Change if you got a company van fuel as an employment benefit from this company"
    val mileageBenefitHiddenText: String = "Change if you got mileage as an employment benefit for using your own car"
    val accommodationHiddenText: String = "Change if you got living accommodation as an employment benefit from this company"
    val qualifyingRelocationCostsHiddenText: String = "Change if you got qualifying relocation as an employment benefit from this company"
    val nonQualifyingRelocationCostsHiddenText: String = "Change if you got non-qualifying relocation as an employment benefit from this company"
    val travelAndSubsistenceHiddenText: String = "Change if you got travel and overnight stays as an employment benefit from this company"
    val personalCostsHiddenText: String = "Change if you got incidental overnight costs as an employment benefit from this company"
    val entertainmentHiddenText: String = "Change if you got entertainment as an employment benefit from this company"
    val telephoneHiddenText: String = "Change if you got a telephone as an employment benefit from this company"
    val servicesProvidedHiddenText: String = "Change if you got services provided by your employer as an employment benefit from this company"
    val profSubscriptionsHiddenText: String = "Change if you got professional fees or subscriptions as an employment benefit from this company"
    val otherServicesHiddenText: String = "Change if you got other services as an employment benefit from this company"
    val medicalInsHiddenText: String = "Change if you got medical or dental insurance as an employment benefit from this company"
    val nurseryHiddenText: String = "Change if you got childcare as an employment benefit from this company"
    val beneficialLoansHiddenText: String = "Change if you got beneficial loans as an employment benefit from this company"
    val educationalHiddenText: String = "Change if you got educational services as an employment benefit from this company"
    val incomeTaxPaidHiddenText: String = "Change if you got Income Tax paid as an employment benefit from this company"
    val incurredCostsPaidHiddenText: String = "Change if you got incurred costs paid as an employment benefit from this company"
    val nonTaxableHiddenText: String = "Change if you got non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsHiddenText: String = "Change if you got taxable costs reimbursed as an employment benefit from this company"
    val vouchersHiddenText: String = "Change if you got vouchers or credit cards as an employment benefit from this company"
    val nonCashHiddenText: String = "Change if you got non-cash employment benefit from this company"
    val otherBenefitsHiddenText: String = "Change if you got other employment benefit from this company"
    val assetsHiddenText: String = "Change if you got assets to use as an employment benefit from this company"
    val assetTransfersHiddenText: String = "Change if you got assets to keep as an employment benefit from this company"
    val companyCarAmountHiddenText: String = "Change the amount for company car as an employment benefit you got"
    val fuelForCompanyCarAmountHiddenText: String = "Change the amount for company car fuel as an employment benefit you got from this company"
    val companyVanAmountHiddenText: String = "Change the amount for company van as an employment benefit you got"
    val fuelForCompanyVanAmountHiddenText: String = "Change the amount for company van fuel as an employment benefit you got from this company"
    val mileageBenefitAmountHiddenText: String = "Change the amount for mileage as an employment benefit you got for using your own car"
    val accommodationAmountHiddenText: String = "Change the amount for living accommodation as an employment benefit you got from this company"
    val qualifyingRelocationCostsAmountHiddenText: String = "Change the amount for qualifying relocation as an employment benefit you got from this company"
    val nonQualifyingRelocationCostsAmountHiddenText: String = "Change the amount for non-qualifying relocation as an employment benefit you got from this company"
    val travelAndSubsistenceAmountHiddenText: String = "Change the amount for travel or overnight stays as an employment benefit you got from this company"
    val personalCostsAmountHiddenText: String = "Change the amount for incidental overnight costs as an employment benefit you got from this company"
    val entertainmentAmountHiddenText: String = "Change the amount for entertainment as an employment benefit you got from this company"
    val telephoneAmountHiddenText: String = "Change the amount for telephone as an employment benefit you got from this company"
    val servicesProvidedAmountHiddenText: String = "Change the amount for services provided by your employer as an employment benefit you got from this company"
    val profSubscriptionsAmountHiddenText: String = "Change the amount for professional fees or subscriptions as an employment benefit you got from this company"
    val otherServicesAmountHiddenText: String = "Change the amount for other services as an employment benefit you got from this company"
    val medicalInsAmountHiddenText: String = "Change the amount for medical or dental insurance as an employment benefit you got from this company"
    val nurseryAmountHiddenText: String = "Change the amount for childcare employment benefit you got from this company"
    val beneficialLoansAmountHiddenText: String = "Change the amount for beneficial loans as an employment benefit you got from this company"
    val educationalAmountHiddenText: String = "Change the amount for educational services as an employment benefit you got from this company"
    val incomeTaxPaidAmountHiddenText: String = "Change the amount for Income Tax paid as an employment benefit you got from this company"
    val incurredCostsPaidAmountHiddenText: String = "Change the amount for incurred costs paid as an employment benefit you got from this company"
    val nonTaxableAmountHiddenText: String = "Change the amount you got for non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsAmountHiddenText: String = "Change the amount you got for taxable costs reimbursed as an employment benefit from this company"
    val vouchersAmountHiddenText: String = "Change the amount you got for vouchers or credit cards as an employment benefit from this company"
    val nonCashAmountHiddenText: String = "Change the amount you got for non-cash employment benefit from this company"
    val otherBenefitsAmountHiddenText: String = "Change the amount you got for other employment benefit from this company"
    val assetsAmountHiddenText: String = "Change the amount you got for assets to use as an employment benefit from this company"
    val assetTransfersAmountHiddenText: String = "Change the amount you got for assets to keep as an employment benefit from this company"
    val carSubheadingHiddenText: String = "Change if you got a car, van or fuel as an employment benefit from this company"
    val accommodationSubheadingHiddenText: String = "Change if you got accommodation or relocation as an employment benefit from this company"
    val travelSubheadingHiddenText: String = "Change if you got travel or entertainment as an employment benefit from this company"
    val utilitiesSubheadingHiddenText: String = "Change if you got utilities or general services as an employment benefit from this company"
    val medicalSubheadingHiddenText: String = "Change if you got medical, dental, childcare, education benefits or loans from this company"
    val incomeTaxSubheadingHiddenText: String = "Change if you got Income Tax or incurred costs paid as an employment benefit from this company"
    val reimbursedSubheadingHiddenText: String = "Change if you got reimbursed costs, vouchers or non-cash benefits as an employment benefit from this company"
    val assetsSubheadingHiddenText: String = "Change if you got assets as an employment benefit from this company"
    val benefitsReceivedHiddenText: String = "Change if you got employment benefits from this company"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    def expectedP2(year: Int = taxYear): String = s"You cannot update your client’s employment benefits until 6 April $year."

    val expectedH1: String = "Check your client’s employment benefits"
    val expectedTitle: String = "Check your client’s employment benefits"
    val expectedP1: String = "Your client’s employment benefits are based on the information we already hold about them."
    val companyCarHiddenText: String = "Change if your client got a company car as an employment benefit from this company"
    val fuelForCompanyCarHiddenText: String = "Change if your client got a company car fuel as an employment benefit from this company"
    val companyVanHiddenText: String = "Change if your client got a company van as an employment benefit from this company"
    val fuelForCompanyVanHiddenText: String = "Change if your client got a company van fuel as an employment benefit from this company"
    val mileageBenefitHiddenText: String = "Change if your client got mileage as an employment benefit for using their own car"
    val accommodationHiddenText: String = "Change if your client got living accommodation as an employment benefit from this company"
    val qualifyingRelocationCostsHiddenText: String = "Change if your client got qualifying relocation as an employment benefit from this company"
    val nonQualifyingRelocationCostsHiddenText: String = "Change if your client got non-qualifying relocation as an employment benefit from this company"
    val travelAndSubsistenceHiddenText: String = "Change if your client got travel and overnight stays as an employment benefit from this company"
    val personalCostsHiddenText: String = "Change if your client got incidental overnight costs as an employment benefit from this company"
    val entertainmentHiddenText: String = "Change if your client got entertainment as an employment benefit from this company"
    val telephoneHiddenText: String = "Change if your client got a telephone as an employment benefit from this company"
    val servicesProvidedHiddenText: String = "Change if your client got services provided by their employer as an employment benefit from this company"
    val profSubscriptionsHiddenText: String = "Change if your client got professional fees or subscriptions as an employment benefit from this company"
    val otherServicesHiddenText: String = "Change if your client got other services as an employment benefit from this company"
    val medicalInsHiddenText: String = "Change if your client got medical or dental insurance as an employment benefit from this company"
    val nurseryHiddenText: String = "Change if your client got childcare as an employment benefit from this company"
    val beneficialLoansHiddenText: String = "Change if your client got beneficial loans as an employment benefit from this company"
    val educationalHiddenText: String = "Change if your client got educational services as an employment benefit from this company"
    val incomeTaxPaidHiddenText: String = "Change if your client got Income Tax paid as an employment benefit from this company"
    val incurredCostsPaidHiddenText: String = "Change if your client got incurred costs paid as an employment benefit from this company"
    val nonTaxableHiddenText: String = "Change if your client got non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsHiddenText: String = "Change if your client got taxable costs reimbursed as an employment benefit from this company"
    val vouchersHiddenText: String = "Change if your client got vouchers or credit cards as an employment benefit from this company"
    val nonCashHiddenText: String = "Change if your client got non-cash employment benefit from this company"
    val otherBenefitsHiddenText: String = "Change if your client got other employment benefit from this company"
    val assetsHiddenText: String = "Change if your client got assets to use as an employment benefit from this company"
    val assetTransfersHiddenText: String = "Change if your client got assets to keep as an employment benefit from this company"
    val companyCarAmountHiddenText: String = "Change the amount for company car as an employment benefit your client got"
    val fuelForCompanyCarAmountHiddenText: String = "Change the amount for company car fuel as an employment benefit your client got from this company"
    val companyVanAmountHiddenText: String = "Change the amount for company van as an employment benefit your client got"
    val fuelForCompanyVanAmountHiddenText: String = "Change the amount for company van fuel as an employment benefit your client got from this company"
    val mileageBenefitAmountHiddenText: String = "Change the amount for mileage as an employment benefit your client got for using their own car"
    val accommodationAmountHiddenText: String = "Change the amount for living accommodation as an employment benefit your client got from this company"
    val qualifyingRelocationCostsAmountHiddenText: String = "Change the amount for qualifying relocation as an employment benefit your client got from this company"
    val nonQualifyingRelocationCostsAmountHiddenText: String = "Change the amount for non-qualifying relocation as an employment benefit your client got from this company"
    val travelAndSubsistenceAmountHiddenText: String = "Change the amount for travel or overnight stays as an employment benefit your client got from this company"
    val personalCostsAmountHiddenText: String = "Change the amount for incidental overnight costs as an employment benefit your client got from this company"
    val entertainmentAmountHiddenText: String = "Change the amount for entertainment as an employment benefit your client got from this company"
    val telephoneAmountHiddenText: String = "Change the amount for telephone as an employment benefit your client got from this company"
    val servicesProvidedAmountHiddenText: String = "Change the amount for services provided by your client’s employer as an employment benefit they got from this company"
    val profSubscriptionsAmountHiddenText: String = "Change the amount for professional fees or subscriptions as an employment benefit your client got from this company"
    val otherServicesAmountHiddenText: String = "Change the amount for other services as an employment benefit your client got from this company"
    val medicalInsAmountHiddenText: String = "Change the amount for medical or dental insurance as an employment benefit your client got from this company"
    val nurseryAmountHiddenText: String = "Change the amount for childcare employment benefit your client got from this company"
    val beneficialLoansAmountHiddenText: String = "Change the amount for beneficial loans as an employment benefit your client got from this company"
    val educationalAmountHiddenText: String = "Change the amount for educational services as an employment benefit your client got from this company"
    val incomeTaxPaidAmountHiddenText: String = "Change the amount for Income Tax paid as an employment benefit your client got from this company"
    val incurredCostsPaidAmountHiddenText: String = "Change the amount for incurred costs paid as an employment benefit your client got from this company"
    val nonTaxableAmountHiddenText: String = "Change the amount your client got for non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsAmountHiddenText: String = "Change the amount your client got for taxable costs reimbursed as an employment benefit from this company"
    val vouchersAmountHiddenText: String = "Change the amount your client got for vouchers or credit cards as an employment benefit from this company"
    val nonCashAmountHiddenText: String = "Change the amount your client got for non-cash employment benefit from this company"
    val otherBenefitsAmountHiddenText: String = "Change the amount your client got for other employment benefit from this company"
    val assetsAmountHiddenText: String = "Change the amount your client got for assets to use as an employment benefit from this company"
    val assetTransfersAmountHiddenText: String = "Change the amount your client got for assets to keep as an employment benefit from this company"
    val carSubheadingHiddenText: String = "Change if your client got a car, van or fuel as an employment benefit from this company"
    val accommodationSubheadingHiddenText: String = "Change if your client got accommodation or relocation as an employment benefit from this company"
    val travelSubheadingHiddenText: String = "Change if your client got travel or entertainment as an employment benefit from this company"
    val utilitiesSubheadingHiddenText: String = "Change if your client got utilities or general services as an employment benefit from this company"
    val medicalSubheadingHiddenText: String = "Change if your client got medical, dental, childcare, education benefits or loans from this company"
    val incomeTaxSubheadingHiddenText: String = "Change if your client got Income Tax or incurred costs paid as an employment benefit from this company"
    val reimbursedSubheadingHiddenText: String = "Change if your client got reimbursed costs, vouchers or non-cash benefits as an employment benefit from this company"
    val assetsSubheadingHiddenText: String = "Change if your client got assets as an employment benefit from this company"
    val benefitsReceivedHiddenText: String = "Change if your client got employment benefits from this company"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    def expectedP2(year: Int = taxYear): String = s"Does dim modd i chi ddiweddaru’ch buddiannau cyflogaeth tan 6 Ebrill $year."

    val expectedH1: String = "Gwiriwch eich buddiannau cyflogaeth"
    val expectedTitle: String = "Gwiriwch eich buddiannau cyflogaeth"
    val expectedP1: String = "Bydd eich buddiannau cyflogaeth yn seiliedig ar yr wybodaeth sydd eisoes gennym amdanoch."
    val companyCarHiddenText: String = "Newidiwch os cawsoch gar cwmni fel buddiant cyflogaeth gan y cwmni hwn"
    val fuelForCompanyCarHiddenText: String = "Newidiwch os cawsoch danwydd ar gyfer car cwmni fel buddiant cyflogaeth gan y cwmni hwn"
    val companyVanHiddenText: String = "Newidiwch os cawsoch fan cwmni fel buddiant cyflogaeth gan y cwmni hwn"
    val fuelForCompanyVanHiddenText: String = "Newidiwch os cawsoch danwydd fan cwmni fel buddiant cyflogaeth gan y cwmni hwn"
    val mileageBenefitHiddenText: String = "Newidiwch os cawsoch gostau milltiroedd fel buddiant cyflogaeth am ddefnyddio eich car eich hun"
    val accommodationHiddenText: String = "Newidiwch os cawsoch lety byw fel buddiant cyflogaeth gan y cwmni hwn"
    val qualifyingRelocationCostsHiddenText: String = "Newidiwch os cawsoch gostau adleoli cymwys fel buddiant cyflogaeth gan y cwmni hwn"
    val nonQualifyingRelocationCostsHiddenText: String = "Newidiwch os cawsoch gostau adleoli anghymwys fel buddiant cyflogaeth gan y cwmni hwn"
    val travelAndSubsistenceHiddenText: String = "Newidiwch os cawsoch gostau teithio ac aros dros nos fel buddiant cyflogaeth gan y cwmni hwn"
    val personalCostsHiddenText: String = "Newidiwch os cawsoch fân gostau dros nos fel buddiant cyflogaeth gan y cwmni hwn"
    val entertainmentHiddenText: String = "Newidiwch os cawsoch chi westeia fel buddiant cyflogaeth gan y cwmni hwn"
    val telephoneHiddenText: String = "Newidiwch os cawsoch ffôn fel buddiant cyflogaeth gan y cwmni hwn"
    val servicesProvidedHiddenText: String = "Newidiwch os darparwyd gwasanaethau gan eich cyflogwr i chi fel buddiant cyflogaeth gan y cwmni hwn"
    val profSubscriptionsHiddenText: String = "Newidiwch os cawsoch ffioedd neu danysgrifiadau proffesiynol fel buddiant cyflogaeth gan y cwmni hwn"
    val otherServicesHiddenText: String = "Newidiwch os cawsoch wasanaethau eraill fel buddiant cyflogaeth gan y cwmni hwn"
    val medicalInsHiddenText: String = "Newidiwch os cawsoch yswiriant meddygol neu ddeintyddol fel buddiant cyflogaeth gan y cwmni hwn"
    val nurseryHiddenText: String = "Newidiwch os cawsoch ofal plant fel buddiant cyflogaeth gan y cwmni hwn"
    val beneficialLoansHiddenText: String = "Newidiwch os cawsoch fenthyciadau buddiannol fel buddiant cyflogaeth gan y cwmni hwn"
    val educationalHiddenText: String = "Newidiwch os cawsoch wasanaethau addysg fel buddiant cyflogaeth gan y cwmni hwn"
    val incomeTaxPaidHiddenText: String = "Newidiwch os cawsoch eich Treth Incwm ei thalu fel buddiant cyflogaeth gan y cwmni hwn"
    val incurredCostsPaidHiddenText: String = "Newidiwch os cawsoch gostau a ysgwyddwyd eu talu fel buddiant cyflogaeth gan y cwmni hwn"
    val nonTaxableHiddenText: String = "Newidiwch os ad-dalwyd eich costau anhrethadwy fel buddiant cyflogaeth gan y cwmni hwn"
    val taxableCostsHiddenText: String = "Newidiwch os ad-dalwyd costau trethadwy i chi fel buddiant cyflogaeth gan y cwmni hwn"
    val vouchersHiddenText: String = "Newidiwch os cawsoch dalebau neu gardiau credyd fel buddiant cyflogaeth gan y cwmni hwn"
    val nonCashHiddenText: String = "Newidiwch os cawsoch fuddiant cyflogaeth sydd ddim ynn arian parod gan y cwmni hwn"
    val otherBenefitsHiddenText: String = "Newidiwch os cawsoch fuddiannau cyflogaeth eraill gan y cwmni hwn"
    val assetsHiddenText: String = "Newidiwch os cawsoch asedion i’w defnyddio fel buddiant cyflogaeth gan y cwmni hwn"
    val assetTransfersHiddenText: String = "Newidiwch os cawsoch asedion i’w cadw fel buddiant cyflogaeth gan y cwmni hwn"
    val companyCarAmountHiddenText: String = "Newidiwch y swm ar gyfer car cwmni fel buddiant cyflogaeth a gawsoch"
    val fuelForCompanyCarAmountHiddenText: String = "Newidiwch y swm ar gyfer tanwydd ar gyfer car cwmni a gawsoch chi fel buddiant cyflogaeth gan y cwmni hwn"
    val companyVanAmountHiddenText: String = "Newidiwch y swm ar gyfer fan cwmni a gawsoch fel buddiant cyflogaeth"
    val fuelForCompanyVanAmountHiddenText: String = "Newidiwch y swm ar gyfer tanwydd fan cwmni a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val mileageBenefitAmountHiddenText: String = "Newidiwch y swm a gawsoch mewn costau milltiroedd fel buddiant cyflogaeth am ddefnyddio eich car eich hun"
    val accommodationAmountHiddenText: String = "Newidiwch y swm ar gyfer llety byw a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val qualifyingRelocationCostsAmountHiddenText: String = "Newidiwch y swm ar gyfer costau adleoli cymwys a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val nonQualifyingRelocationCostsAmountHiddenText: String = "Newidiwch y swm ar gyfer costau adleoli anghymwys a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val travelAndSubsistenceAmountHiddenText: String = "Newidiwch y swm ar gyfer costau teithio neu aros dros nos a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val personalCostsAmountHiddenText: String = "Newidiwch y swm ar gyfer mân gostau dros nos a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val entertainmentAmountHiddenText: String = "Newidiwch y swm ar gyfer gwesteia a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val telephoneAmountHiddenText: String = "Newidiwch y swm ar gyfer y Ffôn a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val servicesProvidedAmountHiddenText: String = "Newidiwch y swm ar gyfer gwasanaethau a ddarparwyd gan eich cyflogwr fel buddiant cyflogaeth gan y cwmni hwn"
    val profSubscriptionsAmountHiddenText: String = "Newidiwch y swm ar gyfer ffioedd neu danysgrifiadau proffesiynol a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val otherServicesAmountHiddenText: String = "Newidiwch y swm ar gyfer gwasanaethau eraill fel buddiant cyflogaeth a gawsoch gan y cwmni hwn"
    val medicalInsAmountHiddenText: String = "Newidiwch y swm ar gyfer yswiriant meddygol neu ddeintyddol a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val nurseryAmountHiddenText: String = "Newidiwch y swm ar gyfer buddiant gofal plant a gawsoch gan y cwmni hwn"
    val beneficialLoansAmountHiddenText: String = "Newidiwch y swm ar gyfer benthyciadau buddiannol a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val educationalAmountHiddenText: String = "Newidiwch y swm ar gyfer gwasanaethau addysg a gawsoch fel buddiant cyflogaeth gan y cwmni hwn"
    val incomeTaxPaidAmountHiddenText: String = "Newidiwch y swm ar gyfer eich Treth Incwm a dalwyd fel buddiant cyflogaeth gan y cwmni hwn"
    val incurredCostsPaidAmountHiddenText: String = "Newidiwch y swm y cawsoch ei dalu ar gyfer costau a ysgwyddwyd fel buddiant cyflogaeth gan y cwmni hwn"
    val nonTaxableAmountHiddenText: String = "Newidiwch swm y costau anhrethadwy a ad-dalwyd i chi fel buddiant cyflogaeth gan y cwmni hwn"
    val taxableCostsAmountHiddenText: String = "Newidiwch swm y costau trethadwy a ad-dalwyd i chi fel buddiant cyflogaeth gan y cwmni hwn"
    val vouchersAmountHiddenText: String = "Newidiwch y swm a gawsoch ar gyfer talebau neu gardiau credyd fel buddiant cyflogaeth gan y cwmni hwn"
    val nonCashAmountHiddenText: String = "Newidiwch y swm a gawsoch fel buddiant cyflogaeth sydd ddim yn arian parod gan y cwmni hwn"
    val otherBenefitsAmountHiddenText: String = "Newidiwch y swm a gawsoch ar gyfer buddiannau cyflogaeth eraill gan y cwmni hwn"
    val assetsAmountHiddenText: String = "Newidiwch y swm a gawsoch ar gyfer asedion i’w defnyddio fel buddiant cyflogaeth gan y cwmni hwn"
    val assetTransfersAmountHiddenText: String = "Newidiwch y swm a gawsoch ar gyfer asedion i’w cadw fel buddiant cyflogaeth gan y cwmni hwn"
    val carSubheadingHiddenText: String = "Newidiwch os cawsoch gar, fan neu danwydd fel buddiant cyflogaeth gan y cwmni hwn"
    val accommodationSubheadingHiddenText: String = "Newidiwch os cawsoch lety neu eich adleoli fel buddiant cyflogaeth gan y cwmni hwn"
    val travelSubheadingHiddenText: String = "Newidiwch os cawsoch gostau teithio neu westeia fel buddiant cyflogaeth gan y cwmni hwn"
    val utilitiesSubheadingHiddenText: String = "Newidiwch os cawsoch gyfleustodau neu wasanaethau cyffredinol fel buddiannau cyflogaeth gan y cwmni hwn"
    val medicalSubheadingHiddenText: String = "Newidiwch os cawsoch fuddiannau neu fenthyciadau meddygol, deintyddol, gofal plant neu addysg gan y cwmni hwn"
    val incomeTaxSubheadingHiddenText: String = "Newidiwch os cawsoch eich Treth Incwm ei thalu neu gostau a ysgwyddwyd eu talu fel buddiant cyflogaeth gan y cwmni hwn"
    val reimbursedSubheadingHiddenText: String = "Newidiwch os cawsoch gostau wedi’u had-dalu, talebau neu fuddiannau sydd ddim yn arian parod fel buddiant cyflogaeth gan y cwmni hwn"
    val assetsSubheadingHiddenText: String = "Newidiwch os cawsoch asedion fel buddiant cyflogaeth gan y cwmni hwn"
    val benefitsReceivedHiddenText: String = "Newidiwch os cawsoch fuddiannau cyflogaeth gan y cwmni hwn"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    def expectedP2(year: Int = taxYear): String = s"Does dim modd i chi ddiweddaru buddiannau cyflogaeth eich cleient tan 6 Ebrill $year."

    val expectedH1: String = "Gwiriwch fuddiannau cyflogaeth eich cleient"
    val expectedTitle: String = "Gwiriwch fuddiannau cyflogaeth eich cleient"
    val expectedP1: String = "Mae buddiannau cyflogaeth eich cleient yn seiliedig ar yr wybodaeth sydd eisoes gennym amdano."
    val companyCarHiddenText: String = "Newidiwch os cafodd eich cleient gar cwmni fel buddiant cyflogaeth gan y cwmni hwn"
    val fuelForCompanyCarHiddenText: String = "Newidiwch os cafodd eich cleient danwydd ar gyfer car cwmni fel buddiant cyflogaeth gan y cwmni hwn"
    val companyVanHiddenText: String = "Newidiwch os cafodd eich cleient fan cwmni fel buddiant cyflogaeth gan y cwmni hwn"
    val fuelForCompanyVanHiddenText: String = "Newidiwch os cafodd eich cleient danwydd fan cwmni fel buddiant cyflogaeth gan y cwmni hwn"
    val mileageBenefitHiddenText: String = "Newidiwch os cafodd eich cleient gostau milltiroedd fel buddiant cyflogaeth am ddefnyddio ei gar ei hun"
    val accommodationHiddenText: String = "Newidiwch os cafodd eich cleient lety byw fel buddiant cyflogaeth gan y cwmni hwn"
    val qualifyingRelocationCostsHiddenText: String = "Newidiwch os cafodd eich cleient gostau adleoli cymwys fel buddiant cyflogaeth gan y cwmni hwn"
    val nonQualifyingRelocationCostsHiddenText: String = "Newidiwch os cafodd eich cleient gostau adleoli anghymwys fel buddiant cyflogaeth gan y cwmni hwn"
    val travelAndSubsistenceHiddenText: String = "Newidiwch os cafodd eich cleient gostau teithio ac aros dros nos fel buddiant cyflogaeth gan y cwmni hwn"
    val personalCostsHiddenText: String = "Newidiwch os cafodd eich cleient mân gostau dros nos fel buddiant cyflogaeth gan y cwmni hwn"
    val entertainmentHiddenText: String = "Newidiwch os cafodd eich cleient westeia fel buddiant cyflogaeth gan y cwmni hwn"
    val telephoneHiddenText: String = "Newidiwch os cafodd eich cleient ffôn fel buddiant cyflogaeth gan y cwmni hwn"
    val servicesProvidedHiddenText: String = "Newidiwch os darparwyd gwasanaethau i’ch cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val profSubscriptionsHiddenText: String = "Newidiwch os cafodd eich cleient ffioedd neu danysgrifiadau proffesiynol fel buddiant cyflogaeth gan y cwmni hwn"
    val otherServicesHiddenText: String = "Newidiwch os cafodd eich cleient wasanaethau eraill fel buddiant cyflogaeth gan y cwmni hwn"
    val medicalInsHiddenText: String = "Newidiwch os cafodd eich cleient yswiriant meddygol neu ddeintyddol fel buddiant cyflogaeth gan y cwmni hwn"
    val nurseryHiddenText: String = "Newidiwch os cafodd eich cleient ofal plant fel buddiant cyflogaeth gan y cwmni hwn"
    val beneficialLoansHiddenText: String = "Newidiwch os cafodd eich cleient fenthyciadau buddiannol fel buddiant cyflogaeth gan y cwmni hwn"
    val educationalHiddenText: String = "Newidiwch os cafodd eich cleient wasanaethau addysg fel buddiant cyflogaeth gan y cwmni hwn"
    val incomeTaxPaidHiddenText: String = "Newidiwch os cafodd Treth Incwm eich cleient ei thalu fel buddiant cyflogaeth gan y cwmni hwn"
    val incurredCostsPaidHiddenText: String = "Newidiwch os cafodd eich cleient gostau a ysgwyddwyd eu talu fel buddiant cyflogaeth gan y cwmni hwn"
    val nonTaxableHiddenText: String = "Newidiwch os ad-dalwyd costau anhrethadwy eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val taxableCostsHiddenText: String = "Newidiwch os ad-dalwyd costau trethadwy i’ch cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val vouchersHiddenText: String = "Newidiwch os cafodd eich cleient dalebau neu gardiau credyd fel buddiant cyflogaeth gan y cwmni hwn"
    val nonCashHiddenText: String = "Newidiwch os cafodd eich cleient fuddiant cyflogaeth sydd ddim yn arian parod gan y cwmni hwn"
    val otherBenefitsHiddenText: String = "Newidiwch os cafodd eich cleient fuddiannau cyflogaeth eraill gan y cwmni hwn"
    val assetsHiddenText: String = "Newidiwch os cafodd eich cleient asedion i’w defnyddio fel buddiant cyflogaeth gan y cwmni hwn"
    val assetTransfersHiddenText: String = "Newidiwch os cafodd eich cleient asedion i’w cadw fel buddiant cyflogaeth gan y cwmni hwn"
    val companyCarAmountHiddenText: String = "Newidiwch y swm ar gyfer car cwmni fel buddiant cyflogaeth a gafodd eich cleient"
    val fuelForCompanyCarAmountHiddenText: String = "Newidiwch y swm ar gyfer tanwydd ar gyfer car cwmni a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val companyVanAmountHiddenText: String = "Newidiwch y swm ar gyfer fan cwmni a gafodd eich cleient fel buddiant cyflogaeth"
    val fuelForCompanyVanAmountHiddenText: String = "Newidiwch y swm ar gyfer tanwydd fan cwmni a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val mileageBenefitAmountHiddenText: String = "Newidiwch y swm a gafodd eich cleient mewn costau milltiroedd fel buddiant cyflogaeth am ddefnyddio ei gar ei hun"
    val accommodationAmountHiddenText: String = "Newidiwch y swm ar gyfer llety byw a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val qualifyingRelocationCostsAmountHiddenText: String = "Newidiwch y swm ar gyfer costau adleoli cymwys a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val nonQualifyingRelocationCostsAmountHiddenText: String = "Newidiwch y swm ar gyfer costau adleoli anghymwys a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val travelAndSubsistenceAmountHiddenText: String = "Newidiwch y swm ar gyfer costau teithio neu aros dros nos a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val personalCostsAmountHiddenText: String = "Newidiwch y swm ar gyfer mân gostau dros nos a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val entertainmentAmountHiddenText: String = "Newidiwch y swm ar gyfer gwesteia a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val telephoneAmountHiddenText: String = "Newidiwch y swm ar gyfer y Ffôn a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val servicesProvidedAmountHiddenText: String = "Newidiwch y swm ar gyfer gwasanaethau a ddarparwyd gan gyflogwr eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val profSubscriptionsAmountHiddenText: String = "Newidiwch y swm ar gyfer ffioedd neu danysgrifiadau proffesiynol a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val otherServicesAmountHiddenText: String = "Newidiwch y swm ar gyfer gwasanaethau eraill a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val medicalInsAmountHiddenText: String = "Newidiwch y swm ar gyfer yswiriant meddygol neu ddeintyddol a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val nurseryAmountHiddenText: String = "Newidiwch y swm ar gyfer buddiant gofal plant a gafodd eich cleient gan y cwmni hwn"
    val beneficialLoansAmountHiddenText: String = "Newidiwch y swm ar gyfer benthyciadau buddiannol a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val educationalAmountHiddenText: String = "Newidiwch y swm ar gyfer gwasanaethau addysg a gafodd eich cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val incomeTaxPaidAmountHiddenText: String = "Newidiwch y swm ar gyfer Treth Incwm eich cleient a dalwyd fel buddiant cyflogaeth gan y cwmni hwn"
    val incurredCostsPaidAmountHiddenText: String = "Newidiwch swm y cafodd eich cleient ei dalu ar gyfer costau a ysgwyddwyd fel buddiant cyflogaeth gan y cwmni hwn"
    val nonTaxableAmountHiddenText: String = "Newidiwch swm y costau anhrethadwy a ad-dalwyd i’ch cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val taxableCostsAmountHiddenText: String = "Newidiwch swm y costau trethadwy a ad-dalwyd i’ch cleient fel buddiant cyflogaeth gan y cwmni hwn"
    val vouchersAmountHiddenText: String = "Newidiwch y swm a gafodd eich cleient ar gyfer talebau neu gardiau credyd fel buddiant cyflogaeth gan y cwmni hwn"
    val nonCashAmountHiddenText: String = "Newidiwch y swm a gafodd eich cleient fel buddiant cyflogaeth sydd ddim yn arian parod gan y cwmni hwn"
    val otherBenefitsAmountHiddenText: String = "Newidiwch y swm a gafodd eich cleient ar gyfer buddiannau cyflogaeth eraill gan y cwmni hwn"
    val assetsAmountHiddenText: String = "Newidiwch y swm a gafodd eich cleient ar gyfer asedion i’w defnyddio fel buddiant cyflogaeth gan y cwmni hwn"
    val assetTransfersAmountHiddenText: String = "Newidiwch y swm a gafodd eich cleient ar gyfer asedion i’w cadw fel buddiant cyflogaeth gan y cwmni hwn"
    val carSubheadingHiddenText: String = "Newidiwch os cafodd eich cleient gar, fan neu danwydd fel buddiant cyflogaeth gan y cwmni hwn"
    val accommodationSubheadingHiddenText: String = "Newidiwch os cafodd eich cleient lety neu ei adleoli fel buddiant cyflogaeth gan y cwmni hwn"
    val travelSubheadingHiddenText: String = "Newidiwch os cafodd eich cleient costau teithio neu westeia fel buddiant cyflogaeth gan y cwmni hwn"
    val utilitiesSubheadingHiddenText: String = "Newidiwch os cafodd eich cleient gyfleustodau neu wasanaethau cyffredinol fel buddiannau cyflogaeth gan y cwmni hwn"
    val medicalSubheadingHiddenText: String = "Newidiwch os cafodd eich cleient fuddiannau neu fenthyciadau meddygol, deintyddol, gofal plant neu addysg gan y cwmni hwn"
    val incomeTaxSubheadingHiddenText: String = "Newidiwch os cafodd Treth Incwm eich cleient ei thalu neu gostau a ysgwyddwyd eu talu fel buddiant cyflogaeth gan y cwmni hwn"
    val reimbursedSubheadingHiddenText: String = "Newidiwch os cafodd eich cleient gostau wedi’u had-dalu, talebau neu fuddiannau sydd ddim yn arian parod fel buddiant cyflogaeth gan y cwmni hwn"
    val assetsSubheadingHiddenText: String = "Newidiwch os cafodd eich cleient asedion fel buddiant cyflogaeth gan y cwmni hwn"
    val benefitsReceivedHiddenText: String = "Newidiwch os cafodd eich cleient fuddiannau cyflogaeth gan y cwmni hwn"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val underTest = inject[CheckYourBenefitsView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    val common = userScenario.commonExpectedResults
    val specific = userScenario.specificExpectedResults.get
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "return a fully populated page when all the fields are populated for in year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYear, employmentId, common.employerName, aBenefitsViewModel, isUsingCustomerData = true, isInYear = true, showNotification = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption())
        textOnPageCheck(specific.expectedP1, Selectors.p1)
        textOnPageCheck(specific.expectedP2(), Selectors.p2)
        textOnPageCheck(common.employerName, fieldHeaderSelector(4))
        textOnPageCheck(common.benefitsReceived, fieldNameSelector(5, 1))
        textOnPageCheck(common.yes, fieldAmountSelector(5, 1), "for benefits question")
        textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(6))
        textOnPageCheck(common.carSubheading, fieldNameSelector(7, 1))
        textOnPageCheck(common.yes, fieldAmountSelector(7, 1), "for vehicles section question")
        textOnPageCheck(common.companyCar, fieldNameSelector(7, 2))
        textOnPageCheck(common.yes, fieldAmountSelector(7, 2), "for carQuestion")
        textOnPageCheck(common.companyCarAmount, fieldNameSelector(7, 3))
        textOnPageCheck("£100", fieldAmountSelector(7, 3), "car")
        textOnPageCheck(common.fuelForCompanyCar, fieldNameSelector(7, 4))
        textOnPageCheck(common.yes, fieldAmountSelector(7, 4), "for carFuelQuestion")
        textOnPageCheck(common.fuelForCompanyCarAmount, fieldNameSelector(7, 5))
        textOnPageCheck("£200", fieldAmountSelector(7, 5), "carFuel")
        textOnPageCheck(common.companyVan, fieldNameSelector(7, 6))
        textOnPageCheck(common.yes, fieldAmountSelector(7, 6), "for vanQuestion")
        textOnPageCheck(common.companyVanAmount, fieldNameSelector(7, 7))
        textOnPageCheck("£300", fieldAmountSelector(7, 7), "van")
        textOnPageCheck(common.fuelForCompanyVan, fieldNameSelector(7, 8))
        textOnPageCheck(common.yes, fieldAmountSelector(7, 8), "for vanFuelQuestion")
        textOnPageCheck(common.fuelForCompanyVanAmount, fieldNameSelector(7, 9))
        textOnPageCheck("£400", fieldAmountSelector(7, 9), "vanFuel")
        textOnPageCheck(common.mileageBenefit, fieldNameSelector(7, 10))
        textOnPageCheck(common.yes, fieldAmountSelector(7, 10), "for mileageQuestion")
        textOnPageCheck(common.mileageBenefitAmount, fieldNameSelector(7, 11))
        textOnPageCheck("£500", fieldAmountSelector(7, 11), "mileage")
        textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(8))
        textOnPageCheck(common.accommodationSubheading, fieldNameSelector(9, 1))
        textOnPageCheck(common.yes, fieldAmountSelector(9, 1), "for accommodation section question")
        textOnPageCheck(common.accommodation, fieldNameSelector(9, 2))
        textOnPageCheck(common.yes, fieldAmountSelector(9, 2), "for accommodationQuestion")
        textOnPageCheck(common.accommodationAmount, fieldNameSelector(9, 3))
        textOnPageCheck("£100", fieldAmountSelector(9, 3), "for accommodation")
        textOnPageCheck(common.qualifyingRelocationCosts, fieldNameSelector(9, 4))
        textOnPageCheck(common.yes, fieldAmountSelector(9, 4), "for qualifyingRelocationExpensesQuestion")
        textOnPageCheck(common.qualifyingRelocationCostsAmount, fieldNameSelector(9, 5))
        textOnPageCheck("£200", fieldAmountSelector(9, 5), "qualifyingRelocationExpenses")
        textOnPageCheck(common.nonQualifyingRelocationCosts, fieldNameSelector(9, 6))
        textOnPageCheck(common.yes, fieldAmountSelector(9, 6), "for nonQualifyingRelocationExpensesQuestion")
        textOnPageCheck(common.nonQualifyingRelocationCostsAmount, fieldNameSelector(9, 7))
        textOnPageCheck("£300", fieldAmountSelector(9, 7), "nonQualifyingRelocationExpenses")
        textOnPageCheck(common.travelHeader, fieldHeaderSelector(10))
        textOnPageCheck(common.travelSubheading, fieldNameSelector(11, 1))
        textOnPageCheck(common.yes, fieldAmountSelector(11, 1), "for travel section question")
        textOnPageCheck(common.travelAndSubsistence, fieldNameSelector(11, 2))
        textOnPageCheck(common.yes, fieldAmountSelector(11, 2), "for travelAndSubsistenceQuestion")
        textOnPageCheck(common.travelAndSubsistenceAmount, fieldNameSelector(11, 3))
        textOnPageCheck("£100", fieldAmountSelector(11, 3), "travelAndSubsistence")
        textOnPageCheck(common.personalCosts, fieldNameSelector(11, 4))
        textOnPageCheck(common.yes, fieldAmountSelector(11, 4), "for personalIncidentalExpensesQuestion")
        textOnPageCheck(common.personalCostsAmount, fieldNameSelector(11, 5))
        textOnPageCheck("£200", fieldAmountSelector(11, 5), "personalIncidentalExpenses")
        textOnPageCheck(common.entertainment, fieldNameSelector(11, 6))
        textOnPageCheck(common.yes, fieldAmountSelector(11, 6), "for entertainingQuestion")
        textOnPageCheck(common.entertainmentAmount, fieldNameSelector(11, 7))
        textOnPageCheck("£300", fieldAmountSelector(11, 7), "entertaining")
        textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(12))
        textOnPageCheck(common.utilitiesSubheading, fieldNameSelector(13, 1))
        textOnPageCheck(common.yes, fieldAmountSelector(13, 1), "for utilities section question")
        textOnPageCheck(common.telephone, fieldNameSelector(13, 2))
        textOnPageCheck(common.yes, fieldAmountSelector(13, 2), "for telephoneQuestion")
        textOnPageCheck(common.telephoneAmount, fieldNameSelector(13, 3))
        textOnPageCheck("£100", fieldAmountSelector(13, 3), "telephone")
        textOnPageCheck(common.servicesProvided, fieldNameSelector(13, 4))
        textOnPageCheck(common.yes, fieldAmountSelector(13, 4), "for employerProvidedServicesQuestion")
        textOnPageCheck(common.servicesProvidedAmount, fieldNameSelector(13, 5))
        textOnPageCheck("£200", fieldAmountSelector(13, 5), "employerProvidedServices")
        textOnPageCheck(common.profSubscriptions, fieldNameSelector(13, 6))
        textOnPageCheck(common.yes, fieldAmountSelector(13, 6), "for employerProvidedProfessionalSubscriptionsQuestion")
        textOnPageCheck(common.profSubscriptionsAmount, fieldNameSelector(13, 7))
        textOnPageCheck("£300", fieldAmountSelector(13, 7), "employerProvidedProfessionalSubscriptions")
        textOnPageCheck(common.otherServices, fieldNameSelector(13, 8))
        textOnPageCheck(common.yes, fieldAmountSelector(13, 8), "for serviceQuestion")
        textOnPageCheck(common.otherServicesAmount, fieldNameSelector(13, 9))
        textOnPageCheck("£400", fieldAmountSelector(13, 9), "service")
        textOnPageCheck(common.medicalHeader, fieldHeaderSelector(14), "for medical section header")
        textOnPageCheck(common.medicalSubheading, fieldNameSelector(15, 1))
        textOnPageCheck(common.yes, fieldAmountSelector(15, 1), "for medical section question")
        textOnPageCheck(common.medicalIns, fieldNameSelector(15, 2))
        textOnPageCheck(common.yes, fieldAmountSelector(15, 2), "for medicalInsuranceQuestion")
        textOnPageCheck(common.medicalInsAmount, fieldNameSelector(15, 3))
        textOnPageCheck("£100", fieldAmountSelector(15, 3), "medicalInsurance")
        textOnPageCheck(common.nursery, fieldNameSelector(15, 4))
        textOnPageCheck(common.yes, fieldAmountSelector(15, 4), "for nurseryPlacesQuestion")
        textOnPageCheck(common.nurseryAmount, fieldNameSelector(15, 5))
        textOnPageCheck("£200", fieldAmountSelector(15, 5), "nurseryPlaces")
        textOnPageCheck(common.educational, fieldNameSelector(15, 6))
        textOnPageCheck(common.yes, fieldAmountSelector(15, 6), "for educationalServicesQuestion")
        textOnPageCheck(common.educationalAmount, fieldNameSelector(15, 7))
        textOnPageCheck("£300", fieldAmountSelector(15, 7), "educationalServices")
        textOnPageCheck(common.beneficialLoans, fieldNameSelector(15, 8))
        textOnPageCheck(common.yes, fieldAmountSelector(15, 8), "for beneficialLoanQuestion")
        textOnPageCheck(common.beneficialLoansAmount, fieldNameSelector(15, 9))
        textOnPageCheck("£400", fieldAmountSelector(15, 9), "beneficialLoan")
        textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(16))
        textOnPageCheck(common.incomeTaxSubheading, fieldNameSelector(17, 1))
        textOnPageCheck(common.yes, fieldAmountSelector(17, 1), "for income tax section question")
        textOnPageCheck(common.incomeTaxPaid, fieldNameSelector(17, 2))
        textOnPageCheck(common.yes, fieldAmountSelector(17, 2), "for incomeTaxPaidByDirectorQuestion")
        textOnPageCheck(common.incomeTaxPaidAmount, fieldNameSelector(17, 3))
        textOnPageCheck("£255", fieldAmountSelector(17, 3), "incomeTaxPaidByDirector")
        textOnPageCheck(common.incurredCostsPaid, fieldNameSelector(17, 4))
        textOnPageCheck(common.yes, fieldAmountSelector(17, 4), "for paymentsOnEmployeesBehalfQuestion")
        textOnPageCheck(common.incurredCostsPaidAmount, fieldNameSelector(17, 5))
        textOnPageCheck("£255", fieldAmountSelector(17, 5), "paymentsOnEmployeesBehalf")
        textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(18), "for reimbursed section header")
        textOnPageCheck(common.reimbursedSubheading, fieldNameSelector(19, 1))
        textOnPageCheck(common.yes, fieldAmountSelector(19, 1), "for reimbursements section question")
        textOnPageCheck(common.nonTaxable, fieldNameSelector(19, 2))
        textOnPageCheck(common.yes, fieldAmountSelector(19, 2), "for expensesQuestion")
        textOnPageCheck(common.nonTaxableAmount, fieldNameSelector(19, 3))
        textOnPageCheck("£100", fieldAmountSelector(19, 3), "expenses")
        textOnPageCheck(common.taxableCosts, fieldNameSelector(19, 4))
        textOnPageCheck(common.yes, fieldAmountSelector(19, 4), "for taxableExpensesQuestion")
        textOnPageCheck(common.taxableCostsAmount, fieldNameSelector(19, 5))
        textOnPageCheck("£200", fieldAmountSelector(19, 5), "taxableExpenses")
        textOnPageCheck(common.vouchers, fieldNameSelector(19, 6))
        textOnPageCheck(common.yes, fieldAmountSelector(19, 6), "for vouchersAndCreditCardsQuestion")
        textOnPageCheck(common.vouchersAmount, fieldNameSelector(19, 7))
        textOnPageCheck("£300", fieldAmountSelector(19, 7), "vouchersAndCreditCards")
        textOnPageCheck(common.nonCash, fieldNameSelector(19, 8))
        textOnPageCheck(common.yes, fieldAmountSelector(19, 8), "for nonCashQuestion")
        textOnPageCheck(common.nonCashAmount, fieldNameSelector(19, 9))
        textOnPageCheck("£400", fieldAmountSelector(19, 9), "nonCash")
        textOnPageCheck(common.otherBenefits, fieldNameSelector(19, 10))
        textOnPageCheck(common.yes, fieldAmountSelector(19, 10), "for otherItemsQuestion")
        textOnPageCheck(common.otherBenefitsAmount, fieldNameSelector(19, 11))
        textOnPageCheck("£500", fieldAmountSelector(19, 11), "otherItems")
        textOnPageCheck(common.assetsHeader, fieldHeaderSelector(20))
        textOnPageCheck(common.assetsSubheading, fieldNameSelector(21, 1), "subHeading")
        textOnPageCheck(common.yes, fieldAmountSelector(21, 1), "for assets section question")
        textOnPageCheck(common.assets, fieldNameSelector(21, 2))
        textOnPageCheck(common.yes, fieldAmountSelector(21, 2), "for assetsQuestion")
        textOnPageCheck(common.assetsAmount, fieldNameSelector(21, 3))
        textOnPageCheck("£100", fieldAmountSelector(21, 3), "assets")
        textOnPageCheck(common.assetTransfers, fieldNameSelector(21, 4))
        textOnPageCheck(common.yes, fieldAmountSelector(21, 4), "for assetTransferQuestion")
        textOnPageCheck(common.assetTransfersAmount, fieldNameSelector(21, 5))
        textOnPageCheck("£200", fieldAmountSelector(21, 5), "assetTransfer")
        elementsNotOnPageCheck(changeLinkCssSelector)
        buttonCheck(common.returnToEmployerText, Selectors.returnToEmployerSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return a fully populated page when all the fields are populated for end of year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, common.employerName, aBenefitsViewModel, isUsingCustomerData = false, isInYear = false, showNotification = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYear - 1))
        textOnPageCheck(specific.expectedP1, Selectors.p1)
        textOnPageCheck(common.employerName, fieldHeaderSelector(3))
        changeAmountRowCheck(common.benefitsReceived, common.yes, 4, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}",
          ReceiveAnyBenefitsController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(5))

        changeAmountRowCheck(common.carSubheading, common.yes, 6, 1, s"${common.changeText} ${specific.carSubheadingHiddenText}",
          CarVanFuelBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.companyCar, common.yes, 6, 2, s"${common.changeText} ${specific.companyCarHiddenText}",
          CompanyCarBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.companyCarAmount, "£100", 6, 3, s"${common.changeText} ${specific.companyCarAmountHiddenText}",
          CompanyCarBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.fuelForCompanyCar, common.yes, 6, 4, s"${common.changeText} ${specific.fuelForCompanyCarHiddenText}",
          CompanyCarFuelBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.fuelForCompanyCarAmount, "£200", 6, 5, s"${common.changeText} ${specific.fuelForCompanyCarAmountHiddenText}",
          CarFuelBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.companyVan, common.yes, 6, 6, s"${common.changeText} ${specific.companyVanHiddenText}",
          CompanyVanBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.fuelForCompanyVan, common.yes, 6, 8, s"${common.changeText} ${specific.fuelForCompanyVanHiddenText}",
          CompanyVanFuelBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.companyVanAmount, "£300", 6, 7, s"${common.changeText} ${specific.companyVanAmountHiddenText}",
          CompanyVanBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.fuelForCompanyVanAmount, "£400", 6, 9, s"${common.changeText} ${specific.fuelForCompanyVanAmountHiddenText}",
          CompanyVanFuelBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.mileageBenefit, common.yes, 6, 10, s"${common.changeText} ${specific.mileageBenefitHiddenText}",
          ReceiveOwnCarMileageBenefitController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.mileageBenefitAmount, "£500", 6, 11, s"${common.changeText} ${specific.mileageBenefitAmountHiddenText}",
          MileageBenefitAmountController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(7))
        changeAmountRowCheck(common.accommodationSubheading, common.yes, 8, 1, s"${common.changeText} ${specific.accommodationSubheadingHiddenText}",
          AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.accommodation, common.yes, 8, 2, s"${common.changeText} ${specific.accommodationHiddenText}",
          LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.accommodationAmount, "£100", 8, 3, s"${common.changeText} ${specific.accommodationAmountHiddenText}",
          LivingAccommodationBenefitAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.qualifyingRelocationCosts, common.yes, 8, 4, s"${common.changeText} ${specific.qualifyingRelocationCostsHiddenText}",
          QualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.qualifyingRelocationCostsAmount, "£200", 8, 5, s"${common.changeText} ${specific.qualifyingRelocationCostsAmountHiddenText}",
          QualifyingRelocationBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.nonQualifyingRelocationCosts, common.yes, 8, 6, s"${common.changeText} ${specific.nonQualifyingRelocationCostsHiddenText}",
          NonQualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.nonQualifyingRelocationCostsAmount, "£300", 8, 7, s"${common.changeText} ${specific.nonQualifyingRelocationCostsAmountHiddenText}",
          NonQualifyingRelocationBenefitsAmountController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.travelHeader, fieldHeaderSelector(9))
        changeAmountRowCheck(common.travelSubheading, common.yes, 10, 1, s"${common.changeText} ${specific.travelSubheadingHiddenText}",
          TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.travelAndSubsistence, common.yes, 10, 2, s"${common.changeText} ${specific.travelAndSubsistenceHiddenText}",
          TravelAndSubsistenceBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.travelAndSubsistenceAmount, "£100", 10, 3, s"${common.changeText} ${specific.travelAndSubsistenceAmountHiddenText}",
          TravelOrSubsistenceBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.personalCosts, common.yes, 10, 4, s"${common.changeText} ${specific.personalCostsHiddenText}",
          IncidentalOvernightCostEmploymentBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.personalCostsAmount, "£200", 10, 5, s"${common.changeText} ${specific.personalCostsAmountHiddenText}",
          IncidentalCostsBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.entertainment, common.yes, 10, 6, s"${common.changeText} ${specific.entertainmentHiddenText}",
          EntertainingBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.entertainmentAmount, "£300", 10, 7, s"${common.changeText} ${specific.entertainmentAmountHiddenText}",
          EntertainmentBenefitsAmountController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(11))
        changeAmountRowCheck(common.utilitiesSubheading, common.yes, 12, 1, s"${common.changeText} ${specific.utilitiesSubheadingHiddenText}",
          UtilitiesOrGeneralServicesBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.telephone, common.yes, 12, 2, s"${common.changeText} ${specific.telephoneHiddenText}",
          TelephoneBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.telephoneAmount, "£100", 12, 3, s"${common.changeText} ${specific.telephoneAmountHiddenText}",
          TelephoneBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.servicesProvided, common.yes, 12, 4, s"${common.changeText} ${specific.servicesProvidedHiddenText}",
          EmployerProvidedServicesBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.servicesProvidedAmount, "£200", 12, 5, s"${common.changeText} ${specific.servicesProvidedAmountHiddenText}",
          EmployerProvidedServicesBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.profSubscriptions, common.yes, 12, 6, s"${common.changeText} ${specific.profSubscriptionsHiddenText}",
          ProfessionalSubscriptionsBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.profSubscriptionsAmount, "£300", 12, 7, s"${common.changeText} ${specific.profSubscriptionsAmountHiddenText}",
          ProfessionalSubscriptionsBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.otherServices, common.yes, 12, 8, s"${common.changeText} ${specific.otherServicesHiddenText}",
          OtherServicesBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.otherServicesAmount, "£400", 12, 9, s"${common.changeText} ${specific.otherServicesAmountHiddenText}",
          OtherServicesBenefitsAmountController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.medicalHeader, fieldHeaderSelector(13), "for medical section header")
        changeAmountRowCheck(common.medicalSubheading, common.yes, 14, 1, s"${common.changeText} ${specific.medicalSubheadingHiddenText}",
          MedicalDentalChildcareBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.medicalIns, common.yes, 14, 2, s"${common.changeText} ${specific.medicalInsHiddenText}",
          MedicalDentalBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.medicalInsAmount, "£100", 14, 3, s"${common.changeText} ${specific.medicalInsAmountHiddenText}",
          MedicalOrDentalBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.nursery, common.yes, 14, 4, s"${common.changeText} ${specific.nurseryHiddenText}",
          ChildcareBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.nurseryAmount, "£200", 14, 5, s"${common.changeText} ${specific.nurseryAmountHiddenText}",
          ChildcareBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.educational, common.yes, 14, 6, s"${common.changeText} ${specific.educationalHiddenText}",
          EducationalServicesBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.educationalAmount, "£300", 14, 7, s"${common.changeText} ${specific.educationalAmountHiddenText}",
          EducationalServicesBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.beneficialLoans, common.yes, 14, 8, s"${common.changeText} ${specific.beneficialLoansHiddenText}",
          BeneficialLoansBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.beneficialLoansAmount, "£400", 14, 9, s"${common.changeText} ${specific.beneficialLoansAmountHiddenText}",
          BeneficialLoansAmountController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(15))
        changeAmountRowCheck(common.incomeTaxSubheading, common.yes, 16, 1, s"${common.changeText} ${specific.incomeTaxSubheadingHiddenText}",
          IncomeTaxOrIncurredCostsBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.incomeTaxPaid, common.yes, 16, 2, s"${common.changeText} ${specific.incomeTaxPaidHiddenText}",
          IncomeTaxBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.incomeTaxPaidAmount, "£255", 16, 3, s"${common.changeText} ${specific.incomeTaxPaidAmountHiddenText}",
          IncomeTaxBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.incurredCostsPaid, common.yes, 16, 4, s"${common.changeText} ${specific.incurredCostsPaidHiddenText}",
          IncurredCostsBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.incurredCostsPaidAmount, "£255", 16, 5, s"${common.changeText} ${specific.incurredCostsPaidAmountHiddenText}",
          IncurredCostsBenefitsAmountController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(17), "for reimbursed section header")
        changeAmountRowCheck(common.reimbursedSubheading, common.yes, 18, 1, s"${common.changeText} ${specific.reimbursedSubheadingHiddenText}",
          ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYearEOY, employmentId).url)

        changeAmountRowCheck(common.nonTaxable, common.yes, 18, 2, s"${common.changeText} ${specific.nonTaxableHiddenText}",
          NonTaxableCostsBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.nonTaxableAmount, "£100", 18, 3, s"${common.changeText} ${specific.nonTaxableAmountHiddenText}",
          NonTaxableCostsBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.taxableCosts, common.yes, 18, 4, s"${common.changeText} ${specific.taxableCostsHiddenText}",
          TaxableCostsBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.taxableCostsAmount, "£200", 18, 5, s"${common.changeText} ${specific.taxableCostsAmountHiddenText}",
          TaxableCostsBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.vouchers, common.yes, 18, 6, s"${common.changeText} ${specific.vouchersHiddenText}",
          VouchersBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.vouchersAmount, "£300", 18, 7, s"${common.changeText} ${specific.vouchersAmountHiddenText}",
          VouchersBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.nonCash, common.yes, 18, 8, s"${common.changeText} ${specific.nonCashHiddenText}",
          NonCashBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.nonCashAmount, "£400", 18, 9, s"${common.changeText} ${specific.nonCashAmountHiddenText}",
          NonCashBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.otherBenefits, common.yes, 18, 10, s"${common.changeText} ${specific.otherBenefitsHiddenText}",
          OtherBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.otherBenefitsAmount, "£500", 18, 11, s"${common.changeText} ${specific.otherBenefitsAmountHiddenText}",
          OtherBenefitsAmountController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.assetsHeader, fieldHeaderSelector(19), "for section")
        changeAmountRowCheck(common.assetsSubheading, common.yes, 20, 1, s"${common.changeText} ${specific.assetsSubheadingHiddenText}",
          AssetsOrAssetTransfersBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.assets, common.yes, 20, 2, s"${common.changeText} ${specific.assetsHiddenText}",
          AssetsBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.assetsAmount, "£100", 20, 3, s"${common.changeText} ${specific.assetsAmountHiddenText}",
          AssetsBenefitsAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.assetTransfers, common.yes, 20, 4, s"${common.changeText} ${specific.assetTransfersHiddenText}",
          AssetTransfersBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.assetTransfersAmount, "£200", 20, 5, s"${common.changeText} ${specific.assetTransfersAmountHiddenText}",
          AssetsTransfersBenefitsAmountController.show(taxYearEOY, employmentId).url)

        buttonCheck(common.saveAndContinue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return only the relevant data on the page when other certain data items are in CYA for EOY, customerData = true to check help text isn't shown" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val accommodationRelocationModel = anAccommodationRelocationModel.copy(
          qualifyingRelocationExpensesQuestion = Some(false),
          qualifyingRelocationExpenses = None,
          nonQualifyingRelocationExpensesQuestion = Some(false),
          nonQualifyingRelocationExpenses = None
        )
        val benefitsViewModel = aBenefitsViewModel.copy(
          carVanFuelModel = None,
          accommodationRelocationModel = Some(accommodationRelocationModel),
          travelEntertainmentModel = None,
          utilitiesAndServicesModel = None,
          medicalChildcareEducationModel = None,
          incomeTaxAndCostsModel = None,
          reimbursedCostsVouchersAndNonCashModel = None,
          assetsModel = None
        )
        val htmlFormat = underTest(taxYearEOY, employmentId, common.employerName, benefitsViewModel, isUsingCustomerData = true, isInYear = false, showNotification = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYear - 1))
        textOnPageCheck(common.employerName, fieldHeaderSelector(2))
        changeAmountRowCheck(common.benefitsReceived, common.yes, 3, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}",
          ReceiveAnyBenefitsController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(4))

        changeAmountRowCheck(common.carSubheading, common.no, 5, 1, s"${common.changeText} ${specific.carSubheadingHiddenText}",
          CarVanFuelBenefitsController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(6))
        changeAmountRowCheck(common.accommodationSubheading, common.yes, 7, 1, s"${common.changeText} ${specific.accommodationSubheadingHiddenText}",
          AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.accommodation, common.yes, 7, 2, s"${common.changeText} ${specific.accommodationHiddenText}",
          LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.accommodationAmount, "£100", 7, 3, s"${common.changeText} ${specific.accommodationAmountHiddenText}",
          LivingAccommodationBenefitAmountController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.qualifyingRelocationCosts, common.no, 7, 4, s"${common.changeText} ${specific.qualifyingRelocationCostsHiddenText}",
          QualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url)
        changeAmountRowCheck(common.nonQualifyingRelocationCosts, common.no, 7, 5, s"${common.changeText} ${specific.nonQualifyingRelocationCostsHiddenText}",
          NonQualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.travelHeader, fieldHeaderSelector(8))
        changeAmountRowCheck(common.travelSubheading, common.no, 9, 1, s"${common.changeText} ${specific.travelSubheadingHiddenText}",
          TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(10))
        changeAmountRowCheck(common.utilitiesSubheading, common.no, 11, 1, s"${common.changeText} ${specific.utilitiesSubheadingHiddenText}",
          UtilitiesOrGeneralServicesBenefitsController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.medicalHeader, fieldHeaderSelector(12), "for medical section header")
        changeAmountRowCheck(common.medicalSubheading, common.no, 13, 1, s"${common.changeText} ${specific.medicalSubheadingHiddenText}",
          MedicalDentalChildcareBenefitsController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(14))
        changeAmountRowCheck(common.incomeTaxSubheading, common.no, 15, 1, s"${common.changeText} ${specific.incomeTaxSubheadingHiddenText}",
          IncomeTaxOrIncurredCostsBenefitsController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(16), "for reimbursed section header")
        changeAmountRowCheck(common.reimbursedSubheading, common.no, 17, 1, s"${common.changeText} ${specific.reimbursedSubheadingHiddenText}",
          ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYearEOY, employmentId).url)

        textOnPageCheck(common.assetsHeader, fieldHeaderSelector(18), "for section")
        changeAmountRowCheck(common.assetsSubheading, common.no, 19, 1, s"${common.changeText} ${specific.assetsSubheadingHiddenText}",
          AssetsOrAssetTransfersBenefitsController.show(taxYearEOY, employmentId).url)

        buttonCheck(common.saveAndContinue)

        welshToggleCheck(userScenario.isWelsh)

        s"should not display the following values" in {
          document.body().toString.contains(specific.expectedP1) shouldBe false
          document.body().toString.contains(common.companyCar) shouldBe false
          document.body().toString.contains(common.fuelForCompanyCar) shouldBe false
          document.body().toString.contains(common.companyVan) shouldBe false
          document.body().toString.contains(common.fuelForCompanyVan) shouldBe false
          document.body().toString.contains(common.mileageBenefit) shouldBe false
          document.body().toString.contains(common.travelAndSubsistence) shouldBe false
          document.body().toString.contains(common.personalCosts) shouldBe false
          document.body().toString.contains(common.entertainment) shouldBe false
          document.body().toString.contains(common.telephone) shouldBe false
          document.body().toString.contains(common.servicesProvided) shouldBe false
          document.body().toString.contains(common.profSubscriptions) shouldBe false
          document.body().toString.contains(common.otherServices) shouldBe false
          document.body().toString.contains(common.nursery) shouldBe false
          document.body().toString.contains(common.beneficialLoans) shouldBe false
          document.body().toString.contains(common.educational) shouldBe false
          document.body().toString.contains(common.incomeTaxPaid) shouldBe false
          document.body().toString.contains(common.incurredCostsPaid) shouldBe false
          document.body().toString.contains(common.nonTaxable) shouldBe false
          document.body().toString.contains(common.taxableCosts) shouldBe false
          document.body().toString.contains(common.vouchers) shouldBe false
          document.body().toString.contains(common.nonCash) shouldBe false
          document.body().toString.contains(common.otherBenefits) shouldBe false
          document.body().toString.contains(common.assetTransfers) shouldBe false
          document.body().toString.contains(common.companyCarAmount) shouldBe false
          document.body().toString.contains(common.fuelForCompanyCarAmount) shouldBe false
          document.body().toString.contains(common.companyVanAmount) shouldBe false
          document.body().toString.contains(common.fuelForCompanyVanAmount) shouldBe false
          document.body().toString.contains(common.mileageBenefitAmount) shouldBe false
          document.body().toString.contains(common.travelAndSubsistenceAmount) shouldBe false
          document.body().toString.contains(common.personalCostsAmount) shouldBe false
          document.body().toString.contains(common.entertainmentAmount) shouldBe false
          document.body().toString.contains(common.telephoneAmount) shouldBe false
          document.body().toString.contains(common.servicesProvidedAmount) shouldBe false
          document.body().toString.contains(common.profSubscriptionsAmount) shouldBe false
          document.body().toString.contains(common.otherServicesAmount) shouldBe false
          document.body().toString.contains(common.medicalInsAmount) shouldBe false
          document.body().toString.contains(common.nurseryAmount) shouldBe false
          document.body().toString.contains(common.beneficialLoansAmount) shouldBe false
          document.body().toString.contains(common.educationalAmount) shouldBe false
          document.body().toString.contains(common.incomeTaxPaidAmount) shouldBe false
          document.body().toString.contains(common.incurredCostsPaidAmount) shouldBe false
          document.body().toString.contains(common.nonTaxableAmount) shouldBe false
          document.body().toString.contains(common.taxableCostsAmount) shouldBe false
          document.body().toString.contains(common.vouchersAmount) shouldBe false
          document.body().toString.contains(common.nonCashAmount) shouldBe false
          document.body().toString.contains(common.otherBenefitsAmount) shouldBe false
          document.body().toString.contains(common.assetsAmount) shouldBe false
          document.body().toString.contains(common.assetTransfersAmount) shouldBe false
        }
      }

      "return a page with only the benefits received subheading when its EOY and only the benefits question answered as no" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val benefitsViewModel = aBenefitsViewModel.copy(isBenefitsReceived = false)
        val htmlFormat = underTest(taxYearEOY, employmentId, common.employerName, benefitsViewModel, isUsingCustomerData = false, isInYear = false, showNotification = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specific.expectedTitle, userScenario.isWelsh)
        h1Check(specific.expectedH1)
        captionCheck(common.expectedCaption(taxYear - 1))
        textOnPageCheck(specific.expectedP1, Selectors.p1)

        changeAmountRowCheck(common.benefitsReceived, common.no, 4, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}",
          ReceiveAnyBenefitsController.show(taxYearEOY, employmentId).url)

        buttonCheck(common.saveAndContinue)

        welshToggleCheck(userScenario.isWelsh)

        s"should not display the following values" in {
          document.body().toString.contains(common.carSubheading) shouldBe false
          document.body().toString.contains(common.accommodationSubheading) shouldBe false
          document.body().toString.contains(common.travelSubheading) shouldBe false
          document.body().toString.contains(common.utilitiesSubheading) shouldBe false
          document.body().toString.contains(common.medicalSubheading) shouldBe false
          document.body().toString.contains(common.incomeTaxSubheading) shouldBe false
          document.body().toString.contains(common.reimbursedSubheading) shouldBe false
          document.body().toString.contains(common.assetsSubheading) shouldBe false
        }
      }
    }
  }
}
