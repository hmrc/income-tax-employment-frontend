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

import common.SessionValues
import config.AppConfig
import controllers.predicates.AuthorisedAction
import models.employment.{AllEmploymentData, EmploymentExpenses, EmploymentSource}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.SingleEmploymentSummaryView
import views.html.employment.MultipleEmploymentsSummaryView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EmploymentSummaryController @Inject()(
                                               implicit val mcc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               implicit val appConfig: AppConfig,
                                               singleEmploymentSummaryView: SingleEmploymentSummaryView,
                                               multipleEmploymentsSummaryView: MultipleEmploymentsSummaryView
                                             ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int) : Action[AnyContent] = authAction { implicit user =>

    val source: Option[AllEmploymentData] = getModelFromSession[AllEmploymentData](SessionValues.EMPLOYMENT_PRIOR_SUB)
    if(source.isDefined) {
      val listOfEmployments = source.get.hmrcEmploymentData ++ source.get.customerEmploymentData
      val doExpensesExist = if(source.get.hmrcExpenses.isDefined || source.get.customerExpenses.isDefined) true else false
      if (listOfEmployments.length == 1) {
        Ok(singleEmploymentSummaryView(taxYear, listOfEmployments.head, doExpensesExist))
      } else {
        Ok(multipleEmploymentsSummaryView(taxYear, listOfEmployments, doExpensesExist))
      }
    } else {
      Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }

}
