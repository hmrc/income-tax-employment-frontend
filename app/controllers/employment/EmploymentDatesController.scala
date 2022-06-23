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
import forms.employment.EmploymentDatesForm
import models.employment.{EmploymentDate, EmploymentDates}
import models.mongo.EmploymentDetails
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.DateTimeUtil.localDateTimeFormat
import utils.{InYearUtil, SessionHelper}
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
                                          implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {


  def datesForm: Form[EmploymentDates] = EmploymentDatesForm.employmentDatesForm

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        (data.employment.employmentDetails.startDate, data.employment.employmentDetails.cessationDate) match {
          case (startDate, endDate) =>
            val parsedStartDate: Option[LocalDate] = startDate.map(LocalDate.parse(_, localDateTimeFormat))
            val parsedEndDate: Option[LocalDate] = endDate.map(LocalDate.parse(_, localDateTimeFormat))
            val filledForm: Form[EmploymentDates] = datesForm.fill(
              EmploymentDates(
                parsedStartDate.map(localDate => EmploymentDate(localDate.getDayOfMonth.toString,
                  localDate.getMonthValue.toString, localDate.getYear.toString)),
                parsedEndDate.map(localDate => EmploymentDate(localDate.getDayOfMonth.toString, localDate.getMonthValue.toString, localDate.getYear.toString))))
            Future.successful(Ok(employmentDatesView(filledForm, taxYear, employmentId, data.employment.employmentDetails.employerName)))
          case _ =>
            Future.successful(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        val newForm = datesForm.bindFromRequest()
        newForm.copy(errors = EmploymentDatesForm.verifyDates(newForm.get, taxYear, request.user.isAgent)).fold(
          { formWithErrors =>
            Future.successful(BadRequest(employmentDatesView(formWithErrors, taxYear, employmentId, data.employment.employmentDetails.employerName)))
          },
          { submittedDate =>
            val cya = data.employment
            val updatedCya = cya.copy(cya.employmentDetails.copy(
              startDate = submittedDate.startDateToLocalDate.map(_.toString),
              cessationDate = submittedDate.endDateToLocalDate.map(_.toString))
            )

            employmentSessionService.createOrUpdateSessionData(
              request.user,
              taxYear,
              employmentId,
              updatedCya,
              data.isPriorSubmission,
              data.hasPriorBenefits,
              data.hasPriorStudentLoans
            )(errorHandler.internalServerError())(Redirect(getRedirectCall(updatedCya.employmentDetails, taxYear, employmentId)))
          }
        )
      }
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
