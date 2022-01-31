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
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.studentLoans.PglAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PglAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                    authAction: AuthorisedAction,
                                    inYearAction: InYearAction,
                                    val employmentSessionService: EmploymentSessionService,
                                    view: PglAmountView,
                                    appConfig: AppConfig,
                                    errorHandler: ErrorHandler,
                                    ec: ExecutionContext,
                                    clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit user =>

      if(appConfig.studentLoansEnabled) {
        employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

          val cyaData = optCya.flatMap(_.employment.studentLoansCYAModel.flatMap(_.pglDeductionAmount))

          val form = fillFormFromPriorAndCYA(amountForm, prior, cyaData, employmentId)(
            employment => employment.employmentData.flatMap(_.deductions.flatMap(_.studentLoans.flatMap(_.pglDeductionAmount)))
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

  private def amountForm(implicit user: User[_]): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"studentLoans.pglAmount.error.noEntry.${if (user.isAgent) "agent" else "individual"}",
    wrongFormatKey = s"studentLoans.pglAmount.invalidFormat.${if (user.isAgent) "agent" else "individual"}"
  )

}
