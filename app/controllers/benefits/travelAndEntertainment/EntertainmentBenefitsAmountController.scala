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

package controllers.benefits.travelAndEntertainment

import config.{AppConfig, ErrorHandler}
import controllers.benefits.utilitiesAndServices.routes.UtilitiesOrGeneralServicesBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{EmploymentSessionService, RedirectService}
import services.RedirectService.{EmploymentBenefitsType, entertainmentBenefitsAmountRedirects, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.EntertainmentBenefitsAmountView

import scala.concurrent.{ExecutionContext, Future}
import controllers.benefits.utilitiesAndServices.routes._

class EntertainmentBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                      authAction: AuthorisedAction,
                                                      inYearAction: InYearAction,
                                                      appConfig: AppConfig,
                                                      entertainmentBenefitsAmountView: EntertainmentBenefitsAmountView,
                                                      val employmentSessionService: EmploymentSessionService,
                                                      errorHandler: ErrorHandler,
                                                      ec: ExecutionContext,
                                                      clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils{

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(entertainmentBenefitsAmountRedirects(_,taxYear,employmentId)) { cya =>
          val cyaAmount: Option[BigDecimal] =
            cya.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertaining))

          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId)(
            employment =>
              employment.employmentBenefits.flatMap(_.benefits.flatMap(_.entertaining))
          )

          Future.successful(Ok(entertainmentBenefitsAmountView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya),
          EmploymentBenefitsType)(entertainmentBenefitsAmountRedirects(_, taxYear, employmentId)) { cya =>
          buildForm(user.isAgent).bindFromRequest().fold(
            { formWithErrors =>

              val fillValue = cya.employment.employmentBenefits.flatMap(_.travelEntertainmentModel).flatMap(_.entertaining)
              Future.successful(BadRequest(entertainmentBenefitsAmountView(taxYear, formWithErrors, fillValue, employmentId)))
            }, {
              newAmount:BigDecimal =>
                val cyaModel = cya.employment
                val benefits = cyaModel.employmentBenefits
                val travelEntertainment = cyaModel.employmentBenefits.flatMap(_.travelEntertainmentModel)

                val updatedCyaModel = cyaModel.copy(
                  employmentBenefits = benefits.map(_.copy(
                    travelEntertainmentModel = travelEntertainment.map(_.copy(entertaining = Some(newAmount)))))
                )

                employmentSessionService.createOrUpdateSessionData(
                  employmentId, updatedCyaModel, taxYear,  cya.isPriorSubmission, cya.hasPriorBenefits)(errorHandler.internalServerError()) {

                  val nextPage = UtilitiesOrGeneralServicesBenefitsController.show(taxYear, employmentId)

                  RedirectService.benefitsSubmitRedirect(updatedCyaModel, nextPage)(taxYear, employmentId)
                }
            }
          )
        }
      }
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"benefits.entertainmentBenefitAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
      s"benefits.entertainmentBenefitAmount.error.invalidFormat.${if (isAgent)"agent" else "individual"}"
      , s"benefits.entertainmentBenefitAmount.error.overMaximum.${if (isAgent)"agent" else "individual"}")
  }

}
