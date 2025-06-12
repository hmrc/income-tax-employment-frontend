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

package controllers.tailorings

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.EmploymentSummaryController
import models.IncomeTaxUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.tailoring.TailoringService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.tailorings.RemoveAllEmploymentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveAllEmploymentController @Inject()(cc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              inYearAction: InYearUtil,
                                              pageView: RemoveAllEmploymentView,
                                              employmentSessionService: EmploymentSessionService,
                                              tailoringService: TailoringService,
                                              errorHandler: ErrorHandler)
                                             (implicit val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      Future.successful(Ok(pageView(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getPriorData(request.user, taxYear).flatMap {
        case Left(error) => Future.successful(errorHandler.handleError(error.status))
        case Right(IncomeTaxUserData(None)) =>
          tailoringService.postExcludedJourney(taxYear, request.user.nino, request.user.mtditid).map {
            case Left(error) => errorHandler.handleError(error.status)
            case Right(_) => Redirect(EmploymentSummaryController.show(taxYear))
          }
        case Right(IncomeTaxUserData(Some(allEmploymentData))) =>
          tailoringService.deleteOrIgnoreAllEmployment(allEmploymentData, taxYear, request.user).flatMap {
            case Left(error) => Future.successful(errorHandler.handleError(error.status))
            case Right(_) =>
              tailoringService.postExcludedJourney(taxYear, request.user.nino, request.user.mtditid).map {
                case Left(error) => errorHandler.handleError(error.status)
                case Right(_) => Redirect(EmploymentSummaryController.show(taxYear))
              }
          }
      }
    }
  }
}
