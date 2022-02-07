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

import actions.{AuthorisedAction, TaxYearAction}
import common.SessionValues
import config.AppConfig
import controllers.employment.routes.EmployerInformationController
import controllers.expenses.routes.CheckEmploymentExpensesController
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.studentLoans.StudentLoansCYAService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.studentLoans.StudentLoansCYAView

import scala.concurrent.{ExecutionContext, Future}

class StudentLoansCYAController @Inject()(
                                           mcc: MessagesControllerComponents,
                                           view: StudentLoansCYAView,
                                           service: StudentLoansCYAService,
                                           authAction: AuthorisedAction,
                                           inYearAction: InYearUtil,
                                           implicit val appConfig: AppConfig,
                                           implicit val ec: ExecutionContext,
                                           implicit val clock: Clock
                                         ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>

    if(appConfig.studentLoansEnabled) {
      val inYear: Boolean = inYearAction.inYear(taxYear)
      
      service.retrieveCyaDataAndIsCustomerHeld(taxYear, employmentId)(_.fold(
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      ) { case (cya, isCustomer) =>
        Ok(view(taxYear, employmentId, cya, isCustomer, inYear))
      })
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }
  
  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
    if(appConfig.studentLoansEnabled) {

      def getResultFromResponse(returnedEmploymentId: Option[String]): Result = {
        Redirect(returnedEmploymentId match {
          case Some(employmentId) => EmployerInformationController.show(taxYear, employmentId)
          case None =>
            getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID) match {
              case Some(sessionEmploymentId) if sessionEmploymentId == employmentId => CheckEmploymentExpensesController.show(taxYear)
              case None => EmployerInformationController.show(taxYear, employmentId)
            }
        })
      }

      service.submitStudentLoans(taxYear, employmentId, getResultFromResponse)

    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }
}
