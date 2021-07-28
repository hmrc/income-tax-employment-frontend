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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.{MultipleEmploymentsSummaryView, SingleEmploymentSummaryView}
import javax.inject.Inject

import scala.concurrent.ExecutionContext

class EmploymentSummaryController @Inject()(implicit val mcc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            implicit val appConfig: AppConfig,
                                            singleEmploymentSummaryView: SingleEmploymentSummaryView,
                                            multipleEmploymentsSummaryView: MultipleEmploymentsSummaryView,
                                            employmentSessionService: EmploymentSessionService,
                                            inYearAction: InYearAction
                                           ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>

    val isInYear: Boolean = inYearAction.inYear(taxYear)

    employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { allEmploymentData =>

      val latestExpenses = employmentSessionService.getLatestExpenses(allEmploymentData, isInYear)
      val doExpensesExist = latestExpenses.isDefined

      val employmentData = employmentSessionService.getLatestEmploymentData(allEmploymentData, isInYear)

      if (employmentData.isEmpty) {
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      } else if (employmentData.size == 1) {
        Ok(singleEmploymentSummaryView(taxYear, employmentData.head, doExpensesExist))
      } else {
        Ok(multipleEmploymentsSummaryView(taxYear, employmentData, doExpensesExist))
      }
    }
  }
}
