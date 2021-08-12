/*
 * Copyright 2021 HM Revenue & Customs
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

import models.User
import models.employment.AllEmploymentData
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import play.api.mvc.Result
import services.DeleteOrIgnoreExpensesService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockDeleteOrIgnoreExpensesService extends MockFactory {

  val mockDeleteOrIgnoreExpensesService: DeleteOrIgnoreExpensesService = mock[DeleteOrIgnoreExpensesService]

  def mockDeleteOrIgnoreExpenses(user: User[_], employmentData: AllEmploymentData, taxYear: Int)(result: Result): CallHandler5[AllEmploymentData, Int, Result, User[_], HeaderCarrier, Future[Result]] = {
    (mockDeleteOrIgnoreExpensesService.deleteOrIgnoreExpenses(_: AllEmploymentData, _: Int)(_: Result)(_: User[_], _:HeaderCarrier))
      .expects(employmentData, taxYear, result, user, *)
      .returns(Future.successful(result))
      .anyNumberOfTimes()
  }

}
