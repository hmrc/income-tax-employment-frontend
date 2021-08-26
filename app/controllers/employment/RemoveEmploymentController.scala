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
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.{IncomeTaxUserData, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DeleteOrIgnoreEmploymentService, EmploymentSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.RemoveEmploymentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveEmploymentController @Inject()(implicit val cc: MessagesControllerComponents,
                                           authAction: AuthorisedAction,
                                           inYearAction: InYearAction,
                                           removeEmploymentView: RemoveEmploymentView,
                                           appConfig: AppConfig,
                                           employmentSessionService: EmploymentSessionService,
                                           deleteOrIgnoreEmploymentService: DeleteOrIgnoreEmploymentService,
                                           errorHandler: ErrorHandler,
                                           ec: ExecutionContext
                                          ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def form(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "employment.removeEmployment.error.no-entry"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { allEmploymentData =>
        val source = employmentSessionService.employmentSourceToUse(allEmploymentData, employmentId, isInYear = false)
        source match {
          case Some((source, _)) => val employerName = source.employerName
            Ok(removeEmploymentView(form, taxYear, employmentId, employerName))
          case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getPriorData(taxYear).flatMap {
        case Right(IncomeTaxUserData(Some(allEmploymentData))) =>
          val source = employmentSessionService.employmentSourceToUse(allEmploymentData, employmentId, isInYear = false)
          source match {
            case Some((source, _)) => val employerName = source.employerName
              form.bindFromRequest().fold(
                { formWithErrors =>
                  Future.successful(BadRequest(removeEmploymentView(formWithErrors, taxYear, employmentId, employerName)))
                },
                {
                  case true =>
                    deleteOrIgnoreEmploymentService.deleteOrIgnoreEmployment(user, allEmploymentData, taxYear, employmentId) {
                        Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear))
                    }
                  case false => Future.successful(Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear)))
                }
              )
            case None => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
        case Right(IncomeTaxUserData(None)) =>
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case Left(error) => Future.successful(errorHandler.handleError(error.status))
      }

    }
  }

}
