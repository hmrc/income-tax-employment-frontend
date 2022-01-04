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

package controllers.benefits.reimbursed

import config.{AppConfig, ErrorHandler}
import controllers.benefits.reimbursed.routes.{NonTaxableCostsBenefitsAmountController, TaxableCostsBenefitsController}
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{EmploymentSessionService, RedirectService}
import services.RedirectService.{commonReimbursedCostsVouchersAndNonCashModelRedirects, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.reimbursed.NonTaxableCostsBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NonTaxableCostsBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                          authAction: AuthorisedAction,
                                          inYearAction: InYearAction,
                                          view: NonTaxableCostsBenefitsView,
                                          appConfig: AppConfig,
                                          employmentSessionService: EmploymentSessionService,
                                          errorHandler: ErrorHandler,
                                          ec: ExecutionContext,
                                          clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.nonTaxableCosts.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(commonReimbursedCostsVouchersAndNonCashModelRedirects(_, taxYear, employmentId)) { cya =>

          cya.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expensesQuestion)) match {
            case Some(questionResult) =>
              Future.successful(Ok(view(yesNoForm.fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(view(yesNoForm, taxYear, employmentId)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(commonReimbursedCostsVouchersAndNonCashModelRedirects(_, taxYear, employmentId)) { data =>

          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, employmentId))),
            yesNo => {
              val cya = data.employment
              val benefits = cya.employmentBenefits
              val reimbursedCostsVouchersAndNonCashModel = benefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

              val updatedCyaModel: EmploymentCYAModel = {
                if (yesNo) {
                  cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
                    reimbursedCostsVouchersAndNonCashModel.map(_.copy(expensesQuestion = Some(true))))))
                } else {
                  cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
                    reimbursedCostsVouchersAndNonCashModel.map(_.copy(expensesQuestion = Some(false), expenses = None)))))
                }
              }

              employmentSessionService.createOrUpdateSessionData(
                employmentId, updatedCyaModel, taxYear, data.isPriorSubmission, data.hasPriorBenefits)(errorHandler.internalServerError()) {
                val nextPage = {
                  if (yesNo) {
                    NonTaxableCostsBenefitsAmountController.show(taxYear, employmentId)
                  } else {
                    TaxableCostsBenefitsController.show(taxYear, employmentId)
                  }
                }

                RedirectService.benefitsSubmitRedirect(updatedCyaModel, nextPage)(taxYear, employmentId)
              }
            }
          )
        }
      }
    }
  }

}
