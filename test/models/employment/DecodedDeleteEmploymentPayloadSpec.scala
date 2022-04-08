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

package models.employment

import models.benefits.Benefits
import models.expenses.Expenses
import play.api.libs.json.{JsValue, Json}
import utils.UnitTest

class DecodedDeleteEmploymentPayloadSpec extends UnitTest {

  "writes" when {
    "passed a valid DeleteEmploymentPayload" should {

      "produce valid json" in {

        val validJson: JsValue = Json.parse(
          s"""{
            |    "employmentData": {
            |      "employerName": "AMD infotech Ltd",
            |      "employerRef": "123/AZ12334",
            |      "employmentId": "ff4e1365-ad4f-406e-abdc-20c589d8c217",
            |      "payrollId": "abcd1234",
            |      "startDate": "2019-01-01",
            |      "didYouLeaveQuestion": true,
            |      "cessationDate": "${taxYearEOY-1}-06-01",
            |      "taxablePayToDate": 100,
            |      "totalTaxToDate": 100,
            |      "isUsingCustomerData": true
            |    },
            |    "benefits": {
            |      "accommodation": 100,
            |      "assets": 100,
            |      "assetTransfer": 100,
            |      "beneficialLoan": 100,
            |      "car": 100,
            |      "carFuel": 100,
            |      "educationalServices": 100,
            |      "entertaining": 100,
            |      "expenses": 100,
            |      "medicalInsurance": 100,
            |      "telephone": 100,
            |      "service": 100,
            |      "taxableExpenses": 100,
            |      "van": 100,
            |      "vanFuel": 100,
            |      "mileage": 100,
            |      "nonQualifyingRelocationExpenses": 100,
            |      "nurseryPlaces": 100,
            |      "otherItems": 100,
            |      "paymentsOnEmployeesBehalf": 100,
            |      "personalIncidentalExpenses": 100,
            |      "qualifyingRelocationExpenses": 100,
            |      "employerProvidedProfessionalSubscriptions": 100,
            |      "employerProvidedServices": 100,
            |      "incomeTaxPaidByDirector": 100,
            |      "travelAndSubsistence": 100,
            |      "vouchersAndCreditCards": 100,
            |      "nonCash": 100
            |    },
            |    "expenses": {
            |      "jobExpenses": 100,
            |      "flatRateJobExpenses": 100,
            |      "professionalSubscriptions": 100,
            |      "otherAndCapitalAllowances": 100
            |    },
            |    "deductions": {
            |      "studentLoans": {
            |        "undergraduateLoanDeductionAmount": 100,
            |        "postgraduateLoanDeductionAmount": 100
            |       }
            |    }
            |}""".stripMargin
        )

        val validModel: DecodedDeleteEmploymentPayload = DecodedDeleteEmploymentPayload(
          employmentData = EmploymentDetailsViewModel(
            employerName = "AMD infotech Ltd",
            employerRef = Some("123/AZ12334"),
            payrollId = Some("abcd1234"),
            employmentId = "ff4e1365-ad4f-406e-abdc-20c589d8c217",
            startDate = Some("2019-01-01"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY-1}-06-01"),
            taxablePayToDate = Some(100),
            totalTaxToDate = Some(100),
            isUsingCustomerData = true
          ),
          benefits = Some(Benefits(
            accommodation = Some(100),
            assets = Some(100),
            assetTransfer = Some(100),
            beneficialLoan = Some(100),
            car = Some(100),
            carFuel = Some(100),
            educationalServices = Some(100),
            entertaining = Some(100),
            expenses = Some(100),
            medicalInsurance = Some(100),
            telephone = Some(100),
            service = Some(100),
            taxableExpenses = Some(100),
            van = Some(100),
            vanFuel = Some(100),
            mileage = Some(100),
            nonQualifyingRelocationExpenses = Some(100),
            nurseryPlaces = Some(100),
            otherItems = Some(100),
            paymentsOnEmployeesBehalf = Some(100),
            personalIncidentalExpenses = Some(100),
            qualifyingRelocationExpenses = Some(100),
            employerProvidedProfessionalSubscriptions = Some(100),
            employerProvidedServices = Some(100),
            incomeTaxPaidByDirector = Some(100),
            travelAndSubsistence = Some(100),
            vouchersAndCreditCards = Some(100),
            nonCash = Some(100)
          )
        ),
          expenses = Some(Expenses(
            businessTravelCosts = Some(100),
            jobExpenses = Some(100),
            flatRateJobExpenses = Some(100),
            professionalSubscriptions = Some(100),
            hotelAndMealExpenses = Some(100),
            otherAndCapitalAllowances = Some(100),
            vehicleExpenses = Some(100),
            mileageAllowanceRelief = Some(100)
          )),
          deductions = Some(Deductions(
            studentLoans = Some(StudentLoans(
              uglDeductionAmount = Some(100),
              pglDeductionAmount = Some(100)
            ))
          ))).toNrsPayloadModel

        Json.toJson(validModel) shouldBe validJson
      }
    }
  }

}
