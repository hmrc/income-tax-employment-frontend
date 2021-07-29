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

import common.UUID
import config.AppConfig
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.{MultipleEmploymentsSummaryView, SingleEmploymentSummaryView, SingleEmploymentSummaryViewEOY}
import javax.inject.Inject
import models.User
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}
import controllers.employment.routes.EmployerNameController

class EmploymentSummaryController @Inject()(implicit val mcc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            implicit val appConfig: AppConfig,
                                            singleEmploymentSummaryView: SingleEmploymentSummaryView,
                                            multipleEmploymentsSummaryView: MultipleEmploymentsSummaryView,
                                            singleEmploymentSummaryEOYView: SingleEmploymentSummaryViewEOY,
                                            employmentSessionService: EmploymentSessionService,
                                            inYearAction: InYearAction
                                           ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "employment.addAnother.error"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    val isInYear: Boolean = inYearAction.inYear(taxYear)
    findPriorDataAndReturnResult(taxYear,isInYear,yesNoForm)
  }

  def findPriorDataAndReturnResult(taxYear: Int, isInYear: Boolean, yesNoForm: Form[Boolean])(implicit user: User[_]): Future[Result] ={

    val status = if(yesNoForm.hasErrors) BadRequest else Ok

    employmentSessionService.findPreviousEmploymentUserData(user, taxYear) { allEmploymentData =>

      val latestExpenses = employmentSessionService.getLatestExpenses(allEmploymentData, isInYear)
      val doExpensesExist = latestExpenses.isDefined

      val employmentData = employmentSessionService.getLatestEmploymentData(allEmploymentData, isInYear)

      employmentData match {
        case Seq() => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        case Seq(employment) if isInYear => status(singleEmploymentSummaryView(taxYear, employment, doExpensesExist))
        case Seq(employment) if !isInYear => status(singleEmploymentSummaryEOYView(taxYear, employment, yesNoForm))
        case _ => status(multipleEmploymentsSummaryView(taxYear, employmentData, doExpensesExist, isInYear, yesNoForm))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      yesNoForm.bindFromRequest().fold(
        formWithErrors => findPriorDataAndReturnResult(taxYear, false, formWithErrors),
        yesNo => {
          if (yesNo) {
            Future.successful(Redirect(EmployerNameController.show(taxYear, UUID.randomUUID)))
          } else {
            Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
        }
      )
    }
  }
}
