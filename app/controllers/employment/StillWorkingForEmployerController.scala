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
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import javax.inject.Inject
import models.User
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.StillWorkingForEmployerView

import scala.concurrent.{ExecutionContext, Future}

class StillWorkingForEmployerController @Inject()(authorisedAction: AuthorisedAction,
                                                  val mcc: MessagesControllerComponents,
                                                  implicit val appConfig: AppConfig,
                                                  stillWorkingForEmployerView: StillWorkingForEmployerView,
                                                  inYearAction: InYearAction,
                                                  errorHandler: ErrorHandler,
                                                  employmentSessionService: EmploymentSessionService,
                                                  implicit val clock: Clock,
                                                  implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"employment.stillWorkingForEmployer.error.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        data.employment.employmentDetails.cessationDateQuestion match {
          case Some(isStillWorkingForEmployer) =>
            Future.successful(Ok(stillWorkingForEmployerView(yesNoForm.fill(isStillWorkingForEmployer), taxYear,
              employmentId, data.employment.employmentDetails.employerName)))
          case None =>
            if (data.isPriorSubmission) {
              val isStillWorkingForEmployer = data.employment.employmentDetails.cessationDate.isEmpty
              Future.successful(Ok(stillWorkingForEmployerView(yesNoForm.fill(isStillWorkingForEmployer), taxYear,
                employmentId, data.employment.employmentDetails.employerName)))
            } else {
              Future.successful(Ok(stillWorkingForEmployerView(yesNoForm, taxYear, employmentId, data.employment.employmentDetails.employerName)))
            }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        yesNoForm.bindFromRequest().fold(
          { formWithErrors =>
            Future.successful(BadRequest(stillWorkingForEmployerView(formWithErrors, taxYear, employmentId,
              data.employment.employmentDetails.employerName)))
          },
          { yesNo =>
            val cya = data.employment
            val cessationDateUpdated = if (yesNo) {
              None
            } else {
              cya.employmentDetails.cessationDate
            }
            val updatedCya = cya.copy(cya.employmentDetails.copy(cessationDateQuestion = Some(yesNo), cessationDate = cessationDateUpdated))
            employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear,
              data.isPriorSubmission,data.hasPriorBenefits)(errorHandler.internalServerError()) {
              employmentDetailsRedirect(updatedCya, taxYear, employmentId, data.isPriorSubmission, isStandaloneQuestion = false)
            }
          }
        )
      }
    }
  }
}
