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
import controllers.employment.routes.{CheckEmploymentDetailsController, OtherPaymentsAmountController}
import controllers.predicates.{AuthorisedAction, InYearAction, QuestionsJourneyValidator}
import forms.AmountForm
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.OtherPaymentsAmountView
import javax.inject.Inject
import models.question.QuestionsJourney

import scala.concurrent.{ExecutionContext, Future}

class OtherPaymentsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              inYearAction: InYearAction,
                                              otherPaymentsAmountView: OtherPaymentsAmountView,
                                              implicit val appConfig: AppConfig,
                                              employmentSessionService: EmploymentSessionService,
                                              errorHandler: ErrorHandler,
                                              clock: Clock,
                                              questionsJourneyValidator: QuestionsJourneyValidator,
                                              implicit val ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def buildForm(implicit isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "otherPaymentsAmount.error.noEntry." + agentOrIndividual,
    wrongFormatKey = "otherPaymentsAmount.incorrectFormat",
    exceedsMaxAmountKey = "otherPaymentsAmount.maximum"
  )

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) if cya.employment.employmentDetails.tipsAndOtherPaymentsQuestion.getOrElse(false) => {
              val cyaTips = cya.employment.employmentDetails.tipsAndOtherPayments
              val priorEmployment = prior.map(priorEmp => employmentSessionService.getLatestEmploymentData(priorEmp, isInYear = false)
                .filter(_.employmentId.equals(employmentId))).getOrElse(Seq.empty)
              val priorTips = priorEmployment.headOption.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.tipsAndOtherPayments)))
              lazy val unfilledForm = buildForm(user.isAgent)
              val form: Form[BigDecimal] = cyaTips.fold(unfilledForm)(
                cyaOther => if (priorTips.exists(_.equals(cyaOther))) unfilledForm else buildForm(user.isAgent).fill(cyaOther))
              Future.successful(Ok(otherPaymentsAmountView(form, taxYear, employmentId, cyaTips)))
            }
          case Some(_) => Future.successful(Redirect(controllers.employment.routes.OtherPaymentsController.show(taxYear, employmentId)))
          case _ => Future.successful(
            Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }
  def submit(taxYear:Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      implicit val journey: QuestionsJourney[EmploymentUserData] = EmploymentUserData.journey(taxYear, employmentId)

      val checkEmploymentDetailsUrl  = CheckEmploymentDetailsController.show(taxYear, employmentId)

      buildForm(user.isAgent).bindFromRequest().fold(
        { formWithErrors =>
          Future.successful(BadRequest(otherPaymentsAmountView(formWithErrors,taxYear, employmentId, None)))
        },
        { submittedAmount =>
          val cyaFuture = employmentSessionService.getSessionData(taxYear,employmentId)
          questionsJourneyValidator.validate(OtherPaymentsAmountController.show(taxYear, employmentId), cyaFuture)(checkEmploymentDetailsUrl.url) { data =>
            val cya = data.employment
            val updatedCya = cya.copy(cya.employmentDetails.copy(tipsAndOtherPayments = Some(submittedAmount)))
            employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()){
              Redirect(checkEmploymentDetailsUrl)
            }
          }
        }
      )
    }
  }
}

