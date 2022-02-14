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

package config

import connectors.parsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models._
import models.employment.AllEmploymentData
import models.mongo.{EmploymentCYAModel, EmploymentUserData, ExpensesCYAModel, ExpensesUserData}
import org.scalamock.handlers._
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{Request, Result}
import services.{CreateOrAmendExpensesService, EmploymentSessionService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockEmploymentSessionService extends MockFactory {

  val mockEmploymentSessionService: EmploymentSessionService = mock[EmploymentSessionService]

  val createOrAmendExpensesService: CreateOrAmendExpensesService = mock[CreateOrAmendExpensesService]

  def mockFind(taxYear: Int, result: Result): CallHandler6[User, Int, Option[Result], AllEmploymentData => Result, Request[_], HeaderCarrier, Future[Result]] = {
    (mockEmploymentSessionService.findPreviousEmploymentUserData(_: User, _: Int, _: Option[Result])
    (_: AllEmploymentData => Result)(_: Request[_], _: HeaderCarrier))
      .expects(*, taxYear, *, *, *, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()
  }

  def mockGetAndHandle(taxYear: Int, result: Result): CallHandler6[Int, String, Boolean, (Option[EmploymentUserData],
    Option[AllEmploymentData]) => Future[Result], AuthorisationRequest[_], HeaderCarrier, Future[Result]] = {
    (mockEmploymentSessionService.getAndHandle(_: Int, _: String, _: Boolean)
    (_: (Option[EmploymentUserData], Option[AllEmploymentData]) => Future[Result])(_: AuthorisationRequest[_], _: HeaderCarrier))
      .expects(taxYear, *, *, *, *, *)
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

  def mockGetSessionData(taxYear: Int, employmentId: String, result: Result)
                        (implicit executionContext: ExecutionContext): CallHandler4[Int, String, Option[EmploymentUserData] => Future[Result], AuthorisationRequest[_], Future[Result]] = {
    (mockEmploymentSessionService.getSessionDataResult(_: Int, _: String)(_: Option[EmploymentUserData] => Future[Result])(_: AuthorisationRequest[_]))
      .expects(taxYear, employmentId, *, *)
      .returns(Future(result))
  }

  def mockGetSessionDataAndReturnResult(taxYear: Int, employmentId: String, result: Result)
                                       (implicit executionContext: ExecutionContext): CallHandler5[Int, String, String, EmploymentUserData => Future[Result], AuthorisationRequest[_], Future[Result]] = {
    (mockEmploymentSessionService.getSessionDataAndReturnResult(_: Int, _: String)(_: String)(_: EmploymentUserData => Future[Result])(_: AuthorisationRequest[_]))
      .expects(taxYear, employmentId, *, *, *)
      .returns(Future(result))
  }

  def mockCreateOrUpdateUserDataWith(taxYear: Int,
                                     employmentId: String,
                                     employmentUserData: EmploymentUserData,
                                     employmentCYAModel: EmploymentCYAModel,
                                     result: Either[Unit, EmploymentUserData])
                                    (implicit executionContext: ExecutionContext): CallHandler5[Int, String, User, EmploymentUserData, EmploymentCYAModel, Future[Either[Unit, EmploymentUserData]]] = {
    (mockEmploymentSessionService.createOrUpdateEmploymentUserDataWith(_: Int, _: String, _: User, _: EmploymentUserData, _: EmploymentCYAModel))
      .expects(taxYear, employmentId, *, *, employmentCYAModel)
      .returns(Future(result))
      .once()
  }

  def mockCreateOrUpdateUserDataWith(taxYear: Int,
                                     isPriorSubmission: Boolean,
                                     hasPriorExpenses: Boolean,
                                     expensesCYAModel: ExpensesCYAModel,
                                     result: Either[Unit, ExpensesUserData])
                                    (implicit ec: ExecutionContext): CallHandler5[User, Int, Boolean, Boolean, ExpensesCYAModel, Future[Either[Unit, ExpensesUserData]]] = {
    (mockEmploymentSessionService.createOrUpdateExpensesUserDataWith(_: User, _: Int, _: Boolean, _: Boolean, _: ExpensesCYAModel))
      .expects(*, taxYear, isPriorSubmission, hasPriorExpenses, expensesCYAModel)
      .returns(Future(result))
      .once()
  }
}
