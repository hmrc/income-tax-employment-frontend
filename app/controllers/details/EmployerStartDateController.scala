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

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.details.routes.DidYouLeaveEmployerController
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.details.DateForm.dateForm
import forms.details.EmploymentDetailsFormsProvider
import models.AuthorisationRequest
import models.benefits.pages.{EmployerStartDatePage => PageModel}
import models.details.EmploymentDetails
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.EmploymentSessionService
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.details.EmployerStartDateView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerStartDateController @Inject()(authorisedAction: AuthorisedAction,
                                            pageView: EmployerStartDateView,
                                            formsProvider: EmploymentDetailsFormsProvider,
                                            inYearAction: InYearUtil,
                                            errorHandler: ErrorHandler,
                                            employmentSessionService: EmploymentSessionService,
                                            employmentService: EmploymentService,
                                            mcc: MessagesControllerComponents)
                                           (implicit val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        Future.successful(Ok(pageView(PageModel(taxYear, employmentId, request.user, dateForm(), data))))
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        val endDate = data.employment.employmentDetails.cessationDate.map(LocalDate.parse)
        val employerName = data.employment.employmentDetails.employerName
        val simpleDateForm = dateForm().bindFromRequest()

        formsProvider.validatedStartDateForm(simpleDateForm, taxYear, request.user.isAgent, employerName, endDate).fold(
          formWithErrors => Future.successful(BadRequest(pageView(PageModel(taxYear, employmentId, request.user, formWithErrors, data)))),
          submittedData => handleSuccessForm(taxYear, employmentId, data, submittedData.toLocalDate.get)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, startedDate: LocalDate)
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
      DidYouLeaveEmployerController.show(taxYear, employmentId)
    }
  }
}
