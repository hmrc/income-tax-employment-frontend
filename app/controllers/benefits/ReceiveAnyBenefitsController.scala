/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.benefits

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.fuel.routes.CarVanFuelBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.benefits.BenefitsFormsProvider
import models.AuthorisationRequest
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.benefits.BenefitsService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.ReceiveAnyBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReceiveAnyBenefitsController @Inject()(authAction: AuthorisedAction,
                                             inYearAction: InYearUtil,
                                             receiveAnyBenefitsView: ReceiveAnyBenefitsView,
                                             employmentSessionService: EmploymentSessionService,
                                             benefitsService: BenefitsService,
                                             redirectService: RedirectService,
                                             benefitsFormsProvider: BenefitsFormsProvider,
                                             errorHandler: ErrorHandler)
                                            (implicit val cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
        case Right(Some(cya: EmploymentUserData)) => cya.employment.employmentBenefits match {
          case Some(model) =>
            Future.successful(Ok(receiveAnyBenefitsView(benefitsFormsProvider.receiveAnyBenefitsForm(
              request.user.isAgent).fill(model.isBenefitsReceived), taxYear, employmentId)))
          case None => Future.successful(Ok(receiveAnyBenefitsView(benefitsFormsProvider.receiveAnyBenefitsForm(request.user.isAgent), taxYear, employmentId)))
        }
        case Right(None) => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
        case Right(Some(cya)) =>
          benefitsFormsProvider.receiveAnyBenefitsForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(receiveAnyBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, cya, yesNo)
          )
        case Right(None) => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    benefitsService.updateIsBenefitsReceived(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => if (questionValue) {
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, CarVanFuelBenefitsController.show(taxYear, employmentId))(taxYear, employmentId)
      } else {
        Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
      }
    }
  }
}
