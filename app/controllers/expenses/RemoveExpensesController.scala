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

package controllers.expenses

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.EmploymentSummaryController
import models.IncomeTaxUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DeleteOrIgnoreExpensesService, EmploymentSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.expenses.RemoveExpensesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveExpensesController @Inject()(authAction: AuthorisedAction,
                                         inYearAction: InYearUtil,
                                         pageView: RemoveExpensesView,
                                         employmentSessionService: EmploymentSessionService,
                                         deleteOrIgnoreExpensesService: DeleteOrIgnoreExpensesService,
                                         errorHandler: ErrorHandler)
                                        (implicit cc: MessagesControllerComponents, ec: ExecutionContext, val appConfig: AppConfig)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.findPreviousEmploymentUserData(request.user, taxYear) { allEmploymentData =>
        (allEmploymentData.customerExpenses, allEmploymentData.notIgnoredHmrcExpenses) match {
          case (None, None) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          case _ => Ok(pageView(taxYear))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getPriorData(request.user, taxYear).flatMap {
        case Left(error) => Future.successful(errorHandler.handleError(error.status))
        case Right(IncomeTaxUserData(None)) => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case Right(IncomeTaxUserData(Some(allEmploymentData))) =>
          (allEmploymentData.customerExpenses, allEmploymentData.notIgnoredHmrcExpenses) match {
            case (None, None) => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
            case _ => deleteOrIgnoreExpensesService.deleteOrIgnoreExpenses(request.user, allEmploymentData, taxYear).map {
              case Left(error) => errorHandler.handleError(error.status)
              case Right(_) => Redirect(EmploymentSummaryController.show(taxYear))
            }
          }
      }
    }
  }
}
