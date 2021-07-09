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

package models.employment

import play.api.libs.json.{Json, OFormat}

case class AllEmploymentData(hmrcEmploymentData: Seq[EmploymentSource],
                             hmrcExpenses: Option[EmploymentExpenses],
                             customerEmploymentData: Seq[EmploymentSource],
                             customerExpenses: Option[EmploymentExpenses])

object AllEmploymentData {
  implicit val format: OFormat[AllEmploymentData] = Json.format[AllEmploymentData]
}

case class EmploymentSource(employmentId: String,
                            employerName: String,
                            employerRef: Option[String],
                            payrollId: Option[String],
                            startDate: Option[String],
                            cessationDate: Option[String],
                            dateIgnored: Option[String],
                            submittedOn: Option[String],
                            employmentData: Option[EmploymentData],
                            employmentBenefits: Option[EmploymentBenefits]){

  def toEmploymentDetailsView(isUsingCustomerData: Boolean): EmploymentDetailsView = {
    EmploymentDetailsView(
      employerName,
      employerRef,
      employmentId,
      startDate,
      Some(cessationDate.isDefined),
      cessationDate,
      employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)),
      employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)),
      employmentData.map(_.pay.exists(_.tipsAndOtherPayments.isDefined)),
      employmentData.flatMap(_.pay.flatMap(_.tipsAndOtherPayments)),
      isUsingCustomerData
    )
  }
}

object EmploymentSource {
  implicit val format: OFormat[EmploymentSource] = Json.format[EmploymentSource]
}

// API#1661 Add Employment
//    "employerRef"
//    "employerName"
//    "startDate"
//    "cessationDate"
//    "payrollId"

// API#1662 Update Employment
//    "employerRef"
//    "employerName"
//    "startDate"
//    "cessationDate"
//    "payrollId"

// API#1643 Create/Update Employment financial Data Submission
//"employment": {
//        "pay": { " - REQUIRED
//            "taxablePayToDate" - REQUIRED
//            "totalTaxToDate" - REQUIRED
//            "tipsAndOtherPayments"
//        },
//        "lumpSums": {
//            "taxableLumpSumsAndCertainIncome": {
//              "amount"
//              "taxPaid"
//              "taxTakenOffInEmployment"
//            },
//            "benefitFromEmployerFinancedRetirementScheme": {
//                "amount"
//                "exemptAmount"
//                "taxPaid"
//                "taxTakenOffInEmployment"
//            },
//            "redundancyCompensationPaymentsOverExemption": {
//                "amount"
//                "taxPaid"
//                "taxTakenOffInEmployment"
//            },
//            "redundancyCompensationPaymentsUnderExemption": {
//                "amount"
//            }
//        },
//        "deductions": {
//            "studentLoans": {
//                "uglDeductionAmount"
//                "pglDeductionAmount"
//          }
//        },
//        "benefitsInKind": {
//            "accommodation"
//            "assets"
//            "assetTransfer"
//            "beneficialLoan"
//            "car"
//            "carFuel"
//            "educationalServices"
//            "entertaining"
//            "expenses"
//            "medicalInsurance"
//            "telephone"
//            "service"
//            "taxableExpenses"
//            "van"
//            "vanFuel"
//            "mileage"
//            "nonQualifyingRelocationExpenses"
//            "nurseryPlaces"
//            "otherItems"
//            "paymentsOnEmployeesBehalf"
//            "personalIncidentalExpenses"
//            "qualifyingRelocationExpenses"
//            "employerProvidedProfessionalSubscriptions"
//            "employerProvidedServices"
//            "incomeTaxPaidByDirector"
//            "travelAndSubsistence"
//            "vouchersAndCreditCards"
//            "nonCash"
//        }
//    }

// API#1669 Create/Update Employment expenses
//"expenses": {
//      "businessTravelCosts"
//      "jobExpenses"
//      "flatRateJobExpenses"
//      "professionalSubscriptions"
//      "hotelAndMealExpenses"
//      "otherAndCapitalAllowances"
//      "vehicleExpenses"
//      "mileageAllowanceRelief"
//   }