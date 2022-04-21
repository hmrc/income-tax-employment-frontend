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

package controllers.benefits.fuel

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.fuel.routes.CompanyCarFuelBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.{AmountForm, FormUtils}
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.RedirectService.{carBenefitsAmountRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.FuelService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.fuel.CompanyCarBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyCarBenefitsAmountController @Inject()(authAction: AuthorisedAction,
                                                   companyCarBenefitsAmountView: CompanyCarBenefitsAmountView,
                                                   inYearAction: InYearUtil,
                                                   employmentSessionService: EmploymentSessionService,
                                                   fuelService: FuelService,
                                                   errorHandler: ErrorHandler)
                                                  (implicit val cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount: Option[BigDecimal] = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car))
          val form = fillFormFromPriorAndCYA(amountForm(request.user.isAgent), prior, cyaAmount, employmentId)(
            employment => employment.employmentBenefits.flatMap(_.benefits.flatMap(_.car))
          )

          Future.successful(Ok(companyCarBenefitsAmountView(form, taxYear, employmentId, cyaAmount)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          amountForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              val cyaCarAmount = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car))
              Future.successful(BadRequest(companyCarBenefitsAmountView(formWithErrors, taxYear, employmentId, cyaCarAmount)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    fuelService.updateCar(request.user, taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = CompanyCarFuelBenefitsController.show(taxYear, employmentId)
        RedirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def amountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.companyCarBenefitsAmount.error.no-entry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.companyCarBenefitsAmount.error.incorrect-format.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.companyCarBenefitsAmount.error.max-length.${if (isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    carBenefitsAmountRedirects(cya, taxYear, employmentId)
  }
}
