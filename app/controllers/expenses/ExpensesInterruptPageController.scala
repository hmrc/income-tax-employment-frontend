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
import models.mongo.ExpensesCYAModel
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.ExpensesRedirectService.{jobExpensesRedirects, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.expenses.ExpensesInterruptPageView
import javax.inject.Inject

import scala.concurrent.Future

class ExpensesInterruptPageController @Inject()(implicit val cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                inYearAction: InYearUtil,
                                                employmentSessionService: EmploymentSessionService,
                                                expensesInterruptPageView: ExpensesInterruptPageView,
                                                appConfig: AppConfig,
                                                errorHandler: ErrorHandler) extends FrontendController(cc) with I18nSupport with SessionHelper {

  implicit val ec = cc.executionContext

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    if(!inYearAction.inYear(taxYear)) {
      Future.successful(Ok(expensesInterruptPageView(taxYear)))
    } else {
      Future.successful(Redirect(controllers.expenses.routes.CheckEmploymentExpensesController.show(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    if(!inYearAction.inYear(taxYear)) {
      employmentSessionService.getPriorData(request.user,taxYear).map {
        case Left(error) => errorHandler.handleError(error.status)
        case Right(data) =>
          if(data.employment.flatMap(_.latestEOYExpenses).nonEmpty){
            Redirect(controllers.expenses.routes.CheckEmploymentExpensesController.show(taxYear))
          } else {
            Redirect(controllers.expenses.routes.BusinessTravelOvernightExpensesController.show(taxYear))
          }
      }
    } else {
      Future.successful(Redirect(controllers.expenses.routes.CheckEmploymentExpensesController.show(taxYear)))
    }
  }
}
