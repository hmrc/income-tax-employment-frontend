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
import models.employment.EmploymentBenefits
import models.mongo.EmploymentCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CheckYourBenefitsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper{

  val defaultTaxYear = 2022
  def url(taxYear: Int = defaultTaxYear) = s"$appUrl/$taxYear/check-employment-benefits?employmentId=001"

  object Selectors {
    val p1 = "#main-content > div > div > p.govuk-body"
    val p2 = "#main-content > div > div > div.govuk-inset-text"
    def fieldNameSelector(section: Int, row: Int) = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dt"
    def fieldAmountSelector(section: Int, row: Int) = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd"
    def fieldHeaderSelector(i: Int) = s"#main-content > div > div > p:nth-child($i)"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedP1: String
    def expectedP2(year: Int = defaultTaxYear): String
  }

  trait CommonExpectedResults {
    def expectedCaption(year: Int = defaultTaxYear): String
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
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(year: Int = defaultTaxYear) = s"Employment for 6 April ${year-1} to 5 April $year"
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
    val medicalHeader = "Medical insurance, nursery, loans and education"
    val medicalIns = "Medical insurance"
    val nursery = "Nursery places"
    val beneficialLoans = "Beneficial loans"
    val educational = "Educational services"
    val incomeTaxHeader = "Income tax and incurred costs"
    val incomeTaxPaid = "Income tax paid by employer"
    val incurredCostsPaid = "Incurred costs paid by employer"
    val reimbursedHeader = "Reimbursed costs, vouchers, and non-cash benefits"
    val nonTaxable = "Non-taxable costs reimbursed by employer"
    val taxableCosts = "Taxable costs reimbursed by employer"
    val vouchers = "Vouchers, credit cards or excess mileage allowance"
    val nonCash = "Non-cash benefits"
    val otherBenefits = "Other benefits"
    val assetsHeader = "Assets and asset transfers"
    val assets = "Assets"
    val assetTransfers = "Asset transfers"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(year: Int = defaultTaxYear) = s"Employment for 6 April ${year-1} to 5 April $year"
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
    val medicalHeader = "Medical insurance, nursery, loans and education"
    val medicalIns = "Medical insurance"
    val nursery = "Nursery places"
    val beneficialLoans = "Beneficial loans"
    val educational = "Educational services"
    val incomeTaxHeader = "Income tax and incurred costs"
    val incomeTaxPaid = "Income tax paid by employer"
    val incurredCostsPaid = "Incurred costs paid by employer"
    val reimbursedHeader = "Reimbursed costs, vouchers, and non-cash benefits"
    val nonTaxable = "Non-taxable costs reimbursed by employer"
    val taxableCosts = "Taxable costs reimbursed by employer"
    val vouchers = "Vouchers, credit cards or excess mileage allowance"
    val nonCash = "Non-cash benefits"
    val otherBenefits = "Other benefits"
    val assetsHeader = "Assets and asset transfers"
    val assets = "Assets"
    val assetTransfers = "Asset transfers"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "Check your employment benefits"
    val expectedTitle: String = "Check your employment benefits"
    val expectedP1: String = "Your employment benefits are based on the information we already hold about you."
    def expectedP2(year: Int = defaultTaxYear): String = s"You cannot update your employment benefits until 6 April $year."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "Check your client’s employment benefits"
    val expectedTitle: String = "Check your client’s employment benefits"
    val expectedP1: String = "Your client’s employment benefits are based on the information we already hold about them."
    def expectedP2(year: Int = defaultTaxYear): String = s"You cannot update your client’s employment benefits until 6 April $year."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Check your employment benefits"
    val expectedTitle: String = "Check your employment benefits"
    val expectedP1: String = "Your employment benefits are based on the information we already hold about you."
    def expectedP2(year: Int = defaultTaxYear): String = s"You cannot update your employment benefits until 6 April $year."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Check your client’s employment benefits"
    val expectedTitle: String = "Check your client’s employment benefits"
    val expectedP1: String = "Your client’s employment benefits are based on the information we already hold about them."
    def expectedP2(year: Int = defaultTaxYear): String = s"You cannot update your client’s employment benefits until 6 April $year."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return a fully populated page when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear)
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
          textOnPageCheck(user.commonExpectedResults.beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(user.commonExpectedResults.educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
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
            userDataStub(userData(fullEmploymentsModel(Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear-1)
            urlGet(url(defaultTaxYear-1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(defaultTaxYear  -1))
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, Selectors.p1)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2(defaultTaxYear - 1), Selectors.p2)
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
          textOnPageCheck(user.commonExpectedResults.beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(user.commonExpectedResults.educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
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

        "return a redirect at the end of the year when id is not found" in {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear-1)
            urlGet(s"$appUrl/${taxYear-1}/check-employment-benefits?employmentId=0022", welsh = user.isWelsh, follow=false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2021/view")
        }

        "return a fully populated page when all the fields are populated when at the end of the year when there is CYA data" which {

          val employmentData: EmploymentCYAModel = {
            employmentUserData.employment.copy(employmentDetails = employmentUserData.employment.employmentDetails.copy(
              employerRef = Some(
                "123/12345"
              ),
              startDate = Some("2020-11-11"),
              taxablePayToDate= Some(55.99),
              totalTaxToDate= Some(3453453.00),
              currentDataIsHmrcHeld = false
            ))
          }

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData.copy(employment = employmentData.copy(employmentBenefits = fullBenefits)).copy(employmentId = "001"),userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear-1)
            urlGet(url(defaultTaxYear-1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(defaultTaxYear  -1))
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, Selectors.p1)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2(defaultTaxYear - 1), Selectors.p2)
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
          textOnPageCheck(user.commonExpectedResults.beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(user.commonExpectedResults.educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
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

        "return a fully populated page when all the fields are populated when at the end of the year when there is CYA data but no benefits" which {

          val employmentData: EmploymentCYAModel = {
            employmentUserData.employment.copy(employmentDetails = employmentUserData.employment.employmentDetails.copy(
              employerRef = Some(
                "123/12345"
              ),
              startDate = Some("2020-11-11"),
              taxablePayToDate= Some(55.99),
              totalTaxToDate= Some(3453453.00),
              currentDataIsHmrcHeld = false
            ))
          }

          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData.copy(employment = employmentData).copy(employmentId = "001"),userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear-1)
            urlGet(url(defaultTaxYear-1), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(defaultTaxYear  -1))
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, Selectors.p1)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2(defaultTaxYear - 1), Selectors.p2)
          welshToggleCheck(user.isWelsh)
        }

        "redirect to overview page when theres no benefits" in {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, defaultTaxYear)
            urlGet(url(), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }


        "return only the relevant data on the page when only certain data items are in mongo" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(Seq(employmentDetailsAndBenefits(filteredBenefits)))), nino, defaultTaxYear)
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

            document().body().toString.contains(user.commonExpectedResults.companyCar) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.fuelForCompanyCar) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.accommodationHeader) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.accommodation) shouldBe false
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
            document().body().toString.contains(user.commonExpectedResults.medicalIns) shouldBe false
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
            document().body().toString.contains(user.commonExpectedResults.assets) shouldBe false
            document().body().toString.contains(user.commonExpectedResults.assetTransfers) shouldBe false
          }
        }

        "render Unauthorised user error page" which {
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
              taxablePayToDate= Some(55.99),
              totalTaxToDate= Some(3453453.00),
              currentDataIsHmrcHeld = false
            ))
          }
          val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData.copy(employment = employmentData).copy(employmentId = "001"),userRequest)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear-1)
            urlPost(url(taxYear-1), body = "{}", welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          result.status shouldBe INTERNAL_SERVER_ERROR
        }
        "return a redirect to show method when at end of year" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear-1)
            urlPost(url(taxYear-1), body = "{}", welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear-1)))
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