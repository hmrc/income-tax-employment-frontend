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

package controllers.benefits.accommodation

import actions.AuthorisedInYearAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.accommodation.routes._
import controllers.benefits.travel.routes._
import forms.YesNoForm
import models.{EmploymentUserDataRequest, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.RedirectService._
import services.benefits.accommodation.AccommodationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.accommodation.AccommodationRelocationBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccommodationRelocationBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                          authorisedInYearAction: AuthorisedInYearAction,
                                                          pageView: AccommodationRelocationBenefitsView,
                                                          appConfig: AppConfig, // TODO: (Hristo) This can be removed
                                                          accommodationService: AccommodationService,
                                                          errorHandler: ErrorHandler,
                                                          ec: ExecutionContext,
                                                          clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedInYearAction.build(taxYear, employmentId).async { implicit request =>
    Future.successful(Ok(pageView(yesNoForm, taxYear, employmentId, request.user)))
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedInYearAction.build(taxYear, employmentId).async { implicit request =>
    val form = yesNoForm.bindFromRequest()

    form.fold(
      formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear, employmentId, request.user))),
      yesNo => handleSuccessForm(taxYear, employmentId, request, yesNo)(request.user)
    )
  }

  private def handleSuccessForm(taxYear: Int,
                                employmentId: String,
                                employmentUserDataRequest: EmploymentUserDataRequest[AnyContent],
                                yesNo: Boolean)(implicit user: User[_]): Future[Result] = {
    accommodationService.updateSessionData(employmentUserDataRequest, employmentId, taxYear, yesNo).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (yesNo) {
          LivingAccommodationBenefitsController.show(taxYear, employmentId)
        } else {
          TravelOrEntertainmentBenefitsController.show(taxYear, employmentId)
        }
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def yesNoForm(implicit employmentUserDataRequest: EmploymentUserDataRequest[_]): Form[Boolean] = {
    val form = YesNoForm.yesNoForm(
      missingInputError = s"benefits.accommodationRelocation.error.${if (employmentUserDataRequest.user.isAgent) "agent" else "individual"}"
    )

    employmentUserDataRequest.employmentUserData.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.sectionQuestion)) match {
      case Some(questionResult) => form.fill(questionResult)
      case None => form
    }
  }
}
