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
import models.User
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

  val employerName = "ABC Digital"

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {
      val redirectUrl = appConfig.incomeTaxSubmissionOverviewUrl(taxYear)

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { data =>

        //look up previous employments for user
        //if they have employment data then they can view this page
        //insert the employer name into page view
      }
      Future.successful(Ok(removeEmploymentView(form, taxYear, employmentId, employerName)))
    }

  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { allEmploymentData =>

        ???
      }
      form.bindFromRequest().fold(
        { formWithErrors =>
          Future.successful(BadRequest(removeEmploymentView(formWithErrors, taxYear, employmentId, employerName)))
        },
        {
          case true =>
            employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { allEmploymentData =>
              deleteOrIgnoreEmploymentService.deleteOrIgnoreEmployment(user, allEmploymentData, taxYear, employmentId) {
                val isLastEmployment = allEmploymentData.hmrcEmploymentData.isEmpty && allEmploymentData.customerEmploymentData.isEmpty
                if (isLastEmployment) {
                  Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
                } else {
                  Future.successful(Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear)))
                }
              }
            }

          case false =>
            Future.successful(Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear)))
        }
      )
    }
  }

}
