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

package controllers.benefits.medical

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.medical.routes._
import forms.{AmountForm, FormUtils}
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.RedirectService.{medicalInsuranceAmountRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.MedicalService
import services.{EmploymentSessionService, RedirectService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.benefits.medical.MedicalOrDentalBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MedicalOrDentalBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                        authAction: AuthorisedAction,
                                                        inYearAction: InYearUtil,
                                                        appConfig: AppConfig,
                                                        medicalOrDentalBenefitsAmountView: MedicalOrDentalBenefitsAmountView,
                                                        val employmentSessionService: EmploymentSessionService,
                                                        medicalService: MedicalService,
                                                        errorHandler: ErrorHandler,
                                                        ec: ExecutionContext,
                                                        clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance))
          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId) { employment =>
            employment.employmentBenefits.flatMap(_.benefits.flatMap(_.medicalInsurance))
          }
          Future(Ok(medicalOrDentalBenefitsAmountView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, cya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          buildForm(user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              val fillValue = cya.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel).flatMap(_.medicalInsurance)
              Future.successful(BadRequest(medicalOrDentalBenefitsAmountView(taxYear, formWithErrors, fillValue, employmentId)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    medicalService.updateMedicalInsurance(taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = ChildcareBenefitsController.show(taxYear, employmentId)
        RedirectService.benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.medicalOrDentalBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.medicalOrDentalBenefitsAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.medicalOrDentalBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    medicalInsuranceAmountRedirects(cya, taxYear, employmentId)
  }
}
