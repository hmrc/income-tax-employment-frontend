/*
 * Copyright 2022 HM Revenue & Customs
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

import actions.AuthorisedAction
import config.AppConfig
import models.employment.EmploymentSourceOrigin
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.employment.EmployerInformationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EmployerInformationController @Inject()(implicit val cc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              employerInformationView: EmployerInformationView,
                                              inYearAction: InYearUtil,
                                              implicit val appConfig: AppConfig,
                                              employmentSessionService: EmploymentSessionService,
                                              implicit val ec: ExecutionContext
                                                      ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { allEmploymentData =>
      val isInYear = inYearAction.inYear(taxYear)
      val source = if (isInYear) allEmploymentData.inYearEmploymentSourceWith(employmentId) else allEmploymentData.eoyEmploymentSourceWith(employmentId)

      source match {
        case Some(EmploymentSourceOrigin(source, _)) =>
          val (name, benefitsIsDefined) = (source.employerName, source.employmentBenefits.isDefined)
          Ok(employerInformationView(name, employmentId, benefitsIsDefined, taxYear, isInYear))
        case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }
}
