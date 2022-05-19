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
import utils.{TestTaxYearHelper, UnitTest}

class AmendEmploymentBenefitsUpdateAuditSpec extends UnitTest with TestTaxYearHelper {

  "writes" when {
    "passed a AmendEmploymentBenefitsUpdate model" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "priorEmploymentBenefitsData": {
             |    "accommodation": 150,
             |    "assets": 200,
             |    "assetTransfer": 250
             |  },
             |  "employmentBenefitsData": {
             |    "accommodation": 150,
             |    "assets": 200,
             |    "assetTransfer": 250,
             |    "beneficialLoan": 350,
             |    "car": 440,
             |    "carFuel": 350,
             |    "educationalServices": 200,
             |    "entertaining": 300,
             |    "expenses": 400,
             |    "medicalInsurance": 200,
             |    "telephone": 100,
             |    "service": 250,
             |    "taxableExpenses": 250,
             |    "van": 300,
             |    "vanFuel": 350,
             |    "mileage": 350,
             |    "nonQualifyingRelocationExpenses": 450,
             |    "nurseryPlaces": 250,
             |    "otherItems": 100,
             |    "paymentsOnEmployeesBehalf": 200,
             |    "personalIncidentalExpenses": 250,
             |    "qualifyingRelocationExpenses": 400,
             |    "employerProvidedProfessionalSubscriptions": 300,
             |    "employerProvidedServices": 250,
             |    "incomeTaxPaidByDirector": 350,
             |    "travelAndSubsistence": 250,
             |    "vouchersAndCreditCards": 350,
             |    "nonCash": 250
             |  }
             |}""".stripMargin)

        //scalastyle:off magic.number
        val auditModel = AmendEmploymentBenefitsUpdateAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          priorEmploymentBenefitsData = Benefits(
            accommodation = Some(150.00),
            assets = Some(200.00),
            assetTransfer = Some(250.00)
          ),
          employmentBenefitsData = Benefits(
            accommodation = Some(150.00),
            assets = Some(200.00),
            assetTransfer = Some(250.00),
            beneficialLoan = Some(350.00),
            car = Some(440.00),
            carFuel = Some(350.00),
            educationalServices = Some(200.00),
            entertaining = Some(300.00),
            expenses = Some(400.00),
            medicalInsurance = Some(200.00),
            telephone = Some(100),
            service = Some(250.00),
            taxableExpenses = Some(250.00),
            van = Some(300.00),
            vanFuel = Some(350.00),
            mileage = Some(350.00),
            nonQualifyingRelocationExpenses = Some(450.00),
            nurseryPlaces = Some(250.00),
            otherItems = Some(100.00),
            paymentsOnEmployeesBehalf = Some(200.00),
            personalIncidentalExpenses = Some(250.00),
            qualifyingRelocationExpenses = Some(400.00),
            employerProvidedProfessionalSubscriptions = Some(300.00),
            employerProvidedServices = Some(250.00),
            incomeTaxPaidByDirector = Some(350.00),
            travelAndSubsistence = Some(250.00),
            vouchersAndCreditCards = Some(350.00),
            nonCash = Some(250.00)
          )
        )
        //scalastyle:on magic.number

        Json.toJson(auditModel) shouldBe json
      }
    }
  }

}

