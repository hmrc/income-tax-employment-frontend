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

package controllers.offPayrollWorking

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckEmploymentDetailsController
import controllers.offPayrollWorking.routes.EmployerOffPayrollWorkingWarningController
import forms.details.EmploymentDetailsFormsProvider
import models.UserSessionDataRequest
import models.employment.EmploymentDetailsType
import models.offPayrollWorking.{EmployerOffPayrollWorkingStatusPage => PageModel}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.offPayrollWorking.EmployerOffPayrollWorkingView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerOffPayrollWorkingController @Inject()(actionsProvider: ActionsProvider,
                                                    view: EmployerOffPayrollWorkingView,
                                                    formsProvider: EmploymentDetailsFormsProvider,
                                                    employmentService: EmploymentService,
                                                    errorHandler: ErrorHandler)
                                                   (implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentDetailsType
  ) { implicit request =>
    val form = formsProvider.offPayrollStatusForm(request.user.isAgent, request.employmentUserData.employment.employmentDetails.employerName)
    Ok(view(PageModel(taxYear, employmentId, request.user, form, request.employmentUserData)))
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentDetailsType
  ).async { implicit request =>
    formsProvider.offPayrollStatusForm(request.user.isAgent, request.employmentUserData.employment.employmentDetails.employerName).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(PageModel(taxYear, employmentId, request.user, formWithErrors, request.employmentUserData)))),
      yesNo => handleSuccessForm(taxYear, employmentId, yesNo)
    )
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, questionValue: Boolean)
                               (implicit request: UserSessionDataRequest[_]): Future[Result] = {
    questionValue match {
      case true =>
        employmentService.updateOffPayrollWorkingStatus(request.user, taxYear, employmentId, request.employmentUserData, questionValue).map {
          case Left(_) => errorHandler.internalServerError()
          case Right(employmentUserData) => Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId))
        }
      case false =>
        employmentService.updateOffPayrollWorkingStatus(request.user, taxYear, employmentId, request.employmentUserData, questionValue).map {
          case Left(_) => errorHandler.internalServerError()
          case Right(employmentUserData) => Redirect(EmployerOffPayrollWorkingWarningController.show(taxYear, employmentId))
        }
    }
  }

}
