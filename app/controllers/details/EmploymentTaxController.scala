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

package controllers.details

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.employment.routes._
import forms.details.EmploymentDetailsFormsProvider
import models.AuthorisationRequest
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.EmploymentSessionService
import services.employment.EmploymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.details.EmploymentTaxView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmploymentTaxController @Inject()(mcc: MessagesControllerComponents,
                                        authAction: AuthorisedAction,
                                        pageView: EmploymentTaxView,
                                        employmentSessionService: EmploymentSessionService,
                                        employmentService: EmploymentService,
                                        inYearAction: InYearUtil,
                                        formsProvider: EmploymentDetailsFormsProvider,
                                        errorHandler: ErrorHandler)
                                       (implicit appConfig: AppConfig) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  private implicit val ec: ExecutionContext = mcc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val cyaTax = cya.employment.employmentDetails.totalTaxToDate
            val priorEmployment = prior.map(priorEmp => priorEmp.latestEOYEmployments.filter(_.employmentId.equals(employmentId))).getOrElse(Seq.empty)
            val priorTax = priorEmployment.headOption.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)))
            lazy val unfilledForm = formsProvider.employmentTaxAmountForm(request.user.isAgent)
            val form: Form[BigDecimal] = cyaTax.fold(unfilledForm)(
              cyaTaxed => if (priorTax.exists(_.equals(cyaTaxed))) unfilledForm else formsProvider.employmentTaxAmountForm(request.user.isAgent).fill(cyaTaxed))
            Future.successful(Ok(pageView(taxYear, employmentId, cya.employment.employmentDetails.employerName, form, cyaTax)))

          case None => Future.successful(
            Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }


  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckEmploymentDetailsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        formsProvider.employmentTaxAmountForm(request.user.isAgent).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(pageView(taxYear, employmentId, cya.employment.employmentDetails.employerName,
            formWithErrors, cya.employment.employmentDetails.totalTaxToDate))),
          completeForm => handleSuccessForm(taxYear, employmentId, cya, completeForm)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, totalTaxToDate: BigDecimal)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    employmentService.updateTotalTaxToDate(request.user, taxYear, employmentId, employmentUserData, totalTaxToDate).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(_) => Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId))
    }
  }
}
