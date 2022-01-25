/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.employment.routes.EmploymentSummaryController
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.IncomeTaxUserData
import models.employment.EmploymentSourceOrigin
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.employment.RemoveEmploymentService
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
                                           removeEmploymentService: RemoveEmploymentService,
                                           errorHandler: ErrorHandler,
                                           ec: ExecutionContext
                                          ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { allEmploymentData =>
        allEmploymentData.eoyEmploymentSourceWith(employmentId) match {
          case Some(EmploymentSourceOrigin(source, _)) => val employerName = source.employerName
            Ok(removeEmploymentView(taxYear, employmentId, employerName, allEmploymentData.isLastEOYEmployment))
          case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getPriorData(taxYear).flatMap {
        case Left(error) => Future.successful(errorHandler.handleError(error.status))
        case Right(IncomeTaxUserData(None)) => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case Right(IncomeTaxUserData(Some(allEmploymentData))) =>
          allEmploymentData.eoyEmploymentSourceWith(employmentId) match {
            case Some(EmploymentSourceOrigin(_, _)) => removeEmploymentService.deleteOrIgnoreEmployment(allEmploymentData, taxYear, employmentId) {
              Redirect(EmploymentSummaryController.show(taxYear))
            }
            case None => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
      }
    }
  }
}
