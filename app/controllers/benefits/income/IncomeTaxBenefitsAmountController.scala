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
import controllers.benefits.income.routes.IncurredCostsBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.FormUtils
import forms.benefits.income.IncomeFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, incomeTaxPaidByDirectorAmountRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.IncomeService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.income.IncomeTaxBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxBenefitsAmountController @Inject()(authAction: AuthorisedAction,
                                                  inYearAction: InYearUtil,
                                                  incomeTaxBenefitsAmountView: IncomeTaxBenefitsAmountView,
                                                  employmentSessionService: EmploymentSessionService,
                                                  incomeService: IncomeService,
                                                  errorHandler: ErrorHandler,
                                                  formsProvider: IncomeFormsProvider)
                                                 (implicit val appConfig: AppConfig, mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  private implicit val ec: ExecutionContext = mcc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirector))
          val form = fillFormFromPriorAndCYA(formsProvider.incomeTaxAmountForm(request.user.isAgent), prior, cyaAmount, employmentId)(
            employment => employment.employmentBenefits.flatMap(_.benefits.flatMap(_.incomeTaxPaidByDirector))
          )
          Future.successful(Ok(incomeTaxBenefitsAmountView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          formsProvider.incomeTaxAmountForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              val cyaAmount = cya.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.incomeTaxPaidByDirector))
              Future.successful(BadRequest(incomeTaxBenefitsAmountView(taxYear, formWithErrors, cyaAmount, employmentId)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    incomeService.updateIncomeTaxPaidByDirector(request.user, taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = IncurredCostsBenefitsController.show(taxYear, employmentId)
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    incomeTaxPaidByDirectorAmountRedirects(cya, taxYear, employmentId)
  }
}
