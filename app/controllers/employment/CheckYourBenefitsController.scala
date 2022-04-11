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
import models.benefits.{Benefits, BenefitsViewModel}
import models.employment.createUpdate.{CreateUpdateEmploymentRequest, JourneyNotFinished, NothingToUpdate}
import models.employment.{AllEmploymentData, EmploymentSourceOrigin, OptionalCyaAndPrior}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.employment.CheckYourBenefitsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.employment.CheckYourBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourBenefitsController @Inject()(implicit val appConfig: AppConfig,
                                            val mcc: MessagesControllerComponents,
                                            authorisedAction: AuthorisedAction,
                                            checkYourBenefitsView: CheckYourBenefitsView,
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
                Ok(checkYourBenefitsView(taxYear, employmentId, source.employerName, benefits.toBenefitsViewModel(isUsingCustomerData), isUsingCustomerData = false, isInYear = true, showNotification = false))
            }
          case None => redirect
        }
      }
    } else {
      employmentSessionService.getAndHandle(taxYear, employmentId, redirectWhenNoPrior = true) { (cya, prior) =>
        (cya, prior) match {
          case (_, Some(allEmploymentData)) if allEmploymentData.eoyEmploymentSourceWith(employmentId).exists(!_.employmentSource.employmentDetailsSubmittable) =>
            val sourceOrigin = allEmploymentData.eoyEmploymentSourceWith(employmentId).get
            val benefits = sourceOrigin.employmentSource.employmentBenefits.flatMap(_.benefits).getOrElse(Benefits())
            Future.successful(Ok(checkYourBenefitsView(taxYear, employmentId, sourceOrigin.employmentSource.employerName, benefits.toBenefitsViewModel(sourceOrigin.isCustomerData), sourceOrigin.isCustomerData, isInYear = false, showNotification = true)))
          case (Some(cya), _) =>
            cya.employment.employmentBenefits match {
              case None => Future(Redirect(ReceiveAnyBenefitsController.show(taxYear, employmentId)))
              case Some(benefits: BenefitsViewModel) => Future(Ok(checkYourBenefitsView(
                taxYear,
                employmentId,
                cya.employment.employmentDetails.employerName,
                benefits.toBenefits.toBenefitsViewModel(benefits.isUsingCustomerData, cyaBenefits = Some(benefits)),
                isUsingCustomerData = benefits.isUsingCustomerData,
                isInYear = false,
                showNotification = false
              )))
            }
          case (None, _) => prior.get.eoyEmploymentSourceWith(employmentId) match {
            case Some(EmploymentSourceOrigin(source, isUsingCustomerData)) =>
              employmentSessionService.createOrUpdateSessionData(request.user, taxYear, employmentId, EmploymentCYAModel(source, isUsingCustomerData),
                isPriorSubmission = true, source.hasPriorBenefits, source.hasPriorStudentLoans)(errorHandler.internalServerError())({
                val benefits: Option[Benefits] = source.employmentBenefits.flatMap(_.benefits)
                benefits match {
                  case None => Redirect(ReceiveAnyBenefitsController.show(taxYear, employmentId))
                  case Some(benefits) =>
                    Ok(checkYourBenefitsView(taxYear, employmentId, source.employerName, benefits.toBenefitsViewModel(isUsingCustomerData), isUsingCustomerData, isInYear = false, showNotification = false))
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
    employmentSessionService.getOptionalCYAAndPriorForEndOfYear(taxYear, employmentId).flatMap {
      case Left(result) => Future.successful(result)
      case Right(OptionalCyaAndPrior(Some(cya), prior)) =>
        employmentSessionService.createModelOrReturnError(request.user, cya, prior, EmploymentSection.EMPLOYMENT_BENEFITS) match {
          case Left(NothingToUpdate) =>
            getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID) match {
              case Some(sessionEmploymentId) if sessionEmploymentId == employmentId =>
                if (appConfig.studentLoansEnabled) {
                  Future.successful(Redirect(StudentLoansCYAController.show(taxYear, employmentId)))
                } else {
                  Future.successful(Redirect(CheckEmploymentExpensesController.show(taxYear)))
                }
              case _ => Future.successful(Redirect(EmployerInformationController.show(taxYear, employmentId)))
            }
          case Left(JourneyNotFinished) =>
            //TODO Route to: journey not finished page / show banner saying not finished / hide submit button when not complete?
            Future.successful(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))

          case Right(model) => employmentSessionService.submitAndClear(taxYear, employmentId, model, cya, prior, Some(auditAndNRS)).flatMap {
            case Left(result) => Future.successful(result)
            case Right((returnedEmploymentId, cya)) => getResultFromResponse(returnedEmploymentId, taxYear, employmentId, cya)
          }
        }

      case _ => Future.successful(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
    }
  }

  private def auditAndNRS(employmentId: String, taxYear: Int, model: CreateUpdateEmploymentRequest,
                          prior: Option[AllEmploymentData], request: AuthorisationRequest[_]): Unit = {

    implicit val implicitRequest: AuthorisationRequest[_] = request

    checkYourBenefitsService.performSubmitAudits(request.user, model, employmentId, taxYear, prior)
    if (appConfig.nrsEnabled) {
      checkYourBenefitsService.performSubmitNrsPayload(request.user, model, employmentId, prior)
    }
  }

  private def getResultFromResponse(returnedEmploymentId: Option[String], taxYear: Int, employmentId: String, cya: EmploymentUserData)(implicit request: AuthorisationRequest[_]): Future[Result] = {
    returnedEmploymentId match {
      case Some(employmentId) => Future.successful(Redirect(EmployerInformationController.show(taxYear, employmentId)))
      case None =>
        getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID) match {
          case Some(sessionEmploymentId) if sessionEmploymentId == employmentId =>
            val result = Redirect(if (appConfig.studentLoansEnabled) {
              StudentLoansCYAController.show(taxYear, employmentId)
            } else {
              CheckEmploymentExpensesController.show(taxYear)
            })

            if (appConfig.mimicEmploymentAPICalls) {
              employmentSessionService.createOrUpdateSessionData(
                request.user, taxYear, employmentId, cya.employment, isPriorSubmission = true, hasPriorBenefits = true, hasPriorStudentLoans = false
              )(errorHandler.internalServerError())(result)
            } else {
              Future.successful(result)
            }
          case _ => Future.successful(Redirect(EmployerInformationController.show(taxYear, employmentId)))
        }
    }
  }
}
