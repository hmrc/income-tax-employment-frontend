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

package controllers.benefits.accommodation

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.accommodation.routes._
import forms.benefits.accommodation.AccommodationFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, commonAccommodationBenefitsRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.AccommodationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.accommodation.LivingAccommodationBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LivingAccommodationBenefitsController @Inject()(authAction: AuthorisedAction,
                                                      inYearAction: InYearUtil,
                                                      livingAccommodationBenefitsView: LivingAccommodationBenefitsView,
                                                      employmentSessionService: EmploymentSessionService,
                                                      accommodationService: AccommodationService,
                                                      errorHandler: ErrorHandler,
                                                      formsProvider: AccommodationFormsProvider)
                                                     (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val isAgent = request.user.isAgent
          cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationQuestion)) match {
            case Some(questionResult) =>
              Future.successful(Ok(livingAccommodationBenefitsView(formsProvider.livingAccommodationForm(isAgent).fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(livingAccommodationBenefitsView(formsProvider.livingAccommodationForm(isAgent), taxYear, employmentId)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { data =>
          formsProvider.livingAccommodationForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(livingAccommodationBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    accommodationService.updateAccommodationQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (questionValue) {
          LivingAccommodationBenefitAmountController.show(taxYear, employmentId)
        } else {
          QualifyingRelocationBenefitsController.show(taxYear, employmentId)
        }
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    commonAccommodationBenefitsRedirects(cya, taxYear, employmentId)
  }
}
