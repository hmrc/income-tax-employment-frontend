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

package controllers.benefits.reimbursed

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.benefits.ReimbursedCostsVouchersAndNonCashModel
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
import views.html.benefits.reimbursed.ReimbursedCostsVouchersAndNonCashBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReimbursedCostsVouchersAndNonCashBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                                    authAction: AuthorisedAction,
                                                                    inYearAction: InYearAction,
                                                                    pageView: ReimbursedCostsVouchersAndNonCashBenefitsView,
                                                                    appConfig: AppConfig,
                                                                    employmentSessionService: EmploymentSessionService,
                                                                    errorHandler: ErrorHandler,
                                                                    ec: ExecutionContext,
                                                                    clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.reimbursedCostsVouchersAndNonCash.error.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    RedirectService.reimbursedCostsVouchersAndNonCashRedirects(cya, taxYear, employmentId)
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          cya.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.sectionQuestion)) match {
            case Some(questionResult) => Future.successful(Ok(pageView(yesNoForm.fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(pageView(yesNoForm, taxYear, employmentId)))
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
            formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear, employmentId))),
            yesNo => {
              val cya = data.employment
              val benefits = cya.employmentBenefits
              val reimbursedCostsModel = cya.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel)

              val updatedCyaModel: EmploymentCYAModel = {
                reimbursedCostsModel match {
                  case Some(reimbursedCosts) if yesNo =>
                    cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
                      Some(reimbursedCosts.copy(sectionQuestion = Some(true))))))
                  case Some(_) =>
                    cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
                      Some(ReimbursedCostsVouchersAndNonCashModel.clear))))
                  case _ =>
                    cya.copy(employmentBenefits = benefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel =
                      Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(yesNo))))))
                }
              }

              employmentSessionService.createOrUpdateSessionData(
                employmentId, updatedCyaModel, taxYear, data.isPriorSubmission, data.hasPriorBenefits)(errorHandler.internalServerError()) {
                Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
              }
            }
          )
        }
      }
    }
  }
}
