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

package controllers.employment

import actions.AuthorisedAction
import actions.AuthorisedTaxYearAction.authorisedTaxYearAction
import audit.{AuditService, ViewEmploymentBenefitsAudit}
import common.{EmploymentSection, SessionValues}
import config.{AppConfig, ErrorHandler}
import controllers.benefits.routes.ReceiveAnyBenefitsController
import controllers.employment.routes.{CheckYourBenefitsController, EmployerInformationController}
import controllers.expenses.routes.CheckEmploymentExpensesController
import controllers.studentLoans.routes.StudentLoansCYAController
import models.AuthorisationRequest
import models.benefits.Benefits
import models.employment.{AllEmploymentData, EmploymentSourceOrigin}
import models.mongo.EmploymentCYAModel
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.employment.CheckYourBenefitsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.employment.{CheckYourBenefitsView, CheckYourBenefitsViewEOY}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourBenefitsController @Inject()(implicit val appConfig: AppConfig,
                                            val mcc: MessagesControllerComponents,
                                            authorisedAction: AuthorisedAction,
                                            checkYourBenefitsView: CheckYourBenefitsView,
                                            checkYourBenefitsViewEOY: CheckYourBenefitsViewEOY,
                                            employmentSessionService: EmploymentSessionService,
                                            checkYourBenefitsService: CheckYourBenefitsService,
                                            auditService: AuditService,
                                            inYearAction: InYearUtil,
                                            errorHandler: ErrorHandler,
                                            implicit val ec: ExecutionContext
                                           ) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  //scalastyle:off
  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedTaxYearAction(taxYear).async { implicit request =>
    if (inYearAction.inYear(taxYear)) {
      employmentSessionService.findPreviousEmploymentUserData(request.user, taxYear) { allEmploymentData: AllEmploymentData =>
        val redirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        allEmploymentData.inYearEmploymentSourceWith(employmentId) match {
          case Some(EmploymentSourceOrigin(source, isUsingCustomerData)) =>
            source.employmentBenefits.flatMap(_.benefits) match {
              case None => redirect
              case Some(benefits) =>
                val auditModel = ViewEmploymentBenefitsAudit(taxYear, request.user.affinityGroup.toLowerCase, request.user.nino, request.user.mtditid, benefits)
                auditService.sendAudit[ViewEmploymentBenefitsAudit](auditModel.toAuditModel)
                Ok(checkYourBenefitsView(taxYear, source.employerName, benefits.toBenefitsViewModel(isUsingCustomerData), allEmploymentData.isLastInYearEmployment, employmentId))
            }
          case None => redirect
        }
      }
    } else {
      employmentSessionService.getAndHandle(taxYear, employmentId, redirectWhenNoPrior = true) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            cya.employment.employmentBenefits match {
              case None => Future(Redirect(ReceiveAnyBenefitsController.show(taxYear, employmentId)))
              case Some(benefits) => Future(Ok(checkYourBenefitsViewEOY(
                taxYear,
                cya.employment.employmentDetails.employerName,
                benefits.toBenefits.toBenefitsViewModel(benefits.isUsingCustomerData, cyaBenefits = Some(benefits)),
                employmentId,
                benefits.isUsingCustomerData
              )))
            }
          case None => prior.get.eoyEmploymentSourceWith(employmentId) match {
            case Some(EmploymentSourceOrigin(source, isUsingCustomerData)) =>
              employmentSessionService.createOrUpdateSessionData(request.user, taxYear, employmentId, EmploymentCYAModel(source, isUsingCustomerData),
                isPriorSubmission = true, source.hasPriorBenefits, source.hasPriorStudentLoans)(errorHandler.internalServerError())({
                val benefits: Option[Benefits] = source.employmentBenefits.flatMap(_.benefits)
                benefits match {
                  case None => Redirect(ReceiveAnyBenefitsController.show(taxYear, employmentId))
                  case Some(benefits) =>
                    Ok(checkYourBenefitsViewEOY(taxYear, source.employerName, benefits.toBenefitsViewModel(isUsingCustomerData), employmentId, isUsingCustomerData))
                }
              })
            case None =>
              logger.info(s"[CheckYourBenefitsController][saveCYAAndReturnEndOfYearResult] No prior employment data exists with employmentId." +
                s"Redirecting to overview page. SessionId: ${request.user.sessionId}")
              Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            employmentSessionService.createModelOrReturnResult(request.user, cya, prior, taxYear, EmploymentSection.EMPLOYMENT_BENEFITS) match {
              case Left(result) => Future.successful(result)
              case Right(model) =>
                employmentSessionService.createOrUpdateEmploymentResult(taxYear, model).flatMap {
                  case Left(result) => Future.successful(result)
                  case Right(returnedEmploymentId) =>
                    checkYourBenefitsService.performSubmitAudits(request.user, model, employmentId, taxYear, prior)
                    if (appConfig.nrsEnabled) {
                      checkYourBenefitsService.performSubmitNrsPayload(request.user, model, employmentId, prior)
                    }
                    employmentSessionService.clear(request.user, taxYear, employmentId).map {
                      case Left(_) => errorHandler.internalServerError()
                      case Right(_) => getResultFromResponse(returnedEmploymentId, taxYear, employmentId)
                    }
                }
            }
          case None => Future.successful(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def getResultFromResponse(returnedEmploymentId: Option[String], taxYear: Int, employmentId: String)(implicit request: AuthorisationRequest[_]): Result = {
    Redirect(returnedEmploymentId match {
      case Some(employmentId) => EmployerInformationController.show(taxYear, employmentId)
      case None =>
        getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID) match {
          case Some(sessionEmploymentId) if sessionEmploymentId == employmentId =>
            if (appConfig.studentLoansEnabled) {
              StudentLoansCYAController.show(taxYear, employmentId)
            }
            else {
              CheckEmploymentExpensesController.show(taxYear)
            }
          case _ => EmployerInformationController.show(taxYear, employmentId)
        }
    })
  }
}
