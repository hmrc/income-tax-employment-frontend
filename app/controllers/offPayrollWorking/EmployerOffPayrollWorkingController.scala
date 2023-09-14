/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.offPayrollWorking

import actions.{ActionsProvider, AuthorisedAction, TaxYearAction}
import config.{AppConfig, ErrorHandler}
import forms.{FormUtils, YesNoForm}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.tailoring.TailoringService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.offPayrollWorking.EmployerOffPayrollWorkingView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerOffPayrollWorkingController @Inject()(tailoringService: TailoringService,
                                            employmentSessionService: EmploymentSessionService,
                                            mcc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            actionsProvider: ActionsProvider,
                                            view: EmployerOffPayrollWorkingView,
                                            errorHandler: ErrorHandler)
                                           (implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with SessionHelper with FormUtils {

  private def form(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(s"tailoring.empty.error.${if (isAgent) "agent" else "individual"}")

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
    if (appConfig.tailoringEnabled) {
      tailoringService.getExcludedJourneys(taxYear, request.user.mtditid, request.user.mtditid).map {
        case Left(error) => errorHandler.handleError(error.status)
        case Right(result) =>
          Ok(view(form(request.user.isAgent).fill(!result.journeys.map(_.journey).contains("employment")), taxYear))
      }
    }
    else {
      Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    if (appConfig.tailoringEnabled) {
      form(request.user.isAgent).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        yesNo => {
          if (yesNo) {
            tailoringService.clearExcludedJourney(taxYear, request.user.nino, request.user.mtditid).map {
              case Left(error) => errorHandler.internalServerError()
              case Right(value) => Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear))
            }
          }
          else {
            Future(Redirect(controllers.tailorings.routes.RemoveAllEmploymentController.show(taxYear)))
          }
        }
      )
    }
    else {
      Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

}
