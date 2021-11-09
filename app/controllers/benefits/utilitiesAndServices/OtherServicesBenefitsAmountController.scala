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

package controllers.benefits.utilitiesAndServices

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckYourBenefitsController
import controllers.benefits.medicalChildcareEducation.routes.MedicalDentalChildcareBenefitsController

import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.employment.EmploymentBenefitsType
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.RedirectService.{redirectBasedOnCurrentAnswers, servicesBenefitsAmountRedirects}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.OtherServicesBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OtherServicesBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                      authAction: AuthorisedAction,
                                                      inYearAction: InYearAction,
                                                      appConfig: AppConfig,
                                                      otherServicesBenefitsAmountView: OtherServicesBenefitsAmountView,
                                                      val employmentSessionService: EmploymentSessionService,
                                                      errorHandler: ErrorHandler,
                                                      ec: ExecutionContext,
                                                      clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(servicesBenefitsAmountRedirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount: Option[BigDecimal] =
            cya.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.service))

          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId)(
            employment =>
              employment.employmentBenefits.flatMap(_.benefits.flatMap(_.service))
          )

          Future.successful(Ok(otherServicesBenefitsAmountView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya),
          EmploymentBenefitsType)(servicesBenefitsAmountRedirects(_, taxYear, employmentId)) { cya =>
          buildForm(user.isAgent).bindFromRequest().fold(
            { formWithErrors =>

              val fillValue = cya.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel).flatMap(_.service)
              Future.successful(BadRequest(otherServicesBenefitsAmountView(taxYear, formWithErrors, fillValue, employmentId)))
            }, {
              newAmount: BigDecimal =>
                val cyaModel = cya.employment
                val benefits = cyaModel.employmentBenefits
                val utilitiesServices = cyaModel.employmentBenefits.flatMap(_.utilitiesAndServicesModel)

                val updatedCyaModel = cyaModel.copy(
                  employmentBenefits = benefits.map(_.copy(
                    utilitiesAndServicesModel = utilitiesServices.map(_.copy(service = Some(newAmount)))))
                )

                employmentSessionService.createOrUpdateSessionData(
                  employmentId, updatedCyaModel, taxYear, cya.isPriorSubmission, cya.hasPriorBenefits)(errorHandler.internalServerError()) {
                  if (cya.isPriorSubmission) {
                    Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
                  } else {
                    Redirect(MedicalDentalChildcareBenefitsController.show(taxYear, employmentId))
                  }
                }
            }
          )
        }
      }
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"benefits.otherServicesBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
      s"benefits.otherServicesBenefitsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}"
      , s"benefits.otherServicesBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}")
  }

}
