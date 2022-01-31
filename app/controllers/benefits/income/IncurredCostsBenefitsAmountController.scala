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

package controllers.benefits.income

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.reimbursed.routes.ReimbursedCostsVouchersAndNonCashBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.{AmountForm, FormUtils}
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, incurredCostsPaidByEmployerAmountRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.IncomeService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.benefits.income.IncurredCostsBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncurredCostsBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                      authAction: AuthorisedAction,
                                                      inYearAction: InYearUtil,
                                                      incurredCostsBenefitsAmountView: IncurredCostsBenefitsAmountView,
                                                      appConfig: AppConfig,
                                                      val employmentSessionService: EmploymentSessionService,
                                                      incomeService: IncomeService,
                                                      errorHandler: ErrorHandler,
                                                      ec: ExecutionContext,
                                                      clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.paymentsOnEmployeesBehalf))
          val form = fillFormFromPriorAndCYA(amountForm, prior, cyaAmount, employmentId)(
            employment => employment.employmentBenefits.flatMap(_.benefits.flatMap(_.paymentsOnEmployeesBehalf))
          )
          Future.successful(Ok(incurredCostsBenefitsAmountView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          amountForm.bindFromRequest().fold(
            formWithErrors => {
              val cyaAmount = cya.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.paymentsOnEmployeesBehalf))
              Future.successful(BadRequest(incurredCostsBenefitsAmountView(taxYear, formWithErrors, cyaAmount, employmentId)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    incomeService.updatePaymentsOnEmployeesBehalf(taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYear, employmentId)
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def amountForm(implicit user: User[_]): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.incurredCostsAmount.error.noEntry.${if (user.isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.incurredCostsAmount.error.incorrectFormat.${if (user.isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.incurredCostsAmount.error.overMaximum.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    incurredCostsPaidByEmployerAmountRedirects(cya, taxYear, employmentId)
  }
}
