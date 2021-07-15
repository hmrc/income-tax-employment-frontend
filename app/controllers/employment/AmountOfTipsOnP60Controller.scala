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

import audit.AuditService
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes.{CheckEmploymentDetailsController, OtherPaymentsController}
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.AmountForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.{AmountOfTipsOnP60View, CheckEmploymentDetailsView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmountOfTipsOnP60Controller @Inject()(implicit val cc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            inYearAction: InYearAction,
                                            amountOfTipsOnP60View: AmountOfTipsOnP60View,
                                            checkEmploymentDetailsView: CheckEmploymentDetailsView,
                                            implicit val appConfig: AppConfig,
                                            employmentSessionService: EmploymentSessionService,
                                            auditService: AuditService,
                                            errorHandler: ErrorHandler,
                                            implicit val ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "amountsNotOnYourP60.error.noEntry." + agentOrIndividual,
    wrongFormatKey = "amountsNotOnYourP60.incorrectFormat",
    exceedsMaxAmountKey = "amountsNotOnYourP60.maximum"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId){
        data =>
          data.employment.employmentDetails.tipsAndOtherPaymentsQuestion match {
            case Some(true) =>
              val amount = data.employment.employmentDetails.tipsAndOtherPayments
              val filledForm = amount.fold(form(user.isAgent))(x => form(user.isAgent).fill(x))
              Future.successful(Ok(amountOfTipsOnP60View(filledForm,taxYear, employmentId, amount)))
            case _ =>
              Future.successful(Redirect(OtherPaymentsController.show(taxYear, employmentId)))
          }
      }
    }
  }
  def submit(taxYear:Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      form(user.isAgent).bindFromRequest().fold(
        { formWithErrors =>
          Future.successful(BadRequest(amountOfTipsOnP60View(formWithErrors,taxYear, employmentId, None)))
        },
        { submittedAmount =>
          employmentSessionService.getSessionDataAndReturnResult(taxYear,employmentId){
            data =>
              val cya = data.employment
              val updatedCya = cya.copy(cya.employmentDetails.copy(tipsAndOtherPayments = Some(submittedAmount)))
              employmentSessionService.updateSessionData(employmentId, updatedCya, taxYear, false, data.isPriorSubmission)(errorHandler.internalServerError()){
                Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId))
              }
          }
        }
      )
    }
  }
}

