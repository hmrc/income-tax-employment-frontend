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
import forms.{AmountForm, FormUtils}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.CarFuelBenefitsAmountView
import controllers.employment.routes.{CheckYourBenefitsController, CompanyCarFuelBenefitsController}
import controllers.benefits.routes.CompanyVanBenefitsController
import models.mongo.EmploymentCYAModel
import services.RedirectService.{ConditionalRedirect, EmploymentBenefitsType, commonCarVanFuelBenefitsRedirects, redirectBasedOnCurrentAnswers}

import javax.inject.Inject
import scala.concurrent.Future

class CarFuelBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                carFuelBenefitsAmountView: CarFuelBenefitsAmountView,
                                                inYearAction: InYearAction,
                                                appConfig: AppConfig,
                                                val employmentSessionService: EmploymentSessionService,
                                                errorHandler: ErrorHandler,
                                                clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {


  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val cyaCarFuelQuestion: Option[Boolean] = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(
      ConditionalRedirect(cyaCarFuelQuestion.isEmpty, CompanyCarFuelBenefitsController.show(taxYear, employmentId)),
      ConditionalRedirect(cyaCarFuelQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
      ConditionalRedirect(cyaCarFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true))
    )
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear){

      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_,taxYear,employmentId))
        { cya =>

          val cyaAmount: Option[BigDecimal] = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel))

          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId)(
            employment =>
              employment.employmentBenefits.flatMap(_.benefits.flatMap(_.carFuel))
          )

          Future.successful(Ok(carFuelBenefitsAmountView(taxYear, form, employmentId, cyaAmount)))
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
              val cyaCarAmount = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel))
              Future.successful(BadRequest(carFuelBenefitsAmountView(taxYear, formWithErrors, employmentId, cyaCarAmount)))
            }, {
              amount =>

                val cyaModel = cya.employment
                val benefits = cyaModel.employmentBenefits
                val carVanFuel = benefits.flatMap(_.carVanFuelModel)

                val updatedCyaModel = cyaModel.copy(
                  employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(carFuel = Some(amount)))))
                )

                employmentSessionService.createOrUpdateSessionData(employmentId, updatedCyaModel, taxYear,
                  isPriorSubmission = cya.isPriorSubmission)(errorHandler.internalServerError()) {

                  if (cya.isPriorSubmission) {
                    Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
                  } else {
                    Redirect(CompanyVanBenefitsController.show(taxYear, employmentId))
                  }
                }
            }
          )
        }
      }
    }
  }

    private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
      AmountForm.amountForm(
        s"benefits.carFuelAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
        s"benefits.carFuelAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
        s"benefits.carFuelAmount.error.tooMuch.${if (isAgent) "agent" else "individual"}")
    }
  }
