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
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.DateTimeUtil.localDateTimeFormat
import utils.{Clock, SessionHelper}
import views.html.employment.EmployerLeaveDateView

import scala.concurrent.{ExecutionContext, Future}

class EmployerLeaveDateController @Inject()(authorisedAction: AuthorisedAction,
                                            val mcc: MessagesControllerComponents,
                                            implicit val appConfig: AppConfig,
                                            employerLeaveDateView: EmployerLeaveDateView,
                                            inYearAction: InYearAction,
                                            errorHandler: ErrorHandler,
                                            employmentSessionService: EmploymentSessionService,
                                            implicit val clock: Clock,
                                            implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport
  with SessionHelper with Logging {

  def form: Form[EmploymentDate] = EmploymentDateForm.employmentStartDateForm

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    val log = "[EmployerLeaveDateController][show]"

    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>

        val startDate = data.employment.employmentDetails.startDate
        val cessationDate = data.employment.employmentDetails.cessationDate
        val cessationDateQuestion = data.employment.employmentDetails.cessationDateQuestion

        (startDate, cessationDateQuestion, cessationDate) match {
          case (startDate,cessationDateQuestion,_) if startDate.isEmpty || cessationDateQuestion.isEmpty =>
            logger.info(s"$log Prior questions for page are not answered. " +
              s"Start date: ${startDate.getOrElse("None")}, Still working for employer: ${cessationDateQuestion.getOrElse("None")}")
            Future.successful(employmentDetailsRedirect(data.employment, taxYear, employmentId, data.isPriorSubmission))
          case (_,Some(true),_) =>
            logger.info(s"$log Still working for employer answer is set to yes. Routing to correct employment redirect.")
            Future.successful(employmentDetailsRedirect(data.employment, taxYear, employmentId, data.isPriorSubmission))
          case (_,_,Some(cessationDate)) =>
            val parsedDate: LocalDate = LocalDate.parse(cessationDate, localDateTimeFormat)
            val filledForm: Form[EmploymentDate] = form.fill(
              EmploymentDate(parsedDate.getDayOfMonth.toString,parsedDate.getMonthValue.toString, parsedDate.getYear.toString))
            Future.successful(Ok(employerLeaveDateView(filledForm, taxYear, employmentId, data.employment.employmentDetails.employerName)))
          case (_,_,None) =>
            Future.successful(Ok(employerLeaveDateView(form, taxYear, employmentId, data.employment.employmentDetails.employerName)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    val log = "[EmployerLeaveDateController][submit]"

    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>

        val startDate = data.employment.employmentDetails.startDate
        val cessationDateQuestion = data.employment.employmentDetails.cessationDateQuestion
        (startDate, cessationDateQuestion) match {
          case (startDate,cessationDateQuestion) if startDate.isEmpty || cessationDateQuestion.isEmpty =>
            logger.info(s"$log Prior questions for page are not answered. " +
              s"Start date: ${startDate.getOrElse("None")}, Still working for employer: ${cessationDateQuestion.getOrElse("None")}")
            Future.successful(employmentDetailsRedirect(data.employment, taxYear, employmentId, data.isPriorSubmission))
          case (_, Some(true)) =>
            logger.info(s"$log Still working for employer answer is set to yes. Routing to correct employment redirect.")
            Future.successful(employmentDetailsRedirect(data.employment, taxYear, employmentId, data.isPriorSubmission))
          case (Some(startDate), _) =>

            val newForm = form.bindFromRequest()
            newForm.copy(errors = EmploymentDateForm.verifyLeaveDate(newForm.get, taxYear, user.isAgent, EmploymentDateForm.leaveDate,startDate)).fold(
              { formWithErrors =>
                Future.successful(BadRequest(employerLeaveDateView(formWithErrors, taxYear, employmentId, data.employment.employmentDetails.employerName)))
              },
              { submittedDate =>
                val cya = data.employment
                val updatedCya = cya.copy(cya.employmentDetails.copy(cessationDate = Some(submittedDate.toLocalDate.toString)))
                employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear, data.isPriorSubmission)(
                  errorHandler.internalServerError()) {
                  employmentDetailsRedirect(updatedCya, taxYear, employmentId, data.isPriorSubmission)
                }
              }
            )
        }
      }
    }
  }
}