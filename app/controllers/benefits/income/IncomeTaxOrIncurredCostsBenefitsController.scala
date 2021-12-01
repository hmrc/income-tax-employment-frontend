/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.benefits.income

import config.{AppConfig, ErrorHandler}
import controllers.benefits.income.routes.IncomeTaxBenefitsController
import controllers.benefits.reimbursed.routes.ReimbursedCostsVouchersAndNonCashBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.benefits.IncomeTaxAndCostsModel
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentCYAModel
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RedirectService._
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.income.IncomeTaxOrIncurredCostsBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxOrIncurredCostsBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                           authAction: AuthorisedAction,
                                                           inYearAction: InYearAction,
                                                           incomeTaxOrIncurredCostsBenefitsView: IncomeTaxOrIncurredCostsBenefitsView,
                                                           appConfig: AppConfig,
                                                           employmentSessionService: EmploymentSessionService,
                                                           errorHandler: ErrorHandler,
                                                           ec: ExecutionContext,
                                                           clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.incomeTaxOrIncurredCosts.error.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    RedirectService.incomeTaxAndCostsRedirects(cya, taxYear, employmentId)
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          cya.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel.flatMap(_.sectionQuestion)) match {
            case Some(questionResult) => Future.successful(Ok(incomeTaxOrIncurredCostsBenefitsView(yesNoForm.fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(incomeTaxOrIncurredCostsBenefitsView(yesNoForm, taxYear, employmentId)))
          }
        }
      }
    }
  }


  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { data =>

          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(incomeTaxOrIncurredCostsBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => {
              val cya = data.employment
              val benefits = cya.employmentBenefits
              val incomeTaxOrCosts = cya.employmentBenefits.flatMap(_.incomeTaxAndCostsModel)

              val updatedCyaModel: EmploymentCYAModel = {
                incomeTaxOrCosts match {
                  case Some(incomeTaxOrCosts) if yesNo =>
                    cya.copy(employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel =
                      Some(incomeTaxOrCosts.copy(sectionQuestion = Some(true))))))
                  case Some(_) =>
                    cya.copy(employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel =
                      Some(IncomeTaxAndCostsModel.clear))))
                  case _ =>
                    cya.copy(employmentBenefits = benefits.map(_.copy(incomeTaxAndCostsModel =
                      Some(IncomeTaxAndCostsModel(sectionQuestion = Some(yesNo))))))
                }
              }

              employmentSessionService.createOrUpdateSessionData(
                employmentId, updatedCyaModel, taxYear, data.isPriorSubmission, data.hasPriorBenefits)(errorHandler.internalServerError()) {
                val nextPage = {
                  if (yesNo) {
                    IncomeTaxBenefitsController.show(taxYear, employmentId)
                  } else {
                    ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYear, employmentId)
                  }
                }

                RedirectService.benefitsSubmitRedirect(updatedCyaModel, nextPage)(taxYear, employmentId)              }
            }
          )
        }
      }
    }
  }
}
