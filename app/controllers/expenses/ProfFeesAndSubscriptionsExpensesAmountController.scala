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
import models.mongo.ExpensesUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.expenses.ProfFeesAndSubscriptionsExpensesAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProfFeesAndSubscriptionsExpensesAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                                 authAction: AuthorisedAction,
                                                                 inYearAction: InYearAction,
                                                                 profSubscriptionsExpensesAmountView: ProfFeesAndSubscriptionsExpensesAmountView,
                                                                 appConfig: AppConfig,
                                                                 val employmentSessionService: EmploymentSessionService,
                                                                 errorHandler: ErrorHandler,
                                                                 ec: ExecutionContext,
                                                                 clock: Clock) extends FrontendController(cc)
  with I18nSupport with SessionHelper with FormUtils {

  def amountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"expenses.professionalFeesAndSubscriptionsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"expenses.professionalFeesAndSubscriptionsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"expenses.professionalFeesAndSubscriptionsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandleExpenses(taxYear) { (optCya, prior) =>
        optCya match {
          case Some(cyaData) =>
            // TODO: move navigation logic to redirect service
            if (cyaData.expensesCya.expenses.professionalSubscriptionsQuestion.contains(false)) {
              Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
            } else {
              val cyaAmount = cyaData.expensesCya.expenses.professionalSubscriptions
              val form = fillExpensesFormFromPriorAndCYA(amountForm(user.isAgent), prior, cyaAmount) { employmentExpenses =>
                employmentExpenses.expenses.flatMap(_.professionalSubscriptions)
              }
              Future(Ok(profSubscriptionsExpensesAmountView(taxYear, form, cyaAmount)))
            }
          case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
        }
      }

    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) {

        case Some(expensesUserData: ExpensesUserData) => amountForm(user.isAgent).bindFromRequest().fold(
          { formWithErrors =>
            val fillValue = expensesUserData.expensesCya.expenses.professionalSubscriptions
            Future.successful(BadRequest(profSubscriptionsExpensesAmountView(taxYear, formWithErrors, fillValue)))
          }, {
            newAmount =>
              val cyaModel = expensesUserData.expensesCya
              val expenses = cyaModel.expenses

              val updatedCyaModel = cyaModel.copy(expenses = expenses.copy(professionalSubscriptions = Some(newAmount))
              )

              employmentSessionService.createOrUpdateExpensesSessionData(updatedCyaModel, taxYear,
                isPriorSubmission = expensesUserData.isPriorSubmission,
                hasPriorExpenses = expensesUserData.hasPriorExpenses)(errorHandler.internalServerError()) {

                if (expensesUserData.isPriorSubmission) {
                  Redirect(CheckEmploymentExpensesController.show(taxYear))
                } else {
                  //TODO - redirect to otherAndCapitalAllowances (other equipment) Question Page
                  Redirect(CheckEmploymentExpensesController.show(taxYear))
                }
              }
          }
        )
        case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
      }
    }
  }
}
