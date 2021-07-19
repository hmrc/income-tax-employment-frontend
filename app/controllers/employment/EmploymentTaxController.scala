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
import views.html.employment.EmploymentTaxView

import javax.inject.Inject
import scala.concurrent.Future

class EmploymentTaxController @Inject()(implicit val mcc: MessagesControllerComponents,
                                        authAction: AuthorisedAction,
                                        appConfig: AppConfig,
                                        employmentTaxView: EmploymentTaxView,
                                        employmentSessionService: EmploymentSessionService,
                                        inYearAction: InYearAction,
                                        errorHandler: ErrorHandler,
                                        implicit val clock: Clock
                                       )
  extends FrontendController(mcc) with I18nSupport with SessionHelper{

  implicit val ec = mcc.executionContext

  def form(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(
      s"employment.employmentTax.error.noEntry.${if(isAgent) "agent" else "individual"}",
      "employment.employmentTax.error.format",
      "employment.employmentTax.error.max"
    )
  }

  def show(taxYear: Int, employmentId: String):Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).map {
          case Some(cya) =>
            val totalTaxToDate = cya.employment.employmentDetails.totalTaxToDate
            Ok(employmentTaxView(taxYear, user.isAgent, cya.employment.employmentDetails.employerName,
              controllers.employment.routes.EmploymentTaxController.submit(taxYear, employmentId),
              totalTaxToDate.fold(form(user.isAgent))(form(user.isAgent).fill), totalTaxToDate))
          case None => Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId))
        }
      }
    }

  def submit(taxYear: Int, employmentId: String):Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).flatMap {
        case Some(cya) =>
          form(user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(employmentTaxView(taxYear, user.isAgent, cya.employment.employmentDetails.employerName,
              controllers.employment.routes.EmploymentTaxController.submit(taxYear, employmentId),
              formWithErrors, cya.employment.employmentDetails.totalTaxToDate))),
            completeForm => {
              val employmentCYAModel = cya.employment.copy(employmentDetails = cya.employment.employmentDetails.copy(totalTaxToDate = Some(completeForm)))
              employmentSessionService.createOrUpdateSessionData(
                employmentId, employmentCYAModel, taxYear, cya.isPriorSubmission)(errorHandler.internalServerError()){
                Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId))
              }
            }
          )
        case None => Future.successful(Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId)))
      }
    }
  }

}
