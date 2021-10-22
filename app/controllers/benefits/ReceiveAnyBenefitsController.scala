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
import controllers.employment.routes.CheckYourBenefitsController
import controllers.benefits.routes.CarVanFuelBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.employment.BenefitsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{EmploymentSessionService, RedirectService}
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
      employmentSessionService.getSessionDataResult(taxYear, employmentId){
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
      employmentSessionService.getSessionDataResult(taxYear, employmentId){
        case Some(cya) =>
          yesNoForm.bindFromRequest().fold({
            formWithErrors => Future.successful(BadRequest(receiveAnyBenefitsView(formWithErrors, taxYear, employmentId)))
          }, { yesNo =>
            if (yesNo) {
              val newBenefits = cya.employment.employmentBenefits match {
                case Some(benefits) => benefits.copy(isBenefitsReceived = true)
                case None => BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true)
              }
              val newCya = cya.employment.copy(employmentBenefits = Some(newBenefits))
              employmentSessionService.createOrUpdateSessionData(employmentId, newCya, taxYear,
                cya.isPriorSubmission,cya.hasPriorBenefits)(errorHandler.internalServerError()) {

                //if prior & finished -> CYA
                //if prior & not finished -> next page
                //if new & finished & seen final CYA -> CYA
                //if new & not finished -> next page
                //if new & not seen final cya -> next page

                RedirectService.benefitsSubmitRedirect(
                  cya.hasPriorBenefits,newCya,CarVanFuelBenefitsController.show(taxYear, employmentId)
                )(taxYear,employmentId)

              }
            } else {
              val customerData = cya.employment.employmentBenefits.map(_.isUsingCustomerData).getOrElse(true)
              val newBenefits = BenefitsViewModel.clear(customerData)
              val newCya = cya.employment.copy(employmentBenefits = Some(newBenefits))
              employmentSessionService.createOrUpdateSessionData(employmentId, newCya, taxYear,cya.isPriorSubmission,
                cya.hasPriorBenefits)(errorHandler.internalServerError()) {
               Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
              }
            }
          })
        case None => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }
}
