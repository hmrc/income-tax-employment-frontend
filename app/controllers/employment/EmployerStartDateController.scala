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
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.{CheckEmploymentDetailsController, EmployerPayrollIdController}
import forms.employment.EmploymentDateForm
import forms.employment.EmploymentDateForm.employmentStartDateForm
import models.AuthorisationRequest
import models.employment.EmploymentDate
import models.mongo.{EmploymentDetails, EmploymentUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.EmploymentSessionService
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.DateTimeUtil.localDateTimeFormat
import utils.{InYearUtil, SessionHelper}
import views.html.employment.EmployerStartDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerStartDateController @Inject()(authorisedAction: AuthorisedAction,
                                            val mcc: MessagesControllerComponents,
                                            implicit val appConfig: AppConfig,
                                            employerStartDateView: EmployerStartDateView,
                                            inYearAction: InYearUtil,
                                            errorHandler: ErrorHandler,
                                            employmentSessionService: EmploymentSessionService,
                                            employmentService: EmploymentService,
                                            implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        data.employment.employmentDetails.startDate match {
          case Some(startDate) =>
            val parsedDate: LocalDate = LocalDate.parse(startDate, localDateTimeFormat)
            val filledForm: Form[EmploymentDate] = employmentStartDateForm.fill(
              EmploymentDate(parsedDate.getDayOfMonth.toString, parsedDate.getMonthValue.toString, parsedDate.getYear.toString))
            Future.successful(Ok(employerStartDateView(filledForm, taxYear, employmentId, data.employment.employmentDetails.employerName)))
          case None =>
            Future.successful(Ok(employerStartDateView(employmentStartDateForm, taxYear, employmentId, data.employment.employmentDetails.employerName)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        val newForm = employmentStartDateForm.bindFromRequest()
        newForm.copy(errors = EmploymentDateForm.verifyStartDate(newForm.get, taxYear, request.user.isAgent, EmploymentDateForm.startDate)).fold(
          formWithErrors =>
            Future.successful(BadRequest(employerStartDateView(formWithErrors, taxYear, employmentId, data.employment.employmentDetails.employerName))),
          submittedDate => handleSuccessForm(taxYear, employmentId, data, submittedDate)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, startedDate: EmploymentDate)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    employmentService.updateStartDate(request.user, taxYear, employmentId, employmentUserData, startedDate).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => Redirect(getRedirectCall(employmentUserData.employment.employmentDetails, taxYear, employmentId))
    }
  }

  private def getRedirectCall(employmentDetails: EmploymentDetails,
                              taxYear: Int,
                              employmentId: String): Call = {
    if (employmentDetails.isFinished) {
      CheckEmploymentDetailsController.show(taxYear, employmentId)
    } else {
      EmployerPayrollIdController.show(taxYear, employmentId)
    }
  }
}
