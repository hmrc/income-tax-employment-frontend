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

package controllers.benefits.accommodation

import config.{AppConfig, ErrorHandler}
import controllers.benefits.accommodation.routes._
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import javax.inject.Inject
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RedirectService.redirectBasedOnCurrentAnswers
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.accommodation.LivingAccommodationBenefitAmountView

import scala.concurrent.{ExecutionContext, Future}

class LivingAccommodationBenefitAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                           authAction: AuthorisedAction,
                                                           inYearAction: InYearAction,
                                                           appConfig: AppConfig,
                                                           livingAccommodationBenefitAmountView: LivingAccommodationBenefitAmountView,
                                                           val employmentSessionService: EmploymentSessionService,
                                                           errorHandler: ErrorHandler,
                                                           ec: ExecutionContext,
                                                           clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation))

          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId)(
            employment =>
              employment.employmentBenefits.flatMap(_.benefits.flatMap(_.accommodation))
          )
          Future(Ok(livingAccommodationBenefitAmountView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          buildForm(user.isAgent).bindFromRequest().fold(
            { formWithErrors =>
              val cyaLivingAccommodationAmount = cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation))
              Future.successful(BadRequest(livingAccommodationBenefitAmountView(taxYear, formWithErrors, cyaLivingAccommodationAmount, employmentId)))
            }, {
              amount =>

                val cyaModel = cya.employment
                val benefits = cyaModel.employmentBenefits
                val accomodationRelocation = benefits.flatMap(_.accommodationRelocationModel)

                val updatedCyaModel = cyaModel.copy(
                  employmentBenefits = benefits.map(_.copy(accommodationRelocationModel = accomodationRelocation.map(_.copy(accommodation = Some(amount)))))
                )

                employmentSessionService.createOrUpdateSessionData(employmentId, updatedCyaModel, taxYear,
                  isPriorSubmission = cya.isPriorSubmission, hasPriorBenefits = cya.hasPriorBenefits)(errorHandler.internalServerError()) {

                  val nextPage = QualifyingRelocationBenefitsController.show(taxYear, employmentId)

                  RedirectService.benefitsSubmitRedirect(updatedCyaModel, nextPage)(taxYear, employmentId)
                }
            }
          )
        }
      }
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"benefits.livingAccommodationAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
      s"benefits.livingAccommodationAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}"
      , s"benefits.livingAccommodationAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}")
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String) = {
    RedirectService.accommodationBenefitsAmountRedirects(cya, taxYear, employmentId)
  }
}
