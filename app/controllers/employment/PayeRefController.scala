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
import controllers.employment.routes._
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.employment.PayeForm
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.employment.PayeRefView

import scala.concurrent.Future

class PayeRefController @Inject()(implicit val authorisedAction: AuthorisedAction,
                                  mcc: MessagesControllerComponents,
                                  appConfig: AppConfig,
                                  payeRefView: PayeRefView,
                                  inYearAction: InYearAction,
                                  errorHandler: ErrorHandler,
                                  employmentSessionService: EmploymentSessionService,
                                  clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val cyaRef = cya.employment.employmentDetails.employerRef
            val priorEmployment = prior.map(priorEmp => employmentSessionService.getLatestEmploymentData(priorEmp, isInYear = false)
              .filter(_.employmentId.equals(employmentId))).getOrElse(Seq.empty)
            val priorRef = priorEmployment.headOption.flatMap(_.employerRef)
            lazy val unfilledForm = PayeForm.payeRefForm(user.isAgent)
            val form: Form[String] = cyaRef.fold(unfilledForm)(
              cyaPaye => if(priorRef.map(_.equals(cyaPaye)).getOrElse(true)) unfilledForm else PayeForm.payeRefForm(user.isAgent).fill(cyaPaye))
            val employerName = cya.employment.employmentDetails.employerName
            Future.successful(Ok(payeRefView(form, taxYear, employerName, cyaRef, employmentId)))

          case _ => Future.successful(
            Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }


  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckEmploymentDetailsController.show(taxYear, employmentId).url
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { data =>
        PayeForm.payeRefForm(user.isAgent).bindFromRequest().fold(
          { formWithErrors =>
            val payeRef = data.employment.employmentDetails.employerRef
            val employerName = data.employment.employmentDetails.employerName
            Future.successful(BadRequest(payeRefView(formWithErrors, taxYear, employerName, payeRef, employmentId)))
          },
          { payeRef =>
            val cya = data.employment
            val updatedCya = cya.copy(cya.employmentDetails.copy(employerRef = Some(payeRef)))
            employmentSessionService.createOrUpdateSessionData(employmentId, updatedCya, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              employmentDetailsRedirect(updatedCya,taxYear,employmentId,data.isPriorSubmission)
            }
          }
        )
      }
    }
  }
}
