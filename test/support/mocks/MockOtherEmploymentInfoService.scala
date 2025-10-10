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
import models.otheremployment.session.TaxableLumpSum
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import services.employment.OtherEmploymentInfoService

import scala.concurrent.Future

trait MockOtherEmploymentInfoService extends MockFactory { _: TestSuite =>

  val mockOtherEmploymentInfoService: OtherEmploymentInfoService = mock[OtherEmploymentInfoService]

  def mockUpdateLumpSums(user: User,
                         taxYear: Int,
                         employmentId: String,
                         originalEmploymentUserData: EmploymentUserData,
                         newLumSum: Seq[TaxableLumpSum],
                         result: Future[Either[Unit, EmploymentUserData]]): CallHandler5[User, Int, String, EmploymentUserData, Seq[TaxableLumpSum], Future[Either[Unit, EmploymentUserData]]] = {
    (mockOtherEmploymentInfoService.updateLumpSums(_: User, _: Int, _: String, _: EmploymentUserData, _: Seq[TaxableLumpSum]))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, newLumSum)
      .returning(result)
  }

}
