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
import connectors.CreateUpdateEmploymentDataConnector
import connectors.parsers.CreateUpdateEmploymentDataHttpParser.CreateUpdateEmploymentDataResponse
import models.User
import models.employment.{AllEmploymentData, EmploymentSource, StudentLoansCYAModel}
import models.mongo.EmploymentCYAModel
import org.slf4j
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.EmploymentSessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StudentLoansCYAService @Inject()(
                                        employmentConnector: CreateUpdateEmploymentDataConnector,
                                        employmentSessionService: EmploymentSessionService,
                                        appConfig: AppConfig,
                                        errorHandler: ErrorHandler,
                                        implicit val ec: ExecutionContext,
                                        implicit val clock: Clock
                                      ) {

  lazy val logger: slf4j.Logger = Logger.apply(this.getClass).logger

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
  def retrieveCyaDataAndIsCustomerHeld(taxYear: Int, employmentId: String)(block: Option[(StudentLoansCYAModel, Boolean)] => Result)
                                      (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {

    employmentSessionService.getAndHandle(taxYear, employmentId) { case (employmentData, optionalAllEmploymentData) =>
      val studentLoansCya: Option[StudentLoansCYAModel] = employmentData.flatMap(_.employment.studentLoans)
      val customerHeld = optionalAllEmploymentData.exists { allEmploymentData =>
        Seq(
          allEmploymentData.inYearEmploymentSourceWith(employmentId).map(_.isCustomerData),
          allEmploymentData.eoyEmploymentSourceWith(employmentId).map(_.isCustomerData)
        ).flatten.forall(isCustomer => isCustomer)
      }

      (studentLoansCya, optionalAllEmploymentData, customerHeld) match {
        case (Some(cya), _, isCustomerHeld) => Future.successful(block(Some(cya, isCustomerHeld)))
        case (_, Some(alLEmploymentData), isCustomerHeld) =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No CYA data. Constructing CYA from prior data.")
          extractEmploymentInformation(alLEmploymentData, employmentId, isCustomerHeld)
            .map(source => (EmploymentCYAModel(source, isCustomerHeld), source.hasPriorBenefits))
            .fold(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))) { case (employmentCya, hasPriorBenefits) =>
              employmentSessionService.createOrUpdateSessionData(
                employmentId, employmentCya, taxYear, isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits
              )(errorHandler.internalServerError()) {
                block(employmentCya.studentLoans.map(cya => (cya, isCustomerHeld)))
              }
            }

        case _ =>
          logger.debug("[StudentLoansCYAService][retrieveCyaDataAndIsCustomerHeld] No CYA or prior data. Redirecting to the overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  def submitStudentLoans(taxYear: Int, employmentId: String)(block: CreateUpdateEmploymentDataResponse => Result)
                        (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {

    employmentSessionService.getAndHandle(taxYear, employmentId) { case (employment, allEmploymentData) =>
      employment.fold(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))))(employmentData => {
        employmentSessionService.createModelAndReturnResult(
          employmentData, allEmploymentData, taxYear, EmploymentSection.STUDENT_LOANS
        ) { createUpdateRequest =>
          employmentConnector.createUpdateEmploymentData(
            user.nino, taxYear, createUpdateRequest
          )(hc.withExtraHeaders("mtditid" -> user.mtditid)).map(response => block(response))
        }
      })
    }
  }

}
