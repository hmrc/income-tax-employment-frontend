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

package controllers.benefits.assets

import config.{AppConfig, ErrorHandler}
import controllers.benefits.assets.routes.AssetTransfersBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{assetsAmountRedirects, benefitsSubmitRedirect, redirectBasedOnCurrentAnswers}
import services.benefits.AssetsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.assets.AssetsBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AssetsBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               inYearAction: InYearAction,
                                               pageView: AssetsBenefitsAmountView,
                                               appConfig: AppConfig,
                                               val employmentSessionService: EmploymentSessionService,
                                               assetsService: AssetsService,
                                               errorHandler: ErrorHandler,
                                               ec: ExecutionContext,
                                               clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assets))

          val form = fillFormFromPriorAndCYA(amountForm, prior, cyaAmount, employmentId)(
            employment => employment.employmentBenefits.flatMap(_.benefits.flatMap(_.assets))
          )
          Future.successful(Ok(pageView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          amountForm.bindFromRequest().fold(
            formWithErrors => {
              val cyaAmount = cya.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assets))
              Future.successful(BadRequest(pageView(taxYear, formWithErrors, cyaAmount, employmentId)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    assetsService.updateAssets(taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = AssetTransfersBenefitsController.show(taxYear, employmentId)
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def amountForm(implicit user: User[_]): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.assetsAmount.error.noEntry.${if (user.isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.assetsAmount.error.incorrectFormat.${if (user.isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.assetsAmount.error.overMaximum.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String) = {
    assetsAmountRedirects(cya, taxYear, employmentId)
  }
}
