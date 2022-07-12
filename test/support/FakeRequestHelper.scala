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

package support

import common.SessionValues
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import support.builders.models.UserBuilder.aUser

trait FakeRequestHelper {

  protected val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  protected val fakeIndividualRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withHeaders(newHeaders = "X-Session-ID" -> aUser.sessionId)

  protected val fakeAgentRequest: FakeRequest[AnyContentAsEmpty.type] = fakeIndividualRequest
    .withSession(SessionValues.CLIENT_MTDITID -> aUser.mtditid, SessionValues.CLIENT_NINO -> aUser.nino)
}
