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
import controllers.employment.routes.CheckYourBenefitsController
import controllers.benefits.routes.{AccommodationRelocationBenefitsController, CompanyCarBenefitsController, CompanyCarFuelBenefitsController}
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RedirectService.{ConditionalRedirect, EmploymentBenefitsType, redirectBasedOnCurrentAnswers}
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.CompanyCarBenefitsAmountView
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

class CompanyCarBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                   authAction: AuthorisedAction,
                                                   companyCarBenefitsAmountView: CompanyCarBenefitsAmountView,
                                                   inYearAction: InYearAction,
                                                   appConfig: AppConfig,
                                                   val employmentSessionService: EmploymentSessionService,
                                                   errorHandler: ErrorHandler,
                                                   ec: ExecutionContext,
                                                   clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils{

  def amountForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(
      s"benefits.companyCarBenefitsAmount.error.no-entry.${if (isAgent) "agent" else "individual"}",
      s"benefits.companyCarBenefitsAmount.error.incorrect-format.${if (isAgent) "agent" else "individual"}",
      s"benefits.companyCarBenefitsAmount.error.max-length.${if (isAgent) "agent" else "individual"}"
    )
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    RedirectService.carBenefitsAmountRedirects(cya,taxYear,employmentId)
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear){

      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_,taxYear,employmentId))
        { cya =>

          val cyaAmount: Option[BigDecimal] = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car))

          val form = fillFormFromPriorAndCYA(amountForm(user.isAgent), prior, cyaAmount, employmentId)(
            employment =>
              employment.employmentBenefits.flatMap(_.benefits.flatMap(_.car))
          )

          Future.successful(Ok(companyCarBenefitsAmountView(form, taxYear, employmentId, cyaAmount)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          amountForm(user.isAgent).bindFromRequest().fold(
            { formWithErrors =>
              val cyaCarAmount = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car))
              Future.successful(BadRequest(companyCarBenefitsAmountView(formWithErrors, taxYear, employmentId, cyaCarAmount)))
            }, {
              amount =>

                val cyaModel = cya.employment
                val benefits = cyaModel.employmentBenefits
                val carVanFuel = benefits.flatMap(_.carVanFuelModel)

                val updatedCyaModel = cyaModel.copy(
                  employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(car = Some(amount)))))
                )

                employmentSessionService.createOrUpdateSessionData(employmentId, updatedCyaModel, taxYear,
                  isPriorSubmission = cya.isPriorSubmission, cya.hasPriorBenefits)(errorHandler.internalServerError()) {

                  val nextPage = CompanyCarFuelBenefitsController.show(taxYear,employmentId)

                  RedirectService.benefitsSubmitRedirect(cya.hasPriorBenefits,updatedCyaModel,nextPage)(taxYear,employmentId)


//                  if (cya.isPriorSubmission) {
//                    Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
//                  } else {
//                    Redirect(CompanyCarFuelBenefitsController.show(taxYear, employmentId))
//                  }
                }
            }
          )
        }
      }
    }
  }
}
