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

import audit.DeleteEmploymentAudit
import models.employment._
import models.expenses.Expenses
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import services.employment.RemoveEmploymentService
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.UserBuilder.aUser
import support.mocks._
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class RemoveEmploymentServiceSpec extends UnitTest
  with TaxYearProvider
  with MockDeleteOrIgnoreEmploymentConnector
  with MockIncomeSourceConnector
  with MockAuditService
  with MockEmploymentUserDataRepository
  with MockDeleteOrIgnoreExpensesService {

  private implicit val headerCarrier: HeaderCarrier = new HeaderCarrier()

  private val underTest: RemoveEmploymentService = new RemoveEmploymentService(
    mockDeleteOrIgnoreEmploymentConnector,
    mockIncomeSourceConnector,
    mockDeleteOrIgnoreExpensesService,
    mockAuditService,
    ExecutionContext.global
  )

  private val employmentId: String = "001"
  private val differentEmploymentId: String = "003"

  private val empSource: HmrcEmploymentSource = HmrcEmploymentSource(
    employmentId = "001",
    employerName = "maggie",
    employerRef = None,
    payrollId = None,
    startDate = None,
    cessationDate = None,
    dateIgnored = None,
    submittedOn = None,
    None,
    None
  )

  private val data: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(empSource),
    hmrcExpenses = None,
    customerEmploymentData = Seq(empSource.toEmploymentSource.copy(employmentId = "002")),
    customerExpenses = None,
    otherEmploymentIncome = None
  )

  private val customerExpenses =
    EmploymentExpenses(
      None,
      None,
      Some(40.00),
      Some(Expenses(Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5)))
    )

  ".deleteOrIgnoreEmployment" should {
    "return a successful result" when {
      "there is hmrc data and no customer data" which {
        "toRemove is equal to 'HMRC-HELD'" in {
          val allEmploymentData = data.copy(customerEmploymentData = Seq())
          val hmrcDataSource = data.hmrcEmploymentData.find(_.employmentId.equals(employmentId)).get
          val employmentDetailsViewModel: EmploymentDetailsViewModel = hmrcDataSource.toEmploymentSource.toEmploymentDetailsViewModel(isUsingCustomerData = false)
          val deleteEmploymentAudit = DeleteEmploymentAudit(taxYear, "individual", aUser.nino, aUser.mtditid, employmentDetailsViewModel, None, None)

          mockAuditSendEvent(deleteEmploymentAudit.toAuditModel)

          mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
          mockDeleteOrIgnoreEmploymentRight(aUser.nino, taxYear, employmentId, "HMRC-HELD")
          mockDeleteOrIgnoreExpenses(anAuthorisationRequest, allEmploymentData, taxYear)

          await(underTest.deleteOrIgnoreEmployment(allEmploymentData, taxYear, employmentId, aUser.copy(sessionId = aUser.sessionId))) shouldBe Right(())
        }
      }

      "there is customer data and no hmrc data" which {
        "toRemove is equal to 'CUSTOMER'" in {
          val allEmploymentData = data.copy(hmrcEmploymentData = Seq())
          val customerDataSource = data.customerEmploymentData.find(_.employmentId.equals("002")).get
          val employmentDetailsViewModel: EmploymentDetailsViewModel = customerDataSource.toEmploymentDetailsViewModel(isUsingCustomerData = true)
          val deleteEmploymentAudit = DeleteEmploymentAudit(taxYear, "individual", aUser.nino, aUser.mtditid, employmentDetailsViewModel, None, None)

          mockAuditSendEvent(deleteEmploymentAudit.toAuditModel)
          mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
          mockDeleteOrIgnoreEmploymentRight(aUser.nino, taxYear, "002", "CUSTOMER")
          mockDeleteOrIgnoreExpenses(anAuthorisationRequest, allEmploymentData, taxYear)

          await(underTest.deleteOrIgnoreEmployment(allEmploymentData, taxYear, employmentId = "002", aUser.copy(sessionId = aUser.sessionId))) shouldBe Right(())
        }
      }
    }

    "returns an unsuccessful result" when {
      "there is no hmrc or customer data" in {
        val allEmploymentData = data.copy(hmrcEmploymentData = Seq(), customerEmploymentData = Seq())

        mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
        mockDeleteOrIgnoreEmploymentRight(aUser.nino, taxYear, employmentId = "002", toRemove = "CUSTOMER")

        await(underTest.deleteOrIgnoreEmployment(allEmploymentData, taxYear, employmentId = "002", aUser)) shouldBe Right(())
      }

      "there is no employment data for that employment id" in {
        mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)

        await(underTest.deleteOrIgnoreEmployment(data, taxYear, differentEmploymentId, aUser)) shouldBe Right(())
      }

      "the connector throws a Left" in {
        val customerDataSource = data.customerEmploymentData.find(_.employmentId.equals("002")).get
        val employmentDetailsViewModel: EmploymentDetailsViewModel = customerDataSource.toEmploymentDetailsViewModel(isUsingCustomerData = true)
        val deleteEmploymentAudit = DeleteEmploymentAudit(taxYear, "individual", aUser.nino, aUser.mtditid, employmentDetailsViewModel, None, None)
        val allEmploymentData = AllEmploymentData(List(), None, List(customerDataSource), None, None)

        mockAuditSendEvent(deleteEmploymentAudit.toAuditModel)
        mockRefreshIncomeSourceResponseSuccess(taxYear, aUser.nino)
        mockDeleteOrIgnoreEmploymentLeft(aUser.nino, taxYear, "002", "CUSTOMER")
        mockDeleteOrIgnoreExpenses(anAuthorisationRequest, allEmploymentData, taxYear)

        await(underTest.deleteOrIgnoreEmployment(allEmploymentData, taxYear, "002", aUser)) shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("", "")))
      }

      "incomeSourceConnector returns error" in {
        val allEmploymentData = data.copy(hmrcEmploymentData = Seq())
        val customerDataSource = data.customerEmploymentData.find(_.employmentId.equals("002")).get
        val employmentDetailsViewModel: EmploymentDetailsViewModel = customerDataSource.toEmploymentDetailsViewModel(isUsingCustomerData = true)
        val deleteEmploymentAudit = DeleteEmploymentAudit(taxYear, "individual", aUser.nino, aUser.mtditid, employmentDetailsViewModel, None, None)

        mockAuditSendEvent(deleteEmploymentAudit.toAuditModel)
        mockRefreshIncomeSourceResponseError(taxYear, aUser.nino)
        mockDeleteOrIgnoreEmploymentRight(aUser.nino, taxYear, "002", "CUSTOMER")
        mockDeleteOrIgnoreExpenses(anAuthorisationRequest, allEmploymentData, taxYear)

        await(underTest.deleteOrIgnoreEmployment(allEmploymentData, taxYear, "002",
          aUser.copy(sessionId = aUser.sessionId))) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("CODE", "REASON")))
      }
    }
  }
}
