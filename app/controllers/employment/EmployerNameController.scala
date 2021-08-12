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

import common.SessionValues
import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.EmployerNameView
import forms.employment.EmployerNameForm
import models.mongo.{EmploymentCYAModel, EmploymentDetails}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerNameController @Inject()(authorisedAction: AuthorisedAction,
                                       val mcc: MessagesControllerComponents,
                                       implicit val appConfig: AppConfig,
                                       employerNameView: EmployerNameView,
                                       inYearAction: InYearAction,
                                       errorHandler: ErrorHandler,
                                       employmentSessionService: EmploymentSessionService,
                                       implicit val clock: Clock,
                                       implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).map {
        case Some(data) =>
          val employerName = data.employment.employmentDetails.employerName
          val prefilledForm: Form[String] = EmployerNameForm.employerNameForm(user.isAgent).fill(employerName)
          Ok(employerNameView(prefilledForm, taxYear, employmentId))
        case None =>
          val form: Form[String] = EmployerNameForm.employerNameForm(user.isAgent)
          Ok(employerNameView(form, taxYear, employmentId))
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {
      val form: Form[String] = EmployerNameForm.employerNameForm(user.isAgent)

      form.bindFromRequest().fold(
        { formWithErrors =>
          Future.successful(BadRequest(employerNameView(formWithErrors, taxYear, employmentId)))
        },
        { submittedName =>
          employmentSessionService.getSessionData(taxYear, employmentId).flatMap {
            case Some(data) =>
              val cya = data.employment
              val updatedCya = cya.copy(cya.employmentDetails.copy(employerName = submittedName))
              employmentSessionService.createOrUpdateSessionData(
                employmentId, updatedCya, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                if(data.isPriorSubmission){
                  Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId))
                }
                else {
                  Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)) //TODO direct to next question page during wireup
                }
              }
            case None =>
              val newCya = EmploymentCYAModel(EmploymentDetails(employerName = submittedName, currentDataIsHmrcHeld = false))
              employmentSessionService.createOrUpdateSessionData(employmentId, newCya, taxYear, false)(errorHandler.internalServerError()) {
                Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
                  .removingFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID) //TODO direct to next question page during wireup
              }
          }
        }
      )
    }
  }
}
