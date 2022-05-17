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

import actions.ActionsProvider
import common.{SessionValues, UUID}
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.EmployerNameController
import forms.employment.SelectEmployerForm
import models.UserPriorDataRequest
import models.employment.EmploymentSource
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{EmploymentSessionService, UnignoreEmploymentService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.SelectEmployerView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectEmployerController @Inject()(actionsProvider: ActionsProvider,
                                         selectEmployerView: SelectEmployerView,
                                         unignoreEmploymentService: UnignoreEmploymentService,
                                         employmentSessionService: EmploymentSessionService,
                                         errorHandler: ErrorHandler,
                                         selectEmployerForm: SelectEmployerForm)
                                        (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.notInYearWithPriorData(taxYear) { implicit request =>
    val ignoredEmployments = request.employmentPriorData.ignoredEmployments
    val prefilledForm: Form[String] = {
      val form = selectEmployerForm.employerListForm(ignoredEmployments.map(_.employmentId))
      idInSession.fold(form)(_ => form.fill(SessionValues.ADD_A_NEW_EMPLOYER))
    }

    if (ignoredEmployments.isEmpty) {
      employerNameRedirect(taxYear)
    } else {
      Ok(selectEmployerView(taxYear, ignoredEmployments.map(_.toEmployerView), prefilledForm))
    }
  }

  private def idInSession(implicit request: UserPriorDataRequest[_]): Option[String] = getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID)

  private def employerNameRedirect(taxYear: Int)(implicit request: UserPriorDataRequest[_]): Result = {
    idInSession.fold {
      val id = UUID.randomUUID
      Redirect(EmployerNameController.show(taxYear, id)).addingToSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> id)
    } { id =>
      Redirect(controllers.employment.routes.EmployerNameController.show(taxYear, id))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.notInYearWithPriorData(taxYear).async { implicit request =>
    val ignoredEmployments = request.employmentPriorData.ignoredEmployments

    if (ignoredEmployments.isEmpty) {
      Future.successful(employerNameRedirect(taxYear))
    } else {
      val form = selectEmployerForm.employerListForm(ignoredEmployments.map(_.employmentId))
      handleForm(form, taxYear, ignoredEmployments)
    }
  }

  private def handleForm(form: Form[String], taxYear: Int, ignoredEmployments: Seq[EmploymentSource])
                        (implicit request: UserPriorDataRequest[_]): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(selectEmployerView(taxYear, ignoredEmployments.map(_.toEmployerView), formWithErrors))),
      employer => if (employer == SessionValues.ADD_A_NEW_EMPLOYER) {
        Future.successful(employerNameRedirect(taxYear))
      } else {
        val employmentSource = request.employmentPriorData.hmrcEmploymentSourceWith(employer).get.employmentSource
        unignoreEmploymentService.unignoreEmployment(request.user, taxYear, employmentSource).flatMap {
          case Left(error) => Future.successful(errorHandler.handleError(error.status))
          case Right(_) =>
            val redirect = Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear))

            idInSession.fold {
              employmentSessionService.clear(request.user, taxYear, employer, clearCYA = false).map {
                case Left(_) => errorHandler.internalServerError()
                case Right(_) => redirect
              }
            } {
              employmentSessionService.clear(request.user, taxYear, _).map {
                case Left(_) => errorHandler.internalServerError()
                case Right(_) => redirect.removingFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID)
              }
            }
        }
      }
    )
  }
}
