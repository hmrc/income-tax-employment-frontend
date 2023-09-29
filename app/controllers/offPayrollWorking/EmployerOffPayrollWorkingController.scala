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
import forms.{YesNoForm}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.offPayrollWorking.EmployerOffPayrollWorkingView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerOffPayrollWorkingController @Inject()(mcc: MessagesControllerComponents,
                                                    authAction: AuthorisedAction,
                                                    actionsProvider: ActionsProvider,
                                                    view: EmployerOffPayrollWorkingView,
                                                    errorHandler: ErrorHandler)
                                                   (implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with SessionHelper {

  private def form(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(s"employment.employerOpw.error.${if (isAgent) "agent" else "individual"}")

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>

    if (appConfig.offPayrollWorking) {
      Future.successful(Ok(view(form(request.user.isAgent), taxYear)))
    } else {
      Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>

    form(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      yesNo => {
        Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    )
  }

}
