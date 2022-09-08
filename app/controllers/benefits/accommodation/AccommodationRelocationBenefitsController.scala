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

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import controllers.benefits.accommodation.routes._
import controllers.benefits.travel.routes._
import forms.benefits.accommodation.AccommodationFormsProvider
import models.User
import models.benefits.pages.{AccommodationRelocationBenefitsPage => PageModel}
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.RedirectService
import services.benefits.AccommodationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.benefits.accommodation.AccommodationRelocationBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AccommodationRelocationBenefitsController @Inject()(actionsProvider: ActionsProvider,
                                                          pageView: AccommodationRelocationBenefitsView,
                                                          accommodationService: AccommodationService,
                                                          redirectService: RedirectService,
                                                          errorHandler: ErrorHandler,
                                                          formsProvider: AccommodationFormsProvider)
                                                         (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentBenefitsType,
    clazz = classOf[AccommodationRelocationBenefitsController]
  ).async { implicit request =>
    val form = formsProvider.accommodationRelocationForm(request.user.isAgent)
    Future.successful(Ok(pageView(PageModel(taxYear, employmentId, request.user, form, request.employmentUserData))))
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentBenefitsType,
    clazz = classOf[AccommodationRelocationBenefitsController]
  ).async { implicit request =>
    formsProvider.accommodationRelocationForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(PageModel(taxYear, employmentId, request.user, formWithErrors, request.employmentUserData)))),
      yesNo => handleSuccessForm(taxYear, employmentId, request.employmentUserData, request.user, yesNo)
    )
  }

  private def handleSuccessForm(taxYear: Int,
                                employmentId: String,
                                employmentUserData: EmploymentUserData,
                                user: User,
                                sectionQuestionValue: Boolean)
                               (implicit request: Request[_]): Future[Result] = {
    accommodationService.saveSectionQuestion(user, taxYear, employmentId, employmentUserData, sectionQuestionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (sectionQuestionValue) {
          LivingAccommodationBenefitsController.show(taxYear, employmentId)
        } else {
          TravelOrEntertainmentBenefitsController.show(taxYear, employmentId)
        }
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }
}
