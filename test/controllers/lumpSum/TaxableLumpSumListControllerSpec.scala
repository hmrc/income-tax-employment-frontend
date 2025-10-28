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

package controllers.lumpSum

import common.SessionValues
import models.employment.EmploymentDetailsType
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.stubMessagesControllerComponents
import support.ControllerUnitTest
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.builders.models.otheremployment.session.OtherEmploymentIncomeCYAModelBuilder.anOtherEmploymentIncomeCYAModel
import support.mocks._
import utils.InYearUtil
import views.html.taxableLumpSum.TaxableLumpSumListView

import scala.concurrent.Future

class TaxableLumpSumListControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockEmploymentSessionService
  with MockAuditService
  with MockCheckEmploymentDetailsService
  with MockErrorHandler {

  private lazy val view = app.injector.instanceOf[TaxableLumpSumListView]

  private def underTest(mimic: Boolean = false, isEmploymentEOYEnabled: Boolean = true) = new TaxableLumpSumListController(
    stubMessagesControllerComponents(),
    mockActionsProvider,
    view,
    new InYearUtil,
    mockErrorHandler
  )(new MockAppConfig().config(_mimicEmploymentAPICalls = mimic, isEmploymentEOYEnabled = isEmploymentEOYEnabled))

  private val employmentId = "223AB12399"

  ".show" when {
    "return a fully populated page when all user has lump sums" in {
      mockEndOfYearSessionData(taxYearEOY,
        employmentId,
        EmploymentDetailsType,
        anEmploymentUserData.copy(employment = anEmploymentCYAModel(otherEmploymentIncome = Some(anOtherEmploymentIncomeCYAModel)))
      )

      val result: Future[Result] = {
        underTest().show(taxYearEOY, employmentId = employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))
      }
      result.map( res => res.header.status shouldBe OK)
    }

    "return an empty page when all user has no lump sums" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData.copy(employment =
        anEmploymentCYAModel()))

      val result: Future[Result] = {
        underTest().show(taxYearEOY, employmentId = employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))
      }
      result.map( res => res.header.status shouldBe OK)
    }
  }
}
