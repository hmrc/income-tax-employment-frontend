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
import forms.AmountForm
import javax.inject.Inject
import models.User
import models.mongo.{EmploymentDetails, EmploymentUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.studentLoans.StudentLoansService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.studentLoans.UndergraduateLoanAmountView

import scala.concurrent.{ExecutionContext, Future}

class undergraduateLoanAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  undergraduateLoanAmountView: UndergraduateLoanAmountView,
                                                  inYearAction: InYearAction,
                                                  appConfig: AppConfig,
                                                  employmentSessionService: EmploymentSessionService,
                                                  studentLoansService: StudentLoansService,
                                                  errorHandler: ErrorHandler,
                                                  clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {
  private implicit val executionContext: ExecutionContext = cc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val cyaAmount = cya.employment.studentLoansCYAModel.flatMap()
            lazy val unfilledForm = buildForm(user.isAgent)
            val form: Form[BigDecimal] = cyaAmount.fold(unfilledForm)(
              cyaPay => if (priorAmount.exists(_.equals(cyaPay))) unfilledForm else buildForm(user.isAgent).fill(cyaPay))

            Future.successful(Ok(undergraduateLoanAmountView(taxYear, form,
              cyaAmount, cya.employment.employmentDetails.employerName, employmentId)))

          case None => Future.successful(
            Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, uglAmount: BigDecimal): Action[AnyContent] = (authAction andThen
    TaxYearAction.taxYearAction(taxYear)).async { implicit user =>
    if(appConfig.studentLoansEnabled) {
      studentLoansService.updateUglDeductionAmount(taxYear, employmentId, employmentUserData, uglAmount) {
          buildForm(user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(undergraduateLoanAmountView(formWithErrors, taxYear,
              data.employment.employmentDetails.employerName, employmentId, data.employment.studentLoansCYAModel.uglDeductionAmount))),
            amount => handleSuccessForm(taxYear, employmentId, data, amount)
          )
        }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }



  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, uglAmount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    studentLoansService.updateUglDeductionAmount(taxYear, employmentId, employmentUserData, uglAmount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => Redirect(employmentUserData.employment, taxYear, employmentId, employmentUserData.isPriorSubmission)
    }
  }

  private def buildForm(isAgent: Boolean, employmentDetails: EmploymentDetails): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"studentLoans.undergraduateLoanAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    emptyFieldArguments = Seq(employmentDetails.employerName),
    wrongFormatKey = "studentLoans.undergraduateLoanAmount.error.invalidFormat"
  )
}
