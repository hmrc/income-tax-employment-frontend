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

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentBenefitsBuilder.anEmploymentBenefits
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import controllers.benefits.routes.ReceiveAnyBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import models.benefits.{AccommodationRelocationModel, Benefits, BenefitsViewModel}
import models.employment.EmploymentBenefits
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CheckYourBenefitsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper {

  private val defaultTaxYear = 2022
  private val employmentId = "001"
  private val dummyHref = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/check-employment-benefits?employmentId=$employmentId"

  def url(taxYear: Int = defaultTaxYear): String = s"$appUrl/$taxYear/check-employment-benefits?employmentId=$employmentId"

  lazy val filteredBenefits: Some[EmploymentBenefits] = Some(EmploymentBenefits(
    submittedOn = "2020-02-12",
    benefits = Some(Benefits(
      van = Some(3.00),
      vanFuel = Some(4.00),
      mileage = Some(5.00),
    ))
  ))

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
    val nonQualifyingRelocationCostsAmountHiddenText: String = "Change the amount for non-qualifying relocation costs as an employment benefit your client got from this company"
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
    val nonQualifyingRelocationCostsAmountHiddenText: String = "Change the amount for non-qualifying relocation costs as an employment benefit your client got from this company"
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
    val receiveAnyBenefitsHref = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/company-benefits?employmentId=$employmentId"
    val carVanFuelBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/car-van-fuel?employmentId=$employmentId"
    val companyCarHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/company-car?employmentId=$employmentId"
    val companyCarAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/company-car-amount?employmentId=$employmentId"
    val companyCarFuelBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/car-fuel?employmentId=$employmentId"
    val carFuelAmountBenefitsHref = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/car-fuel-amount?employmentId=$employmentId"
    val mileageAmountBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/mileage-amount?employmentId=$employmentId"
    val receivedOwnCarMileageBenefitHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/mileage?employmentId=$employmentId"
    val companyVanBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/company-van?employmentId=$employmentId"
    val companyVanFuelBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/van-fuel?employmentId=$employmentId"
    val companyVanBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/company-van-amount?employmentId=$employmentId"
    val companyVanFuelBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/van-fuel-amount?employmentId=$employmentId"
    val accommodationRelocationBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/accommodation-relocation?employmentId=$employmentId"
    val livingAccommodationBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/living-accommodation?employmentId=$employmentId"
    val livingAccommodationAmountBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/living-accommodation-amount?employmentId=$employmentId"
    val qualifyingRelocationBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/qualifying-relocation?employmentId=$employmentId"
    val qualifyingRelocationBenefitsAmountHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/qualifying-relocation-amount?employmentId=$employmentId"
    val nonQualifyingRelocationBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/non-qualifying-relocation?employmentId=$employmentId"
    val nonQualifyingRelocationAmountBenefitsHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/non-qualifying-relocation-amount?employmentId=$employmentId"
    val travelEntertainmentBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/travel-entertainment?employmentId=$employmentId"
    val travelOrSubsistenceBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/travel-subsistence?employmentId=$employmentId"
    val travelOrSubsistenceBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/travel-subsistence-amount?employmentId=$employmentId"
    val incidentalCostsBenefitsAmountHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/incidental-overnight-costs-amount?employmentId=$employmentId"
    val utilitiesOrGeneralServicesBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/utility-general-service?employmentId=$employmentId"
    val entertainingBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/entertainment-expenses?employmentId=$employmentId"
    val telephoneBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/telephone?employmentId=$employmentId"
    val otherServicesBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/other-services-amount?employmentId=$employmentId"
    val incidentalOvernightCostEmploymentBenefitsHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/incidental-overnight-costs?employmentId=$employmentId"
    val otherServicesBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/other-services?employmentId=$employmentId"
    val entertainmentAmountBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/entertainment-expenses-amount?employmentId=$employmentId"
    val employerProvidedServicesBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/employer-provided-services?employmentId=$employmentId"
    val telephoneEmploymentBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/telephone-amount?employmentId=$employmentId"
    val educationalServicesAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/educational-services-amount?employmentId=$employmentId"
    val employerProvidedServicesBenefitsAmountHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/employer-provided-services-amount?employmentId=$employmentId"
    val professionalSubscriptionsBenefitsHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/professional-fees-or-subscriptions?employmentId=$employmentId"
    val professionalSubscriptionsBenefitsAmountHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/professional-fees-or-subscriptions-amount?employmentId=$employmentId"
    val medicalChildcareEducationHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/medical-dental-childcare-education-loans?employmentId=$employmentId"
    val medicalDentalHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/medical-dental?employmentId=$employmentId"
    val childcareBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/childcare?employmentId=$employmentId"
    val childcareBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/childcare-amount?employmentId=$employmentId"
    val beneficialLoansBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/beneficial-loans?employmentId=$employmentId"
    val medicalOrDentalBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/medical-dental-amount?employmentId=$employmentId"
    val beneficialLoansAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/beneficial-loans-amount?employmentId=$employmentId"
    val incomeTaxOrIncurredCostsBenefitsHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/employer-income-tax-or-incurred-costs?employmentId=$employmentId"
    val incomeTaxBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/employer-income-tax-amount?employmentId=001"
    val incomeTaxBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/employer-income-tax?employmentId=$employmentId"
    val educationServiceBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/educational-services?employmentId=$employmentId"
    val incurredCostsBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/incurred-costs?employmentId=$employmentId"
    val incurredCostsBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/incurred-costs-amount?employmentId=$employmentId"
    val reimbursedCostsVouchersAndNonCashBenefitsHref: String =
      s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/reimbursed-costs-vouchers-non-cash-benefits?employmentId=$employmentId"
    val nonTaxableCostsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/non-taxable-costs?employmentId=$employmentId"
    val taxableCostsBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/taxable-costs?employmentId=$employmentId"
    val taxableCostsReimbursedByEmployerAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/taxable-costs-amount?employmentId=$employmentId"
    val nonTaxableCostsBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/non-taxable-costs-amount?employmentId=$employmentId"
    val vouchersBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/vouchers-or-credit-cards?employmentId=$employmentId"
    val vouchersBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/vouchers-or-credit-cards-amount?employmentId=$employmentId"
    val nonCashBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/non-cash-benefits?employmentId=001"
    val nonCashBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/non-cash-benefits-amount?employmentId=$employmentId"
    val otherBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/other-benefits-amount?employmentId=001"
    val otherBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/other-benefits?employmentId=001"
    val assetsOrAssetTransfersBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/assets?employmentId=$employmentId"
    val assetsBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/assets-available-for-use?employmentId=001"
    val assetsBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/assets-available-for-use-amount?employmentId=001"
    val assetTransfersBenefitsHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/assets-to-keep?employmentId=001"
    val assetTransfersBenefitsAmountHref: String = s"/update-and-submit-income-tax-return/employment-income/${defaultTaxYear - 1}/benefits/assets-to-keep-amount?employmentId=001"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
    import Hrefs._
    import Selectors._
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        val common = user.commonExpectedResults
        val specific = user.specificExpectedResults.get
        "return a fully populated page when all the fields are populated for in year" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            urlGet(url(), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption())
          textOnPageCheck(specific.expectedP1, Selectors.p1)
          textOnPageCheck(specific.expectedP2(), Selectors.p2)
          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(common.companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(common.fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(common.companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(common.fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(common.mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(common.accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(common.qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(common.nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(common.travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(common.travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(common.personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(common.entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(common.telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(common.servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(common.profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(common.otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(common.medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(common.medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(common.nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(common.educational, fieldNameSelector(13, 3))
          textOnPageCheck("£19", fieldAmountSelector(13, 3))
          textOnPageCheck(common.beneficialLoans, fieldNameSelector(13, 4))
          textOnPageCheck("£18", fieldAmountSelector(13, 4))
          textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(common.incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(common.incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(common.nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(common.taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(common.vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(common.nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(common.otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(common.assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(common.assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(common.assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))
          welshToggleCheck(user.isWelsh)
        }

        "return a fully populated page when all the fields are populated when at the end of the year" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(defaultTaxYear - 1))
          textOnPageCheck(specific.expectedP1, Selectors.p1)

          changeAmountRowCheck(common.benefitsReceived, common.yes, 3, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}", receiveAnyBenefitsHref)

          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(4))

          changeAmountRowCheck(common.carSubheading, common.yes, 5, 1, s"${common.changeText} ${specific.carSubheadingHiddenText}", carVanFuelBenefitsHref)
          changeAmountRowCheck(common.companyCar, common.yes, 5, 2, s"${common.changeText} ${specific.companyCarHiddenText}", companyCarHref)
          changeAmountRowCheck(common.companyCarAmount, "£1.23", 5, 3, s"${common.changeText} ${specific.companyCarAmountHiddenText}", companyCarAmountHref)
          changeAmountRowCheck(common.fuelForCompanyCar, common.yes, 5, 4, s"${common.changeText} ${specific.fuelForCompanyCarHiddenText}", companyCarFuelBenefitsHref)
          changeAmountRowCheck(common.fuelForCompanyCarAmount, "£2", 5, 5, s"${common.changeText} ${specific.fuelForCompanyCarAmountHiddenText}", carFuelAmountBenefitsHref)
          changeAmountRowCheck(common.companyVan, common.yes, 5, 6, s"${common.changeText} ${specific.companyVanHiddenText}", companyVanBenefitsHref)
          changeAmountRowCheck(common.fuelForCompanyVan, common.yes, 5, 8, s"${common.changeText} ${specific.fuelForCompanyVanHiddenText}", companyVanFuelBenefitsHref)
          changeAmountRowCheck(common.companyVanAmount, "£3", 5, 7, s"${common.changeText} ${specific.companyVanAmountHiddenText}", companyVanBenefitsAmountHref)
          changeAmountRowCheck(common.fuelForCompanyVanAmount, "£4", 5, 9, s"${common.changeText} ${specific.fuelForCompanyVanAmountHiddenText}", companyVanFuelBenefitsAmountHref)
          changeAmountRowCheck(common.mileageBenefit, common.yes, 5, 10, s"${common.changeText} ${specific.mileageBenefitHiddenText}", receivedOwnCarMileageBenefitHref)
          changeAmountRowCheck(common.mileageBenefitAmount, "£5", 5, 11, s"${common.changeText} ${specific.mileageBenefitAmountHiddenText}", mileageAmountBenefitsHref)

          textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(6))
          changeAmountRowCheck(common.accommodationSubheading, common.yes, 7, 1, s"${common.changeText} ${specific.accommodationSubheadingHiddenText}", accommodationRelocationBenefitsHref)
          changeAmountRowCheck(common.accommodation, common.yes, 7, 2, s"${common.changeText} ${specific.accommodationHiddenText}", livingAccommodationBenefitsHref)
          changeAmountRowCheck(common.accommodationAmount, "£6", 7, 3, s"${common.changeText} ${specific.accommodationAmountHiddenText}", livingAccommodationAmountBenefitsHref)
          changeAmountRowCheck(common.qualifyingRelocationCosts, common.yes, 7, 4, s"${common.changeText} ${specific.qualifyingRelocationCostsHiddenText}", qualifyingRelocationBenefitsHref)
          changeAmountRowCheck(common.qualifyingRelocationCostsAmount, "£7", 7, 5, s"${common.changeText} ${specific.qualifyingRelocationCostsAmountHiddenText}",
            qualifyingRelocationBenefitsAmountHref)
          changeAmountRowCheck(common.nonQualifyingRelocationCosts, common.yes, 7, 6, s"${common.changeText} ${specific.nonQualifyingRelocationCostsHiddenText}", nonQualifyingRelocationBenefitsHref)
          changeAmountRowCheck(common.nonQualifyingRelocationCostsAmount, "£8", 7, 7, s"${common.changeText} ${specific.nonQualifyingRelocationCostsAmountHiddenText}",
            nonQualifyingRelocationAmountBenefitsHref)

          textOnPageCheck(common.travelHeader, fieldHeaderSelector(8))
          changeAmountRowCheck(common.travelSubheading, common.yes, 9, 1, s"${common.changeText} ${specific.travelSubheadingHiddenText}", travelEntertainmentBenefitsAmountHref)
          changeAmountRowCheck(common.travelAndSubsistence, common.yes, 9, 2, s"${common.changeText} ${specific.travelAndSubsistenceHiddenText}", travelOrSubsistenceBenefitsHref)
          changeAmountRowCheck(common.travelAndSubsistenceAmount, "£9", 9, 3, s"${common.changeText} ${specific.travelAndSubsistenceAmountHiddenText}", travelOrSubsistenceBenefitsAmountHref)
          changeAmountRowCheck(common.personalCosts, common.yes, 9, 4, s"${common.changeText} ${specific.personalCostsHiddenText}", incidentalOvernightCostEmploymentBenefitsHref)
          changeAmountRowCheck(common.personalCostsAmount, "£10", 9, 5, s"${common.changeText} ${specific.personalCostsAmountHiddenText}", incidentalCostsBenefitsAmountHref)
          changeAmountRowCheck(common.entertainment, common.yes, 9, 6, s"${common.changeText} ${specific.entertainmentHiddenText}", entertainingBenefitsHref)
          changeAmountRowCheck(common.entertainmentAmount, "£11", 9, 7, s"${common.changeText} ${specific.entertainmentAmountHiddenText}", entertainmentAmountBenefitsHref)

          textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(10))
          changeAmountRowCheck(common.utilitiesSubheading, common.yes, 11, 1, s"${common.changeText} ${specific.utilitiesSubheadingHiddenText}", utilitiesOrGeneralServicesBenefitsHref)
          changeAmountRowCheck(common.telephone, common.yes, 11, 2, s"${common.changeText} ${specific.telephoneHiddenText}", telephoneBenefitsHref)
          changeAmountRowCheck(common.telephoneAmount, "£12", 11, 3, s"${common.changeText} ${specific.telephoneAmountHiddenText}", telephoneEmploymentBenefitsAmountHref)
          changeAmountRowCheck(common.servicesProvided, common.yes, 11, 4, s"${common.changeText} ${specific.servicesProvidedHiddenText}", employerProvidedServicesBenefitsHref)
          changeAmountRowCheck(common.servicesProvidedAmount, "£13", 11, 5, s"${common.changeText} ${specific.servicesProvidedAmountHiddenText}", employerProvidedServicesBenefitsAmountHref)
          changeAmountRowCheck(common.profSubscriptions, common.yes, 11, 6, s"${common.changeText} ${specific.profSubscriptionsHiddenText}", professionalSubscriptionsBenefitsHref)
          changeAmountRowCheck(common.profSubscriptionsAmount, "£14", 11, 7, s"${common.changeText} ${specific.profSubscriptionsAmountHiddenText}", professionalSubscriptionsBenefitsAmountHref)
          changeAmountRowCheck(common.otherServices, common.yes, 11, 8, s"${common.changeText} ${specific.otherServicesHiddenText}", otherServicesBenefitsHref)
          changeAmountRowCheck(common.otherServicesAmount, "£15", 11, 9, s"${common.changeText} ${specific.otherServicesAmountHiddenText}", otherServicesBenefitsAmountHref)

          textOnPageCheck(common.medicalHeader, fieldHeaderSelector(12))
          changeAmountRowCheck(common.medicalSubheading, common.yes, 13, 1, s"${common.changeText} ${specific.medicalSubheadingHiddenText}", medicalChildcareEducationHref)
          changeAmountRowCheck(common.medicalIns, common.yes, 13, 2, s"${common.changeText} ${specific.medicalInsHiddenText}", medicalDentalHref)
          changeAmountRowCheck(common.medicalInsAmount, "£16", 13, 3, s"${common.changeText} ${specific.medicalInsAmountHiddenText}", medicalOrDentalBenefitsAmountHref)
          changeAmountRowCheck(common.nursery, common.yes, 13, 4, s"${common.changeText} ${specific.nurseryHiddenText}", childcareBenefitsHref)
          changeAmountRowCheck(common.nurseryAmount, "£17", 13, 5, s"${common.changeText} ${specific.nurseryAmountHiddenText}", childcareBenefitsAmountHref)
          changeAmountRowCheck(common.educational, common.yes, 13, 6, s"${common.changeText} ${specific.educationalHiddenText}", educationServiceBenefitsHref)
          changeAmountRowCheck(common.educationalAmount, "£19", 13, 7, s"${common.changeText} ${specific.educationalAmountHiddenText}", educationalServicesAmountHref)
          changeAmountRowCheck(common.beneficialLoans, common.yes, 13, 8, s"${common.changeText} ${specific.beneficialLoansHiddenText}", beneficialLoansBenefitsHref)
          changeAmountRowCheck(common.beneficialLoansAmount, "£18", 13, 9, s"${common.changeText} ${specific.beneficialLoansAmountHiddenText}", beneficialLoansAmountHref)

          textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(14))
          changeAmountRowCheck(common.incomeTaxSubheading, common.yes, 15, 1, s"${common.changeText} ${specific.incomeTaxSubheadingHiddenText}", incomeTaxOrIncurredCostsBenefitsHref)
          changeAmountRowCheck(common.incomeTaxPaid, common.yes, 15, 2, s"${common.changeText} ${specific.incomeTaxPaidHiddenText}", incomeTaxBenefitsHref)
          changeAmountRowCheck(common.incomeTaxPaidAmount, "£20", 15, 3, s"${common.changeText} ${specific.incomeTaxPaidAmountHiddenText}", incomeTaxBenefitsAmountHref)
          changeAmountRowCheck(common.incurredCostsPaid, common.yes, 15, 4, s"${common.changeText} ${specific.incurredCostsPaidHiddenText}", incurredCostsBenefitsHref)
          changeAmountRowCheck(common.incurredCostsPaidAmount, "£21", 15, 5, s"${common.changeText} ${specific.incurredCostsPaidAmountHiddenText}", incurredCostsBenefitsAmountHref)

          textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(16))
          changeAmountRowCheck(common.reimbursedSubheading, common.yes, 17, 1, s"${common.changeText} ${specific.reimbursedSubheadingHiddenText}", reimbursedCostsVouchersAndNonCashBenefitsHref)

          changeAmountRowCheck(common.nonTaxable, common.yes, 17, 2, s"${common.changeText} ${specific.nonTaxableHiddenText}", nonTaxableCostsHref)
          changeAmountRowCheck(common.nonTaxableAmount, "£22", 17, 3, s"${common.changeText} ${specific.nonTaxableAmountHiddenText}", nonTaxableCostsBenefitsAmountHref)

          changeAmountRowCheck(common.taxableCosts, common.yes, 17, 4, s"${common.changeText} ${specific.taxableCostsHiddenText}", taxableCostsBenefitsHref)
          changeAmountRowCheck(common.taxableCostsAmount, "£23", 17, 5, s"${common.changeText} ${specific.taxableCostsAmountHiddenText}", taxableCostsReimbursedByEmployerAmountHref)
          changeAmountRowCheck(common.vouchers, common.yes, 17, 6, s"${common.changeText} ${specific.vouchersHiddenText}", vouchersBenefitsHref)
          changeAmountRowCheck(common.vouchersAmount, "£24", 17, 7, s"${common.changeText} ${specific.vouchersAmountHiddenText}", vouchersBenefitsAmountHref)
          changeAmountRowCheck(common.nonCash, common.yes, 17, 8, s"${common.changeText} ${specific.nonCashHiddenText}", nonCashBenefitsHref)
          changeAmountRowCheck(common.nonCashAmount, "£25", 17, 9, s"${common.changeText} ${specific.nonCashAmountHiddenText}", nonCashBenefitsAmountHref)
          changeAmountRowCheck(common.otherBenefits, common.yes, 17, 10, s"${common.changeText} ${specific.otherBenefitsHiddenText}", otherBenefitsHref)
          changeAmountRowCheck(common.otherBenefitsAmount, "£26", 17, 11, s"${common.changeText} ${specific.otherBenefitsAmountHiddenText}", otherBenefitsAmountHref)

          textOnPageCheck(common.assetsHeader, fieldHeaderSelector(18))
          changeAmountRowCheck(common.assetsSubheading, common.yes, 19, 1, s"${common.changeText} ${specific.assetsSubheadingHiddenText}", assetsOrAssetTransfersBenefitsHref)
          changeAmountRowCheck(common.assets, common.yes, 19, 2, s"${common.changeText} ${specific.assetsHiddenText}", assetsBenefitsHref)
          changeAmountRowCheck(common.assetsAmount, "£27", 19, 3, s"${common.changeText} ${specific.assetsAmountHiddenText}", assetsBenefitsAmountHref)
          changeAmountRowCheck(common.assetTransfers, common.yes, 19, 4, s"${common.changeText} ${specific.assetTransfersHiddenText}", assetTransfersBenefitsHref)
          changeAmountRowCheck(common.assetTransfersAmount, "£280000", 19, 5, s"${common.changeText} ${specific.assetTransfersAmountHiddenText}", assetTransfersBenefitsAmountHref)

          buttonCheck(common.saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }

        "return only the relevant data on the page when only certain data items are in mongo for EOY" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val employmentSources = Seq(anEmploymentSource.copy(employmentBenefits = filteredBenefits))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = employmentSources))), nino, defaultTaxYear - 1)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(defaultTaxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(defaultTaxYear - 1))
          textOnPageCheck(specific.expectedP1, Selectors.p1)

          changeAmountRowCheck(common.benefitsReceived, common.yes, 3, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}", receiveAnyBenefitsHref)

          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(4))

          changeAmountRowCheck(common.carSubheading, common.yes, 5, 1, s"${common.changeText} ${specific.carSubheadingHiddenText}", carVanFuelBenefitsHref)
          changeAmountRowCheck(common.companyCar, common.no, 5, 2, s"${common.changeText} ${specific.companyCarHiddenText}", companyCarHref)
          changeAmountRowCheck(common.fuelForCompanyCar, common.no, 5, 3, s"${common.changeText} ${specific.fuelForCompanyCarHiddenText}", companyCarFuelBenefitsHref)
          changeAmountRowCheck(common.companyVan, common.yes, 5, 4, s"${common.changeText} ${specific.companyVanHiddenText}", companyVanBenefitsHref)
          changeAmountRowCheck(common.fuelForCompanyVan, common.yes, 5, 6, s"${common.changeText} ${specific.fuelForCompanyVanHiddenText}", companyVanFuelBenefitsHref)
          changeAmountRowCheck(common.companyVanAmount, "£3", 5, 5, s"${common.changeText} ${specific.companyVanAmountHiddenText}", companyVanBenefitsAmountHref)
          changeAmountRowCheck(common.fuelForCompanyVanAmount, "£4", 5, 7, s"${common.changeText} ${specific.fuelForCompanyVanAmountHiddenText}", companyVanFuelBenefitsAmountHref)
          changeAmountRowCheck(common.mileageBenefit, common.yes, 5, 8, s"${common.changeText} ${specific.mileageBenefitHiddenText}", receivedOwnCarMileageBenefitHref)
          changeAmountRowCheck(common.mileageBenefitAmount, "£5", 5, 9, s"${common.changeText} ${specific.mileageBenefitAmountHiddenText}", mileageAmountBenefitsHref)

          textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(6))
          changeAmountRowCheck(common.accommodationSubheading, common.no, 7, 1, s"${common.changeText} ${specific.accommodationSubheadingHiddenText}", accommodationRelocationBenefitsHref)

          textOnPageCheck(common.travelHeader, fieldHeaderSelector(8))
          changeAmountRowCheck(common.travelSubheading, common.no, 9, 1, s"${common.changeText} ${specific.travelSubheadingHiddenText}", travelEntertainmentBenefitsAmountHref)

          textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(10))
          changeAmountRowCheck(common.utilitiesSubheading, common.no, 11, 1, s"${common.changeText} ${specific.utilitiesSubheadingHiddenText}", utilitiesOrGeneralServicesBenefitsHref)

          textOnPageCheck(common.medicalHeader, fieldHeaderSelector(12))
          changeAmountRowCheck(common.medicalSubheading, common.no, 13, 1, s"${common.changeText} ${specific.medicalSubheadingHiddenText}", medicalChildcareEducationHref)

          textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(14))
          changeAmountRowCheck(common.incomeTaxSubheading, common.no, 15, 1, s"${common.changeText} ${specific.incomeTaxSubheadingHiddenText}", incomeTaxOrIncurredCostsBenefitsHref)

          textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(16))
          changeAmountRowCheck(common.reimbursedSubheading, common.no, 17, 1, s"${common.changeText} ${specific.reimbursedSubheadingHiddenText}", reimbursedCostsVouchersAndNonCashBenefitsHref)

          textOnPageCheck(common.assetsHeader, fieldHeaderSelector(18))
          changeAmountRowCheck(common.assetsSubheading, common.no, 19, 1, s"${common.changeText} ${specific.assetsSubheadingHiddenText}", assetsOrAssetTransfersBenefitsHref)

          buttonCheck(common.saveAndContinue)

          welshToggleCheck(user.isWelsh)

          s"should not display the following values" in {
            document().body().toString.contains(common.assetTransfers) shouldBe false
            document().body().toString.contains(common.companyCarAmount) shouldBe false
            document().body().toString.contains(common.fuelForCompanyCarAmount) shouldBe false
            document().body().toString.contains(common.accommodationAmount) shouldBe false
            document().body().toString.contains(common.qualifyingRelocationCostsAmount) shouldBe false
            document().body().toString.contains(common.nonQualifyingRelocationCostsAmount) shouldBe false
            document().body().toString.contains(common.travelAndSubsistenceAmount) shouldBe false
            document().body().toString.contains(common.personalCostsAmount) shouldBe false
            document().body().toString.contains(common.entertainmentAmount) shouldBe false
            document().body().toString.contains(common.telephoneAmount) shouldBe false
            document().body().toString.contains(common.servicesProvidedAmount) shouldBe false
            document().body().toString.contains(common.profSubscriptionsAmount) shouldBe false
            document().body().toString.contains(common.otherServicesAmount) shouldBe false
            document().body().toString.contains(common.medicalInsAmount) shouldBe false
            document().body().toString.contains(common.nurseryAmount) shouldBe false
            document().body().toString.contains(common.beneficialLoansAmount) shouldBe false
            document().body().toString.contains(common.educationalAmount) shouldBe false
            document().body().toString.contains(common.incomeTaxPaidAmount) shouldBe false
            document().body().toString.contains(common.incurredCostsPaidAmount) shouldBe false
            document().body().toString.contains(common.nonTaxableAmount) shouldBe false
            document().body().toString.contains(common.taxableCostsAmount) shouldBe false
            document().body().toString.contains(common.vouchersAmount) shouldBe false
            document().body().toString.contains(common.nonCashAmount) shouldBe false
            document().body().toString.contains(common.otherBenefitsAmount) shouldBe false
            document().body().toString.contains(common.assetsAmount) shouldBe false
            document().body().toString.contains(common.assetTransfersAmount) shouldBe false
          }
        }

        "return only the relevant data on the page when other certain data items are in CYA for EOY, customerData = true " +
          "to check help text isn't shown" which {
          def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
            EmploymentUserData(sessionId, mtditid, nino, defaultTaxYear - 1, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

          def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel =
            EmploymentCYAModel(
              EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc),
              Some(BenefitsViewModel(
                accommodationRelocationModel = Some(AccommodationRelocationModel(
                  sectionQuestion = Some(true),
                  accommodationQuestion = Some(true),
                  accommodation = Some(3.00),
                  qualifyingRelocationExpensesQuestion = Some(false),
                  nonQualifyingRelocationExpensesQuestion = Some(false))),
                isUsingCustomerData = true
              ))
            )

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)), aUserRequest)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(defaultTaxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(defaultTaxYear - 1))

          changeAmountRowCheck(common.benefitsReceived, common.yes, 2, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}", receiveAnyBenefitsHref)

          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(3))

          changeAmountRowCheck(common.carSubheading, common.no, 4, 1, s"${common.changeText} ${specific.carSubheadingHiddenText}", carVanFuelBenefitsHref)

          textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(5))
          changeAmountRowCheck(common.accommodationSubheading, common.yes, 6, 1, s"${common.changeText} ${specific.accommodationSubheadingHiddenText}", accommodationRelocationBenefitsHref)
          changeAmountRowCheck(common.accommodation, common.yes, 6, 2, s"${common.changeText} ${specific.accommodationHiddenText}", livingAccommodationBenefitsHref)
          changeAmountRowCheck(common.accommodationAmount, "£3", 6, 3, s"${common.changeText} ${specific.accommodationAmountHiddenText}", livingAccommodationAmountBenefitsHref)
          changeAmountRowCheck(common.qualifyingRelocationCosts, common.no, 6, 4, s"${common.changeText} ${specific.qualifyingRelocationCostsHiddenText}", qualifyingRelocationBenefitsHref)
          changeAmountRowCheck(common.nonQualifyingRelocationCosts, common.no, 6, 5, s"${common.changeText} ${specific.nonQualifyingRelocationCostsHiddenText}", nonQualifyingRelocationBenefitsHref)

          textOnPageCheck(common.travelHeader, fieldHeaderSelector(7))
          changeAmountRowCheck(common.travelSubheading, common.no, 8, 1, s"${common.changeText} ${specific.travelSubheadingHiddenText}", travelEntertainmentBenefitsAmountHref)

          textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(9))
          changeAmountRowCheck(common.utilitiesSubheading, common.no, 10, 1, s"${common.changeText} ${specific.utilitiesSubheadingHiddenText}", utilitiesOrGeneralServicesBenefitsHref)

          textOnPageCheck(common.medicalHeader, fieldHeaderSelector(11))
          changeAmountRowCheck(common.medicalSubheading, common.no, 12, 1, s"${common.changeText} ${specific.medicalSubheadingHiddenText}", medicalChildcareEducationHref)

          textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(13))
          changeAmountRowCheck(common.incomeTaxSubheading, common.no, 14, 1, s"${common.changeText} ${specific.incomeTaxSubheadingHiddenText}", incomeTaxOrIncurredCostsBenefitsHref)

          textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(15))
          changeAmountRowCheck(common.reimbursedSubheading, common.no, 16, 1, s"${common.changeText} ${specific.reimbursedSubheadingHiddenText}", reimbursedCostsVouchersAndNonCashBenefitsHref)

          textOnPageCheck(common.assetsHeader, fieldHeaderSelector(17))
          changeAmountRowCheck(common.assetsSubheading, common.no, 18, 1, s"${common.changeText} ${specific.assetsSubheadingHiddenText}", assetsOrAssetTransfersBenefitsHref)

          buttonCheck(common.saveAndContinue)

          welshToggleCheck(user.isWelsh)

          s"should not display the following values" in {
            document().body().toString.contains(specific.expectedP1) shouldBe false
            document().body().toString.contains(common.companyCar) shouldBe false
            document().body().toString.contains(common.fuelForCompanyCar) shouldBe false
            document().body().toString.contains(common.companyVan) shouldBe false
            document().body().toString.contains(common.fuelForCompanyVan) shouldBe false
            document().body().toString.contains(common.mileageBenefit) shouldBe false
            document().body().toString.contains(common.travelAndSubsistence) shouldBe false
            document().body().toString.contains(common.personalCosts) shouldBe false
            document().body().toString.contains(common.entertainment) shouldBe false
            document().body().toString.contains(common.telephone) shouldBe false
            document().body().toString.contains(common.servicesProvided) shouldBe false
            document().body().toString.contains(common.profSubscriptions) shouldBe false
            document().body().toString.contains(common.otherServices) shouldBe false
            document().body().toString.contains(common.nursery) shouldBe false
            document().body().toString.contains(common.beneficialLoans) shouldBe false
            document().body().toString.contains(common.educational) shouldBe false
            document().body().toString.contains(common.incomeTaxPaid) shouldBe false
            document().body().toString.contains(common.incurredCostsPaid) shouldBe false
            document().body().toString.contains(common.nonTaxable) shouldBe false
            document().body().toString.contains(common.taxableCosts) shouldBe false
            document().body().toString.contains(common.vouchers) shouldBe false
            document().body().toString.contains(common.nonCash) shouldBe false
            document().body().toString.contains(common.otherBenefits) shouldBe false
            document().body().toString.contains(common.assetTransfers) shouldBe false
            document().body().toString.contains(common.companyCarAmount) shouldBe false
            document().body().toString.contains(common.fuelForCompanyCarAmount) shouldBe false
            document().body().toString.contains(common.companyVanAmount) shouldBe false
            document().body().toString.contains(common.fuelForCompanyVanAmount) shouldBe false
            document().body().toString.contains(common.mileageBenefitAmount) shouldBe false
            document().body().toString.contains(common.travelAndSubsistenceAmount) shouldBe false
            document().body().toString.contains(common.personalCostsAmount) shouldBe false
            document().body().toString.contains(common.entertainmentAmount) shouldBe false
            document().body().toString.contains(common.telephoneAmount) shouldBe false
            document().body().toString.contains(common.servicesProvidedAmount) shouldBe false
            document().body().toString.contains(common.profSubscriptionsAmount) shouldBe false
            document().body().toString.contains(common.otherServicesAmount) shouldBe false
            document().body().toString.contains(common.medicalInsAmount) shouldBe false
            document().body().toString.contains(common.nurseryAmount) shouldBe false
            document().body().toString.contains(common.beneficialLoansAmount) shouldBe false
            document().body().toString.contains(common.educationalAmount) shouldBe false
            document().body().toString.contains(common.incomeTaxPaidAmount) shouldBe false
            document().body().toString.contains(common.incurredCostsPaidAmount) shouldBe false
            document().body().toString.contains(common.nonTaxableAmount) shouldBe false
            document().body().toString.contains(common.taxableCostsAmount) shouldBe false
            document().body().toString.contains(common.vouchersAmount) shouldBe false
            document().body().toString.contains(common.nonCashAmount) shouldBe false
            document().body().toString.contains(common.otherBenefitsAmount) shouldBe false
            document().body().toString.contains(common.assetsAmount) shouldBe false
            document().body().toString.contains(common.assetTransfersAmount) shouldBe false
          }
        }

        "return a page with only the benefits received subheading when its EOY and only the benefits question answered as no" which {
          def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
            EmploymentUserData(sessionId, mtditid, nino, defaultTaxYear - 1, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

          def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel =
            EmploymentCYAModel(
              EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc),
              Some(BenefitsViewModel(isUsingCustomerData = false))
            )

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            urlGet(url(defaultTaxYear - 1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(defaultTaxYear - 1))
          textOnPageCheck(specific.expectedP1, Selectors.p1)

          changeAmountRowCheck(common.benefitsReceived, common.no, 3, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}", receiveAnyBenefitsHref)

          buttonCheck(common.saveAndContinue)

          welshToggleCheck(user.isWelsh)

          s"should not display the following values" in {
            document().body().toString.contains(common.carSubheading) shouldBe false
            document().body().toString.contains(common.accommodationSubheading) shouldBe false
            document().body().toString.contains(common.travelSubheading) shouldBe false
            document().body().toString.contains(common.utilitiesSubheading) shouldBe false
            document().body().toString.contains(common.medicalSubheading) shouldBe false
            document().body().toString.contains(common.incomeTaxSubheading) shouldBe false
            document().body().toString.contains(common.reimbursedSubheading) shouldBe false
            document().body().toString.contains(common.assetsSubheading) shouldBe false
          }
        }

        "return only the relevant data on the page when only certain data items are in mongo and in year" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val employmentSources = Seq(anEmploymentSource.copy(employmentBenefits = filteredBenefits))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = employmentSources))), nino, defaultTaxYear)
            urlGet(url(), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption())
          textOnPageCheck(specific.expectedP1, Selectors.p1)
          textOnPageCheck(specific.expectedP2(), Selectors.p2)

          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(common.companyVan, fieldNameSelector(5, 1))
          textOnPageCheck("£3", fieldAmountSelector(5, 1))
          textOnPageCheck(common.fuelForCompanyVan, fieldNameSelector(5, 2))
          textOnPageCheck("£4", fieldAmountSelector(5, 2))
          textOnPageCheck(common.mileageBenefit, fieldNameSelector(5, 3))
          textOnPageCheck("£5", fieldAmountSelector(5, 3))

          welshToggleCheck(user.isWelsh)

          s"should not display the following values" in {
            document().body().toString.contains(common.accommodationHeader) shouldBe false
            document().body().toString.contains(common.qualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(common.nonQualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(common.travelHeader) shouldBe false
            document().body().toString.contains(common.travelAndSubsistence) shouldBe false
            document().body().toString.contains(common.personalCosts) shouldBe false
            document().body().toString.contains(common.entertainment) shouldBe false
            document().body().toString.contains(common.utilitiesHeader) shouldBe false
            document().body().toString.contains(common.telephone) shouldBe false
            document().body().toString.contains(common.servicesProvided) shouldBe false
            document().body().toString.contains(common.profSubscriptions) shouldBe false
            document().body().toString.contains(common.otherServices) shouldBe false
            document().body().toString.contains(common.medicalHeader) shouldBe false
            document().body().toString.contains(common.nursery) shouldBe false
            document().body().toString.contains(common.beneficialLoans) shouldBe false
            document().body().toString.contains(common.educational) shouldBe false
            document().body().toString.contains(common.incomeTaxHeader) shouldBe false
            document().body().toString.contains(common.incomeTaxPaid) shouldBe false
            document().body().toString.contains(common.incurredCostsPaid) shouldBe false
            document().body().toString.contains(common.reimbursedHeader) shouldBe false
            document().body().toString.contains(common.nonTaxable) shouldBe false
            document().body().toString.contains(common.taxableCosts) shouldBe false
            document().body().toString.contains(common.vouchers) shouldBe false
            document().body().toString.contains(common.nonCash) shouldBe false
            document().body().toString.contains(common.otherBenefits) shouldBe false
            document().body().toString.contains(common.assetsHeader) shouldBe false
            document().body().toString.contains(common.assetTransfers) shouldBe false
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

    "return a redirect at the end of the year when id is not found" in {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentSources = Seq(anEmploymentSource.copy(employmentBenefits = Some(anEmploymentBenefits)))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = employmentSources))), nino, defaultTaxYear)
        urlGet(s"$appUrl/${taxYear - 1}/check-employment-benefits?employmentId=0022", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear - 1))
    }

    "redirect to the Did your client receive any benefits page when its EOY and theres no benefits model in the session data" in {
      def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
        EmploymentUserData(sessionId, mtditid, nino, defaultTaxYear - 1, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

      def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel =
        EmploymentCYAModel(
          EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc),
          None
        )

      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        val employmentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None)))
        userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, taxYear - 1)
        urlGet(url(defaultTaxYear - 1), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(ReceiveAnyBenefitsController.show(taxYear - 1, employmentId).url)
    }

    "redirect to the Did your client receive any benefits page when its EOY and theres no benefits model in the mongo data" in {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None)))
        userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, defaultTaxYear)
        urlGet(url(defaultTaxYear - 1), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(ReceiveAnyBenefitsController.show(taxYear - 1, employmentId).url)
    }

    "redirect to overview page when theres no benefits and in year" in {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val hmrcEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = hmrcEmploymentData))), nino, defaultTaxYear)
        urlGet(url(), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }


  ".submit" when {
    "return a redirect when in year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = true)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlPost(url(), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "return internal server error page whilst not implemented" in {
      val employmentData = anEmploymentCYAModel.copy(employmentBenefits = None)
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = employmentData).copy(employmentId = employmentId), aUserRequest)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlPost(url(taxYear - 1), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return a redirect to show method when at end of year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlPost(url(taxYear - 1), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has a url of benefits show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYear - 1, employmentId).url)
      }
    }
  }
}