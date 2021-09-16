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
import forms.employment.EmploymentStartDateForm
import models.User
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
import views.html.employment.StillWorkingForEmployerView
import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StillWorkingForEmployerController @Inject()(authorisedAction: AuthorisedAction,
                                                  val mcc: MessagesControllerComponents,
                                                  implicit val appConfig: AppConfig,
                                                  //employerStartDateView: EmployerStartDateView,
                                                  stillWorkingForEmployerView: StillWorkingForEmployerView,
                                                  inYearAction: InYearAction,
                                                  errorHandler: ErrorHandler,
                                                  employmentSessionService: EmploymentSessionService,
                                                  implicit val clock: Clock,
                                                  implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {


  //def form: Form[EmploymentDate] = EmploymentStartDateForm.employmentStartDateForm

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"employment.stillWorkingForEmployer.error.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
        data.employment.employmentDetails.stillWorkingQuestion match {
          case Some(isStillWorkingForEmployer) =>
            //val parsedDate: LocalDate = LocalDate.parse(startDate, localDateTimeFormat)
            //val filledForm: Form[EmploymentDate] = form.fill(
              //EmploymentDate(parsedDate.getDayOfMonth.toString,parsedDate.getMonthValue.toString, parsedDate.getYear.toString))
            //Future.successful(Ok(employerStartDateView(filledForm, taxYear, employmentId, data.employment.employmentDetails.employerName)))

            Future.successful(Ok(stillWorkingForEmployerView(yesNoForm.fill(isStillWorkingForEmployer), taxYear,
              employmentId,data.employment.employmentDetails.employerName)))
          case None =>
            Future.successful(Ok(stillWorkingForEmployerView(yesNoForm, taxYear, employmentId, data.employment.employmentDetails.employerName)))
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
              val updatedCya = cya.copy(cya.employmentDetails.copy(stillWorkingQuestion = Some(yesNo)))
              employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear,
                data.isPriorSubmission)(errorHandler.internalServerError()) {
                employmentDetailsRedirect(updatedCya, taxYear, employmentId, data.isPriorSubmission)
              }
            }
          )
        }
      }
    }}

//    def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
//      inYearAction.notInYear(taxYear) {
//
//        employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
//
//          yesNoForm.bindFromRequest().fold(
//            formWithErrors => Future.successful(BadRequest(stillWorkingForEmployerView(formWithErrors, taxYear, employmentId))),
//            yesNo => {
//              val cya = data.employment
//              val updatedCya = cya.copy(cya.employmentDetails.copy(stillWorkingQuestion = Some(yesNo)))
//              employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
//                employmentDetailsRedirect(updatedCya,taxYear,employmentId,data.isPriorSubmission)
//              }
//            }
//
//
//
//
//
//    }
//


//  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
//    inYearAction.notInYear(taxYear) {
//      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)() { data =>
//        val newForm = form.bindFromRequest()
//          newForm.copy(errors = EmploymentStartDateForm.verifyNewDate(newForm.get, taxYear, user.isAgent)).fold(
//          { formWithErrors =>
//            Future.successful(BadRequest(employerStartDateView(formWithErrors, taxYear, employmentId, data.employment.employmentDetails.employerName)))
//          },
//          { submittedDate =>
//            val cya = data.employment
//            val updatedCya = cya.copy(cya.employmentDetails.copy(startDate = Some(submittedDate.toLocalDate.toString)))
//            employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
//              employmentDetailsRedirect(updatedCya,taxYear,employmentId,data.isPriorSubmission)
//            }
//          }
//        )
//      }
//    }
//  }

