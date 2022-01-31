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

package controllers.benefits.reimbursed

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.assets.routes.AssetsOrAssetTransfersBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.{AmountForm, FormUtils}
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, otherItemsAmountRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.ReimbursedService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.benefits.reimbursed.OtherBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OtherBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              inYearAction: InYearUtil,
                                              appConfig: AppConfig,
                                              pageView: OtherBenefitsAmountView,
                                              val employmentSessionService: EmploymentSessionService,
                                              reimbursedService: ReimbursedService,
                                              errorHandler: ErrorHandler,
                                              ec: ExecutionContext,
                                              clock: Clock
                                             ) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.otherItems))
          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId)(employment =>
            employment.employmentBenefits.flatMap(_.benefits.flatMap(_.otherItems))
          )

          Future.successful(Ok(pageView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(CheckYourBenefitsController.show(taxYear, employmentId).url) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          buildForm(user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              val fillValue = cya.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel).flatMap(_.otherItems)
              Future.successful(BadRequest(pageView(taxYear, formWithErrors, fillValue, employmentId)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount))
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    reimbursedService.updateOtherItems(taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = AssetsOrAssetTransfersBenefitsController.show(taxYear, employmentId)
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.otherBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.otherBenefitsAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = "benefits.otherBenefitsAmount.error.overMaximum"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    otherItemsAmountRedirects(cya, taxYear, employmentId)
  }
}
