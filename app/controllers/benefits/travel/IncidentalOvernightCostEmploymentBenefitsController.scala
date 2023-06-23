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

package controllers.benefits.travel

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.travel.routes._
import forms.benefits.travel.TravelFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.benefits.TravelService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.travel.IncidentalOvernightCostEmploymentBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncidentalOvernightCostEmploymentBenefitsController @Inject()(authAction: AuthorisedAction,
                                                                    inYearAction: InYearUtil,
                                                                    pageView: IncidentalOvernightCostEmploymentBenefitsView,
                                                                    employmentSessionService: EmploymentSessionService,
                                                                    travelService: TravelService,
                                                                    redirectService: RedirectService,
                                                                    formsProvider: TravelFormsProvider,
                                                                    errorHandler: ErrorHandler)
                                                                   (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionData(taxYear, employmentId, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
        case Right(optCya) =>
          redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

            cya.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion)) match {
              case Some(yesNo) => Future.successful(Ok(pageView(formsProvider.incidentalOvernightCostEmploymentBenefitsForm(
                request.user.isAgent).fill(yesNo), taxYear, employmentId)))
              case None => Future.successful(Ok(pageView(formsProvider.incidentalOvernightCostEmploymentBenefitsForm(
                request.user.isAgent), taxYear, employmentId)))
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
          redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { data =>

            formsProvider.incidentalOvernightCostEmploymentBenefitsForm(request.user.isAgent).bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear, employmentId))),
              yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
            )
          }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    travelService.updatePersonalIncidentalExpensesQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (questionValue) {
          IncidentalCostsBenefitsAmountController.show(taxYear, employmentId)
        } else {
          EntertainingBenefitsController.show(taxYear, employmentId)
        }
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    redirectService.incidentalCostsBenefitsRedirects(cya, taxYear, employmentId)
  }
}
