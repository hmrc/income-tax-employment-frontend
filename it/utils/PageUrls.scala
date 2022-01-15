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

package utils

//scalastyle:off number.of.methods
object PageUrls extends IntegrationTest {

  def fullUrl(endOfUrl: String): String = s"http://localhost:$port" + endOfUrl
  override lazy val appUrl = "/update-and-submit-income-tax-return/employment-income"

  //  *****************       Overview page      *****************************************

  def overviewUrl(taxYear: Int): String = s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"
  def startUrl(taxYear: Int): String = s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/start"
  val tryAnotherExpectedHref = "http://localhost:11111/report-quarterly/income-and-expenses/view/agents/client-utr"

  //  *****************       External pages      *****************************************

  val exemptLink: String = "https://www.gov.uk/expenses-and-benefits-childcare/whats-exempt"
  val taxReliefLink = "https://www.gov.uk/tax-relief-for-employees"
  val professionalFeesLink = "https://www.gov.uk/tax-relief-for-employees/professional-fees-and-subscriptions"
  val uniformsAndToolsLink = "https://www.gov.uk/guidance/job-expenses-for-uniforms-work-clothing-and-tools"
  val authoriseAsAnAgentLink = "https://www.gov.uk/guidance/client-authorisation-an-overview"
  val signUpForMTDLink: String = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax"
  val selfAssessmentLink: String = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
  val incomeTaxHomePageLink = "https://www.gov.uk/income-tax"
  val createAnAgentLink = "https://www.gov.uk/guidance/get-an-hmrc-agent-services-account"

  //  *****************       Summary pages      *****************************************

  def employmentSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/employment-summary"
  def employerInformationUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/employer-information?employmentId=$employmentId"

//  *****************       Check your answers pages      ******************************

  def checkYourDetailsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/check-employment-details?employmentId=$employmentId"
  def checkYourBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/check-employment-benefits?employmentId=$employmentId"
  def checkYourExpensesUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/check-employment-expenses"

//  *****************       Employment details pages      ******************************

  def employerNameUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/employer-name?employmentId=$employmentId"
  def employerNameUrlWithoutEmploymentId(taxYear: Int): String = s"$appUrl/$taxYear/employer-name?employmentId="
  def employerPayeReferenceUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/employer-paye-reference?employmentId=$employmentId"
  def employmentStartDateUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/employment-start-date?employmentId=$employmentId"
  def stillWorkingForUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/still-working-for-employer?employmentId=$employmentId"
  def employmentEndDateUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/employment-end-date?employmentId=$employmentId"
  def payrollIdUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/payroll-id?employmentId=$employmentId"
  def howMuchPayUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/how-much-pay?employmentId=$employmentId"
  def howMuchTaxUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/uk-tax?employmentId=$employmentId"
  def employmentDatesUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/employment-dates?employmentId=$employmentId"

//  *****************       Employment management pages      ***************************

  def addEmploymentUrl(taxYear: Int): String = s"$appUrl/$taxYear/add-employment"
  def removeEmploymentUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/remove-employment?employmentId=$employmentId"

  //  *****************       Employment benefits pages      *****************************
  
  def companyBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/company-benefits?employmentId=$employmentId"

  //  *****************       Car van fuel benefits pages      ***************************
  
  def carVanFuelBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/car-van-fuel?employmentId=$employmentId"
  def carBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/company-car?employmentId=$employmentId"
  def carBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/company-car-amount?employmentId=$employmentId"
  def carFuelBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/car-fuel?employmentId=$employmentId"
  def carFuelBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/car-fuel-amount?employmentId=$employmentId"
  def vanBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/company-van?employmentId=$employmentId"
  def vanBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/company-van-amount?employmentId=$employmentId"
  def vanFuelBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/van-fuel?employmentId=$employmentId"
  def vanFuelBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/van-fuel-amount?employmentId=$employmentId"
  def mileageBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/mileage?employmentId=$employmentId"
  def mileageBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/mileage-amount?employmentId=$employmentId"

//  *****************       Accommodation relocation benefits pages      ***************

  def accommodationRelocationBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/accommodation-relocation?employmentId=$employmentId"
  def livingAccommodationBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/living-accommodation?employmentId=$employmentId"
  def livingAccommodationBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/living-accommodation-amount?employmentId=$employmentId"
  def qualifyingRelocationBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/qualifying-relocation?employmentId=$employmentId"
  def qualifyingRelocationBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/qualifying-relocation-amount?employmentId=$employmentId"
  def nonQualifyingRelocationBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/non-qualifying-relocation?employmentId=$employmentId"
  def nonQualifyingRelocationBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/non-qualifying-relocation-amount?employmentId=$employmentId"

//  *****************       Travel entertainment benefits pages      *******************

  def travelOrEntertainmentBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/travel-entertainment?employmentId=$employmentId"
  def travelSubsistenceBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/travel-subsistence?employmentId=$employmentId"
  def travelSubsistenceBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/travel-subsistence-amount?employmentId=$employmentId"
  def incidentalOvernightCostsBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/incidental-overnight-costs?employmentId=$employmentId"
  def incidentalOvernightCostsBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/incidental-overnight-costs-amount?employmentId=$employmentId"
  def entertainmentExpensesBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/entertainment-expenses?employmentId=$employmentId"
  def entertainmentExpensesBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/entertainment-expenses-amount?employmentId=$employmentId"

//  *****************       Utilities benefits pages      ******************************

  def utilitiesOrGeneralServicesBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/utility-general-service?employmentId=$employmentId"
  def telephoneBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/telephone?employmentId=$employmentId"
  def telephoneBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/telephone-amount?employmentId=$employmentId"
  def employerProvidedServicesBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/employer-provided-services?employmentId=$employmentId"
  def employerProvidedServicesBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/employer-provided-services-amount?employmentId=$employmentId"
  def professionalFeesOrSubscriptionsBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/professional-fees-or-subscriptions?employmentId=$employmentId"
  def professionalFeesOrSubscriptionsBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/professional-fees-or-subscriptions-amount?employmentId=$employmentId"
  def otherServicesBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/other-services?employmentId=$employmentId"
  def otherServicesBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/other-services-amount?employmentId=$employmentId"

//  *****************       Medical benefits pages      ********************************

  def medicalDentalChildcareLoansBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/medical-dental-childcare-education-loans?employmentId=$employmentId"
  def medicalDentalBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/medical-dental?employmentId=$employmentId"
  def medicalDentalBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/medical-dental-amount?employmentId=$employmentId"
  def childcareBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/childcare?employmentId=$employmentId"
  def childcareBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/childcare-amount?employmentId=$employmentId"
  def educationalServicesBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/educational-services?employmentId=$employmentId"
  def educationalServicesBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/educational-services-amount?employmentId=$employmentId"
  def beneficialLoansBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/beneficial-loans?employmentId=$employmentId"
  def beneficialLoansBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/beneficial-loans-amount?employmentId=$employmentId"

//  *****************       Income tax benefits pages      *****************************

  def incomeTaxOrIncurredCostsBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/employer-income-tax-or-incurred-costs?employmentId=$employmentId"
  def incomeTaxBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/employer-income-tax?employmentId=$employmentId"
  def incomeTaxBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/employer-income-tax-amount?employmentId=$employmentId"
  def incurredCostsBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/incurred-costs?employmentId=$employmentId"
  def incurredCostsBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/incurred-costs-amount?employmentId=$employmentId"

//  *****************       Reimbursed costs benefits pages      ***********************

  def reimbursedCostsBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/reimbursed-costs-vouchers-non-cash-benefits?employmentId=$employmentId"
  def nonTaxableCostsBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/non-taxable-costs?employmentId=$employmentId"
  def nonTaxableCostsBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/non-taxable-costs-amount?employmentId=$employmentId"
  def taxableCostsBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/taxable-costs?employmentId=$employmentId"
  def taxableCostsBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/taxable-costs-amount?employmentId=$employmentId"
  def vouchersOrCreditCardsBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/vouchers-or-credit-cards?employmentId=$employmentId"
  def vouchersOrCreditCardsBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/vouchers-or-credit-cards-amount?employmentId=$employmentId"
  def nonCashBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/non-cash-benefits?employmentId=$employmentId"
  def nonCashBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/non-cash-benefits-amount?employmentId=$employmentId"
  def otherBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/other-benefits?employmentId=$employmentId"
  def otherBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/other-benefits-amount?employmentId=$employmentId"

//  *****************       Assets benefits pages      *********************************

  def assetsBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/assets?employmentId=$employmentId"
  def assetsForUseBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/assets-available-for-use?employmentId=$employmentId"
  def assetsForUseBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/assets-available-for-use-amount?employmentId=$employmentId"
  def assetsToKeepBenefitsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/assets-to-keep?employmentId=$employmentId"
  def assetsToKeepBenefitsAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/benefits/assets-to-keep-amount?employmentId=$employmentId"

//  *****************       Employment expenses pages      *****************************

  def claimEmploymentExpensesUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/claim-employment-expenses"

//  *****************       Employment expenses details pages      *********************

  def businessTravelExpensesUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/business-travel-and-overnight-expenses"
  def travelAmountExpensesUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/travel-amount"
  def uniformsWorkClothesToolsExpensesUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/uniforms-work-clothes-or-tools"
  def uniformsClothesToolsExpensesAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/amount-for-uniforms-work-clothes-or-tools"
  def professionalFeesExpensesUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/professional-fees-and-subscriptions"
  def professionalFeesExpensesAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/amount-for-professional-fees-and-subscriptions"
  def otherEquipmentExpensesUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/other-equipment"
  def otherEquipmentExpensesAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/expenses/amount-for-other-equipment"
  def startEmploymentExpensesUrl(taxYear: Int): String = s"$appUrl/$taxYear/employment/expenses/start-employment-expenses"

//  ***************************     Error pages     ************************************

  def youNeedToSignUpUrl: String = s"$appUrl/error/you-need-to-sign-up"
  def wrongTaxYearUrl: String = s"$appUrl/error/wrong-tax-year"
  def youNeedClientAuthUrl: String = s"$appUrl/error/you-need-client-authorisation "
  def notAuthorisedUrl: String = s"$appUrl/error/not-authorised-to-use-service"
  def youNeedAgentServicesUrl: String = s"$appUrl/error/you-need-agent-services-account"
  def timeoutUrl: String = s"$appUrl/timeout"

}
//scalastyle:on number.of.methods