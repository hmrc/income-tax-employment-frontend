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

package audit

import models.benefits.Benefits
import play.api.libs.json.Json
import support.{TaxYearProvider, UnitTest}

class ViewEmploymentBenefitsSpec extends UnitTest with TaxYearProvider {

  "writes" when {
    "passed a ViewEmploymentBenefitsModel" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |"taxYear": ${taxYearEOY - 1},
             |"userType": "individual",
             |"nino":"AA12343AA",
             |"mtditid":"mtditid",
             |"benefits":{
             |  "accommodation": 100,
             |  "assets": 200,
             |  "assetTransfer": 300,
             |  "beneficialLoan": 400,
             |  "car": 500,
             |  "carFuel": 600,
             |  "educationalServices": 700,
             |  "entertaining": 800,
             |  "expenses": 900,
             |  "medicalInsurance": 1000,
             |  "telephone": 1100,
             |  "service": 1200,
             |  "taxableExpenses": 1300,
             |  "van": 1400,
             |  "vanFuel": 1500,
             |  "mileage": 1600,
             |  "nonQualifyingRelocationExpenses": 1700,
             |  "nurseryPlaces": 1800,
             |  "otherItems": 1900,
             |  "paymentsOnEmployeesBehalf": 2000,
             |  "personalIncidentalExpenses": 2100,
             |  "qualifyingRelocationExpenses": 2200,
             |  "employerProvidedProfessionalSubscriptions": 2300,
             |  "employerProvidedServices": 2400,
             |  "incomeTaxPaidByDirector": 2500,
             |  "travelAndSubsistence": 2600,
             |  "vouchersAndCreditCards": 2700,
             |  "nonCash": 2800
             |}
             |}""".stripMargin)

        val auditModel = ViewEmploymentBenefitsAudit(taxYearEOY - 1, "individual", "AA12343AA",
          "mtditid",
          Benefits(
            accommodation = Some(100),
            assets = Some(200),
            assetTransfer = Some(300),
            beneficialLoan = Some(400),
            car = Some(500),
            carFuel = Some(600),
            educationalServices = Some(700),
            entertaining = Some(800),
            expenses = Some(900),
            medicalInsurance = Some(1000),
            telephone = Some(1100),
            service = Some(1200),
            taxableExpenses = Some(1300),
            van = Some(1400),
            vanFuel = Some(1500),
            mileage = Some(1600),
            nonQualifyingRelocationExpenses = Some(1700),
            nurseryPlaces = Some(1800),
            otherItems = Some(1900),
            paymentsOnEmployeesBehalf = Some(2000),
            personalIncidentalExpenses = Some(2100),
            qualifyingRelocationExpenses = Some(2200),
            employerProvidedProfessionalSubscriptions = Some(2300),
            employerProvidedServices = Some(2400),
            incomeTaxPaidByDirector = Some(2500),
            travelAndSubsistence = Some(2600),
            vouchersAndCreditCards = Some(2700),
            nonCash = Some(2800)
          ))
        Json.toJson(auditModel) shouldBe json
      }
    }
  }
}
