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

package controllers.studentLoans

import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction, TaxYearAction}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.studentLoans.StudentLoansCYAService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.studentLoans.StudentLoansCYAView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StudentLoansCYAController @Inject()(
                                           mcc: MessagesControllerComponents,
                                           view: StudentLoansCYAView,
                                           service: StudentLoansCYAService,
                                           authAction: AuthorisedAction,
                                           inYearAction: InYearAction,
                                           errorHandler: ErrorHandler,
                                           implicit val appConfig: AppConfig,
                                           implicit val ec: ExecutionContext
                                         ) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
    
    val inYear: Boolean = inYearAction.inYear(taxYear)
    
    service.retrieveCyaDataAndIsPrior(taxYear, employmentId).map {
      case Some((data, isPriorData)) => Ok(view(taxYear, employmentId, data, isPriorData, inYear))
      case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
    
  }
  
  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
    service.submitStudentLoans(taxYear, employmentId) {
      case Some(employmentResponse) => employmentResponse match {
        case Right(_) => Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear))
        case Left(_) => errorHandler.internalServerError()
      }
      case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }
  
}
