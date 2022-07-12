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

import actions.ActionsProvider
import models.UserSessionDataRequest
import models.employment.EmploymentType
import org.scalamock.handlers.CallHandler4
import play.api.mvc._
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData

import scala.concurrent.{ExecutionContext, Future}

trait MockActionsProvider extends MockAuthorisedAction
  with MockEmploymentSessionService
  with MockErrorHandler {


  protected val mockActionsProvider: ActionsProvider = mock[ActionsProvider]

  def mockEndOfYearWithSessionData(taxYear: Int,
                                   employmentId: String,
                                   employmentType: EmploymentType,
                                   controllerName: String
                                  ): CallHandler4[Int, String, EmploymentType, String, ActionBuilder[UserSessionDataRequest, AnyContent]] = {
    val actionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(anEmploymentUserData.copy(employmentId = employmentId, taxYear = taxYear), aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

    (mockActionsProvider.endOfYearWithSessionData(_: Int, _: String, _: EmploymentType, _: String))
      .expects(taxYear, employmentId, employmentType, controllerName)
      .returns(value = actionBuilder)
  }
}
