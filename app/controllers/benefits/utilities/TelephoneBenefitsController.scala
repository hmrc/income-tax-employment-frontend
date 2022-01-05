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

package controllers.benefits.utilities

import config.{AppConfig, ErrorHandler}
import controllers.benefits.utilities.routes.{EmployerProvidedServicesBenefitsController, TelephoneBenefitsAmountController}
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, commonUtilitiesAndServicesBenefitsRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.UtilitiesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.utilities.TelephoneBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TelephoneBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            inYearAction: InYearAction,
                                            telephoneBenefitsView: TelephoneBenefitsView,
                                            appConfig: AppConfig,
                                            employmentSessionService: EmploymentSessionService,
                                            utilitiesService: UtilitiesService,
                                            errorHandler: ErrorHandler,
                                            ec: ExecutionContext,
                                            clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(commonUtilitiesAndServicesBenefitsRedirects(_, taxYear, employmentId)) { cya =>

          cya.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.telephoneQuestion)) match {
            case Some(questionResult) => Future.successful(Ok(telephoneBenefitsView(yesNoForm.fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(telephoneBenefitsView(yesNoForm, taxYear, employmentId)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(commonUtilitiesAndServicesBenefitsRedirects(_, taxYear, employmentId)) { data =>

          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(telephoneBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, data, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit user: User[_]): Future[Result] = {
    utilitiesService.updateTelephoneQuestion(taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (questionValue) {
          TelephoneBenefitsAmountController.show(taxYear, employmentId)
        } else {
          EmployerProvidedServicesBenefitsController.show(taxYear, employmentId)
        }
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.telephoneBenefits.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )
}
