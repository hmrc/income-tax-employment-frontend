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
import javax.inject.Inject
import models.{AuthorisationRequest, IncomeTaxUserData, UserPriorDataRequest, UserSessionDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.EmploymentSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.InYearUtil

import scala.concurrent.{ExecutionContext, Future}

class ActionsProvider @Inject()(authAction: AuthorisedAction,
                                employmentSessionService: EmploymentSessionService,
                                errorHandler: ErrorHandler,
                                inYearUtil: InYearUtil,
                                appConfig: AppConfig
                               )(implicit ec: ExecutionContext) {

  def inYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] = authAction.andThen(inYearActionBuilder(taxYear))

  def notInYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] = authAction.andThen(notInYearActionBuilder(taxYear))

  def notInYearWithSessionData(taxYear: Int, employmentId: String): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authAction
      .andThen(notInYearActionBuilder(taxYear))
      .andThen(employmentUserDataAction(taxYear, employmentId))

  def notInYearWithPriorData(taxYear: Int, overrideRedirect: Option[Result] = None): ActionBuilder[UserPriorDataRequest, AnyContent] =
    authAction
      .andThen(notInYearActionBuilder(taxYear))
      .andThen(employmentPriorDataAction(taxYear, overrideRedirect))

  private def notInYearActionBuilder(taxYear: Int): ActionFilter[AuthorisationRequest] = new ActionFilter[AuthorisationRequest] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: AuthorisationRequest[A]): Future[Option[Result]] = Future.successful {
      if (inYearUtil.inYear(taxYear)) {
        Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      } else {
        None
      }
    }
  }

  private def inYearActionBuilder(taxYear: Int): ActionFilter[AuthorisationRequest] = new ActionFilter[AuthorisationRequest] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: AuthorisationRequest[A]): Future[Option[Result]] = Future.successful {
      if (!inYearUtil.inYear(taxYear)) {
        Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      } else {
        None
      }
    }
  }

  private def employmentUserDataAction(taxYear: Int, employmentId: String)
                                      (implicit ec: ExecutionContext): ActionRefiner[AuthorisationRequest, UserSessionDataRequest] = {
    new ActionRefiner[AuthorisationRequest, UserSessionDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserSessionDataRequest[A]]] = {

        employmentSessionService.getSessionData(taxYear, employmentId)(input).map {
          case Left(_) => Left(errorHandler.internalServerError()(input))
          case Right(None) => Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          case Right(Some(employmentUserData)) => Right(UserSessionDataRequest(employmentUserData, input.user, input.request))
        }
      }
    }
  }

  private def employmentPriorDataAction(taxYear: Int, overrideRedirect: Option[Result] = None)
                                       (implicit ec: ExecutionContext): ActionRefiner[AuthorisationRequest, UserPriorDataRequest] = {
    new ActionRefiner[AuthorisationRequest, UserPriorDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserPriorDataRequest[A]]] = {

        implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(input.request, input.request.session)

        employmentSessionService.getPriorData(input.user, taxYear).map {
          case Left(error) => Left(errorHandler.handleError(error.status)(input))
          case Right(IncomeTaxUserData(None)) => Left(overrideRedirect.getOrElse(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))))
          case Right(IncomeTaxUserData(Some(employmentUserData))) => Right(UserPriorDataRequest(employmentUserData, input.user, input.request))
        }
      }
    }
  }
}