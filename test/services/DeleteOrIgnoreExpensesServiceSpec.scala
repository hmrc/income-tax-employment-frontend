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

package services

import audit.DeleteEmploymentExpensesAudit
import models.employment._
import models.expenses.Expenses
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import support.builders.models.UserBuilder.aUser
import support.mocks._
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class DeleteOrIgnoreExpensesServiceSpec extends UnitTest
  with TaxYearProvider
  with MockDeleteOrIgnoreExpensesConnector
  with MockIncomeSourceConnector
  with MockDeleteOrIgnoreExpensesService
  with MockAuditService {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val underTest: DeleteOrIgnoreExpensesService = new DeleteOrIgnoreExpensesService(
    mockDeleteOrIgnoreExpensesConnector,
    mockIncomeSourceConnector,
    mockAuditService,
    ExecutionContext.global
  )

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
      Some(s"${taxYearEOY - 1}-04-04T01:01:01Z"),
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

  def data(hmrcExpenses: Option[EmploymentExpenses], customerExpenses: Option[EmploymentExpenses]): AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(),
    hmrcExpenses = hmrcExpenses,
    customerEmploymentData = Seq(),
    customerExpenses = customerExpenses,
    otherEmploymentIncome = None
  )

  ".deleteOrIgnoreExpenses" should {
    "return a successful result" when {
      "there is both hmrc expenses and customer expenses" which {
        "toRemove is equal to 'ALL'" in {
          val deleteEmploymentExpensesAudit = DeleteEmploymentExpensesAudit(taxYear, "individual", aUser.nino, aUser.mtditid, customerExpenses.expenses.getOrElse(Expenses()))

          mockAuditSendEvent(deleteEmploymentExpensesAudit.toAuditModel)

          mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
          mockDeleteOrIgnoreExpensesSuccess(aUser.nino, taxYear, "ALL")

          val response = underTest.deleteOrIgnoreExpenses(aUser, data(Some(hmrcExpensesWithoutDateIgnored), Some(customerExpenses)), taxYear)

          await(response) shouldBe Right(())
        }
      }

      "there is both hmrc expenses and customer expenses but hmrc data has dateIgnored" which {
        "toRemove is equal to 'CUSTOMER'" in {
          val deleteEmploymentExpensesAudit = DeleteEmploymentExpensesAudit(taxYear, "individual", aUser.nino, aUser.mtditid, customerExpenses.expenses.getOrElse(Expenses()))

          mockAuditSendEvent(deleteEmploymentExpensesAudit.toAuditModel)

          mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
          mockDeleteOrIgnoreExpensesSuccess(aUser.nino, taxYear, "CUSTOMER")

          val response = underTest.deleteOrIgnoreExpenses(aUser, data(Some(hmrcExpensesWithDateIgnored), Some(customerExpenses)), taxYear)

          await(response) shouldBe Right(())
        }
      }

      "there is hmrc data and no customer data" which {
        "toRemove is equal to 'HMRC-HELD'" in {
          val deleteEmploymentExpensesAudit = DeleteEmploymentExpensesAudit(taxYear, "individual", aUser.nino, aUser.mtditid, hmrcExpensesWithoutDateIgnored.expenses.getOrElse(Expenses()))

          mockAuditSendEvent(deleteEmploymentExpensesAudit.toAuditModel)

          mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
          mockDeleteOrIgnoreExpensesSuccess(aUser.nino, taxYear, "HMRC-HELD")

          val response = underTest.deleteOrIgnoreExpenses(aUser, data(Some(hmrcExpensesWithoutDateIgnored), None), taxYear)

          await(response) shouldBe Right(())
        }
      }

      "there is customer data and no hmrc data" which {
        "toRemove is equal to 'CUSTOMER'" in {
          val deleteEmploymentExpensesAudit = DeleteEmploymentExpensesAudit(taxYear, "individual", aUser.nino, aUser.mtditid, customerExpenses.expenses.getOrElse(Expenses()))

          mockAuditSendEvent(deleteEmploymentExpensesAudit.toAuditModel)

          mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
          mockDeleteOrIgnoreExpensesSuccess(aUser.nino, taxYear, "CUSTOMER")

          val response = underTest.deleteOrIgnoreExpenses(aUser, data(None, Some(customerExpenses)), taxYear)

          await(response) shouldBe Right(())
        }
      }
    }

    "returns an unsuccessful result" when {
      "there is no hmrc or customer data" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
        val response = underTest.deleteOrIgnoreExpenses(aUser, data(None, None), taxYear)

        await(response) shouldBe Right(())
      }

      "the connector throws a Left" in {
        val deleteEmploymentExpensesAudit = DeleteEmploymentExpensesAudit(taxYear, "individual", aUser.nino, aUser.mtditid, customerExpenses.expenses.getOrElse(Expenses()))

        mockAuditSendEvent(deleteEmploymentExpensesAudit.toAuditModel)

        mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
        mockDeleteOrIgnoreExpensesError(aUser.nino, taxYear, "ALL")

        val response = underTest.deleteOrIgnoreExpenses(aUser, data(Some(hmrcExpensesWithoutDateIgnored), Some(customerExpenses)), taxYear)

        await(response) shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("CODE", "REASON")))
      }

      "incomeSourceConnector returns error" in {
        val deleteEmploymentExpensesAudit = DeleteEmploymentExpensesAudit(taxYear, "individual", aUser.nino, aUser.mtditid, customerExpenses.expenses.getOrElse(Expenses()))

        mockAuditSendEvent(deleteEmploymentExpensesAudit.toAuditModel)

        mockRefreshIncomeSourceResponseError(taxYear, aUser.nino)
        mockDeleteOrIgnoreExpensesSuccess(aUser.nino, taxYear, "CUSTOMER")

        val response = underTest.deleteOrIgnoreExpenses(aUser, data(None, Some(customerExpenses)), taxYear)

        await(response) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("CODE", "REASON")))
      }
    }
  }

}
