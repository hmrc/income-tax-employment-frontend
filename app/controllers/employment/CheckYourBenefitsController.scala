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

import config.{AppConfig, ErrorHandler}
import controllers.benefits.routes.ReceiveAnyBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.User
import models.benefits.{Benefits, BenefitsViewModel}
import models.employment.AllEmploymentData
import models.mongo.EmploymentCYAModel
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.employment.CheckYourBenefitsService
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
                                            checkYourBenefitsService: CheckYourBenefitsService,
                                            inYearAction: InYearAction,
                                            errorHandler: ErrorHandler,
                                            implicit val clock: Clock,
                                            implicit val ec: ExecutionContext
                                           ) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  //scalastyle:off
  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    if (inYearAction.inYear(taxYear)) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { result: AllEmploymentData =>
        val redirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        employmentSessionService.employmentSourceToUse(result, employmentId, inYearAction.inYear(taxYear)) match {
          case Some((source, isUsingCustomerData)) =>
            val benefits = source.employmentBenefits.flatMap(_.benefits)
            benefits match {
              case None => redirect
              case Some(benefits) =>
                val singleEmployment = checkYourBenefitsService.isSingleEmploymentAndAudit(benefits, taxYear, inYearAction.inYear(taxYear), allEmploymentData = result)
                if (inYearAction.inYear(taxYear)) {
                  Ok(checkYourBenefitsView(taxYear, benefits.toBenefitsViewModel(isUsingCustomerData), singleEmployment, employmentId))
                } else {
                  Ok(checkYourBenefitsViewEOY(taxYear, benefits.toBenefitsViewModel(isUsingCustomerData), employmentId, isUsingCustomerData))
                }
            }
          case None => redirect
        }
      }
    } else {
      employmentSessionService.getAndHandle(taxYear, employmentId, redirectWhenNoPrior = true) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val benefits: Option[BenefitsViewModel] = cya.employment.employmentBenefits
            benefits match {
              case None => Future(Redirect(ReceiveAnyBenefitsController.show(taxYear, employmentId)))
              case Some(benefits) => Future {
                val isSingleEmploymentValue = checkYourBenefitsService.isSingleEmploymentAndAudit(benefits.toBenefits, taxYear, inYearAction.inYear(taxYear),
                  allEmploymentData = prior.get)
                if (inYearAction.inYear(taxYear)) {
                  Ok(checkYourBenefitsView(taxYear, benefits.toBenefits.toBenefitsViewModel(benefits.isUsingCustomerData,
                    cyaBenefits = Some(benefits)), isSingleEmploymentValue, employmentId))
                } else {
                  Ok(checkYourBenefitsViewEOY(taxYear, benefits.toBenefits.toBenefitsViewModel(benefits.isUsingCustomerData,
                    cyaBenefits = Some(benefits)), employmentId, benefits.isUsingCustomerData))
                }
              }
            }
          case None => saveCYAAndReturnEndOfYearResult(taxYear, employmentId, prior.get)
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
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

          case None => Future.successful(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def saveCYAAndReturnEndOfYearResult(taxYear: Int, employmentId: String, allEmploymentData: AllEmploymentData)
                                     (implicit user: User[AnyContent]): Future[Result] = {
    employmentSessionService.employmentSourceToUse(allEmploymentData, employmentId, inYearAction.inYear(taxYear)) match {
      case Some((source, isUsingCustomerData)) =>
        employmentSessionService.createOrUpdateSessionData(employmentId, EmploymentCYAModel(source, isUsingCustomerData),
          taxYear, isPriorSubmission = true, source.hasPriorBenefits
        )(errorHandler.internalServerError()) {
          val benefits: Option[Benefits] = source.employmentBenefits.flatMap(_.benefits)
          benefits match {
            case None => Redirect(ReceiveAnyBenefitsController.show(taxYear, employmentId))
            case Some(benefits) =>
              val isSingleEmploymentValue = checkYourBenefitsService.isSingleEmploymentAndAudit(benefits, taxYear, inYearAction.inYear(taxYear), allEmploymentData = allEmploymentData)

              if (inYearAction.inYear(taxYear)) {
                Ok(checkYourBenefitsView(taxYear, benefits.toBenefitsViewModel(isUsingCustomerData), isSingleEmploymentValue, employmentId))
              } else {
                Ok(checkYourBenefitsViewEOY(taxYear, benefits.toBenefitsViewModel(isUsingCustomerData), employmentId, isUsingCustomerData))
              }
          }
        }
      case None =>
        logger.info(s"[CheckYourBenefitsController][saveCYAAndReturnEndOfYearResult] No prior employment data exists with employmentId." +
          s"Redirecting to overview page. SessionId: ${user.sessionId}")
        Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }
}
