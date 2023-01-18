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

package controllers.benefits.reimbursed

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.assets.routes.AssetsOrAssetTransfersBenefitsController
import controllers.benefits.reimbursed.routes.OtherBenefitsAmountController
import forms.benefits.reimbursed.ReimbursedFormsProvider
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.benefits.ReimbursedService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.reimbursed.OtherBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OtherBenefitsController @Inject()(authAction: AuthorisedAction,
                                        inYearAction: InYearUtil,
                                        pageView: OtherBenefitsView,
                                        employmentSessionService: EmploymentSessionService,
                                        reimbursedService: ReimbursedService,
                                        redirectService: RedirectService,
                                        errorHandler: ErrorHandler,
                                        formsProvider: ReimbursedFormsProvider)
                                       (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(redirectService.otherItemsRedirects(_, taxYear, employmentId)) { cya =>

          cya.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.otherItemsQuestion)) match {
            case Some(questionResult) =>
              Future.successful(Ok(pageView(formsProvider.otherBenefitsForm(request.user.isAgent).fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(pageView(formsProvider.otherBenefitsForm(request.user.isAgent), taxYear, employmentId)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectService.redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(redirectService.otherItemsRedirects(_, taxYear, employmentId)) { data =>

          formsProvider.otherBenefitsForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    reimbursedService.updateOtherItemsQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage =
          if (questionValue) {
            OtherBenefitsAmountController.show(taxYear, employmentId)
          } else {
            AssetsOrAssetTransfersBenefitsController.show(taxYear, employmentId)
          }
        redirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }
}
