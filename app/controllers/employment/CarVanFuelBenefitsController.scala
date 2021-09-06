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

package controllers.employment

import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.employment.CarVanFuelModel
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.CarVanFuelBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CarVanFuelBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                        authAction: AuthorisedAction,
                                        inYearAction: InYearAction,
                                        carVanFuelBenefitsView: CarVanFuelBenefitsView,
                                        appConfig: AppConfig,
                                        employmentSessionService: EmploymentSessionService,
                                        errorHandler: ErrorHandler,
                                        ec: ExecutionContext,
                                        clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.carVanFuel.error.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).map {
        case Some(data) =>
          data.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion)) match {
            case Some(questionResult) => Ok(carVanFuelBenefitsView(yesNoForm.fill(questionResult), taxYear, employmentId))
            case None => Ok(carVanFuelBenefitsView(yesNoForm, taxYear, employmentId))
          }
        case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }

  def submit(taxYear:Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).flatMap {
        case Some(data) =>
          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(carVanFuelBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => {
              val cya = data.employment
              val updatedCyaModel: EmploymentCYAModel = {
                cya.employmentBenefits.flatMap(_.carVanFuelModel) match {
                  case Some(model) =>
                    if(yesNo){
                      cya.copy(employmentBenefits = cya.employmentBenefits.map(_.copy(
                        carVanFuelModel = Some(model.copy(carVanFuelQuestion = Some(true)))
                      )))
                    } else {
                      cya.copy(employmentBenefits = cya.employmentBenefits.map(_.copy(
                        carVanFuelModel = Some(CarVanFuelModel.clear)
                      )))
                    }
                  case None =>
//                  TODO: Need to potentially update this to make a cya or something
                  cya
                }
              }

              employmentSessionService.createOrUpdateSessionData(
                employmentId, updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()){
                  Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
              }
            }
          )
        case None => Future(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
      }
    }
  }
}
