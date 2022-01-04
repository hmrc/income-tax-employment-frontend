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

package controllers.benefits.accommodation

import config.{AppConfig, ErrorHandler}
import controllers.benefits.accommodation.routes._
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, qualifyingRelocationBenefitsRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.AccommodationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.accommodation.QualifyingRelocationBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class QualifyingRelocationBenefitsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                       authAction: AuthorisedAction,
                                                       inYearAction: InYearAction,
                                                       qualifyingRelocationBenefitsView: QualifyingRelocationBenefitsView,
                                                       appConfig: AppConfig,
                                                       employmentSessionService: EmploymentSessionService,
                                                       accommodationService: AccommodationService,
                                                       errorHandler: ErrorHandler,
                                                       ec: ExecutionContext,
                                                       clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {

      employmentSessionService.getSessionDataResult(taxYear, employmentId) { optCya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val qualifyingRelocationBenefitsQuestion: Option[Boolean] = cya.employment.employmentBenefits.flatMap(
            _.accommodationRelocationModel).flatMap(_.qualifyingRelocationExpensesQuestion)

          qualifyingRelocationBenefitsQuestion match {
            case Some(questionResult) => Future.successful(Ok(qualifyingRelocationBenefitsView(yesNoForm.fill(questionResult), taxYear, employmentId)))
            case None => Future.successful(Ok(qualifyingRelocationBenefitsView(yesNoForm, taxYear, employmentId)))
          }
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataResult(taxYear, employmentId) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, cya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>

          yesNoForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(qualifyingRelocationBenefitsView(formWithErrors, taxYear, employmentId))),
            yesNo => handleSuccessForm(taxYear, employmentId, cya, yesNo)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, questionValue: Boolean)
                               (implicit user: User[_]): Future[Result] = {
    accommodationService.updateQualifyingExpensesQuestion(taxYear, employmentId, employmentUserData, questionValue).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = if (questionValue) {
          QualifyingRelocationBenefitsAmountController.show(taxYear, employmentId)
        } else {
          NonQualifyingRelocationBenefitsController.show(taxYear, employmentId)
        }

        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.qualifyingRelocationBenefits.error.${if (user.isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    qualifyingRelocationBenefitsRedirects(cya, taxYear, employmentId)
  }
}



