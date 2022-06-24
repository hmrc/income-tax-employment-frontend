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
import controllers.expenses.routes.UniformsOrToolsExpensesController
import forms.FormUtils
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.ExpensesRedirectService.{expensesSubmitRedirect, jobExpensesAmountRedirects, redirectBasedOnCurrentAnswers}
import services.expenses.ExpensesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.expenses.TravelAndOvernightAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TravelAndOvernightAmountController @Inject()(authAction: AuthorisedAction,
                                                   inYearAction: InYearUtil,
                                                   pageView: TravelAndOvernightAmountView,
                                                   employmentSessionService: EmploymentSessionService,
                                                   expensesService: ExpensesService,
                                                   errorHandler: ErrorHandler,
                                                   formsProvider: ExpensesFormsProvider)
                                                  (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandleExpenses(taxYear)({ (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { data =>
          val cyaAmount = data.expensesCya.expenses.jobExpenses

          val form = fillExpensesFormFromPriorAndCYA(formsProvider.businessTravelAndOvernightAmountForm(request.user.isAgent), prior, cyaAmount)(
            expenses => expenses.expenses.flatMap(_.jobExpenses)
          )
          Future.successful(Ok(pageView(taxYear, form, cyaAmount)))
        }
      })
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getExpensesSessionDataResult(taxYear) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, cya)(redirects(_, taxYear)) { data =>
          formsProvider.businessTravelAndOvernightAmountForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(pageView(taxYear, formWithErrors, data.expensesCya.expenses.jobExpenses))),
            amount => handleSuccessForm(taxYear, data, amount)
          )
        }

      }
    }
  }

  private def handleSuccessForm(taxYear: Int, expensesUserData: ExpensesUserData, amount: BigDecimal)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    expensesService.updateJobExpenses(request.user, taxYear, expensesUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(expensesUserData) => expensesSubmitRedirect(expensesUserData.expensesCya, UniformsOrToolsExpensesController.show(taxYear))(taxYear)
    }
  }

  private def redirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    jobExpensesAmountRedirects(cya, taxYear)
  }
}
