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
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.employment.EmploymentDatesForm
import models.employment.EmploymentDates
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.DateTimeUtil.localDateTimeFormat
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.employment.EmploymentDatesView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentDatesController @Inject()(authorisedAction: AuthorisedAction,
                                          val mcc: MessagesControllerComponents,
                                          implicit val appConfig: AppConfig,
                                          employmentDatesView: EmploymentDatesView,
                                          inYearAction: InYearUtil,
                                          errorHandler: ErrorHandler,
                                          employmentSessionService: EmploymentSessionService,
                                          implicit val clock: Clock,
                                          implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {


  def datesForm: Form[EmploymentDates] = EmploymentDatesForm.employmentDatesForm

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        (data.employment.employmentDetails.startDate, data.employment.employmentDetails.cessationDate)  match {
          case (Some(startDate), Some(endDate)) =>
            val parsedStartDate: LocalDate = LocalDate.parse(startDate, localDateTimeFormat)
            val parsedEndDate: LocalDate = LocalDate.parse(endDate, localDateTimeFormat)
            val filledForm: Form[EmploymentDates] = datesForm.fill(
              EmploymentDates(
                parsedStartDate.getDayOfMonth.toString,parsedStartDate.getMonthValue.toString, parsedStartDate.getYear.toString,
                parsedEndDate.getDayOfMonth.toString,parsedEndDate.getMonthValue.toString, parsedEndDate.getYear.toString))
            Future.successful(Ok(employmentDatesView(filledForm, taxYear, employmentId, data.employment.employmentDetails.employerName)))
          case _ =>
            Future.successful(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        val newForm = datesForm.bindFromRequest()
          newForm.copy(errors = EmploymentDatesForm.verifyDates(newForm.get, taxYear, user.isAgent)).fold(
          { formWithErrors =>
            Future.successful(BadRequest(employmentDatesView(formWithErrors, taxYear, employmentId, data.employment.employmentDetails.employerName)))
          },
          { submittedDate =>
            val cya = data.employment
            val leaveDate = cya.employmentDetails.cessationDate

            val updatedCya = cya.copy(cya.employmentDetails.copy(
              startDate = Some(submittedDate.startDateToLocalDate.toString),
              cessationDate = Some(submittedDate.endDateToLocalDate.toString))
            )

            employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear, data.isPriorSubmission,
              data.hasPriorBenefits)(errorHandler.internalServerError()) {
              employmentDetailsRedirect(updatedCya,taxYear,employmentId,data.isPriorSubmission,isStandaloneQuestion = false)
            }
          }
        )
      }
    }
  }
}
