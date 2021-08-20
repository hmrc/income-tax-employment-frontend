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
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.AmountForm
import javax.inject.Inject
import forms.employment.PayeForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.EmployerPayAmountView

import scala.concurrent.Future

class EmployerPayAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            employerPayAmountView: EmployerPayAmountView,
                                            inYearAction: InYearAction,
                                            appConfig: AppConfig,
                                            employmentSessionService: EmploymentSessionService,
                                            errorHandler: ErrorHandler,
                                            clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {


  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {
      val redirectUrl = controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId).url

      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val cyaAmount = cya.employment.employmentDetails.taxablePayToDate
            val priorEmployment = prior.map(priorEmp => employmentSessionService.getLatestEmploymentData(priorEmp, isInYear = false)
              .filter(_.employmentId.equals(employmentId))).getOrElse(Seq.empty)
            val priorAmount = priorEmployment.headOption.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)))
            lazy val unfilledForm = buildForm(user.isAgent)
            val form: Form[BigDecimal] = cyaAmount.fold(unfilledForm)(
              cyaPay => if(priorAmount.map(_.equals(cyaPay)).getOrElse(false)) unfilledForm else buildForm(user.isAgent).fill(cyaPay))

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
          { formWithErrors =>
            Future.successful(BadRequest(employerPayAmountView(taxYear, formWithErrors,
              data.employment.employmentDetails.taxablePayToDate, data.employment.employmentDetails.employerName, employmentId)))
          },
          {
            amount =>
              val cya = data.employment
              val updatedCyaModel = cya.copy(employmentDetails = cya.employmentDetails.copy(taxablePayToDate = Some(amount)))
              employmentSessionService.createOrUpdateSessionData(employmentId, updatedCyaModel, taxYear,
                isPriorSubmission = data.isPriorSubmission)(errorHandler.internalServerError()) {
                employmentDetailsRedirect(updatedCyaModel,taxYear,employmentId,data.isPriorSubmission)
              }
          }
        )
      }
    }
  }


    private def buildForm(isAgent: Boolean): Form[BigDecimal] = {
      AmountForm.amountForm(s"employerPayAmount.error.empty.${if (isAgent) "agent" else "individual"}",
        "employerPayAmount.error.wrongFormat", "employerPayAmount.error.amountMaxLimit")
    }
  }
