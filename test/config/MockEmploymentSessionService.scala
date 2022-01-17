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
import models.employment.{AllEmploymentData, EmploymentExpenses, EmploymentSource}
import models.employment.AllEmploymentData
import models.mongo.{EmploymentCYAModel, EmploymentUserData, ExpensesCYAModel, ExpensesUserData}
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData, User}
import org.scalamock.handlers._
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{Request, Result}
import services.{CreateOrAmendExpensesService, EmploymentSessionService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock

import scala.concurrent.{ExecutionContext, Future}

trait MockEmploymentSessionService extends MockFactory {

  val mockEmploymentSessionService: EmploymentSessionService = mock[EmploymentSessionService]

  val createOrAmendExpensesService: CreateOrAmendExpensesService = mock[CreateOrAmendExpensesService]

  def mockFind(taxYear: Int, result: Result): CallHandler6[User[_], Int, Option[Result], AllEmploymentData => Result, Request[_], HeaderCarrier, Future[Result]] = {
    (mockEmploymentSessionService.findPreviousEmploymentUserData(_: User[_], _: Int, _: Option[Result])
    (_: AllEmploymentData => Result)(_: Request[_], _: HeaderCarrier))
      .expects(*, taxYear, *, *, *, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()
  }

  def mockGetAndHandle(taxYear: Int, result: Result): CallHandler6[Int, String, Boolean, (Option[EmploymentUserData],
    Option[AllEmploymentData]) => Future[Result], User[_], HeaderCarrier, Future[Result]] = {
    (mockEmploymentSessionService.getAndHandle(_: Int, _: String, _: Boolean)
    (_: (Option[EmploymentUserData], Option[AllEmploymentData]) => Future[Result])(_: User[_], _: HeaderCarrier))
      .expects(taxYear, *, *, *, *, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()
  }

  def mockGetPriorRight(taxYear: Int,
                        allEmploymentData: Option[AllEmploymentData]): CallHandler3[Int, User[_], HeaderCarrier, Future[IncomeTaxUserDataResponse]] = {
    (mockEmploymentSessionService.getPriorData(_: Int)
    (_: User[_], _: HeaderCarrier))
      .expects(taxYear, *, *)
      .returns(Future.successful(Right(IncomeTaxUserData(allEmploymentData))))
      .anyNumberOfTimes()
  }

  def mockGetPriorLeft(taxYear: Int): CallHandler3[Int, User[_], HeaderCarrier, Future[IncomeTaxUserDataResponse]] = {
    (mockEmploymentSessionService.getPriorData(_: Int)
    (_: User[_], _: HeaderCarrier))
      .expects(taxYear, *, *)
      .returns(Future.successful(Left(APIErrorModel(500, APIErrorBodyModel("test", "test")))))
      .anyNumberOfTimes()
  }

  def mockGetSessionData(taxYear: Int, employmentId: String, result: Result)
                        (implicit executionContext: ExecutionContext): CallHandler5[Int, String, Option[EmploymentUserData] => Future[Result], User[_], Request[_], Future[Result]] = {
    (mockEmploymentSessionService.getSessionDataResult(_: Int, _: String)(_: Option[EmploymentUserData] => Future[Result])(_: User[_], _: Request[_]))
      .expects(taxYear, employmentId, *, *, *)
      .returns(Future(result))
  }

  def mockGetSessionDataAndReturnResult(taxYear: Int, employmentId: String, result: Result)
                                       (implicit executionContext: ExecutionContext): CallHandler5[Int, String, String, EmploymentUserData => Future[Result], User[_], Future[Result]] = {
    (mockEmploymentSessionService.getSessionDataAndReturnResult(_: Int, _: String)(_: String)(_: EmploymentUserData => Future[Result])(_: User[_]))
      .expects(taxYear, employmentId, *, *, *)
      .returns(Future(result))
  }

  def mockCreateOrUpdateUserDataWith(taxYear: Int,
                                     employmentId: String,
                                     isPriorSubmission: Boolean,
                                     hasPriorBenefits: Boolean,
                                     employmentCYAModel: EmploymentCYAModel,
                                     result: Either[Unit, EmploymentUserData])
                                    (implicit executionContext: ExecutionContext): CallHandler7[Int, String, Boolean, Boolean, EmploymentCYAModel, User[_], Clock,
    Future[Either[Unit, EmploymentUserData]]] = {
    (mockEmploymentSessionService.createOrUpdateEmploymentUserDataWith(_: Int, _: String, _: Boolean, _: Boolean, _: EmploymentCYAModel)(_: User[_], _: Clock))
      .expects(taxYear, employmentId, isPriorSubmission, hasPriorBenefits, employmentCYAModel, *, *)
      .returns(Future(result))
      .once()
  }

  def mockCreateOrUpdateUserDataWith(taxYear: Int,
                                     isPriorSubmission: Boolean,
                                     hasPriorExpenses: Boolean,
                                     expensesCYAModel: ExpensesCYAModel,
                                     result: Either[Unit, ExpensesUserData])
                                    (implicit ec: ExecutionContext): CallHandler6[Int, Boolean, Boolean, ExpensesCYAModel, User[_], Clock, Future[Either[Unit, ExpensesUserData]]] = {
    (mockEmploymentSessionService.createOrUpdateExpensesUserDataWith(_: Int, _: Boolean, _: Boolean, _: ExpensesCYAModel)(_: User[_], _: Clock))
      .expects(taxYear, isPriorSubmission, hasPriorExpenses, expensesCYAModel, *, *)
      .returns(Future(result))
      .once()
  }
}
