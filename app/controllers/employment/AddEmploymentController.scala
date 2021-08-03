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

import common.{SessionValues, UUID}
import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.{IncomeTaxUserData, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.AddEmploymentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddEmploymentController @Inject()(implicit val cc: MessagesControllerComponents,
                                        authAction: AuthorisedAction,
                                        inYearAction: InYearAction,
                                        addEmploymentView: AddEmploymentView,
                                        appConfig: AppConfig,
                                        employmentSessionService: EmploymentSessionService,
                                        errorHandler: ErrorHandler,
                                        ec: ExecutionContext,
                                        clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {


  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    redirectOrRenderView(taxYear){
      val form = if (getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID).isEmpty) buildForm else buildForm.fill(value = true)
      Future(Ok(addEmploymentView(form, taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    redirectOrRenderView(taxYear){
      submitForm(taxYear)
    }
  }

  private def buildForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "AddEmployment.error"
  )


  private def submitForm(taxYear:Int)(implicit user:User[_]): Future[Result] = {
    buildForm.bindFromRequest().fold(
      { formWithErrors =>
        Future.successful(BadRequest(addEmploymentView(formWithErrors, taxYear)))
      },
      { radioValue =>
        radioValue match {
          case true => val id = UUID.randomUUID
            Future.successful(Redirect(controllers.employment.routes.EmployerNameController.show(taxYear, id))
              .addingToSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID ->id))
          case _ => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        }
      })
  }

  private def redirectOrRenderView(taxYear:Int)(block: => Future[Result])(implicit user:User[_]): Future[Result] = {
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getPriorData(taxYear).flatMap {
        case Right(IncomeTaxUserData(Some(_))) => Future(Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear)))
        case Right(IncomeTaxUserData(None)) => block
        case Left(error) => Future(errorHandler.handleError(error.status))
      }
    }
  }
}
