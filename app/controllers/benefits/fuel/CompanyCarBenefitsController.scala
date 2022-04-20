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
import controllers.benefits.fuel.routes.{CompanyCarBenefitsAmountController, CompanyVanBenefitsController}
import forms.YesNoForm
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, carBenefitsRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.FuelService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.fuel.CompanyCarBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// TODO: This controller does not have enough test coverage in CompanyCarBenefitsControllerISpec
class CompanyCarBenefitsController @Inject()(authAction: AuthorisedAction,
                                             inYearAction: InYearUtil,
                                             companyCarBenefitsView: CompanyCarBenefitsView,
                                             employmentSessionService: EmploymentSessionService,
                                             fuelService: FuelService,
                                             errorHandler: ErrorHandler)
                                            (implicit val cc: MessagesControllerComponents, appConfig: AppConfig)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  private implicit val ec: ExecutionContext = cc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId)(handleShow(taxYear, employmentId, _))
    }
  }

  def handleShow(taxYear: Int, employmentId: String, optCya: Option[EmploymentUserData])
                (implicit request: AuthorisationRequest[_]): Future[Result] = {
    redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

      cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) match {
        case Some(value) =>
          Future.successful(Ok(companyCarBenefitsView(buildForm(request.user.isAgent).fill(value), taxYear, employmentId)))
        case None => Future.successful(Ok(companyCarBenefitsView(buildForm(request.user.isAgent), taxYear, employmentId)))
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { data =>

          buildForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(companyCarBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    fuelService.updateCarQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = {
          if (questionValue) CompanyCarBenefitsAmountController.show(taxYear, employmentId) else CompanyVanBenefitsController.show(taxYear, employmentId)
        }

        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def buildForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"CompanyCarBenefits.error.${if (isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    carBenefitsRedirects(cya, taxYear, employmentId)
  }
}
