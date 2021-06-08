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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class CheckYourBenefitsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach {

  val defaultTaxYear = 2022
  val url = s"${appUrl(port)}/$defaultTaxYear/check-employment-benefits?employmentId=001"

  private def fieldNameSelector(section: Int, row: Int) = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dt"

  private def fieldAmountSelector(section: Int, row: Int) = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd"

  private def fieldHeaderSelector(i: Int) = s"#main-content > div > div > p:nth-child($i)"

  object Selectors {
    val p1 = "#main-content > div > div > p.govuk-body"
    val p2 = "#main-content > div > div > div.govuk-inset-text"
  }

  object ExpectedResults {

    object ContentEN {
      val titleIndividual = "Check your employment benefits"
      val titleAgent = "Check your client’s employment benefits"
      val headingIndividual = "Check your employment benefits"
      val headingAgent = "Check your client’s employment benefits"
      val caption = "Employment for 6 April 2021 to 5 April 2022"
      val p1Individual = "Your employment benefits are based on the information we already hold about you."
      val p1Agent = "Your client’s employment benefits are based on the information we already hold about them."
      val p2Individual = s"You cannot update your employment benefits until 6 April $defaultTaxYear."
      val p2Agent = s"You cannot update your client’s employment benefits until 6 April $defaultTaxYear."
      val vehicleHeader = "Vehicles, fuel and mileage"
      val companyCar = "Company car"
      val fuelForCompanyCar = "Fuel for company car"
      val companyVan = "Company van"
      val fuelForCompanyVan = "Fuel for company van"
      val mileageBenefit = "Mileage benefit"
      val accommodationHeader = "Accommodation and relocation"
      val accommodation = "Accommodation"
      val qualifyingRelocationCosts = "Qualifying relocation costs"
      val nonQualifyingRelocationCosts = "Non qualifying relocation costs"
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
      val nonTaxable = "Non taxable costs reimbursed by employer"
      val taxableCosts = "Taxable costs reimbursed by employer"
      val vouchers = "Vouchers, credit cards or excess mileage allowance"
      val nonCash = "Non cash benefits"
      val otherBenefits = "Other benefits"
      val assetsHeader = "Assets and asset transfers"
      val assets = "Assets"
      val assetTransfers = "Asset transfers"
    }

    object ContentCY {
      val titleIndividual = "Check your employment benefits"
      val titleAgent = "Check your client’s employment benefits"
      val headingIndividual = "Check your employment benefits"
      val headingAgent = "Check your client’s employment benefits"
      val caption = "Employment for 6 April 2021 to 5 April 2022"
      val p1Individual = "Your employment benefits are based on the information we already hold about you."
      val p1Agent = "Your client’s employment benefits are based on the information we already hold about them."
      val p2Individual = s"You cannot update your employment benefits until 6 April $defaultTaxYear."
      val p2Agent = s"You cannot update your client’s employment benefits until 6 April $defaultTaxYear."
      val vehicleHeader = "Vehicles, fuel and mileage"
      val companyCar = "Company car"
      val fuelForCompanyCar = "Fuel for company car"
      val companyVan = "Company van"
      val fuelForCompanyVan = "Fuel for company van"
      val mileageBenefit = "Mileage benefit"
      val accommodationHeader = "Accommodation and relocation"
      val accommodation = "Accommodation"
      val qualifyingRelocationCosts = "Qualifying relocation costs"
      val nonQualifyingRelocationCosts = "Non qualifying relocation costs"
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
      val nonTaxable = "Non taxable costs reimbursed by employer"
      val taxableCosts = "Taxable costs reimbursed by employer"
      val vouchers = "Vouchers, credit cards or excess mileage allowance"
      val nonCash = "Non cash benefits"
      val otherBenefits = "Other benefits"
      val assetsHeader = "Assets and asset transfers"
      val assets = "Assets"
      val assetTransfers = "Asset transfers"
    }
  }

  "in english" when {

    import ExpectedResults.ContentEN._

    "calling GET" when {

      "an individual" should {

        "return a fully populated page when all the fields are populated" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, defaultTaxYear)
            urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleIndividual)
          captionCheck(caption)
          h1Check(headingIndividual)
          textOnPageCheck(p1Individual, Selectors.p1)
          textOnPageCheck(p2Individual, Selectors.p2)
          textOnPageCheck(vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
          textOnPageCheck(incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))

          welshToggleCheck(ENGLISH)
        }

        "redirect to overview page when theres no benefits" in {

          lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(None)), nino, defaultTaxYear)
            urlGet(url, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }

        "return only the relevant data on the page when only certain data items are in mongo" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(filteredBenefits)), nino, defaultTaxYear)
            urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleIndividual)
          h1Check(headingIndividual)
          textOnPageCheck(p1Individual, Selectors.p1)
          textOnPageCheck(p2Individual, Selectors.p2)
          textOnPageCheck(vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(companyVan, fieldNameSelector(5, 1))
          textOnPageCheck("£3", fieldAmountSelector(5, 1))
          textOnPageCheck(fuelForCompanyVan, fieldNameSelector(5, 2))
          textOnPageCheck("£4", fieldAmountSelector(5, 2))
          textOnPageCheck(mileageBenefit, fieldNameSelector(5, 3))
          textOnPageCheck("£5", fieldAmountSelector(5, 3))

          welshToggleCheck(ENGLISH)

          s"should not display the following values" in {

            document().body().toString.contains(companyCar) shouldBe false
            document().body().toString.contains(fuelForCompanyCar) shouldBe false
            document().body().toString.contains(accommodationHeader) shouldBe false
            document().body().toString.contains(accommodation) shouldBe false
            document().body().toString.contains(qualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(nonQualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(travelHeader) shouldBe false
            document().body().toString.contains(travelAndSubsistence) shouldBe false
            document().body().toString.contains(personalCosts) shouldBe false
            document().body().toString.contains(entertainment) shouldBe false
            document().body().toString.contains(utilitiesHeader) shouldBe false
            document().body().toString.contains(telephone) shouldBe false
            document().body().toString.contains(servicesProvided) shouldBe false
            document().body().toString.contains(profSubscriptions) shouldBe false
            document().body().toString.contains(otherServices) shouldBe false
            document().body().toString.contains(medicalHeader) shouldBe false
            document().body().toString.contains(medicalIns) shouldBe false
            document().body().toString.contains(nursery) shouldBe false
            document().body().toString.contains(beneficialLoans) shouldBe false
            document().body().toString.contains(educational) shouldBe false
            document().body().toString.contains(incomeTaxHeader) shouldBe false
            document().body().toString.contains(incomeTaxPaid) shouldBe false
            document().body().toString.contains(incurredCostsPaid) shouldBe false
            document().body().toString.contains(reimbursedHeader) shouldBe false
            document().body().toString.contains(nonTaxable) shouldBe false
            document().body().toString.contains(taxableCosts) shouldBe false
            document().body().toString.contains(vouchers) shouldBe false
            document().body().toString.contains(nonCash) shouldBe false
            document().body().toString.contains(otherBenefits) shouldBe false
            document().body().toString.contains(assetsHeader) shouldBe false
            document().body().toString.contains(assets) shouldBe false
            document().body().toString.contains(assetTransfers) shouldBe false
          }
        }
      }

      "an agent" should {

        "return a fully populated page when all the fields are populated" which {

          lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, defaultTaxYear)
            urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleAgent)
          h1Check(headingAgent)
          textOnPageCheck(p1Agent, Selectors.p1)
          textOnPageCheck(p2Agent, Selectors.p2)
          textOnPageCheck(vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
          textOnPageCheck(incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))

          welshToggleCheck(ENGLISH)
        }
      }
    }
  }

  "in welsh" when {

    import ExpectedResults.ContentCY._

    "calling GET" when {

      "an individual" should {

        "return a fully populated page when all the fields are populated" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, defaultTaxYear)
            urlGet(url, welsh = true, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleIndividual)
          captionCheck(caption)
          h1Check(headingIndividual)
          textOnPageCheck(p1Individual, Selectors.p1)
          textOnPageCheck(p2Individual, Selectors.p2)
          textOnPageCheck(vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
          textOnPageCheck(incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))

          welshToggleCheck(WELSH)
        }

        "return only the relevant data on the page when only certain data items are in mongo" which {

          implicit lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(fullEmploymentsModel(filteredBenefits)), nino, defaultTaxYear)
            urlGet(url, welsh = true, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleIndividual)
          h1Check(headingIndividual)
          textOnPageCheck(p1Individual, Selectors.p1)
          textOnPageCheck(p2Individual, Selectors.p2)
          textOnPageCheck(vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(companyVan, fieldNameSelector(5, 1))
          textOnPageCheck("£3", fieldAmountSelector(5, 1))
          textOnPageCheck(fuelForCompanyVan, fieldNameSelector(5, 2))
          textOnPageCheck("£4", fieldAmountSelector(5, 2))
          textOnPageCheck(mileageBenefit, fieldNameSelector(5, 3))
          textOnPageCheck("£5", fieldAmountSelector(5, 3))

          welshToggleCheck(WELSH)

          s"should not display the following values" in {

            document().body().toString.contains(companyCar) shouldBe false
            document().body().toString.contains(fuelForCompanyCar) shouldBe false
            document().body().toString.contains(accommodationHeader) shouldBe false
            document().body().toString.contains(accommodation) shouldBe false
            document().body().toString.contains(qualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(nonQualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(travelHeader) shouldBe false
            document().body().toString.contains(travelAndSubsistence) shouldBe false
            document().body().toString.contains(personalCosts) shouldBe false
            document().body().toString.contains(entertainment) shouldBe false
            document().body().toString.contains(utilitiesHeader) shouldBe false
            document().body().toString.contains(telephone) shouldBe false
            document().body().toString.contains(servicesProvided) shouldBe false
            document().body().toString.contains(profSubscriptions) shouldBe false
            document().body().toString.contains(otherServices) shouldBe false
            document().body().toString.contains(medicalHeader) shouldBe false
            document().body().toString.contains(medicalIns) shouldBe false
            document().body().toString.contains(nursery) shouldBe false
            document().body().toString.contains(beneficialLoans) shouldBe false
            document().body().toString.contains(educational) shouldBe false
            document().body().toString.contains(incomeTaxHeader) shouldBe false
            document().body().toString.contains(incomeTaxPaid) shouldBe false
            document().body().toString.contains(incurredCostsPaid) shouldBe false
            document().body().toString.contains(reimbursedHeader) shouldBe false
            document().body().toString.contains(nonTaxable) shouldBe false
            document().body().toString.contains(taxableCosts) shouldBe false
            document().body().toString.contains(vouchers) shouldBe false
            document().body().toString.contains(nonCash) shouldBe false
            document().body().toString.contains(otherBenefits) shouldBe false
            document().body().toString.contains(assetsHeader) shouldBe false
            document().body().toString.contains(assets) shouldBe false
            document().body().toString.contains(assetTransfers) shouldBe false
          }
        }
      }

      "an agent" should {

        "return a fully populated page when all the fields are populated" which {

          lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(fullEmploymentsModel(fullBenefits)), nino, defaultTaxYear)
            urlGet(url, welsh = true, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(titleAgent)
          h1Check(headingAgent)
          textOnPageCheck(p1Agent, Selectors.p1)
          textOnPageCheck(p2Agent, Selectors.p2)
          textOnPageCheck(vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
          textOnPageCheck(incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))

          welshToggleCheck(WELSH)
        }
      }
    }
  }

}