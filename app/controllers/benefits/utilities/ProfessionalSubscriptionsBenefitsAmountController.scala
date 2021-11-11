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

package controllers.benefits.utilities

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import javax.inject.Inject
import models.benefits.UtilitiesAndServicesModel
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RedirectService.redirectBasedOnCurrentAnswers
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.ProfessionalSubscriptionsBenefitsAmountView

import scala.concurrent.{ExecutionContext, Future}


class ProfessionalSubscriptionsBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                                  authAction: AuthorisedAction,
                                                                  inYearAction: InYearAction,
                                                                  appConfig: AppConfig,
                                                                  professionalSubscriptionBenefitsView: ProfessionalSubscriptionsBenefitsAmountView,
                                                                  val employmentSessionService: EmploymentSessionService,
                                                                  errorHandler: ErrorHandler,
                                                                  ec: ExecutionContext,
                                                                  clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"benefits.professionalSubscriptionsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
      s"benefits.professionalSubscriptionsAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
      s"benefits.professionalSubscriptionsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}")
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String) = {
    RedirectService.employerProvidedSubscriptionsBenefitsAmountRedirects(cya, taxYear, employmentId)
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions))
          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId) { employment =>
            employment.employmentBenefits.flatMap(_.benefits.flatMap(_.employerProvidedProfessionalSubscriptions))
          }

          Future.successful(Ok(professionalSubscriptionBenefitsView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, cya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          buildForm(user.isAgent).bindFromRequest().fold(
            { formWithErrors =>
              val fillValue = cya.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel).flatMap(_.employerProvidedProfessionalSubscriptions)
              Future.successful(BadRequest(professionalSubscriptionBenefitsView(taxYear, formWithErrors, fillValue, employmentId)))
            }, {
              newAmount =>
                val cyaModel = cya.employment
                val utilitiesAndServices: Option[UtilitiesAndServicesModel] = cyaModel.employmentBenefits.flatMap(_.utilitiesAndServicesModel)
                val updatedCyaModel = cyaModel.copy(employmentBenefits = cyaModel.employmentBenefits.map(_.copy(utilitiesAndServicesModel =
                  utilitiesAndServices.map(_.copy(employerProvidedProfessionalSubscriptions = Some(newAmount))))))

                employmentSessionService.createOrUpdateSessionData(employmentId, updatedCyaModel, taxYear,
                  cya.isPriorSubmission, cya.hasPriorBenefits)(errorHandler.internalServerError()) {
                  if (cya.isPriorSubmission) {
                    Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
                  } else {
                    // TODO: Go to services question radio page
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
