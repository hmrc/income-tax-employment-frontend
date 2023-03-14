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
import services.benefits.FuelService

import scala.concurrent.Future

trait MockFuelService extends MockFactory {

  val mockFuelService: FuelService = mock[FuelService]

  def mockUpdateSectionQuestion(user: User,
                                taxYear: Int,
                                employmentId: String,
                                originalEmploymentUserData: EmploymentUserData,
                                questionValue: Boolean,
                                result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, Boolean, Future[Either[Unit, EmploymentUserData]]] = {
    (mockFuelService.updateSectionQuestion(_: User, _: Int, _: String, _: EmploymentUserData, _: Boolean))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, questionValue)
      .returns(Future.successful(result))
  }

  def mockUpdateCarQuestion(user: User,
                            taxYear: Int,
                            employmentId: String,
                            originalEmploymentUserData: EmploymentUserData,
                            questionValue: Boolean,
                            result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, Boolean, Future[Either[Unit, EmploymentUserData]]] = {
    (mockFuelService.updateCarQuestion(_: User, _: Int, _: String, _: EmploymentUserData, _: Boolean))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, questionValue)
      .returns(Future.successful(result))
  }

  def mockUpdateCar(user: User,
                    taxYear: Int,
                    employmentId: String,
                    originalEmploymentUserData: EmploymentUserData,
                    amount: BigDecimal,
                    result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, BigDecimal, Future[Either[Unit, EmploymentUserData]]] = {
    (mockFuelService.updateCar(_: User, _: Int, _: String, _: EmploymentUserData, _: BigDecimal))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, amount)
      .returning(Future.successful(result))
  }

  def mockUpdateCarFuel(user: User,
                        taxYear: Int,
                        employmentId: String,
                        originalEmploymentUserData: EmploymentUserData,
                        amount: BigDecimal,
                        result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, BigDecimal, Future[Either[Unit, EmploymentUserData]]] = {
    (mockFuelService.updateCarFuel(_: User, _: Int, _: String, _: EmploymentUserData, _: BigDecimal))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, amount)
      .returning(Future.successful(result))
  }
}
