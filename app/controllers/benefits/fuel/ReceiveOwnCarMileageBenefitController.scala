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

package controllers.benefits.fuel

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.accommodation.routes.AccommodationRelocationBenefitsController
import controllers.benefits.fuel.routes.MileageBenefitAmountController
import controllers.employment.routes.CheckYourBenefitsController
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.benefits.FuelService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.fuel.ReceiveOwnCarMileageBenefitView

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class ReceiveOwnCarMileageBenefitController @Inject()(authAction: AuthorisedAction,
                                                      inYearAction: InYearUtil,
                                                      receiveOwnCarMileageBenefitView: ReceiveOwnCarMileageBenefitView,
                                                      employmentSessionService: EmploymentSessionService,
                                                      fuelService: FuelService,
                                                      redirectService: RedirectService,
                                                      errorHandler: ErrorHandler,
                                                      formsProvider: FuelFormsProvider)
                                                     (implicit cc: MessagesControllerComponents, val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
        case Right(optCya) =>
          redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
            EmploymentBenefitsType)(redirectService.mileageBenefitsRedirects(_, taxYear, employmentId)) { cya =>
            val mileageBenefitQuestion = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion))
            val isAgent = request.user.isAgent
            mileageBenefitQuestion match {
              case Some(questionResult) =>
                successful(Ok(receiveOwnCarMileageBenefitView(formsProvider.receiveOwnCarMileageForm(isAgent).fill(questionResult), taxYear, employmentId)))
              case None => successful(Ok(receiveOwnCarMileageBenefitView(formsProvider.receiveOwnCarMileageForm(isAgent), taxYear, employmentId)))
            }
          }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl: String = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>

        redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya),
          EmploymentBenefitsType)(redirectService.mileageBenefitsRedirects(_, taxYear, employmentId)) { cya =>

          formsProvider.receiveOwnCarMileageForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => successful(BadRequest(receiveOwnCarMileageBenefitView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, cya, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    fuelService.updateMileageQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = {
          if (questionValue) {
            MileageBenefitAmountController.show(taxYear, employmentId)
          } else {
            AccommodationRelocationBenefitsController.show(taxYear, employmentId)
          }
        }
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }
}
