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

package support.mocks

import common.EmploymentSection
import connectors.parsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models._
import models.employment.createUpdate.{CreateUpdateEmploymentRequest, CreateUpdateEmploymentRequestError}
import models.employment.{AllEmploymentData, OptionalCyaAndPrior}
import models.mongo._
import org.scalamock.handlers._
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.mvc.{Request, Result}
import services.{CreateOrAmendExpensesService, EmploymentSessionService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockEmploymentSessionService extends MockFactory { _: TestSuite =>

  val mockEmploymentSessionService: EmploymentSessionService = mock[EmploymentSessionService]

  val createOrAmendExpensesService: CreateOrAmendExpensesService = mock[CreateOrAmendExpensesService]

  def mockFind(taxYear: Int, result: Result): CallHandler6[User, Int, Option[Result], AllEmploymentData => Result, Request[_], HeaderCarrier, Future[Result]] = {
    (mockEmploymentSessionService.findPreviousEmploymentUserData(_: User, _: Int, _: Option[Result])
    (_: AllEmploymentData => Result)(_: Request[_], _: HeaderCarrier))
      .expects(*, taxYear, *, *, *, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()
  }

  @deprecated("should move to other retrieval methods like mockGetOptionalCYAAndPriorForEndOfYear")
  def mockGetAndHandle(taxYear: Int, result: Result): CallHandler6[Int, String, Boolean, (Option[EmploymentUserData],
    Option[AllEmploymentData]) => Future[Result], AuthorisationRequest[_], HeaderCarrier, Future[Result]] = {
    (mockEmploymentSessionService.getAndHandle(_: Int, _: String, _: Boolean)
    (_: (Option[EmploymentUserData], Option[AllEmploymentData]) => Future[Result])(_: AuthorisationRequest[_], _: HeaderCarrier))
      .expects(taxYear, *, *, *, *, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()
  }

  def mockGetOptionalCYAAndPriorForEndOfYear(taxYear: Int, result: Either[Result, OptionalCyaAndPrior]): CallHandler4[Int, String,
    AuthorisationRequest[_], HeaderCarrier, Future[Either[Result, OptionalCyaAndPrior]]] = {
    (mockEmploymentSessionService.getOptionalCYAAndPriorForEndOfYear(_: Int, _: String)
    (_: AuthorisationRequest[_], _: HeaderCarrier))
      .expects(taxYear, *, *, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()
  }

  def mockGetPriorRight(taxYear: Int,
                        allEmploymentData: Option[AllEmploymentData]): CallHandler3[User, Int, HeaderCarrier, Future[IncomeTaxUserDataResponse]] = {
    (mockEmploymentSessionService.getPriorData(_: User, _: Int)(_: HeaderCarrier))
      .expects(*, taxYear, *)
      .returns(Future.successful(Right(IncomeTaxUserData(allEmploymentData))))
      .anyNumberOfTimes()
  }

  def mockGetPriorLeft(taxYear: Int): CallHandler3[User, Int, HeaderCarrier, Future[IncomeTaxUserDataResponse]] = {
    (mockEmploymentSessionService.getPriorData(_: User, _: Int)(_: HeaderCarrier))
      .expects(*, taxYear, *)
      .returns(Future.successful(Left(APIErrorModel(500, APIErrorBodyModel("test", "test")))))
      .anyNumberOfTimes()
  }

  def mockClear(response: Either[Unit, Unit] = Right(()), clearCya: Boolean = true): CallHandler5[User, Int, String, Boolean, HeaderCarrier, Future[Either[Unit, Unit]]] = {
    (mockEmploymentSessionService.clear(_: User, _: Int, _: String, _: Boolean)(_: HeaderCarrier))
      .expects(*, *, *, clearCya, *)
      .returns(Future.successful(response))
  }

  def mockGetSessionData(taxYear: Int,
                         employmentId: String,
                         user: User,
                         result: Either[DatabaseError, Option[EmploymentUserData]]): CallHandler3[Int, String, User, Future[Either[DatabaseError, Option[EmploymentUserData]]]] = {
    (mockEmploymentSessionService.getSessionData(_: Int, _: String, _: User))
      .expects(taxYear, employmentId, user)
      .returns(Future.successful(result))
  }

  def mockGetSessionDataAndReturnResult(taxYear: Int,
                                        employmentId: String,
                                        result: Result): CallHandler5[Int, String, String, EmploymentUserData => Future[Result], AuthorisationRequest[_], Future[Result]] = {
    (mockEmploymentSessionService.getSessionDataAndReturnResult(_: Int, _: String)(_: String)(_: EmploymentUserData => Future[Result])(_: AuthorisationRequest[_]))
      .expects(taxYear, employmentId, *, *, *)
      .returns(Future.successful(result))
  }

  def mockCreateOrUpdateUserDataWith(taxYear: Int,
                                     employmentId: String,
                                     employmentCYAModel: EmploymentCYAModel,
                                     result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, EmploymentCYAModel, Future[Either[Unit, EmploymentUserData]]] = {
    ((user: User, taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, employment: EmploymentCYAModel) =>
      mockEmploymentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, employment))
      .expects(*, taxYear, employmentId, *, employmentCYAModel)
      .returns(Future.successful(result))
      .once()
  }

  def mockCreateOrUpdateSessionData[Result](result: Result): CallHandler9[User, Int, String, EmploymentCYAModel, Boolean, Boolean, Boolean, Result, Result, Future[Result]] = {
    (mockEmploymentSessionService.createOrUpdateSessionData[Result](_: User,
      _: Int,
      _: String,
      _: EmploymentCYAModel,
      _: Boolean,
      _: Boolean,
      _: Boolean)
      (_: Result)(_: Result))
      .expects(*, *, *, *, *, *, *, *, *)
      .returns(Future.successful(result))
      .once()
  }

  def mockCreateModelOrReturnError(section: EmploymentSection, result: Either[CreateUpdateEmploymentRequestError, CreateUpdateEmploymentRequest]): CallHandler4[User,
    EmploymentUserData, Option[AllEmploymentData], EmploymentSection, Either[CreateUpdateEmploymentRequestError, CreateUpdateEmploymentRequest]] = {
    (mockEmploymentSessionService.createModelOrReturnError(_: User, _: EmploymentUserData, _: Option[AllEmploymentData], _: EmploymentSection))
      .expects(*, *, *, section)
      .returns(result)
      .once()
  }

  def mockGetAndHandleExpenses(taxYear: Int,
                               result: Result): CallHandler4[Int, (Option[ExpensesUserData], Option[AllEmploymentData]) => Future[Result], AuthorisationRequest[_], HeaderCarrier, Future[Result]] = {
    (mockEmploymentSessionService.getAndHandleExpenses(_: Int)(_: (Option[ExpensesUserData], Option[AllEmploymentData]) => Future[Result])
    (_: AuthorisationRequest[_], _: HeaderCarrier))
      .expects(taxYear, *, *, *)
      .returns(Future.successful(result))
  }

  def mockSubmitAndClear(taxYear: Int, employmentId: String, model: CreateUpdateEmploymentRequest, result: Either[Result, (Option[String], EmploymentUserData)]): CallHandler8[Int,
    String, CreateUpdateEmploymentRequest, EmploymentUserData, Option[AllEmploymentData], Option[(String, Int, CreateUpdateEmploymentRequest,
    Option[AllEmploymentData], AuthorisationRequest[_]) => Unit], AuthorisationRequest[_], HeaderCarrier, Future[Either[Result, (Option[String], EmploymentUserData)]]] = {
    (mockEmploymentSessionService.submitAndClear(_: Int, _: String, _: CreateUpdateEmploymentRequest, _: EmploymentUserData,
      _: Option[AllEmploymentData],
      _: Option[(String, Int, CreateUpdateEmploymentRequest, Option[AllEmploymentData], AuthorisationRequest[_]) => Unit])(_: AuthorisationRequest[_], _: HeaderCarrier))
      .expects(taxYear, employmentId, model, *, *, *, *, *)
      .returns(Future.successful(result))
      .once()
  }

  def mockCreateOrUpdateUserDataWith(taxYear: Int,
                                     isPriorSubmission: Boolean,
                                     hasPriorExpenses: Boolean,
                                     expensesCYAModel: ExpensesCYAModel,
                                     result: Either[Unit, ExpensesUserData]): CallHandler5[User, Int, Boolean, Boolean, ExpensesCYAModel, Future[Either[Unit, ExpensesUserData]]] = {
    (mockEmploymentSessionService.createOrUpdateExpensesUserData(_: User, _: Int, _: Boolean, _: Boolean, _: ExpensesCYAModel))
      .expects(*, taxYear, isPriorSubmission, hasPriorExpenses, expensesCYAModel)
      .returns(Future.successful(result))
      .once()
  }

  def mockFindEmploymentUserData(taxYear: Int,
                                 employmentId: String,
                                 user: User,
                                 result: Either[Unit, Option[EmploymentUserData]]): Unit = {
    (mockEmploymentSessionService.findEmploymentUserData(_: Int, _: String, _: User))
      .expects(taxYear, employmentId, user)
      .returns(Future.successful(result))
  }
}
