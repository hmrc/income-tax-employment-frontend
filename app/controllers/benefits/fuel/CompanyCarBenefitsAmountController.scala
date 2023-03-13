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

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import controllers.benefits.fuel.routes.CompanyCarFuelBenefitsController
import forms.benefits.fuel.FuelFormsProvider
import models.UserSessionDataRequest
import models.benefits.pages.{CompanyCarBenefitsAmountPage => PageModel}
import models.employment.EmploymentBenefitsType
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.RedirectService
import services.benefits.FuelService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.benefits.fuel.CompanyCarBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyCarBenefitsAmountController @Inject()(actionsProvider: ActionsProvider,
                                                   pageView: CompanyCarBenefitsAmountView,
                                                   fuelService: FuelService,
                                                   redirectService: RedirectService,
                                                   errorHandler: ErrorHandler,
                                                   formsProvider: FuelFormsProvider)
                                                  (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionDataWithRedirects(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentBenefitsType,
    clazz = classOf[CompanyCarBenefitsAmountController]
  ) { implicit request =>
    val form = formsProvider.companyCarAmountForm(request.user.isAgent)
    Ok(pageView(PageModel(taxYear, employmentId, request.user, form, request.employmentUserData)))
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionDataWithRedirects(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentBenefitsType,
    clazz = classOf[CompanyCarBenefitsAmountController]
  ).async { implicit request =>
    formsProvider.companyCarAmountForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(PageModel(taxYear, employmentId, request.user, formWithErrors, request.employmentUserData)))),
      amount => handleSuccessForm(taxYear, employmentId, amount)
    )
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, amount: BigDecimal)
                               (implicit request: UserSessionDataRequest[_]): Future[Result] = {
    fuelService.updateCar(request.user, taxYear, employmentId, request.employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = CompanyCarFuelBenefitsController.show(taxYear, employmentId)
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }
}
