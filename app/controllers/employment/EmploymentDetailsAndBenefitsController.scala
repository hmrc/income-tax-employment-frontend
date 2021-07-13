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
import javax.inject.Inject
import models.employment.EmploymentSource
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.EmploymentDetailsAndBenefitsView

import scala.concurrent.ExecutionContext

class EmploymentDetailsAndBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                       authAction: AuthorisedAction,
                                                       employmentDetailsAndBenefitsView: EmploymentDetailsAndBenefitsView,
                                                       implicit val appConfig: AppConfig,
                                                       employmentSessionService: EmploymentSessionService,
                                                       implicit val ec: ExecutionContext
                                                      ) extends FrontendController(cc) with I18nSupport with SessionHelper {


  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    employmentSessionService.findPreviousEmploymentUserData(user, taxYear){ allEmploymentData =>
      val source: Option[EmploymentSource] = {
        allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId))
      }

      source match {
        case Some(source) =>
          val (name, benefitsIsDefined) = (source.employerName, source.employmentBenefits.isDefined)
          Ok(employmentDetailsAndBenefitsView(name, employmentId, benefitsIsDefined, taxYear))
        case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }
}
