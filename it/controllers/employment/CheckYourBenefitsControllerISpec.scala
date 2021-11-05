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

package controllers.employment

import models.User
import models.benefits.{AccommodationRelocationModel, BenefitsViewModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CheckYourBenefitsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper {

  val defaultTaxYear = 2022
  def url(taxYear: Int = defaultTaxYear): String = s"$appUrl/$taxYear/check-employment-benefits?employmentId=001"

  object Selectors {
    val p1 = "#main-content > div > div > p.govuk-body"
    val p2 = "#main-content > div > div > div.govuk-inset-text"
    def fieldNameSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dt"
    def fieldAmountSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd.govuk-summary-list__value"
    def fieldChangeLinkSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd > a"
    def fieldHeaderSelector(i: Int): String = s"#main-content > div > div > h2:nth-child($i)"
  }

  trait SpecificExpectedResults {
    def expectedP2(year: Int = defaultTaxYear): String

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
    def expectedCaption(year: Int = defaultTaxYear): String

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
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(year: Int = defaultTaxYear): String = s"Employment for 6 April ${year - 1} to 5 April $year"

    val changeText: String = "Change"
    val vehicleHeader = "Vehicles, fuel and mileage"
    val companyCar = "Company car"
    val fuelForCompanyCar = "Fuel for company car"
    val companyVan = "Company van"
    val fuelForCompanyVan = "Fuel for company van"
    val mileageBenefit = "Mileage benefit"
    val accommodationHeader = "Accommodation and relocation"
    val accommodation = "Accommodation"
    val qualifyingRelocationCosts = "Qualifying relocation costs"
    val nonQualifyingRelocationCosts = "Non-qualifying relocation costs"
    val travelHeader = "Travel and entertainment"
    val travelAndSubsistence = "Travel and subsistence"
    val personalCosts = "Personal incidental costs"
    val entertainment = "Entertainment"
    val utilitiesHeader = "Utilities and general services"
    val telephone = "Telephone"
    val servicesProvided = "Services provided by employer"
    val profSubscriptions = "Professional subscriptions"
    val otherServices = "Other services"
    val medicalHeader = "Medical, dental, childcare, education benefits or loans"
    val medicalIns = "Medical or dental insurance"
    val nursery = "Childcare"
    val beneficialLoans = "Beneficial loans"
    val educational = "Educational services"
    val incomeTaxHeader = "Income Tax and incurred costs"
    val incomeTaxPaid = "Income Tax paid by employer"
    val incurredCostsPaid = "Incurred costs paid by employer"
    val reimbursedHeader = "Reimbursed costs, vouchers, and non-cash benefits"
    val nonTaxable = "Non-taxable costs reimbursed by employer"
    val taxableCosts = "Taxable costs reimbursed by employer"
    val vouchers = "Vouchers, credit cards or mileage allowance"
    val nonCash = "Non-cash benefits"
    val otherBenefits = "Other benefits"
    val assetsHeader = "Assets and asset transfers"
    val assets = "Assets"
    val assetTransfers = "Benefits for asset transfers"
    val companyCarAmount = "Amount for company car"
    val fuelForCompanyCarAmount = "Amount of company car fuel"
    val companyVanAmount = "Amount for company van"
    val fuelForCompanyVanAmount = "Amount for company van fuel"
    val mileageBenefitAmount = "Amount for mileage benefit"
    val accommodationAmount = "Amount for accommodation"
    val qualifyingRelocationCostsAmount = "Amount for qualifying relocation costs"
    val nonQualifyingRelocationCostsAmount = "Amount for non-qualifying relocation costs"
    val travelAndSubsistenceAmount = "Amount for travel and subsistence"
    val personalCostsAmount = "Amount for personal incidental costs"
    val entertainmentAmount = "Amount of entertainment"
    val telephoneAmount = "Amount for telephone"
    val servicesProvidedAmount = "Amount for services provided by employer"
    val profSubscriptionsAmount = "Amount for professional subscriptions"
    val otherServicesAmount = "Amount for other services"
    val medicalInsAmount = "Amount for medical or dental insurance"
    val nurseryAmount = "Amount for childcare"
    val beneficialLoansAmount = "Amount for beneficial loans"
    val educationalAmount = "Amount for educational services"
    val incomeTaxPaidAmount = "Amount of Income Tax paid by employer"
    val incurredCostsPaidAmount = "Amount of incurred costs paid by employer"
    val nonTaxableAmount = "Amount for non-taxable costs reimbursed by employer"
    val taxableCostsAmount = "Amount for taxable costs reimbursed by employer"
    val vouchersAmount = "Amount for vouchers, credit cards or mileage allowance"
    val nonCashAmount = "Amount for non-cash benefits"
    val otherBenefitsAmount = "Amount for other benefits"
    val assetsAmount = "Amount for assets"
    val assetTransfersAmount = "Amount for asset transfers"
    val carSubheading: String = "Car, van or fuel"
    val accommodationSubheading: String = "Accommodation or relocation"
    val travelSubheading: String = "Travel or entertainment"
    val utilitiesSubheading: String = "Utilities or general services"
    val medicalSubheading: String = "Medical insurance, nursery, education benefits or loans"
    val incomeTaxSubheading: String = "Income Tax or incurred costs"
    val reimbursedSubheading: String = "Vouchers, non-cash benefits or reimbursed costs"
    val assetsSubheading: String = "Assets or asset transfers"
    val yes: String = "Yes"
    val no: String = "No"
    val benefitsReceived = "Benefits received"
    val saveAndContinue: String = "Save and continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(year: Int = defaultTaxYear): String = s"Employment for 6 April ${year - 1} to 5 April $year"

    val changeText: String = "Change"
    val vehicleHeader = "Vehicles, fuel and mileage"
    val companyCar = "Company car"
    val fuelForCompanyCar = "Fuel for company car"
    val companyVan = "Company van"
    val fuelForCompanyVan = "Fuel for company van"
    val mileageBenefit = "Mileage benefit"
    val accommodationHeader = "Accommodation and relocation"
    val accommodation = "Accommodation"
    val qualifyingRelocationCosts = "Qualifying relocation costs"
    val nonQualifyingRelocationCosts = "Non-qualifying relocation costs"
    val travelHeader = "Travel and entertainment"
    val travelAndSubsistence = "Travel and subsistence"
    val personalCosts = "Personal incidental costs"
    val entertainment = "Entertainment"
    val utilitiesHeader = "Utilities and general services"
    val telephone = "Telephone"
    val servicesProvided = "Services provided by employer"
    val profSubscriptions = "Professional subscriptions"
    val otherServices = "Other services"
    val medicalHeader = "Medical, dental, childcare, education benefits or loans"
    val medicalIns = "Medical or dental insurance"
    val nursery = "Childcare"
    val beneficialLoans = "Beneficial loans"
    val educational = "Educational services"
    val incomeTaxHeader = "Income Tax and incurred costs"
    val incomeTaxPaid = "Income Tax paid by employer"
    val incurredCostsPaid = "Incurred costs paid by employer"
    val reimbursedHeader = "Reimbursed costs, vouchers, and non-cash benefits"
    val nonTaxable = "Non-taxable costs reimbursed by employer"
    val taxableCosts = "Taxable costs reimbursed by employer"
    val vouchers = "Vouchers, credit cards or mileage allowance"
    val nonCash = "Non-cash benefits"
    val otherBenefits = "Other benefits"
    val assetsHeader = "Assets and asset transfers"
    val assets = "Assets"
    val assetTransfers = "Benefits for asset transfers"
    val companyCarAmount = "Amount for company car"
    val fuelForCompanyCarAmount = "Amount of company car fuel"
    val companyVanAmount = "Amount for company van"
    val fuelForCompanyVanAmount = "Amount for company van fuel"
    val mileageBenefitAmount = "Amount for mileage benefit"
    val accommodationAmount = "Amount for accommodation"
    val qualifyingRelocationCostsAmount = "Amount for qualifying relocation costs"
    val nonQualifyingRelocationCostsAmount = "Amount for non-qualifying relocation costs"
    val travelAndSubsistenceAmount = "Amount for travel and subsistence"
    val personalCostsAmount = "Amount for personal incidental costs"
    val entertainmentAmount = "Amount of entertainment"
    val telephoneAmount = "Amount for telephone"
    val servicesProvidedAmount = "Amount for services provided by employer"
    val profSubscriptionsAmount = "Amount for professional subscriptions"
    val otherServicesAmount = "Amount for other services"
    val medicalInsAmount = "Amount for medical or dental insurance"
    val nurseryAmount = "Amount for childcare"
    val beneficialLoansAmount = "Amount for beneficial loans"
    val educationalAmount = "Amount for educational services"
    val incomeTaxPaidAmount = "Amount of Income Tax paid by employer"
    val incurredCostsPaidAmount = "Amount of incurred costs paid by employer"
    val nonTaxableAmount = "Amount for non-taxable costs reimbursed by employer"
    val taxableCostsAmount = "Amount for taxable costs reimbursed by employer"
    val vouchersAmount = "Amount for vouchers, credit cards or mileage allowance"
    val nonCashAmount = "Amount for non-cash benefits"
    val otherBenefitsAmount = "Amount for other benefits"
    val assetsAmount = "Amount for assets"
    val assetTransfersAmount = "Amount for asset transfers"
    val carSubheading: String = "Car, van or fuel"
    val accommodationSubheading: String = "Accommodation or relocation"
    val travelSubheading: String = "Travel or entertainment"
    val utilitiesSubheading: String = "Utilities or general services"
    val medicalSubheading: String = "Medical insurance, nursery, education benefits or loans"
    val incomeTaxSubheading: String = "Income Tax or incurred costs"
    val reimbursedSubheading: String = "Vouchers, non-cash benefits or reimbursed costs"
    val assetsSubheading: String = "Assets or asset transfers"
    val yes: String = "Yes"
    val no: String = "No"
    val benefitsReceived = "Benefits received"
    val saveAndContinue: String = "Save and continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    def expectedP2(year: Int = defaultTaxYear): String = s"You cannot update your employment benefits until 6 April $year."

    val expectedH1: String = "Check your employment benefits"
    val expectedTitle: String = "Check your employment benefits"
    val expectedP1: String = "Your employment benefits are based on the information we already hold about you."
    val companyCarHiddenText: String = "Change if you got a company car as an employment benefit from this company"
    val fuelForCompanyCarHiddenText: String = "Change if you got a company car fuel as an employment benefit from this company"
    val companyVanHiddenText: String = "Change if you got a company van as an employment benefit from this company"
    val fuelForCompanyVanHiddenText: String = "Change if you got a company van fuel as an employment benefit from this company"
    val mileageBenefitHiddenText: String = "Change if you got mileage as an employment benefit for using your own car"
    val accommodationHiddenText: String = "Change if you got accommodation as an employment benefit from this company"
    val qualifyingRelocationCostsHiddenText: String = "Change if you got qualifying relocation costs as an employment benefit from this company"
    val nonQualifyingRelocationCostsHiddenText: String = "Change if you got non-qualifying relocation costs as an employment benefit from this company"
    val travelAndSubsistenceHiddenText: String = "Change if you got travel and overnight stays as an employment benefit from this company"
    val personalCostsHiddenText: String = "Change if you got personal incidental costs as an employment benefit from this company"
    val entertainmentHiddenText: String = "Change if you got entertainment as an employment benefit from this company"
    val telephoneHiddenText: String = "Change if you got a telephone as an employment benefit from this company"
    val servicesProvidedHiddenText: String = "Change if you got services provided by your employer as an employment benefit from this company"
    val profSubscriptionsHiddenText: String = "Change if you got professional subscriptions as an employment benefit from this company"
    val otherServicesHiddenText: String = "Change if you got other services as an employment benefit from this company"
    val medicalInsHiddenText: String = "Change if you got medical or dental insurance as an employment benefit from this company"
    val nurseryHiddenText: String = "Change if you got childcare as an employment benefit from this company"
    val beneficialLoansHiddenText: String = "Change if you got beneficial loans as an employment benefit from this company"
    val educationalHiddenText: String = "Change if you got educational services as an employment benefit from this company"
    val incomeTaxPaidHiddenText: String = "Change if you got Income Tax paid as an employment benefit from this company"
    val incurredCostsPaidHiddenText: String = "Change if you got incurred costs paid as an employment benefit from this company"
    val nonTaxableHiddenText: String = "Change if you got non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsHiddenText: String = "Change if you got taxable costs reimbursed as an employment benefit from this company"
    val vouchersHiddenText: String = "Change if you got vouchers, credit cards, or mileage allowance as an employment benefit from this company"
    val nonCashHiddenText: String = "Change if you got non-cash employment benefit from this company"
    val otherBenefitsHiddenText: String = "Change if you got other employment benefit from this company"
    val assetsHiddenText: String = "Change if you got assets as an employment benefit from this company"
    val assetTransfersHiddenText: String = "Change if you got asset transfers as an employment benefit from this company"
    val companyCarAmountHiddenText: String = "Change the amount for company car as an employment benefit you got"
    val fuelForCompanyCarAmountHiddenText: String = "Change the amount for company car fuel as an employment benefit you got from this company"
    val companyVanAmountHiddenText: String = "Change the amount for company van as an employment benefit you got"
    val fuelForCompanyVanAmountHiddenText: String = "Change the amount for company van fuel as an employment benefit you got from this company"
    val mileageBenefitAmountHiddenText: String = "Change the amount for mileage as an employment benefit you got for using your own car"
    val accommodationAmountHiddenText: String = "Change the amount for accommodation as an employment benefit you got from this company"
    val qualifyingRelocationCostsAmountHiddenText: String = "Change the amount for qualifying relocation costs as an employment benefit you got from this company"
    val nonQualifyingRelocationCostsAmountHiddenText: String = "Change the amount for non-qualifying relocation costs as an employment benefit you got from this company"
    val travelAndSubsistenceAmountHiddenText: String = "Change the amount for travel or overnight stays as an employment benefit you got from this company"
    val personalCostsAmountHiddenText: String = "Change the amount for personal incidental costs as an employment benefit you got from this company"
    val entertainmentAmountHiddenText: String = "Change the amount for entertainment as an employment benefit you got from this company"
    val telephoneAmountHiddenText: String = "Change the amount for telephone as an employment benefit you got from this company"
    val servicesProvidedAmountHiddenText: String = "Change the amount for services provided by your employer as an employment benefit you got from this company"
    val profSubscriptionsAmountHiddenText: String = "Change the amount for professional subscriptions as an employment benefit you got from this company"
    val otherServicesAmountHiddenText: String = "Change the amount for other services as an employment benefit you got from this company"
    val medicalInsAmountHiddenText: String = "Change the amount for medical or dental insurance as an employment benefit you got from this company"
    val nurseryAmountHiddenText: String = "Change the amount for childcare employment benefit you got from this company"
    val beneficialLoansAmountHiddenText: String = "Change the amount for beneficial loans as an employment benefit you got from this company"
    val educationalAmountHiddenText: String = "Change the amount for educational services as an employment benefit you got from this company"
    val incomeTaxPaidAmountHiddenText: String = "Change the amount for Income Tax paid as an employment benefit you got from this company"
    val incurredCostsPaidAmountHiddenText: String = "Change the amount for incurred costs paid as an employment benefit you got from this company"
    val nonTaxableAmountHiddenText: String = "Change the amount you got for non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsAmountHiddenText: String = "Change the amount you got for taxable costs reimbursed as an employment benefit from this company"
    val vouchersAmountHiddenText: String = "Change the amount you got for vouchers, credit cards, or mileage allowance as an employment benefit from this company"
    val nonCashAmountHiddenText: String = "Change the amount you got for non-cash employment benefit from this company"
    val otherBenefitsAmountHiddenText: String = "Change the amount you got for other employment benefit from this company"
    val assetsAmountHiddenText: String = "Change the amount you got for assets as an employment benefit from this company"
    val assetTransfersAmountHiddenText: String = "Change the amount you got for asset transfers as an employment benefit from this company"
    val carSubheadingHiddenText: String = "Change if you got a car, van or fuel as an employment benefit from this company"
    val accommodationSubheadingHiddenText: String = "Change if you got accommodation or relocation as an employment benefit from this company"
    val travelSubheadingHiddenText: String = "Change if you got travel or entertainment as an employment benefit from this company"
    val utilitiesSubheadingHiddenText: String = "Change if you got utilities or general services as an employment benefit from this company"
    val medicalSubheadingHiddenText: String = "Change if you got medical insurance, nursery, education or loans as an employment benefit from this company"
    val incomeTaxSubheadingHiddenText: String = "Change if you got Income Tax or incurred costs paid as an employment benefit from this company"
    val reimbursedSubheadingHiddenText: String = "Change if you got vouchers, non-cash benefits or reimbursed costs as an employment benefit from this company"
    val assetsSubheadingHiddenText: String = "Change if you got asset or asset transfers as an employment benefit from this company"
    val benefitsReceivedHiddenText: String = "Change if you got employment benefits from this company"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    def expectedP2(year: Int = defaultTaxYear): String = s"You cannot update your client’s employment benefits until 6 April $year."

    val expectedH1: String = "Check your client’s employment benefits"
    val expectedTitle: String = "Check your client’s employment benefits"
    val expectedP1: String = "Your client’s employment benefits are based on the information we already hold about them."
    val companyCarHiddenText: String = "Change if your client got a company car as an employment benefit from this company"
    val fuelForCompanyCarHiddenText: String = "Change if your client got a company car fuel as an employment benefit from this company"
    val companyVanHiddenText: String = "Change if your client got a company van as an employment benefit from this company"
    val fuelForCompanyVanHiddenText: String = "Change if your client got a company van fuel as an employment benefit from this company"
    val mileageBenefitHiddenText: String = "Change if your client got mileage as an employment benefit for using their own car"
    val accommodationHiddenText: String = "Change if your client got accommodation as an employment benefit from this company"
    val qualifyingRelocationCostsHiddenText: String = "Change if your client got qualifying relocation costs as an employment benefit from this company"
    val nonQualifyingRelocationCostsHiddenText: String = "Change if your client got non-qualifying relocation costs as an employment benefit from this company"
    val travelAndSubsistenceHiddenText: String = "Change if your client got travel and overnight stays as an employment benefit from this company"
    val personalCostsHiddenText: String = "Change if your client got personal incidental costs as an employment benefit from this company"
    val entertainmentHiddenText: String = "Change if your client got entertainment as an employment benefit from this company"
    val telephoneHiddenText: String = "Change if your client got a telephone as an employment benefit from this company"
    val servicesProvidedHiddenText: String = "Change if your client got services provided by their employer as an employment benefit from this company"
    val profSubscriptionsHiddenText: String = "Change if your client got professional subscriptions as an employment benefit from this company"
    val otherServicesHiddenText: String = "Change if your client got other services as an employment benefit from this company"
    val medicalInsHiddenText: String = "Change if your client got medical or dental insurance as an employment benefit from this company"
    val nurseryHiddenText: String = "Change if your client got childcare as an employment benefit from this company"
    val beneficialLoansHiddenText: String = "Change if your client got beneficial loans as an employment benefit from this company"
    val educationalHiddenText: String = "Change if your client got educational services as an employment benefit from this company"
    val incomeTaxPaidHiddenText: String = "Change if your client got Income Tax paid as an employment benefit from this company"
    val incurredCostsPaidHiddenText: String = "Change if your client got incurred costs paid as an employment benefit from this company"
    val nonTaxableHiddenText: String = "Change if your client got non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsHiddenText: String = "Change if your client got taxable costs reimbursed as an employment benefit from this company"
    val vouchersHiddenText: String = "Change if your client got vouchers, credit cards, or mileage allowance as an employment benefit from this company"
    val nonCashHiddenText: String = "Change if your client got non-cash employment benefit from this company"
    val otherBenefitsHiddenText: String = "Change if your client got other employment benefit from this company"
    val assetsHiddenText: String = "Change if your client got assets as an employment benefit from this company"
    val assetTransfersHiddenText: String = "Change if your client got asset transfers as an employment benefit from this company"
    val companyCarAmountHiddenText: String = "Change the amount for company car as an employment benefit your client got"
    val fuelForCompanyCarAmountHiddenText: String = "Change the amount for company car fuel as an employment benefit your client got from this company"
    val companyVanAmountHiddenText: String = "Change the amount for company van as an employment benefit your client got"
    val fuelForCompanyVanAmountHiddenText: String = "Change the amount for company van fuel as an employment benefit your client got from this company"
    val mileageBenefitAmountHiddenText: String = "Change the amount for mileage as an employment benefit your client got for using their own car"
    val accommodationAmountHiddenText: String = "Change the amount for accommodation as an employment benefit your client got from this company"
    val qualifyingRelocationCostsAmountHiddenText: String = "Change the amount for qualifying relocation costs as an employment benefit your client got from this company"
    val nonQualifyingRelocationCostsAmountHiddenText: String =
      "Change the amount for non-qualifying relocation costs as an employment benefit your client got from this company"
    val travelAndSubsistenceAmountHiddenText: String = "Change the amount for travel or overnight stays as an employment benefit your client got from this company"
    val personalCostsAmountHiddenText: String = "Change the amount for personal incidental costs as an employment benefit your client got from this company"
    val entertainmentAmountHiddenText: String = "Change the amount for entertainment as an employment benefit your client got from this company"
    val telephoneAmountHiddenText: String = "Change the amount for telephone as an employment benefit your client got from this company"
    val servicesProvidedAmountHiddenText: String = "Change the amount for services provided by your client’s employer as an employment benefit they got from this company"
    val profSubscriptionsAmountHiddenText: String = "Change the amount for professional subscriptions as an employment benefit your client got from this company"
    val otherServicesAmountHiddenText: String = "Change the amount for other services as an employment benefit your client got from this company"
    val medicalInsAmountHiddenText: String = "Change the amount for medical or dental insurance as an employment benefit your client got from this company"
    val nurseryAmountHiddenText: String = "Change the amount for childcare employment benefit your client got from this company"
    val beneficialLoansAmountHiddenText: String = "Change the amount for beneficial loans as an employment benefit your client got from this company"
    val educationalAmountHiddenText: String = "Change the amount for educational services as an employment benefit your client got from this company"
    val incomeTaxPaidAmountHiddenText: String = "Change the amount for Income Tax paid as an employment benefit your client got from this company"
    val incurredCostsPaidAmountHiddenText: String = "Change the amount for incurred costs paid as an employment benefit your client got from this company"
    val nonTaxableAmountHiddenText: String = "Change the amount your client got for non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsAmountHiddenText: String = "Change the amount your client got for taxable costs reimbursed as an employment benefit from this company"
    val vouchersAmountHiddenText: String = "Change the amount your client got for vouchers, credit cards, or mileage allowance as an employment benefit from this company"
    val nonCashAmountHiddenText: String = "Change the amount your client got for non-cash employment benefit from this company"
    val otherBenefitsAmountHiddenText: String = "Change the amount your client got for other employment benefit from this company"
    val assetsAmountHiddenText: String = "Change the amount your client got for assets as an employment benefit from this company"
    val assetTransfersAmountHiddenText: String = "Change the amount your client got for asset transfers as an employment benefit from this company"
    val carSubheadingHiddenText: String = "Change if your client got a car, van or fuel as an employment benefit from this company"
    val accommodationSubheadingHiddenText: String = "Change if your client got accommodation or relocation as an employment benefit from this company"
    val travelSubheadingHiddenText: String = "Change if your client got travel or entertainment as an employment benefit from this company"
    val utilitiesSubheadingHiddenText: String = "Change if your client got utilities or general services as an employment benefit from this company"
    val medicalSubheadingHiddenText: String = "Change if your client got medical insurance, nursery, education or loans as an employment benefit from this company"
    val incomeTaxSubheadingHiddenText: String = "Change if your client got Income Tax or incurred costs paid as an employment benefit from this company"
    val reimbursedSubheadingHiddenText: String = "Change if your client got vouchers, non-cash benefits or reimbursed costs as an employment benefit from this company"
    val assetsSubheadingHiddenText: String = "Change if your client got asset or asset transfers as an employment benefit from this company"
    val benefitsReceivedHiddenText: String = "Change if your client got employment benefits from this company"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    def expectedP2(year: Int = defaultTaxYear): String = s"You cannot update your employment benefits until 6 April $year."

    val expectedH1: String = "Check your employment benefits"
    val expectedTitle: String = "Check your employment benefits"
    val expectedP1: String = "Your employment benefits are based on the information we already hold about you."
    val companyCarHiddenText: String = "Change if you got a company car as an employment benefit from this company"
    val fuelForCompanyCarHiddenText: String = "Change if you got a company car fuel as an employment benefit from this company"
    val companyVanHiddenText: String = "Change if you got a company van as an employment benefit from this company"
    val fuelForCompanyVanHiddenText: String = "Change if you got a company van fuel as an employment benefit from this company"
    val mileageBenefitHiddenText: String = "Change if you got mileage as an employment benefit for using your own car"
    val accommodationHiddenText: String = "Change if you got accommodation as an employment benefit from this company"
    val qualifyingRelocationCostsHiddenText: String = "Change if you got qualifying relocation costs as an employment benefit from this company"
    val nonQualifyingRelocationCostsHiddenText: String = "Change if you got non-qualifying relocation costs as an employment benefit from this company"
    val travelAndSubsistenceHiddenText: String = "Change if you got travel and overnight stays as an employment benefit from this company"
    val personalCostsHiddenText: String = "Change if you got personal incidental costs as an employment benefit from this company"
    val entertainmentHiddenText: String = "Change if you got entertainment as an employment benefit from this company"
    val telephoneHiddenText: String = "Change if you got a telephone as an employment benefit from this company"
    val servicesProvidedHiddenText: String = "Change if you got services provided by your employer as an employment benefit from this company"
    val profSubscriptionsHiddenText: String = "Change if you got professional subscriptions as an employment benefit from this company"
    val otherServicesHiddenText: String = "Change if you got other services as an employment benefit from this company"
    val medicalInsHiddenText: String = "Change if you got medical or dental insurance as an employment benefit from this company"
    val nurseryHiddenText: String = "Change if you got childcare as an employment benefit from this company"
    val beneficialLoansHiddenText: String = "Change if you got beneficial loans as an employment benefit from this company"
    val educationalHiddenText: String = "Change if you got educational services as an employment benefit from this company"
    val incomeTaxPaidHiddenText: String = "Change if you got Income Tax paid as an employment benefit from this company"
    val incurredCostsPaidHiddenText: String = "Change if you got incurred costs paid as an employment benefit from this company"
    val nonTaxableHiddenText: String = "Change if you got non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsHiddenText: String = "Change if you got taxable costs reimbursed as an employment benefit from this company"
    val vouchersHiddenText: String = "Change if you got vouchers, credit cards, or mileage allowance as an employment benefit from this company"
    val nonCashHiddenText: String = "Change if you got non-cash employment benefit from this company"
    val otherBenefitsHiddenText: String = "Change if you got other employment benefit from this company"
    val assetsHiddenText: String = "Change if you got assets as an employment benefit from this company"
    val assetTransfersHiddenText: String = "Change if you got asset transfers as an employment benefit from this company"
    val companyCarAmountHiddenText: String = "Change the amount for company car as an employment benefit you got"
    val fuelForCompanyCarAmountHiddenText: String = "Change the amount for company car fuel as an employment benefit you got from this company"
    val companyVanAmountHiddenText: String = "Change the amount for company van as an employment benefit you got"
    val fuelForCompanyVanAmountHiddenText: String = "Change the amount for company van fuel as an employment benefit you got from this company"
    val mileageBenefitAmountHiddenText: String = "Change the amount for mileage as an employment benefit you got for using your own car"
    val accommodationAmountHiddenText: String = "Change the amount for accommodation as an employment benefit you got from this company"
    val qualifyingRelocationCostsAmountHiddenText: String = "Change the amount for qualifying relocation costs as an employment benefit you got from this company"
    val nonQualifyingRelocationCostsAmountHiddenText: String = "Change the amount for non-qualifying relocation costs as an employment benefit you got from this company"
    val travelAndSubsistenceAmountHiddenText: String = "Change the amount for travel or overnight stays as an employment benefit you got from this company"
    val personalCostsAmountHiddenText: String = "Change the amount for personal incidental costs as an employment benefit you got from this company"
    val entertainmentAmountHiddenText: String = "Change the amount for entertainment as an employment benefit you got from this company"
    val telephoneAmountHiddenText: String = "Change the amount for telephone as an employment benefit you got from this company"
    val servicesProvidedAmountHiddenText: String = "Change the amount for services provided by your employer as an employment benefit you got from this company"
    val profSubscriptionsAmountHiddenText: String = "Change the amount for professional subscriptions as an employment benefit you got from this company"
    val otherServicesAmountHiddenText: String = "Change the amount for other services as an employment benefit you got from this company"
    val medicalInsAmountHiddenText: String = "Change the amount for medical or dental insurance as an employment benefit you got from this company"
    val nurseryAmountHiddenText: String = "Change the amount for childcare employment benefit you got from this company"
    val beneficialLoansAmountHiddenText: String = "Change the amount for beneficial loans as an employment benefit you got from this company"
    val educationalAmountHiddenText: String = "Change the amount for educational services as an employment benefit you got from this company"
    val incomeTaxPaidAmountHiddenText: String = "Change the amount for Income Tax paid as an employment benefit you got from this company"
    val incurredCostsPaidAmountHiddenText: String = "Change the amount for incurred costs paid as an employment benefit you got from this company"
    val nonTaxableAmountHiddenText: String = "Change the amount you got for non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsAmountHiddenText: String = "Change the amount you got for taxable costs reimbursed as an employment benefit from this company"
    val vouchersAmountHiddenText: String = "Change the amount you got for vouchers, credit cards, or mileage allowance as an employment benefit from this company"
    val nonCashAmountHiddenText: String = "Change the amount you got for non-cash employment benefit from this company"
    val otherBenefitsAmountHiddenText: String = "Change the amount you got for other employment benefit from this company"
    val assetsAmountHiddenText: String = "Change the amount you got for assets as an employment benefit from this company"
    val assetTransfersAmountHiddenText: String = "Change the amount you got for asset transfers as an employment benefit from this company"
    val carSubheadingHiddenText: String = "Change if you got a car, van or fuel as an employment benefit from this company"
    val accommodationSubheadingHiddenText: String = "Change if you got accommodation or relocation as an employment benefit from this company"
    val travelSubheadingHiddenText: String = "Change if you got travel or entertainment as an employment benefit from this company"
    val utilitiesSubheadingHiddenText: String = "Change if you got utilities or general services as an employment benefit from this company"
    val medicalSubheadingHiddenText: String = "Change if you got medical insurance, nursery, education or loans as an employment benefit from this company"
    val incomeTaxSubheadingHiddenText: String = "Change if you got Income Tax or incurred costs paid as an employment benefit from this company"
    val reimbursedSubheadingHiddenText: String = "Change if you got vouchers, non-cash benefits or reimbursed costs as an employment benefit from this company"
    val assetsSubheadingHiddenText: String = "Change if you got asset or asset transfers as an employment benefit from this company"
    val benefitsReceivedHiddenText: String = "Change if you got employment benefits from this company"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    def expectedP2(year: Int = defaultTaxYear): String = s"You cannot update your client’s employment benefits until 6 April $year."

    val expectedH1: String = "Check your client’s employment benefits"
    val expectedTitle: String = "Check your client’s employment benefits"
    val expectedP1: String = "Your client’s employment benefits are based on the information we already hold about them."
    val companyCarHiddenText: String = "Change if your client got a company car as an employment benefit from this company"
    val fuelForCompanyCarHiddenText: String = "Change if your client got a company car fuel as an employment benefit from this company"
    val companyVanHiddenText: String = "Change if your client got a company van as an employment benefit from this company"
    val fuelForCompanyVanHiddenText: String = "Change if your client got a company van fuel as an employment benefit from this company"
    val mileageBenefitHiddenText: String = "Change if your client got mileage as an employment benefit for using their own car"
    val accommodationHiddenText: String = "Change if your client got accommodation as an employment benefit from this company"
    val qualifyingRelocationCostsHiddenText: String = "Change if your client got qualifying relocation costs as an employment benefit from this company"
    val nonQualifyingRelocationCostsHiddenText: String = "Change if your client got non-qualifying relocation costs as an employment benefit from this company"
    val travelAndSubsistenceHiddenText: String = "Change if your client got travel and overnight stays as an employment benefit from this company"
    val personalCostsHiddenText: String = "Change if your client got personal incidental costs as an employment benefit from this company"
    val entertainmentHiddenText: String = "Change if your client got entertainment as an employment benefit from this company"
    val telephoneHiddenText: String = "Change if your client got a telephone as an employment benefit from this company"
    val servicesProvidedHiddenText: String = "Change if your client got services provided by their employer as an employment benefit from this company"
    val profSubscriptionsHiddenText: String = "Change if your client got professional subscriptions as an employment benefit from this company"
    val otherServicesHiddenText: String = "Change if your client got other services as an employment benefit from this company"
    val medicalInsHiddenText: String = "Change if your client got medical or dental insurance as an employment benefit from this company"
    val nurseryHiddenText: String = "Change if your client got childcare as an employment benefit from this company"
    val beneficialLoansHiddenText: String = "Change if your client got beneficial loans as an employment benefit from this company"
    val educationalHiddenText: String = "Change if your client got educational services as an employment benefit from this company"
    val incomeTaxPaidHiddenText: String = "Change if your client got Income Tax paid as an employment benefit from this company"
    val incurredCostsPaidHiddenText: String = "Change if your client got incurred costs paid as an employment benefit from this company"
    val nonTaxableHiddenText: String = "Change if your client got non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsHiddenText: String = "Change if your client got taxable costs reimbursed as an employment benefit from this company"
    val vouchersHiddenText: String = "Change if your client got vouchers, credit cards, or mileage allowance as an employment benefit from this company"
    val nonCashHiddenText: String = "Change if your client got non-cash employment benefit from this company"
    val otherBenefitsHiddenText: String = "Change if your client got other employment benefit from this company"
    val assetsHiddenText: String = "Change if your client got assets as an employment benefit from this company"
    val assetTransfersHiddenText: String = "Change if your client got asset transfers as an employment benefit from this company"
    val companyCarAmountHiddenText: String = "Change the amount for company car as an employment benefit your client got"
    val fuelForCompanyCarAmountHiddenText: String = "Change the amount for company car fuel as an employment benefit your client got from this company"
    val companyVanAmountHiddenText: String = "Change the amount for company van as an employment benefit your client got"
    val fuelForCompanyVanAmountHiddenText: String = "Change the amount for company van fuel as an employment benefit your client got from this company"
    val mileageBenefitAmountHiddenText: String = "Change the amount for mileage as an employment benefit your client got for using their own car"
    val accommodationAmountHiddenText: String = "Change the amount for accommodation as an employment benefit your client got from this company"
    val qualifyingRelocationCostsAmountHiddenText: String = "Change the amount for qualifying relocation costs as an employment benefit your client got from this company"
    val nonQualifyingRelocationCostsAmountHiddenText: String =
      "Change the amount for non-qualifying relocation costs as an employment benefit your client got from this company"
    val travelAndSubsistenceAmountHiddenText: String = "Change the amount for travel or overnight stays as an employment benefit your client got from this company"
    val personalCostsAmountHiddenText: String = "Change the amount for personal incidental costs as an employment benefit your client got from this company"
    val entertainmentAmountHiddenText: String = "Change the amount for entertainment as an employment benefit your client got from this company"
    val telephoneAmountHiddenText: String = "Change the amount for telephone as an employment benefit your client got from this company"
    val servicesProvidedAmountHiddenText: String = "Change the amount for services provided by your client’s employer as an employment benefit they got from this company"
    val profSubscriptionsAmountHiddenText: String = "Change the amount for professional subscriptions as an employment benefit your client got from this company"
    val otherServicesAmountHiddenText: String = "Change the amount for other services as an employment benefit your client got from this company"
    val medicalInsAmountHiddenText: String = "Change the amount for medical or dental insurance as an employment benefit your client got from this company"
    val nurseryAmountHiddenText: String = "Change the amount for childcare employment benefit your client got from this company"
    val beneficialLoansAmountHiddenText: String = "Change the amount for beneficial loans as an employment benefit your client got from this company"
    val educationalAmountHiddenText: String = "Change the amount for educational services as an employment benefit your client got from this company"
    val incomeTaxPaidAmountHiddenText: String = "Change the amount for Income Tax paid as an employment benefit your client got from this company"
    val incurredCostsPaidAmountHiddenText: String = "Change the amount for incurred costs paid as an employment benefit your client got from this company"
    val nonTaxableAmountHiddenText: String = "Change the amount your client got for non-taxable costs reimbursed as an employment benefit from this company"
    val taxableCostsAmountHiddenText: String = "Change the amount your client got for taxable costs reimbursed as an employment benefit from this company"
    val vouchersAmountHiddenText: String = "Change the amount your client got for vouchers, credit cards, or mileage allowance as an employment benefit from this company"
    val nonCashAmountHiddenText: String = "Change the amount your client got for non-cash employment benefit from this company"
    val otherBenefitsAmountHiddenText: String = "Change the amount your client got for other employment benefit from this company"
    val assetsAmountHiddenText: String = "Change the amount your client got for assets as an employment benefit from this company"
    val assetTransfersAmountHiddenText: String = "Change the amount your client got for asset transfers as an employment benefit from this company"
    val carSubheadingHiddenText: String = "Change if your client got a car, van or fuel as an employment benefit from this company"
    val accommodationSubheadingHiddenText: String = "Change if your client got accommodation or relocation as an employment benefit from this company"
    val travelSubheadingHiddenText: String = "Change if your client got travel or entertainment as an employment benefit from this company"
    val utilitiesSubheadingHiddenText: String = "Change if your client got utilities or general services as an employment benefit from this company"
    val medicalSubheadingHiddenText: String = "Change if your client got medical insurance, nursery, education or loans as an employment benefit from this company"
    val incomeTaxSubheadingHiddenText: String = "Change if your client got Income Tax or incurred costs paid as an employment benefit from this company"
    val reimbursedSubheadingHiddenText: String = "Change if your client got vouchers, non-cash benefits or reimbursed costs as an employment benefit from this company"
    val assetsSubheadingHiddenText: String = "Change if your client got asset or asset transfers as an employment benefit from this company"
    val benefitsReceivedHiddenText: String = "Change if your client got employment benefits from this company"
  }

  object Hrefs {
    val receiveAnyBenefitsHref = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/company-benefits?employmentId=001"
    val carVanFuelBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/car-van-fuel?employmentId=001"
    val companyCarHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/company-car?employmentId=001"
    val companyCarAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/company-car-amount?employmentId=001"
    val companyCarFuelBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/car-fuel?employmentId=001"
    val carFuelAmountBenefitsHref = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/car-fuel-amount?employmentId=001"
    val mileageAmountBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/mileage-amount?employmentId=001"
    val receivedOwnCarMileageBenefitHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/mileage?employmentId=001"
    val companyVanBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/company-van?employmentId=001"
    val companyVanFuelBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/van-fuel?employmentId=001"
    val companyVanBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/company-van-amount?employmentId=001"
    val companyVanFuelBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/van-fuel-amount?employmentId=001"
    val accommodationRelocationBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/accommodation-relocation?employmentId=001"
    val livingAccommodationBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/living-accommodation?employmentId=001"
    val livingAccommodationAmountBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/living-accommodation-amount?employmentId=001"
    val qualifyingRelocationBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/qualifying-relocation?employmentId=001"
    val qualifyingRelocationBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/qualifying-relocation-amount?employmentId=001"
    val nonQualifyingRelocationBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/non-qualifying-relocation?employmentId=001"
    val nonQualifyingRelocationAmountBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/non-qualifying-relocation-amount?employmentId=001"
    val travelEntertainmentBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/travel-entertainment?employmentId=001"
    val travelOrSubsistenceBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/travel-subsistence?employmentId=001"
    val travelOrSubsistenceBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/travel-subsistence-amount?employmentId=001"
    val incidentalCostsBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/incidental-overnight-costs-amount?employmentId=001"
    val utilitiesOrGeneralServicesBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/utility-general-service?employmentId=001"
    val entertainingBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/entertainment-expenses?employmentId=001"
    val telephoneBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear-1}/benefits/telephone?employmentId=001"
    val otherServicesBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear-1}/benefits/other-services-amount?employmentId=001"
    val incidentalOvernightCostEmploymentBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/incidental-overnight-costs?employmentId=001"
    val otherServicesBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear-1}/benefits/other-services?employmentId=001"
    val entertainmentAmountBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/entertainment-expenses-amount?employmentId=001"
    val employerProvidedServicesBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/employer-provided-services?employmentId=001"
    val telephoneEmploymentBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/benefits/telephone-amount?employmentId=001"
    val employerProvidedServicesBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear-1}/benefits/employer-provided-services-amount?employmentId=001"
    val professionalSubscriptionsBenefitsHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear-1}/benefits/professional-fees-or-subscriptions?employmentId=001"
    val professionalSubscriptionsBenefitsAmountHref: String = s"/income-through-software/return/employment-income/${defaultTaxYear-1}/benefits/professional-fees-or-subscriptions-amount?employmentId=001"
    }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {
    import Hrefs._
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return a fully populated page when all the fields are populated for in year" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear)
            urlGet(url(), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption())
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, Selectors.p1)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2(), Selectors.p2)
          textOnPageCheck(user.commonExpectedResults.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(user.commonExpectedResults.companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(user.commonExpectedResults.fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(user.commonExpectedResults.companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(user.commonExpectedResults.fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(user.commonExpectedResults.mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(user.commonExpectedResults.accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(user.commonExpectedResults.accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(user.commonExpectedResults.qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(user.commonExpectedResults.nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(user.commonExpectedResults.travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(user.commonExpectedResults.travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(user.commonExpectedResults.personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(user.commonExpectedResults.entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(user.commonExpectedResults.utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(user.commonExpectedResults.telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(user.commonExpectedResults.servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(user.commonExpectedResults.profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(user.commonExpectedResults.otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(user.commonExpectedResults.medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(user.commonExpectedResults.medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(user.commonExpectedResults.nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(user.commonExpectedResults.educational, fieldNameSelector(13, 3))
          textOnPageCheck("£19", fieldAmountSelector(13, 3))
          textOnPageCheck(user.commonExpectedResults.beneficialLoans, fieldNameSelector(13, 4))
          textOnPageCheck("£18", fieldAmountSelector(13, 4))
          textOnPageCheck(user.commonExpectedResults.incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(user.commonExpectedResults.incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(user.commonExpectedResults.incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(user.commonExpectedResults.reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(user.commonExpectedResults.nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(user.commonExpectedResults.taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(user.commonExpectedResults.vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(user.commonExpectedResults.nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(user.commonExpectedResults.otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(user.commonExpectedResults.assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(user.commonExpectedResults.assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(user.commonExpectedResults.assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))
          welshToggleCheck(user.isWelsh)
        }

        "return a fully populated page when all the fields are populated when at the end of the year" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear - 1)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          val specificResults = user.specificExpectedResults.get
          val commonResults = user.commonExpectedResults
          val dummyHref = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/check-employment-benefits?employmentId=001"

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specificResults.expectedTitle)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(defaultTaxYear - 1))
          textOnPageCheck(specificResults.expectedP1, Selectors.p1)

          changeAmountRowCheck(commonResults.benefitsReceived, commonResults.yes, 3, 1, s"${user.commonExpectedResults.changeText} ${specificResults.benefitsReceivedHiddenText}", receiveAnyBenefitsHref)

          textOnPageCheck(commonResults.vehicleHeader, fieldHeaderSelector(4))

          changeAmountRowCheck(commonResults.carSubheading, commonResults.yes, 5, 1, s"${user.commonExpectedResults.changeText} ${specificResults.carSubheadingHiddenText}", carVanFuelBenefitsHref)
          changeAmountRowCheck(commonResults.companyCar, commonResults.yes, 5, 2, s"${user.commonExpectedResults.changeText} ${specificResults.companyCarHiddenText}", companyCarHref)
          changeAmountRowCheck(commonResults.companyCarAmount, "£1.23", 5, 3, s"${user.commonExpectedResults.changeText} ${specificResults.companyCarAmountHiddenText}", companyCarAmountHref)
          changeAmountRowCheck(commonResults.fuelForCompanyCar, commonResults.yes, 5, 4, s"${user.commonExpectedResults.changeText} ${specificResults.fuelForCompanyCarHiddenText}", companyCarFuelBenefitsHref)
          changeAmountRowCheck(commonResults.fuelForCompanyCarAmount, "£2", 5, 5, s"${user.commonExpectedResults.changeText} ${specificResults.fuelForCompanyCarAmountHiddenText}", carFuelAmountBenefitsHref)
          changeAmountRowCheck(commonResults.companyVan, commonResults.yes, 5, 6, s"${user.commonExpectedResults.changeText} ${specificResults.companyVanHiddenText}", companyVanBenefitsHref)
          changeAmountRowCheck(commonResults.fuelForCompanyVan, commonResults.yes, 5, 8, s"${user.commonExpectedResults.changeText} ${specificResults.fuelForCompanyVanHiddenText}", companyVanFuelBenefitsHref)
          changeAmountRowCheck(commonResults.companyVanAmount, "£3", 5, 7, s"${user.commonExpectedResults.changeText} ${specificResults.companyVanAmountHiddenText}", companyVanBenefitsAmountHref)
          changeAmountRowCheck(commonResults.fuelForCompanyVanAmount, "£4", 5, 9, s"${user.commonExpectedResults.changeText} ${specificResults.fuelForCompanyVanAmountHiddenText}", companyVanFuelBenefitsAmountHref)
          changeAmountRowCheck(commonResults.mileageBenefit, commonResults.yes, 5, 10, s"${user.commonExpectedResults.changeText} ${specificResults.mileageBenefitHiddenText}", receivedOwnCarMileageBenefitHref)
          changeAmountRowCheck(commonResults.mileageBenefitAmount, "£5", 5, 11, s"${user.commonExpectedResults.changeText} ${specificResults.mileageBenefitAmountHiddenText}", mileageAmountBenefitsHref)

          textOnPageCheck(commonResults.accommodationHeader, fieldHeaderSelector(6))
          changeAmountRowCheck(commonResults.accommodationSubheading, commonResults.yes, 7, 1, s"${user.commonExpectedResults.changeText} ${specificResults.accommodationSubheadingHiddenText}", accommodationRelocationBenefitsHref)
          changeAmountRowCheck(commonResults.accommodation, commonResults.yes, 7, 2, s"${user.commonExpectedResults.changeText} ${specificResults.accommodationHiddenText}", livingAccommodationBenefitsHref)
          changeAmountRowCheck(commonResults.accommodationAmount, "£6", 7, 3, s"${user.commonExpectedResults.changeText} ${specificResults.accommodationAmountHiddenText}", livingAccommodationAmountBenefitsHref)
          changeAmountRowCheck(commonResults.qualifyingRelocationCosts, commonResults.yes, 7, 4, s"${user.commonExpectedResults.changeText} ${specificResults.qualifyingRelocationCostsHiddenText}", qualifyingRelocationBenefitsHref)
          changeAmountRowCheck(commonResults.qualifyingRelocationCostsAmount, "£7", 7, 5, s"${user.commonExpectedResults.changeText} ${specificResults.qualifyingRelocationCostsAmountHiddenText}", qualifyingRelocationBenefitsAmountHref)
          changeAmountRowCheck(commonResults.nonQualifyingRelocationCosts, commonResults.yes, 7, 6, s"${user.commonExpectedResults.changeText} ${specificResults.nonQualifyingRelocationCostsHiddenText}", nonQualifyingRelocationBenefitsHref)
          changeAmountRowCheck(commonResults.nonQualifyingRelocationCostsAmount, "£8", 7, 7, s"${user.commonExpectedResults.changeText} ${specificResults.nonQualifyingRelocationCostsAmountHiddenText}", nonQualifyingRelocationAmountBenefitsHref)

          textOnPageCheck(commonResults.travelHeader, fieldHeaderSelector(8))
          changeAmountRowCheck(commonResults.travelSubheading, commonResults.yes, 9, 1, s"${user.commonExpectedResults.changeText} ${specificResults.travelSubheadingHiddenText}", travelEntertainmentBenefitsAmountHref)
          changeAmountRowCheck(commonResults.travelAndSubsistence, commonResults.yes, 9, 2, s"${user.commonExpectedResults.changeText} ${specificResults.travelAndSubsistenceHiddenText}", travelOrSubsistenceBenefitsHref)
          changeAmountRowCheck(commonResults.travelAndSubsistenceAmount, "£9", 9, 3, s"${user.commonExpectedResults.changeText} ${specificResults.travelAndSubsistenceAmountHiddenText}", travelOrSubsistenceBenefitsAmountHref)
          changeAmountRowCheck(commonResults.personalCosts, commonResults.yes, 9, 4, s"${user.commonExpectedResults.changeText} ${specificResults.personalCostsHiddenText}", incidentalOvernightCostEmploymentBenefitsHref)
          changeAmountRowCheck(commonResults.personalCostsAmount, "£10", 9, 5, s"${user.commonExpectedResults.changeText} ${specificResults.personalCostsAmountHiddenText}", incidentalCostsBenefitsAmountHref)
          changeAmountRowCheck(commonResults.entertainment, commonResults.yes, 9, 6, s"${user.commonExpectedResults.changeText} ${specificResults.entertainmentHiddenText}", entertainingBenefitsHref)
          changeAmountRowCheck(commonResults.entertainmentAmount, "£11", 9, 7, s"${user.commonExpectedResults.changeText} ${specificResults.entertainmentAmountHiddenText}", entertainmentAmountBenefitsHref)

          textOnPageCheck(commonResults.utilitiesHeader, fieldHeaderSelector(10))
          changeAmountRowCheck(commonResults.utilitiesSubheading, commonResults.yes, 11, 1, s"${user.commonExpectedResults.changeText} ${specificResults.utilitiesSubheadingHiddenText}", utilitiesOrGeneralServicesBenefitsHref)
          changeAmountRowCheck(commonResults.telephone, commonResults.yes, 11, 2, s"${user.commonExpectedResults.changeText} ${specificResults.telephoneHiddenText}", telephoneBenefitsHref)
          changeAmountRowCheck(commonResults.telephoneAmount, "£12", 11, 3, s"${user.commonExpectedResults.changeText} ${specificResults.telephoneAmountHiddenText}", telephoneEmploymentBenefitsAmountHref)
          changeAmountRowCheck(commonResults.servicesProvided, commonResults.yes, 11, 4, s"${user.commonExpectedResults.changeText} ${specificResults.servicesProvidedHiddenText}", employerProvidedServicesBenefitsHref)
          changeAmountRowCheck(commonResults.servicesProvidedAmount, "£13", 11, 5, s"${user.commonExpectedResults.changeText} ${specificResults.servicesProvidedAmountHiddenText}", employerProvidedServicesBenefitsAmountHref)
          changeAmountRowCheck(commonResults.profSubscriptions, commonResults.yes, 11, 6, s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsHiddenText}", professionalSubscriptionsBenefitsHref)
          changeAmountRowCheck(commonResults.profSubscriptionsAmount, "£14", 11, 7, s"${user.commonExpectedResults.changeText} ${specificResults.profSubscriptionsAmountHiddenText}", professionalSubscriptionsBenefitsAmountHref)
          changeAmountRowCheck(commonResults.otherServices, commonResults.yes, 11, 8, s"${user.commonExpectedResults.changeText} ${specificResults.otherServicesHiddenText}", otherServicesBenefitsHref)
          changeAmountRowCheck(commonResults.otherServicesAmount, "£15", 11, 9, s"${user.commonExpectedResults.changeText} ${specificResults.otherServicesAmountHiddenText}", otherServicesBenefitsAmountHref)

          textOnPageCheck(commonResults.medicalHeader, fieldHeaderSelector(12))
          changeAmountRowCheck(commonResults.medicalSubheading, commonResults.yes, 13, 1, s"${user.commonExpectedResults.changeText} ${specificResults.medicalSubheadingHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.medicalIns, commonResults.yes, 13, 2, s"${user.commonExpectedResults.changeText} ${specificResults.medicalInsHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.medicalInsAmount, "£16", 13, 3, s"${user.commonExpectedResults.changeText} ${specificResults.medicalInsAmountHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.nursery, commonResults.yes, 13, 4, s"${user.commonExpectedResults.changeText} ${specificResults.nurseryHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.nurseryAmount, "£17", 13, 5, s"${user.commonExpectedResults.changeText} ${specificResults.nurseryAmountHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.educational, commonResults.yes, 13, 6, s"${user.commonExpectedResults.changeText} ${specificResults.educationalHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.educationalAmount, "£19", 13, 7, s"${user.commonExpectedResults.changeText} ${specificResults.educationalAmountHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.beneficialLoans, commonResults.yes, 13, 8, s"${user.commonExpectedResults.changeText} ${specificResults.beneficialLoansHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.beneficialLoansAmount, "£18", 13, 9, s"${user.commonExpectedResults.changeText} ${specificResults.beneficialLoansAmountHiddenText}", dummyHref)

          textOnPageCheck(commonResults.incomeTaxHeader, fieldHeaderSelector(14))
          changeAmountRowCheck(commonResults.incomeTaxSubheading, commonResults.yes, 15, 1, s"${user.commonExpectedResults.changeText} ${specificResults.incomeTaxSubheadingHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.incomeTaxPaid, commonResults.yes, 15, 2, s"${user.commonExpectedResults.changeText} ${specificResults.incomeTaxPaidHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.incomeTaxPaidAmount, "£20", 15, 3, s"${user.commonExpectedResults.changeText} ${specificResults.incomeTaxPaidAmountHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.incurredCostsPaid, commonResults.yes, 15, 4, s"${user.commonExpectedResults.changeText} ${specificResults.incurredCostsPaidHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.incurredCostsPaidAmount, "£21", 15, 5, s"${user.commonExpectedResults.changeText} ${specificResults.incurredCostsPaidAmountHiddenText}", dummyHref)

          textOnPageCheck(commonResults.reimbursedHeader, fieldHeaderSelector(16))
          changeAmountRowCheck(commonResults.reimbursedSubheading, commonResults.yes, 17, 1, s"${user.commonExpectedResults.changeText} ${specificResults.reimbursedSubheadingHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.nonTaxable, commonResults.yes, 17, 2, s"${user.commonExpectedResults.changeText} ${specificResults.nonTaxableHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.nonTaxableAmount, "£22", 17, 3, s"${user.commonExpectedResults.changeText} ${specificResults.nonTaxableAmountHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.taxableCosts, commonResults.yes, 17, 4, s"${user.commonExpectedResults.changeText} ${specificResults.taxableCostsHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.taxableCostsAmount, "£23", 17, 5, s"${user.commonExpectedResults.changeText} ${specificResults.taxableCostsAmountHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.vouchers, commonResults.yes, 17, 6, s"${user.commonExpectedResults.changeText} ${specificResults.vouchersHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.vouchersAmount, "£24", 17, 7, s"${user.commonExpectedResults.changeText} ${specificResults.vouchersAmountHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.nonCash, commonResults.yes, 17, 8, s"${user.commonExpectedResults.changeText} ${specificResults.nonCashHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.nonCashAmount, "£25", 17, 9, s"${user.commonExpectedResults.changeText} ${specificResults.nonCashAmountHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.otherBenefits, commonResults.yes, 17, 10, s"${user.commonExpectedResults.changeText} ${specificResults.otherBenefitsHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.otherBenefitsAmount, "£26", 17, 11, s"${user.commonExpectedResults.changeText} ${specificResults.otherBenefitsAmountHiddenText}", dummyHref)

          textOnPageCheck(commonResults.assetsHeader, fieldHeaderSelector(18))
          changeAmountRowCheck(commonResults.assetsSubheading, commonResults.yes, 19, 1, s"${user.commonExpectedResults.changeText} ${specificResults.assetsSubheadingHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.assets, commonResults.yes, 19, 2, s"${user.commonExpectedResults.changeText} ${specificResults.assetsHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.assetsAmount, "£27", 19, 3, s"${user.commonExpectedResults.changeText} ${specificResults.assetsAmountHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.assetTransfers, commonResults.yes, 19, 4, s"${user.commonExpectedResults.changeText} ${specificResults.assetTransfersHiddenText}", dummyHref)
          changeAmountRowCheck(commonResults.assetTransfersAmount, "£280000", 19, 5, s"${user.commonExpectedResults.changeText} ${specificResults.assetTransfersAmountHiddenText}", dummyHref)

          buttonCheck(commonResults.saveAndContinue)

          welshToggleCheck(user.isWelsh)
        }

        "return a redirect at the end of the year when id is not found" in {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear - 1)
            urlGet(s"$appUrl/${taxYear - 1}/check-employment-benefits?employmentId=0022", welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2021/view")
        }

        "return only the relevant data on the page when only certain data items are in mongo for EOY" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(filteredBenefits)))), nino, defaultTaxYear - 1)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(defaultTaxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val specificResults = user.specificExpectedResults.get
          val commonResults = user.commonExpectedResults
          val dummyHref = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/check-employment-benefits?employmentId=001"

          titleCheck(specificResults.expectedTitle)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(defaultTaxYear - 1))
          textOnPageCheck(specificResults.expectedP1, Selectors.p1)

          changeAmountRowCheck(commonResults.benefitsReceived, commonResults.yes, 3, 1, s"${user.commonExpectedResults.changeText} ${specificResults.benefitsReceivedHiddenText}", receiveAnyBenefitsHref)

          textOnPageCheck(commonResults.vehicleHeader, fieldHeaderSelector(4))

          changeAmountRowCheck(commonResults.carSubheading, commonResults.yes, 5, 1, s"${user.commonExpectedResults.changeText} ${specificResults.carSubheadingHiddenText}", carVanFuelBenefitsHref)
          changeAmountRowCheck(commonResults.companyCar, commonResults.no, 5, 2, s"${user.commonExpectedResults.changeText} ${specificResults.companyCarHiddenText}", companyCarHref)
          changeAmountRowCheck(commonResults.fuelForCompanyCar, commonResults.no, 5, 3, s"${user.commonExpectedResults.changeText} ${specificResults.fuelForCompanyCarHiddenText}", companyCarFuelBenefitsHref)
          changeAmountRowCheck(commonResults.companyVan, commonResults.yes, 5, 4, s"${user.commonExpectedResults.changeText} ${specificResults.companyVanHiddenText}", companyVanBenefitsHref)
          changeAmountRowCheck(commonResults.fuelForCompanyVan, commonResults.yes, 5, 6, s"${user.commonExpectedResults.changeText} ${specificResults.fuelForCompanyVanHiddenText}", companyVanFuelBenefitsHref)
          changeAmountRowCheck(commonResults.companyVanAmount, "£3", 5, 5, s"${user.commonExpectedResults.changeText} ${specificResults.companyVanAmountHiddenText}", companyVanBenefitsAmountHref)
          changeAmountRowCheck(commonResults.fuelForCompanyVanAmount, "£4", 5, 7, s"${user.commonExpectedResults.changeText} ${specificResults.fuelForCompanyVanAmountHiddenText}", companyVanFuelBenefitsAmountHref)
          changeAmountRowCheck(commonResults.mileageBenefit, commonResults.yes, 5, 8, s"${user.commonExpectedResults.changeText} ${specificResults.mileageBenefitHiddenText}", receivedOwnCarMileageBenefitHref)
          changeAmountRowCheck(commonResults.mileageBenefitAmount, "£5", 5, 9, s"${user.commonExpectedResults.changeText} ${specificResults.mileageBenefitAmountHiddenText}", mileageAmountBenefitsHref)

          textOnPageCheck(user.commonExpectedResults.accommodationHeader, fieldHeaderSelector(6))
          changeAmountRowCheck(commonResults.accommodationSubheading, commonResults.no, 7, 1, s"${user.commonExpectedResults.changeText} ${specificResults.accommodationSubheadingHiddenText}", accommodationRelocationBenefitsHref)

          textOnPageCheck(user.commonExpectedResults.travelHeader, fieldHeaderSelector(8))
          changeAmountRowCheck(commonResults.travelSubheading, commonResults.no, 9, 1, s"${user.commonExpectedResults.changeText} ${specificResults.travelSubheadingHiddenText}", travelEntertainmentBenefitsAmountHref)

          textOnPageCheck(user.commonExpectedResults.utilitiesHeader, fieldHeaderSelector(10))
          changeAmountRowCheck(commonResults.utilitiesSubheading, commonResults.no, 11, 1, s"${user.commonExpectedResults.changeText} ${specificResults.utilitiesSubheadingHiddenText}", utilitiesOrGeneralServicesBenefitsHref)

          textOnPageCheck(user.commonExpectedResults.medicalHeader, fieldHeaderSelector(12))
          changeAmountRowCheck(commonResults.medicalSubheading, commonResults.no, 13, 1, s"${user.commonExpectedResults.changeText} ${specificResults.medicalSubheadingHiddenText}", dummyHref)

          textOnPageCheck(user.commonExpectedResults.incomeTaxHeader, fieldHeaderSelector(14))
          changeAmountRowCheck(commonResults.incomeTaxSubheading, commonResults.no, 15, 1, s"${user.commonExpectedResults.changeText} ${specificResults.incomeTaxSubheadingHiddenText}", dummyHref)

          textOnPageCheck(user.commonExpectedResults.reimbursedHeader, fieldHeaderSelector(16))
          changeAmountRowCheck(commonResults.reimbursedSubheading, commonResults.no, 17, 1, s"${user.commonExpectedResults.changeText} ${specificResults.reimbursedSubheadingHiddenText}", dummyHref)

          textOnPageCheck(user.commonExpectedResults.assetsHeader, fieldHeaderSelector(18))
          changeAmountRowCheck(commonResults.assetsSubheading, commonResults.no, 19, 1, s"${user.commonExpectedResults.changeText} ${specificResults.assetsSubheadingHiddenText}", dummyHref)

          buttonCheck(commonResults.saveAndContinue)

          welshToggleCheck(user.isWelsh)

          s"should not display the following values" in {
            document().body().toString.contains(commonResults.nursery) shouldBe false
            document().body().toString.contains(commonResults.beneficialLoans) shouldBe false
            document().body().toString.contains(commonResults.educational) shouldBe false
            document().body().toString.contains(commonResults.incomeTaxPaid) shouldBe false
            document().body().toString.contains(commonResults.incurredCostsPaid) shouldBe false
            document().body().toString.contains(commonResults.nonTaxable) shouldBe false
            document().body().toString.contains(commonResults.taxableCosts) shouldBe false
            document().body().toString.contains(commonResults.vouchers) shouldBe false
            document().body().toString.contains(commonResults.nonCash) shouldBe false
            document().body().toString.contains(commonResults.otherBenefits) shouldBe false
            document().body().toString.contains(commonResults.assetTransfers) shouldBe false
            document().body().toString.contains(commonResults.companyCarAmount) shouldBe false
            document().body().toString.contains(commonResults.fuelForCompanyCarAmount) shouldBe false
            document().body().toString.contains(commonResults.accommodationAmount) shouldBe false
            document().body().toString.contains(commonResults.qualifyingRelocationCostsAmount) shouldBe false
            document().body().toString.contains(commonResults.nonQualifyingRelocationCostsAmount) shouldBe false
            document().body().toString.contains(commonResults.travelAndSubsistenceAmount) shouldBe false
            document().body().toString.contains(commonResults.personalCostsAmount) shouldBe false
            document().body().toString.contains(commonResults.entertainmentAmount) shouldBe false
            document().body().toString.contains(commonResults.telephoneAmount) shouldBe false
            document().body().toString.contains(commonResults.servicesProvidedAmount) shouldBe false
            document().body().toString.contains(commonResults.profSubscriptionsAmount) shouldBe false
            document().body().toString.contains(commonResults.otherServicesAmount) shouldBe false
            document().body().toString.contains(commonResults.medicalInsAmount) shouldBe false
            document().body().toString.contains(commonResults.nurseryAmount) shouldBe false
            document().body().toString.contains(commonResults.beneficialLoansAmount) shouldBe false
            document().body().toString.contains(commonResults.educationalAmount) shouldBe false
            document().body().toString.contains(commonResults.incomeTaxPaidAmount) shouldBe false
            document().body().toString.contains(commonResults.incurredCostsPaidAmount) shouldBe false
            document().body().toString.contains(commonResults.nonTaxableAmount) shouldBe false
            document().body().toString.contains(commonResults.taxableCostsAmount) shouldBe false
            document().body().toString.contains(commonResults.vouchersAmount) shouldBe false
            document().body().toString.contains(commonResults.nonCashAmount) shouldBe false
            document().body().toString.contains(commonResults.otherBenefitsAmount) shouldBe false
            document().body().toString.contains(commonResults.assetsAmount) shouldBe false
            document().body().toString.contains(commonResults.assetTransfersAmount) shouldBe false
          }
        }

        "return only the relevant data on the page when other certain data items are in CYA for EOY, customerData = true " +
          "to check help text isn't shown" which {

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

          def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
            EmploymentUserData(sessionId, mtditid, nino, defaultTaxYear - 1, "001", isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

          def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel =
            EmploymentCYAModel(
              EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc),
              Some(BenefitsViewModel(
                accommodationRelocationModel = Some(AccommodationRelocationModel(
                  accommodationRelocationQuestion = Some(true), accommodationQuestion = Some(true), accommodation = Some(3.00), qualifyingRelocationExpensesQuestion = Some(false), nonQualifyingRelocationExpensesQuestion = Some(false))), isUsingCustomerData = true
              ))
            )

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)), userRequest)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(defaultTaxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val specificResults = user.specificExpectedResults.get
          val commonResults = user.commonExpectedResults
          val dummyHref = s"/income-through-software/return/employment-income/${defaultTaxYear - 1}/check-employment-benefits?employmentId=001"

          titleCheck(specificResults.expectedTitle)
          h1Check(specificResults.expectedH1)
          captionCheck(commonResults.expectedCaption(defaultTaxYear - 1))

          changeAmountRowCheck(commonResults.benefitsReceived, commonResults.yes, 2, 1, s"${user.commonExpectedResults.changeText} ${specificResults.benefitsReceivedHiddenText}", receiveAnyBenefitsHref)

          textOnPageCheck(commonResults.vehicleHeader, fieldHeaderSelector(3))

          changeAmountRowCheck(commonResults.carSubheading, commonResults.no, 4, 1, s"${user.commonExpectedResults.changeText} ${specificResults.carSubheadingHiddenText}", carVanFuelBenefitsHref)

          textOnPageCheck(user.commonExpectedResults.accommodationHeader, fieldHeaderSelector(5))
          changeAmountRowCheck(commonResults.accommodationSubheading, commonResults.yes, 6, 1, s"${user.commonExpectedResults.changeText} ${specificResults.accommodationSubheadingHiddenText}", accommodationRelocationBenefitsHref)
          changeAmountRowCheck(commonResults.accommodation, commonResults.yes, 6, 2, s"${user.commonExpectedResults.changeText} ${specificResults.accommodationHiddenText}", livingAccommodationBenefitsHref)
          changeAmountRowCheck(commonResults.accommodationAmount, "£3", 6, 3, s"${user.commonExpectedResults.changeText} ${specificResults.accommodationAmountHiddenText}", livingAccommodationAmountBenefitsHref)
          changeAmountRowCheck(commonResults.qualifyingRelocationCosts, commonResults.no, 6, 4, s"${user.commonExpectedResults.changeText} ${specificResults.qualifyingRelocationCostsHiddenText}", qualifyingRelocationBenefitsHref)
          changeAmountRowCheck(commonResults.nonQualifyingRelocationCosts, commonResults.no, 6, 5, s"${user.commonExpectedResults.changeText} ${specificResults.nonQualifyingRelocationCostsHiddenText}", nonQualifyingRelocationBenefitsHref)

          textOnPageCheck(user.commonExpectedResults.travelHeader, fieldHeaderSelector(7))
          changeAmountRowCheck(commonResults.travelSubheading, commonResults.no, 8, 1, s"${user.commonExpectedResults.changeText} ${specificResults.travelSubheadingHiddenText}", travelEntertainmentBenefitsAmountHref)

          textOnPageCheck(user.commonExpectedResults.utilitiesHeader, fieldHeaderSelector(9))
          changeAmountRowCheck(commonResults.utilitiesSubheading, commonResults.no, 10, 1, s"${user.commonExpectedResults.changeText} ${specificResults.utilitiesSubheadingHiddenText}", utilitiesOrGeneralServicesBenefitsHref)

          textOnPageCheck(user.commonExpectedResults.medicalHeader, fieldHeaderSelector(11))
          changeAmountRowCheck(commonResults.medicalSubheading, commonResults.no, 12, 1, s"${user.commonExpectedResults.changeText} ${specificResults.medicalSubheadingHiddenText}", dummyHref)

          textOnPageCheck(user.commonExpectedResults.incomeTaxHeader, fieldHeaderSelector(13))
          changeAmountRowCheck(commonResults.incomeTaxSubheading, commonResults.no, 14, 1, s"${user.commonExpectedResults.changeText} ${specificResults.incomeTaxSubheadingHiddenText}", dummyHref)

          textOnPageCheck(user.commonExpectedResults.reimbursedHeader, fieldHeaderSelector(15))
          changeAmountRowCheck(commonResults.reimbursedSubheading, commonResults.no, 16, 1, s"${user.commonExpectedResults.changeText} ${specificResults.reimbursedSubheadingHiddenText}", dummyHref)

          textOnPageCheck(user.commonExpectedResults.assetsHeader, fieldHeaderSelector(17))
          changeAmountRowCheck(commonResults.assetsSubheading, commonResults.no, 18, 1, s"${user.commonExpectedResults.changeText} ${specificResults.assetsSubheadingHiddenText}", dummyHref)

          buttonCheck(commonResults.saveAndContinue)

          welshToggleCheck(user.isWelsh)

          s"should not display the following values" in {
            document().body().toString.contains(specificResults.expectedP1) shouldBe false
            document().body().toString.contains(commonResults.companyCar) shouldBe false
            document().body().toString.contains(commonResults.fuelForCompanyCar) shouldBe false
            document().body().toString.contains(commonResults.companyVan) shouldBe false
            document().body().toString.contains(commonResults.fuelForCompanyVan) shouldBe false
            document().body().toString.contains(commonResults.mileageBenefit) shouldBe false
            document().body().toString.contains(commonResults.travelAndSubsistence) shouldBe false
            document().body().toString.contains(commonResults.personalCosts) shouldBe false
            document().body().toString.contains(commonResults.entertainment) shouldBe false
            document().body().toString.contains(commonResults.telephone) shouldBe false
            document().body().toString.contains(commonResults.servicesProvided) shouldBe false
            document().body().toString.contains(commonResults.profSubscriptions) shouldBe false
            document().body().toString.contains(commonResults.otherServices) shouldBe false
            document().body().toString.contains(commonResults.nursery) shouldBe false
            document().body().toString.contains(commonResults.beneficialLoans) shouldBe false
            document().body().toString.contains(commonResults.educational) shouldBe false
            document().body().toString.contains(commonResults.incomeTaxPaid) shouldBe false
            document().body().toString.contains(commonResults.incurredCostsPaid) shouldBe false
            document().body().toString.contains(commonResults.nonTaxable) shouldBe false
            document().body().toString.contains(commonResults.taxableCosts) shouldBe false
            document().body().toString.contains(commonResults.vouchers) shouldBe false
            document().body().toString.contains(commonResults.nonCash) shouldBe false
            document().body().toString.contains(commonResults.otherBenefits) shouldBe false
            document().body().toString.contains(commonResults.assetTransfers) shouldBe false
            document().body().toString.contains(commonResults.companyCarAmount) shouldBe false
            document().body().toString.contains(commonResults.fuelForCompanyCarAmount) shouldBe false
            document().body().toString.contains(commonResults.companyVanAmount) shouldBe false
            document().body().toString.contains(commonResults.fuelForCompanyVanAmount) shouldBe false
            document().body().toString.contains(commonResults.mileageBenefitAmount) shouldBe false
            document().body().toString.contains(commonResults.travelAndSubsistenceAmount) shouldBe false
            document().body().toString.contains(commonResults.personalCostsAmount) shouldBe false
            document().body().toString.contains(commonResults.entertainmentAmount) shouldBe false
            document().body().toString.contains(commonResults.telephoneAmount) shouldBe false
            document().body().toString.contains(commonResults.servicesProvidedAmount) shouldBe false
            document().body().toString.contains(commonResults.profSubscriptionsAmount) shouldBe false
            document().body().toString.contains(commonResults.otherServicesAmount) shouldBe false
            document().body().toString.contains(commonResults.medicalInsAmount) shouldBe false
            document().body().toString.contains(commonResults.nurseryAmount) shouldBe false
            document().body().toString.contains(commonResults.beneficialLoansAmount) shouldBe false
            document().body().toString.contains(commonResults.educationalAmount) shouldBe false
            document().body().toString.contains(commonResults.incomeTaxPaidAmount) shouldBe false
            document().body().toString.contains(commonResults.incurredCostsPaidAmount) shouldBe false
            document().body().toString.contains(commonResults.nonTaxableAmount) shouldBe false
            document().body().toString.contains(commonResults.taxableCostsAmount) shouldBe false
            document().body().toString.contains(commonResults.vouchersAmount) shouldBe false
            document().body().toString.contains(commonResults.nonCashAmount) shouldBe false
            document().body().toString.contains(commonResults.otherBenefitsAmount) shouldBe false
            document().body().toString.contains(commonResults.assetsAmount) shouldBe false
            document().body().toString.contains(commonResults.assetTransfersAmount) shouldBe false
          }
        }

        "return a page with only the benefits received subheading when its EOY and only the benefits question answered as no" which {

          def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
            EmploymentUserData(sessionId, mtditid, nino, defaultTaxYear - 1, "001", isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

          def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel =
            EmploymentCYAModel(
              EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc),
              Some(BenefitsViewModel(
                isUsingCustomerData = false, isBenefitsReceived = false
              ))
            )

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)
          val commonResults = user.commonExpectedResults

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear - 1)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(commonResults.expectedCaption(defaultTaxYear - 1))
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, Selectors.p1)

          changeAmountRowCheck(commonResults.benefitsReceived, commonResults.no, 3, 1, s"${user.commonExpectedResults.changeText} ${user.specificExpectedResults.get.benefitsReceivedHiddenText}", receiveAnyBenefitsHref)

          buttonCheck(user.commonExpectedResults.saveAndContinue)

          welshToggleCheck(user.isWelsh)

          s"should not display the following values" in {
            document().body().toString.contains(commonResults.carSubheading) shouldBe false
            document().body().toString.contains(commonResults.accommodationSubheading) shouldBe false
            document().body().toString.contains(commonResults.travelSubheading) shouldBe false
            document().body().toString.contains(commonResults.utilitiesSubheading) shouldBe false
            document().body().toString.contains(commonResults.medicalSubheading) shouldBe false
            document().body().toString.contains(commonResults.incomeTaxSubheading) shouldBe false
            document().body().toString.contains(commonResults.reimbursedSubheading) shouldBe false
            document().body().toString.contains(commonResults.assetsSubheading) shouldBe false
          }
        }

        "redirect to the Did your client receive any benefits page when its EOY and theres no benefits model in the session data" in {

          def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
            EmploymentUserData(sessionId, mtditid, nino, defaultTaxYear - 1, "001", isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

          def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel =
            EmploymentCYAModel(
              EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc),
              None
            )

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear - 1)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/benefits/company-benefits?employmentId=001")

        }

        "redirect to the Did your client receive any benefits page when its EOY and theres no benefits model in the mongo data" in {

          def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
            EmploymentUserData(sessionId, mtditid, nino, defaultTaxYear - 1, "001", isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(None)))), nino, defaultTaxYear)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/benefits/company-benefits?employmentId=001")

        }

        "redirect to overview page when theres no benefits and in year" in {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, defaultTaxYear)
            urlGet(url(), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }

        "return only the relevant data on the page when only certain data items are in mongo and in year" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(filteredBenefits)))), nino, defaultTaxYear)
            urlGet(url(), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption())
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, Selectors.p1)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2(), Selectors.p2)

          textOnPageCheck(user.commonExpectedResults.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(user.commonExpectedResults.companyVan, fieldNameSelector(5, 1))
          textOnPageCheck("£3", fieldAmountSelector(5, 1))
          textOnPageCheck(user.commonExpectedResults.fuelForCompanyVan, fieldNameSelector(5, 2))
          textOnPageCheck("£4", fieldAmountSelector(5, 2))
          textOnPageCheck(user.commonExpectedResults.mileageBenefit, fieldNameSelector(5, 3))
          textOnPageCheck("£5", fieldAmountSelector(5, 3))

          welshToggleCheck(user.isWelsh)

          s"should not display the following values" in {

            document().body().toString.contains(user.commonExpectedResults.accommodationHeader) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.qualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.nonQualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.travelHeader) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.travelAndSubsistence) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.personalCosts) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.entertainment) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.utilitiesHeader) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.telephone) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.servicesProvided) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.profSubscriptions) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.otherServices) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.medicalHeader) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.nursery) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.beneficialLoans) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.educational) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.incomeTaxHeader) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.incomeTaxPaid) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.incurredCostsPaid) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.reimbursedHeader) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.nonTaxable) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.taxableCosts) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.vouchers) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.nonCash) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.otherBenefits) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.assetsHeader) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.assetTransfers) shouldBe false
          }
        }

        "render Unauthorised user error page when the user is unauthorized" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(url(), welsh = user.isWelsh)
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }


  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return a redirect when in year" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
            urlPost(url(), body = "{}", welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has a url of overview page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
          }
        }

        "return internal server error page whilst not implemented" in {
          val employmentData: EmploymentCYAModel = {
            employmentUserData.employment.copy(employmentDetails = employmentUserData.employment.employmentDetails.copy(
              employerRef = Some(
                "123/12345"
              ),
              startDate = Some("2020-11-11"),
              taxablePayToDate = Some(55.99),
              totalTaxToDate = Some(3453453.00),
              currentDataIsHmrcHeld = false
            ))
          }
          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData.copy(employment = employmentData).copy(employmentId = "001"), userRequest)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear - 1)
            urlPost(url(taxYear - 1), body = "{}", welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          result.status shouldBe INTERNAL_SERVER_ERROR
        }

        "return a redirect to show method when at end of year" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear - 1)
            urlPost(url(taxYear - 1), body = "{}", welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has a url of benefits show method" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001")
          }
        }
      }
    }
  }
}