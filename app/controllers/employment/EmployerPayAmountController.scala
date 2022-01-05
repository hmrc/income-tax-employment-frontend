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

import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.AmountForm
import models.User
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.EmployerPayAmountView

import javax.inject.Inject
import scala.concurrent.Future

class EmployerPayAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            employerPayAmountView: EmployerPayAmountView,
                                            inYearAction: InYearAction,
                                            appConfig: AppConfig,
                                            employmentSessionService: EmploymentSessionService,
                                            employmentService: EmploymentService,
                                            errorHandler: ErrorHandler,
                                            clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {
  private implicit val executionContext = cc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val cyaAmount = cya.employment.employmentDetails.taxablePayToDate
            val priorEmployment = prior.map(priorEmp => employmentSessionService.getLatestEmploymentData(priorEmp, isInYear = false)
              .filter(_.employmentId.equals(employmentId))).getOrElse(Seq.empty)
            val priorAmount = priorEmployment.headOption.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)))
            lazy val unfilledForm = buildForm(user.isAgent)
            val form: Form[BigDecimal] = cyaAmount.fold(unfilledForm)(
              cyaPay => if (priorAmount.exists(_.equals(cyaPay))) unfilledForm else buildForm(user.isAgent).fill(cyaPay))

            Future.successful(Ok(employerPayAmountView(taxYear, form,
              cyaAmount, cya.employment.employmentDetails.employerName, employmentId)))

          case None => Future.successful(
            Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { data =>
        buildForm(user.isAgent).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(employerPayAmountView(taxYear, formWithErrors,
            data.employment.employmentDetails.taxablePayToDate, data.employment.employmentDetails.employerName, employmentId))),
          amount => handleSuccessForm(taxYear, employmentId, data, amount)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    employmentService.updateTaxablePayToDate(taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => employmentDetailsRedirect(employmentUserData.employment, taxYear, employmentId, employmentUserData.isPriorSubmission)
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"employerPayAmount.error.empty.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = "employerPayAmount.error.wrongFormat", exceedsMaxAmountKey = "employerPayAmount.error.amountMaxLimit"
  )
}
