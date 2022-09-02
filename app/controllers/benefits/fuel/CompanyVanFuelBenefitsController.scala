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

package controllers.benefits.fuel

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.fuel.routes.{CompanyVanFuelBenefitsAmountController, ReceiveOwnCarMileageBenefitController}
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
import views.html.benefits.fuel.CompanyVanFuelBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyVanFuelBenefitsController @Inject()(authAction: AuthorisedAction,
                                                 inYearAction: InYearUtil,
                                                 companyVanFuelBenefitsView: CompanyVanFuelBenefitsView,
                                                 employmentSessionService: EmploymentSessionService,
                                                 fuelService: FuelService,
                                                 redirectService: RedirectService,
                                                 errorHandler: ErrorHandler,
                                                 formsProvider: FuelFormsProvider)
                                                (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(redirectService.vanFuelBenefitsRedirects(_, taxYear, employmentId)) { cya =>
          val vanFuelBenefitQuestion = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion))
          val isAgent = request.user.isAgent
          vanFuelBenefitQuestion match {
            case Some(questionResult) =>
              Future.successful(Ok(companyVanFuelBenefitsView(formsProvider.companyVanFuelForm(isAgent).fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(companyVanFuelBenefitsView(formsProvider.companyVanFuelForm(isAgent), taxYear, employmentId)))
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
          EmploymentBenefitsType)(redirectService.vanFuelBenefitsRedirects(_, taxYear, employmentId)) { cya =>

          formsProvider.companyVanFuelForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(companyVanFuelBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, cya, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    fuelService.updateVanFuelQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage =
          if (questionValue) {
            CompanyVanFuelBenefitsAmountController.show(taxYear, employmentId)
          } else {
            ReceiveOwnCarMileageBenefitController.show(taxYear, employmentId)
          }
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }
}



