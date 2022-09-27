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

package controllers.tailorings

  import actions.{AuthorisedAction, TaxYearAction}
  import akka.actor.Status.Success
  import config.{AppConfig, ErrorHandler}
  import forms.{FormUtils, YesNoForm}
  import models.{AuthorisationRequest, IncomeTaxUserData, User}
  import models.employment.AllEmploymentData
  import play.api.data.Form
  import play.api.i18n.I18nSupport
  import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
  import services.{EmploymentSessionService, ExcludeJourneyService}
  import uk.gov.hmrc.http.HeaderCarrier
  import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
  import utils.SessionHelper
  import views.html.tailorings.EmploymentGatewayView

  import javax.inject.Inject
  import scala.concurrent.{ExecutionContext, Future}

  class EmploymentGatewayController @Inject()(mcc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              employmentSessionService: EmploymentSessionService,
                                              view: EmploymentGatewayView,
                                              errorHandler: ErrorHandler,
                                              excludeJourneyService: ExcludeJourneyService
                                             )
                                             (implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc)
    with I18nSupport with SessionHelper with FormUtils {

    private def form(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(s"tailoring.empty.error.${if (isAgent) "agent" else "individual"}")

    def show(taxYear: Int): Action[AnyContent] = (authAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>
      if (appConfig.tailoringEnabled) {
        Future(Ok(view(form(request.user.isAgent), taxYear)))
      }
      else {
        Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }

    def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
      if (appConfig.tailoringEnabled) {
        form(request.user.isAgent).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
          yesNo => {
            employmentSessionService.getPriorData(request.user, taxYear).map {
              case Left(error) => Future.successful(Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear, "true")))

              case Right(allEmploymentData) =>
                if (allEmploymentData.employment.isDefined && !yesNo) {
                  deleteAndExcludeData(taxYear, request.user, allEmploymentData)
                  Future.successful(Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear, "true")))
                } else if (allEmploymentData.employment.isEmpty && !yesNo) {
                  Future.successful(Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear, "true")))
                } else {
                  Future.successful(Redirect(controllers.employment.routes.EmploymentSummaryController.show(taxYear, gateway = "false")))
                }
            }.flatMap(f => f)
          }
        )
      } else {
        Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }

    }

    private def deleteAndExcludeData(taxYear: Int, user: User, incomeTaxUserData: IncomeTaxUserData)(implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Unit = {
      excludeJourneyService.excludeJourney("employments", taxYear, user.nino)(user, hc)
      incomeTaxUserData.employment.fold()(employments => employments.customerEmploymentData.map(employment =>
        employmentSessionService.clear(user, taxYear, employment.employmentId)
      ))
    }


  }
