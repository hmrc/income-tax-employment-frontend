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
import controllers.benefits.travel.routes._
import forms.YesNoForm
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService._
import services.benefits.AccommodationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.accommodation.AccommodationRelocationBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccommodationRelocationBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                          authAction: AuthorisedAction,
                                                          inYearAction: InYearUtil,
                                                          accommodationRelocationBenefitsView: AccommodationRelocationBenefitsView,
                                                          appConfig: AppConfig,
                                                          employmentSessionService: EmploymentSessionService,
                                                          accommodationService: AccommodationService,
                                                          errorHandler: ErrorHandler,
                                                          ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.findEmploymentUserData(taxYear, employmentId, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
        case Right(optEmploymentUserData) =>
          redirectBasedOnCurrentAnswers(taxYear, employmentId, optEmploymentUserData, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
            cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.sectionQuestion)) match {
              case Some(questionResult) => Future.successful(Ok(accommodationRelocationBenefitsView(yesNoForm.fill(questionResult), taxYear, employmentId)))
              case None => Future.successful(Ok(accommodationRelocationBenefitsView(yesNoForm, taxYear, employmentId)))
            }
          }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { data =>

          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(accommodationRelocationBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
          )
        }
      }
    }
  }

  private def yesNoForm(implicit request: AuthorisationRequest[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.accommodationRelocation.error.${if (request.user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    accommodationRelocationBenefitsRedirects(cya, taxYear, employmentId)
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, sectionQuestionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    accommodationService.updateSectionQuestion(request.user, taxYear, employmentId, employmentUserData, sectionQuestionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (sectionQuestionValue) {
          LivingAccommodationBenefitsController.show(taxYear, employmentId)
        }
        else {
          TravelOrEntertainmentBenefitsController.show(taxYear, employmentId)
        }
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }
}
