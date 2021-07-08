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

import config.AppConfig
import controllers.predicates.{AuthorisedAction, InYearAction}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.IncomeTaxUserDataService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.EmployerNameView
import forms.employment.EmployerNameForm

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerNameController @Inject()(authorisedAction: AuthorisedAction,
                                       val mcc: MessagesControllerComponents,
                                       implicit val appConfig: AppConfig,
                                       employerNameView: EmployerNameView,
                                       implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>

    val previousNames = List("Google","Apple")

    val form: Form[String] = EmployerNameForm.employerNameForm(previousNames, user.isAgent)

    Future.successful(Ok(employerNameView(form, taxYear)))

  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>

    val previousNames = List("Google","Apple")

    val form: Form[String] = EmployerNameForm.employerNameForm(previousNames, user.isAgent)

    form.bindFromRequest().fold(
      { formWithErrors =>
        Future.successful(BadRequest(employerNameView(formWithErrors, taxYear)))
      },
      { submittedName =>
        //TODO - Once Create and Update API has been orchestrated
        Future.successful(Ok("Redirect next page"))
      }
    )

  }

}
