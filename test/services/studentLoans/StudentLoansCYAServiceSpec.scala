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

import connectors.CreateUpdateEmploymentDataConnector
import models.employment.{AllEmploymentData, EmploymentSource}
import services.EmploymentSessionService
import utils.UnitTest

class StudentLoansCYAServiceSpec extends UnitTest {

  lazy val connector: CreateUpdateEmploymentDataConnector = mock[CreateUpdateEmploymentDataConnector]
  lazy val session: EmploymentSessionService = mock[EmploymentSessionService]
  
  lazy val service = new StudentLoansCYAService(
    connector, session, mockAppConfig, mockErrorHandler, mockExecutionContext, testClock
  )
  
  lazy val employerId = "1234567890"
  
  lazy val hmrcSource: EmploymentSource = EmploymentSource(
    employerId, "HMRC", None, None, None, None, None, None, None, None
  )
  
  lazy val customerSource: EmploymentSource = EmploymentSource(
    employerId, "Customer", None, None, None, None, None, None, None, None
  )
  
  lazy val allEmploymentData: AllEmploymentData = AllEmploymentData(
    Seq(hmrcSource), None, Seq(customerSource), None
  )
  
  ".extractEmploymentInformation" should {
    
    "return an employment source" when {
      
      "the data contains customer data and is flagged as customer held" in {
        val result = service.extractEmploymentInformation(allEmploymentData, employerId, isCustomerHeld = true)
        
        result shouldBe Some(customerSource)
      }
      
      "the data contains hmrc data and is flagged as hmrc held" in {
        val result = service.extractEmploymentInformation(allEmploymentData, employerId, isCustomerHeld = false)
        
        result shouldBe Some(hmrcSource)
      }
      
    }
    
    "return a none" when {
      
      "the data does not contain customer data and is flagged as customer held" in {
        val result = service.extractEmploymentInformation(allEmploymentData.copy(customerEmploymentData = Seq()), employerId, isCustomerHeld = true)
        
        result shouldBe None
      }
      
      "the data does not contain hmrc data and is flagged as hmrc held" in {
        val result = service.extractEmploymentInformation(allEmploymentData.copy(hmrcEmploymentData = Seq()), employerId, isCustomerHeld = false)
        
        result shouldBe None
      }
      
    }
    
  }
  
}
