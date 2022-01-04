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
import controllers.benefits.travel.routes.TravelOrEntertainmentBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.{AmountForm, FormUtils}
import models.User
import models.employment.EmploymentBenefitsType
import models.mongo.EmploymentUserData
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.EmploymentSessionService
import services.RedirectService.{benefitsSubmitRedirect, nonQualifyingRelocationBenefitsAmountRedirects, redirectBasedOnCurrentAnswers}
import services.benefits.AccommodationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.benefits.accommodation.NonQualifyingRelocationBenefitsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NonQualifyingRelocationBenefitsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                                                authAction: AuthorisedAction,
                                                                inYearAction: InYearAction,
                                                                appConfig: AppConfig,
                                                                nonQualifyingRelocationBenefitsView: NonQualifyingRelocationBenefitsAmountView,
                                                                val employmentSessionService: EmploymentSessionService,
                                                                accommodationService: AccommodationService,
                                                                errorHandler: ErrorHandler,
                                                                ec: ExecutionContext,
                                                                clock: Clock) extends FrontendController(cc)
  with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (optCya, prior) =>

        redirectBasedOnCurrentAnswers(taxYear, employmentId, optCya,
          EmploymentBenefitsType)(nonQualifyingRelocationBenefitsAmountRedirects(_, taxYear, employmentId)) { cya =>
          val cyaAmount = cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses))

          val form = fillFormFromPriorAndCYA(buildForm(user.isAgent), prior, cyaAmount, employmentId)(
            employment => employment.employmentBenefits.flatMap(_.benefits.flatMap(_.nonQualifyingRelocationExpenses))
          )

          Future.successful(Ok(nonQualifyingRelocationBenefitsView(taxYear, form, cyaAmount, employmentId)))
        }
      }
    }
  }

  def submit(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>
    inYearAction.notInYear(taxYear) {
      val redirectUrl = CheckYourBenefitsController.show(taxYear, employmentId).url

      employmentSessionService.getSessionDataAndReturnResult(taxYear, employmentId)(redirectUrl) { cya =>
        redirectBasedOnCurrentAnswers(taxYear, employmentId, Some(cya),
          EmploymentBenefitsType)(nonQualifyingRelocationBenefitsAmountRedirects(_, taxYear, employmentId)) { cya =>
          buildForm(user.isAgent).bindFromRequest().fold(
            formWithErrors => {
              val fillValue = cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.nonQualifyingRelocationExpenses)
              Future.successful(BadRequest(nonQualifyingRelocationBenefitsView(taxYear, formWithErrors, fillValue, employmentId)))
            },
            amount => handleSuccessForm(taxYear, employmentId, cya, amount)
          )
        }
      }
    }
  }

  private def handleSuccessForm(taxYear: Int, employmentId: String, originalEmploymentUserData: EmploymentUserData, amount: BigDecimal)
                               (implicit user: User[_]): Future[Result] = {
    accommodationService.updateNonQualifyingExpenses(taxYear, employmentId, originalEmploymentUserData, amount).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(employmentUserData) =>
        val nextPage = TravelOrEntertainmentBenefitsController.show(taxYear, employmentId)
        benefitsSubmitRedirect(employmentUserData.employment, nextPage)(taxYear, employmentId)
    }
  }

  private def buildForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.nonQualifyingRelocationBenefitAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.nonQualifyingRelocationBenefitAmount.error.invalidFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.nonQualifyingRelocationBenefitAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )
}
