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
import controllers.employment.routes._
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
import views.html.employment.EmploymentTaxView

import javax.inject.Inject
import scala.concurrent.Future

class EmploymentTaxController @Inject()(implicit val mcc: MessagesControllerComponents,
                                        authAction: AuthorisedAction,
                                        appConfig: AppConfig,
                                        employmentTaxView: EmploymentTaxView,
                                        employmentSessionService: EmploymentSessionService,
                                        employmentService: EmploymentService,
                                        inYearAction: InYearAction,
                                        errorHandler: ErrorHandler,
                                        implicit val clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  private implicit val ec = mcc.executionContext

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) =>
            val cyaTax = cya.employment.employmentDetails.totalTaxToDate
            val priorEmployment = prior.map(priorEmp => employmentSessionService.getLatestEmploymentData(priorEmp, isInYear = false)
              .filter(_.employmentId.equals(employmentId))).getOrElse(Seq.empty)
            val priorTax = priorEmployment.headOption.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)))
            lazy val unfilledForm = buildForm(user.isAgent)
            val form: Form[BigDecimal] = cyaTax.fold(unfilledForm)(
              cyaTaxed => if (priorTax.exists(_.equals(cyaTaxed))) unfilledForm else buildForm(user.isAgent).fill(cyaTaxed))
            Future.successful(Ok(employmentTaxView(taxYear, employmentId, cya.employment.employmentDetails.employerName, form, cyaTax)))

          case None => Future.successful(
            Redirect(controllers.employment.routes.CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
      }
    }
  }


  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckEmploymentDetailsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        buildForm(user.isAgent).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(employmentTaxView(taxYear, employmentId, cya.employment.employmentDetails.employerName,
            formWithErrors, cya.employment.employmentDetails.totalTaxToDate))),
          completeForm => handleSuccessForm(taxYear, employmentId, cya, completeForm)
        )
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, totalTaxToDate: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    employmentService.updateTotalTaxToDate(taxYear, employmentId, employmentUserData, totalTaxToDate).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) => employmentDetailsRedirect(employmentUserData.employment, taxYear, employmentId, employmentUserData.isPriorSubmission)
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"employment.employmentTax.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = "employment.employmentTax.error.format",
    exceedsMaxAmountKey = "employment.employmentTax.error.max"
  )
}
