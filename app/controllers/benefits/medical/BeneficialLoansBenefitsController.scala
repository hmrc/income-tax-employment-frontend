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
import controllers.benefits.income.routes._
import controllers.benefits.medical.routes._
import forms.YesNoForm
import models.AuthorisationRequest
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{beneficialLoansRedirects, benefitsSubmitRedirect, redirectBasedOnCurrentAnswers}
import services.benefits.MedicalService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.benefits.medical.BeneficialLoansBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BeneficialLoansBenefitsController @Inject()(authAction: AuthorisedAction,
                                                  inYearAction: InYearUtil,
                                                  beneficialLoansBenefitsView: BeneficialLoansBenefitsView,
                                                  employmentSessionService: EmploymentSessionService,
                                                  medicalService: MedicalService,
                                                  errorHandler: ErrorHandler)
                                                 (implicit val appConfig: AppConfig, mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(beneficialLoansRedirects(_, taxYear, employmentId)) { cya =>

          cya.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoanQuestion)) match {
            case Some(questionResult) =>
              Future.successful(Ok(beneficialLoansBenefitsView(yesNoForm(request.user.isAgent).fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(beneficialLoansBenefitsView(yesNoForm(request.user.isAgent), taxYear, employmentId)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit request =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(beneficialLoansRedirects(_, taxYear, employmentId)) { data =>

          yesNoForm(request.user.isAgent).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(beneficialLoansBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit request: AuthorisationRequest[_]): Future[Result] = {
    medicalService.updateBeneficialLoanQuestion(request.user, taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (questionValue) {
          BeneficialLoansAmountController.show(taxYear, employmentId)
        } else {
          IncomeTaxOrIncurredCostsBenefitsController.show(taxYear, employmentId)
        }
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def yesNoForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.beneficialLoans.error.noEntry.${if (isAgent) "agent" else "individual"}"
  )
}
