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

package controllers.benefits.income

import config.{AppConfig, ErrorHandler}
import controllers.benefits.income.routes.IncurredCostsBenefitsAmountController
import controllers.benefits.reimbursed.routes.ReimbursedCostsVouchersAndNonCashBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, incurredCostsPaidByEmployerRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.IncomeService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.income.IncurredCostsBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncurredCostsBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                inYearAction: InYearAction,
                                                incurredCostsBenefitsView: IncurredCostsBenefitsView,
                                                appConfig: AppConfig,
                                                employmentSessionService: EmploymentSessionService,
                                                incomeService: IncomeService,
                                                errorHandler: ErrorHandler,
                                                clock: Clock
                                               ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  private implicit val ec: ExecutionContext = cc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          cya.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.paymentsOnEmployeesBehalfQuestion)) match {
            case Some(questionResult) => Future.successful(Ok(incurredCostsBenefitsView(buildForm.fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(incurredCostsBenefitsView(buildForm, taxYear, employmentId)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { data =>

          buildForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(incurredCostsBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit user: User[_]): Future[Result] = {
    incomeService.updatePaymentsOnEmployeesBehalfQuestion(taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (questionValue) {
          IncurredCostsBenefitsAmountController.show(taxYear, employmentId)
        } else {
          ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYear, employmentId)
        }
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def buildForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.incurredCosts.error.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String) = {
    incurredCostsPaidByEmployerRedirects(cya, taxYear, employmentId)
  }
}
