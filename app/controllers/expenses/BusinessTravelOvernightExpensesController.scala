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
import controllers.expenses.routes._
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.ExpensesRedirectService._
import services.expenses.ExpensesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.expenses.BusinessTravelOvernightExpensesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessTravelOvernightExpensesController @Inject()(authAction: AuthorisedAction,
                                                          inYearAction: InYearUtil,
                                                          pageView: BusinessTravelOvernightExpensesView,
                                                          employmentSessionService: EmploymentSessionService,
                                                          expensesService: ExpensesService,
                                                          errorHandler: ErrorHandler,
                                                          formsProvider: ExpensesFormsProvider)
                                                         (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { data =>

          data.expensesCya.expenses.jobExpensesQuestion match {
            case Some(questionResult) =>
              Future.successful(Ok(pageView(formsProvider.businessTravelExpensesForm(request.user.isAgent).fill(questionResult), taxYear)))
            case None => Future.successful(Ok(pageView(formsProvider.businessTravelExpensesForm(request.user.isAgent), taxYear)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { data =>
          formsProvider.businessTravelExpensesForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear))),
            yesNo => handleSuccessForm(taxYear, data, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, expensesUserData: ExpensesUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    expensesService.updateJobExpensesQuestion(request.user, taxYear, expensesUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(expensesUserData) =>
        val nextPage = if (questionValue) {
          TravelAndOvernightAmountController.show(taxYear)
        } else {
          UniformsOrToolsExpensesController.show(taxYear)
        }
        expensesSubmitRedirect(expensesUserData.expensesCya, nextPage)(taxYear)
    }
  }

  private def redirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    jobExpensesRedirects(cya, taxYear)
  }
}
