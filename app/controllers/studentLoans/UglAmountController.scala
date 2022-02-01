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
import models.User
import controllers.studentLoans.routes.StudentLoansCYAController
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.EmploymentUserDataRepository
import services.EmploymentSessionService
import services.studentLoans.StudentLoansService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.studentLoans.UglAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UglAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                    authAction: AuthorisedAction,
                                    inYearAction: InYearAction,
                                    val employmentSessionService: EmploymentSessionService,
                                    val studentLoansService: StudentLoansService,
                                    employmentUserDataRepository: EmploymentUserDataRepository,
                                    view: UglAmountView,
                                    appConfig: AppConfig,
                                    errorHandler: ErrorHandler,
                                    ec: ExecutionContext,
                                    clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit user =>

    if (appConfig.studentLoansEnabled) {
      employmentUserDataRepository.find(taxYear, employmentId).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(optionCyaData) =>
          optionCyaData match {
            case Some(cyaData) =>
              val uglAmount = cyaData.employment.studentLoans.flatMap(_.uglDeductionAmount)
              val employerName = cyaData.employment.employmentDetails.employerName

              val form = uglAmount.fold(amountForm(employerName)
              )(amountForm(employerName).fill _)
              Ok(view(taxYear, form, employmentId, employerName))
            case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }

      }
    }

    else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit user =>

    if (appConfig.studentLoansEnabled) {
      val redirectUrl = StudentLoansCYAController.show(taxYear, employmentId).url
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        amountForm(cya.employment.employmentDetails.employerName).bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(view(taxYear, formWithErrors, employmentId, cya.employment.employmentDetails.employerName)))
          },
          amount => handleSuccessForm(taxYear, employmentId, cya, amount)
        )
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }



  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    studentLoansService.updateUglDeductionAmount(taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(_) => Redirect(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYear, employmentId))
    }
  }

  private def amountForm(employerName: String)(implicit user: User[_]): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"studentLoans.undergraduateLoanAmount.error.noEntry.${if (user.isAgent) "agent" else "individual"}",
    wrongFormatKey = "studentLoans.undergraduateLoanAmount.error.invalidFormat",
    emptyFieldArguments = Seq(employerName)
  )
}
