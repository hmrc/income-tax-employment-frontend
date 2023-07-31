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
import forms.AmountForm
import forms.lumpSums.LumpSumFormsProvider
import models.employment.EmploymentDetailsType
import models.otheremployment.session.{OtherEmploymentIncomeCYAModel, TaxableLumpSum}
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.stubMessagesControllerComponents
import sttp.model.Method.POST
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.builders.models.otheremployment.session.OtherEmploymentIncomeCYAModelBuilder.anOtherEmploymentIncomeCYAModel
import support.mocks._
import utils.InYearUtil
import views.html.taxableLumpSum.TaxableLumpSumAmountView

import scala.concurrent.Future

class TaxableLumpSumAmountControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockEmploymentSessionService
  with MockOtherEmploymentInfoService
  with MockAuditService
  with MockCheckEmploymentDetailsService
  with MockErrorHandler
  with MockRedirectService {

  private lazy val view = app.injector.instanceOf[TaxableLumpSumAmountView]
  private val formsProvider = new LumpSumFormsProvider


  private def underTest(mimic: Boolean = false, isEmploymentEOYEnabled: Boolean = true) = new TaxableLumpSumAmountController(
    stubMessagesControllerComponents(),
    mockActionsProvider,
    formsProvider,
    view,
    new InYearUtil,
    mockOtherEmploymentInfoService,
    mockErrorHandler
  )(new MockAppConfig().config(_mimicEmploymentAPICalls = mimic, isEmploymentEOYEnabled = isEmploymentEOYEnabled), ec)


  private val employmentId = "223AB12399"

  ".show" when {
    "return an ok when the user has a lump sum" in {
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
      result.map(res => res.header.status shouldBe OK)
    }

    "return an ok when the user has no lump sums" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData.copy(employment =
        anEmploymentCYAModel()))

      val result: Future[Result] = {
        underTest().show(taxYearEOY, employmentId = employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))
      }
      result.map(res => res.header.status shouldBe OK)
    }
  }
}
