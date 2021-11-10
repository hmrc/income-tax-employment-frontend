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

package controllers.benefits.travel

import config.{AppConfig, ErrorHandler}
import controllers.benefits.travel.routes._
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import javax.inject.Inject
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RedirectService.redirectBasedOnCurrentAnswers
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.travel.TravelAndSubsistenceBenefitsView

import scala.concurrent.{ExecutionContext, Future}

class TravelAndSubsistenceBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                       authAction: AuthorisedAction,
                                                       inYearAction: InYearAction,
                                                       travelAndSubsistenceBenefitsView: TravelAndSubsistenceBenefitsView,
                                                       appConfig: AppConfig,
                                                       employmentSessionService: EmploymentSessionService,
                                                       errorHandler: ErrorHandler,
                                                       ec: ExecutionContext,
                                                       clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.travelAndSubsistence.error.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String) = {
    RedirectService.commonTravelEntertainmentBenefitsRedirects(cya, taxYear, employmentId)
  }

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          cya.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelAndSubsistenceQuestion)) match {
            case Some(questionResult) => Future.successful(Ok(travelAndSubsistenceBenefitsView(yesNoForm.fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(travelAndSubsistenceBenefitsView(yesNoForm, taxYear, employmentId)))
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
            formWithErrors => Future.successful(BadRequest(travelAndSubsistenceBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => {
              val cya = data.employment
              val benefits = cya.employmentBenefits
              val travelEntertainmentModel = cya.employmentBenefits.flatMap(_.travelEntertainmentModel)

              val updatedCyaModel: EmploymentCYAModel = {
                if (yesNo) {
                  cya.copy(employmentBenefits = benefits.map(_.copy(travelEntertainmentModel =
                    travelEntertainmentModel.map(_.copy(travelAndSubsistenceQuestion = Some(true))))))
                } else {
                  cya.copy(employmentBenefits = benefits.map(_.copy(
                    travelEntertainmentModel = travelEntertainmentModel.map(_.copy(travelAndSubsistenceQuestion = Some(false),
                      travelAndSubsistence = None)))))
                }
              }

              employmentSessionService.createOrUpdateSessionData(
                employmentId, updatedCyaModel, taxYear, data.isPriorSubmission,data.hasPriorBenefits)(errorHandler.internalServerError()) {

                val nextPage = {
                  if (yesNo) {
                    TravelOrSubsistenceBenefitsAmountController.show(taxYear, employmentId)
                  } else {
                    IncidentalOvernightCostEmploymentBenefitsController.show(taxYear, employmentId)
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
