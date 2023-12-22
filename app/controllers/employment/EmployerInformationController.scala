/*
 * Copyright 2023 HM Revenue & Customs
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
import config.AppConfig
import controllers.employment.routes._
import controllers.lumpSum.routes._
import controllers.studentLoans.routes._
import models.employment.EmploymentSourceOrigin
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.mvc.Call
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import viewmodels.employment._
import views.html.employment.EmployerInformationView

import javax.inject.Inject
import scala.Option.{unless, when}
import scala.concurrent.Future


class EmployerInformationController @Inject()(authAction: AuthorisedAction,
                                              pageView: EmployerInformationView,
                                              inYearAction: InYearUtil,
                                              employmentSessionService: EmploymentSessionService
                                             )(implicit cc: MessagesControllerComponents, val appConfig: AppConfig)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    if (!inYearAction.inYear(taxYear) && !appConfig.employmentEOYEnabled) {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      employmentSessionService.findPreviousEmploymentUserData(request.user, taxYear) { allEmploymentData =>
        val isInYear = inYearAction.inYear(taxYear)
        val source = if (isInYear) allEmploymentData.hmrcEmploymentSourceWith(employmentId) else allEmploymentData.eoyEmploymentSourceWith(employmentId)

        source match {
          case Some(EmploymentSourceOrigin(source, _)) =>
            val uglExists = source.employmentData.flatMap(_.deductions).flatMap(_.studentLoans).flatMap(_.uglDeductionAmount).isDefined
            val pglExists = source.employmentData.flatMap(_.deductions).flatMap(_.studentLoans).flatMap(_.pglDeductionAmount).isDefined
            val showNotification = !isInYear && !source.employmentDetailsSubmittable
            val lumpSumsDefined = allEmploymentData.otherEmploymentIncome.flatMap(_.lumpSums).isDefined

            val benefitsDefined = source.employmentBenefits.isDefined
            val studentLoansDefined = uglExists || pglExists

            val rows = makeRows(taxYear, employmentId, showNotification, benefitsDefined, studentLoansDefined, lumpSumsDefined)

            Ok(pageView(source.employerName, employmentId, rows, taxYear, isInYear, showNotification))
          case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }
    }
  }

  private def summaryEmployerInformationRowFor(labelKey: LabelMessageKey, isDefined: Boolean, maybeAction: Option[Call],
                                               isInYear: Boolean, showNotification: Boolean): EmployerInformationRow = {
    val status =
      if (showNotification) {
        CannotUpdate
      }
      else if (isDefined) {
        Updated
      }
      else if (isInYear) {
        CannotUpdate
      }
      else {
        NotStarted
      }
    val updateAvailable = if (showNotification) false else isDefined

    EmployerInformationRow(labelKey, status, maybeAction, updateAvailable)
  }

  private def makeRows(taxYear: Int, employmentId: String, showNotification: Boolean,
                       benefitsDefined: Boolean, studentLoansDefined: Boolean, lumpSumsDefined: Boolean): Seq[EmployerInformationRow] = {
    val isInYear = inYearAction.inYear(taxYear)
    Seq(
      EmployerInformationRow(
        EmploymentDetails,
        if (showNotification) ToDo else Updated,
        Some(CheckEmploymentDetailsController.show(taxYear, employmentId)),
        !showNotification
      ),
      {
        val maybeAction = unless(isInYear && !benefitsDefined)(CheckYourBenefitsController.show(taxYear, employmentId))
        summaryEmployerInformationRowFor(EmploymentBenefits, benefitsDefined, maybeAction, isInYear, showNotification)
      }
    ) ++
      when(appConfig.studentLoansEnabled) {
        val maybeAction = unless(isInYear && !studentLoansDefined)(StudentLoansCYAController.show(taxYear, employmentId))
        summaryEmployerInformationRowFor(StudentLoans, studentLoansDefined, maybeAction, isInYear, showNotification)
      } ++
      when(appConfig.taxableLumpSumsEnabled) {
        val maybeAction = Some(TaxableLumpSumListController.show(taxYear, employmentId))
        summaryEmployerInformationRowFor(TaxableLumpSums, lumpSumsDefined, maybeAction, isInYear, showNotification)
      }
  }
}
