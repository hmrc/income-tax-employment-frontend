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

package controllers.details

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import controllers.details.routes.EmployerPayrollIdController
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.details.DateForm.dateForm
import forms.details.EmploymentDetailsFormsProvider
import models.benefits.pages.{EmployerEndDatePage => PageModel}
import models.details.EmploymentDetails
import models.employment.EmploymentDetailsType
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.details.EmployerEndDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerEndDateController @Inject()(actionsProvider: ActionsProvider,
                                          pageView: EmployerEndDateView,
                                          formsProvider: EmploymentDetailsFormsProvider,
                                          employmentService: EmploymentService,
                                          errorHandler: ErrorHandler)
                                         (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentDetailsType
  ) { implicit request =>
    Ok(pageView(PageModel(taxYear, employmentId, request.user, dateForm(), request.employmentUserData)))
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentDetailsType
  ).async { implicit request =>
    val data = request.employmentUserData
    val simpleDateForm = dateForm().bindFromRequest()
    val startDate = LocalDate.parse(data.employment.employmentDetails.startDate.get)

    formsProvider.validatedEndDateForm(simpleDateForm, taxYear, request.user.isAgent, PageModel.pageNameKey, startDate).fold(
      formWithErrors => Future.successful(BadRequest(pageView(PageModel(taxYear, employmentId, request.user, formWithErrors, request.employmentUserData)))),
      submittedDate => employmentService.updateEndDate(request.user, taxYear, employmentId, data, submittedDate.toLocalDate.toString).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(employmentUserData) => Redirect(getRedirectCall(employmentUserData.employment.employmentDetails, taxYear, employmentId))
      }
    )
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
