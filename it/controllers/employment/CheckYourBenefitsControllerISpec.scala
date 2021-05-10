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

import common.SessionValues
import helpers.PlaySessionCookieBaker
import models.EmploymentBenefitsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}

class CheckYourBenefitsControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val defaultTaxYear = 2022
  val url =
    s"http://localhost:$port/income-through-software/return/employment-income/$defaultTaxYear/check-your-employment-benefits"

  val fullEmploymentBenefitsModel: EmploymentBenefitsModel = EmploymentBenefitsModel(
    car = Some(1.23),
    carFuel = Some(2.00),
    van = Some(3.00),
    vanFuel = Some(4.00),
    mileage = Some(5.00),
    accommodation = Some(6.00),
    qualifyingRelocationExpenses = Some(7.00),
    nonQualifyingRelocationExpenses = Some(8.00),
    travelAndSubsistence = Some(9.00),
    personalIncidentalExpenses = Some(10.00),
    entertaining = Some(11.00),
    telephone = Some(12.00),
    employerProvidedServices = Some(13.00),
    employerProvidedProfessionalSubscriptions = Some(14.00),
    service = Some(15.00),
    medicalInsurance = Some(16.00),
    nurseryPlaces = Some(17.00),
    beneficialLoan = Some(18.00),
    educationalServices = Some(19.00),
    incomeTaxPaidByDirector = Some(20.00),
    paymentsOnEmployeesBehalf = Some(21.00),
    expenses = Some(22.00),
    taxableExpenses = Some(23.00),
    vouchersAndCreditCards = Some(24.00),
    nonCash = Some(25.00),
    otherItems = Some(26.00),
    assets = Some(27.00),
    assetTransfer = Some(280000.00)
  )

  val filteredEmploymentBenefitsModel: EmploymentBenefitsModel = EmploymentBenefitsModel(
    van = Some(3.00),
    vanFuel = Some(4.00),
    mileage = Some(5.00),
  )

  private def fieldNameSelector(section: Int, row: Int) = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dt"

  private def fieldAmountSelector(section: Int, row: Int) = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd"

  private def fieldHeaderSelector(i: Int) = s"#main-content > div > div > p:nth-child($i)"

  object Selectors {
    val p1 = "#main-content > div > div > p.govuk-body"
    val p2 = "#main-content > div > div > div.govuk-inset-text"
  }

  object ExpectedResultsEn {
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

  object ExpectedResultsCy {
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


  "in english" when {
    "calling GET" when {

      "an individual" should {

        "return a fully populated page when all the fields are populated" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.BENEFITS_CYA -> Json.prettyPrint(Json.toJson(fullEmploymentBenefitsModel))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsEn.titleIndividual)
          captionCheck(ExpectedResultsEn.caption)
          h1Check(ExpectedResultsEn.headingIndividual)
          textOnPageCheck(ExpectedResultsEn.p1Individual, Selectors.p1)
          textOnPageCheck(ExpectedResultsEn.p2Individual, Selectors.p2)
          textOnPageCheck(ExpectedResultsEn.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(ExpectedResultsEn.companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(ExpectedResultsEn.fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(ExpectedResultsEn.companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(ExpectedResultsEn.fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(ExpectedResultsEn.mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(ExpectedResultsEn.accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(ExpectedResultsEn.accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(ExpectedResultsEn.qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(ExpectedResultsEn.nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(ExpectedResultsEn.travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(ExpectedResultsEn.travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(ExpectedResultsEn.personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(ExpectedResultsEn.entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(ExpectedResultsEn.utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(ExpectedResultsEn.telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(ExpectedResultsEn.servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(ExpectedResultsEn.profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(ExpectedResultsEn.otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(ExpectedResultsEn.medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(ExpectedResultsEn.medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(ExpectedResultsEn.nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(ExpectedResultsEn.beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(ExpectedResultsEn.educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
          textOnPageCheck(ExpectedResultsEn.incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(ExpectedResultsEn.incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(ExpectedResultsEn.incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(ExpectedResultsEn.reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(ExpectedResultsEn.nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(ExpectedResultsEn.taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(ExpectedResultsEn.vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(ExpectedResultsEn.nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(ExpectedResultsEn.otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(ExpectedResultsEn.assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(ExpectedResultsEn.assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(ExpectedResultsEn.assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))

          welshToggleCheck(ENGLISH)
        }

        "return only the relevant data on the page when only certain data items are in session" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.BENEFITS_CYA -> Json.prettyPrint(Json.toJson(filteredEmploymentBenefitsModel))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsEn.titleIndividual)
          h1Check(ExpectedResultsEn.headingIndividual)
          textOnPageCheck(ExpectedResultsEn.p1Individual, Selectors.p1)
          textOnPageCheck(ExpectedResultsEn.p2Individual, Selectors.p2)
          textOnPageCheck(ExpectedResultsEn.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(ExpectedResultsEn.companyVan, fieldNameSelector(5, 1))
          textOnPageCheck("£3", fieldAmountSelector(5, 1))
          textOnPageCheck(ExpectedResultsEn.fuelForCompanyVan, fieldNameSelector(5, 2))
          textOnPageCheck("£4", fieldAmountSelector(5, 2))
          textOnPageCheck(ExpectedResultsEn.mileageBenefit, fieldNameSelector(5, 3))
          textOnPageCheck("£5", fieldAmountSelector(5, 3))

          welshToggleCheck(ENGLISH)

          s"should not display the following values" in {

            document().body().toString.contains(ExpectedResultsEn.companyCar) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.fuelForCompanyCar) shouldBe false

            document().body().toString.contains(ExpectedResultsEn.accommodationHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.accommodation) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.qualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.nonQualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.travelHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.travelAndSubsistence) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.personalCosts) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.entertainment) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.utilitiesHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.telephone) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.servicesProvided) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.profSubscriptions) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.otherServices) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.medicalHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.medicalIns) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.nursery) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.beneficialLoans) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.educational) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.incomeTaxHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.incomeTaxPaid) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.incurredCostsPaid) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.reimbursedHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.nonTaxable) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.taxableCosts) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.vouchers) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.nonCash) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.otherBenefits) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.assetsHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.assets) shouldBe false
            document().body().toString.contains(ExpectedResultsEn.assetTransfers) shouldBe false
          }
        }
      }

      "an agent" should {

        "return a fully populated page when all the fields are populated" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.BENEFITS_CYA -> Json.prettyPrint(Json.toJson(fullEmploymentBenefitsModel))
          ))

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsEn.titleAgent)
          h1Check(ExpectedResultsEn.headingAgent)
          textOnPageCheck(ExpectedResultsEn.p1Agent, Selectors.p1)
          textOnPageCheck(ExpectedResultsEn.p2Agent, Selectors.p2)
          textOnPageCheck(ExpectedResultsEn.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(ExpectedResultsEn.companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(ExpectedResultsEn.fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(ExpectedResultsEn.companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(ExpectedResultsEn.fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(ExpectedResultsEn.mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(ExpectedResultsEn.accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(ExpectedResultsEn.accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(ExpectedResultsEn.qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(ExpectedResultsEn.nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(ExpectedResultsEn.travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(ExpectedResultsEn.travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(ExpectedResultsEn.personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(ExpectedResultsEn.entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(ExpectedResultsEn.utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(ExpectedResultsEn.telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(ExpectedResultsEn.servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(ExpectedResultsEn.profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(ExpectedResultsEn.otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(ExpectedResultsEn.medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(ExpectedResultsEn.medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(ExpectedResultsEn.nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(ExpectedResultsEn.beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(ExpectedResultsEn.educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
          textOnPageCheck(ExpectedResultsEn.incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(ExpectedResultsEn.incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(ExpectedResultsEn.incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(ExpectedResultsEn.reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(ExpectedResultsEn.nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(ExpectedResultsEn.taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(ExpectedResultsEn.vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(ExpectedResultsEn.nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(ExpectedResultsEn.otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(ExpectedResultsEn.assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(ExpectedResultsEn.assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(ExpectedResultsEn.assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))

          welshToggleCheck(ENGLISH)
        }
      }
    }
  }

  "in welsh" when {
    "calling GET" when {

      "an individual" should {

        "return a fully populated page when all the fields are populated" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.BENEFITS_CYA -> Json.prettyPrint(Json.toJson(fullEmploymentBenefitsModel))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck",
              HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsCy.titleIndividual)
          captionCheck(ExpectedResultsCy.caption)
          h1Check(ExpectedResultsCy.headingIndividual)
          textOnPageCheck(ExpectedResultsCy.p1Individual, Selectors.p1)
          textOnPageCheck(ExpectedResultsCy.p2Individual, Selectors.p2)
          textOnPageCheck(ExpectedResultsCy.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(ExpectedResultsCy.companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(ExpectedResultsCy.fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(ExpectedResultsCy.companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(ExpectedResultsCy.fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(ExpectedResultsCy.mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(ExpectedResultsCy.accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(ExpectedResultsCy.accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(ExpectedResultsCy.qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(ExpectedResultsCy.nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(ExpectedResultsCy.travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(ExpectedResultsCy.travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(ExpectedResultsCy.personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(ExpectedResultsCy.entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(ExpectedResultsCy.utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(ExpectedResultsCy.telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(ExpectedResultsCy.servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(ExpectedResultsCy.profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(ExpectedResultsCy.otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(ExpectedResultsCy.medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(ExpectedResultsCy.medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(ExpectedResultsCy.nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(ExpectedResultsCy.beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(ExpectedResultsCy.educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
          textOnPageCheck(ExpectedResultsCy.incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(ExpectedResultsCy.incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(ExpectedResultsCy.incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(ExpectedResultsCy.reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(ExpectedResultsCy.nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(ExpectedResultsCy.taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(ExpectedResultsCy.vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(ExpectedResultsCy.nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(ExpectedResultsCy.otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(ExpectedResultsCy.assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(ExpectedResultsCy.assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(ExpectedResultsCy.assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))

          welshToggleCheck(WELSH)
        }

        "return only the relevant data on the page when only certain data items are in session" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.BENEFITS_CYA -> Json.prettyPrint(Json.toJson(filteredEmploymentBenefitsModel))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck",
              HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsCy.titleIndividual)
          h1Check(ExpectedResultsCy.headingIndividual)
          textOnPageCheck(ExpectedResultsCy.p1Individual, Selectors.p1)
          textOnPageCheck(ExpectedResultsCy.p2Individual, Selectors.p2)
          textOnPageCheck(ExpectedResultsCy.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(ExpectedResultsCy.companyVan, fieldNameSelector(5, 1))
          textOnPageCheck("£3", fieldAmountSelector(5, 1))
          textOnPageCheck(ExpectedResultsCy.fuelForCompanyVan, fieldNameSelector(5, 2))
          textOnPageCheck("£4", fieldAmountSelector(5, 2))
          textOnPageCheck(ExpectedResultsCy.mileageBenefit, fieldNameSelector(5, 3))
          textOnPageCheck("£5", fieldAmountSelector(5, 3))

          welshToggleCheck(WELSH)

          s"should not display the following values" in {

            document().body().toString.contains(ExpectedResultsCy.companyCar) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.fuelForCompanyCar) shouldBe false

            document().body().toString.contains(ExpectedResultsCy.accommodationHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.accommodation) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.qualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.nonQualifyingRelocationCosts) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.travelHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.travelAndSubsistence) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.personalCosts) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.entertainment) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.utilitiesHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.telephone) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.servicesProvided) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.profSubscriptions) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.otherServices) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.medicalHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.medicalIns) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.nursery) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.beneficialLoans) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.educational) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.incomeTaxHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.incomeTaxPaid) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.incurredCostsPaid) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.reimbursedHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.nonTaxable) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.taxableCosts) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.vouchers) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.nonCash) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.otherBenefits) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.assetsHeader) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.assets) shouldBe false
            document().body().toString.contains(ExpectedResultsCy.assetTransfers) shouldBe false
          }
        }
      }

      "an agent" should {

        "return a fully populated page when all the fields are populated" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.BENEFITS_CYA -> Json.prettyPrint(Json.toJson(fullEmploymentBenefitsModel))
          ))

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck",
              HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsCy.titleAgent)
          h1Check(ExpectedResultsCy.headingAgent)
          textOnPageCheck(ExpectedResultsCy.p1Agent, Selectors.p1)
          textOnPageCheck(ExpectedResultsCy.p2Agent, Selectors.p2)
          textOnPageCheck(ExpectedResultsCy.vehicleHeader, fieldHeaderSelector(4))
          textOnPageCheck(ExpectedResultsCy.companyCar, fieldNameSelector(5, 1))
          textOnPageCheck("£1.23", fieldAmountSelector(5, 1))
          textOnPageCheck(ExpectedResultsCy.fuelForCompanyCar, fieldNameSelector(5, 2))
          textOnPageCheck("£2", fieldAmountSelector(5, 2))
          textOnPageCheck(ExpectedResultsCy.companyVan, fieldNameSelector(5, 3))
          textOnPageCheck("£3", fieldAmountSelector(5, 3))
          textOnPageCheck(ExpectedResultsCy.fuelForCompanyVan, fieldNameSelector(5, 4))
          textOnPageCheck("£4", fieldAmountSelector(5, 4))
          textOnPageCheck(ExpectedResultsCy.mileageBenefit, fieldNameSelector(5, 5))
          textOnPageCheck("£5", fieldAmountSelector(5, 5))
          textOnPageCheck(ExpectedResultsCy.accommodationHeader, fieldHeaderSelector(6))
          textOnPageCheck(ExpectedResultsCy.accommodation, fieldNameSelector(7, 1))
          textOnPageCheck("£6", fieldAmountSelector(7, 1))
          textOnPageCheck(ExpectedResultsCy.qualifyingRelocationCosts, fieldNameSelector(7, 2))
          textOnPageCheck("£7", fieldAmountSelector(7, 2))
          textOnPageCheck(ExpectedResultsCy.nonQualifyingRelocationCosts, fieldNameSelector(7, 3))
          textOnPageCheck("£8", fieldAmountSelector(7, 3))
          textOnPageCheck(ExpectedResultsCy.travelHeader, fieldHeaderSelector(8))
          textOnPageCheck(ExpectedResultsCy.travelAndSubsistence, fieldNameSelector(9, 1))
          textOnPageCheck("£9", fieldAmountSelector(9, 1))
          textOnPageCheck(ExpectedResultsCy.personalCosts, fieldNameSelector(9, 2))
          textOnPageCheck("£10", fieldAmountSelector(9, 2))
          textOnPageCheck(ExpectedResultsCy.entertainment, fieldNameSelector(9, 3))
          textOnPageCheck("£11", fieldAmountSelector(9, 3))
          textOnPageCheck(ExpectedResultsCy.utilitiesHeader, fieldHeaderSelector(10))
          textOnPageCheck(ExpectedResultsCy.telephone, fieldNameSelector(11, 1))
          textOnPageCheck("£12", fieldAmountSelector(11, 1))
          textOnPageCheck(ExpectedResultsCy.servicesProvided, fieldNameSelector(11, 2))
          textOnPageCheck("£13", fieldAmountSelector(11, 2))
          textOnPageCheck(ExpectedResultsCy.profSubscriptions, fieldNameSelector(11, 3))
          textOnPageCheck("£14", fieldAmountSelector(11, 3))
          textOnPageCheck(ExpectedResultsCy.otherServices, fieldNameSelector(11, 4))
          textOnPageCheck("£15", fieldAmountSelector(11, 4))
          textOnPageCheck(ExpectedResultsCy.medicalHeader, fieldHeaderSelector(12))
          textOnPageCheck(ExpectedResultsCy.medicalIns, fieldNameSelector(13, 1))
          textOnPageCheck("£16", fieldAmountSelector(13, 1))
          textOnPageCheck(ExpectedResultsCy.nursery, fieldNameSelector(13, 2))
          textOnPageCheck("£17", fieldAmountSelector(13, 2))
          textOnPageCheck(ExpectedResultsCy.beneficialLoans, fieldNameSelector(13, 3))
          textOnPageCheck("£18", fieldAmountSelector(13, 3))
          textOnPageCheck(ExpectedResultsCy.educational, fieldNameSelector(13, 4))
          textOnPageCheck("£19", fieldAmountSelector(13, 4))
          textOnPageCheck(ExpectedResultsCy.incomeTaxHeader, fieldHeaderSelector(14))
          textOnPageCheck(ExpectedResultsCy.incomeTaxPaid, fieldNameSelector(15, 1))
          textOnPageCheck("£20", fieldAmountSelector(15, 1))
          textOnPageCheck(ExpectedResultsCy.incurredCostsPaid, fieldNameSelector(15, 2))
          textOnPageCheck("£21", fieldAmountSelector(15, 2))
          textOnPageCheck(ExpectedResultsCy.reimbursedHeader, fieldHeaderSelector(16))
          textOnPageCheck(ExpectedResultsCy.nonTaxable, fieldNameSelector(17, 1))
          textOnPageCheck("£22", fieldAmountSelector(17, 1))
          textOnPageCheck(ExpectedResultsCy.taxableCosts, fieldNameSelector(17, 2))
          textOnPageCheck("£23", fieldAmountSelector(17, 2))
          textOnPageCheck(ExpectedResultsCy.vouchers, fieldNameSelector(17, 3))
          textOnPageCheck("£24", fieldAmountSelector(17, 3))
          textOnPageCheck(ExpectedResultsCy.nonCash, fieldNameSelector(17, 4))
          textOnPageCheck("£25", fieldAmountSelector(17, 4))
          textOnPageCheck(ExpectedResultsCy.otherBenefits, fieldNameSelector(17, 5))
          textOnPageCheck("£26", fieldAmountSelector(17, 5))
          textOnPageCheck(ExpectedResultsCy.assetsHeader, fieldHeaderSelector(18))
          textOnPageCheck(ExpectedResultsCy.assets, fieldNameSelector(19, 1))
          textOnPageCheck("£27", fieldAmountSelector(19, 1))
          textOnPageCheck(ExpectedResultsCy.assetTransfers, fieldNameSelector(19, 2))
          textOnPageCheck("£280000", fieldAmountSelector(19, 2))

          welshToggleCheck(WELSH)
        }
      }
    }
  }

}