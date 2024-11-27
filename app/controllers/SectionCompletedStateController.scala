/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import config.{AppConfig, ErrorHandler}
import actions.AuthorisedAction
import actions.TaxYearAction.taxYearAction
import forms.YesNoForm
import models.mongo.{JourneyAnswers, JourneyStatus}
import models.mongo.JourneyStatus.{Completed, InProgress}
import models.Journey
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import services.SectionCompletedService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.SectionCompletedStateView

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SectionCompletedStateController @Inject()(implicit val cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                view: SectionCompletedStateView,
                                                errorHandler: ErrorHandler,
                                                implicit val appConfig: AppConfig,
                                                sectionCompletedService: SectionCompletedService,
                                                ec: ExecutionContext
                                               ) extends FrontendController(cc) with I18nSupport {

  def form(): Form[Boolean] = YesNoForm.yesNoForm("sectionCompletedState.error.required")

  def show(taxYear: Int, journey: String): Action[AnyContent] =
    (authAction andThen taxYearAction(taxYear)).async { implicit user =>
      Journey.pathBindable.bind("journey", journey) match {
        case (Right(journeyType)) =>
          val journeyName = journeyType.toString
          sectionCompletedService.get(user.user.mtditid, taxYear, journeyName).flatMap {
            case Some(value) =>
              value.data("status").validate[JourneyStatus].asOpt match {
                case Some(JourneyStatus.Completed) =>
                  Future.successful(Ok(view(form().fill(true), taxYear, journeyName)))

                case Some(JourneyStatus.InProgress) =>
                  Future.successful(Ok(view(form().fill(false), taxYear, journeyName)))

                case _ => Future.successful(Ok(view(form(), taxYear, journeyName)))
              }
            case None => Future.successful(Ok(view(form(), taxYear, journeyName)))
          }
        case _ => Future.successful(errorHandler.handleError(BAD_REQUEST))
      }
    }

  def submit(taxYear: Int, journey: String): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit user =>
    form()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, journey))),
        answer => {
          val maybeJourney: Either[String, Journey] = Journey.pathBindable.bind("journey", journey)

          maybeJourney match {
            case (Right(journey)) => saveAndRedirect(answer, taxYear, journey, user.user.mtditid)
            case _ =>
              Future.successful(errorHandler.handleError(BAD_REQUEST))
          }

        }
      )
  }

  private def saveAndRedirect(answer: Boolean, taxYear: Int, journey: Journey, mtditid: String)(implicit hc: HeaderCarrier): Future[Result] = {
    val status: JourneyStatus = if (answer) Completed else InProgress
    val model = JourneyAnswers(mtditid, taxYear, journey.toString, Json.obj({
      "status" -> status
    }), Instant.now)
    sectionCompletedService.set(model)
    Future.successful(Redirect(appConfig.commonTaskListUrl(taxYear)))
  }

}
