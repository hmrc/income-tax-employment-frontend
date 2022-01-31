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
import common.{EmploymentSection, SessionValues}
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckEmploymentDetailsController
import models.User
import models.employment._
import models.mongo.EmploymentCYAModel
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import services.employment.CheckEmploymentDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.employment.CheckEmploymentDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentDetailsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 employmentDetailsView: CheckEmploymentDetailsView,
                                                 authorisedAction: AuthorisedAction,
                                                 inYearAction: InYearUtil,
                                                 appConfig: AppConfig,
                                                 employmentSessionService: EmploymentSessionService,
                                                 checkEmploymentDetailsService: CheckEmploymentDetailsService,
                                                 ec: ExecutionContext,
                                                 errorHandler: ErrorHandler,
                                                 clock: Clock
                                                ) extends FrontendController(cc) with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedTaxYearAction(taxYear).async { implicit user =>
    if (inYearAction.inYear(taxYear)) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { employmentData =>
        employmentData.inYearEmploymentSourceWith(employmentId) match {
          case Some(EmploymentSourceOrigin(source, isUsingCustomerData)) =>
            val viewModel = source.toEmploymentDetailsViewModel(isUsingCustomerData)
            val isSingleEmploymentValue = checkEmploymentDetailsService.isSingleEmploymentAndAudit(viewModel, taxYear, isInYear = true, Some(employmentData))
            Ok(employmentDetailsView(viewModel, taxYear, isInYear = true, isSingleEmployment = isSingleEmploymentValue))
          case None =>
            logger.info(s"[CheckEmploymentDetailsController][inYearResult] No prior employment data exists with employmentId." +
              s"Redirecting to overview page. SessionId: ${user.sessionId}")
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }
    } else {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) => if (!cya.isPriorSubmission && !cya.employment.employmentDetails.isFinished) {
            Future.successful(employmentDetailsRedirect(cya.employment, taxYear, employmentId, cya.isPriorSubmission))
          } else {
            prior match {
              case Some(employment) => Future.successful {
                val viewModel = cya.employment.toEmploymentDetailsView(employmentId, !cya.employment.employmentDetails.currentDataIsHmrcHeld)
                val isSingleEmploymentValue = checkEmploymentDetailsService
                  .isSingleEmploymentAndAudit(viewModel, taxYear, isInYear = false, Some(employment))
                Ok(employmentDetailsView(viewModel, taxYear, isInYear = false, isSingleEmployment = isSingleEmploymentValue))
              }
              case None => Future.successful {
                val viewModel = cya.employment.toEmploymentDetailsView(employmentId, !cya.employment.employmentDetails.currentDataIsHmrcHeld)
                val isSingleEmploymentValue = checkEmploymentDetailsService
                  .isSingleEmploymentAndAudit(viewModel, taxYear, isInYear = false, None)
                Ok(employmentDetailsView(viewModel, taxYear, isInYear = false, isSingleEmployment = isSingleEmploymentValue))
              }
            }
          }
          case None => prior.fold(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))) { employmentData =>
            saveCYAAndReturnEndOfYearResult(taxYear, employmentId, employmentData)
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) => employmentSessionService.createModelAndReturnResult(cya, prior, taxYear, EmploymentSection.EMPLOYMENT_DETAILS) { model =>
            employmentSessionService.createOrUpdateEmploymentResult(taxYear, model).flatMap {
              case Left(result) => Future.successful(result)
              case Right(result) => checkEmploymentDetailsService.performSubmitAudits(model, employmentId, taxYear, prior)
                if (appConfig.nrsEnabled) {
                  checkEmploymentDetailsService.performSubmitNrsPayload(model, employmentId, prior)
                }
                employmentSessionService.clear(taxYear, employmentId).map {
                  case Left(_) => errorHandler.internalServerError()
                  case Right(_) => result.removingFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID)
                }
            }
          }
          case None => Future.successful(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  private def saveCYAAndReturnEndOfYearResult(taxYear: Int, employmentId: String, employmentData: AllEmploymentData)
                                             (implicit user: User[AnyContent]): Future[Result] = {
    employmentData.eoyEmploymentSourceWith(employmentId) match {
      case Some(EmploymentSourceOrigin(source, isUsingCustomerData)) =>
        employmentSessionService.createOrUpdateSessionData(employmentId, EmploymentCYAModel.apply(source, isUsingCustomerData),
          taxYear, isPriorSubmission = true, source.hasPriorBenefits
        )(errorHandler.internalServerError()) {
          val viewModel = source.toEmploymentDetailsViewModel(isUsingCustomerData)
          val isSingleEmploymentValue = checkEmploymentDetailsService.isSingleEmploymentAndAudit(viewModel, taxYear, isInYear = false, Some(employmentData))
          Ok(employmentDetailsView(viewModel, taxYear, isInYear = false, isSingleEmployment = isSingleEmploymentValue))
        }
      case None =>
        logger.info(s"[CheckEmploymentDetailsController][saveCYAAndReturnEndOfYearResult] No prior employment data exists with employmentId." +
          s"Redirecting to overview page. SessionId: ${user.sessionId}")
        Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }
}
