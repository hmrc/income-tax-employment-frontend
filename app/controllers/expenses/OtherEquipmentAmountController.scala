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
import controllers.expenses.routes.CheckEmploymentExpensesController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.mongo.ExpensesCYAModel
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ExpensesRedirectService.redirectBasedOnCurrentAnswers
import services.{EmploymentSessionService, ExpensesRedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.expenses.OtherEquipmentAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class OtherEquipmentAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               inYearAction: InYearAction,
                                               otherEquipmentAmountView: OtherEquipmentAmountView,
                                               appConfig: AppConfig,
                                               val employmentSessionService: EmploymentSessionService,
                                               errorHandler: ErrorHandler,
                                               ec: ExecutionContext,
                                               clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {


  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandleExpenses(taxYear) { (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { cyaData =>

          val cyaAmount = cyaData.expensesCya.expenses.otherAndCapitalAllowances

          val form = fillExpensesFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount) { employmentExpenses =>
            employmentExpenses.expenses.flatMap(_.otherAndCapitalAllowances)
          }
          Future.successful(Ok(otherEquipmentAmountView(taxYear, form, cyaAmount)))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, cya)(redirects(_, taxYear)) { data =>
          buildForm(user.isAgent).bindFromRequest().fold(
            { formWithErrors =>
              val fillValue = data.expensesCya.expenses.otherAndCapitalAllowances
              Future.successful(BadRequest(otherEquipmentAmountView(taxYear, formWithErrors, fillValue)))
            }, {
              newAmount =>

                val expensesCYAModel = data.expensesCya
                val expensesViewModel = expensesCYAModel.expenses
                val updatedCyaModel = expensesCYAModel.copy(expenses = expensesViewModel.copy(otherAndCapitalAllowances = Some(newAmount)))

                employmentSessionService.createOrUpdateExpensesSessionData(updatedCyaModel,
                  taxYear,
                  data.isPriorSubmission,
                  data.isPriorSubmission)(errorHandler.internalServerError()) {

                  val nextPage = CheckEmploymentExpensesController.show(taxYear)
                  ExpensesRedirectService.expensesSubmitRedirect(updatedCyaModel, nextPage)(taxYear)
                }
            }
          )
        }
      }
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"expenses.otherEquipmentAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
      s"expenses.otherEquipmentAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
      s"expenses.otherEquipmentAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}")
  }

  private def redirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    ExpensesRedirectService.otherAllowanceAmountRedirects(cya, taxYear)
  }
}
