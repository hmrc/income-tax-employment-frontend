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

package models.requests

import models.User
import models.benefits.Benefits
import models.employment.{DecodedDeleteEmploymentPayload, EmploymentDetailsViewModel}
import models.expenses.{DecodedDeleteEmploymentExpensesPayload, DecodedExpensesData, Expenses}

case class DeleteEmploymentExpensesRequest(expenses: Expenses, benefits: Benefits, employmentDetails: EmploymentDetailsViewModel) {

  def toDeleteEmploymentPayloadModel(isUsingCustomerData: Boolean)(implicit user: User[_]): DecodedDeleteEmploymentPayload = {

    DecodedDeleteEmploymentPayload(
      employmentData = EmploymentDetailsViewModel(
        employmentDetails.employerName,
        employmentDetails.employerRef,
        employmentDetails.payrollId,
        employmentDetails.employmentId,
        employmentDetails.startDate,
        employmentDetails.cessationDateQuestion,
        employmentDetails.cessationDate,
        employmentDetails.taxablePayToDate,
        employmentDetails.totalTaxToDate,
        isUsingCustomerData = isUsingCustomerData
      ),
      benefits = Benefits(
        benefits.accommodation,
        benefits.assets,
        benefits.assetTransfer,
        benefits.beneficialLoan,
        benefits.car,
        benefits.carFuel,
        benefits.educationalServices,
        benefits.entertaining,
        benefits.expenses,
        benefits.medicalInsurance,
        benefits.telephone,
        benefits.service,
        benefits.taxableExpenses,
        benefits.van,
        benefits.vanFuel,
        benefits.mileage,
        benefits.nonQualifyingRelocationExpenses,
        benefits.nurseryPlaces,
        benefits.otherItems,
        benefits.paymentsOnEmployeesBehalf,
        benefits.personalIncidentalExpenses,
        benefits.qualifyingRelocationExpenses,
        benefits.employerProvidedProfessionalSubscriptions,
        benefits.employerProvidedServices,
        benefits.incomeTaxPaidByDirector,
        benefits.travelAndSubsistence,
        benefits.vouchersAndCreditCards
      ),
      expenses = DecodedExpensesData(
        expenses.jobExpenses,
        expenses.flatRateJobExpenses,
        expenses.professionalSubscriptions,
        expenses.otherAndCapitalAllowances
      )
    )
  }

  def toDeleteEmploymentExpensesModel()(implicit user: User[_]): DecodedDeleteEmploymentExpensesPayload = {

    DecodedDeleteEmploymentExpensesPayload(
      expenses = DecodedExpensesData(
        expenses.jobExpenses,
        expenses.flatRateJobExpenses,
        expenses.professionalSubscriptions,
        expenses.otherAndCapitalAllowances
      )
    )
  }
}
