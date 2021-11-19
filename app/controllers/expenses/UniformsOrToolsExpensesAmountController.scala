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
import controllers.expenses.routes.ProfessionalFeesAndSubscriptionsExpensesController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.mongo.ExpensesUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.expenses.UniformsOrToolsExpensesAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class UniformsOrToolsExpensesAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                        authAction: AuthorisedAction,
                                                        inYearAction: InYearAction,
                                                        uniformsOrToolsExpensesAmountView: UniformsOrToolsExpensesAmountView,
                                                        appConfig: AppConfig,
                                                        val employmentSessionService: EmploymentSessionService,
                                                        errorHandler: ErrorHandler,
                                                        ec: ExecutionContext,
                                                        clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {


  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandleExpenses(taxYear) { (optCya, prior) =>

        optCya match {
          case Some(cyaData) =>

            // TODO: move navigation logic to redirect service
            if(cyaData.expensesCya.expenses.flatRateJobExpensesQuestion.contains(false)) {
              Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
            } else {

              val cyaAmount = cyaData.expensesCya.expenses.flatRateJobExpenses

              val form = fillExpensesFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount) { employmentExpenses =>
                employmentExpenses.expenses.flatMap(_.flatRateJobExpenses)
              }

              Future(Ok(uniformsOrToolsExpensesAmountView(taxYear, form, cyaAmount)))
            }

          case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
        }
      }

    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) {
        case Some(expensesUserData: ExpensesUserData) => buildForm(user.isAgent).bindFromRequest().fold(
          { formWithErrors =>
            val fillValue = expensesUserData.expensesCya.expenses.flatRateJobExpenses
            Future.successful(BadRequest(uniformsOrToolsExpensesAmountView(taxYear, formWithErrors, fillValue)))
          }, {
            newAmount =>

              val expensesCYAModel = expensesUserData.expensesCya
              val expensesViewModel = expensesCYAModel.expenses
              val updatedCyaModel = expensesCYAModel.copy(expenses = expensesViewModel.copy(flatRateJobExpenses = Some(newAmount)))

              employmentSessionService.createOrUpdateExpensesSessionData(updatedCyaModel,
                taxYear,
                expensesUserData.isPriorSubmission,
                expensesUserData.isPriorSubmission)(errorHandler.internalServerError()) {

                if (expensesUserData.isPriorSubmission) {
                  Redirect(CheckEmploymentExpensesController.show(taxYear))
                } else {
                  Redirect(ProfessionalFeesAndSubscriptionsExpensesController.show(taxYear))
                }

              }
          }
        )
        case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
      }
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"expenses.uniformsWorkClothesToolsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
      s"expenses.uniformsWorkClothesToolsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
      s"expenses.uniformsWorkClothesToolsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}")
  }
}
