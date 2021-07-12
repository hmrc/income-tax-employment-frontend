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

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckEmploymentDetailsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.OtherPaymentsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OtherPaymentsController @Inject()(implicit val cc: MessagesControllerComponents,
                                        authAction: AuthorisedAction,
                                        inYearAction: InYearAction,
                                        otherPaymentsView: OtherPaymentsView,
                                        appConfig: AppConfig,
                                        employmentSessionService: EmploymentSessionService,
                                        errorHandler: ErrorHandler,
                                        ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"employment.other-payments.errors.noRadioSelected.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).map {
        case Some(data) =>
          val preFilledForm = data.employment.employmentDetails.tipsAndOtherPaymentsQuestion.map(yesNoForm.fill(_)).getOrElse(yesNoForm)
          Ok(otherPaymentsView(preFilledForm, taxYear, employmentId))
        case None => Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId))
      }
    }
  }

  def submit(taxYear:Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).flatMap {
        case Some(data) =>
          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(otherPaymentsView(formWithErrors, taxYear, employmentId))),
            yesNo => {
              val cya = data.employment
              val updatedTipsAndOtherPaymentsAmount = if(yesNo) cya.employmentDetails.tipsAndOtherPayments else None
              val updatedCyaModel: EmploymentCYAModel =
                cya.copy(employmentDetails = cya.employmentDetails.copy(tipsAndOtherPaymentsQuestion = Some(yesNo), tipsAndOtherPayments = updatedTipsAndOtherPaymentsAmount))

              employmentSessionService.updateSessionData(employmentId, updatedCyaModel, taxYear, false, data.isPriorSubmission)(errorHandler.internalServerError()){
                // TODO replace this with actual amount page url, which is implemented in SASS-1029
                if(yesNo) { Redirect(Call("POST", "/other-payments-p60-amount-TO-BE-DEFINED")) }
                // TODO - This would need to change when we introduce the "new Employment Flow (E4)"
                else { Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)) }
              }

            }
          )
        case None => Future(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
      }
    }
  }

}
