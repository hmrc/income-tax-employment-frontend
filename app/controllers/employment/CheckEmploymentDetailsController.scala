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

import audit.{AuditService, ViewEmploymentDetailsAudit}
import config.AppConfig
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.employment.{AllEmploymentData, EmploymentSource}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.CheckEmploymentDetailsView

import javax.inject.Inject
import services.IncomeTaxUserDataService

import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentDetailsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 inYearAction: InYearAction,
                                                 employmentDetailsView: CheckEmploymentDetailsView,
                                                 appConfig: AppConfig,
                                                 incomeTaxUserDataService: IncomeTaxUserDataService,
                                                 auditService: AuditService,
                                                 ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with SessionHelper {


  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    def result(allEmploymentData: AllEmploymentData): Result = {
      val isInYear: Boolean = inYearAction.inYear(taxYear)
      val customerData: Option[EmploymentSource] = allEmploymentData.customerEmploymentData.find(source => source.employmentId.equals(employmentId))
      val isUsingCustomerData: Boolean = customerData.isDefined && !isInYear

      val source: Option[EmploymentSource] = if(isUsingCustomerData){
        customerData
      }else {
        allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId))
      }

      source match {
          case Some(source) =>
            val (name, ref, data, empId) = (source.employerName, source.employerRef, source.employmentData, source.employmentId)
            val auditModel = ViewEmploymentDetailsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, name, ref, data)
            auditService.auditModel[ViewEmploymentDetailsAudit](auditModel.toAuditModel)
            Ok(employmentDetailsView(name, ref, data, taxYear, isInYear, empId, isUsingCustomerData))
            case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
    incomeTaxUserDataService.findUserData(user, taxYear)(result)
  }

  def submit(taxYear:Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    //TODO - Once Create and Update API has been orchestrated
    Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
  }

}
