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
import controllers.details.routes.EmploymentTaxController
import controllers.employment.routes.CheckEmploymentDetailsController
import forms.details.EmploymentDetailsFormsProvider
import models.AuthorisationRequest
import models.details.EmploymentDetails
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.EmploymentSessionService
import services.employment.EmploymentService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.details.EmployerPayAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmployerPayAmountController @Inject()(authAction: AuthorisedAction,
                                            pageView: EmployerPayAmountView,
                                            inYearAction: InYearUtil,
                                            employmentSessionService: EmploymentSessionService,
                                            employmentService: EmploymentService,
                                            errorHandler: ErrorHandler,
                                            formsProvider: EmploymentDetailsFormsProvider)
                                           (implicit cc: MessagesControllerComponents, val appConfig: AppConfig)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  private implicit val executionContext: ExecutionContext = cc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val cyaAmount = cya.employment.employmentDetails.taxablePayToDate
            lazy val unfilledForm = formsProvider.employerPayAmountForm(request.user.isAgent)
            val form: Form[BigDecimal] = cyaAmount.fold(unfilledForm)(
              cyaPay => formsProvider.employerPayAmountForm(request.user.isAgent).fill(cyaPay))

            Future.successful(Ok(pageView(taxYear, form,
              cya.employment.employmentDetails.employerName, employmentId)))

          case None => Future.successful(
            Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(CheckEmploymentDetailsController.show(taxYear, employmentId).url) { data =>
        formsProvider.employerPayAmountForm(request.user.isAgent).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(pageView(taxYear, formWithErrors,
            data.employment.employmentDetails.employerName, employmentId))),
          amount => handleSuccessForm(taxYear, employmentId, data, amount)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    employmentService.updateTaxablePayToDate(request.user, taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => Redirect(getRedirectCall(employmentUserData.employment.employmentDetails, taxYear, employmentId))
    }
  }

  private def getRedirectCall(employmentDetails: EmploymentDetails,
                              taxYear: Int,
                              employmentId: String)
                             (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Call = {
    if (employmentDetails.isFinished) {
      CheckEmploymentDetailsController.show(taxYear, employmentId)
    } else {
      EmploymentTaxController.show(taxYear, employmentId)
    }
  }
}
