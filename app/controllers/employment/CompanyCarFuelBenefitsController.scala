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
import views.html.employment.{ CompanyCarFuelBenefitsView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyCarFuelBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 inYearAction: InYearAction,
                                                 companyCarFuelBenefitsView: CompanyCarFuelBenefitsView,
                                                 appConfig: AppConfig,
                                                 employmentSessionService: EmploymentSessionService,
                                                 errorHandler: ErrorHandler,
                                                 ec: ExecutionContext,
                                                 clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.companyCarFuelBenefits.error.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).map {
        case Some(data) =>
          data.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion)) match {
            case Some(questionResult) => Ok(companyCarFuelBenefitsView(yesNoForm.fill(questionResult), taxYear, employmentId))
            case None => Ok(companyCarFuelBenefitsView(yesNoForm, taxYear, employmentId))
          }
        case None => Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
      }
    }
  }

  def submit(taxYear:Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionData(taxYear, employmentId).flatMap {
        case Some(data) =>
          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(companyCarFuelBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => {
              val cya = data.employment
              val updatedCyaModel: Option[EmploymentCYAModel] = {
                cya.employmentBenefits.flatMap(_.carVanFuelModel) match {
                  case Some(model) =>
                    if(yesNo){
                      Some(cya.copy(employmentBenefits = cya.employmentBenefits.map(_.copy(
                        carVanFuelModel = Some(model.copy(carFuelQuestion = Some(true)))
                      ))))
                    } else {
                      Some(cya.copy(employmentBenefits = cya.employmentBenefits.map(_.copy(
                        carVanFuelModel = Some(model.copy(carFuelQuestion = Some(false), carFuel = None))
                      ))))
                    }
                  case None =>
                    //                  TODO: Need to potentially update this to make a cya or something
                    None
                }
              }

              if(updatedCyaModel.isDefined) {
                employmentSessionService.createOrUpdateSessionData(
                  employmentId, updatedCyaModel.get, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
                }
              } else {
                Future(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
              }
            }
          )
        case None => Future(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
      }
    }
  }
}
