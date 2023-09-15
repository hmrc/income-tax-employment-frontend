/*
 * Copyright 2023 HM Revenue & Customs
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
import models.employment.{Deductions, EmploymentDetailsViewModel, StudentLoans}
import play.api.libs.json.Json
import support.{TaxYearProvider, UnitTest}

class DeleteEmploymentAuditSpec extends UnitTest with TaxYearProvider {

  "writes" when {
    "passed a DeleteEmploymentAudit model" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": $taxYearEOY,
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100,
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          deductions = Some(Deductions(
            Some(StudentLoans(
              Some(100),
              Some(100)
            ))
          ))
        )

        Json.toJson(auditModel) shouldBe json
      }

      "omit employment data fields if fields are undefined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": $taxYearEOY,
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employmentId": "id",
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100,
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = None,
            payrollId = None,
            employmentId = "id",
            startDate = None,
            didYouLeaveQuestion = None,
            cessationDate = None,
            taxablePayToDate = None,
            totalTaxToDate = None,
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          deductions = Some(Deductions(
            Some(StudentLoans(
              Some(100),
              Some(100)
            ))
          ))
        )

        Json.toJson(auditModel) shouldBe json
      }

      "omit benefits if undefined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": $taxYearEOY,
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100,
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          None,
          deductions = Some(Deductions(
            Some(StudentLoans(
              Some(100),
              Some(100)
            ))
          ))
        )

        Json.toJson(auditModel) shouldBe json
      }

      "omit benefits fields if fields are undefined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": $taxYearEOY,
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100,
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits()),
          deductions = Some(Deductions(
            Some(StudentLoans(
              Some(100),
              Some(100)
            ))
          ))
        )

        val jsonWithBenefitsFieldsDefined = Json.parse(
          s"""{
             |  "taxYear": $taxYearEOY,
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100,
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModelWithBenefitsFieldsDefined = DeleteEmploymentAudit(taxYearEOY, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
            van = Some(1400)
          )),
          deductions = Some(Deductions(
            Some(StudentLoans(
              Some(100),
              Some(100)
            ))
          ))
        )

        Json.toJson(auditModel) shouldBe json
        Json.toJson(auditModelWithBenefitsFieldsDefined) shouldBe jsonWithBenefitsFieldsDefined
      }

      "omit expenses if undefined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100,
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          deductions = Some(Deductions(
            Some(StudentLoans(
              Some(100),
              Some(100)
            ))
          ))
        )

        Json.toJson(auditModel) shouldBe json
      }

      "omit expenses fields if fields are undefined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100,
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          deductions = Some(Deductions(
            Some(StudentLoans(
              Some(100),
              Some(100)
            ))
          ))
        )

        val jsonWithExpensesFieldsDefined = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100,
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModelWithExpensesFieldsDefined = DeleteEmploymentAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          deductions = Some(Deductions(
            Some(StudentLoans(
              Some(100),
              Some(100)
            ))
          ))
        )

        val jsonWithUnusedExpensesFieldsDefined = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100,
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModelWithUnusedExpensesFieldsDefined = DeleteEmploymentAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          deductions = Some(Deductions(
            Some(StudentLoans(
              Some(100),
              Some(100)
            ))
          ))
        )

        Json.toJson(auditModel) shouldBe json
        Json.toJson(auditModelWithExpensesFieldsDefined) shouldBe jsonWithExpensesFieldsDefined
        Json.toJson(auditModelWithUnusedExpensesFieldsDefined) shouldBe jsonWithUnusedExpensesFieldsDefined
      }

      "omit deductions block if deductions are not defined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          deductions = None
        )

        Json.toJson(auditModel) shouldBe json
      }

      "omit deductions block if student loans are not defined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          deductions = Some(Deductions(
            studentLoans = None
          ))
        )

        Json.toJson(auditModel) shouldBe json
      }

      "omit deductions block if undergraduate loan deduction amount and postgraduate loan deduction amount values are not defined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          deductions = Some(Deductions(
            studentLoans = Some(StudentLoans(pglDeductionAmount = None, uglDeductionAmount = None))
          ))
        )

        Json.toJson(auditModel) shouldBe json
      }

      "omit undergraduate loan deduction amount in deductions block if value is not defined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "postgraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          Some(Deductions(
            Some(StudentLoans(pglDeductionAmount = Some(100), uglDeductionAmount = None))
          ))
        )

        Json.toJson(auditModel) shouldBe json
      }

      "omit postgraduate loan deduction amount in deductions block if value is not defined" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY - 1},
             |  "userType": "individual",
             |  "nino": "AA12343AA",
             |  "mtditid": "mtditid",
             |  "employmentData": {
             |    "employerName": "Dave",
             |    "employerRef": "reference",
             |    "payrollId": "12345678",
             |    "employmentId": "id",
             |    "startDate": "${taxYearEOY - 1}-02-12",
             |    "didYouLeaveQuestion": true,
             |    "cessationDate": "${taxYearEOY - 1}-02-12",
             |    "taxablePayToDate": 34234.15,
             |    "totalTaxToDate": 6782.92,
             |    "isUsingCustomerData": false,
             |    "offPayrollWorkingStatus": false
             |  },
             |  "benefits": {
             |    "accommodation": 100,
             |    "assets": 200,
             |    "assetTransfer": 300,
             |    "beneficialLoan": 400,
             |    "car": 500,
             |    "carFuel": 600,
             |    "educationalServices": 700,
             |    "entertaining": 800,
             |    "expenses": 900,
             |    "medicalInsurance": 1000,
             |    "telephone": 1100,
             |    "service": 1200,
             |    "taxableExpenses": 1300,
             |    "van": 1400,
             |    "vanFuel": 1500,
             |    "mileage": 1600,
             |    "nonQualifyingRelocationExpenses": 1700,
             |    "nurseryPlaces": 1800,
             |    "otherItems": 1900,
             |    "paymentsOnEmployeesBehalf": 2000,
             |    "personalIncidentalExpenses": 2100,
             |    "qualifyingRelocationExpenses": 2200,
             |    "employerProvidedProfessionalSubscriptions": 2300,
             |    "employerProvidedServices": 2400,
             |    "incomeTaxPaidByDirector": 2500,
             |    "travelAndSubsistence": 2600,
             |    "vouchersAndCreditCards": 2700,
             |    "nonCash": 2800
             |  },
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 100
             |    }
             |  }
             |}""".stripMargin)

        val auditModel = DeleteEmploymentAudit(taxYearEOY - 1, "individual", "AA12343AA", "mtditid",
          employmentData = EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false,
            offPayrollWorkingStatus = Some(false)
          ),
          Some(Benefits(
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
          )),
          Some(Deductions(
            Some(StudentLoans(pglDeductionAmount = None, uglDeductionAmount = Some(100)))
          ))
        )

        Json.toJson(auditModel) shouldBe json
      }
    }
  }
}
