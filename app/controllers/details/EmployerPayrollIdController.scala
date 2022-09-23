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
import config.{AppConfig, ErrorHandler}
import controllers.details.routes.EmployerPayAmountController
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.details.EmployerPayrollIdForm
import models.AuthorisationRequest
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.EmploymentSessionService
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.details.EmployerPayrollIdView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerPayrollIdController @Inject()(authorisedAction: AuthorisedAction,
                                            pageView: EmployerPayrollIdView,
                                            inYearAction: InYearUtil,
                                            errorHandler: ErrorHandler,
                                            employmentSessionService: EmploymentSessionService,
                                            employmentService: EmploymentService)
                                           (implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    val emptyForm = EmployerPayrollIdForm.employerPayrollIdForm(request.user.isAgent)

    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataOld(taxYear, employmentId).map {
        case Right(Some(cya)) =>
          cya.employment.employmentDetails.payrollId match {
            case Some(payrollId) =>
              val filledForm = emptyForm.fill(payrollId)
              Ok(pageView(filledForm, taxYear, employmentId))
            case None =>
              Ok(pageView(emptyForm, taxYear, employmentId))
          }
        case _ => Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId))
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckEmploymentDetailsController.show(taxYear, employmentId).url
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { data =>
        EmployerPayrollIdForm.employerPayrollIdForm(request.user.isAgent).bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(pageView(formWithErrors, taxYear, employmentId)))
          },
          submittedPayrollId => handleSuccessForm(taxYear, employmentId, data, submittedPayrollId)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, payrollId: String)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    employmentService.updatePayrollId(request.user, taxYear, employmentId, employmentUserData, payrollId).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => Redirect(getRedirectCall(employmentUserData, taxYear, employmentId))
    }
  }

  private def getRedirectCall(employmentUserData: EmploymentUserData,
                              taxYear: Int,
                              employmentId: String): Call = {
    if (employmentUserData.employment.employmentDetails.isFinished(employmentUserData.isPriorSubmission)) {
      CheckEmploymentDetailsController.show(taxYear, employmentId)
    } else {
      EmployerPayAmountController.show(taxYear, employmentId)
    }
  }
}
