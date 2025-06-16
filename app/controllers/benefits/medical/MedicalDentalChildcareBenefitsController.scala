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
import controllers.benefits.income.routes._
import controllers.benefits.medical.routes._
import forms.benefits.medical.MedicalFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.benefits.MedicalService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.medical.MedicalDentalChildcareBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MedicalDentalChildcareBenefitsController @Inject()(authAction: AuthorisedAction,
                                                         inYearAction: InYearUtil,
                                                         pageView: MedicalDentalChildcareBenefitsView,
                                                         employmentSessionService: EmploymentSessionService,
                                                         medicalService: MedicalService,
                                                         redirectService: RedirectService,
                                                         errorHandler: ErrorHandler,
                                                         formsProvider: MedicalFormsProvider)
                                                        (implicit val appConfig: AppConfig, mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionData(taxYear, employmentId, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
        case Right(optCya) =>
          redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
            EmploymentBenefitsType)(redirectService.medicalBenefitsRedirects(_, taxYear, employmentId)) { cya =>

            cya.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) match {
              case Some(questionResult) =>
                Future.successful(Ok(pageView(formsProvider.medicalDentalChildcareForm(request.user.isAgent).fill(questionResult), taxYear, employmentId)))
              case None => Future.successful(Ok(pageView(formsProvider.medicalDentalChildcareForm(request.user.isAgent), taxYear, employmentId)))
            }
          }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionData(taxYear, employmentId, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
        case Right(optCya) =>
          redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
            EmploymentBenefitsType)(redirectService.medicalBenefitsRedirects(_, taxYear, employmentId)) { data =>

            formsProvider.medicalDentalChildcareForm(request.user.isAgent).bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear, employmentId))),
              yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
            )
          }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    medicalService.updateSectionQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (questionValue) {
          MedicalDentalBenefitsController.show(taxYear, employmentId)
        } else {
          IncomeTaxOrIncurredCostsBenefitsController.show(taxYear, employmentId)
        }
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }
}
