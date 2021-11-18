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
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import javax.inject.Inject
import models.User
import models.mongo.ExpensesCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.expenses.TravelAndOvernightAmountView

import scala.concurrent.{ExecutionContext, Future}

class TravelAndOvernightAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                   authAction: AuthorisedAction,
                                                   inYearAction: InYearAction,
                                                   travelAndOvernightAmountView: TravelAndOvernightAmountView,
                                                   appConfig: AppConfig,
                                                   val employmentSessionService: EmploymentSessionService,
                                                   errorHandler: ErrorHandler,
                                                   ec: ExecutionContext,
                                                   clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def amountForm(implicit user: User[_]): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"expenses.businessTravelAndOvernightAmount.error.noEntry.${if (user.isAgent) "agent" else "individual"}",
    wrongFormatKey = s"expenses.businessTravelAndOvernightAmount.error.incorrectFormat.${if (user.isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"expenses.businessTravelAndOvernightAmount.error.overMaximum.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandleExpenses(taxYear) { (optCya, prior) =>

        optCya.map(_.expensesCya.expenses) match {
          case Some(cya) =>

            // TODO: move navigation logic to redirect service
            if(cya.jobExpensesQuestion.contains(true)) {
              val form = fillExpensesFormFromPriorAndCYA(amountForm, prior, cya.jobExpenses)(expenses => expenses.expenses.flatMap(_.jobExpenses))
              Future.successful(Ok(
                travelAndOvernightAmountView(taxYear, form, cya.jobExpenses)
              ))
            } else {
              Future.successful(Redirect(controllers.employment.routes.CheckEmploymentExpensesController.show(taxYear)))
            }

          case None =>
            Future.successful(Redirect(controllers.employment.routes.CheckEmploymentExpensesController.show(taxYear)))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getExpensesSessionDataResult(taxYear) {
        case Some(data) =>
          amountForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(travelAndOvernightAmountView(taxYear, formWithErrors, None))),
            amount => {
              val expensesUserData = data.expensesCya
              val cya = expensesUserData.expenses

              val updatedCyaModel: ExpensesCYAModel =
                expensesUserData.copy(expenses = cya.copy(jobExpenses = Some(amount)))

              employmentSessionService.createOrUpdateExpensesSessionData(
                updatedCyaModel, taxYear, data.isPriorSubmission, data.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(controllers.employment.routes.CheckEmploymentExpensesController.show(taxYear))
              }
            }
          )
        case None =>
          Future.successful(Redirect(controllers.employment.routes.CheckEmploymentExpensesController.show(taxYear)))
      }
    }
  }

}
