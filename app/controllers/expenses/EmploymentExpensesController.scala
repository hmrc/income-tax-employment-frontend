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
import controllers.expenses.routes.{CheckEmploymentExpensesController, ExpensesInterruptPageController}
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.ExpensesRedirectService.expensesSubmitRedirect
import services.expenses.ExpensesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.expenses.EmploymentExpensesView

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class EmploymentExpensesController @Inject()(authAction: AuthorisedAction,
                                             inYearAction: InYearUtil,
                                             pageView: EmploymentExpensesView,
                                             employmentSessionService: EmploymentSessionService,
                                             expensesService: ExpensesService,
                                             errorHandler: ErrorHandler,
                                             formsProvider: ExpensesFormsProvider)
                                            (implicit cc: MessagesControllerComponents, val appConfig: AppConfig, ec: ExecutionContext, clock: Clock)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val isAgent = request.user.isAgent
      employmentSessionService.getExpensesSessionDataResult(taxYear) {
        case Some(data) =>
          successful(Ok(pageView(formsProvider.claimEmploymentExpensesForm(isAgent).fill(data.expensesCya.expenses.claimingEmploymentExpenses), taxYear)))
        case None => successful(Ok(pageView(formsProvider.claimEmploymentExpensesForm(isAgent), taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getExpensesSessionDataResult(taxYear) { data =>
        formsProvider.claimEmploymentExpensesForm(request.user.isAgent).bindFromRequest().fold(
          formWithErrors => successful(BadRequest(pageView(formWithErrors, taxYear))),
          yesNo => handleSuccessForm(taxYear, data, yesNo)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, data: Option[ExpensesUserData], questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    val expensesUserData = data.getOrElse(ExpensesUserData(request.user.sessionId, request.user.mtditid, request.user.nino, taxYear, isPriorSubmission = false,
      hasPriorExpenses = false, ExpensesCYAModel(ExpensesViewModel(isUsingCustomerData = true)), clock.now())
    )
    expensesService.updateClaimingEmploymentExpenses(request.user, taxYear, expensesUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(expensesUserData) =>
        val nextPage = if (questionValue) {
          ExpensesInterruptPageController.show(taxYear)
        } else {
          CheckEmploymentExpensesController.show(taxYear)
        }
        expensesSubmitRedirect(expensesUserData.expensesCya, nextPage)(taxYear)
    }
  }
}
