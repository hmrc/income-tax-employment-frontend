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
import controllers.predicates.AuthorisedAction
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.GetEmploymentsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.EmploymentSummaryView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EmploymentSummaryController @Inject()(
                                               implicit val mcc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               implicit val appConfig: AppConfig,
                                               employmentSummaryView: EmploymentSummaryView,
                                               getEmploymentsService: GetEmploymentsService
                                             ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int) : Action[AnyContent] = authAction.async { implicit user =>
    val employments = Seq("employment1")
    getEmploymentsService.getEmployments(user.nino, taxYear)(hc.copy().withExtraHeaders("mtditid" -> user.mtditid)).map {
      case Right(Some(listOfEmployments)) => Ok(employmentSummaryView(taxYear, employments))
      case Right(None) => Ok(employmentSummaryView(taxYear, employments))
      case Left(apiErrorModel) => Status(apiErrorModel.status)(apiErrorModel.toJson)
    }

  }


}
