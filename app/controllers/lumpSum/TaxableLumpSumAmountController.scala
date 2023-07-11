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

package controllers.lumpSum

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import forms.lumpSums.LumpSumFormsProvider
import models.UserSessionDataRequest
import models.employment.EmploymentDetailsType
import models.otheremployment.pages.TaxableLumpSumAmountPage
import models.otheremployment.session.TaxableLumpSum
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.employment.OtherEmploymentInfoService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.taxableLumpSum.TaxableLumpSumAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxableLumpSumAmountController @Inject()( mcc: MessagesControllerComponents,
                                                actionsProvider: ActionsProvider,
                                                formProvider: LumpSumFormsProvider,
                                                view: TaxableLumpSumAmountView,
                                                inYearAction: InYearUtil,
                                                otherEmploymentInfoService: OtherEmploymentInfoService,
                                                errorHandler: ErrorHandler)
                                              (implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String, index: Option[Int] = None): Action[AnyContent] = actionsProvider.endOfYearSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentDetailsType
  ) { implicit request =>
    if (!appConfig.taxableLumpSumsEnabled) {
      Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear))
    } else {
      val page = TaxableLumpSumAmountPage(request, index)
      val form = formProvider.TaxableLumpSumAmountForm(request.user.isAgent, request.employmentUserData.employment.employmentDetails.employerName)
      Ok(view(page,
        page.amount.fold(form)(amount =>
          formProvider.TaxableLumpSumAmountForm(request.user.isAgent, request.employmentUserData.employment.employmentDetails.employerName).fill(amount))
      ))
    }
  }

  def submit(taxYear: Int, employmentId: String, index: Option[Int] = None): Action[AnyContent] = actionsProvider.endOfYearSessionData(
    taxYear = taxYear,
    employmentId = employmentId,
    employmentType = EmploymentDetailsType
  ).async { implicit request =>
    formProvider.TaxableLumpSumAmountForm(request.user.isAgent, request.employmentUserData.employment.employmentDetails.employerName).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(TaxableLumpSumAmountPage(request, index), formWithErrors))),
      amount => onSuccess(index, amount, taxYear)(request))
  }

  def onSuccess(index: Option[Int], amount: BigDecimal, taxYear: Int)(implicit request: UserSessionDataRequest[_]): Future[Result] = {
    val oldLumpSum = request.employmentUserData.employment.otherEmploymentIncome.map(oEI => oEI.taxableLumpSums).getOrElse(Seq.empty[TaxableLumpSum])

    val newLumpSums: Seq[TaxableLumpSum] = {
      index.fold(oldLumpSum ++ Seq(TaxableLumpSum(amount, None, None)))(
        i => oldLumpSum.updated(i, TaxableLumpSum(amount, oldLumpSum(i).payrollAmount, oldLumpSum(i).payrollHasPaidNoneSomeAll)))
    }

    otherEmploymentInfoService.updateLumpSums(request.user, taxYear, request.employmentUserData.employmentId, request.employmentUserData, newLumpSums).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        Ok(view(TaxableLumpSumAmountPage(request, index),
          formProvider.TaxableLumpSumAmountForm(request.user.isAgent,
            request.employmentUserData.employment.employmentDetails.employerName)))
    }

  }
}