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

package controllers.benefits.assets

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import controllers.benefits.assets.routes.AssetTransfersBenefitsController
import forms.benefits.assets.AssetsFormsProvider
import models.UserSessionDataRequest
import models.benefits.pages.{AssetsBenefitsAmountPage => PageModel}
import models.employment.EmploymentBenefitsType
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.RedirectService
import services.benefits.AssetsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.benefits.assets.AssetsBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AssetsBenefitsAmountController @Inject()(actionsProvider: ActionsProvider,
                                               pageView: AssetsBenefitsAmountView,
                                               assetsService: AssetsService,
                                               redirectService: RedirectService,
                                               errorHandler: ErrorHandler,
                                               formsProvider: AssetsFormsProvider)
                                              (implicit cc: MessagesControllerComponents, val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionDataWithRedirects(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentBenefitsType,
    clazz = classOf[AssetsBenefitsAmountController]
  ) { implicit request =>
    val form = formsProvider.assetsAmountForm(request.user.isAgent)
    Ok(pageView(PageModel(taxYear, employmentId, request.user, form, request.employmentUserData)))
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = actionsProvider.endOfYearSessionDataWithRedirects(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentBenefitsType,
    clazz = classOf[AssetsBenefitsAmountController]
  ).async { implicit request =>
    formsProvider.assetsAmountForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(PageModel(taxYear, employmentId, request.user, formWithErrors, request.employmentUserData)))),
      amount => handleSuccessForm(taxYear, employmentId, amount)
    )
  }

  private def handleSuccessForm(taxYear: Int,
                                employmentId: String,
                                amount: BigDecimal)
                               (implicit request: UserSessionDataRequest[_]): Future[Result] = {
    assetsService.updateAssets(request.user, taxYear, employmentId, request.employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = AssetTransfersBenefitsController.show(taxYear, employmentId)
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }
}
