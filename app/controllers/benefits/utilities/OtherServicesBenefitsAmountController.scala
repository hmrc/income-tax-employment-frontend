/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.benefits.medical.routes.MedicalDentalChildcareBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.benefits.utilities.UtilitiesFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.benefits.UtilitiesService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.utilities.OtherServicesBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OtherServicesBenefitsAmountController @Inject()(authAction: AuthorisedAction,
                                                      inYearAction: InYearUtil,
                                                      pageView: OtherServicesBenefitsAmountView,
                                                      employmentSessionService: EmploymentSessionService,
                                                      utilitiesService: UtilitiesService,
                                                      redirectService: RedirectService,
                                                      formsProvider: UtilitiesFormsProvider,
                                                      errorHandler: ErrorHandler)
                                                     (implicit cc: MessagesControllerComponents, val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, _) =>

        redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(redirectService.servicesBenefitsAmountRedirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.service))
          val form = cyaAmount.fold(formsProvider.otherServicesBenefitsAmountForm(request.user.isAgent)) { amount =>
            formsProvider.otherServicesBenefitsAmountForm(request.user.isAgent).fill(amount)
          }
          Future.successful(Ok(pageView(taxYear, form, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya),
          EmploymentBenefitsType)(redirectService.servicesBenefitsAmountRedirects(_, taxYear, employmentId)) { cya =>
          formsProvider.otherServicesBenefitsAmountForm(request.user.isAgent).bindFromRequest().fold(
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
    utilitiesService.updateService(request.user, taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = MedicalDentalChildcareBenefitsController.show(taxYear, employmentId)
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }
}
