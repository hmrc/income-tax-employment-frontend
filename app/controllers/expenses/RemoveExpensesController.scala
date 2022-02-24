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

package controllers.expenses

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.EmploymentSummaryController
import models.IncomeTaxUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.expenses.ExpensesService
import services.{DeleteOrIgnoreExpensesService, EmploymentSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.expenses.{EmploymentExpensesView, RemoveExpensesView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveExpensesController @Inject() (implicit val cc: MessagesControllerComponents,
                                          authAction: AuthorisedAction,
                                          inYearAction: InYearUtil,
                                          removeExpensesView: RemoveExpensesView,
                                          employmentExpensesView: EmploymentExpensesView,
                                          appConfig: AppConfig,
                                          employmentSessionService: EmploymentSessionService,
                                          deleteOrIgnoreExpensesService: DeleteOrIgnoreExpensesService,
                                          expensesService: ExpensesService,
                                          errorHandler: ErrorHandler,
                                          ec: ExecutionContext,
                                          clock: Clock
                                         ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.findPreviousEmploymentUserData(request.user, taxYear) { allEmploymentData =>
        (allEmploymentData.customerExpenses, allEmploymentData.hmrcExpenses) match {
          case (None, None) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          case _ => Ok(removeExpensesView(taxYear))
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
          deleteOrIgnoreExpensesService.deleteOrIgnoreExpenses(request.user, allEmploymentData, taxYear).map {
            case Left(error) => errorHandler.handleError(error.status)
            case Right(_) => Redirect(EmploymentSummaryController.show(taxYear))
          }
      }
    }
  }

}
