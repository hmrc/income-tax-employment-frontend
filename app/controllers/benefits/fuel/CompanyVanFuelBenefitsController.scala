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

package controllers.benefits.fuel

import config.{AppConfig, ErrorHandler}
import controllers.benefits.fuel.routes.{CompanyVanFuelBenefitsAmountController, ReceiveOwnCarMileageBenefitController}
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RedirectService.{redirectBasedOnCurrentAnswers, vanFuelBenefitsRedirects}
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.fuel.CompanyVanFuelBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyVanFuelBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 inYearAction: InYearAction,
                                                 companyVanFuelBenefitsView: CompanyVanFuelBenefitsView,
                                                 appConfig: AppConfig,
                                                 employmentSessionService: EmploymentSessionService,
                                                 errorHandler: ErrorHandler,
                                                 ec: ExecutionContext,
                                                 clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {
  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.companyVanFuelBenefits.error.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(vanFuelBenefitsRedirects(_, taxYear, employmentId)) { cya =>

          val vanFuelBenefitQuestion: Option[Boolean] =
            cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion))

          vanFuelBenefitQuestion match {
            case Some(questionResult) => Future.successful(Ok(companyVanFuelBenefitsView(yesNoForm.fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(companyVanFuelBenefitsView(yesNoForm, taxYear, employmentId)))
          }

        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl: String = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya),
          EmploymentBenefitsType)(vanFuelBenefitsRedirects(_, taxYear, employmentId)) { cya =>

          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(companyVanFuelBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => {
              val cyaModel: EmploymentCYAModel = cya.employment
              val benefits: Option[BenefitsViewModel] = cyaModel.employmentBenefits
              val carVanFuelModel: Option[CarVanFuelModel] = cyaModel.employmentBenefits.flatMap(_.carVanFuelModel)

              val updatedCyaModel = cyaModel.copy(
                employmentBenefits = benefits.map(_.copy(
                  carVanFuelModel =
                    if (yesNo) {
                      carVanFuelModel.map(_.copy(
                        vanFuelQuestion = Some(true)
                      ))
                    } else {
                      carVanFuelModel.map(_.copy(
                        vanFuelQuestion = Some(false), vanFuel = None
                      ))
                    }
                )))

              employmentSessionService.createOrUpdateSessionData(
                employmentId, updatedCyaModel, taxYear, cya.isPriorSubmission, cya.hasPriorBenefits)(errorHandler.internalServerError()) {
                val nextPage = {
                  if (yesNo) {
                    CompanyVanFuelBenefitsAmountController.show(taxYear, employmentId)
                  } else {
                    ReceiveOwnCarMileageBenefitController.show(taxYear, employmentId)
                  }
                }
                RedirectService.benefitsSubmitRedirect(updatedCyaModel, nextPage)(taxYear, employmentId)
              }
            }
          )
        }
      }
    }
  }
}



