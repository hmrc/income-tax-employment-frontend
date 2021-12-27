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

package actions

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import models.{EmploymentUserDataRequest, User}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.Redirect
import play.api.mvc._
import repositories.EmploymentUserDataRepository
import services.RedirectService.{accommodationRelocationBenefitsRedirects, calculateRedirect}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthorisedInYearAction @Inject()(authAction: AuthorisedAction,
                                       inYearAction: InYearAction,
                                       employmentUserDataRepository: EmploymentUserDataRepository,
                                       errorHandler: ErrorHandler,
                                       appConfig: AppConfig) {

  def build(taxYear: Int, employmentId: String)
           (implicit ec: ExecutionContext): ActionBuilder[EmploymentUserDataRequest, AnyContent] = {
    authAction
      .andThen(inYearActionBuilder(taxYear, inYearAction, appConfig))
      .andThen(employmentUserDataAction(taxYear, employmentId, employmentUserDataRepository, errorHandler))
      .andThen(redirectAction(accommodationRelocationBenefitsRedirects(_, taxYear, employmentId)))
  }

  private def inYearActionBuilder(taxYear: Int, inYearAction: InYearAction, appConfig: AppConfig)
                                 (implicit ec: ExecutionContext): ActionFilter[User] = new ActionFilter[User] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: User[A]): Future[Option[Result]] = Future.successful {
      if (inYearAction.inYear(taxYear)) {
        Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      } else {
        None
      }
    }
  }

  private def employmentUserDataAction(taxYear: Int,
                                       employmentId: String,
                                       employmentUserDataRepository: EmploymentUserDataRepository,
                                       errorHandler: ErrorHandler)
                                      (implicit ec: ExecutionContext): ActionRefiner[User, EmploymentUserDataRequest] = {
    new ActionRefiner[User, EmploymentUserDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](input: User[A]): Future[Either[Result, EmploymentUserDataRequest[A]]] = {
        employmentUserDataRepository
          .find(taxYear, employmentId)(input).map {
          case Left(_) => Left(errorHandler.handleError(INTERNAL_SERVER_ERROR)(input))
          case Right(maybeUserData: Option[EmploymentUserData]) => maybeUserData
            .map(userData => Right(models.EmploymentUserDataRequest(userData, input)))
            .getOrElse(Left(Redirect(CheckYourBenefitsController.show(taxYear, employmentId))))
        }
      }
    }
  }

  private def redirectAction(cyaConditions: EmploymentCYAModel => Seq[ConditionalRedirect])
                            (implicit ec: ExecutionContext): ActionFilter[EmploymentUserDataRequest] = new ActionFilter[EmploymentUserDataRequest] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: EmploymentUserDataRequest[A]): Future[Option[Result]] = Future.successful {
      val redirect: Either[Result, EmploymentUserData] = calculateRedirect(
        request.employmentUserData.taxYear,
        request.employmentUserData.employmentId,
        Some(request.employmentUserData),
        EmploymentBenefitsType,
        cyaConditions)

      redirect match {
        case Left(redirect) => Some(redirect)
        case Right(_) => None
      }
    }
  }
}
