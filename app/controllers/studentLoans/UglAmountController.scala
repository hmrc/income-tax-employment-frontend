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

package controllers.studentLoans

import actions.{AuthorisedAction, TaxYearAction}
import config.{AppConfig, ErrorHandler}
import forms.studentLoans.StudentLoansFormsProvider
import models.AuthorisationRequest
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.studentLoans.StudentLoansService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.studentLoans.UglAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UglAmountController @Inject()(mcc: MessagesControllerComponents,
                                    authAction: AuthorisedAction,
                                    employmentSessionService: EmploymentSessionService,
                                    studentLoansService: StudentLoansService,
                                    view: UglAmountView,
                                    formsProvider: StudentLoansFormsProvider,
                                    inYearAction: InYearUtil,
                                    errorHandler: ErrorHandler)
                                   (implicit val appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
    if (appConfig.studentLoansEnabled && appConfig.employmentEOYEnabled && !inYearAction.inYear(taxYear)) {
      employmentSessionService.getSessionData(taxYear, employmentId, request.user).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(optionCyaData) =>
          optionCyaData match {
            case Some(cyaData) =>
              val uglQuestion: Boolean = cyaData.employment.studentLoans.fold(false)(_.uglDeduction)

              if (uglQuestion) {
                val uglAmount = cyaData.employment.studentLoans.flatMap(_.uglDeductionAmount)
                val employerName = cyaData.employment.employmentDetails.employerName

                val form = uglAmount.fold(formsProvider.uglAmountForm(employerName, request.user.isAgent)
                )(formsProvider.uglAmountForm(employerName, request.user.isAgent).fill _)
                Ok(view(taxYear, form, employmentId, employerName))

              } else {
                Redirect(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYear, employmentId))
              }

            case None => Redirect(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYear, employmentId))
          }

      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
    val studentLoansCyaControllerRoute = controllers.studentLoans.routes.StudentLoansCYAController

    if (appConfig.studentLoansEnabled && appConfig.employmentEOYEnabled && !inYearAction.inYear(taxYear)) {
      val redirectUrl = studentLoansCyaControllerRoute.show(taxYear, employmentId).url
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        formsProvider.uglAmountForm(cya.employment.employmentDetails.employerName, request.user.isAgent).bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(view(taxYear, formWithErrors, employmentId, cya.employment.employmentDetails.employerName)))
          },
          amount => {
            val uglQuestion: Boolean = cya.employment.studentLoans.fold(false)(_.uglDeduction)
            if (uglQuestion) {
              handleSuccessForm(taxYear, employmentId, cya, amount)
            } else {
              Future.successful(Redirect(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYear, employmentId)))
            }
          }
        )
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }


  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    studentLoansService.updateUglDeductionAmount(request.user, taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(_) =>
        if (employmentUserData.employment.studentLoans.fold(false)(data => data.pglDeduction && data.pglDeductionAmount.isEmpty)) {
          Redirect(controllers.studentLoans.routes.PglAmountController.show(taxYear, employmentId))
        }
        else {
          Redirect(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYear, employmentId))
        }
    }
  }
}
