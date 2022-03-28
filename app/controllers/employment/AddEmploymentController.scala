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

import actions.AuthorisedAction
import common.{SessionValues, UUID}
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.{EmployerNameController, SelectEmployerController}
import forms.YesNoForm
import models.{AuthorisationRequest, IncomeTaxUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.employment.AddEmploymentView
import javax.inject.Inject
import models.employment.AllEmploymentData

import scala.concurrent.{ExecutionContext, Future}

class AddEmploymentController @Inject()(implicit val cc: MessagesControllerComponents,
                                        authAction: AuthorisedAction,
                                        inYearAction: InYearUtil,
                                        addEmploymentView: AddEmploymentView,
                                        appConfig: AppConfig,
                                        employmentSessionService: EmploymentSessionService,
                                        errorHandler: ErrorHandler,
                                        ec: ExecutionContext
                                       ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    redirectOrRenderView(taxYear) { _ =>
      val form = if (getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID).isEmpty) buildForm else buildForm.fill(value = true)
      Future(Ok(addEmploymentView(form, taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    redirectOrRenderView(taxYear) { data =>
      submitForm(taxYear, data)
    }
  }

  private def submitForm(taxYear: Int, data: Option[AllEmploymentData])(implicit request: AuthorisationRequest[_]): Future[Result] = {
    val idInSession: Option[String] = getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID)

    def redirect: Future[Result] = {
      if(data.map(_.ignoredEmployments).getOrElse(Seq()).nonEmpty){
        Future.successful(Redirect(SelectEmployerController.show(taxYear)))
      } else {
        idInSession.fold {
          val id = UUID.randomUUID
          Future.successful(Redirect(EmployerNameController.show(taxYear, id)).addingToSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> id))
        } { id =>
          employmentSessionService.getSessionDataResult(taxYear, id) {
            _.fold(Future.successful(Redirect(EmployerNameController.show(taxYear, id))))(cya => Future.successful(
              employmentDetailsRedirect(cya.employment, taxYear, id, cya.isPriorSubmission)))
          }
        }
      }
    }

    buildForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(addEmploymentView(formWithErrors, taxYear))),
      {
        case true => redirect
        case _ =>
          val redirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          idInSession.fold(Future.successful(redirect))(employmentSessionService.clear(request.user, taxYear, _).map {
            case Left(_) => errorHandler.internalServerError()
            case Right(_) => redirect.removingFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID)
          })
      })
  }

  private def buildForm: Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "employment.addEmployment.error"
  )

  private def redirectOrRenderView(taxYear: Int)
                                  (block: Option[AllEmploymentData] => Future[Result])(implicit request: AuthorisationRequest[_]): Future[Result] = {
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getPriorData(request.user, taxYear).flatMap {
        case Right(IncomeTaxUserData(Some(data))) if data.latestEOYEmployments.nonEmpty =>
          Future(Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear)))
        case Right(data) => block(data.employment)
        case Left(error) => Future(errorHandler.handleError(error.status))
      }
    }
  }
}
