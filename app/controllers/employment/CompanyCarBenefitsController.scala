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
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.CompanyCarBenefitsView
import javax.inject.Inject
import services.RedirectService.{ConditionalRedirect, EmploymentBenefitsType, redirectBasedOnCurrentAnswers}

import scala.concurrent.{ExecutionContext, Future}

class CompanyCarBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             inYearAction: InYearAction,
                                             companyCarBenefitsView: CompanyCarBenefitsView,
                                             appConfig: AppConfig,
                                             employmentSessionService: EmploymentSessionService,
                                             errorHandler: ErrorHandler,
                                             clock: Clock
                                            ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  implicit val ec: ExecutionContext = cc.executionContext

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    RedirectService.carBenefitsRedirects(cya,taxYear,employmentId)
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId)(handleShow(taxYear, employmentId, _))
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { data =>

          buildForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(companyCarBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => {

              val cya = data.employment
              val benefits = cya.employmentBenefits
              val carVanFuel = cya.employmentBenefits.flatMap(_.carVanFuelModel)

              val updatedCyaModel: EmploymentCYAModel = {
                if (yesNo) {
                  cya.copy(employmentBenefits = benefits.map(_.copy(carVanFuelModel = carVanFuel.map(_.copy(carQuestion = Some(true))))))
                } else {
                  cya.copy(employmentBenefits = benefits.map(_.copy(
                    carVanFuelModel = carVanFuel.map(_.copy(carQuestion = Some(false), car = None, carFuelQuestion = None, carFuel = None)))))
                }
              }

              employmentSessionService.createOrUpdateSessionData(
                employmentId, updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
              }
            }
          )
        }
      }
    }
  }

  def handleShow(taxYear: Int, employmentId: String, optCya: Option[EmploymentUserData])
                (implicit user: User[_]): Future[Result] = {

    redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

      cya.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) match {
        case Some(value) => Future.successful(Ok(companyCarBenefitsView(buildForm.fill(value), taxYear, employmentId)))
        case None => Future.successful(Ok(companyCarBenefitsView(buildForm, taxYear, employmentId)))
      }
    }
  }

  private def buildForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"CompanyCarBenefits.error.${if(user.isAgent) "agent" else "individual"}"
  )
}
