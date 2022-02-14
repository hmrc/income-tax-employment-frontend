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

import actions._
import config.{AppConfig, ErrorHandler}
import forms.studentLoans.StudentLoanQuestionForm
import models.employment.StudentLoansCYAModel
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InYearUtil
import views.html.studentLoans.StudentLoansQuestionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StudentLoansQuestionController @Inject()(
                                                mcc: MessagesControllerComponents,
                                                view: StudentLoansQuestionView,
                                                employmentSessionService: EmploymentSessionService,
                                                authAction: AuthorisedAction,
                                                inYearAction: InYearUtil,
                                                errorHandler: ErrorHandler,
                                                implicit val appConfig: AppConfig,
                                                implicit val ec: ExecutionContext
                                              ) extends FrontendController(mcc) with I18nSupport {


  def show(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>

    if (appConfig.studentLoansEnabled) {
      val inYear: Boolean = inYearAction.inYear(taxYear)
      employmentSessionService.getSessionDataResult(taxYear, employmentId) {
        case Some(employmentData) => {
          employmentData.employment.studentLoans
            .fold(
              Future.successful(Ok(view(taxYear, employmentId, inYear, StudentLoanQuestionForm.studentLoanForm(request.user.isAgent))))
            )(
              studentLoans =>
                Future.successful(Ok(view(taxYear, employmentId, inYear, StudentLoanQuestionForm.studentLoanForm(request.user.isAgent), Some(studentLoans))))
            )

        }
        case _ => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
    else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
    if (appConfig.studentLoansEnabled) {
      val inYear: Boolean = inYearAction.inYear(taxYear)
      StudentLoanQuestionForm.studentLoanForm(request.user.isAgent).bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(view(taxYear, employmentId, inYear, formWithErrors)))
        },
        result => {
          employmentSessionService.getSessionDataResult(taxYear, employmentId) {
            case Some(data: EmploymentUserData) =>
              val cya = data.employment

              val newStudentLoans: StudentLoansCYAModel = result.toStudentLoansCyaModel(cya.studentLoans)

              val updatedCya = cya.copy(studentLoans = Some(newStudentLoans))
              employmentSessionService.createOrUpdateSessionData(
                employmentId,
                updatedCya,
                taxYear,
                data.isPriorSubmission,
                data.hasPriorBenefits,
                data.hasPriorStudentLoans,
                request.user)({
                errorHandler.internalServerError()
              })(studentLoansRedirect(newStudentLoans, taxYear, employmentId))
            case None =>
              Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
        }
      )
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def studentLoansRedirect(newStudentLoans: StudentLoansCYAModel, taxYear: Int, employmentId: String): Result = {
    newStudentLoans match {
      case StudentLoansCYAModel(true, None, _, _) => Ok("uglAmount page")
      case StudentLoansCYAModel(_, _, true, None) => Redirect(controllers.studentLoans.routes.PglAmountController.show(taxYear, employmentId))
      case _ => Redirect(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYear, employmentId))
    }
  }


}
