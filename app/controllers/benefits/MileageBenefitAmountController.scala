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
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import forms.FormUtils.fillForm
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.MileageBenefitAmountView

import scala.concurrent.Future

class MileageBenefitAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               mileageBenefitAmountView: MileageBenefitAmountView,
                                               inYearAction: InYearAction,
                                               appConfig: AppConfig,
                                               val employmentSessionService: EmploymentSessionService,
                                               errorHandler: ErrorHandler,
                                               clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {


  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>

            val cyaMileageQuestion: Option[Boolean] = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion))

            cyaMileageQuestion match {
              case Some(true) =>

                val cyaAmount: Option[BigDecimal] = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage))

                val form = fillFormFromPriorAndCYA(buildForm(user.isAgent),prior,cyaAmount,employmentId)(
                  employment =>
                    employment.employmentBenefits.flatMap(_.benefits.flatMap(_.mileage))
                )

                Future.successful(Ok(mileageBenefitAmountView(taxYear, form,
                  cyaAmount, cya.employment.employmentDetails.employerName, employmentId)))

              case _ => Future.successful(RedirectService.mileageRedirect(cya.employment,taxYear,employmentId,cya.isPriorSubmission))
            }

          case None => Future.successful(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
    AmountForm.amountForm(s"benefits.mileageBenefitAmount.error.empty.${if (isAgent) "agent" else "individual"}",
      s"benefits.mileageBenefitAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
      s"benefits.mileageBenefitAmount.error.amountMaxLimit.${if (isAgent) "agent" else "individual"}")
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>

        val cyaMileageQuestion: Option[Boolean] = cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion))

        cyaMileageQuestion match {
          case Some(true) =>

            buildForm(user.isAgent).bindFromRequest().fold(
              { formWithErrors =>

                Future.successful(BadRequest(mileageBenefitAmountView(taxYear, formWithErrors,
                  cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)),
                  cya.employment.employmentDetails.employerName, employmentId)))

              }, {
                amount =>

                  val cyaModel = cya.employment
                  val benefits = cyaModel.employmentBenefits
                  val carVanFuel = cyaModel.employmentBenefits.flatMap(_.carVanFuelModel)

                  val updatedCyaModel = cyaModel.copy(
                    employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(mileage = Some(amount)))))
                  )

                  employmentSessionService.createOrUpdateSessionData(employmentId, updatedCyaModel, taxYear,
                    isPriorSubmission = cya.isPriorSubmission)(errorHandler.internalServerError()) {
                    RedirectService.mileageRedirect(updatedCyaModel,taxYear,employmentId,cya.isPriorSubmission)
                  }
              }
            )

          case _ => Future.successful(RedirectService.mileageRedirect(cya.employment,taxYear,employmentId,cya.isPriorSubmission))

//          case Some(false) => Future.successful(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
//          case None => Future.successful(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
        }
      }
    }
  }
}