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
import forms.YesNoForm
import models.AuthorisationRequest
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.employment.StillWorkingForEmployerView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StillWorkingForEmployerController @Inject()(authorisedAction: AuthorisedAction,
                                                  val mcc: MessagesControllerComponents,
                                                  implicit val appConfig: AppConfig,
                                                  stillWorkingForEmployerView: StillWorkingForEmployerView,
                                                  inYearAction: InYearUtil,
                                                  errorHandler: ErrorHandler,
                                                  employmentSessionService: EmploymentSessionService,
                                                  employmentService: EmploymentService,
                                                  implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        data.employment.employmentDetails.cessationDateQuestion match {
          case Some(isStillWorkingForEmployer) =>
            Future.successful(Ok(stillWorkingForEmployerView(yesNoForm(request.user.isAgent).fill(isStillWorkingForEmployer), taxYear,
              employmentId, data.employment.employmentDetails.employerName)))
          case None =>
            if (data.isPriorSubmission) {
              val isStillWorkingForEmployer = data.employment.employmentDetails.cessationDate.isEmpty
              Future.successful(Ok(stillWorkingForEmployerView(yesNoForm(request.user.isAgent).fill(isStillWorkingForEmployer), taxYear,
                employmentId, data.employment.employmentDetails.employerName)))
            } else {
              Future.successful(Ok(stillWorkingForEmployerView(yesNoForm(request.user.isAgent),
                taxYear, employmentId, data.employment.employmentDetails.employerName)))
            }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        yesNoForm(request.user.isAgent).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(stillWorkingForEmployerView(formWithErrors, taxYear, employmentId,
            data.employment.employmentDetails.employerName))),
          yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    employmentService.updateCessationDateQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        employmentDetailsRedirect(employmentUserData.employment, taxYear, employmentId, employmentUserData.isPriorSubmission, isStandaloneQuestion = false)
    }
  }

  private def yesNoForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"employment.stillWorkingForEmployer.error.${if (isAgent) "agent" else "individual"}"
  )
}
