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

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes._
import forms.employment.PayeForm
import models.User
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.employmentDetailsRedirect
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.employment.PayeRefView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PayeRefController @Inject()(implicit val authorisedAction: AuthorisedAction,
                                  mcc: MessagesControllerComponents,
                                  appConfig: AppConfig,
                                  payeRefView: PayeRefView,
                                  inYearAction: InYearUtil,
                                  errorHandler: ErrorHandler,
                                  employmentSessionService: EmploymentSessionService,
                                  employmentService: EmploymentService,
                                  clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper {
  private implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val cyaRef = cya.employment.employmentDetails.employerRef
            val priorEmployment = prior.map(priorEmp => priorEmp.latestEOYEmployments.filter(_.employmentId.equals(employmentId))).getOrElse(Seq.empty)
            val priorRef = priorEmployment.headOption.flatMap(_.employerRef)
            lazy val unfilledForm = PayeForm.payeRefForm(user.isAgent)
            val form: Form[String] = cyaRef.fold(unfilledForm)(
              cyaPaye => if (priorRef.exists(_.equals(cyaPaye))) unfilledForm else PayeForm.payeRefForm(user.isAgent).fill(cyaPaye))
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
          formWithErrors => {
            val payeRef = data.employment.employmentDetails.employerRef
            val employerName = data.employment.employmentDetails.employerName
            Future.successful(BadRequest(payeRefView(formWithErrors, taxYear, employerName, payeRef, employmentId)))
          },
          payeRef => handleSuccessForm(taxYear, employmentId, data, payeRef)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, payeRef: String)
                               (implicit user: User[_]): Future[Result] = {
    employmentService.updateEmployerRef(taxYear, employmentId, employmentUserData, payeRef).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => employmentDetailsRedirect(employmentUserData.employment, taxYear, employmentId, employmentUserData.isPriorSubmission)
    }
  }
}
