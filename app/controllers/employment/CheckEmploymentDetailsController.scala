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

import audit._
import common.SessionValues
import config.{AppConfig, ErrorHandler}
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import controllers.employment.routes.CheckEmploymentDetailsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.User
import models.employment._
import models.employment.createUpdate.CreateUpdateEmploymentRequest
import models.mongo.EmploymentCYAModel
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{EmploymentSessionService, NrsService, RedirectService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.CheckEmploymentDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentDetailsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 employmentDetailsView: CheckEmploymentDetailsView,
                                                 inYearAction: InYearAction,
                                                 appConfig: AppConfig,
                                                 employmentSessionService: EmploymentSessionService,
                                                 nrsService: NrsService,
                                                 auditService: AuditService,
                                                 ec: ExecutionContext,
                                                 errorHandler: ErrorHandler,
                                                 clock: Clock) extends FrontendController(cc)
  with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    val isInYear: Boolean = inYearAction.inYear(taxYear)

    def inYearResult(allEmploymentData: AllEmploymentData): Result = {
      employmentSessionService.employmentSourceToUse(allEmploymentData, employmentId, isInYear) match {
        case Some((source, isUsingCustomerData)) =>
          performAuditAndRenderView(source.toEmploymentDetailsViewModel(isUsingCustomerData), taxYear, isInYear, allEmploymentData)
        case None =>
          logger.info(s"[CheckEmploymentDetailsController][inYearResult] No prior employment data exists with employmentId." +
            s"Redirecting to overview page. SessionId: ${user.sessionId}")
          Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    def saveCYAAndReturnEndOfYearResult(allEmploymentData: AllEmploymentData): Future[Result] = {
      employmentSessionService.employmentSourceToUse(allEmploymentData, employmentId, isInYear) match {
        case Some((source, isUsingCustomerData)) =>
          employmentSessionService.createOrUpdateSessionData(employmentId, EmploymentCYAModel.apply(source, isUsingCustomerData),
            taxYear, isPriorSubmission = true, source.hasPriorBenefits
          )(errorHandler.internalServerError()) {
            performAuditAndRenderView(source.toEmploymentDetailsViewModel(isUsingCustomerData), taxYear, isInYear, allEmploymentData)
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
          case Some(cya) =>
            if (!cya.isPriorSubmission && !cya.employment.employmentDetails.isFinished) {
              Future.successful(RedirectService.employmentDetailsRedirect(cya.employment, taxYear, employmentId, cya.isPriorSubmission))
            } else {
              prior match {
                case Some(employment) => Future.successful(performAuditAndRenderView(cya.employment.toEmploymentDetailsView(
                  employmentId, !cya.employment.employmentDetails.currentDataIsHmrcHeld), taxYear, isInYear, employment))
                case None => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
              }
            }
          case None =>
            prior.fold(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))) {
              saveCYAAndReturnEndOfYearResult
            }
        }
      }
    }
  }

  def performAuditAndRenderView(employmentDetails: EmploymentDetailsViewModel, taxYear: Int, isInYear: Boolean, allEmploymentData: AllEmploymentData)(implicit user: User[AnyContent]): Result = {
    val auditModel = ViewEmploymentDetailsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, employmentDetails)
    auditService.sendAudit[ViewEmploymentDetailsAudit](auditModel.toAuditModel)

    val employmentSource: Seq[EmploymentSource] = employmentSessionService.getLatestEmploymentData(allEmploymentData, isInYear)
    val isSingleEmployment: Boolean = employmentSource.length == 1
    Ok(employmentDetailsView(employmentDetails, taxYear, isInYear, isSingleEmployment))
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>

            employmentSessionService.createModelAndReturnResult(cya, prior, taxYear) {
              model =>

                employmentSessionService.createOrUpdateEmploymentResult(taxYear, model).flatMap {
                  case Left(result) =>
                    Future.successful(result)
                  case Right(result) =>
                    performSubmitAudits(model, employmentId, taxYear, prior)

                    if (appConfig.nrsEnabled) {
                      performSubmitNrsPayload(model, employmentId, prior)
                    }

                    employmentSessionService.clear(taxYear, employmentId)(
                      result.removingFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID)
                    )
                }
            }
          case None => Future.successful(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def performSubmitAudits(model: CreateUpdateEmploymentRequest, employmentId: String, taxYear: Int,
                          prior: Option[AllEmploymentData])(implicit user: User[_]): Future[AuditResult] = {

    val audit: Either[AuditModel[AmendEmploymentDetailsUpdateAudit], AuditModel[CreateNewEmploymentDetailsAudit]] = prior.flatMap {
      prior =>
        val priorData = employmentSessionService.employmentSourceToUse(prior, employmentId, isInYear = false)
        priorData.map(prior => model.toAmendAuditModel(employmentId, taxYear, prior._1).toAuditModel)
    }.map(Left(_)).getOrElse {

      val existingEmployments = prior.map {
        prior =>
          employmentSessionService.getLatestEmploymentData(prior, isInYear = false).map {
            employment =>
              PriorEmploymentAuditInfo(employment.employerName, employment.employerRef)
          }
      }.getOrElse(Seq.empty)

      Right(model.toCreateAuditModel(taxYear, existingEmployments = existingEmployments).toAuditModel)
    }

    audit match {
      case Left(amend) => auditService.sendAudit(amend)
      case Right(create) => auditService.sendAudit(create)
    }
  }

  def performSubmitNrsPayload(model: CreateUpdateEmploymentRequest, employmentId: String, prior: Option[AllEmploymentData])
                             (implicit user: User[_]): Future[NrsSubmissionResponse] = {

    val nrsPayload: Either[DecodedAmendEmploymentDetailsPayload, DecodedCreateNewEmploymentDetailsPayload] = prior.flatMap {
      prior =>
        val priorData = employmentSessionService.employmentSourceToUse(prior, employmentId, isInYear = false)
        priorData.map(prior => model.toAmendDecodedPayloadModel(employmentId, prior._1))
    }.map(Left(_)).getOrElse {

      val existingEmployments = prior.map {
        prior =>
          employmentSessionService.getLatestEmploymentData(prior, isInYear = false).map {
            employment =>
              DecodedPriorEmploymentInfo(employment.employerName, employment.employerRef)
          }
      }.getOrElse(Seq.empty)

      Right(model.toCreateDecodedPayloadModel(existingEmployments))
    }

    nrsPayload match {
      case Left(amend) => nrsService.submit(user.nino, amend, user.mtditid)
      case Right(create) => nrsService.submit(user.nino, create, user.mtditid)
    }
  }
}
