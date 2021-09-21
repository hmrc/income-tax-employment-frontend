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

import java.time.LocalDate

import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.employment.EmploymentDateForm
import javax.inject.Inject
import models.employment.EmploymentDate
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.DateTimeUtil.localDateTimeFormat
import utils.{Clock, SessionHelper}
import views.html.employment.EmployerStartDateView

import scala.concurrent.{ExecutionContext, Future}

class EmployerStartDateController @Inject()(authorisedAction: AuthorisedAction,
                                            val mcc: MessagesControllerComponents,
                                            implicit val appConfig: AppConfig,
                                            employerStartDateView: EmployerStartDateView,
                                            inYearAction: InYearAction,
                                            errorHandler: ErrorHandler,
                                            employmentSessionService: EmploymentSessionService,
                                            implicit val clock: Clock,
                                            implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {


  def form: Form[EmploymentDate] = EmploymentDateForm.employmentStartDateForm

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        data.employment.employmentDetails.startDate match {
          case Some(startDate) =>
            val parsedDate: LocalDate = LocalDate.parse(startDate, localDateTimeFormat)
            val filledForm: Form[EmploymentDate] = form.fill(
              EmploymentDate(parsedDate.getDayOfMonth.toString,parsedDate.getMonthValue.toString, parsedDate.getYear.toString))
            Future.successful(Ok(employerStartDateView(filledForm, taxYear, employmentId, data.employment.employmentDetails.employerName)))
          case None =>
            Future.successful(Ok(employerStartDateView(form, taxYear, employmentId, data.employment.employmentDetails.employerName)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        val newForm = form.bindFromRequest()
          newForm.copy(errors = EmploymentDateForm.verifyNewDate(newForm.get, taxYear, user.isAgent, EmploymentDateForm.startDate)).fold(
          { formWithErrors =>
            Future.successful(BadRequest(employerStartDateView(formWithErrors, taxYear, employmentId, data.employment.employmentDetails.employerName)))
          },
          { submittedDate =>
            val cya = data.employment
            val updatedCya = cya.copy(cya.employmentDetails.copy(startDate = Some(submittedDate.toLocalDate.toString)))
            employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              employmentDetailsRedirect(updatedCya,taxYear,employmentId,data.isPriorSubmission)
            }
          }
        )
      }
    }
  }
}
