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

import audit._
import config.{AppConfig, ErrorHandler}
import controllers.expenses.routes.CheckEmploymentExpensesController
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.User
import models.employment.{AllEmploymentData, EmploymentExpenses}
import models.expenses.createUpdate.CreateUpdateExpensesRequest
import models.expenses.{Expenses, ExpensesViewModel}
import models.mongo.ExpensesCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CreateOrAmendExpensesService, EmploymentSessionService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, EmploymentExpensesUtils, SessionHelper}
import views.html.expenses.{CheckEmploymentExpensesView, CheckEmploymentExpensesViewEOY}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentExpensesController @Inject()(authorisedAction: AuthorisedAction,
                                                  checkEmploymentExpensesView: CheckEmploymentExpensesView,
                                                  checkEmploymentExpensesViewEOY: CheckEmploymentExpensesViewEOY,
                                                  createOrAmendExpensesService: CreateOrAmendExpensesService,
                                                  employmentSessionService: EmploymentSessionService,
                                                  auditService: AuditService,
                                                  inYearAction: InYearAction,
                                                  errorHandler: ErrorHandler,
                                                  implicit val clock: Clock,
                                                  implicit val appConfig: AppConfig,
                                                  implicit val mcc: MessagesControllerComponents,
                                                  implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>

    val isInYear: Boolean = inYearAction.inYear(taxYear)

    def isMultipleEmployments(allEmploymentData: AllEmploymentData): Boolean = {
      employmentSessionService.getLatestEmploymentData(allEmploymentData, isInYear).length > 1
    }

    def inYearResult(allEmploymentData: AllEmploymentData): Result = {
      employmentSessionService.getLatestExpenses(allEmploymentData, isInYear = true) match {
        case Some((EmploymentExpenses(_, _, _, Some(expenses)), isUsingCustomerData)) => performAuditAndRenderView(expenses, taxYear, isInYear,
          isMultipleEmployments(allEmploymentData), isUsingCustomerData)
        case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    def saveCYAAndReturnEndOfYearResult(allEmploymentData: Option[AllEmploymentData]): Future[Result] = {
      allEmploymentData match {
        case Some(allEmploymentData) =>
          employmentSessionService.getLatestExpenses(allEmploymentData, isInYear) match {
            case Some((EmploymentExpenses(submittedOn, _, _, Some(expenses)), isUsingCustomerData)) =>
              employmentSessionService.createOrUpdateExpensesSessionData(ExpensesCYAModel.makeModel(expenses, isUsingCustomerData, submittedOn),
                taxYear, isPriorSubmission = true, hasPriorExpenses = false
              )(errorHandler.internalServerError()) {
                performAuditAndRenderView(expenses, taxYear, isInYear, isMultipleEmployments(allEmploymentData), isUsingCustomerData)
              }
            //TODO redirect to receive any expenses page
            case None => Future(performAuditAndRenderView(Expenses(), taxYear, isInYear, isMultipleEmployments(allEmploymentData), isUsingCustomerData = true))
          }
        //TODO redirect to receive any expenses page
        case None => Future(performAuditAndRenderView(Expenses(), taxYear, isInYear, isMultipleEmployments = false, isUsingCustomerData = true))
      }
    }

    if (isInYear) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear)(inYearResult)
    } else {
      employmentSessionService.getAndHandleExpenses(taxYear) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val expensesViewModel: ExpensesViewModel = cya.expensesCya.expenses
            val isUsingCustomerData = expensesViewModel.isUsingCustomerData

            Future(performAuditAndRenderView(expensesViewModel.toExpenses, taxYear, isInYear, prior.exists(isMultipleEmployments),
              isUsingCustomerData, Some(expensesViewModel)))
          case None =>
            saveCYAAndReturnEndOfYearResult(prior)
        }
      }
    }
  }

  def performAuditAndRenderView(expenses: Expenses, taxYear: Int, isInYear: Boolean, isMultipleEmployments: Boolean,
                                isUsingCustomerData: Boolean, cya: Option[ExpensesViewModel] = None)
                               (implicit user: User[AnyContent]): Result = {
    val auditModel = ViewEmploymentExpensesAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, expenses)
    auditService.sendAudit[ViewEmploymentExpensesAudit](auditModel.toAuditModel)

    if (isInYear) {
      Ok(checkEmploymentExpensesView(taxYear, expenses, isInYear, isMultipleEmployments))
    } else {
      Ok(checkEmploymentExpensesViewEOY(taxYear, expenses.toExpensesViewModel(isUsingCustomerData, cyaExpenses = cya), isInYear, isMultipleEmployments))

    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {

      employmentSessionService.getAndHandleExpenses(taxYear) { (cya, prior) =>
        cya match {
          case Some(cya) =>

            cya.expensesCya.expenses.expensesIsFinished(taxYear) match {
              // TODO: potentially navigate elsewhere - design to confirm
              case Some(unfinishedRedirect) => Future.successful(Redirect(unfinishedRedirect))
              case _ =>
                createOrAmendExpensesService.createExpensesModelAndReturnResult(cya, prior, taxYear) {
                  model =>
                    createOrAmendExpensesService.createOrUpdateExpensesResult(taxYear, model).flatMap {
                      case Left(result) =>
                        Future.successful(result)
                      case Right(result) =>
                        performSubmitAudits(model, taxYear, prior)
                        employmentSessionService.clearExpenses(taxYear)(
                          result
                        )
                    }
                }
            }

          case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
        }
      }
    }
  }

  def performSubmitAudits(model: CreateUpdateExpensesRequest, taxYear: Int, prior: Option[AllEmploymentData])
                         (implicit user: User[_]): Future[AuditResult] = {

    val audit: Either[AuditModel[AmendEmploymentExpensesUpdateAudit], AuditModel[CreateNewEmploymentExpensesAudit]] =
      prior.flatMap {
        prior =>
          val priorData = EmploymentExpensesUtils.getLatestExpenses(prior, isInYear = false)
          priorData.map(prior => model.toAmendAuditModel(taxYear, prior._1).toAuditModel)
      }.map(Left(_)).getOrElse {
        Right(model.toCreateAuditModel(taxYear).toAuditModel)
      }
    audit match {
      case Left(amend) => auditService.sendAudit(amend)
      case Right(create) => auditService.sendAudit(create)
    }
  }
}



