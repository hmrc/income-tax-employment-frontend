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

package services.studentLoans

import common.EmploymentSection
import config.{AppConfig, ErrorHandler}
import models.AuthorisationRequest
import models.employment.{AllEmploymentData, EmploymentSource, StudentLoansCYAModel}
import models.mongo.EmploymentCYAModel
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.EmploymentSessionService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StudentLoansCYAService @Inject()(employmentSessionService: EmploymentSessionService,
                                       appConfig: AppConfig,
                                       errorHandler: ErrorHandler,
                                       implicit val ec: ExecutionContext) extends Logging {

  private[studentLoans] def extractEmploymentInformation(
                                                          allEmploymentData: AllEmploymentData,
                                                          employmentId: String,
                                                          isCustomerHeld: Boolean
                                                        ): Option[EmploymentSource] = {

    if (isCustomerHeld) {
      allEmploymentData.customerEmploymentData.find(_.employmentId == employmentId)
    } else {
      allEmploymentData.hmrcEmploymentData.find(_.employmentId == employmentId)
    }
  }

  //noinspection ScalaStyle
  def retrieveCyaDataAndIsCustomerHeld(taxYear: Int, employmentId: String)(block: (StudentLoansCYAModel, Boolean, Boolean) => Result)
                                      (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Result] = {

    employmentSessionService.getAndHandle(taxYear, employmentId) { case (employmentData, optionalAllEmploymentData) =>
      val studentLoansCya: Option[StudentLoansCYAModel] = employmentData.flatMap(_.employment.studentLoans)
      val customerHeld = optionalAllEmploymentData.exists { allEmploymentData =>
        Seq(
          allEmploymentData.inYearEmploymentSourceWith(employmentId).map(_.isCustomerData),
          allEmploymentData.eoyEmploymentSourceWith(employmentId).map(_.isCustomerData)
        ).flatten.forall(isCustomer => isCustomer)
      }

      (studentLoansCya, optionalAllEmploymentData, customerHeld) match {
        case (Some(cya), _, isCustomerHeld) =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] Student Loans CYA data found. Performing block action.")
          Future.successful(block(cya, isCustomerHeld, optionalAllEmploymentData.fold(false)(data => data.isLastInYearEmployment)))
        case (None, _, _) if employmentData.isDefined =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No Student Loans CYA data exist, but employment CYA does. Redirecting to start of SL journey.")
          Future.successful(Redirect(controllers.studentLoans.routes.StudentLoansQuestionController.show(taxYear, employmentId)))
        case (_, Some(alLEmploymentData), isCustomerHeld) =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No CYA data. Constructing CYA from prior data.")
          extractEmploymentInformation(alLEmploymentData, employmentId, isCustomerHeld)
            .map(source => (EmploymentCYAModel(source, isCustomerHeld), source.hasPriorBenefits, source.hasPriorStudentLoans)) match {

            case None =>
              logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No employment CYA data extracted from prior data. Redirecting back to overview.")
              Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
            case Some((employmentCya, hasPriorBenefits, hasPriorStudentLoans)) =>
              employmentSessionService.createOrUpdateSessionData(
                employmentId, employmentCya, taxYear, isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits,
                hasPriorStudentLoans = hasPriorStudentLoans, request.user
              )(errorHandler.internalServerError()) {
                employmentCya.studentLoans match {
                  case None =>
                    logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No SL CYA data exist, but there is prior data. Redirecting to the start of the SL journey.")
                    Redirect(controllers.studentLoans.routes.StudentLoansQuestionController.show(taxYear, employmentId))
                  case Some(slCya) =>
                    logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] CYA data exist. Performing block action.")
                    block(slCya, isCustomerHeld, alLEmploymentData.isLastInYearEmployment)
                }
              }
          }
        case _ =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No CYA or prior data. Redirecting to the overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  def submitStudentLoans(taxYear: Int, employmentId: String, result: Option[String] => Result)
                        (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Result] = {

    employmentSessionService.getAndHandle(taxYear, employmentId) { case (employment, allEmploymentData) =>
      employment.fold(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))))(employmentData => {

        employmentSessionService.createModelOrReturnResult(request.user, employmentData, allEmploymentData, taxYear, EmploymentSection.STUDENT_LOANS) match {

          case Left(result) => Future.successful(result)
          case Right(model) =>

            employmentSessionService.createOrUpdateEmploymentResult(taxYear, model).flatMap {
              case Left(result) => Future.successful(result)
              case Right(returnedEmploymentId) =>

                employmentSessionService.clear(request.user, taxYear, employmentId).map {
                  case Left(_) => errorHandler.internalServerError()
                  case Right(_) => result(returnedEmploymentId)
                }
            }
        }
      })
    }
  }

}
