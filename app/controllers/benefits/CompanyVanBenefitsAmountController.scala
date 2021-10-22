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
import controllers.benefits.routes.CompanyVanFuelBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{EmploymentSessionService, RedirectService}
import services.RedirectService.{ConditionalRedirect, EmploymentBenefitsType, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.benefits.CompanyVanBenefitsAmountView
import utils.{Clock, SessionHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyVanBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                   authAction: AuthorisedAction,
                                                   inYearAction: InYearAction,
                                                   appConfig: AppConfig,
                                                   companyVanBenefitsAmountView: CompanyVanBenefitsAmountView,
                                                   val employmentSessionService: EmploymentSessionService,
                                                   errorHandler: ErrorHandler,
                                                   ec: ExecutionContext,
                                                   clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils{

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_,taxYear,employmentId))
        { cya =>

          val cyaAmount: Option[BigDecimal] = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.van))

          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId)(
            employment =>
              employment.employmentBenefits.flatMap(_.benefits.flatMap(_.van))
          )

          Future.successful(Ok(companyVanBenefitsAmountView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          buildForm(user.isAgent).bindFromRequest().fold(
            { formWithErrors =>
              val fillValue = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel).flatMap(_.van)
              Future.successful(BadRequest(companyVanBenefitsAmountView(taxYear, formWithErrors, fillValue, employmentId)))
            }, {
              newAmount =>
                val cyaModel = cya.employment
                val benefits = cyaModel.employmentBenefits
                val carVanFuel = cyaModel.employmentBenefits.flatMap(_.carVanFuelModel)
                val updatedCyaModel = cyaModel.copy(
                  employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(van = Some(newAmount)))))
                )
                employmentSessionService.createOrUpdateSessionData(
                  employmentId, updatedCyaModel, taxYear, cya.isPriorSubmission, cya.hasPriorBenefits)(errorHandler.internalServerError()) {

                  val nextPage = CompanyVanFuelBenefitsController.show(taxYear, employmentId)

                  RedirectService.benefitsSubmitRedirect(cya.hasPriorBenefits,updatedCyaModel,nextPage)(taxYear,employmentId)
                }
            }
          )
        }
      }
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"benefits.companyVanAmountBenefits.error.noEntry.${if (isAgent) "agent" else "individual"}",
      s"benefits.companyVanAmountBenefits.error.wrongFormat.${if (isAgent)"agent" else "individual"}"
      , s"benefits.companyVanAmountBenefits.error.overMaximum.${if (isAgent)"agent" else "individual"}")
  }

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    RedirectService.vanBenefitsAmountRedirects(cya,taxYear,employmentId)
  }

}
