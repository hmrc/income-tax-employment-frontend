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

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckEmploymentDetailsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.employment.EmployerPayrollIdForm
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.EmployerPayrollIdView

import scala.concurrent.{ExecutionContext, Future}

class EmployerPayrollIdController @Inject()(authorisedAction: AuthorisedAction,
                                            val mcc: MessagesControllerComponents,
                                            implicit val appConfig: AppConfig,
                                            employerPayrollIdView: EmployerPayrollIdView,
                                            inYearAction: InYearAction,
                                            errorHandler: ErrorHandler,
                                            employmentSessionService: EmploymentSessionService,
                                            implicit val clock: Clock,
                                            implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    val emptyForm = EmployerPayrollIdForm.employerPayrollIdForm(user.isAgent)

    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).map{
          case Right(Some(cya)) =>
            cya.employment.employmentDetails.payrollId match {
              case Some(payrollId) =>
                val filledForm = emptyForm.fill(payrollId)
                Ok(employerPayrollIdView(filledForm, taxYear, employmentId, Some(payrollId)))
              case None =>
                Ok(employerPayrollIdView(emptyForm, taxYear, employmentId, None))
            }
          case _ => Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId))
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckEmploymentDetailsController.show(taxYear, employmentId).url
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { data =>
        EmployerPayrollIdForm.employerPayrollIdForm(user.isAgent).bindFromRequest().fold(
          { formWithErrors =>
            val previousData = data.employment.employmentDetails.payrollId
            Future.successful(BadRequest(employerPayrollIdView(formWithErrors, taxYear, employmentId, previousData)))
          },
          { submittedPayrollId =>
            val cya = data.employment
            val updatedCya = cya.copy(cya.employmentDetails.copy(payrollId = Some(submittedPayrollId)))
            employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear, data.isPriorSubmission,
              data.hasPriorBenefits)(errorHandler.internalServerError()) {
              employmentDetailsRedirect(updatedCya,taxYear,employmentId,data.isPriorSubmission)
            }
          }
        )
      }
    }
  }
}
