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
import controllers.expenses.routes._
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.mongo.ExpensesCYAModel
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ExpensesRedirectService._
import services.{EmploymentSessionService, ExpensesRedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.expenses.BusinessTravelOvernightExpensesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessTravelOvernightExpensesController @Inject()(implicit val cc: MessagesControllerComponents,
                                                          authAction: AuthorisedAction,
                                                          inYearAction: InYearAction,
                                                          businessTravelOvernightExpensesView: BusinessTravelOvernightExpensesView,
                                                          appConfig: AppConfig,
                                                          employmentSessionService: EmploymentSessionService,
                                                          errorHandler: ErrorHandler,
                                                          ec: ExecutionContext,
                                                          clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {


  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"expenses.businessTravelOvernightExpenses.error.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    ExpensesRedirectService.jobExpensesRedirects(cya, taxYear)
  }

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getExpensesSessionDataResult(taxYear) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, optCya)(redirects(_, taxYear)) { data =>

          data.expensesCya.expenses.jobExpensesQuestion match {
            case Some(questionResult) => Future.successful(Ok(businessTravelOvernightExpensesView(yesNoForm.fill(questionResult), taxYear)))
            case None => Future.successful(Ok(businessTravelOvernightExpensesView(yesNoForm, taxYear)))
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
            formWithErrors => Future.successful(BadRequest(businessTravelOvernightExpensesView(formWithErrors, taxYear))),
            yesNo => {
              val cyaModel = data.expensesCya
              val expenses = data.expensesCya.expenses

              val updatedCyaModel: ExpensesCYAModel = {
                if (yesNo) {
                  cyaModel.copy(expenses = expenses.copy(jobExpensesQuestion = Some(true)))
                } else {
                  cyaModel.copy(expenses = expenses.copy(jobExpensesQuestion = Some(false), jobExpenses = None))
                }
              }

              employmentSessionService.createOrUpdateExpensesSessionData(
                updatedCyaModel, taxYear, data.isPriorSubmission, data.isPriorSubmission)(errorHandler.internalServerError()) {

                val nextPage = {
                  if (yesNo) {
                    TravelAndOvernightAmountController.show(taxYear)
                  } else {
                    UniformsOrToolsExpensesController.show(taxYear)
                  }
                }
                ExpensesRedirectService.expensesSubmitRedirect(updatedCyaModel, nextPage)(taxYear)
              }
            }
          )
        }
      }
    }
  }
}
