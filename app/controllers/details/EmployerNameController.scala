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

package controllers.details

import actions.AuthorisedAction
import common.SessionValues
import config.{AppConfig, ErrorHandler}
import controllers.details.routes.PayeRefController
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.details.EmployerNameForm
import models.AuthorisationRequest
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.details.EmployerNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerNameController @Inject()(authorisedAction: AuthorisedAction,
                                       pageView: EmployerNameView,
                                       inYearAction: InYearUtil,
                                       errorHandler: ErrorHandler,
                                       employmentSessionService: EmploymentSessionService)
                                       (implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID).fold(
        handleSuccessfulGet(taxYear, employmentId).map(_.addingToSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))
      )(_ => handleSuccessfulGet(taxYear, employmentId))
    }
  }

  private def handleSuccessfulGet(taxYear: Int, employmentId: String)(implicit request: AuthorisationRequest[_]): Future[Result] = {
    employmentSessionService.getSessionDataResult(taxYear, employmentId) {
      case Some(data) =>
        val employerName = data.employment.employmentDetails.employerName
        val prefilledForm: Form[String] = EmployerNameForm.employerNameForm(request.user.isAgent).fill(employerName)
        Future.successful(Ok(pageView(prefilledForm, taxYear, employmentId)))
      case None =>
        val form: Form[String] = EmployerNameForm.employerNameForm(request.user.isAgent)
        Future.successful(Ok(pageView(form, taxYear, employmentId)))
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val form: Form[String] = EmployerNameForm.employerNameForm(request.user.isAgent)
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear, employmentId))),
        submittedName => handleSuccessForm(taxYear, employmentId, submittedName)
      )
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employerName: String)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    employmentSessionService.getSessionDataResult(taxYear, employmentId) {
      case Some(data: EmploymentUserData) =>
        val cya = data.employment
        val updatedCya = cya.copy(cya.employmentDetails.copy(employerName = employerName))
        employmentSessionService.createOrUpdateSessionData(
          user = request.user,
          taxYear = taxYear,
          employmentId = employmentId,
          cyaModel = updatedCya,
          isPriorSubmission = data.isPriorSubmission,
          hasPriorBenefits = data.hasPriorBenefits,
          hasPriorStudentLoans = data.hasPriorStudentLoans
        )(errorHandler.internalServerError())(Redirect(getRedirectCall(cya.employmentDetails, taxYear, employmentId)))
      case None =>
        val newCya = EmploymentCYAModel(EmploymentDetails(employerName = employerName, currentDataIsHmrcHeld = false))
        employmentSessionService.createOrUpdateSessionData(
          user = request.user,
          taxYear = taxYear,
          employmentId = employmentId,
          cyaModel = newCya,
          isPriorSubmission = false,
          hasPriorBenefits = false,
          hasPriorStudentLoans = false
        )(errorHandler.internalServerError())(Redirect(getRedirectCall(newCya.employmentDetails, taxYear, employmentId)))
    }
  }

  private def getRedirectCall(employmentDetails: EmploymentDetails,
                              taxYear: Int,
                              employmentId: String): Call = {
    if (employmentDetails.isFinished) CheckEmploymentDetailsController.show(taxYear, employmentId) else PayeRefController.show(taxYear, employmentId)
  }
}
