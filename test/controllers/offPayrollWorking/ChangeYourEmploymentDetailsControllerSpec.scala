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

package controllers.offPayrollWorking

import play.api.http.Status.OK
import play.api.test.Helpers.{contentType, status, stubMessagesControllerComponents}
import support.ControllerUnitTest
import support.mocks.MockActionsProvider
import views.html.offPayrollWorking.ChangeYourEmploymentDetailsView


class ChangeYourEmploymentDetailsControllerSpec extends ControllerUnitTest with MockActionsProvider {

  private val view = inject[ChangeYourEmploymentDetailsView]

  private val underTest = new ChangeYourEmploymentDetailsController(
    stubMessagesControllerComponents(),
    mockAuthorisedAction,
    view
  )

  ".show" must {
    "must return OK and the correct view for a GET (Individual)" in {
      val result = underTest.show(taxYearEOY).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }

    //    "must return OK and the correct view for a GET (Agent)" in {
    //      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)
    //
    //      val result = underTest.show(taxYearEOY).apply(fakeAgentRequest)
    //
    //      status(result) shouldBe OK
    //      contentType(result) shouldBe Some("text/html")
    //    }
  }

}
