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

package builders.models.benefits

import models.benefits.Benefits

object BenefitsBuilder {

  val aBenefits: Benefits = Benefits(
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
}
