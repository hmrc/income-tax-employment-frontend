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
import forms.AmountForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.EmployerPayAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerPayAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            employerPayAmountView: EmployerPayAmountView,
                                            inYearAction: InYearAction,
                                            appConfig: AppConfig,
                                            employmentSessionService: EmploymentSessionService,
                                            errorHandler: ErrorHandler,
                                            clock: Clock,
                                            ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with SessionHelper {


  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {
      val redirectUrl = controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { data =>
        val amount = data.employment.employmentDetails.taxablePayToDate
        val form = amount.fold(buildForm(user.isAgent))(x => buildForm(user.isAgent).fill(x))
        Future.successful(Ok(employerPayAmountView(taxYear, form,
          amount, data.employment.employmentDetails.employerName, employmentId)))
      }
    }
  }


  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {
      val redirectUrl = controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { data =>

        buildForm(user.isAgent).bindFromRequest().fold(
          {
            formWithErrors =>
              Future.successful(BadRequest(employerPayAmountView(taxYear, formWithErrors,
                data.employment.employmentDetails.taxablePayToDate, data.employment.employmentDetails.employerName, employmentId)))
          }
          ,
          {
            amount =>
              val cya = data.employment
              val updatedCyaModel = cya.copy(employmentDetails = cya.employmentDetails.copy(taxablePayToDate = Some(amount)))
              employmentSessionService.createOrUpdateSessionData(employmentId, updatedCyaModel, taxYear,
                isPriorSubmission = data.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId))
              }
          }
        )
      }
    }
  }


  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"employerPayAmount.error.empty.${if (isAgent) "agent" else "individual"}",
      "employerPayAmount.error.wrongFormat", "employerPayAmount.error.amountMaxLimit")
  }
}
