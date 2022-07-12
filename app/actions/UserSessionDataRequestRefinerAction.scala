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

package actions

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.{CheckEmploymentDetailsController, CheckYourBenefitsController}
import models.employment.{EmploymentBenefitsType, EmploymentDetailsType, EmploymentType}
import models.mongo.EmploymentUserData
import models.{AuthorisationRequest, UserSessionDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.EmploymentSessionService

import scala.concurrent.{ExecutionContext, Future}

case class UserSessionDataRequestRefinerAction(taxYear: Int,
                                               employmentId: String,
                                               employmentType: EmploymentType,
                                               employmentSessionService: EmploymentSessionService,
                                               errorHandler: ErrorHandler,
                                               appConfig: AppConfig
                                              )(implicit ec: ExecutionContext) extends ActionRefiner[AuthorisationRequest, UserSessionDataRequest] {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserSessionDataRequest[A]]] = {
    employmentSessionService.getSessionData(taxYear, employmentId, input.user).map {
      case Left(_) => Left(errorHandler.internalServerError()(input))
      case Right(maybeEmploymentUserData: Option[EmploymentUserData]) => maybeEmploymentUserData
        .map(employmentUserData => Right(UserSessionDataRequest(employmentUserData, input.user, input.request)))
        .getOrElse(Left(employmentTypeRedirect(employmentType, taxYear, employmentId)))
    }
  }

  private def employmentTypeRedirect(employmentType: EmploymentType, taxYear: Int, employmentId: String): Result = employmentType match {
    case EmploymentBenefitsType => Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
    case EmploymentDetailsType => Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId))
  }
}
