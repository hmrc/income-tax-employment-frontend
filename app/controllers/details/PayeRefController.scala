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

package controllers.details

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.details.routes._
import controllers.employment.routes._
import forms.details.PayeRefForm
import models.AuthorisationRequest
import models.details.EmploymentDetails
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.EmploymentSessionService
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.details.PayeRefView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PayeRefController @Inject()(authorisedAction: AuthorisedAction,
                                  mcc: MessagesControllerComponents,
                                  pageView: PayeRefView,
                                  inYearAction: InYearUtil,
                                  errorHandler: ErrorHandler,
                                  employmentSessionService: EmploymentSessionService,
                                  employmentService: EmploymentService)
                                 (implicit appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {
  private implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, _) =>
        cya match {
          case Some(cya) =>
            val cyaRef = cya.employment.employmentDetails.employerRef

            lazy val unfilledForm = PayeRefForm.payeRefForm //TODO - unit tests unfilled forms and pre-filled forms conditions
            val form: Form[String] = cyaRef.fold(unfilledForm)(
              cyaPaye => PayeRefForm.payeRefForm.fill(cyaPaye))
            val employerName = cya.employment.employmentDetails.employerName
            val employmentEnded = cya.employment.employmentDetails.cessationDate.isDefined
            Future.successful(Ok(pageView(form, taxYear, employerName, employmentId, employmentEnded)))

          case _ => Future.successful(
            Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckEmploymentDetailsController.show(taxYear, employmentId).url
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { data =>
        PayeRefForm.payeRefForm.bindFromRequest().fold(
          formWithErrors => {
            val employerName = data.employment.employmentDetails.employerName
            val employmentEnded = data.employment.employmentDetails.cessationDate.isDefined
            Future.successful(BadRequest(pageView(formWithErrors, taxYear, employerName, employmentId, employmentEnded)))
          },
          payeRef => handleSuccessForm(taxYear, employmentId, data, if (payeRef.trim.isEmpty) None else Some(payeRef))
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, payeRef: Option[String])
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    employmentService.updateEmployerRef(request.user, taxYear, employmentId, employmentUserData, payeRef).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => Redirect(getRedirectCall(employmentUserData.employment.employmentDetails, taxYear, employmentId))
    }
  }

  private def getRedirectCall(employmentDetails: EmploymentDetails,
                              taxYear: Int,
                              employmentId: String): Call = {
    if (employmentDetails.isFinished) CheckEmploymentDetailsController.show(taxYear, employmentId) else EmployerPayrollIdController.show(taxYear, employmentId)
  }
}
