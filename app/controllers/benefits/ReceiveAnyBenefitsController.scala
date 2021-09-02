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

package controllers.benefits

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.{CheckEmploymentDetailsController, CheckYourBenefitsController}
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.employment.BenefitsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.ReceiveAnyBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReceiveAnyBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             inYearAction: InYearAction,
                                             receiveAnyBenefitsView: ReceiveAnyBenefitsView,
                                             appConfig: AppConfig,
                                             employmentSessionService: EmploymentSessionService,
                                             errorHandler: ErrorHandler,
                                             ec: ExecutionContext,
                                             clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"receiveAnyBenefits.errors.noRadioSelected.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).map {
        case Some(cya) =>
          val form  = cya.employment.employmentBenefits.map(_.isBenefitsReceived.fold(yesNoForm)(received => yesNoForm.fill(received))).getOrElse(yesNoForm)
          Ok(receiveAnyBenefitsView(form, taxYear, employmentId))
        case None => Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).flatMap {
        case Some(cya) =>
          yesNoForm.bindFromRequest().fold({
            formWithErrors => Future.successful(BadRequest(receiveAnyBenefitsView(formWithErrors, taxYear, employmentId)))
          }, { yesNo =>
            if (yesNo) {
              val newBenefits = cya.employment.employmentBenefits.map(_.copy(isBenefitsReceived = Some(true)))
              val newCya = cya.employment.copy(employmentBenefits = newBenefits)
              employmentSessionService.createOrUpdateSessionData(employmentId, newCya, taxYear,cya.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(CheckYourBenefitsController.show(taxYear, employmentId)) //TODO Redirect To Next Page
              }
            }
            else {
              val newBenefits = BenefitsViewModel.clear(cya.employment.employmentBenefits.map(_.isUsingCustomerData).getOrElse(true))
              val newCya = cya.employment.copy(employmentBenefits = Some(newBenefits))
              employmentSessionService.createOrUpdateSessionData(employmentId, newCya, taxYear,cya.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
              }
            }
          })
        case None => Future.successful(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
      }
    }
  }
}
