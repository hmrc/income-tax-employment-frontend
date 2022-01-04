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
import controllers.benefits.assets.routes.AssetsTransfersBenefitsAmountController
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.employment.EmploymentBenefitsType
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.RedirectService.{assetTransferRedirects, benefitsSubmitRedirect, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.assets.AssetTransfersBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AssetTransfersBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 inYearAction: InYearAction,
                                                 view: AssetTransfersBenefitsView,
                                                 appConfig: AppConfig,
                                                 employmentSessionService: EmploymentSessionService,
                                                 errorHandler: ErrorHandler,
                                                 ec: ExecutionContext,
                                                 clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(assetTransferRedirects(_, taxYear, employmentId)) { cya =>
          cya.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assetTransferQuestion)) match {
            case Some(questionResult) => Future.successful(Ok(view(yesNoForm.fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(view(yesNoForm, taxYear, employmentId)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(assetTransferRedirects(_, taxYear, employmentId)) { data =>
          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, employmentId))),
            yesNo => {
              val cya = data.employment
              val benefits = cya.employmentBenefits
              val assetsModel = benefits.flatMap(_.assetsModel)

              val updatedCyaModel = if (yesNo) {
                cya.copy(employmentBenefits = benefits.map(_.copy(assetsModel = assetsModel.map(_.copy(assetTransferQuestion = Some(true))))))
              } else {
                cya.copy(employmentBenefits = benefits.map(_.copy(
                  assetsModel = assetsModel.map(_.copy(assetTransferQuestion = Some(false), assetTransfer = None)))))
              }

              employmentSessionService.createOrUpdateSessionData(
                employmentId,
                updatedCyaModel,
                taxYear,
                data.isPriorSubmission,
                data.hasPriorBenefits
              )(errorHandler.internalServerError()) {
                val nextPage = if (yesNo) {
                  AssetsTransfersBenefitsAmountController.show(taxYear, employmentId)
                } else {
                  CheckYourBenefitsController.show(taxYear, employmentId)
                }
                benefitsSubmitRedirect(updatedCyaModel, nextPage)(taxYear, employmentId)
              }
            }
          )
        }
      }
    }
  }

  private def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.assetTransfers.error.${if (user.isAgent) "agent" else "individual"}"
  )
}
