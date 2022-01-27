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

import audit.{AuditService, ViewEmploymentBenefitsAudit}
import common.{EmploymentSection, SessionValues}
import config.{AppConfig, ErrorHandler}
import controllers.benefits.routes.ReceiveAnyBenefitsController
import controllers.employment.routes.{CheckYourBenefitsController, EmploymentSummaryController}
import controllers.expenses.routes.CheckEmploymentExpensesController
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.benefits.Benefits
import models.employment.{AllEmploymentData, EmploymentSourceOrigin}
import models.mongo.EmploymentCYAModel
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.{CheckYourBenefitsView, CheckYourBenefitsViewEOY}
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

class CheckYourBenefitsController @Inject()(authorisedAction: AuthorisedAction,
                                            val mcc: MessagesControllerComponents,
                                            implicit val appConfig: AppConfig,
                                            checkYourBenefitsView: CheckYourBenefitsView,
                                            checkYourBenefitsViewEOY: CheckYourBenefitsViewEOY,
                                            employmentSessionService: EmploymentSessionService,
                                            auditService: AuditService,
                                            inYearAction: InYearAction,
                                            errorHandler: ErrorHandler,
                                            implicit val clock: Clock,
                                            implicit val ec: ExecutionContext
                                           ) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  //scalastyle:off
  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    if (inYearAction.inYear(taxYear)) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { allEmploymentData: AllEmploymentData =>
        val redirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        allEmploymentData.inYearEmploymentSourceWith(employmentId) match {
          case Some(EmploymentSourceOrigin(source, isUsingCustomerData)) =>
            source.employmentBenefits.flatMap(_.benefits) match {
              case None => redirect
              case Some(benefits) =>
                val auditModel = ViewEmploymentBenefitsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, benefits)
                auditService.sendAudit[ViewEmploymentBenefitsAudit](auditModel.toAuditModel)
                Ok(checkYourBenefitsView(taxYear, benefits.toBenefitsViewModel(isUsingCustomerData), allEmploymentData.isLastInYearEmployment, employmentId))
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
                benefits.toBenefits.toBenefitsViewModel(benefits.isUsingCustomerData, cyaBenefits = Some(benefits)),
                employmentId,
                benefits.isUsingCustomerData
              )))
            }
          case None => prior.get.eoyEmploymentSourceWith(employmentId) match {
            case Some(EmploymentSourceOrigin(source, isUsingCustomerData)) =>
              employmentSessionService.createOrUpdateSessionData(employmentId, EmploymentCYAModel(source, isUsingCustomerData),
                taxYear, isPriorSubmission = true, source.hasPriorBenefits
              )(errorHandler.internalServerError()) {
                val benefits: Option[Benefits] = source.employmentBenefits.flatMap(_.benefits)
                benefits match {
                  case None => Redirect(ReceiveAnyBenefitsController.show(taxYear, employmentId))
                  case Some(benefits) =>
                    Ok(checkYourBenefitsViewEOY(taxYear, benefits.toBenefitsViewModel(isUsingCustomerData), employmentId, isUsingCustomerData))
                }
              }
            case None =>
              logger.info(s"[CheckYourBenefitsController][saveCYAAndReturnEndOfYearResult] No prior employment data exists with employmentId." +
                s"Redirecting to overview page. SessionId: ${user.sessionId}")
              Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            employmentSessionService.createModelAndReturnResult(cya, prior, taxYear, EmploymentSection.EMPLOYMENT_BENEFITS) {
              model =>
                employmentSessionService.createOrUpdateEmploymentResult(taxYear, model).flatMap {
                  case Left(result) => Future.successful(result)
                  case Right(result) =>
                    employmentSessionService.clear(taxYear, employmentId)(
                      if (cya.hasPriorBenefits) {
                        Redirect(EmploymentSummaryController.show(taxYear))
                      } else {
                        Redirect(CheckEmploymentExpensesController.show(taxYear)).addingToSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId)
                      }
                    )
                }
            }
          case None => Future.successful(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
        }
      }
    }
  }

}
