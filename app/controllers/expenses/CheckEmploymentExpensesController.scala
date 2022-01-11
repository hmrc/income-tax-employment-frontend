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
import controllers.expenses.routes.CheckEmploymentExpensesController
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.User
import models.employment.{AllEmploymentData, EmploymentExpenses}
import models.expenses.{Expenses, ExpensesViewModel}
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.expenses.CheckEmploymentExpensesService
import services.{CreateOrAmendExpensesService, EmploymentSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.expenses.{CheckEmploymentExpensesView, CheckEmploymentExpensesViewEOY}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentExpensesController @Inject()(authorisedAction: AuthorisedAction,
                                                  checkEmploymentExpensesView: CheckEmploymentExpensesView,
                                                  checkEmploymentExpensesViewEOY: CheckEmploymentExpensesViewEOY,
                                                  createOrAmendExpensesService: CreateOrAmendExpensesService,
                                                  employmentSessionService: EmploymentSessionService,
                                                  checkEmploymentExpensesService: CheckEmploymentExpensesService,
                                                  inYearAction: InYearAction,
                                                  errorHandler: ErrorHandler,
                                                  implicit val clock: Clock,
                                                  implicit val appConfig: AppConfig,
                                                  implicit val mcc: MessagesControllerComponents,
                                                  implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    if (inYearAction.inYear(taxYear)) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear)(inYearResult(taxYear, _))
    } else {
      employmentSessionService.getAndHandleExpenses(taxYear) { (cya, prior) =>
        cya match {
          case Some(cya: ExpensesUserData) =>
            val expensesViewModel: ExpensesViewModel = cya.expensesCya.expenses
            val isUsingCustomerData = expensesViewModel.isUsingCustomerData

            Future(performAuditAndRenderView(
              expensesViewModel.toExpenses,
              taxYear,
              inYearAction.inYear(taxYear),
              prior.exists(isMultipleEmployments(_, inYearAction.inYear(taxYear))),
              isUsingCustomerData,
              Some(expensesViewModel)))
          case None =>
            prior match {
              case Some(allEmploymentData) =>
                employmentSessionService.getLatestExpenses(allEmploymentData, inYearAction.inYear(taxYear)) match {
                  case Some((EmploymentExpenses(submittedOn, _, _, Some(expenses)), isUsingCustomerData)) =>
                    employmentSessionService.createOrUpdateExpensesSessionData(ExpensesCYAModel.makeModel(expenses, isUsingCustomerData, submittedOn),
                      taxYear, isPriorSubmission = true, hasPriorExpenses = false
                    )(errorHandler.internalServerError()) {
                      performAuditAndRenderView(
                        expenses,
                        taxYear,
                        inYearAction.inYear(taxYear),
                        isMultipleEmployments(allEmploymentData, inYearAction.inYear(taxYear)),
                        isUsingCustomerData)
                    }
                  //TODO redirect to receive any expenses page
                  case None => Future(performAuditAndRenderView(
                    Expenses(),
                    taxYear,
                    inYearAction.inYear(taxYear),
                    isMultipleEmployments(allEmploymentData, inYearAction.inYear(taxYear)),
                    isUsingCustomerData = true))
                }
              //TODO redirect to receive any expenses page
              case None => Future(performAuditAndRenderView(
                Expenses(),
                taxYear,
                inYearAction.inYear(taxYear),
                isMultipleEmployments = false,
                isUsingCustomerData = true))
            }
        }
      }
    }
  }

  private def performAuditAndRenderView(expenses: Expenses, taxYear: Int, isInYear: Boolean, isMultipleEmployments: Boolean,
                                        isUsingCustomerData: Boolean, cya: Option[ExpensesViewModel] = None)
                                       (implicit user: User[_]): Result = {
    checkEmploymentExpensesService.sendViewEmploymentExpensesAudit(taxYear, expenses)
    if (isInYear) {
      Ok(checkEmploymentExpensesView(taxYear, expenses.toExpensesViewModel(isUsingCustomerData, cyaExpenses = cya), isInYear, isMultipleEmployments))
    } else {
      Ok(checkEmploymentExpensesViewEOY(taxYear, expenses.toExpensesViewModel(isUsingCustomerData, cyaExpenses = cya), isInYear, isMultipleEmployments))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandleExpenses(taxYear) { (cya, prior) =>
        cya match {
          case Some(cya) => cya.expensesCya.expenses.expensesIsFinished(taxYear) match {
            // TODO: potentially navigate elsewhere - design to confirm
            case Some(unfinishedRedirect) => Future.successful(Redirect(unfinishedRedirect))
            case _ => createOrAmendExpensesService.createExpensesModelAndReturnResult(cya, prior, taxYear) { model =>
              createOrAmendExpensesService.createOrUpdateExpensesResult(taxYear, model).flatMap {
                case Left(result) => Future.successful(result)
                case Right(result) =>
                  checkEmploymentExpensesService.performSubmitAudits(model, taxYear, prior)

                  if(appConfig.nrsEnabled){
                    checkEmploymentExpensesService.performSubmitNrsPayload(model, prior)
                  }

                  employmentSessionService.clearExpenses(taxYear)(result)
              }
            }
          }
          case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
        }
      }
    }
  }

  private def inYearResult(taxYear: Int, allEmploymentData: AllEmploymentData)(implicit user: User[_]): Result = {
    employmentSessionService.getLatestExpenses(allEmploymentData, isInYear = true) match {
      case Some((EmploymentExpenses(_, _, _, Some(expenses)), isUsingCustomerData)) => performAuditAndRenderView(
        expenses,
        taxYear,
        inYearAction.inYear(taxYear),
        isMultipleEmployments(allEmploymentData, inYearAction.inYear(taxYear)),
        isUsingCustomerData)
      case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }

  private def isMultipleEmployments(allEmploymentData: AllEmploymentData, inYear: Boolean): Boolean = {
    employmentSessionService.getLatestEmploymentData(allEmploymentData, inYear).length > 1
  }
}



