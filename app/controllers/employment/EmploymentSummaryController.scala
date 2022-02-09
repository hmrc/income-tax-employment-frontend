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
import controllers.employment.routes.AddEmploymentController
import models.User
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.employment.EmploymentSummaryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentSummaryController @Inject()(implicit val mcc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            implicit val appConfig: AppConfig,
                                            employmentSummaryView: EmploymentSummaryView,
                                            employmentSessionService: EmploymentSessionService,
                                            inYearAction: InYearUtil
                                           ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  private implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    val isInYear: Boolean = inYearAction.inYear(taxYear)
    findPriorDataAndReturnResult(taxYear, isInYear)
  }

  private def findPriorDataAndReturnResult(taxYear: Int, isInYear: Boolean)
                                          (implicit user: User[_]): Future[Result] = {
    val overrideRedirect = if (isInYear) None else Some(Redirect(AddEmploymentController.show(taxYear)))

    employmentSessionService.findPreviousEmploymentUserData(user, taxYear, overrideRedirect) { allEmploymentData =>
      val latestExpenses = if (isInYear) allEmploymentData.latestInYearExpenses else allEmploymentData.latestEOYExpenses
      val doExpensesExist = latestExpenses.isDefined
      val employmentData = if (isInYear) allEmploymentData.latestInYearEmployments else allEmploymentData.latestEOYEmployments

      employmentData match {
        case Seq() if isInYear => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        case Seq() if !isInYear => Redirect(AddEmploymentController.show(taxYear))
        case _ => Ok(employmentSummaryView(taxYear, employmentData, doExpensesExist, isInYear))
      }
    }
  }
}
