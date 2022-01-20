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

import common.SessionValues
import config.{AppConfig, ErrorHandler}
import controllers.expenses.routes.{CheckEmploymentExpensesController, EmploymentExpensesController}
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.User
import models.employment.AllEmploymentData.employmentIdExists
import models.employment.{EmploymentExpenses, LatestExpensesOrigin}
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
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear)(allEmploymentData =>
        allEmploymentData.latestInYearExpenses match {
          case Some(LatestExpensesOrigin(EmploymentExpenses(_, _, _, Some(expenses)), isUsingCustomerData)) => performAuditAndRenderView(expenses,
            taxYear, isInYear = true, isUsingCustomerData = isUsingCustomerData)
          case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        })
    } else {
      employmentSessionService.getAndHandleExpenses(taxYear) { (cya, prior) =>
        cya match {
          case Some(cya: ExpensesUserData) =>
            Future(performAuditAndRenderView(
              cya.expensesCya.expenses.toExpenses,
              taxYear,
              isInYear = false,
              isUsingCustomerData = cya.expensesCya.expenses.isUsingCustomerData,
              Some(cya.expensesCya.expenses)))
          case None =>
            prior match {
              case Some(allEmploymentData) =>
                allEmploymentData.latestEOYExpenses match {
                  case Some(LatestExpensesOrigin(EmploymentExpenses(submittedOn, _, _, Some(expenses)), isUsingCustomerData)) =>
                    employmentSessionService.createOrUpdateExpensesSessionData(ExpensesCYAModel.makeModel(expenses, isUsingCustomerData, submittedOn),
                      taxYear, isPriorSubmission = true, hasPriorExpenses = true)(errorHandler.internalServerError()) {
                      if (employmentIdExists(allEmploymentData, getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID))) {
                        //TODO: add a redirect for "do you need to add any additional/new expenses?" page when available
                        Redirect(EmploymentExpensesController.show(taxYear))
                      } else {
                        performAuditAndRenderView(
                          expenses,
                          taxYear,
                          isInYear = false,
                          isUsingCustomerData = isUsingCustomerData)
                      }
                    }
                  case None =>
                    if (employmentIdExists(allEmploymentData, getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID))) {
                      Future.successful(Redirect(EmploymentExpensesController.show(taxYear)))
                    } else {
                      Future.successful(performAuditAndRenderView(
                        Expenses(),
                        taxYear,
                        isInYear = false,
                        isUsingCustomerData = true))
                    }
                }
              //TODO redirect to receive any expenses page
              case None => Future(performAuditAndRenderView(Expenses(), taxYear, isInYear = false, isUsingCustomerData = true))
            }
        }
      }
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

                  if (appConfig.nrsEnabled) checkEmploymentExpensesService.performSubmitNrsPayload(model, prior)

                  employmentSessionService.clearExpenses(taxYear)(result.removingFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID))
              }
            }
          }
          case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
        }
      }
    }
  }

  private def performAuditAndRenderView(expenses: Expenses,
                                        taxYear: Int,
                                        isInYear: Boolean,
                                        isUsingCustomerData: Boolean,
                                        cya: Option[ExpensesViewModel] = None)(implicit user: User[_]): Result = {
    checkEmploymentExpensesService.sendViewEmploymentExpensesAudit(taxYear, expenses)
    if (isInYear) {
      Ok(checkEmploymentExpensesView(taxYear, expenses.toExpensesViewModel(isUsingCustomerData, cyaExpenses = cya), isInYear))
    } else {
      Ok(checkEmploymentExpensesViewEOY(taxYear, expenses.toExpensesViewModel(isUsingCustomerData, cyaExpenses = cya), isInYear))
    }
  }
}



