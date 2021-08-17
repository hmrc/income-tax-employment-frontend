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

package controllers.employment

import audit.{AuditService, ViewEmploymentExpensesAudit}
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckEmploymentExpensesController
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.employment.{AllEmploymentData, EmploymentExpenses, EmploymentSource, Expenses}

import javax.inject.Inject
import models.User
import models.mongo.ExpensesCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.CheckEmploymentExpensesView

import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentExpensesController @Inject()(authorisedAction: AuthorisedAction,
                                                  checkEmploymentExpensesView: CheckEmploymentExpensesView,
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

    def isSingleEmployment(allEmploymentData: AllEmploymentData):Boolean = {
      employmentSessionService.getLatestEmploymentData(allEmploymentData,isInYear).length ==1
    }

    def inYearResult(allEmploymentData: AllEmploymentData): Result = {
      allEmploymentData.hmrcExpenses match {
        case Some(EmploymentExpenses(_, _, _, Some(expenses))) => performAuditAndRenderView(expenses,taxYear,isInYear, isSingleEmployment(allEmploymentData))
        case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    def saveCYAAndReturnEndOfYearResult(allEmploymentData: Option[AllEmploymentData]): Future[Result] = {
      allEmploymentData match {
        case Some(allEmploymentData) =>
          employmentSessionService.getLatestExpenses(allEmploymentData, isInYear) match {
            case Some((EmploymentExpenses(_, _, _, Some(expenses)), isUsingCustomerData)) =>
              employmentSessionService.createOrUpdateExpensesSessionData(ExpensesCYAModel.makeModel(expenses, isUsingCustomerData),
                taxYear, isPriorSubmission = true
              )(errorHandler.internalServerError()) {
                performAuditAndRenderView(expenses, taxYear, isInYear, isSingleEmployment(allEmploymentData))
              }
            case None => Future(performAuditAndRenderView(Expenses(), taxYear, isInYear, isSingleEmployment(allEmploymentData)))
          }
        case None => Future(performAuditAndRenderView(Expenses(), taxYear, isInYear, None))
    }

    if (isInYear) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear)(inYearResult)
    } else {
      employmentSessionService.getAndHandleExpenses(taxYear) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val expenses = cya.expensesCya
            Future(performAuditAndRenderView(expenses.expenses, taxYear, isInYear))
          case None =>
            saveCYAAndReturnEndOfYearResult(prior)
        }
      }
    }
  }

  def performAuditAndRenderView(expenses:Expenses, taxYear: Int, isInYear: Boolean)
                               (implicit user: User[AnyContent]): Result ={
    val auditModel = ViewEmploymentExpensesAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, expenses)
    auditService.auditModel[ViewEmploymentExpensesAudit](auditModel.toAuditModel)
    Ok(checkEmploymentExpensesView(taxYear, expenses, isInYear, isSingleEmployment))
  }

  def submit(taxYear:Int): Action[AnyContent] = authorisedAction.async { implicit user =>

    inYearAction.notInYear(taxYear){
      employmentSessionService.getAndHandleExpenses(taxYear) { (cya, prior) =>
        cya match {
          case Some(cya) =>

            //TODO create CreateUpdateExpensesRequest model with new expenses data
            //            employmentSessionService.createModelAndReturnResult(cya,prior,taxYear){
            //              model =>
            //                employmentSessionService.createOrUpdateEmploymentResult(taxYear,model).flatMap{
            //                  result =>
            //                    employmentSessionService.clear(taxYear)(errorHandler.internalServerError())(result)
            //                }
            //            }
            Future.successful(errorHandler.internalServerError())

          case None => Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
        }
      }
    }
  }
}
