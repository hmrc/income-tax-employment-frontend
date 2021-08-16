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

import audit.{AuditService, ViewEmploymentBenefitsAudit}
import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction}
import javax.inject.Inject
import models.User
import models.employment.{AllEmploymentData, Benefits}
import models.mongo.EmploymentCYAModel
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.CheckYourBenefitsView
import controllers.employment.routes.CheckYourBenefitsController

import scala.concurrent.{ExecutionContext, Future}

class CheckYourBenefitsController @Inject()(authorisedAction: AuthorisedAction,
                                            val mcc: MessagesControllerComponents,
                                            implicit val appConfig: AppConfig,
                                            checkYourBenefitsView: CheckYourBenefitsView,
                                            employmentSessionService: EmploymentSessionService,
                                            auditService: AuditService,
                                            inYearAction: InYearAction,
                                            errorHandler: ErrorHandler,
                                            implicit val clock: Clock,
                                            implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport
  with SessionHelper with Logging {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    val isInYear: Boolean = inYearAction.inYear(taxYear)

    def inYearResult(allEmploymentData: AllEmploymentData): Result = {
      val benefits = employmentSessionService.employmentSourceToUse(allEmploymentData, employmentId, isInYear).flatMap(
        _._1.employmentBenefits.flatMap(_.benefits))

      benefits match {
        case Some(benefits) => performAuditAndRenderView(benefits, taxYear, isInYear)
        case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    def saveCYAAndReturnEndOfYearResult(allEmploymentData: AllEmploymentData): Future[Result] = {
      employmentSessionService.employmentSourceToUse(allEmploymentData, employmentId, isInYear) match {
        case Some((source, isUsingCustomerData)) =>
          employmentSessionService.createOrUpdateSessionData(employmentId, EmploymentCYAModel.apply(source, isUsingCustomerData),
            taxYear, isPriorSubmission = true
          )(errorHandler.internalServerError()) {

            val benefits: Option[Benefits] = source.employmentBenefits.flatMap(_.benefits)
            performAuditAndRenderView(benefits.getOrElse(Benefits()), taxYear, isInYear)
          }

        case None =>
          logger.info(s"[CheckYourBenefitsController][saveCYAAndReturnEndOfYearResult] No prior employment data exists with employmentId." +
            s"Redirecting to overview page. SessionId: ${user.sessionId}")
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }

    if (isInYear) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear)(inYearResult)
    } else {
      employmentSessionService.getAndHandle(taxYear, employmentId, redirectWhenNoPrior = true) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val benefits: Option[Benefits] = cya.employment.employmentBenefits.flatMap(_.benefits)
            Future(performAuditAndRenderView(benefits.getOrElse(Benefits()), taxYear, isInYear))

          case None =>
            prior.fold(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))) {
              saveCYAAndReturnEndOfYearResult
            }
        }
      }
    }
  }

  def performAuditAndRenderView(benefits: Benefits, taxYear: Int, isInYear: Boolean)(implicit user: User[AnyContent]): Result ={
    val auditModel = ViewEmploymentBenefitsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, benefits)
    auditService.auditModel[ViewEmploymentBenefitsAudit](auditModel.toAuditModel)
    Ok(checkYourBenefitsView(taxYear, benefits, isInYear))
  }

  def submit(taxYear:Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    inYearAction.notInYear(taxYear){
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>

            //TODO create CreateUpdateEmploymentRequest model with new benefits data
//            employmentSessionService.createModelAndReturnResult(cya,prior,taxYear){
//              model =>
//                employmentSessionService.createOrUpdateEmploymentResult(taxYear,model).flatMap{
//                  result =>
//                    employmentSessionService.clear(taxYear,employmentId)(errorHandler.internalServerError())(result)
//                }
//            }
            Future.successful(errorHandler.internalServerError())

          case None => Future.successful(Redirect(CheckYourBenefitsController.show(taxYear,employmentId)))
        }
      }
    }
  }
}
