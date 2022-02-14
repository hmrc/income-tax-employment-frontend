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
import forms.YesNoForm
import models.AuthorisationRequest
import models.mongo.ExpensesUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.ExpensesRedirectService.{expensesSubmitRedirect, flatRateRedirects, redirectBasedOnCurrentAnswers}
import services.expenses.ExpensesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.expenses.UniformsOrToolsExpensesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UniformsOrToolsExpensesController @Inject()(implicit val cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  inYearAction: InYearUtil,
                                                  pageView: UniformsOrToolsExpensesView,
                                                  appConfig: AppConfig,
                                                  employmentSessionService: EmploymentSessionService,
                                                  expensesService: ExpensesService,
                                                  errorHandler: ErrorHandler,
                                                  ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getExpensesSessionDataResult(taxYear) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(flatRateRedirects(_, taxYear)) { data =>

          data.expensesCya.expenses.flatRateJobExpensesQuestion match {
            case Some(questionResult) => Future.successful(Ok(pageView(yesNoForm(request.user.isAgent).fill(questionResult), taxYear)))
            case None => Future.successful(Ok(pageView(yesNoForm(request.user.isAgent), taxYear)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(flatRateRedirects(_, taxYear)) { data =>

          yesNoForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear))),
            yesNo => handleSuccessForm(taxYear, data, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, expensesUserData: ExpensesUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    expensesService.updateFlatRateJobExpensesQuestion(request.user, taxYear, expensesUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(expensesUserData) =>
        val nextPage = if (questionValue) {
          UniformsOrToolsExpensesAmountController.show(taxYear)
        } else {
          ProfessionalFeesAndSubscriptionsExpensesController.show(taxYear)
        }
        expensesSubmitRedirect(expensesUserData.expensesCya, nextPage)(taxYear)
    }
  }

  private def yesNoForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.uniformsWorkClothesTools.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )
}
