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

package controllers.benefits.utilities

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.utilities.routes.ProfessionalSubscriptionsBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.FormUtils
import forms.benefits.utilities.UtilitiesFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.benefits.UtilitiesService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.utilities.EmployerProvidedServicesBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerProvidedServicesBenefitsAmountController @Inject()(authAction: AuthorisedAction,
                                                                 inYearAction: InYearUtil,
                                                                 pageView: EmployerProvidedServicesBenefitsAmountView,
                                                                 employmentSessionService: EmploymentSessionService,
                                                                 utilitiesService: UtilitiesService,
                                                                 redirectService: RedirectService,
                                                                 formsProvider: UtilitiesFormsProvider,
                                                                 errorHandler: ErrorHandler)
                                                                (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, _) =>
        redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServices))
          val form = fillForm(formsProvider.employerProvidedServicesBenefitsAmountForm(request.user.isAgent), cyaAmount)
          Future.successful(Ok(pageView(taxYear, form, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(CheckYourBenefitsController.show(taxYear, employmentId).url) { cya =>
        redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          formsProvider.employerProvidedServicesBenefitsAmountForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              Future.successful(BadRequest(pageView(taxYear, formWithErrors, employmentId)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    utilitiesService.updateEmployerProvidedServices(request.user, taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = ProfessionalSubscriptionsBenefitsController.show(taxYear, employmentId)
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    redirectService.employerProvidedServicesAmountRedirects(cya, taxYear, employmentId)
  }
}
