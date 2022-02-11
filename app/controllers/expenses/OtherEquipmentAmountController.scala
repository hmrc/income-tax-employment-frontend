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
import controllers.expenses.routes.CheckEmploymentExpensesController
import forms.{AmountForm, FormUtils}
import models.AuthorisationRequest
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ExpensesRedirectService.{otherAllowanceAmountRedirects, redirectBasedOnCurrentAnswers}
import services.expenses.ExpensesService
import services.{EmploymentSessionService, ExpensesRedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.expenses.OtherEquipmentAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class OtherEquipmentAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               inYearAction: InYearUtil,
                                               otherEquipmentAmountView: OtherEquipmentAmountView,
                                               appConfig: AppConfig,
                                               val employmentSessionService: EmploymentSessionService,
                                               expensesService: ExpensesService,
                                               errorHandler: ErrorHandler,
                                               ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandleExpenses(taxYear) { (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { cyaData =>
          val cyaAmount = cyaData.expensesCya.expenses.otherAndCapitalAllowances
          val form = fillExpensesFormFromPriorAndCYA(buildForm(request.user.isAgent), prior, cyaAmount) { employmentExpenses =>
            employmentExpenses.expenses.flatMap(_.otherAndCapitalAllowances)
          }

          Future.successful(Ok(otherEquipmentAmountView(taxYear, form, cyaAmount)))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, cya)(redirects(_, taxYear)) { data =>
          buildForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              val fillValue = data.expensesCya.expenses.otherAndCapitalAllowances
              Future.successful(BadRequest(otherEquipmentAmountView(taxYear, formWithErrors, fillValue)))
            },
            amount => handleSuccessForm(taxYear, data, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, expensesUserData: ExpensesUserData, amount: BigDecimal)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    expensesService.updateOtherAndCapitalAllowances(request.user, taxYear, expensesUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(expensesUserData) =>
        val nextPage = CheckEmploymentExpensesController.show(taxYear)
        ExpensesRedirectService.expensesSubmitRedirect(expensesUserData.expensesCya, nextPage)(taxYear)
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"expenses.otherEquipmentAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"expenses.otherEquipmentAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"expenses.otherEquipmentAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    otherAllowanceAmountRedirects(cya, taxYear)
  }
}
