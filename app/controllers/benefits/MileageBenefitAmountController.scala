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

package controllers.benefits

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import javax.inject.Inject
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RedirectService._
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.MileageBenefitAmountView

import scala.concurrent.Future

class MileageBenefitAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               mileageBenefitAmountView: MileageBenefitAmountView,
                                               inYearAction: InYearAction,
                                               appConfig: AppConfig,
                                               val employmentSessionService: EmploymentSessionService,
                                               errorHandler: ErrorHandler,
                                               clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    RedirectService.mileageBenefitsAmountRedirects(cya,taxYear,employmentId)
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear){

      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_,taxYear,employmentId))
        { cya =>

          val cyaAmount: Option[BigDecimal] = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage))

          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId)(
            employment =>
              employment.employmentBenefits.flatMap(_.benefits.flatMap(_.mileage))
          )

          Future.successful(Ok(mileageBenefitAmountView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"benefits.mileageBenefitAmount.error.empty.${if (isAgent) "agent" else "individual"}",
      s"benefits.mileageBenefitAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
      s"benefits.mileageBenefitAmount.error.amountMaxLimit.${if (isAgent) "agent" else "individual"}")
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_,taxYear,employmentId))
        { cya =>

          buildForm(user.isAgent).bindFromRequest().fold(
            { formWithErrors =>

              Future.successful(BadRequest(mileageBenefitAmountView(taxYear, formWithErrors,
                cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)), employmentId)))

            },{
              amount =>

                val cyaModel = cya.employment
                val benefits = cyaModel.employmentBenefits
                val carVanFuel = cyaModel.employmentBenefits.flatMap(_.carVanFuelModel)

                val updatedCyaModel = cyaModel.copy(
                  employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(mileage = Some(amount)))))
                )

                employmentSessionService.createOrUpdateSessionData(employmentId, updatedCyaModel, taxYear,
                  isPriorSubmission = cya.isPriorSubmission)(errorHandler.internalServerError()) {

                  if (cya.isPriorSubmission) {
                    Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
                  } else {
                    //TODO Update to accommodation controller
                    Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
                  }
                }
            }
          )
        }
      }
    }
  }
}
