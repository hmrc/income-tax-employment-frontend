/*
 * Copyright 2023 HM Revenue & Customs
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

package services.studentLoans

import audit._
import config.{AppConfig, ErrorHandler}
import models.employment.createUpdate.CreateUpdateEmploymentRequest
import models.employment.{AllEmploymentData, Deductions, EmploymentSource, StudentLoansCYAModel}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.{AuthorisationRequest, User}
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.EmploymentSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.SessionHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StudentLoansCYAService @Inject()(employmentSessionService: EmploymentSessionService,
                                       val appConfig: AppConfig,
                                       errorHandler: ErrorHandler,
                                       auditService: AuditService,
                                       implicit val ec: ExecutionContext) extends Logging with SessionHelper {

  private[studentLoans] def extractEmploymentInformation(
                                                          allEmploymentData: AllEmploymentData,
                                                          employmentId: String,
                                                          isCustomerHeld: Boolean
                                                        ): Option[EmploymentSource] = {

    if (isCustomerHeld) {
      allEmploymentData.customerEmploymentData.find(_.employmentId == employmentId)
    } else {
      allEmploymentData.hmrcEmploymentData.find(_.employmentId == employmentId).map(_.toEmploymentSource)
    }
  }

  //scalastyle:off
  def retrieveCyaDataAndIsCustomerHeld(taxYear: Int, employmentId: String)(block: (StudentLoansCYAModel, Boolean, Boolean) => Result)
                                      (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Result] = {

    employmentSessionService.getAndHandle(taxYear, employmentId) { case (employmentData, optionalAllEmploymentData) =>
      val studentLoansCya: Option[StudentLoansCYAModel] = employmentData.flatMap(_.employment.studentLoans)
      val customerHeld = optionalAllEmploymentData.exists { allEmploymentData =>
        Seq(
          allEmploymentData.hmrcEmploymentSourceWith(employmentId).map(_.isCustomerData),
          allEmploymentData.eoyEmploymentSourceWith(employmentId).map(_.isCustomerData)
        ).flatten.forall(isCustomer => isCustomer)
      }

      (studentLoansCya, optionalAllEmploymentData, customerHeld) match {
        case (_, Some(allEmploymentData), isCustomerHeld)
          if allEmploymentData.eoyEmploymentSourceWith(employmentId).exists(!_.employmentSource.employmentDetailsSubmittable) =>
          val cya = extractEmploymentInformation(allEmploymentData, employmentId, isCustomerHeld)
            .map(source => EmploymentCYAModel(source, isCustomerHeld, allEmploymentData.otherEmploymentIncome))
          Future.successful(block(cya.flatMap(_.studentLoans)
            .getOrElse(StudentLoansCYAModel(uglDeduction = false, pglDeduction = false)), isCustomerHeld, true))
        case (Some(cya), _, isCustomerHeld) =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] Student Loans CYA data found. Performing block action.")
          Future.successful(block(cya, isCustomerHeld, false))
        case (None, _, _) if employmentData.isDefined =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No Student Loans CYA data exist, " +
            "but employment CYA does. Redirecting to start of SL journey.")
          Future.successful(Redirect(controllers.studentLoans.routes.StudentLoansQuestionController.show(taxYear, employmentId)))
        case (_, Some(alLEmploymentData), isCustomerHeld) =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No CYA data. Constructing CYA from prior data.")
          extractEmploymentInformation(alLEmploymentData, employmentId, isCustomerHeld)
            .map(source => (EmploymentCYAModel(source, isCustomerHeld, alLEmploymentData.otherEmploymentIncome), source.hasPriorBenefits, source.hasPriorStudentLoans)) match {

            case None =>
              logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No employment CYA data extracted from prior data. " +
                "Redirecting back to overview.")
              Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
            case Some((employmentCya, hasPriorBenefits, hasPriorStudentLoans)) =>
              employmentSessionService.createOrUpdateSessionData(request.user, taxYear, employmentId, employmentCya, isPriorSubmission = true,
                hasPriorBenefits = hasPriorBenefits, hasPriorStudentLoans = hasPriorStudentLoans)(errorHandler.internalServerError())({
                employmentCya.studentLoans match {
                  case None =>
                    logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No SL CYA data exist, but there is prior data. " +
                      "Redirecting to the start of the SL journey.")
                    Redirect(controllers.studentLoans.routes.StudentLoansQuestionController.show(taxYear, employmentId))
                  case Some(slCya) =>
                    logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] CYA data exist. Performing block action.")
                    block(slCya, isCustomerHeld, false)
                }
              })
          }
        case _ =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No CYA or prior data. Redirecting to the overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }
  //scalastyle:on

  //test only for when mimicEmploymentAPICalls is true
  def createOrUpdateSessionData(employmentId: String,
                                cya: EmploymentUserData,
                                taxYear: Int,
                                user: User)(onSuccess: Result)(implicit request: AuthorisationRequest[_]): Future[Result] = {

    employmentSessionService.createOrUpdateSessionData(
      user, taxYear, employmentId, cya.employment, isPriorSubmission = true, hasPriorBenefits = true, hasPriorStudentLoans = true
    )(errorHandler.internalServerError())(onSuccess)
  }

  def performSubmitAudits(user: User, createUpdateStudentLoansRequest: CreateUpdateEmploymentRequest, employmentId: String,
                          taxYear: Int, prior: Option[AllEmploymentData])
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {

    val employmentSource = prior.flatMap(_.eoyEmploymentSourceWith(employmentId).map(_.employmentSource))

    val audit: Either[AuditModel[AmendStudentLoansDeductionsUpdateAudit], AuditModel[CreateNewStudentLoansDeductionsAudit]] = employmentSource match {
      case Some(source) =>
        if (source.hasPriorStudentLoans) {
          Left(createUpdateStudentLoansRequest.toAmendStudentLoansAuditModel(user, taxYear, source).toAuditModel)
        } else {
          Right(createUpdateStudentLoansRequest.toCreateStudentLoansAuditModel(user, taxYear).toAuditModel)
        }
      case None => Right(createUpdateStudentLoansRequest.toCreateStudentLoansAuditModel(user, taxYear).toAuditModel)
    }

    audit match {
      case Left(amend) => auditService.sendAudit(amend)
      case Right(create) => auditService.sendAudit(create)
    }
  }

  def sendViewStudentLoansDeductionsAudit(user: User, taxYear: Int, deductions: Option[Deductions])
                                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val auditModel = ViewStudentLoansDeductionsAudit(taxYear, user.affinityGroup.toLowerCase, user.nino, user.mtditid, deductions)
    auditService.sendAudit[ViewStudentLoansDeductionsAudit](auditModel.toAuditModel)
  }
}