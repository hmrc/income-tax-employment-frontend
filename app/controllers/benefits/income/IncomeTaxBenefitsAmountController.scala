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
import controllers.employment.routes.CheckYourBenefitsController
import controllers.benefits.income.routes.IncurredCostsBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RedirectService.redirectBasedOnCurrentAnswers
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.income.IncomeTaxBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  inYearAction: InYearAction,
                                                  incomeTaxBenefitsAmountView: IncomeTaxBenefitsAmountView,
                                                  appConfig: AppConfig,
                                                  val employmentSessionService: EmploymentSessionService,
                                                  errorHandler: ErrorHandler,
                                                  clock: Clock
                                                 ) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  implicit val ec: ExecutionContext = cc.executionContext

  def amountForm(implicit user: User[_]): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.incomeTaxBenefitsAmount.error.noEntry.${if (user.isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.incomeTaxBenefitsAmount.error.incorrectFormat.${if (user.isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.incomeTaxBenefitsAmount.error.overMaximum.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String) = {
    RedirectService.incomeTaxPaidByDirectorAmountRedirects(cya, taxYear, employmentId)
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirector))

          val form = fillFormFromPriorAndCYA(amountForm, prior, cyaAmount, employmentId)(
            employment =>
              employment.employmentBenefits.flatMap(_.benefits.flatMap(_.incomeTaxPaidByDirector))
          )
          Future.successful(Ok(incomeTaxBenefitsAmountView(taxYear, form, cyaAmount, employmentId)))
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
            { formWithErrors =>
              val cyaAmount = cya.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirector))
              Future.successful(BadRequest(incomeTaxBenefitsAmountView(taxYear, formWithErrors, cyaAmount, employmentId)))
            }, {
              amount =>

                val cyaModel = cya.employment
                val benefits = cyaModel.employmentBenefits
                val incomeTaxModel = benefits.flatMap(_.incomeTaxAndCostsModel)

                val updatedCyaModel = cyaModel.copy(
                  employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel =
                    incomeTaxModel.map(_.copy(incomeTaxPaidByDirector = Some(amount)))))
                )

                employmentSessionService.createOrUpdateSessionData(employmentId, updatedCyaModel, taxYear,
                  isPriorSubmission = cya.isPriorSubmission, hasPriorBenefits = cya.hasPriorBenefits)(errorHandler.internalServerError()) {

                  if (cya.isPriorSubmission) {
                    Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
                  } else {
                    Redirect(IncurredCostsBenefitsController.show(taxYear, employmentId))
                  }
                }
            }
          )
        }
      }
    }


  }

}