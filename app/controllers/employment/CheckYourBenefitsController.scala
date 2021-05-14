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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.CheckYourBenefitsView
import javax.inject.Inject
import models.employment.{AllEmploymentData, Benefits}
import services.IncomeTaxUserDataService

import scala.concurrent.ExecutionContext


class CheckYourBenefitsController @Inject()(authorisedAction: AuthorisedAction,
                                            val mcc: MessagesControllerComponents,
                                            implicit val appConfig: AppConfig,
                                            checkYourBenefitsView: CheckYourBenefitsView,
                                            incomeTaxUserDataService: IncomeTaxUserDataService,
                                            implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    def result(allEmploymentData: AllEmploymentData): Result = {

      val benefits: Option[Benefits] = {
        allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId)).flatMap(_.employmentBenefits).flatMap(_.benefits)
      }

      benefits match {
        case Some(benefits) => Ok(checkYourBenefitsView(taxYear, benefits))
        case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)) //This will be changed to serve its own page as part of SASS-669
      }
    }

    incomeTaxUserDataService.findUserData(user, taxYear, result)
  }
}
