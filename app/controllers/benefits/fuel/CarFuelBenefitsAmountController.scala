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
import controllers.benefits.fuel.routes.{CompanyCarFuelBenefitsController, CompanyVanBenefitsController}
import controllers.employment.routes.CheckYourBenefitsController
import forms.FormUtils
import forms.benefits.fuel.FuelFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.RedirectService.{commonCarVanFuelBenefitsRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.FuelService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.fuel.CarFuelBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CarFuelBenefitsAmountController @Inject()(authAction: AuthorisedAction,
                                                pageView: CarFuelBenefitsAmountView,
                                                inYearAction: InYearUtil,
                                                employmentSessionService: EmploymentSessionService,
                                                fuelService: FuelService,
                                                errorHandler: ErrorHandler,
                                                formsProvider: FuelFormsProvider)
                                               (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, _) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel))

          val form = fillForm(formsProvider.carFuelAmountForm(request.user.isAgent), cyaAmount)
          Future.successful(Ok(pageView(taxYear, form, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          formsProvider.carFuelAmountForm(request.user.isAgent).bindFromRequest().fold(
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
    fuelService.updateCarFuel(request.user, taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = CompanyVanBenefitsController.show(taxYear, employmentId)
        RedirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    val cyaCarFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(
      ConditionalRedirect(cyaCarFuelQuestion.isEmpty, CompanyCarFuelBenefitsController.show(taxYear, employmentId)),
      ConditionalRedirect(cyaCarFuelQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), hasPrior = Some(false)),
      ConditionalRedirect(cyaCarFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPrior = Some(true))
    )
  }
}
