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

import actions.{AuthorisedAction, TaxYearAction}
import config.AppConfig
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.offPayrollWorking.UkTaxTakenFromPayView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkTaxTakenFromPayController @Inject()(mcc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            view: UkTaxTakenFromPayView)
                                           (implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
    val headingIndividual = "employment.employerOpw.warning.tax.heading.individual"
    val headingAgent = "employment.employerOpw.warning.tax.heading.agent"

    val titleIndividual = "employment.employerOpw.warning.tax.title.individual"
    val titleAgent = "employment.employerOpw.warning.tax.title.agent"

    val cancelUrl = appConfig.incomeTaxSubmissionOverviewUrl(taxYear) //TODO: This needs to be changed when Samuel's ticket is merged in
    val continueUrl = appConfig.incomeTaxSubmissionOverviewUrl(taxYear)

    if (request.user.isAgent) {
      Future.successful(Ok(view(taxYear, titleAgent, headingAgent, continueUrl, cancelUrl)))
    } else {
      Future.successful(Ok(view(taxYear, titleIndividual, headingIndividual, continueUrl, cancelUrl)))
    }
  }

}
