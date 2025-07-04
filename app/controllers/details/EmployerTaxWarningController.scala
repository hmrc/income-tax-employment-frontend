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

package controllers.details

import actions.{ActionsProvider, AuthorisedAction}
import config.AppConfig
import models.details.pages.{EmployerTaxWarningPage => PageModel}
import models.employment.EmploymentDetailsType
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.details.EmployerTaxWarningView

import javax.inject.Inject

class EmployerTaxWarningController @Inject()(actionsProvider: ActionsProvider,
                                             employmentService: EmploymentService,
                                             authAction: AuthorisedAction,
                                             view: EmployerTaxWarningView)
                                            (implicit mcc: MessagesControllerComponents, val appConfig: AppConfig)
  extends FrontendController(mcc)
  with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentDetailsType
  ){ implicit request =>
      Ok(view(PageModel(taxYear, employmentId, request.user, request.employmentUserData)))
  }
}