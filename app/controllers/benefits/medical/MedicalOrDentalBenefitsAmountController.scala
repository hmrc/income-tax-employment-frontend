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

package controllers.benefits.medical

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.medical.routes._
import forms.benefits.medical.MedicalFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.benefits.MedicalService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.medical.MedicalOrDentalBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MedicalOrDentalBenefitsAmountController @Inject()(authAction: AuthorisedAction,
                                                        inYearAction: InYearUtil,
                                                        pageView: MedicalOrDentalBenefitsAmountView,
                                                        employmentSessionService: EmploymentSessionService,
                                                        medicalService: MedicalService,
                                                        redirectService: RedirectService,
                                                        errorHandler: ErrorHandler,
                                                        formsProvider: MedicalFormsProvider)
                                                       (implicit val appConfig: AppConfig, mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, _) =>
        redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance))
          val form = cyaAmount.fold(formsProvider.medicalOrAmountForm(request.user.isAgent)) { amount =>
            formsProvider.medicalOrAmountForm(request.user.isAgent).fill(amount)
          }
          Future(Ok(pageView(taxYear, form, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionData(taxYear, employmentId, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
        case Right(cya) =>
          redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, cya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

            formsProvider.medicalOrAmountForm(request.user.isAgent).bindFromRequest().fold(
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
    medicalService.updateMedicalInsurance(request.user, taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = ChildcareBenefitsController.show(taxYear, employmentId)
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    redirectService.medicalInsuranceAmountRedirects(cya, taxYear, employmentId)
  }
}
