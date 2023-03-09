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
import services.benefits.AssetsService

import scala.concurrent.Future

trait MockAssetsService extends MockFactory {

  protected val mockAssetsService: AssetsService = mock[AssetsService]

  def mockUpdateAssetTransferQuestion(user: User,
                                      taxYear: Int,
                                      employmentId: String,
                                      originalEmploymentUserData: EmploymentUserData,
                                      questionValue: Boolean,
                                      result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, Boolean, Future[Either[Unit, EmploymentUserData]]] = {
    (mockAssetsService.updateAssetTransferQuestion(_: User, _: Int, _: String, _: EmploymentUserData, _: Boolean))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, questionValue)
      .returns(Future.successful(result))
  }

  def mockUpdateAssetTransfer(user: User,
                              taxYear: Int,
                              employmentId: String,
                              originalEmploymentUserData: EmploymentUserData,
                              amount: BigDecimal,
                              result: Either[Unit, EmploymentUserData]): CallHandler5[User, Int, String, EmploymentUserData, BigDecimal, Future[Either[Unit, EmploymentUserData]]] = {
    (mockAssetsService.updateAssetTransfer(_: User, _: Int, _: String, _: EmploymentUserData, _: BigDecimal))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, amount)
      .returns(Future.successful(result))
  }
}
