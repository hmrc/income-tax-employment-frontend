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
import models.User
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.ExpensesRedirectService.{expensesSubmitRedirect, professionalSubscriptionsRedirects, redirectBasedOnCurrentAnswers}
import services.expenses.ExpensesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.expenses.ProfessionalFeesAndSubscriptionsExpensesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProfessionalFeesAndSubscriptionsExpensesController @Inject()(implicit val cc: MessagesControllerComponents,
                                                                   authAction: AuthorisedAction,
                                                                   inYearAction: InYearUtil,
                                                                   professionalFeesAndSubscriptionsExpensesView: ProfessionalFeesAndSubscriptionsExpensesView,
                                                                   appConfig: AppConfig,
                                                                   employmentSessionService: EmploymentSessionService,
                                                                   expensesService: ExpensesService,
                                                                   errorHandler: ErrorHandler,
                                                                   ec: ExecutionContext,
                                                                   clock: Clock
                                                                  ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { data =>

          data.expensesCya.expenses.professionalSubscriptionsQuestion match {
            case Some(cya) => Future.successful(Ok(professionalFeesAndSubscriptionsExpensesView(yesNoForm.fill(cya), taxYear)))
            case None => Future.successful(Ok(professionalFeesAndSubscriptionsExpensesView(yesNoForm, taxYear)))
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
            formWithErrors => Future.successful(BadRequest(professionalFeesAndSubscriptionsExpensesView(formWithErrors, taxYear))),
            yesNo => handleSuccessForm(taxYear, data, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, expensesUserData: ExpensesUserData, questionValue: Boolean)
                               (implicit user: User[_]): Future[Result] = {
    expensesService.updateProfessionalSubscriptionsQuestion(taxYear, expensesUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(expensesUserData) =>
        val nextPage = if (questionValue) {
          ProfFeesAndSubscriptionsExpensesAmountController.show(taxYear)
        } else {
          OtherEquipmentController.show(taxYear)
        }
        expensesSubmitRedirect(expensesUserData.expensesCya, nextPage)(taxYear)
    }
  }

  private def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.professionalFeesAndSubscriptions.error.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    professionalSubscriptionsRedirects(cya, taxYear)
  }
}

