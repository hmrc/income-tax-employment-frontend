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

import audit.{AuditService, ViewEmploymentDetailsAudit}
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckEmploymentDetailsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import javax.inject.Inject
import models.User
import models.employment.{AllEmploymentData, EmploymentDetailsViewModel}
import models.mongo.EmploymentCYAModel
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.CheckEmploymentDetailsView

import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentDetailsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 employmentDetailsView: CheckEmploymentDetailsView,
                                                 inYearAction: InYearAction,
                                                 appConfig: AppConfig,
                                                 employmentSessionService: EmploymentSessionService,
                                                 auditService: AuditService,
                                                 ec: ExecutionContext,
                                                 errorHandler: ErrorHandler,
                                                 clock: Clock) extends FrontendController(cc)
  with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    val isInYear: Boolean = inYearAction.inYear(taxYear)

    def inYearResult(allEmploymentData: AllEmploymentData): Result = {
      employmentSessionService.employmentSourceToUse(allEmploymentData,employmentId,isInYear) match {
        case Some((source, isUsingCustomerData)) =>
          performAuditAndRenderView(source.toEmploymentDetailsViewModel(isUsingCustomerData), taxYear, isInYear)
        case None =>
          logger.info(s"[CheckEmploymentDetailsController][inYearResult] No prior employment data exists with employmentId." +
            s"Redirecting to overview page. SessionId: ${user.sessionId}")
          Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    def saveCYAAndReturnEndOfYearResult(allEmploymentData: AllEmploymentData): Future[Result] = {
      employmentSessionService.employmentSourceToUse(allEmploymentData,employmentId,isInYear) match {
        case Some((source, isUsingCustomerData)) =>
          employmentSessionService.createOrUpdateSessionData(employmentId, EmploymentCYAModel.apply(source, isUsingCustomerData),
            taxYear, isPriorSubmission = true
          )(errorHandler.internalServerError()){
            performAuditAndRenderView(source.toEmploymentDetailsViewModel(isUsingCustomerData),taxYear, isInYear)
          }

        case None =>
          logger.info(s"[CheckEmploymentDetailsController][saveCYAAndReturnEndOfYearResult] No prior employment data exists with employmentId." +
            s"Redirecting to overview page. SessionId: ${user.sessionId}")
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }

    if (isInYear) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear)(inYearResult)
    } else {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) => Future(
            performAuditAndRenderView(cya.employment.toEmploymentDetailsView(employmentId, !cya.employment.employmentDetails.currentDataIsHmrcHeld),
              taxYear, isInYear)
          )
          case None =>
            prior.fold(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))) {
              prior => saveCYAAndReturnEndOfYearResult(prior)
            }
        }
      }
    }
  }

  def performAuditAndRenderView(employmentDetails: EmploymentDetailsViewModel, taxYear: Int, isInYear: Boolean)(implicit user: User[AnyContent]): Result ={
    val auditModel = ViewEmploymentDetailsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, employmentDetails)
    auditService.auditModel[ViewEmploymentDetailsAudit](auditModel.toAuditModel)
    Ok(employmentDetailsView(employmentDetails, taxYear, isInYear))
  }

  def submit(taxYear:Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    inYearAction.notInYear(taxYear){
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>

            employmentSessionService.createModelAndReturnResult(cya,prior,taxYear){
              model =>
                employmentSessionService.createOrUpdateEmploymentResult(taxYear,model).flatMap{
                  result =>
                    employmentSessionService.clear(taxYear,employmentId)(errorHandler.internalServerError())(result)
                }
            }

          case None => Future.successful(Redirect(CheckEmploymentDetailsController.show(taxYear,employmentId)))
        }
      }
    }
  }
}
