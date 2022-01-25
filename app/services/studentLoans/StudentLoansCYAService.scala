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

import config.AppConfig
import connectors.CreateUpdateEmploymentDataConnector
import connectors.parsers.CreateUpdateEmploymentDataHttpParser.CreateUpdateEmploymentDataResponse
import models.User
import models.employment.StudentLoansCYAModel
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import repositories.EmploymentUserDataRepository
import services.EmploymentSessionService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StudentLoansCYAService @Inject()(
                                        repo: EmploymentUserDataRepository,
                                        employmentConnector: CreateUpdateEmploymentDataConnector,
                                        employmentSessionService: EmploymentSessionService,
                                        appConfig: AppConfig,
                                        implicit val ec: ExecutionContext
                                      ) {

  def retrieveCyaDataAndIsPrior(taxYear: Int, employmentId: String)(implicit user: User[_]): Future[Option[(StudentLoansCYAModel, Boolean)]] = {
    repo.find(taxYear, employmentId).map {
      case Right(data) => data.flatMap(sessionData => sessionData.employment.studentLoansCYAModel.map(_ -> sessionData.isPriorSubmission))
      case Left(_) => None
    }
  }
  
  //noinspection ScalaStyle
  def submitStudentLoans(taxYear: Int, employmentId: String)(block: Option[CreateUpdateEmploymentDataResponse] => Result)(implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    employmentSessionService.getAndHandle(taxYear, employmentId) { case (employment, prior) =>
      employment.fold(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))))(employmentData => {
        employmentSessionService.cyaAndPriorToCreateUpdateEmploymentRequest(employmentData, prior) match {
          case Right(createUpdateRequest) =>
            employmentConnector.createUpdateEmploymentData(user.nino, taxYear, createUpdateRequest)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map(response => block(Some(response)))
          case Left(_) => Future.successful(block(None))
        }
      })
    }
  }

}
