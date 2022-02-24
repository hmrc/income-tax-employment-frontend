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

package controllers.studentLoans

import actions.{AuthorisedAction, TaxYearAction}
import common.{EmploymentSection, SessionValues}
import config.AppConfig
import controllers.employment.routes.EmployerInformationController
import controllers.expenses.routes.CheckEmploymentExpensesController
import javax.inject.Inject
import models.employment.OptionalCyaAndPrior
import models.employment.createUpdate.{JourneyNotFinished, NothingToUpdate}
import models.mongo.EmploymentUserData
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.studentLoans.StudentLoansCYAService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.studentLoans.StudentLoansCYAView

import scala.concurrent.{ExecutionContext, Future}

class StudentLoansCYAController @Inject()(mcc: MessagesControllerComponents,
                                          view: StudentLoansCYAView,
                                          service: StudentLoansCYAService,
                                          employmentSessionService: EmploymentSessionService,
                                          authAction: AuthorisedAction,
                                          inYearAction: InYearUtil,
                                          implicit val appConfig: AppConfig,
                                          implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>

    if (appConfig.studentLoansEnabled) {
      val inYear: Boolean = inYearAction.inYear(taxYear)
      
      service.retrieveCyaDataAndIsCustomerHeld(taxYear, employmentId) { case (cya, isCustomer, isSingleEmployment) =>
        Ok(view(taxYear, employmentId, cya, isCustomer, inYear, isSingleEmployment))
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
    if (appConfig.studentLoansEnabled) {
      def getResultFromResponse(returnedEmploymentId: Option[String], cya: EmploymentUserData): Future[Result] = {
        val log = "[StudentLoansCYAController][getResultFromResponse]"
        returnedEmploymentId match {
          case Some(employmentId) =>
            logger.info(s"$log An employment id was returned meaning that the employment was a prior employment. Returning to employer information page.")
            Future.successful(Redirect(EmployerInformationController.show(taxYear, employmentId)))
          case None =>
            getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID) match {
              case Some(sessionEmploymentId) if sessionEmploymentId == employmentId =>
                logger.info(s"$log Update to student loans was made. Continuing to expenses as it's a new employment.")
                val result = Redirect(CheckEmploymentExpensesController.show(taxYear))
                if (appConfig.mimicEmploymentAPICalls) {
                  service.createOrUpdateSessionData(employmentId, cya, taxYear, request.user)(result)
                } else {
                  Future.successful(result)
                }
              case _ => logger.info(s"$log Update was made to existing employment returning to employer information page.")
                Future.successful(Redirect(EmployerInformationController.show(taxYear, employmentId)))
            }
        }
      }
      def nothingToUpdateRedirect: Future[Result] = {
        val log = "[StudentLoansCYAController][nothingToUpdateRedirect]"
        getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID) match {
          case Some(sessionEmploymentId) if sessionEmploymentId == employmentId =>
            logger.info(s"$log Nothing to update for student loans. Continuing to expenses as it's a new employment.")
            Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
          case _ => logger.info(s"$log Nothing to update for student loans. Returning to employer information page.")
            Future.successful(Redirect(EmployerInformationController.show(taxYear, employmentId)))
        }
      }
      employmentSessionService.getOptionalCYAAndPriorForEndOfYear(taxYear, employmentId).flatMap {
        case Left(result) => Future.successful(result)
        case Right(OptionalCyaAndPrior(Some(cya), prior)) =>
          employmentSessionService.createModelOrReturnError(request.user, cya, prior, EmploymentSection.STUDENT_LOANS) match {
            case Right(model) => employmentSessionService.submitAndClear(taxYear, employmentId, model, cya, prior).flatMap {
              case Left(result) => Future.successful(result)
              case Right((returnedEmploymentId, cya)) => getResultFromResponse(returnedEmploymentId, cya)
            }
            case Left(JourneyNotFinished) => Future.successful(Redirect(routes.StudentLoansCYAController.show(taxYear, employmentId)))
            case Left(NothingToUpdate) => nothingToUpdateRedirect
          }
        case _ => Future.successful(Redirect(routes.StudentLoansCYAController.show(taxYear, employmentId)))
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }
}
