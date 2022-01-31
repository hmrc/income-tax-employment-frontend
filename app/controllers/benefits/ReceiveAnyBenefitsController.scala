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

package controllers.benefits

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.fuel.routes.CarVanFuelBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.User
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.benefitsSubmitRedirect
import services.benefits.BenefitsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.benefits.ReceiveAnyBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReceiveAnyBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             inYearAction: InYearUtil,
                                             receiveAnyBenefitsView: ReceiveAnyBenefitsView,
                                             appConfig: AppConfig,
                                             employmentSessionService: EmploymentSessionService,
                                             benefitsService: BenefitsService,
                                             errorHandler: ErrorHandler,
                                             ec: ExecutionContext,
                                             clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) {
        case Some(cya) =>
          cya.employment.employmentBenefits match {
            case Some(model) => Future.successful(Ok(receiveAnyBenefitsView(yesNoForm.fill(model.isBenefitsReceived), taxYear, employmentId)))
            case None => Future.successful(Ok(receiveAnyBenefitsView(yesNoForm, taxYear, employmentId)))
          }
        case None => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) {
        case Some(cya) =>
          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(receiveAnyBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, cya, yesNo)
          )
        case None => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit user: User[_]): Future[Result] = {
    benefitsService.updateIsBenefitsReceived(taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => if (questionValue) {
        benefitsSubmitRedirect(employmentUserData.employment, CarVanFuelBenefitsController.show(taxYear, employmentId))(taxYear, employmentId)
      } else {
        Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
      }
    }
  }

  private def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"receiveAnyBenefits.errors.noRadioSelected.${if (user.isAgent) "agent" else "individual"}"
  )
}
