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

package services.tailorings

import models.employment._
import models.expenses.Expenses
import models.tailoring.{ExcludeJourneyModel, ExcludedJourneysResponseModel}
import models.{APIErrorBodyModel, APIErrorModel, User}
import services.tailoring.TailoringService
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.UserBuilder.aUser
import support.mocks._
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class TailoringServiceSpec extends UnitTest
  with TaxYearProvider
  with MockTailoringConnector
  with MockRemoveEmploymentService
  with MockDeleteOrIgnoreExpensesService
  with MockDeleteOrIgnoreEmploymentConnector
  with MockAuditService {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val underTest: TailoringService = new TailoringService(
    mockTailoringDataConnector,
    mockRemoveEmploymentService,
    mockDeleteOrIgnoreExpensesService,
    ExecutionContext.global
  )

  private val customerExpenses =
    EmploymentExpenses(
      None,
      None,
      Some(40.00),
      Some(Expenses(Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5), Some(5)))
    )

  private val employmentSource1: HmrcEmploymentSource = HmrcEmploymentSource(
    employmentId = "001",
    employerName = "Mishima Zaibatsu",
    employerRef = Some("223/AB12399"),
    payrollId = Some("123456789999"),
    startDate = Some("2019-04-21"),
    cessationDate = Some(s"${taxYearEOY - 1}-03-11"),
    dateIgnored = None,
    submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"),
    hmrcEmploymentFinancialData = Some(EmploymentFinancialData(
      employmentData = Some(EmploymentData(
        submittedOn = s"${taxYearEOY - 1}-02-12",
        employmentSequenceNumber = Some("123456789999"),
        companyDirector = Some(true),
        closeCompany = Some(false),
        directorshipCeasedDate = Some(s"${taxYearEOY - 1}-02-12"),
        disguisedRemuneration = Some(false),
        offPayrollWorker = Some(false),
        pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some(s"${taxYearEOY - 1}-04-23"), Some(32), Some(2))),
        Some(Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(100.00),
            pglDeductionAmount = Some(100.00)
          ))
        ))
      )),
      None
    )),
    None,
    offPayrollWorkingStatus = Some(false)
  )
  private val employmentSource2: EmploymentSource =
    EmploymentSource(employmentId = "002", employerName = "maggie", None, None, None, None, None, None, None, None, None)

  private val priorData: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(employmentSource1),
    hmrcExpenses = None,
    customerEmploymentData = Seq(employmentSource2),
    customerExpenses = Some(customerExpenses),
    None
  )

  def data(hmrcExpenses: Option[EmploymentExpenses], customerExpenses: Option[EmploymentExpenses]): AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(),
    hmrcExpenses = hmrcExpenses,
    customerEmploymentData = Seq(),
    customerExpenses = customerExpenses,
    None
  )

  ".deleteOrIgnoreAllEmployment" should {
    "return a successful result" when {
      "there is both hmrc and customer data with expenses" in {

          mockDeleteOrIgnore(priorData, taxYear, "001")
          mockDeleteOrIgnore(priorData, taxYear, "002")
          mockDeleteOrIgnoreExpenses(anAuthorisationRequest, priorData, taxYear)
          val response = underTest.deleteOrIgnoreAllEmployment(priorData, taxYear, aUser)

          await(response) shouldBe Right(())
      }
    }
    "return an error " when {
      "a call fails" in {
        mockDeleteOrIgnore(priorData, taxYear, "001")
        mockDeleteOrIgnore(priorData, taxYear, "002")
        (mockDeleteOrIgnoreExpensesService.deleteOrIgnoreExpenses(_: User, _: AllEmploymentData, _: Int)(_: HeaderCarrier))
          .expects(aUser, priorData, taxYear, *)
          .returns(Future.successful(Left(APIErrorModel(500, APIErrorBodyModel("500", "")))))
          .anyNumberOfTimes()
        val response = underTest.deleteOrIgnoreAllEmployment(priorData, taxYear, aUser)

        await(response) shouldBe Left(APIErrorModel(500, APIErrorBodyModel("[TailoringService] Delete/Ignore ALL Employment Error", "Error Deleting/Ignore all Employment/ExpensesData")))
      }
    }
  }

  ".getExcludedJourneys" should {
    "return a successful result" in {

          mockGetExcludedJourneys(ExcludedJourneysResponseModel(Seq(ExcludeJourneyModel("employment", None))), taxYear,"nino")
          val response = underTest.getExcludedJourneys(taxYear, "nino", "1234567890")

          await(response) shouldBe Right(ExcludedJourneysResponseModel(Seq(ExcludeJourneyModel("employment", None))))
    }
  }
  ".clearExcludedJourneys" should {
    "return a successful result" in {

          mockClearExcludedJourneys(taxYear, "nino")
          val response = underTest.clearExcludedJourney(taxYear, "nino", "1234567890")

          await(response) shouldBe Right(true)
    }
  }
  ".postExcludedJourneys" should {
    "return a successful result" in {

          mockPostExcludedJourneys(taxYear, "nino")
          val response = underTest.postExcludedJourney(taxYear, "nino", "1234567890")

          await(response) shouldBe Right(true)
    }
  }
}
