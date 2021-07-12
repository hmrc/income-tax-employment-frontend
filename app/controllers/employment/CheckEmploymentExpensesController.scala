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

import audit.{AuditService, ViewEmploymentExpensesAudit}
import config.AppConfig
import controllers.predicates.AuthorisedAction
import models.employment.EmploymentExpenses

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.CheckEmploymentExpensesView

import scala.concurrent.ExecutionContext

class CheckEmploymentExpensesController @Inject()(authorisedAction: AuthorisedAction,
                                                  checkEmploymentExpensesView: CheckEmploymentExpensesView,
                                                  employmentSessionService: EmploymentSessionService,
                                                  auditService: AuditService,
                                                  implicit val appConfig: AppConfig,
                                                  implicit val mcc: MessagesControllerComponents,
                                                  implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>

    employmentSessionService.findPreviousEmploymentUserData(user, taxYear)(allEmploymentData =>
      allEmploymentData.hmrcExpenses match {
      case Some(employmentExpenses@EmploymentExpenses(_,_, Some(expenses))) =>
        val auditModel = ViewEmploymentExpensesAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, expenses)
        auditService.auditModel[ViewEmploymentExpensesAudit](auditModel.toAuditModel)
        Ok(checkEmploymentExpensesView(taxYear, employmentExpenses))
      case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    })
  }
}
