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

package controllers.employment

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentBenefitsBuilder.anEmploymentBenefits
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.employment.StudentLoansBuilder.aStudentLoans
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import helpers.SessionCookieCrumbler.getSessionMap
import models.benefits.{AccommodationRelocationModel, Benefits, BenefitsViewModel}
import models.employment.createUpdate.{CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.employment.{Deductions, EmploymentBenefits}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
//import org.scalatest.DoNotDiscover
//
//@DoNotDiscover
class CheckYourBenefitsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"
  private val taxYearEOY = taxYear - 1

  private lazy val filteredBenefits: Some[EmploymentBenefits] = Some(EmploymentBenefits(
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
    val returnToEmploymentSummarySelector = "#returnToEmploymentSummaryBtn"
    val returnToEmployerSelector = "#returnToEmployerBtn"

    def fieldNameSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dt"

    def fieldAmountSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd.govuk-summary-list__value"

    def fieldChangeLinkSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd > a"

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
    val returnToEmploymentSummaryText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(year: Int = taxYear): String = s"Employment for 6 April ${year - 1} to 5 April $year"

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
    val returnToEmploymentSummaryText: String = "Return to employment summary"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(year: Int = taxYear): String = s"Employment for 6 April ${year - 1} to 5 April $year"

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
    val returnToEmploymentSummaryText: String = "Return to employment summary"
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

  object ExpectedAgentCY extends SpecificExpectedResults {
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

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
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
            urlGet(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption())
          textOnPageCheck(specific.expectedP1, Selectors.p1)
          textOnPageCheck(specific.expectedP2(), Selectors.p2)
          textOnPageCheck(common.benefitsReceived, fieldNameSelector(4, 1))
          textOnPageCheck("Yes", fieldAmountSelector(4, 1), "for benefits question")
          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(5))
          textOnPageCheck(common.carSubheading, fieldNameSelector(6, 1))
          textOnPageCheck("Yes", fieldAmountSelector(6, 1), "for vehicles section question")
          textOnPageCheck(common.companyCar, fieldNameSelector(6, 2))
          textOnPageCheck("Yes", fieldAmountSelector(6, 2), "for carQuestion")
          textOnPageCheck(common.companyCarAmount, fieldNameSelector(6, 3))
          textOnPageCheck("£1.23", fieldAmountSelector(6, 3))
          textOnPageCheck(common.fuelForCompanyCar, fieldNameSelector(6, 4))
          textOnPageCheck("Yes", fieldAmountSelector(6, 4), "for carFuelQuestion")
          textOnPageCheck(common.fuelForCompanyCarAmount, fieldNameSelector(6, 5))
          textOnPageCheck("£2", fieldAmountSelector(6, 5))
          textOnPageCheck(common.companyVan, fieldNameSelector(6, 6))
          textOnPageCheck("Yes", fieldAmountSelector(6, 6), "for vanQuestion")
          textOnPageCheck(common.companyVanAmount, fieldNameSelector(6, 7))
          textOnPageCheck("£3", fieldAmountSelector(6, 7))
          textOnPageCheck(common.fuelForCompanyVan, fieldNameSelector(6, 8))
          textOnPageCheck("Yes", fieldAmountSelector(6, 8), "for vanFuelQuestion")
          textOnPageCheck(common.fuelForCompanyVanAmount, fieldNameSelector(6, 9))
          textOnPageCheck("£4", fieldAmountSelector(6, 9))
          textOnPageCheck(common.mileageBenefit, fieldNameSelector(6, 10))
          textOnPageCheck("Yes", fieldAmountSelector(6, 10), "for mileageQuestion")
          textOnPageCheck(common.mileageBenefitAmount, fieldNameSelector(6, 11))
          textOnPageCheck("£5", fieldAmountSelector(6, 11))
          textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(7))
          textOnPageCheck(common.accommodationSubheading, fieldNameSelector(8, 1))
          textOnPageCheck("Yes", fieldAmountSelector(8, 1), "for accommodation section question")
          textOnPageCheck(common.accommodation, fieldNameSelector(8, 2))
          textOnPageCheck("Yes", fieldAmountSelector(8, 2), "for accommodationQuestion")
          textOnPageCheck(common.accommodationAmount, fieldNameSelector(8, 3))
          textOnPageCheck("£6", fieldAmountSelector(8, 3))
          textOnPageCheck(common.qualifyingRelocationCosts, fieldNameSelector(8, 4))
          textOnPageCheck("Yes", fieldAmountSelector(8, 4), "for qualifyingRelocationExpensesQuestion")
          textOnPageCheck(common.qualifyingRelocationCostsAmount, fieldNameSelector(8, 5))
          textOnPageCheck("£7", fieldAmountSelector(8, 5))
          textOnPageCheck(common.nonQualifyingRelocationCosts, fieldNameSelector(8, 6))
          textOnPageCheck("Yes", fieldAmountSelector(8, 6), "for nonQualifyingRelocationExpensesQuestion")
          textOnPageCheck(common.nonQualifyingRelocationCostsAmount, fieldNameSelector(8, 7))
          textOnPageCheck("£8", fieldAmountSelector(8, 7))
          textOnPageCheck(common.travelHeader, fieldHeaderSelector(9))
          textOnPageCheck(common.travelSubheading, fieldNameSelector(10, 1))
          textOnPageCheck("Yes", fieldAmountSelector(10, 1), "for travel section question")
          textOnPageCheck(common.travelAndSubsistence, fieldNameSelector(10, 2))
          textOnPageCheck("Yes", fieldAmountSelector(10, 2), "for travelAndSubsistenceQuestion")
          textOnPageCheck(common.travelAndSubsistenceAmount, fieldNameSelector(10, 3))
          textOnPageCheck("£9", fieldAmountSelector(10, 3))
          textOnPageCheck(common.personalCosts, fieldNameSelector(10, 4))
          textOnPageCheck("Yes", fieldAmountSelector(10, 4), "for personalIncidentalExpensesQuestion")
          textOnPageCheck(common.personalCostsAmount, fieldNameSelector(10, 5))
          textOnPageCheck("£10", fieldAmountSelector(10, 5))
          textOnPageCheck(common.entertainment, fieldNameSelector(10, 6))
          textOnPageCheck("Yes", fieldAmountSelector(10, 6), "for entertainingQuestion")
          textOnPageCheck(common.entertainmentAmount, fieldNameSelector(10, 7))
          textOnPageCheck("£11", fieldAmountSelector(10, 7))
          textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(11))
          textOnPageCheck(common.utilitiesSubheading, fieldNameSelector(12, 1))
          textOnPageCheck("Yes", fieldAmountSelector(12, 1), "for utilities section question")
          textOnPageCheck(common.telephone, fieldNameSelector(12, 2))
          textOnPageCheck("Yes", fieldAmountSelector(12, 2), "for telephoneQuestion")
          textOnPageCheck(common.telephoneAmount, fieldNameSelector(12, 3))
          textOnPageCheck("£12", fieldAmountSelector(12, 3))
          textOnPageCheck(common.servicesProvided, fieldNameSelector(12, 4))
          textOnPageCheck("Yes", fieldAmountSelector(12, 4), "for employerProvidedServicesQuestion")
          textOnPageCheck(common.servicesProvidedAmount, fieldNameSelector(12, 5))
          textOnPageCheck("£13", fieldAmountSelector(12, 5))
          textOnPageCheck(common.profSubscriptions, fieldNameSelector(12, 6))
          textOnPageCheck("Yes", fieldAmountSelector(12, 6), "for employerProvidedProfessionalSubscriptionsQuestion")
          textOnPageCheck(common.profSubscriptionsAmount, fieldNameSelector(12, 7))
          textOnPageCheck("£14", fieldAmountSelector(12, 7))
          textOnPageCheck(common.otherServices, fieldNameSelector(12, 8))
          textOnPageCheck("Yes", fieldAmountSelector(12, 8), "for serviceQuestion")
          textOnPageCheck(common.otherServicesAmount, fieldNameSelector(12, 9))
          textOnPageCheck("£15", fieldAmountSelector(12, 9))
          textOnPageCheck(common.medicalHeader, fieldHeaderSelector(13))
          textOnPageCheck(common.medicalSubheading, fieldNameSelector(14, 1))
          textOnPageCheck("Yes", fieldAmountSelector(14, 1), "for medical section question")
          textOnPageCheck(common.medicalIns, fieldNameSelector(14, 2))
          textOnPageCheck("Yes", fieldAmountSelector(14, 2), "for medicalInsuranceQuestion")
          textOnPageCheck(common.medicalInsAmount, fieldNameSelector(14, 3))
          textOnPageCheck("£16", fieldAmountSelector(14, 3))
          textOnPageCheck(common.nursery, fieldNameSelector(14, 4))
          textOnPageCheck("Yes", fieldAmountSelector(14, 4), "for nurseryPlacesQuestion")
          textOnPageCheck(common.nurseryAmount, fieldNameSelector(14, 5))
          textOnPageCheck("£17", fieldAmountSelector(14, 5))
          textOnPageCheck(common.educational, fieldNameSelector(14, 6))
          textOnPageCheck("Yes", fieldAmountSelector(14, 6), "for educationalServicesQuestion")
          textOnPageCheck(common.educationalAmount, fieldNameSelector(14, 7))
          textOnPageCheck("£19", fieldAmountSelector(14, 7))
          textOnPageCheck(common.beneficialLoans, fieldNameSelector(14, 8))
          textOnPageCheck("Yes", fieldAmountSelector(14, 8), "for beneficialLoanQuestion")
          textOnPageCheck(common.beneficialLoansAmount, fieldNameSelector(14, 9))
          textOnPageCheck("£18", fieldAmountSelector(14, 9))
          textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(15))
          textOnPageCheck(common.incomeTaxSubheading, fieldNameSelector(16, 1))
          textOnPageCheck("Yes", fieldAmountSelector(16, 1), "for income tax section question")
          textOnPageCheck(common.incomeTaxPaid, fieldNameSelector(16, 2))
          textOnPageCheck("Yes", fieldAmountSelector(16, 2), "for incomeTaxPaidByDirectorQuestion")
          textOnPageCheck(common.incomeTaxPaidAmount, fieldNameSelector(16, 3))
          textOnPageCheck("£20", fieldAmountSelector(16, 3))
          textOnPageCheck(common.incurredCostsPaid, fieldNameSelector(16, 4))
          textOnPageCheck("Yes", fieldAmountSelector(16, 4), "for paymentsOnEmployeesBehalfQuestion")
          textOnPageCheck(common.incurredCostsPaidAmount, fieldNameSelector(16, 5))
          textOnPageCheck("£21", fieldAmountSelector(16, 5))
          textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(17))
          textOnPageCheck(common.reimbursedSubheading, fieldNameSelector(18, 1))
          textOnPageCheck("Yes", fieldAmountSelector(18, 1), "for reimbursements section question")
          textOnPageCheck(common.nonTaxable, fieldNameSelector(18, 2))
          textOnPageCheck("Yes", fieldAmountSelector(18, 2), "for expensesQuestion")
          textOnPageCheck(common.nonTaxableAmount, fieldNameSelector(18, 3))
          textOnPageCheck("£22", fieldAmountSelector(18, 3))
          textOnPageCheck(common.taxableCosts, fieldNameSelector(18, 4))
          textOnPageCheck("Yes", fieldAmountSelector(18, 4), "for taxableExpensesQuestion")
          textOnPageCheck(common.taxableCostsAmount, fieldNameSelector(18, 5))
          textOnPageCheck("£23", fieldAmountSelector(18, 5))
          textOnPageCheck(common.vouchers, fieldNameSelector(18, 6))
          textOnPageCheck("Yes", fieldAmountSelector(18, 6), "for vouchersAndCreditCardsQuestion")
          textOnPageCheck(common.vouchersAmount, fieldNameSelector(18, 7))
          textOnPageCheck("£24", fieldAmountSelector(18, 7))
          textOnPageCheck(common.nonCash, fieldNameSelector(18, 8))
          textOnPageCheck("Yes", fieldAmountSelector(18, 8), "for nonCashQuestion")
          textOnPageCheck(common.nonCashAmount, fieldNameSelector(18, 9))
          textOnPageCheck("£25", fieldAmountSelector(18, 9))
          textOnPageCheck(common.otherBenefits, fieldNameSelector(18, 10))
          textOnPageCheck("Yes", fieldAmountSelector(18, 10), "for otherItemsQuestion")
          textOnPageCheck(common.otherBenefitsAmount, fieldNameSelector(18, 11))
          textOnPageCheck("£26", fieldAmountSelector(18, 11))
          textOnPageCheck(common.assetsHeader, fieldHeaderSelector(19))
          textOnPageCheck(common.assetsSubheading, fieldNameSelector(20, 1), "subHeading")
          textOnPageCheck("Yes", fieldAmountSelector(20, 1), "for assets section question")
          textOnPageCheck(common.assets, fieldNameSelector(20, 2))
          textOnPageCheck("Yes", fieldAmountSelector(20, 2), "for assetsQuestion")
          textOnPageCheck(common.assetsAmount, fieldNameSelector(20, 3))
          textOnPageCheck("£27", fieldAmountSelector(20, 3))
          textOnPageCheck(common.assetTransfers, fieldNameSelector(20, 4))
          textOnPageCheck("Yes", fieldAmountSelector(20, 4), "for assetTransferQuestion")
          textOnPageCheck(common.assetTransfersAmount, fieldNameSelector(20, 5))
          textOnPageCheck("£280,000", fieldAmountSelector(20, 5))
          buttonCheck(common.returnToEmploymentSummaryText, Selectors.returnToEmploymentSummarySelector)
          welshToggleCheck(user.isWelsh)
        }

        "return a fully populated page when there are multiple employment sources and all fields are populated for in year" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val multipleSources = Seq(
              anEmploymentSource,
              anEmploymentSource.copy(
                employmentId = "002",
                employerName = "dave",
                payrollId = Some("12345693"),
                startDate = Some("2018-04-18"),
              ))

            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = multipleSources))), nino, taxYear)
            urlGet(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption())
          textOnPageCheck(specific.expectedP1, Selectors.p1)
          textOnPageCheck(specific.expectedP2(), Selectors.p2)
          textOnPageCheck(common.benefitsReceived, fieldNameSelector(4, 1))
          textOnPageCheck("Yes", fieldAmountSelector(4, 1), "for benefits question")
          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(5))
          textOnPageCheck(common.carSubheading, fieldNameSelector(6, 1))
          textOnPageCheck("Yes", fieldAmountSelector(6, 1), "for vehicles section question")
          textOnPageCheck(common.companyCar, fieldNameSelector(6, 2))
          textOnPageCheck("Yes", fieldAmountSelector(6, 2), "for carQuestion")
          textOnPageCheck(common.companyCarAmount, fieldNameSelector(6, 3))
          textOnPageCheck("£1.23", fieldAmountSelector(6, 3))
          textOnPageCheck(common.fuelForCompanyCar, fieldNameSelector(6, 4))
          textOnPageCheck("Yes", fieldAmountSelector(6, 4), "for carFuelQuestion")
          textOnPageCheck(common.fuelForCompanyCarAmount, fieldNameSelector(6, 5))
          textOnPageCheck("£2", fieldAmountSelector(6, 5))
          textOnPageCheck(common.companyVan, fieldNameSelector(6, 6))
          textOnPageCheck("Yes", fieldAmountSelector(6, 6), "for vanQuestion")
          textOnPageCheck(common.companyVanAmount, fieldNameSelector(6, 7))
          textOnPageCheck("£3", fieldAmountSelector(6, 7))
          textOnPageCheck(common.fuelForCompanyVan, fieldNameSelector(6, 8))
          textOnPageCheck("Yes", fieldAmountSelector(6, 8), "for vanFuelQuestion")
          textOnPageCheck(common.fuelForCompanyVanAmount, fieldNameSelector(6, 9))
          textOnPageCheck("£4", fieldAmountSelector(6, 9))
          textOnPageCheck(common.mileageBenefit, fieldNameSelector(6, 10))
          textOnPageCheck("Yes", fieldAmountSelector(6, 10), "for mileageQuestion")
          textOnPageCheck(common.mileageBenefitAmount, fieldNameSelector(6, 11))
          textOnPageCheck("£5", fieldAmountSelector(6, 11))
          textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(7))
          textOnPageCheck(common.accommodationSubheading, fieldNameSelector(8, 1))
          textOnPageCheck("Yes", fieldAmountSelector(8, 1), "for accommodation section question")
          textOnPageCheck(common.accommodation, fieldNameSelector(8, 2))
          textOnPageCheck("Yes", fieldAmountSelector(8, 2), "for accommodationQuestion")
          textOnPageCheck(common.accommodationAmount, fieldNameSelector(8, 3))
          textOnPageCheck("£6", fieldAmountSelector(8, 3))
          textOnPageCheck(common.qualifyingRelocationCosts, fieldNameSelector(8, 4))
          textOnPageCheck("Yes", fieldAmountSelector(8, 4), "for qualifyingRelocationExpensesQuestion")
          textOnPageCheck(common.qualifyingRelocationCostsAmount, fieldNameSelector(8, 5))
          textOnPageCheck("£7", fieldAmountSelector(8, 5))
          textOnPageCheck(common.nonQualifyingRelocationCosts, fieldNameSelector(8, 6))
          textOnPageCheck("Yes", fieldAmountSelector(8, 6), "for nonQualifyingRelocationExpensesQuestion")
          textOnPageCheck(common.nonQualifyingRelocationCostsAmount, fieldNameSelector(8, 7))
          textOnPageCheck("£8", fieldAmountSelector(8, 7))
          textOnPageCheck(common.travelHeader, fieldHeaderSelector(9))
          textOnPageCheck(common.travelSubheading, fieldNameSelector(10, 1))
          textOnPageCheck("Yes", fieldAmountSelector(10, 1), "for travel section question")
          textOnPageCheck(common.travelAndSubsistence, fieldNameSelector(10, 2))
          textOnPageCheck("Yes", fieldAmountSelector(10, 2), "for travelAndSubsistenceQuestion")
          textOnPageCheck(common.travelAndSubsistenceAmount, fieldNameSelector(10, 3))
          textOnPageCheck("£9", fieldAmountSelector(10, 3))
          textOnPageCheck(common.personalCosts, fieldNameSelector(10, 4))
          textOnPageCheck("Yes", fieldAmountSelector(10, 4), "for personalIncidentalExpensesQuestion")
          textOnPageCheck(common.personalCostsAmount, fieldNameSelector(10, 5))
          textOnPageCheck("£10", fieldAmountSelector(10, 5))
          textOnPageCheck(common.entertainment, fieldNameSelector(10, 6))
          textOnPageCheck("Yes", fieldAmountSelector(10, 6), "for entertainingQuestion")
          textOnPageCheck(common.entertainmentAmount, fieldNameSelector(10, 7))
          textOnPageCheck("£11", fieldAmountSelector(10, 7))
          textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(11))
          textOnPageCheck(common.utilitiesSubheading, fieldNameSelector(12, 1))
          textOnPageCheck("Yes", fieldAmountSelector(12, 1), "for utilities section question")
          textOnPageCheck(common.telephone, fieldNameSelector(12, 2))
          textOnPageCheck("Yes", fieldAmountSelector(12, 2), "for telephoneQuestion")
          textOnPageCheck(common.telephoneAmount, fieldNameSelector(12, 3))
          textOnPageCheck("£12", fieldAmountSelector(12, 3))
          textOnPageCheck(common.servicesProvided, fieldNameSelector(12, 4))
          textOnPageCheck("Yes", fieldAmountSelector(12, 4), "for employerProvidedServicesQuestion")
          textOnPageCheck(common.servicesProvidedAmount, fieldNameSelector(12, 5))
          textOnPageCheck("£13", fieldAmountSelector(12, 5))
          textOnPageCheck(common.profSubscriptions, fieldNameSelector(12, 6))
          textOnPageCheck("Yes", fieldAmountSelector(12, 6), "for employerProvidedProfessionalSubscriptionsQuestion")
          textOnPageCheck(common.profSubscriptionsAmount, fieldNameSelector(12, 7))
          textOnPageCheck("£14", fieldAmountSelector(12, 7))
          textOnPageCheck(common.otherServices, fieldNameSelector(12, 8))
          textOnPageCheck("Yes", fieldAmountSelector(12, 8), "for serviceQuestion")
          textOnPageCheck(common.otherServicesAmount, fieldNameSelector(12, 9))
          textOnPageCheck("£15", fieldAmountSelector(12, 9))
          textOnPageCheck(common.medicalHeader, fieldHeaderSelector(13))
          textOnPageCheck(common.medicalSubheading, fieldNameSelector(14, 1))
          textOnPageCheck("Yes", fieldAmountSelector(14, 1), "for medical section question")
          textOnPageCheck(common.medicalIns, fieldNameSelector(14, 2))
          textOnPageCheck("Yes", fieldAmountSelector(14, 2), "for medicalInsuranceQuestion")
          textOnPageCheck(common.medicalInsAmount, fieldNameSelector(14, 3))
          textOnPageCheck("£16", fieldAmountSelector(14, 3))
          textOnPageCheck(common.nursery, fieldNameSelector(14, 4))
          textOnPageCheck("Yes", fieldAmountSelector(14, 4), "for nurseryPlacesQuestion")
          textOnPageCheck(common.nurseryAmount, fieldNameSelector(14, 5))
          textOnPageCheck("£17", fieldAmountSelector(14, 5))
          textOnPageCheck(common.educational, fieldNameSelector(14, 6))
          textOnPageCheck("Yes", fieldAmountSelector(14, 6), "for educationalServicesQuestion")
          textOnPageCheck(common.educationalAmount, fieldNameSelector(14, 7))
          textOnPageCheck("£19", fieldAmountSelector(14, 7))
          textOnPageCheck(common.beneficialLoans, fieldNameSelector(14, 8))
          textOnPageCheck("Yes", fieldAmountSelector(14, 8), "for beneficialLoanQuestion")
          textOnPageCheck(common.beneficialLoansAmount, fieldNameSelector(14, 9))
          textOnPageCheck("£18", fieldAmountSelector(14, 9))
          textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(15))
          textOnPageCheck(common.incomeTaxSubheading, fieldNameSelector(16, 1))
          textOnPageCheck("Yes", fieldAmountSelector(16, 1), "for income tax section question")
          textOnPageCheck(common.incomeTaxPaid, fieldNameSelector(16, 2))
          textOnPageCheck("Yes", fieldAmountSelector(16, 2), "for incomeTaxPaidByDirectorQuestion")
          textOnPageCheck(common.incomeTaxPaidAmount, fieldNameSelector(16, 3))
          textOnPageCheck("£20", fieldAmountSelector(16, 3))
          textOnPageCheck(common.incurredCostsPaid, fieldNameSelector(16, 4))
          textOnPageCheck("Yes", fieldAmountSelector(16, 4), "for paymentsOnEmployeesBehalfQuestion")
          textOnPageCheck(common.incurredCostsPaidAmount, fieldNameSelector(16, 5))
          textOnPageCheck("£21", fieldAmountSelector(16, 5))
          textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(17))
          textOnPageCheck(common.reimbursedSubheading, fieldNameSelector(18, 1))
          textOnPageCheck("Yes", fieldAmountSelector(18, 1), "for reimbursements section question")
          textOnPageCheck(common.nonTaxable, fieldNameSelector(18, 2))
          textOnPageCheck("Yes", fieldAmountSelector(18, 2), "for expensesQuestion")
          textOnPageCheck(common.nonTaxableAmount, fieldNameSelector(18, 3))
          textOnPageCheck("£22", fieldAmountSelector(18, 3))
          textOnPageCheck(common.taxableCosts, fieldNameSelector(18, 4))
          textOnPageCheck("Yes", fieldAmountSelector(18, 4), "for taxableExpensesQuestion")
          textOnPageCheck(common.taxableCostsAmount, fieldNameSelector(18, 5))
          textOnPageCheck("£23", fieldAmountSelector(18, 5))
          textOnPageCheck(common.vouchers, fieldNameSelector(18, 6))
          textOnPageCheck("Yes", fieldAmountSelector(18, 6), "for vouchersAndCreditCardsQuestion")
          textOnPageCheck(common.vouchersAmount, fieldNameSelector(18, 7))
          textOnPageCheck("£24", fieldAmountSelector(18, 7))
          textOnPageCheck(common.nonCash, fieldNameSelector(18, 8))
          textOnPageCheck("Yes", fieldAmountSelector(18, 8), "for nonCashQuestion")
          textOnPageCheck(common.nonCashAmount, fieldNameSelector(18, 9))
          textOnPageCheck("£25", fieldAmountSelector(18, 9))
          textOnPageCheck(common.otherBenefits, fieldNameSelector(18, 10))
          textOnPageCheck("Yes", fieldAmountSelector(18, 10), "for otherItemsQuestion")
          textOnPageCheck(common.otherBenefitsAmount, fieldNameSelector(18, 11))
          textOnPageCheck("£26", fieldAmountSelector(18, 11))
          textOnPageCheck(common.assetsHeader, fieldHeaderSelector(19))
          textOnPageCheck(common.assetsSubheading, fieldNameSelector(20, 1), "subHeading")
          textOnPageCheck("Yes", fieldAmountSelector(20, 1), "for assets section question")
          textOnPageCheck(common.assets, fieldNameSelector(20, 2))
          textOnPageCheck("Yes", fieldAmountSelector(20, 2), "for assetsQuestion")
          textOnPageCheck(common.assetsAmount, fieldNameSelector(20, 3))
          textOnPageCheck("£27", fieldAmountSelector(20, 3))
          textOnPageCheck(common.assetTransfers, fieldNameSelector(20, 4))
          textOnPageCheck("Yes", fieldAmountSelector(20, 4), "for assetTransferQuestion")
          textOnPageCheck(common.assetTransfersAmount, fieldNameSelector(20, 5))
          textOnPageCheck("£280,000", fieldAmountSelector(20, 5))
          buttonCheck(common.returnToEmployerText, Selectors.returnToEmployerSelector)
          welshToggleCheck(user.isWelsh)
        }

        "return a fully populated page when all the fields are populated when at the end of the year" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
            urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYear - 1))
          textOnPageCheck(specific.expectedP1, Selectors.p1)

          changeAmountRowCheck(common.benefitsReceived, common.yes, 3, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}", companyBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(4))

          changeAmountRowCheck(common.carSubheading, common.yes, 5, 1, s"${common.changeText} ${specific.carSubheadingHiddenText}", carVanFuelBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.companyCar, common.yes, 5, 2, s"${common.changeText} ${specific.companyCarHiddenText}", carBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.companyCarAmount, "£1.23", 5, 3, s"${common.changeText} ${specific.companyCarAmountHiddenText}", carBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.fuelForCompanyCar, common.yes, 5, 4, s"${common.changeText} ${specific.fuelForCompanyCarHiddenText}", carFuelBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.fuelForCompanyCarAmount, "£2", 5, 5, s"${common.changeText} ${specific.fuelForCompanyCarAmountHiddenText}",
            carFuelBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.companyVan, common.yes, 5, 6, s"${common.changeText} ${specific.companyVanHiddenText}", vanBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.fuelForCompanyVan, common.yes, 5, 8, s"${common.changeText} ${specific.fuelForCompanyVanHiddenText}", vanFuelBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.companyVanAmount, "£3", 5, 7, s"${common.changeText} ${specific.companyVanAmountHiddenText}", vanBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.fuelForCompanyVanAmount, "£4", 5, 9, s"${common.changeText} ${specific.fuelForCompanyVanAmountHiddenText}",
            vanFuelBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.mileageBenefit, common.yes, 5, 10, s"${common.changeText} ${specific.mileageBenefitHiddenText}", mileageBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.mileageBenefitAmount, "£5", 5, 11, s"${common.changeText} ${specific.mileageBenefitAmountHiddenText}", mileageBenefitsAmountUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(6))
          changeAmountRowCheck(common.accommodationSubheading, common.yes, 7, 1, s"${common.changeText} ${specific.accommodationSubheadingHiddenText}",
            accommodationRelocationBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.accommodation, common.yes, 7, 2, s"${common.changeText} ${specific.accommodationHiddenText}", livingAccommodationBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.accommodationAmount, "£6", 7, 3, s"${common.changeText} ${specific.accommodationAmountHiddenText}",
            livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.qualifyingRelocationCosts, common.yes, 7, 4, s"${common.changeText} ${specific.qualifyingRelocationCostsHiddenText}",
            qualifyingRelocationBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.qualifyingRelocationCostsAmount, "£7", 7, 5, s"${common.changeText} ${specific.qualifyingRelocationCostsAmountHiddenText}",
            qualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.nonQualifyingRelocationCosts, common.yes, 7, 6, s"${common.changeText} ${specific.nonQualifyingRelocationCostsHiddenText}",
            nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.nonQualifyingRelocationCostsAmount, "£8", 7, 7, s"${common.changeText} ${specific.nonQualifyingRelocationCostsAmountHiddenText}",
            nonQualifyingRelocationBenefitsAmountUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.travelHeader, fieldHeaderSelector(8))
          changeAmountRowCheck(common.travelSubheading, common.yes, 9, 1, s"${common.changeText} ${specific.travelSubheadingHiddenText}",
            travelOrEntertainmentBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.travelAndSubsistence, common.yes, 9, 2, s"${common.changeText} ${specific.travelAndSubsistenceHiddenText}",
            travelSubsistenceBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.travelAndSubsistenceAmount, "£9", 9, 3, s"${common.changeText} ${specific.travelAndSubsistenceAmountHiddenText}",
            travelSubsistenceBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.personalCosts, common.yes, 9, 4, s"${common.changeText} ${specific.personalCostsHiddenText}", incidentalOvernightCostsBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.personalCostsAmount, "£10", 9, 5, s"${common.changeText} ${specific.personalCostsAmountHiddenText}",
            incidentalOvernightCostsBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.entertainment, common.yes, 9, 6, s"${common.changeText} ${specific.entertainmentHiddenText}", entertainmentExpensesBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.entertainmentAmount, "£11", 9, 7, s"${common.changeText} ${specific.entertainmentAmountHiddenText}",
            entertainmentExpensesBenefitsAmountUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(10))
          changeAmountRowCheck(common.utilitiesSubheading, common.yes, 11, 1, s"${common.changeText} ${specific.utilitiesSubheadingHiddenText}",
            utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.telephone, common.yes, 11, 2, s"${common.changeText} ${specific.telephoneHiddenText}", telephoneBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.telephoneAmount, "£12", 11, 3, s"${common.changeText} ${specific.telephoneAmountHiddenText}", telephoneBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.servicesProvided, common.yes, 11, 4, s"${common.changeText} ${specific.servicesProvidedHiddenText}",
            employerProvidedServicesBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.servicesProvidedAmount, "£13", 11, 5, s"${common.changeText} ${specific.servicesProvidedAmountHiddenText}",
            employerProvidedServicesBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.profSubscriptions, common.yes, 11, 6, s"${common.changeText} ${specific.profSubscriptionsHiddenText}",
            professionalFeesOrSubscriptionsBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.profSubscriptionsAmount, "£14", 11, 7, s"${common.changeText} ${specific.profSubscriptionsAmountHiddenText}",
            professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.otherServices, common.yes, 11, 8, s"${common.changeText} ${specific.otherServicesHiddenText}", otherServicesBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.otherServicesAmount, "£15", 11, 9, s"${common.changeText} ${specific.otherServicesAmountHiddenText}",
            otherServicesBenefitsAmountUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.medicalHeader, fieldHeaderSelector(12))
          changeAmountRowCheck(common.medicalSubheading, common.yes, 13, 1, s"${common.changeText} ${specific.medicalSubheadingHiddenText}",
            medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.medicalIns, common.yes, 13, 2, s"${common.changeText} ${specific.medicalInsHiddenText}", medicalDentalBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.medicalInsAmount, "£16", 13, 3, s"${common.changeText} ${specific.medicalInsAmountHiddenText}", medicalDentalBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.nursery, common.yes, 13, 4, s"${common.changeText} ${specific.nurseryHiddenText}", childcareBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.nurseryAmount, "£17", 13, 5, s"${common.changeText} ${specific.nurseryAmountHiddenText}", childcareBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.educational, common.yes, 13, 6, s"${common.changeText} ${specific.educationalHiddenText}", educationalServicesBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.educationalAmount, "£19", 13, 7, s"${common.changeText} ${specific.educationalAmountHiddenText}",
            educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.beneficialLoans, common.yes, 13, 8, s"${common.changeText} ${specific.beneficialLoansHiddenText}", beneficialLoansBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.beneficialLoansAmount, "£18", 13, 9, s"${common.changeText} ${specific.beneficialLoansAmountHiddenText}",
            beneficialLoansBenefitsAmountUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(14))
          changeAmountRowCheck(common.incomeTaxSubheading, common.yes, 15, 1, s"${common.changeText} ${specific.incomeTaxSubheadingHiddenText}",
            incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.incomeTaxPaid, common.yes, 15, 2, s"${common.changeText} ${specific.incomeTaxPaidHiddenText}", incomeTaxBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.incomeTaxPaidAmount, "£20", 15, 3, s"${common.changeText} ${specific.incomeTaxPaidAmountHiddenText}", incomeTaxBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.incurredCostsPaid, common.yes, 15, 4, s"${common.changeText} ${specific.incurredCostsPaidHiddenText}", incurredCostsBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.incurredCostsPaidAmount, "£21", 15, 5, s"${common.changeText} ${specific.incurredCostsPaidAmountHiddenText}",
            incurredCostsBenefitsAmountUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(16))
          changeAmountRowCheck(common.reimbursedSubheading, common.yes, 17, 1, s"${common.changeText} ${specific.reimbursedSubheadingHiddenText}",
            reimbursedCostsBenefitsUrl(taxYearEOY, employmentId))

          changeAmountRowCheck(common.nonTaxable, common.yes, 17, 2, s"${common.changeText} ${specific.nonTaxableHiddenText}", nonTaxableCostsBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.nonTaxableAmount, "£22", 17, 3, s"${common.changeText} ${specific.nonTaxableAmountHiddenText}", nonTaxableCostsBenefitsAmountUrl(taxYearEOY, employmentId))

          changeAmountRowCheck(common.taxableCosts, common.yes, 17, 4, s"${common.changeText} ${specific.taxableCostsHiddenText}", taxableCostsBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.taxableCostsAmount, "£23", 17, 5, s"${common.changeText} ${specific.taxableCostsAmountHiddenText}", taxableCostsBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.vouchers, common.yes, 17, 6, s"${common.changeText} ${specific.vouchersHiddenText}", vouchersOrCreditCardsBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.vouchersAmount, "£24", 17, 7, s"${common.changeText} ${specific.vouchersAmountHiddenText}", vouchersOrCreditCardsBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.nonCash, common.yes, 17, 8, s"${common.changeText} ${specific.nonCashHiddenText}", nonCashBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.nonCashAmount, "£25", 17, 9, s"${common.changeText} ${specific.nonCashAmountHiddenText}", nonCashBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.otherBenefits, common.yes, 17, 10, s"${common.changeText} ${specific.otherBenefitsHiddenText}", otherBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.otherBenefitsAmount, "£26", 17, 11, s"${common.changeText} ${specific.otherBenefitsAmountHiddenText}", otherBenefitsAmountUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.assetsHeader, fieldHeaderSelector(18), "for section")
          changeAmountRowCheck(common.assetsSubheading, common.yes, 19, 1, s"${common.changeText} ${specific.assetsSubheadingHiddenText}", assetsBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.assets, common.yes, 19, 2, s"${common.changeText} ${specific.assetsHiddenText}", assetsForUseBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.assetsAmount, "£27", 19, 3, s"${common.changeText} ${specific.assetsAmountHiddenText}", assetsForUseBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.assetTransfers, common.yes, 19, 4, s"${common.changeText} ${specific.assetTransfersHiddenText}", assetsToKeepBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.assetTransfersAmount, "£280,000", 19, 5, s"${common.changeText} ${specific.assetTransfersAmountHiddenText}",
            assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId))

          buttonCheck(common.saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }

        "return only the relevant data on the page when only certain data items are in mongodb for EOY" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val employmentSources = Seq(anEmploymentSource.copy(employmentBenefits = filteredBenefits))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = employmentSources))), nino, taxYear - 1)
            urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYear - 1))
          textOnPageCheck(specific.expectedP1, Selectors.p1)

          changeAmountRowCheck(common.benefitsReceived, common.yes, 3, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}", companyBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(4))

          changeAmountRowCheck(common.carSubheading, common.yes, 5, 1, s"${common.changeText} ${specific.carSubheadingHiddenText}", carVanFuelBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.companyCar, common.no, 5, 2, s"${common.changeText} ${specific.companyCarHiddenText}", carBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.fuelForCompanyCar, common.no, 5, 3, s"${common.changeText} ${specific.fuelForCompanyCarHiddenText}", carFuelBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.companyVan, common.yes, 5, 4, s"${common.changeText} ${specific.companyVanHiddenText}", vanBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.fuelForCompanyVan, common.yes, 5, 6, s"${common.changeText} ${specific.fuelForCompanyVanHiddenText}", vanFuelBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.companyVanAmount, "£3", 5, 5, s"${common.changeText} ${specific.companyVanAmountHiddenText}", vanBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.fuelForCompanyVanAmount, "£4", 5, 7, s"${common.changeText} ${specific.fuelForCompanyVanAmountHiddenText}",
            vanFuelBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.mileageBenefit, common.yes, 5, 8, s"${common.changeText} ${specific.mileageBenefitHiddenText}", mileageBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.mileageBenefitAmount, "£5", 5, 9, s"${common.changeText} ${specific.mileageBenefitAmountHiddenText}", mileageBenefitsAmountUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(6))
          changeAmountRowCheck(common.accommodationSubheading, common.no, 7, 1, s"${common.changeText} ${specific.accommodationSubheadingHiddenText}",
            accommodationRelocationBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.travelHeader, fieldHeaderSelector(8))
          changeAmountRowCheck(common.travelSubheading, common.no, 9, 1, s"${common.changeText} ${specific.travelSubheadingHiddenText}", travelOrEntertainmentBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(10))
          changeAmountRowCheck(common.utilitiesSubheading, common.no, 11, 1, s"${common.changeText} ${specific.utilitiesSubheadingHiddenText}",
            utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.medicalHeader, fieldHeaderSelector(12))
          changeAmountRowCheck(common.medicalSubheading, common.no, 13, 1, s"${common.changeText} ${specific.medicalSubheadingHiddenText}",
            medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(14))
          changeAmountRowCheck(common.incomeTaxSubheading, common.no, 15, 1, s"${common.changeText} ${specific.incomeTaxSubheadingHiddenText}",
            incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(16))
          changeAmountRowCheck(common.reimbursedSubheading, common.no, 17, 1, s"${common.changeText} ${specific.reimbursedSubheadingHiddenText}",
            reimbursedCostsBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.assetsHeader, fieldHeaderSelector(18), "for section")
          changeAmountRowCheck(common.assetsSubheading, common.no, 19, 1, s"${common.changeText} ${specific.assetsSubheadingHiddenText}", assetsBenefitsUrl(taxYearEOY, employmentId))

          buttonCheck(common.saveAndContinue)

          welshToggleCheck(user.isWelsh)

          s"should not display the following values" in {
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
            EmploymentUserData(sessionId, mtditid, nino, taxYear - 1, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

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
            urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYear - 1))

          changeAmountRowCheck(common.benefitsReceived, common.yes, 2, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}", companyBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(3))

          changeAmountRowCheck(common.carSubheading, common.no, 4, 1, s"${common.changeText} ${specific.carSubheadingHiddenText}", carVanFuelBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.accommodationHeader, fieldHeaderSelector(5))
          changeAmountRowCheck(common.accommodationSubheading, common.yes, 6, 1, s"${common.changeText} ${specific.accommodationSubheadingHiddenText}",
            accommodationRelocationBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.accommodation, common.yes, 6, 2, s"${common.changeText} ${specific.accommodationHiddenText}", livingAccommodationBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.accommodationAmount, "£3", 6, 3, s"${common.changeText} ${specific.accommodationAmountHiddenText}",
            livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.qualifyingRelocationCosts, common.no, 6, 4, s"${common.changeText} ${specific.qualifyingRelocationCostsHiddenText}",
            qualifyingRelocationBenefitsUrl(taxYearEOY, employmentId))
          changeAmountRowCheck(common.nonQualifyingRelocationCosts, common.no, 6, 5, s"${common.changeText} ${specific.nonQualifyingRelocationCostsHiddenText}",
            nonQualifyingRelocationBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.travelHeader, fieldHeaderSelector(7))
          changeAmountRowCheck(common.travelSubheading, common.no, 8, 1, s"${common.changeText} ${specific.travelSubheadingHiddenText}", travelOrEntertainmentBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.utilitiesHeader, fieldHeaderSelector(9))
          changeAmountRowCheck(common.utilitiesSubheading, common.no, 10, 1, s"${common.changeText} ${specific.utilitiesSubheadingHiddenText}",
            utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.medicalHeader, fieldHeaderSelector(11))
          changeAmountRowCheck(common.medicalSubheading, common.no, 12, 1, s"${common.changeText} ${specific.medicalSubheadingHiddenText}",
            medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.incomeTaxHeader, fieldHeaderSelector(13))
          changeAmountRowCheck(common.incomeTaxSubheading, common.no, 14, 1, s"${common.changeText} ${specific.incomeTaxSubheadingHiddenText}",
            incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.reimbursedHeader, fieldHeaderSelector(15))
          changeAmountRowCheck(common.reimbursedSubheading, common.no, 16, 1, s"${common.changeText} ${specific.reimbursedSubheadingHiddenText}",
            reimbursedCostsBenefitsUrl(taxYearEOY, employmentId))

          textOnPageCheck(common.assetsHeader, fieldHeaderSelector(17), "for section")
          changeAmountRowCheck(common.assetsSubheading, common.no, 18, 1, s"${common.changeText} ${specific.assetsSubheadingHiddenText}", assetsBenefitsUrl(taxYearEOY, employmentId))

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
            EmploymentUserData(sessionId, mtditid, nino, taxYear - 1, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

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
            urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYear - 1))
          textOnPageCheck(specific.expectedP1, Selectors.p1)

          changeAmountRowCheck(common.benefitsReceived, common.no, 3, 1, s"${common.changeText} ${specific.benefitsReceivedHiddenText}", companyBenefitsUrl(taxYearEOY, employmentId))

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

        "return only the relevant data on the page when only certain data items are in mongodb and in year" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val employmentSources = Seq(anEmploymentSource.copy(employmentBenefits = filteredBenefits))
            userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = employmentSources))), nino, taxYear)
            urlGet(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption())
          textOnPageCheck(specific.expectedP1, Selectors.p1)
          textOnPageCheck(specific.expectedP2(), Selectors.p2)

          textOnPageCheck(common.benefitsReceived, fieldNameSelector(4, 1))
          textOnPageCheck("Yes", fieldAmountSelector(4, 1), "for benefits question")
          textOnPageCheck(common.vehicleHeader, fieldHeaderSelector(5))
          textOnPageCheck(common.carSubheading, fieldNameSelector(6, 1))
          textOnPageCheck("Yes", fieldAmountSelector(6, 1), "for vehicles section question")
          textOnPageCheck(common.companyCar, fieldNameSelector(6, 2))
          textOnPageCheck("No", fieldAmountSelector(6, 2), "for carQuestion")
          textOnPageCheck(common.fuelForCompanyCar, fieldNameSelector(6, 3))
          textOnPageCheck("No", fieldAmountSelector(6, 3), "for carFuelQuestion")
          textOnPageCheck(common.companyVan, fieldNameSelector(6, 4))
          textOnPageCheck("Yes", fieldAmountSelector(6, 4), "for vanQuestion")
          textOnPageCheck(common.companyVanAmount, fieldNameSelector(6, 5))
          textOnPageCheck("£3", fieldAmountSelector(6, 5))
          textOnPageCheck(common.fuelForCompanyVan, fieldNameSelector(6, 6))
          textOnPageCheck("Yes", fieldAmountSelector(6, 6), "for vanFuelQuestion")
          textOnPageCheck(common.fuelForCompanyVanAmount, fieldNameSelector(6, 7))
          textOnPageCheck("£4", fieldAmountSelector(6, 7))
          textOnPageCheck(common.mileageBenefit, fieldNameSelector(6, 8))
          textOnPageCheck("Yes", fieldAmountSelector(6, 8), "for mileageQuestion")
          textOnPageCheck(common.mileageBenefitAmount, fieldNameSelector(6, 9))
          textOnPageCheck("£5", fieldAmountSelector(6, 9))

          buttonCheck(common.returnToEmploymentSummaryText, Selectors.returnToEmploymentSummarySelector)

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
            document().body().toString.contains(common.assetsHeader) shouldBe false
            document().body().toString.contains(common.assetTransfers) shouldBe false
          }
        }

        "render Unauthorised user error page when the user is unauthorized" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), welsh = user.isWelsh)
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
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = employmentSources))), nino, taxYear)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, "0022")), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYearEOY)) shouldBe true
    }

    "redirect to the Did your client receive any benefits page when its EOY and theres no benefits model in the session data" in {
      def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
        EmploymentUserData(sessionId, mtditid, nino, taxYear - 1, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

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
        urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(companyBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }

    "redirect to the Did your client receive any benefits page when its EOY and theres no benefits model in the mongodb data" in {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None)))
        userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, taxYear)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(companyBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }

    "redirect to overview page when theres no benefits and in year" in {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val hmrcEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = hmrcEmploymentData))), nino, taxYear)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }
  }


  ".submit" when {
    "return a redirect when in year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = true)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlPost(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "return internal server error page whilst not implemented" in {
      val employmentData = anEmploymentCYAModel.copy(employmentBenefits = None)
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = employmentData).copy(employmentId = employmentId), aUserRequest)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return a redirect to show method when at end of year and no cya data" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has a url of benefits show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "create a model when adding employment benefits for the first time (adding new employment journey)" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val customerEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(customerEmploymentData = customerEmploymentData))), nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(hasPriorBenefits = false, employment = anEmploymentCYAModel).copy(employmentId = employmentId), aUserRequest)

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
          None,
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))).get,
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))).get
              ),
              deductions = Some(Deductions(Some(aStudentLoans))),
              benefitsInKind = anEmploymentCYAModel.employmentBenefits.map(_.toBenefits)
            )
          )
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "return a redirect to the check employment expenses page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        getSessionMap(result, "mdtp").get("TEMP_NEW_EMPLOYMENT_ID") shouldBe Some(employmentId)
      }
    }

    "update model and return a redirect when the user has prior benefits" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)

        val customerEmploymentData = Seq(anEmploymentSource.copy(employmentData = anEmploymentSource.employmentData.map(_.copy(
          pay = Some(anEmploymentSource.employmentData.flatMap(_.pay).get.copy(taxablePayToDate = Some(34786788.77), totalTaxToDate = Some(35553311.89))
          )))))

        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(customerEmploymentData = customerEmploymentData))), nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel, hasPriorBenefits = true), aUserRequest)

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
          None,
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                34786788.77, 35553311.89
              ),
              deductions = Some(Deductions(Some(aStudentLoans))),
              benefitsInKind = Some(anEmploymentCYAModel.employmentBenefits.get.toBenefits)
            )
          )
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the employment summary page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
      }
    }
    "return a redirect when the user has prior benefits but is removing them" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)

        val customerEmploymentData = Seq(anEmploymentSource.copy(employmentData = anEmploymentSource.employmentData.map(_.copy(
          pay = Some(anEmploymentSource.employmentData.flatMap(_.pay).get.copy(taxablePayToDate = Some(34786788.77), totalTaxToDate = Some(35553311.89))
          )))))

        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(customerEmploymentData = customerEmploymentData))), nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = Some(
          BenefitsViewModel(
            isUsingCustomerData = true, isBenefitsReceived = true
          )
        )), hasPriorBenefits = true), aUserRequest)

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
          None,
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                34786788.77, 35553311.89
              ),
              deductions = Some(Deductions(Some(aStudentLoans))),
              benefitsInKind = None
            )
          )
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the employment summary page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
      }
    }
  }
}