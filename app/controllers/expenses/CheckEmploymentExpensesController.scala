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
import actions.AuthorisedTaxYearAction.authorisedTaxYearAction
import common.SessionValues
import config.{AppConfig, ErrorHandler}
import controllers.expenses.routes.{CheckEmploymentExpensesController, EmploymentExpensesController}
import models.AuthorisationRequest
import models.employment.AllEmploymentData.employmentIdExists
import models.employment.{EmploymentExpenses, LatestExpensesOrigin}
import models.expenses.{Expenses, ExpensesViewModel}
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.expenses.CheckEmploymentExpensesService
import services.{CreateOrAmendExpensesService, EmploymentSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.expenses.CheckEmploymentExpensesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentExpensesController @Inject()(checkEmploymentExpensesView: CheckEmploymentExpensesView,
                                                  createOrAmendExpensesService: CreateOrAmendExpensesService,
                                                  employmentSessionService: EmploymentSessionService,
                                                  checkEmploymentExpensesService: CheckEmploymentExpensesService,
                                                  inYearAction: InYearUtil,
                                                  errorHandler: ErrorHandler
                                                 )(implicit appConfig: AppConfig,
                                                   authorisedAction: AuthorisedAction,
                                                   mcc: MessagesControllerComponents,
                                                   ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  //scalastyle:off
  def show(taxYear: Int): Action[AnyContent] = authorisedTaxYearAction(taxYear).async { implicit request =>
    if (inYearAction.inYear(taxYear)) {
      employmentSessionService.findPreviousEmploymentUserData(request.user, taxYear)(allEmploymentData =>
        allEmploymentData.latestInYearExpenses match {
          case Some(LatestExpensesOrigin(EmploymentExpenses(_, _, _, Some(expenses)), isUsingCustomerData)) => performAuditAndRenderView(expenses,
            taxYear, isInYear = true, isUsingCustomerData = isUsingCustomerData)
          case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        })
    } else if (!appConfig.employmentEOYEnabled) {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      employmentSessionService.getAndHandleExpenses(taxYear)({ (cya, prior) =>
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
                      taxYear, isPriorSubmission = true, hasPriorExpenses = true, request.user)(errorHandler.internalServerError()) {
                      if (employmentIdExists(allEmploymentData, getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID))) {
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
              case None => Future(performAuditAndRenderView(Expenses(), taxYear, isInYear = false, isUsingCustomerData = true))
            }
        }
      })
    }
  }
  //scalastyle:on

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    if (!inYearAction.inYear(taxYear) && appConfig.employmentEOYEnabled) {
      employmentSessionService.getAndHandleExpenses(taxYear)({ (cya, prior) =>
        cya match {
          case Some(cya) => cya.expensesCya.expenses.expensesIsFinished(taxYear) match {
            // TODO: potentially navigate elsewhere - design to confirm
            case Some(unfinishedRedirect) => Future.successful(Redirect(unfinishedRedirect))
            case _ => createOrAmendExpensesService.createExpensesModelAndReturnResult(request.user, cya, prior, taxYear) { model =>
              createOrAmendExpensesService.createOrUpdateExpensesResult(taxYear, model).flatMap {
                case Left(result) => Future.successful(result)
                case Right(result) =>
                  checkEmploymentExpensesService.performSubmitAudits(request.user, model, taxYear, prior)

                  if (appConfig.nrsEnabled) checkEmploymentExpensesService.performSubmitNrsPayload(model, prior, request.user)

                  employmentSessionService.clearExpenses(taxYear)(result.removingFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID))
              }
            }
          }
          case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
        }
      })
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  private def performAuditAndRenderView(expenses: Expenses,
                                        taxYear: Int,
                                        isInYear: Boolean,
                                        isUsingCustomerData: Boolean,
                                        cya: Option[ExpensesViewModel] = None)(implicit request: AuthorisationRequest[_]): Result = {
    checkEmploymentExpensesService.sendViewEmploymentExpensesAudit(request.user, taxYear, expenses)
    Ok(checkEmploymentExpensesView(taxYear, expenses.toExpensesViewModel(isUsingCustomerData, cyaExpenses = cya), isInYear))
  }
}



