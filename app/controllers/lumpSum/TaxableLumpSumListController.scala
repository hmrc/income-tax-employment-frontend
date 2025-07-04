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

package controllers.lumpSum

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import models.employment.EmploymentDetailsType
import models.otheremployment.pages.TaxableLumpSumListPage
import models.otheremployment.session.{OtherEmploymentIncomeCYAModel, TaxableLumpSum}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.taxableLumpSum.TaxableLumpSumListView

import javax.inject.Inject

class TaxableLumpSumListController @Inject()(mcc: MessagesControllerComponents,
                                             actionsProvider: ActionsProvider,
                                             view: TaxableLumpSumListView,
                                             inYearAction: InYearUtil,
                                             errorHandler: ErrorHandler)
                                            (implicit val appConfig: AppConfig) extends FrontendController(mcc)
  with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentDetailsType
  ) { implicit request =>
    if (!appConfig.taxableLumpSumsEnabled) {
      Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear))
    } else {
      Ok(view(TaxableLumpSumListPage(
        request.employmentUserData.employment.otherEmploymentIncome.getOrElse(OtherEmploymentIncomeCYAModel(Seq.empty[TaxableLumpSum])),
        request.employmentUserData.taxYear,
        request.employmentUserData.employmentId)))
    }
  }
}
