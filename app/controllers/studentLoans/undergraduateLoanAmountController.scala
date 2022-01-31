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
import forms.{AmountForm, FormUtils}
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
                                                  view: UndergraduateLoanAmountView,
                                                  inYearAction: InYearAction,
                                                  appConfig: AppConfig,
                                                  val employmentSessionService: EmploymentSessionService,
                                                  studentLoansService: StudentLoansService,
                                                  errorHandler: ErrorHandler,
                                                  clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {
  private implicit val executionContext: ExecutionContext = cc.executionContext

  import config.{AppConfig, ErrorHandler}
  import controllers.predicates.{AuthorisedAction, InYearAction, TaxYearAction}
  import forms.{AmountForm, FormUtils}
  import models.User
  import play.api.data.Form
  import play.api.i18n.I18nSupport
  import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
  import services.EmploymentSessionService
  import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
  import utils.{Clock, SessionHelper}
  import views.html.studentLoans.UndergraduateLoanAmountView

  import javax.inject.Inject
  import scala.concurrent.{ExecutionContext, Future}

  class PglAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                      authAction: AuthorisedAction,
                                      inYearAction: InYearAction,
                                      val employmentSessionService: EmploymentSessionService,
                                      view: UndergraduateLoanAmountView,
                                      appConfig: AppConfig,
                                      errorHandler: ErrorHandler,
                                      ec: ExecutionContext,
                                      clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

    def show(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit user =>

      if(appConfig.studentLoansEnabled) {
        employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

          val cyaData = optCya.flatMap(_.employment.studentLoansCYAModel.flatMap(_.pglDeductionAmount))

          val form = fillFormFromPriorAndCYA(buildForm, prior, cyaData, employmentId)(
            employment => employment.employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.uglDeductionAmount)))
          )
          Future.successful(Ok(view(taxYear, form, cyaData, employmentId)))
        }
      } else {
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }

    }

    def submit(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit user =>

      if(appConfig.studentLoansEnabled) {

      }

    }



  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, uglAmount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    studentLoansService.updateUglDeductionAmount(taxYear, employmentId, employmentUserData, uglAmount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }

  private def buildForm(implicit user: User[_]): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"studentLoans.undergraduateLoanAmount.error.noEntry.${if (user.isAgent) "agent" else "individual"}",
    emptyFieldArguments = Seq(employmentDetails.employerName),
    wrongFormatKey = "studentLoans.undergraduateLoanAmount.error.invalidFormat"
  )
}
