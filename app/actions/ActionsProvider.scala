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
import models._
import models.employment.EmploymentType
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.EmploymentSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{InYearUtil, RedirectsMapper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ActionsProvider @Inject()(authAction: AuthorisedAction,
                                employmentSessionService: EmploymentSessionService,
                                errorHandler: ErrorHandler,
                                inYearUtil: InYearUtil,
                                redirectsMapper: RedirectsMapper,
                                appConfig: AppConfig
                               )(implicit ec: ExecutionContext) {

  def endOfYearWithSessionData(taxYear: Int,
                               employmentId: String,
                               employmentType: EmploymentType,
                               clazz: Class[_]): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(UserSessionDataRequestRefinerAction(taxYear, employmentId, employmentType, employmentSessionService, errorHandler, appConfig))
      .andThen(RedirectsFilterAction(redirectsMapper, clazz, taxYear, employmentId))

  // TODO: Refactor
  def notInYearWithPriorData(taxYear: Int, overrideRedirect: Option[Result] = None): ActionBuilder[UserPriorDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(employmentPriorDataAction(taxYear, overrideRedirect))

  // TODO: Refactor
  def authenticatedPriorDataAction(taxYear: Int): ActionBuilder[OptionalUserPriorDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(optionalEmploymentPriorDataAction(taxYear))

  // TODO: Refactor
  private def employmentPriorDataAction(taxYear: Int, overrideRedirect: Option[Result])
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

  // TODO: Refactor
  private def optionalEmploymentPriorDataAction(taxYear: Int)
                                               (implicit ec: ExecutionContext): ActionRefiner[AuthorisationRequest, OptionalUserPriorDataRequest] = {
    new ActionRefiner[AuthorisationRequest, OptionalUserPriorDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, OptionalUserPriorDataRequest[A]]] = {

        implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(input.request, input.request.session)

        employmentSessionService.getPriorData(input.user, taxYear).map {
          case Left(error) => Left(errorHandler.handleError(error.status)(input))
          case Right(IncomeTaxUserData(data)) => Right(OptionalUserPriorDataRequest(data, input.user, input.request))
        }
      }
    }
  }
}
