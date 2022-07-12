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

package support.mocks

import config.ErrorHandler
import models.AuthorisationRequest
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{Request, Result}

trait MockErrorHandler extends MockFactory {

  protected val mockErrorHandler: ErrorHandler = mock[ErrorHandler]

  def mockHandleError(status: Int, result: Result): Unit = {
    (mockErrorHandler.handleError(_: Int)(_: Request[_]))
      .expects(status, *)
      .returns(result)
  }

  def mockInternalServerError(result: Result): Unit = {
    (mockErrorHandler.internalServerError()(_: AuthorisationRequest[_]))
      .expects(*)
      .returns(result)
  }
}
