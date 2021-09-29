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

package controllers.benefits

import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction}
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.EmployerPayAmountView
import controllers.employment.routes.CheckYourBenefitsController
import forms.AmountForm
import play.api.data.Form

import scala.concurrent.Future

class MileageBenefitAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               employerPayAmountView: EmployerPayAmountView,
                                               inYearAction: InYearAction,
                                               appConfig: AppConfig,
                                               employmentSessionService: EmploymentSessionService,
                                               errorHandler: ErrorHandler,
                                               clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {


  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"mileageBenefitAmount.error.empty.${if (isAgent) "agent" else "individual"}",
      "mileageBenefitAmount.error.wrongFormat", "mileageBenefitAmount.error.amountMaxLimit")
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>

            val cyaAmount: Option[BigDecimal] = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage))

            val priorAmount: Option[BigDecimal] = prior.flatMap { priorEmp =>
              employmentSessionService.employmentSourceToUse(priorEmp, employmentId, isInYear = false).flatMap{
                employmentSource =>
                  employmentSource._1.employmentBenefits.flatMap(_.benefits.flatMap(_.mileage))
              }
            }

            lazy val unfilledForm = buildForm(user.isAgent)

            ???

          case None => Future.successful(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      ???
    }
  }
}