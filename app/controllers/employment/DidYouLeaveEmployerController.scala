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
import views.html.employment.DidYouLeaveEmployerView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DidYouLeaveEmployerController @Inject()(authorisedAction: AuthorisedAction,
                                              val mcc: MessagesControllerComponents,
                                              implicit val appConfig: AppConfig,
                                              didYouLeaveView: DidYouLeaveEmployerView,
                                              inYearAction: InYearUtil,
                                              errorHandler: ErrorHandler,
                                              employmentSessionService: EmploymentSessionService,
                                              employmentService: EmploymentService,
                                              implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        val employerName = data.employment.employmentDetails.employerName
        data.employment.employmentDetails.didYouLeaveQuestion match {
          case Some(didYouLeaveEmployer) =>
            Future.successful(Ok(didYouLeaveView(yesNoForm(request.user.isAgent, employerName).fill(didYouLeaveEmployer), taxYear,
              employmentId, employerName)))
          case None =>
            if (data.isPriorSubmission) {
              val didYouLeaveEmployer = data.employment.employmentDetails.cessationDate.isEmpty
              Future.successful(Ok(didYouLeaveView(yesNoForm(request.user.isAgent, employerName).fill(didYouLeaveEmployer), taxYear,
                employmentId, employerName)))
            } else {
              Future.successful(Ok(didYouLeaveView(yesNoForm(request.user.isAgent, employerName),
                taxYear, employmentId, employerName)))
            }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        val employerName = data.employment.employmentDetails.employerName
        yesNoForm(request.user.isAgent, employerName).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(didYouLeaveView(formWithErrors, taxYear, employmentId, employerName))),
          yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    employmentService.updateDidYouLeaveQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        employmentDetailsRedirect(employmentUserData.employment, taxYear, employmentId, employmentUserData.isPriorSubmission, isStandaloneQuestion = false)
    }
  }

  private def yesNoForm(isAgent: Boolean, employerName: String): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"employment.didYouLeave.error.${if (isAgent) "agent" else "individual"}", Seq(employerName)
  )
}
