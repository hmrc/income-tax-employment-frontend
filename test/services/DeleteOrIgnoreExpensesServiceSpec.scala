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

package services

import models.employment._
import models.expenses.{DecodedDeleteEmploymentExpensesPayload, Expenses}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import support.mocks.{MockDeleteOrIgnoreExpensesConnector, MockDeleteOrIgnoreExpensesService, MockIncomeSourceConnector, MockNrsService}
import utils.UnitTest

class DeleteOrIgnoreExpensesServiceSpec extends UnitTest
  with MockDeleteOrIgnoreExpensesConnector
  with MockIncomeSourceConnector
  with MockNrsService
  with MockDeleteOrIgnoreExpensesService {

  val service: DeleteOrIgnoreExpensesService = new DeleteOrIgnoreExpensesService(mockDeleteOrIgnoreExpensesConnector, mockIncomeSourceConnector, mockNrsService, mockExecutionContext)

  val taxYear = 2022

  private val hmrcExpensesWithoutDateIgnored =
    EmploymentExpenses(
      None,
      None,
      Some(8),
      Some(Expenses(Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(1)))
    )

  private val hmrcExpensesWithDateIgnored =
    EmploymentExpenses(
      None,
      Some("2020-04-04T01:01:01Z"),
      Some(8),
      Some(Expenses(Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(1)))
    )

  private val customerExpenses =
    EmploymentExpenses(
      None,
      None,
      Some(40.00),
      Some(Expenses(Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5)))
    )

  private val employmentSource1: EmploymentSource = EmploymentSource(
    employmentId = "001",
    employerName = "Mishima Zaibatsu",
    employerRef = Some("223/AB12399"),
    payrollId = Some("123456789999"),
    startDate = Some("2019-04-21"),
    cessationDate = Some("2020-03-11"),
    dateIgnored = None,
    submittedOn = Some("2020-01-04T05:01:01Z"),
    employmentData = Some(EmploymentData(
      submittedOn = "2020-02-12",
      employmentSequenceNumber = Some("123456789999"),
      companyDirector = Some(true),
      closeCompany = Some(false),
      directorshipCeasedDate = Some("2020-02-12"),
      occPen = Some(false),
      disguisedRemuneration = Some(false),
      pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
      Some(Deductions(
        studentLoans = Some(StudentLoans(
          uglDeductionAmount = Some(100.00),
          pglDeductionAmount = Some(100.00)
        ))
      ))
    )),
    None
  )

  private val priorData: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(employmentSource1),
    hmrcExpenses = None,
    customerEmploymentData = Seq(),
    customerExpenses = Some(customerExpenses)
  )

  def data(hmrcExpenses: Option[EmploymentExpenses], customerExpenses: Option[EmploymentExpenses]): AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(),
    hmrcExpenses = hmrcExpenses,
    customerEmploymentData = Seq(),
    customerExpenses = customerExpenses
  )

  ".deleteOrIgnoreExpenses" should {
    "return a successful result" when {
      "there is both hmrc expenses and customer expenses" which {
        "toRemove is equal to 'ALL'" in {

          verifySubmitEvent(Some(DecodedDeleteEmploymentExpensesPayload(expenses = Some(Expenses(
            businessTravelCosts = customerExpenses.expenses.flatMap(_.businessTravelCosts),
            jobExpenses = customerExpenses.expenses.flatMap(_.jobExpenses),
            flatRateJobExpenses = customerExpenses.expenses.flatMap(_.flatRateJobExpenses),
            professionalSubscriptions = customerExpenses.expenses.flatMap(_.professionalSubscriptions),
            hotelAndMealExpenses = customerExpenses.expenses.flatMap(_.hotelAndMealExpenses),
            otherAndCapitalAllowances = customerExpenses.expenses.flatMap(_.otherAndCapitalAllowances),
            vehicleExpenses = customerExpenses.expenses.flatMap(_.vehicleExpenses),
            mileageAllowanceRelief = customerExpenses.expenses.flatMap(_.mileageAllowanceRelief)
          ))
          ).toNrsPayloadModel))

          mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
          mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "ALL")

          val response = service.deleteOrIgnoreExpenses(authorisationRequest.user, data(Some(hmrcExpensesWithoutDateIgnored), Some(customerExpenses)), taxYear)

          await(response) shouldBe Right()
        }
      }

      "there is both hmrc expenses and customer expenses but hmrc data has dateIgnored" which {
        "toRemove is equal to 'CUSTOMER'" in {
          verifySubmitEvent(Some(DecodedDeleteEmploymentExpensesPayload(expenses = Some(Expenses(
            businessTravelCosts = customerExpenses.expenses.flatMap(_.businessTravelCosts),
            jobExpenses = customerExpenses.expenses.flatMap(_.jobExpenses),
            flatRateJobExpenses = customerExpenses.expenses.flatMap(_.flatRateJobExpenses),
            professionalSubscriptions = customerExpenses.expenses.flatMap(_.professionalSubscriptions),
            hotelAndMealExpenses = customerExpenses.expenses.flatMap(_.hotelAndMealExpenses),
            otherAndCapitalAllowances = customerExpenses.expenses.flatMap(_.otherAndCapitalAllowances),
            vehicleExpenses = customerExpenses.expenses.flatMap(_.vehicleExpenses),
            mileageAllowanceRelief = customerExpenses.expenses.flatMap(_.mileageAllowanceRelief)
          ))
          ).toNrsPayloadModel))

          mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
          mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "CUSTOMER")

          val response = service.deleteOrIgnoreExpenses(authorisationRequest.user, data(Some(hmrcExpensesWithDateIgnored), Some(customerExpenses)), taxYear)

          await(response) shouldBe Right()
        }
      }

      "there is hmrc data and no customer data" which {
        "toRemove is equal to 'HMRC-HELD'" in {
          verifySubmitEvent(Some(DecodedDeleteEmploymentExpensesPayload(expenses = Some(Expenses(
            businessTravelCosts = hmrcExpensesWithoutDateIgnored.expenses.flatMap(_.businessTravelCosts),
            jobExpenses = hmrcExpensesWithoutDateIgnored.expenses.flatMap(_.jobExpenses),
            flatRateJobExpenses = hmrcExpensesWithoutDateIgnored.expenses.flatMap(_.flatRateJobExpenses),
            professionalSubscriptions = hmrcExpensesWithoutDateIgnored.expenses.flatMap(_.professionalSubscriptions),
            hotelAndMealExpenses = hmrcExpensesWithoutDateIgnored.expenses.flatMap(_.hotelAndMealExpenses),
            otherAndCapitalAllowances = hmrcExpensesWithoutDateIgnored.expenses.flatMap(_.otherAndCapitalAllowances),
            vehicleExpenses = hmrcExpensesWithoutDateIgnored.expenses.flatMap(_.vehicleExpenses),
            mileageAllowanceRelief = hmrcExpensesWithoutDateIgnored.expenses.flatMap(_.mileageAllowanceRelief)
          ))
          ).toNrsPayloadModel))

          mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
          mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "HMRC-HELD")

          val response = service.deleteOrIgnoreExpenses(authorisationRequest.user, data(Some(hmrcExpensesWithoutDateIgnored), None), taxYear)

          await(response) shouldBe Right()
        }
      }

      "there is customer data and no hmrc data" which {
        "toRemove is equal to 'CUSTOMER'" in {
          verifySubmitEvent(Some(DecodedDeleteEmploymentExpensesPayload(expenses = Some(Expenses(
            businessTravelCosts = customerExpenses.expenses.flatMap(_.businessTravelCosts),
            jobExpenses = customerExpenses.expenses.flatMap(_.jobExpenses),
            flatRateJobExpenses = customerExpenses.expenses.flatMap(_.flatRateJobExpenses),
            professionalSubscriptions = customerExpenses.expenses.flatMap(_.professionalSubscriptions),
            hotelAndMealExpenses = customerExpenses.expenses.flatMap(_.hotelAndMealExpenses),
            otherAndCapitalAllowances = customerExpenses.expenses.flatMap(_.otherAndCapitalAllowances),
            vehicleExpenses = customerExpenses.expenses.flatMap(_.vehicleExpenses),
            mileageAllowanceRelief = customerExpenses.expenses.flatMap(_.mileageAllowanceRelief)
          ))
          ).toNrsPayloadModel))

          mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
          mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "CUSTOMER")

          val response = service.deleteOrIgnoreExpenses(authorisationRequest.user, data(None, Some(customerExpenses)), taxYear)

          await(response) shouldBe Right()
        }
      }
    }

    "returns an unsuccessful result" when {
      "there is no hmrc or customer data" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
        val response = service.deleteOrIgnoreExpenses(authorisationRequest.user, data(None, None), taxYear)

        await(response) shouldBe Right()
      }

      "the connector throws a Left" in {
        verifySubmitEvent(Some(DecodedDeleteEmploymentExpensesPayload(expenses = Some(Expenses(
          businessTravelCosts = customerExpenses.expenses.flatMap(_.businessTravelCosts),
          jobExpenses = customerExpenses.expenses.flatMap(_.jobExpenses),
          flatRateJobExpenses = customerExpenses.expenses.flatMap(_.flatRateJobExpenses),
          professionalSubscriptions = customerExpenses.expenses.flatMap(_.professionalSubscriptions),
          hotelAndMealExpenses = customerExpenses.expenses.flatMap(_.hotelAndMealExpenses),
          otherAndCapitalAllowances = customerExpenses.expenses.flatMap(_.otherAndCapitalAllowances),
          vehicleExpenses = customerExpenses.expenses.flatMap(_.vehicleExpenses),
          mileageAllowanceRelief = customerExpenses.expenses.flatMap(_.mileageAllowanceRelief)
        ))
        ).toNrsPayloadModel))

        mockRefreshIncomeSourceResponseSuccess(taxYear, nino)
        mockDeleteOrIgnoreExpensesError(nino, taxYear, "ALL")

        val response = service.deleteOrIgnoreExpenses(authorisationRequest.user, data(Some(hmrcExpensesWithoutDateIgnored), Some(customerExpenses)), taxYear)

        await(response) shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("CODE", "REASON")))
      }

      "incomeSourceConnector returns error" in {
        verifySubmitEvent(Some(DecodedDeleteEmploymentExpensesPayload(expenses = Some(Expenses(
          businessTravelCosts = customerExpenses.expenses.flatMap(_.businessTravelCosts),
          jobExpenses = customerExpenses.expenses.flatMap(_.jobExpenses),
          flatRateJobExpenses = customerExpenses.expenses.flatMap(_.flatRateJobExpenses),
          professionalSubscriptions = customerExpenses.expenses.flatMap(_.professionalSubscriptions),
          hotelAndMealExpenses = customerExpenses.expenses.flatMap(_.hotelAndMealExpenses),
          otherAndCapitalAllowances = customerExpenses.expenses.flatMap(_.otherAndCapitalAllowances),
          vehicleExpenses = customerExpenses.expenses.flatMap(_.vehicleExpenses),
          mileageAllowanceRelief = customerExpenses.expenses.flatMap(_.mileageAllowanceRelief)
        ))
        ).toNrsPayloadModel))

        mockRefreshIncomeSourceResponseError(taxYear, nino)
        mockDeleteOrIgnoreExpensesSuccess(nino, taxYear, "CUSTOMER")

        val response = service.deleteOrIgnoreExpenses(authorisationRequest.user, data(None, Some(customerExpenses)), taxYear)

        await(response) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("CODE", "REASON")))
      }
    }
  }

  "calling performNrsSubmitPayload" should {
    "send the event from the model" in {

      verifySubmitEvent(Some(DecodedDeleteEmploymentExpensesPayload(expenses = Some(Expenses(
        businessTravelCosts = customerExpenses.expenses.flatMap(_.businessTravelCosts),
        jobExpenses = customerExpenses.expenses.flatMap(_.jobExpenses),
        flatRateJobExpenses = customerExpenses.expenses.flatMap(_.flatRateJobExpenses),
        professionalSubscriptions = customerExpenses.expenses.flatMap(_.professionalSubscriptions),
        hotelAndMealExpenses = customerExpenses.expenses.flatMap(_.hotelAndMealExpenses),
        otherAndCapitalAllowances = customerExpenses.expenses.flatMap(_.otherAndCapitalAllowances),
        vehicleExpenses = customerExpenses.expenses.flatMap(_.vehicleExpenses),
        mileageAllowanceRelief = customerExpenses.expenses.flatMap(_.mileageAllowanceRelief)
      ))
      ).toNrsPayloadModel))

      await(service.performSubmitNrsPayload(authorisationRequest.user, priorData)) shouldBe Right()
    }
  }
}
