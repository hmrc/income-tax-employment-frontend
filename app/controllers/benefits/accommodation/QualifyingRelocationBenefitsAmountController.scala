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

import actions.AuthorisedAction
import config.{AppConfig, ErrorHandler}
import controllers.benefits.accommodation.routes._
import controllers.employment.routes.CheckYourBenefitsController
import forms.{AmountForm, FormUtils}
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, qualifyingRelocationBenefitsAmountRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.AccommodationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, InYearUtil, SessionHelper}
import views.html.benefits.accommodation.QualifyingRelocationBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class QualifyingRelocationBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                             authAction: AuthorisedAction,
                                                             inYearAction: InYearUtil,
                                                             appConfig: AppConfig,
                                                             qualifyingRelocationBenefitsAmountView: QualifyingRelocationBenefitsAmountView,
                                                             val employmentSessionService: EmploymentSessionService,
                                                             accommodationService: AccommodationService,
                                                             errorHandler: ErrorHandler,
                                                             ec: ExecutionContext,
                                                             clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya, EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses))
          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId) { employment =>
            employment.employmentBenefits.flatMap(_.benefits.flatMap(_.qualifyingRelocationExpenses))
          }

          Future.successful(Ok(qualifyingRelocationBenefitsAmountView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(CheckYourBenefitsController.show(taxYear, employmentId).url) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya), EmploymentBenefitsType)(redirects(_, taxYear, employmentId)) { cya =>
          buildForm(user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              val fillValue = cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.qualifyingRelocationExpenses)
              Future.successful(BadRequest(qualifyingRelocationBenefitsAmountView(taxYear, formWithErrors, fillValue, employmentId)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, employmentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    accommodationService.updateQualifyingExpenses(taxYear, employmentId, employmentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = NonQualifyingRelocationBenefitsController.show(taxYear, employmentId)
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.qualifyingRelocationBenefitsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.qualifyingRelocationBenefitsAmount.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.qualifyingRelocationBenefitsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  private def redirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String) = {
    qualifyingRelocationBenefitsAmountRedirects(cya, taxYear, employmentId)
  }
}
