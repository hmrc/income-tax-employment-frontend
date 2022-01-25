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

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import connectors.CreateUpdateEmploymentDataConnector
import connectors.parsers.CreateUpdateEmploymentDataHttpParser.CreateUpdateEmploymentDataResponse
import models.User
import models.employment.{AllEmploymentData, StudentLoansCYAModel}
import models.employment.createUpdate.CreateUpdateEmploymentRequest
import models.mongo.{DatabaseError, EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.scalamock.handlers.CallHandler3
import play.api.mvc.Result
import play.api.mvc.Results.{Created, InternalServerError, NoContent}
import repositories.EmploymentUserDataRepository
import services.EmploymentSessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.Future

class StudentLoansCYAServiceSpec extends UnitTest {
  
  val taxYear: Int = 2022
  val employmentId: String = "1234567890-1234567890"
  
  lazy val repo: EmploymentUserDataRepository = mock[EmploymentUserDataRepository]
  lazy val connector: CreateUpdateEmploymentDataConnector = mock[CreateUpdateEmploymentDataConnector]
  lazy val session: EmploymentSessionService = mock[EmploymentSessionService]
  
  private def mockRepoCall(employmentData: Option[EmploymentUserData]) = {
    (repo.find(_: Int, _: String)(_: User[_]))
      .expects(*, *, *)
      .returns(Future.successful(Right(employmentData)))
  }
  
  lazy val controller = new StudentLoansCYAService(repo, connector, session, mockAppConfig, mockExecutionContext)
  
  ".retrieveCyaData" should {
    
    "return a CYA model" when {
      
      "data exist in session (mongo)" in {
        val employmentUserData = EmploymentUserData(
          sessionId, mtditid, nino, taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false,
          EmploymentCYAModel(
            EmploymentDetails(
              "AN Employer",
              currentDataIsHmrcHeld = false
            ),
            studentLoansCYAModel = Some(StudentLoansCYAModel(uglDeduction = false, None, pglDeduction = false, None))
          )
        )
        
        val result = {
          mockRepoCall(Some(employmentUserData))
          await(controller.retrieveCyaDataAndIsPrior(taxYear, employmentId)(user))
        }
        
        result shouldBe employmentUserData.employment.studentLoansCYAModel.map(_ -> false)
      }
      
    }
    
    "return a None" when {
      
      "there's no data in session (mongo)" in {
        val result = {
          mockRepoCall(None)
          await(controller.retrieveCyaDataAndIsPrior(taxYear, employmentId)(user))
        }

        result shouldBe None
      }
      
    }
    
  }
  
}
