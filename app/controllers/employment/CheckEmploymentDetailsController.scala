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
import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction}
import models.employment.{AllEmploymentData, EmploymentDetailsView, EmploymentSource}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.employment.CheckEmploymentDetailsView
import javax.inject.Inject
import services.EmploymentSessionService

import scala.concurrent.{ExecutionContext, Future}

class CheckEmploymentDetailsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 inYearAction: InYearAction,
                                                 employmentDetailsView: CheckEmploymentDetailsView,
                                                 appConfig: AppConfig,
                                                 employmentSessionService: EmploymentSessionService,
                                                 auditService: AuditService,
                                                 ec: ExecutionContext,
                                                 errorHandler: ErrorHandler) extends FrontendController(cc) with I18nSupport with SessionHelper {



  //scalastyle:off
  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    val isInYear: Boolean = inYearAction.inYear(taxYear)

    def performAuditAndRenderView(employmentDetailsView: EmploymentDetailsView): Result ={
//      val (name, ref, data, empId) = (source.employerName, source.employerRef, source.employmentData, source.employmentId)
//      val auditModel = ViewEmploymentDetailsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, name, ref, data)
//      auditService.auditModel[ViewEmploymentDetailsAudit](auditModel.toAuditModel)
      Ok(employmentDetailsView(employmentDetailsView, taxYear, isInYear))
    }

    def customerData(allEmploymentData: AllEmploymentData): Option[EmploymentSource] = allEmploymentData.customerEmploymentData.find(source => source.employmentId.equals(employmentId))
    def isUsingCustomerData(allEmploymentData: AllEmploymentData): Boolean = customerData(allEmploymentData).isDefined && !isInYear

    def result(allEmploymentData: AllEmploymentData): Result = {

      val source: Option[EmploymentSource] = if(isUsingCustomerData(allEmploymentData)){
        customerData(allEmploymentData)
      } else {
        allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId))
      }

      source match {
        case Some(source) => performAuditAndRenderView(source.toEmploymentDetailsView(isUsingCustomerData(allEmploymentData)))
        case None => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    if(isInYear){
      employmentSessionService.findPreviousEmploymentUserData(user, taxYear)(result)
    } else {
      employmentSessionService.getAndHandle(taxYear, employmentId) { (cya, prior) =>
        cya match {
          case Some(cya) => Future(performAuditAndRenderView(cya.toEmploymentDetailsView(employmentId,isUsingCustomerData(prior))))
          case None =>
            //TODO save cya to mongo
            Future(result(prior))
        }
      }
    }
  }

  def submit(taxYear:Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    //TODO - Once Create and Update API has been orchestrated
    Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
  }

}
