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
  lazy val controller: CheckYourBenefitsController = app.injector.instanceOf[CheckYourBenefitsController]
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

  object ExpectedResults {
    val title = "Check employment benefits"
    val heading = "Check employment benefits"
    val caption = "Employments to charity for 6 April 2021 to 5 April 2022"
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

        titleCheck(ExpectedResults.title)
        h1Check(ExpectedResults.heading)
        textOnPageCheck(ExpectedResults.p1Individual, Selectors.p1)
        textOnPageCheck(ExpectedResults.p2Individual, Selectors.p2)
        textOnPageCheck(ExpectedResults.vehicleHeader, fieldHeaderSelector(4))
        textOnPageCheck(ExpectedResults.companyCar, fieldNameSelector(5,1))
        textOnPageCheck("£1.23", fieldAmountSelector(5,1))
        textOnPageCheck(ExpectedResults.fuelForCompanyCar, fieldNameSelector(5,2))
        textOnPageCheck("£2", fieldAmountSelector(5,2))
        textOnPageCheck(ExpectedResults.companyVan, fieldNameSelector(5,3))
        textOnPageCheck("£3", fieldAmountSelector(5,3))
        textOnPageCheck(ExpectedResults.fuelForCompanyVan, fieldNameSelector(5,4))
        textOnPageCheck("£4", fieldAmountSelector(5,4))
        textOnPageCheck(ExpectedResults.mileageBenefit, fieldNameSelector(5,5))
        textOnPageCheck("£5", fieldAmountSelector(5,5))
        textOnPageCheck(ExpectedResults.accommodationHeader, fieldHeaderSelector(6))
        textOnPageCheck(ExpectedResults.accommodation, fieldNameSelector(7, 1))
        textOnPageCheck("£6", fieldAmountSelector(7,1))
        textOnPageCheck(ExpectedResults.qualifyingRelocationCosts, fieldNameSelector(7,2))
        textOnPageCheck("£7", fieldAmountSelector(7,2))
        textOnPageCheck(ExpectedResults.nonQualifyingRelocationCosts, fieldNameSelector(7,3))
        textOnPageCheck("£8", fieldAmountSelector(7,3))
        textOnPageCheck(ExpectedResults.travelHeader, fieldHeaderSelector(8))
        textOnPageCheck(ExpectedResults.travelAndSubsistence, fieldNameSelector(9,1))
        textOnPageCheck("£9", fieldAmountSelector(9,1))
        textOnPageCheck(ExpectedResults.personalCosts, fieldNameSelector(9,2))
        textOnPageCheck("£10", fieldAmountSelector(9,2))
        textOnPageCheck(ExpectedResults.entertainment, fieldNameSelector(9,3))
        textOnPageCheck("£11", fieldAmountSelector(9,3))
        textOnPageCheck(ExpectedResults.utilitiesHeader, fieldHeaderSelector(10))
        textOnPageCheck(ExpectedResults.telephone, fieldNameSelector(11,1))
        textOnPageCheck("£12", fieldAmountSelector(11,1))
        textOnPageCheck(ExpectedResults.servicesProvided, fieldNameSelector(11,2))
        textOnPageCheck("£13", fieldAmountSelector(11,2))
        textOnPageCheck(ExpectedResults.profSubscriptions, fieldNameSelector(11,3))
        textOnPageCheck("£14", fieldAmountSelector(11,3))
        textOnPageCheck(ExpectedResults.otherServices, fieldNameSelector(11,4))
        textOnPageCheck("£15", fieldAmountSelector(11,4))
        textOnPageCheck(ExpectedResults.medicalHeader, fieldHeaderSelector(12))
        textOnPageCheck(ExpectedResults.medicalIns, fieldNameSelector(13,1))
        textOnPageCheck("£16", fieldAmountSelector(13,1))
        textOnPageCheck(ExpectedResults.nursery, fieldNameSelector(13,2))
        textOnPageCheck("£17", fieldAmountSelector(13,2))
        textOnPageCheck(ExpectedResults.beneficialLoans, fieldNameSelector(13,3))
        textOnPageCheck("£18", fieldAmountSelector(13,3))
        textOnPageCheck(ExpectedResults.educational, fieldNameSelector(13,4))
        textOnPageCheck("£19", fieldAmountSelector(13,4))
        textOnPageCheck(ExpectedResults.incomeTaxHeader, fieldHeaderSelector(14))
        textOnPageCheck(ExpectedResults.incomeTaxPaid, fieldNameSelector(15,1))
        textOnPageCheck("£20", fieldAmountSelector(15,1))
        textOnPageCheck(ExpectedResults.incurredCostsPaid, fieldNameSelector(15,2))
        textOnPageCheck("£21", fieldAmountSelector(15,2))
        textOnPageCheck(ExpectedResults.reimbursedHeader, fieldHeaderSelector(16))
        textOnPageCheck(ExpectedResults.nonTaxable, fieldNameSelector(17,1))
        textOnPageCheck("£22", fieldAmountSelector(17,1))
        textOnPageCheck(ExpectedResults.taxableCosts, fieldNameSelector(17,2))
        textOnPageCheck("£23", fieldAmountSelector(17,2))
        textOnPageCheck(ExpectedResults.vouchers, fieldNameSelector(17,3))
        textOnPageCheck("£24", fieldAmountSelector(17,3))
        textOnPageCheck(ExpectedResults.nonCash, fieldNameSelector(17,4))
        textOnPageCheck("£25", fieldAmountSelector(17,4))
        textOnPageCheck(ExpectedResults.otherBenefits, fieldNameSelector(17,5))
        textOnPageCheck("£26", fieldAmountSelector(17,5))
        textOnPageCheck(ExpectedResults.assetsHeader, fieldHeaderSelector(18))
        textOnPageCheck(ExpectedResults.assets, fieldNameSelector(19,1))
        textOnPageCheck("£27", fieldAmountSelector(19,1))
        textOnPageCheck(ExpectedResults.assetTransfers, fieldNameSelector(19,2))
        textOnPageCheck("£280000", fieldAmountSelector(19,2))
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

        titleCheck(ExpectedResults.title)
        h1Check(ExpectedResults.heading)
        textOnPageCheck(ExpectedResults.p1Individual, Selectors.p1)
        textOnPageCheck(ExpectedResults.p2Individual, Selectors.p2)
        textOnPageCheck(ExpectedResults.vehicleHeader, fieldHeaderSelector(4))
        textOnPageCheck(ExpectedResults.companyVan, fieldNameSelector(5,1))
        textOnPageCheck("£3", fieldAmountSelector(5,1))
        textOnPageCheck(ExpectedResults.fuelForCompanyVan, fieldNameSelector(5,2))
        textOnPageCheck("£4", fieldAmountSelector(5,2))
        textOnPageCheck(ExpectedResults.mileageBenefit, fieldNameSelector(5,3))
        textOnPageCheck("£5", fieldAmountSelector(5,3))

        s"should not display the following values" in {

          document().body().toString.contains(ExpectedResults.companyCar) shouldBe false
          document().body().toString.contains(ExpectedResults.fuelForCompanyCar) shouldBe false

          document().body().toString.contains(ExpectedResults.accommodationHeader) shouldBe false
          document().body().toString.contains(ExpectedResults.accommodation) shouldBe false
          document().body().toString.contains(ExpectedResults.qualifyingRelocationCosts) shouldBe false
          document().body().toString.contains(ExpectedResults.nonQualifyingRelocationCosts) shouldBe false
          document().body().toString.contains(ExpectedResults.travelHeader) shouldBe false
          document().body().toString.contains(ExpectedResults.travelAndSubsistence) shouldBe false
          document().body().toString.contains(ExpectedResults.personalCosts) shouldBe false
          document().body().toString.contains(ExpectedResults.entertainment) shouldBe false
          document().body().toString.contains(ExpectedResults.utilitiesHeader) shouldBe false
          document().body().toString.contains(ExpectedResults.telephone) shouldBe false
          document().body().toString.contains(ExpectedResults.servicesProvided) shouldBe false
          document().body().toString.contains(ExpectedResults.profSubscriptions) shouldBe false
          document().body().toString.contains(ExpectedResults.otherServices) shouldBe false
          document().body().toString.contains(ExpectedResults.medicalHeader) shouldBe false
          document().body().toString.contains(ExpectedResults.medicalIns) shouldBe false
          document().body().toString.contains(ExpectedResults.nursery) shouldBe false
          document().body().toString.contains(ExpectedResults.beneficialLoans) shouldBe false
          document().body().toString.contains(ExpectedResults.educational) shouldBe false
          document().body().toString.contains(ExpectedResults.incomeTaxHeader) shouldBe false
          document().body().toString.contains(ExpectedResults.incomeTaxPaid) shouldBe false
          document().body().toString.contains(ExpectedResults.incurredCostsPaid) shouldBe false
          document().body().toString.contains(ExpectedResults.reimbursedHeader) shouldBe false
          document().body().toString.contains(ExpectedResults.nonTaxable) shouldBe false
          document().body().toString.contains(ExpectedResults.taxableCosts) shouldBe false
          document().body().toString.contains(ExpectedResults.vouchers) shouldBe false
          document().body().toString.contains(ExpectedResults.nonCash) shouldBe false
          document().body().toString.contains(ExpectedResults.otherBenefits) shouldBe false
          document().body().toString.contains(ExpectedResults.assetsHeader) shouldBe false
          document().body().toString.contains(ExpectedResults.assets) shouldBe false
          document().body().toString.contains(ExpectedResults.assetTransfers) shouldBe false
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

        titleCheck(ExpectedResults.title)
        h1Check(ExpectedResults.heading)
        textOnPageCheck(ExpectedResults.p1Agent, Selectors.p1)
        textOnPageCheck(ExpectedResults.p2Agent, Selectors.p2)
        textOnPageCheck(ExpectedResults.vehicleHeader, fieldHeaderSelector(4))
        textOnPageCheck(ExpectedResults.companyCar, fieldNameSelector(5,1))
        textOnPageCheck("£1.23", fieldAmountSelector(5,1))
        textOnPageCheck(ExpectedResults.fuelForCompanyCar, fieldNameSelector(5,2))
        textOnPageCheck("£2", fieldAmountSelector(5,2))
        textOnPageCheck(ExpectedResults.companyVan, fieldNameSelector(5,3))
        textOnPageCheck("£3", fieldAmountSelector(5,3))
        textOnPageCheck(ExpectedResults.fuelForCompanyVan, fieldNameSelector(5,4))
        textOnPageCheck("£4", fieldAmountSelector(5,4))
        textOnPageCheck(ExpectedResults.mileageBenefit, fieldNameSelector(5,5))
        textOnPageCheck("£5", fieldAmountSelector(5,5))
        textOnPageCheck(ExpectedResults.accommodationHeader, fieldHeaderSelector(6))
        textOnPageCheck(ExpectedResults.accommodation, fieldNameSelector(7, 1))
        textOnPageCheck("£6", fieldAmountSelector(7,1))
        textOnPageCheck(ExpectedResults.qualifyingRelocationCosts, fieldNameSelector(7,2))
        textOnPageCheck("£7", fieldAmountSelector(7,2))
        textOnPageCheck(ExpectedResults.nonQualifyingRelocationCosts, fieldNameSelector(7,3))
        textOnPageCheck("£8", fieldAmountSelector(7,3))
        textOnPageCheck(ExpectedResults.travelHeader, fieldHeaderSelector(8))
        textOnPageCheck(ExpectedResults.travelAndSubsistence, fieldNameSelector(9,1))
        textOnPageCheck("£9", fieldAmountSelector(9,1))
        textOnPageCheck(ExpectedResults.personalCosts, fieldNameSelector(9,2))
        textOnPageCheck("£10", fieldAmountSelector(9,2))
        textOnPageCheck(ExpectedResults.entertainment, fieldNameSelector(9,3))
        textOnPageCheck("£11", fieldAmountSelector(9,3))
        textOnPageCheck(ExpectedResults.utilitiesHeader, fieldHeaderSelector(10))
        textOnPageCheck(ExpectedResults.telephone, fieldNameSelector(11,1))
        textOnPageCheck("£12", fieldAmountSelector(11,1))
        textOnPageCheck(ExpectedResults.servicesProvided, fieldNameSelector(11,2))
        textOnPageCheck("£13", fieldAmountSelector(11,2))
        textOnPageCheck(ExpectedResults.profSubscriptions, fieldNameSelector(11,3))
        textOnPageCheck("£14", fieldAmountSelector(11,3))
        textOnPageCheck(ExpectedResults.otherServices, fieldNameSelector(11,4))
        textOnPageCheck("£15", fieldAmountSelector(11,4))
        textOnPageCheck(ExpectedResults.medicalHeader, fieldHeaderSelector(12))
        textOnPageCheck(ExpectedResults.medicalIns, fieldNameSelector(13,1))
        textOnPageCheck("£16", fieldAmountSelector(13,1))
        textOnPageCheck(ExpectedResults.nursery, fieldNameSelector(13,2))
        textOnPageCheck("£17", fieldAmountSelector(13,2))
        textOnPageCheck(ExpectedResults.beneficialLoans, fieldNameSelector(13,3))
        textOnPageCheck("£18", fieldAmountSelector(13,3))
        textOnPageCheck(ExpectedResults.educational, fieldNameSelector(13,4))
        textOnPageCheck("£19", fieldAmountSelector(13,4))
        textOnPageCheck(ExpectedResults.incomeTaxHeader, fieldHeaderSelector(14))
        textOnPageCheck(ExpectedResults.incomeTaxPaid, fieldNameSelector(15,1))
        textOnPageCheck("£20", fieldAmountSelector(15,1))
        textOnPageCheck(ExpectedResults.incurredCostsPaid, fieldNameSelector(15,2))
        textOnPageCheck("£21", fieldAmountSelector(15,2))
        textOnPageCheck(ExpectedResults.reimbursedHeader, fieldHeaderSelector(16))
        textOnPageCheck(ExpectedResults.nonTaxable, fieldNameSelector(17,1))
        textOnPageCheck("£22", fieldAmountSelector(17,1))
        textOnPageCheck(ExpectedResults.taxableCosts, fieldNameSelector(17,2))
        textOnPageCheck("£23", fieldAmountSelector(17,2))
        textOnPageCheck(ExpectedResults.vouchers, fieldNameSelector(17,3))
        textOnPageCheck("£24", fieldAmountSelector(17,3))
        textOnPageCheck(ExpectedResults.nonCash, fieldNameSelector(17,4))
        textOnPageCheck("£25", fieldAmountSelector(17,4))
        textOnPageCheck(ExpectedResults.otherBenefits, fieldNameSelector(17,5))
        textOnPageCheck("£26", fieldAmountSelector(17,5))
        textOnPageCheck(ExpectedResults.assetsHeader, fieldHeaderSelector(18))
        textOnPageCheck(ExpectedResults.assets, fieldNameSelector(19,1))
        textOnPageCheck("£27", fieldAmountSelector(19,1))
        textOnPageCheck(ExpectedResults.assetTransfers, fieldNameSelector(19,2))
        textOnPageCheck("£280000", fieldAmountSelector(19,2))
      }
    }
  }
}
