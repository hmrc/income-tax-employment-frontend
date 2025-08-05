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

import models.employment.AllEmploymentData
import models.{APIErrorModel, AuthorisationRequest, User}
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import services.DeleteOrIgnoreExpensesService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockDeleteOrIgnoreExpensesService extends MockFactory { _: TestSuite =>

  val mockDeleteOrIgnoreExpensesService: DeleteOrIgnoreExpensesService = mock[DeleteOrIgnoreExpensesService]

  def mockDeleteOrIgnoreExpenses(user: AuthorisationRequest[_], employmentData: AllEmploymentData, taxYear: Int): CallHandler4[User,
    AllEmploymentData, Int, HeaderCarrier, Future[Either[APIErrorModel, Unit]]] = {
      (mockDeleteOrIgnoreExpensesService.deleteOrIgnoreExpenses(_: User, _: AllEmploymentData, _: Int)(_:HeaderCarrier))
        .expects(user.user, employmentData, taxYear, *)
        .returns(Future.successful(Right(())))
        .anyNumberOfTimes()
  }
}
