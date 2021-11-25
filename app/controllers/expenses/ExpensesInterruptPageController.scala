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

package controllers.expenses

import config.AppConfig
import controllers.predicates.{AuthorisedAction, InYearAction}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.expenses.ExpensesInterruptPageView

import javax.inject.Inject
import scala.concurrent.Future

class ExpensesInterruptPageController @Inject() (implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 inYearAction: InYearAction,
                                                 expensesInterruptPageView: ExpensesInterruptPageView,
                                                 appConfig: AppConfig,
                                                 clock: Clock
                                                )extends FrontendController(cc) with I18nSupport with SessionHelper{


   def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
        Future.successful(Ok(expensesInterruptPageView(taxYear)))
      }
    }

}
