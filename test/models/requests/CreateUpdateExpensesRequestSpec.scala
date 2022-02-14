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

import models.employment.EmploymentExpenses
import models.expenses.Expenses
import play.api.libs.json.Json
import utils.UnitTest

class CreateUpdateExpensesRequestSpec extends UnitTest {

  val taxYear = 2020

  val defaultExpenses: Expenses = Expenses(
    businessTravelCosts = Some(150),
    jobExpenses = Some(100),
    flatRateJobExpenses = Some(50),
    professionalSubscriptions = Some(140),
    hotelAndMealExpenses = Some(123),
    otherAndCapitalAllowances = Some(210),
    vehicleExpenses = Some(250),
    mileageAllowanceRelief = Some(300)
  )

  val defaultModel: CreateUpdateExpensesRequest = CreateUpdateExpensesRequest(Some(true),
    defaultExpenses)

  val defaultPriorCustomerEmploymentExpenses: EmploymentExpenses = employmentExpenses.copy(
    expenses = Some(defaultExpenses.copy(
      businessTravelCosts = Some(15),
      jobExpenses = Some(10),
      flatRateJobExpenses = Some(5),
      professionalSubscriptions = Some(14),
      hotelAndMealExpenses = Some(12),
      otherAndCapitalAllowances = Some(21),
      vehicleExpenses = Some(25),
      mileageAllowanceRelief = Some(30)
    )))

  "creates a create audit event" when {

    "all expenses are provided" in {
      val actualResult = defaultModel.toCreateAuditModel(authorisationRequest.user, taxYear)
      val expectedJson = Json.parse(
        s"""{
           | "taxYear": $taxYear,
           | "userType": "${authorisationRequest.user.affinityGroup.toLowerCase}",
           | "nino": "${authorisationRequest.user.nino}",
           | "mtditid": "${authorisationRequest.user.mtditid}",
           | "employmentExpensesData": {
           |  "jobExpenses": 100,
           |  "flatRateJobExpenses": 50,
           |  "professionalSubscriptions": 140,
           |  "otherAndCapitalAllowances": 210
           | }
           |}""".stripMargin)
      Json.toJson(actualResult) shouldBe expectedJson
    }

    "some expenses are provided  " in {
      val model = defaultModel.copy(
        expenses = defaultExpenses.copy(
          professionalSubscriptions = None,
          otherAndCapitalAllowances = None
        )
      )

      val actualResult = model.toCreateAuditModel(authorisationRequest.user, taxYear)
      val expectedJson = Json.parse(
        s"""{
           | "taxYear": $taxYear,
           | "userType": "${authorisationRequest.user.affinityGroup.toLowerCase}",
           | "nino": "${authorisationRequest.user.nino}",
           | "mtditid": "${authorisationRequest.user.mtditid}",
           | "employmentExpensesData": {
           |  "jobExpenses": 100,
           |  "flatRateJobExpenses": 50
           | }
           |}""".stripMargin)
      Json.toJson(actualResult) shouldBe expectedJson
    }

    "no expenses are provided " in {
      val model = defaultModel.copy(
        expenses = defaultExpenses.copy(
          jobExpenses = None,
          flatRateJobExpenses = None,
          professionalSubscriptions = None,
          otherAndCapitalAllowances = None
        )
      )

      val actualResult = model.toCreateAuditModel(authorisationRequest.user, taxYear)
      val expectedJson = Json.parse(
        s"""{
           | "taxYear": $taxYear,
           | "userType": "${authorisationRequest.user.affinityGroup.toLowerCase}",
           | "nino": "${authorisationRequest.user.nino}",
           | "mtditid": "${authorisationRequest.user.mtditid}",
           | "employmentExpensesData": {}
           |}""".stripMargin)
      Json.toJson(actualResult) shouldBe expectedJson
    }
  }

  "creates an amend audit event" when {
    "all prior and current expenses are provided" in {
      val actualResult = defaultModel.toAmendAuditModel(authorisationRequest.user, taxYear, defaultPriorCustomerEmploymentExpenses)
      val expectedJson = Json.parse(
        s"""{
           | "taxYear": $taxYear,
           | "userType": "${authorisationRequest.user.affinityGroup.toLowerCase}",
           | "nino": "${authorisationRequest.user.nino}",
           | "mtditid": "${authorisationRequest.user.mtditid}",
           |  "priorEmploymentExpensesData": {
           |  "jobExpenses": 10,
           |  "flatRateJobExpenses": 5,
           |  "professionalSubscriptions": 14,
           |  "otherAndCapitalAllowances": 21
           |  },
           | "employmentExpensesData": {
           |  "jobExpenses": 100,
           |  "flatRateJobExpenses": 50,
           |  "professionalSubscriptions": 140,
           |  "otherAndCapitalAllowances": 210
           | }
           |}""".stripMargin)
      Json.toJson(actualResult) shouldBe expectedJson
    }

    "some prior expenses are provided " in {

      val priorCustomerEmploymentExpenses = employmentExpenses.copy(
        expenses = Some(defaultExpenses.copy(
          businessTravelCosts = Some(15),
          jobExpenses = Some(10),
          flatRateJobExpenses = Some(5),
          professionalSubscriptions = None,
          hotelAndMealExpenses = Some(12),
          otherAndCapitalAllowances = None,
          vehicleExpenses = Some(25),
          mileageAllowanceRelief = Some(30)
        )))

      val actualResult = defaultModel.toAmendAuditModel(authorisationRequest.user, taxYear, priorCustomerEmploymentExpenses)

      val expectedJson = Json.parse(
        s"""{
           | "taxYear": $taxYear,
           | "userType": "${authorisationRequest.user.affinityGroup.toLowerCase}",
           | "nino": "${authorisationRequest.user.nino}",
           | "mtditid": "${authorisationRequest.user.mtditid}",
           |  "priorEmploymentExpensesData": {
           |  "jobExpenses": 10,
           |  "flatRateJobExpenses": 5
           |  },
           | "employmentExpensesData": {
           |  "jobExpenses": 100,
           |  "flatRateJobExpenses": 50,
           |  "professionalSubscriptions": 140,
           |  "otherAndCapitalAllowances": 210
           | }
           |}
           |""".stripMargin)
      Json.toJson(actualResult) shouldBe expectedJson
    }

    "no prior expenses are provided" in {
      val model = defaultModel.copy(
        expenses = defaultExpenses.copy(
          professionalSubscriptions = None,
          otherAndCapitalAllowances = None
        )
      )

      val priorExpenses = defaultPriorCustomerEmploymentExpenses.copy(
        expenses = None
      )

      val actualResult = model.toAmendAuditModel(authorisationRequest.user, taxYear, priorExpenses)
      val expectedJson = Json.parse(
        s"""{
           | "taxYear": $taxYear,
           | "userType": "${authorisationRequest.user.affinityGroup.toLowerCase}",
           | "nino": "${authorisationRequest.user.nino}",
           | "mtditid": "${authorisationRequest.user.mtditid}",
           |   "priorEmploymentExpensesData": {},
           | "employmentExpensesData": {
           |  "jobExpenses": 100,
           |  "flatRateJobExpenses": 50
           | }
           |}""".stripMargin)
      Json.toJson(actualResult) shouldBe expectedJson
    }

    "some new expenses are provided" in {
      val model = defaultModel.copy(
        expenses = defaultExpenses.copy(
          professionalSubscriptions = None,
          otherAndCapitalAllowances = None
        )
      )

      val actualResult = model.toAmendAuditModel(authorisationRequest.user, taxYear, defaultPriorCustomerEmploymentExpenses)
      val expectedJson = Json.parse(
        s"""{
           | "taxYear": $taxYear,
           | "userType": "${authorisationRequest.user.affinityGroup.toLowerCase}",
           | "nino": "${authorisationRequest.user.nino}",
           | "mtditid": "${authorisationRequest.user.mtditid}",
           |   "priorEmploymentExpensesData": {
           |  "jobExpenses": 10,
           |  "flatRateJobExpenses": 5,
           |  "professionalSubscriptions": 14,
           |  "otherAndCapitalAllowances": 21
           |  },
           | "employmentExpensesData": {
           |  "jobExpenses": 100,
           |  "flatRateJobExpenses": 50
           | }
           |}""".stripMargin)
      Json.toJson(actualResult) shouldBe expectedJson
    }

    "no new expenses are provided" in {
      val model = defaultModel.copy(
        expenses = defaultExpenses.copy(
          flatRateJobExpenses = None,
          professionalSubscriptions = None,
          otherAndCapitalAllowances = None,
          jobExpenses = None
        )
      )

      val actualResult = model.toAmendAuditModel(authorisationRequest.user, taxYear, defaultPriorCustomerEmploymentExpenses)
      val expectedJson = Json.parse(
        s"""{
           | "taxYear": $taxYear,
           | "userType": "${authorisationRequest.user.affinityGroup.toLowerCase}",
           | "nino": "${authorisationRequest.user.nino}",
           | "mtditid": "${authorisationRequest.user.mtditid}",
           |   "priorEmploymentExpensesData": {
           |  "jobExpenses": 10,
           |  "flatRateJobExpenses": 5,
           |  "professionalSubscriptions": 14,
           |  "otherAndCapitalAllowances": 21
           |  },
           | "employmentExpensesData": {}
           |}""".stripMargin)
      Json.toJson(actualResult) shouldBe expectedJson
    }
  }
}
