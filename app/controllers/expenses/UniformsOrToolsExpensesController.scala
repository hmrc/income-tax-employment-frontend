/*
 * Copyright 2021 HM Revenue & Customs
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

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckEmploymentExpensesController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.mongo.ExpensesUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.expenses.UniformsOrToolsExpensesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UniformsOrToolsExpensesController @Inject()(implicit val cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  inYearAction: InYearAction,
                                                  pageView: UniformsOrToolsExpensesView,
                                                  appConfig: AppConfig,
                                                  employmentSessionService: EmploymentSessionService,
                                                  errorHandler: ErrorHandler,
                                                  ec: ExecutionContext,
                                                  clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  private def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.uniformsWorkClothesTools.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) {
        case Some(data) => data.expensesCya.expenses.flatRateJobExpensesQuestion match {
          case Some(questionResult) => Future.successful(Ok(pageView(yesNoForm.fill(questionResult), taxYear)))
          case None => Future.successful(Ok(pageView(yesNoForm, taxYear)))
        }
        case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) {
        case Some(expensesUserData: ExpensesUserData) => yesNoForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear))),
          yesNo => {
            val expensesCYAModel = expensesUserData.expensesCya
            val expensesViewModel = expensesCYAModel.expenses

            val updatedCyaModel = if (yesNo) {
              expensesCYAModel.copy(expenses = expensesViewModel.copy(flatRateJobExpensesQuestion = Some(true)))
            } else {
              expensesCYAModel.copy(expenses = expensesViewModel.copy(flatRateJobExpensesQuestion = Some(false), flatRateJobExpenses = None))
            }

            employmentSessionService.createOrUpdateExpensesSessionData(updatedCyaModel,
              taxYear,
              expensesUserData.isPriorSubmission,
              expensesUserData.isPriorSubmission)(errorHandler.internalServerError()) {
              Redirect(CheckEmploymentExpensesController.show(taxYear))
            }
          }
        )
        case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
      }
    }
  }
}
