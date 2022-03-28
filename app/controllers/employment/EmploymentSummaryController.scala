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

import actions.{ActionsProvider, AuthorisedAction}
import common.{SessionValues, UUID}
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.AddEmploymentController
import models.AuthorisationRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.employment.EmploymentSummaryView
import controllers.employment.routes.SelectEmployerController
import controllers.employment.routes.EmployerNameController
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

class EmploymentSummaryController @Inject()(implicit val mcc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            implicit val appConfig: AppConfig,
                                            employmentSummaryView: EmploymentSummaryView,
                                            employmentSessionService: EmploymentSessionService,
                                            inYearAction: InYearUtil,
                                            errorHandler: ErrorHandler,
                                            actionsProvider: ActionsProvider
                                           ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  private implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    val isInYear: Boolean = inYearAction.inYear(taxYear)
    findPriorDataAndReturnResult(taxYear, isInYear)
  }

  private def findPriorDataAndReturnResult(taxYear: Int, isInYear: Boolean)
                                          (implicit request: AuthorisationRequest[_]): Future[Result] = {
    val overrideRedirect = if (isInYear) None else addEmploymentRedirect(taxYear)

    employmentSessionService.findPreviousEmploymentUserData(request.user, taxYear, overrideRedirect) { allEmploymentData =>
      val employmentData = if (isInYear) allEmploymentData.latestInYearEmployments else allEmploymentData.latestEOYEmployments
      lazy val latestExpenses = if (isInYear) allEmploymentData.latestInYearExpenses else allEmploymentData.latestEOYExpenses
      lazy val doExpensesExist = latestExpenses.isDefined

      employmentData match {
        case Seq() if isInYear => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        case Seq() if !isInYear => Redirect(AddEmploymentController.show(taxYear))
        case _ => Ok(employmentSummaryView(taxYear, employmentData, doExpensesExist, isInYear))
      }
    }
  }

  private def addEmploymentRedirect(taxYear: Int): Option[Result] = Some(Redirect(AddEmploymentController.show(taxYear)))

  def addNewEmployment(taxYear: Int): Action[AnyContent] = actionsProvider.notInYearWithPriorData(taxYear,
    addEmploymentRedirect(taxYear)).async { implicit request =>

    lazy val hasIgnoredEmployments = request.employmentPriorData.ignoredEmployments.nonEmpty

    val result = Redirect(if(hasIgnoredEmployments) SelectEmployerController.show(taxYear) else EmployerNameController.show(taxYear, UUID.randomUUID))

    getFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID).fold(Future.successful(result))(employmentSessionService.clear(request.user, taxYear, _).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(_) => Future.successful(result.removingFromSession(SessionValues.TEMP_NEW_EMPLOYMENT_ID))
    })
  }
}
