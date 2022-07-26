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

package controllers.benefits.medical

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.medical.routes._
import forms.FormUtils
import forms.benefits.medical.MedicalFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, educationalServicesAmountRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.MedicalService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.medical.EducationalServicesBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EducationalServicesBenefitsAmountController @Inject()(authAction: AuthorisedAction,
                                                            inYearAction: InYearUtil,
                                                            pageView: EducationalServicesBenefitsAmountView,
                                                            employmentSessionService: EmploymentSessionService,
                                                            medicalService: MedicalService,
                                                            errorHandler: ErrorHandler,
                                                            formsProvider: MedicalFormsProvider)
                                                           (implicit appConfig: AppConfig, mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, _) =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.educationalServices))
          val form = fillForm(formsProvider.educationalServicesAmountForm(request.user.isAgent), cyaAmount)
          Future.successful(Ok(pageView(taxYear, form, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, cya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          formsProvider.educationalServicesAmountForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              Future.successful(BadRequest(pageView(taxYear, formWithErrors, employmentId)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount))
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    medicalService.updateEducationalServices(request.user, taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = BeneficialLoansBenefitsController.show(taxYear, employmentId)
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    educationalServicesAmountRedirects(cya, taxYear, employmentId)
  }
}
