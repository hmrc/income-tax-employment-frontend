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
import controllers.expenses.routes._
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.mongo.ExpensesCYAModel
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ExpensesRedirectService.{expensesSubmitRedirect, redirectBasedOnCurrentAnswers}
import services.{EmploymentSessionService, ExpensesRedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.expenses.OtherEquipmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OtherEquipmentController @Inject()(implicit val cc: MessagesControllerComponents,
                                         authAction: AuthorisedAction,
                                         inYearAction: InYearAction,
                                         otherEquipmentView: OtherEquipmentView,
                                         appConfig: AppConfig,
                                         employmentSessionService: EmploymentSessionService,
                                         errorHandler: ErrorHandler,
                                         ec: ExecutionContext,
                                         clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.otherEquipment.error.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    ExpensesRedirectService.otherAllowancesRedirects(cya, taxYear)
  }

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { data =>

          data.expensesCya.expenses.otherAndCapitalAllowancesQuestion match {
            case Some(cya) => Future.successful(Ok(otherEquipmentView(yesNoForm.fill(cya), taxYear)))
            case None => Future.successful(Ok(otherEquipmentView(yesNoForm, taxYear)))
          }
        }
      }
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getExpensesSessionDataResult(taxYear) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { data =>
          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(otherEquipmentView(formWithErrors, taxYear))),
            yesNo => {
              val expensesCYAModel = data.expensesCya
              val expenses = expensesCYAModel.expenses

              val updatedCyaModel: ExpensesCYAModel = {
                if (yesNo) {
                  expensesCYAModel.copy(expenses = expenses.copy(otherAndCapitalAllowancesQuestion = Some(true)))
                } else {
                  expensesCYAModel.copy(expenses = expenses.copy(otherAndCapitalAllowancesQuestion = Some(false), otherAndCapitalAllowances = None))
                }
              }

              employmentSessionService.createOrUpdateExpensesSessionData(
                updatedCyaModel, taxYear, data.isPriorSubmission, data.isPriorSubmission)(errorHandler.internalServerError()) {
                val nextPage = if (yesNo) {
                  OtherEquipmentAmountController.show(taxYear)
                } else {
                  CheckEmploymentExpensesController.show(taxYear)
                }

                expensesSubmitRedirect(updatedCyaModel, nextPage)(taxYear)
              }
            }
          )
        }
      }
    }
  }
}

