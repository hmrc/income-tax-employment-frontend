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

import models.User
import models.mongo.EmploymentUserData
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import services.benefits.AccommodationService

import scala.concurrent.Future

trait MockAccommodationService extends MockFactory {

  protected val mockAccommodationService: AccommodationService = mock[AccommodationService]

  def mockSaveSectionQuestion(user: User,
                              taxYear: Int,
                              employmentId: String,
                              originalEmploymentUserData: EmploymentUserData,
                              questionValue: Boolean,
                              result: Either[Unit, EmploymentUserData]
                             ): CallHandler5[User, Int, String, EmploymentUserData, Boolean, Future[Either[Unit, EmploymentUserData]]] = {
    (mockAccommodationService.saveSectionQuestion(_: User, _: Int, _: String, _: EmploymentUserData, _: Boolean))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, questionValue)
      .returns(Future.successful(result))
  }

  def mockUpdateAccommodationQuestion(user: User,
                                      taxYear: Int,
                                      employmentId: String,
                                      originalEmploymentUserData: EmploymentUserData,
                                      questionValue: Boolean,
                                      result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, Boolean, Future[Either[Unit, EmploymentUserData]]] = {
    (mockAccommodationService.updateAccommodationQuestion(_: User, _: Int, _: String, _: EmploymentUserData, _: Boolean))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, questionValue)
      .returns(Future.successful(result))
  }

  def mockUpdateAccommodation(user: User,
                              taxYear: Int,
                              employmentId: String,
                              originalEmploymentUserData: EmploymentUserData,
                              amount: BigDecimal,
                              result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, BigDecimal, Future[Either[Unit, EmploymentUserData]]] = {
    (mockAccommodationService.updateAccommodation(_: User, _: Int, _: String, _: EmploymentUserData, _: BigDecimal))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, amount)
      .returns(Future.successful(result))
  }

  def mockUpdateNonQualifyingExpensesQuestion(user: User,
                                              taxYear: Int,
                                              employmentId: String,
                                              originalEmploymentUserData: EmploymentUserData,
                                              questionValue: Boolean,
                                              result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, Boolean, Future[Either[Unit, EmploymentUserData]]] = {
    (mockAccommodationService.updateNonQualifyingExpensesQuestion(_: User, _: Int, _: String, _: EmploymentUserData, _: Boolean))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, questionValue)
      .returns(Future.successful(result))
  }

  def mockUpdateNonQualifyingExpenses(user: User,
                                      taxYear: Int,
                                      employmentId: String,
                                      originalEmploymentUserData: EmploymentUserData,
                                      amount: BigDecimal,
                                      result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, BigDecimal, Future[Either[Unit, EmploymentUserData]]] = {
    (mockAccommodationService.updateNonQualifyingExpenses(_: User, _: Int, _: String, _: EmploymentUserData, _: BigDecimal))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, amount)
      .returns(Future.successful(result))
  }

  def mockUpdateQualifyingExpensesQuestion(user: User,
                                           taxYear: Int,
                                           employmentId: String,
                                           originalEmploymentUserData: EmploymentUserData,
                                           questionValue: Boolean,
                                           result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, Boolean, Future[Either[Unit, EmploymentUserData]]] = {
    (mockAccommodationService.updateQualifyingExpensesQuestion(_: User, _: Int, _: String, _: EmploymentUserData, _: Boolean))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, questionValue)
      .returns(Future.successful(result))
  }

  def mockUpdateQualifyingExpenses(user: User,
                                   taxYear: Int,
                                   employmentId: String,
                                   originalEmploymentUserData: EmploymentUserData,
                                   amount: BigDecimal,
                                   result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, BigDecimal, Future[Either[Unit, EmploymentUserData]]] = {
    (mockAccommodationService.updateQualifyingExpenses(_: User, _: Int, _: String, _: EmploymentUserData, _: BigDecimal))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, amount)
      .returns(Future.successful(result))
  }
}
