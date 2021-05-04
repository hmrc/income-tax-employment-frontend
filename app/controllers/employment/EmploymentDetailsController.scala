/*
 * Copyright 2021 HM Revenue & Customs
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

  import common.SessionValues
  import config.AppConfig
  import controllers.predicates.AuthorisedAction
  import models.{EmployerModel, EmploymentModel, GetEmploymentDataModel, PayModel, User}
  import play.api.i18n.I18nSupport
  import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
  import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
  import utils.SessionHelper
  import views.html.employment.EmploymentDetailsView

  import javax.inject.Inject

  class EmploymentDetailsController @Inject()(
                                                   implicit val cc: MessagesControllerComponents,
                                                   authAction: AuthorisedAction,
                                                   employmentDetailsView: EmploymentDetailsView,
                                                   implicit val appConfig: AppConfig
                                                 ) extends FrontendController(cc) with I18nSupport with SessionHelper {


    def show(taxYear: Int): Action[AnyContent] = authAction { implicit user =>

      val employmentDetails: Option[GetEmploymentDataModel] = getModelFromSession[GetEmploymentDataModel](SessionValues.EMPLOYMENT_DATA)
      //val employmentDetails: Option[GetEmploymentDataModel] = Some(getEmploymentDataModel)

      employmentDetails match {
        case Some(empModel) => Ok(employmentDetailsView(empModel, taxYear))
        case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

    }
    val payModel : PayModel = PayModel(111.4,1000.00, Some(10000000), "Monthly", "14/83/2022", None, None)
    val employerModel : EmployerModel = EmployerModel(Some("#Lon"), "Londis LTD 2020 PLC Company")
    val employmentModel:EmploymentModel = EmploymentModel(None, None, Some(true), Some(true), Some("14/07/1990"), None, None, None, None, employerModel, payModel)
    val getEmploymentDataModel:GetEmploymentDataModel = GetEmploymentDataModel("Today", None, None, None, employmentModel)

  }
