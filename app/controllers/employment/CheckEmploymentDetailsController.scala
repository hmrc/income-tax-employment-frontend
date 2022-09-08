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
import controllers.details.routes.EmployerNameController
import controllers.employment.routes.{CheckEmploymentDetailsController, CheckYourBenefitsController, EmployerInformationController}
import models.AuthorisationRequest
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmploymentRequest, JourneyNotFinished, NothingToUpdate}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.employment.CheckEmploymentDetailsService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.employment.CheckEmploymentDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentDetailsController @Inject()(pageView: CheckEmploymentDetailsView,
                                                 inYearAction: InYearUtil,
                                                 employmentSessionService: EmploymentSessionService,
                                                 checkEmploymentDetailsService: CheckEmploymentDetailsService,
                                                 redirectService: RedirectService,
                                                 errorHandler: ErrorHandler)
                                                (implicit cc: MessagesControllerComponents, ec: ExecutionContext,
                                                 appConf: AppConfig, authAction: AuthorisedAction)
  extends FrontendController(cc) with I18nSupport with SessionHelper with Logging {
  //scalastyle:off
  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedTaxYearAction(taxYear).async { implicit request =>
    if (inYearAction.inYear(taxYear)) {
      employmentSessionService.findPreviousEmploymentUserData(request.user, taxYear) { employmentData =>
        employmentData.hmrcEmploymentSourceWith(employmentId) match {
          case Some(EmploymentSourceOrigin(source, isUsingCustomerData)) =>
            val viewModel = source.toEmploymentDetailsViewModel(isUsingCustomerData)
            checkEmploymentDetailsService.sendViewEmploymentDetailsAudit(request.user, viewModel, taxYear)
            Ok(pageView(viewModel, taxYear, isInYear = true))
          case None =>
            logger.info(s"[CheckEmploymentDetailsController][inYearResult] No prior employment data exists with employmentId." +
              s"Redirecting to overview page. SessionId: ${request.user.sessionId}")
            Redirect(appConf.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }
    } else if (!appConf.employmentEOYEnabled) {
      Future.successful(Redirect(appConf.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) => if (!cya.isPriorSubmission && !cya.employment.employmentDetails.isFinished) {
            Future.successful(redirectService.employmentDetailsRedirect(cya.employment, taxYear, employmentId))
          } else {
            prior match {
              case Some(_) if !cya.employment.employmentDetails.isSubmittable => Future.successful(Redirect(EmployerNameController.show(taxYear, employmentId)))
              case _ => Future.successful {
                val viewModel = cya.employment.toEmploymentDetailsView(employmentId, !cya.employment.employmentDetails.currentDataIsHmrcHeld)
                checkEmploymentDetailsService.sendViewEmploymentDetailsAudit(request.user, viewModel, taxYear)
                Ok(pageView(viewModel, taxYear, isInYear = false))
              }
            }
          }
          case None => prior.fold(Future.successful(Redirect(appConf.incomeTaxSubmissionOverviewUrl(taxYear)))) { employmentData =>
            saveCYAAndReturnEndOfYearResult(taxYear, employmentId, employmentData)
          }
        }
      }
    }
  }
  //scalastyle:on

  //scalastyle:off
  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    if (!inYearAction.inYear(taxYear) && !appConf.employmentEOYEnabled) {
      Future.successful(Redirect(appConf.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      employmentSessionService.getOptionalCYAAndPriorForEndOfYear(taxYear, employmentId).flatMap {
        case Left(result) => Future.successful(result)
        case Right(OptionalCyaAndPrior(Some(cya), prior)) =>

          employmentSessionService.createModelOrReturnError(request.user, cya, prior, EmploymentSection.EMPLOYMENT_DETAILS) match {
            case Left(NothingToUpdate) =>
              logger.info("[CheckEmploymentDetailsController][submit] Nothing to update for employment details. Returning to employer information.")
              Future.successful(Redirect(EmployerInformationController.show(taxYear, employmentId)))
            case Left(JourneyNotFinished) =>
              //TODO Route to: journey not finished page / show banner saying not finished / hide submit button when not complete?
              Future.successful(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))

            case Right(model) =>
              employmentSessionService.submitAndClear(taxYear, employmentId, model, cya, prior, Some(auditAndNRS)).flatMap {
                case Left(result) => Future.successful(result)
                case Right((returnedEmploymentId, cya)) => getResultFromResponse(returnedEmploymentId, taxYear, model, cya.hasPriorBenefits, employmentId, cya)
              }
          }

        case _ =>
          logger.info("[CheckEmploymentDetailsController][submit] User has no cya data in order to submit. Redirecting to cya page show method.")
          Future.successful(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
      }
    }
  }
  //scalastyle:on

  private def auditAndNRS(employmentId: String, taxYear: Int, model: CreateUpdateEmploymentRequest,
                          prior: Option[AllEmploymentData], request: AuthorisationRequest[_]): Unit = {
    implicit val implicitRequest: AuthorisationRequest[_] = request

    checkEmploymentDetailsService.performSubmitAudits(request.user, model, employmentId, taxYear, prior)
    if (appConf.nrsEnabled) {
      checkEmploymentDetailsService.performSubmitNrsPayload(request.user, model, employmentId, prior)
    }
  }

  private def getResultFromResponse(returnedEmploymentId: Option[String],
                                    taxYear: Int,
                                    model: CreateUpdateEmploymentRequest,
                                    hasPriorBenefits: Boolean,
                                    employmentId: String,
                                    cya: EmploymentUserData)(implicit request: AuthorisationRequest[_]): Future[Result] = {
    val log = "[CheckEmploymentDetailsController][getResultFromResponse]"
    returnedEmploymentId match {
      case Some(employmentId) =>
        if (hasPriorBenefits || model.hmrcEmploymentIdToIgnore.isDefined) {
          logger.info(s"$log An employment id was created but was based on an existing employment. Returning to employer information page.")
          Future.successful(Redirect(EmployerInformationController.show(taxYear, employmentId)))
        } else {
          val result = Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
            .removingFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID)
            .addingToSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId)

          logger.info(s"$log User has created a new employment. Continuing to benefits section.")
          if (appConf.mimicEmploymentAPICalls) {
            logger.info(s"$log [mimicEmploymentAPICalls] Saving expected CYA data.")
            employmentSessionService.createOrUpdateSessionData(
              request.user, taxYear, employmentId, cya.employment, isPriorSubmission = true, hasPriorBenefits = false, hasPriorStudentLoans = false
            )(errorHandler.internalServerError())(result)
          } else {
            Future.successful(result)
          }
        }
      case None =>
        logger.info(s"$log User made an update to an existing employment. Returning to employer information page.")
        Future.successful(Redirect(EmployerInformationController.show(taxYear, employmentId)))
    }
  }

  private def saveCYAAndReturnEndOfYearResult(taxYear: Int, employmentId: String, employmentData: AllEmploymentData)
                                             (implicit request: AuthorisationRequest[AnyContent]): Future[Result] = {
    employmentData.eoyEmploymentSourceWith(employmentId) match {
      case Some(EmploymentSourceOrigin(source, isUsingCustomerData)) => employmentSessionService.createOrUpdateSessionData(
        request.user,
        taxYear,
        employmentId,
        EmploymentCYAModel.apply(source, isUsingCustomerData),
        isPriorSubmission = true,
        source.hasPriorBenefits,
        source.hasPriorStudentLoans
      )(errorHandler.internalServerError())({
        val viewModel = source.toEmploymentDetailsViewModel(isUsingCustomerData)
        checkEmploymentDetailsService.sendViewEmploymentDetailsAudit(request.user, viewModel, taxYear)
        Ok(pageView(viewModel, taxYear, isInYear = false))
      })
      case None =>
        logger.info(s"[CheckEmploymentDetailsController][saveCYAAndReturnEndOfYearResult] No prior employment data exists with employmentId." +
          s"Redirecting to overview page. SessionId: ${request.user.sessionId}")
        Future(Redirect(appConf.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }
}
