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

import config.{AppConfig, ErrorHandler}
import controllers.expenses.routes.OtherEquipmentController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.User
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.ExpensesRedirectService.{expensesSubmitRedirect, professionalSubscriptionsAmountRedirects, redirectBasedOnCurrentAnswers}
import services.expenses.ExpensesService
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
                                                                 expensesService: ExpensesService,
                                                                 errorHandler: ErrorHandler,
                                                                 ec: ExecutionContext,
                                                                 clock: Clock
                                                                ) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandleExpenses(taxYear) { (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { cyaData =>

          val cyaAmount = cyaData.expensesCya.expenses.professionalSubscriptions
          val form = fillExpensesFormFromPriorAndCYA(amountForm(user.isAgent), prior, cyaAmount) { employmentExpenses =>
            employmentExpenses.expenses.flatMap(_.professionalSubscriptions)
          }
          Future(Ok(profSubscriptionsExpensesAmountView(taxYear, form, cyaAmount)))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, cya)(redirects(_, taxYear)) { data =>

          amountForm(user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              val fillValue = data.expensesCya.expenses.professionalSubscriptions
              Future.successful(BadRequest(profSubscriptionsExpensesAmountView(taxYear, formWithErrors, fillValue)))
            },
            amount => handleSuccessForm(taxYear, data, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, expensesUserData: ExpensesUserData, amount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    expensesService.updateProfessionalSubscriptions(taxYear, expensesUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(expensesUserData) => expensesSubmitRedirect(expensesUserData.expensesCya, OtherEquipmentController.show(taxYear))(taxYear)
    }
  }

  private def amountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"expenses.professionalFeesAndSubscriptionsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"expenses.professionalFeesAndSubscriptionsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"expenses.professionalFeesAndSubscriptionsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    professionalSubscriptionsAmountRedirects(cya, taxYear)
  }
}
