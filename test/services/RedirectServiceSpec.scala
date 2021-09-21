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

package services

import models.mongo.{EmploymentCYAModel, EmploymentDetails}
import play.api.http.Status.SEE_OTHER
import utils.UnitTest

import scala.concurrent.Future

class RedirectServiceSpec extends UnitTest {


  val cyaModel = EmploymentCYAModel(EmploymentDetails("employerName", currentDataIsHmrcHeld = true))
  val taxYear = 2021

  "employmentDetailsRedirect" should {
    "redirect to check employment details page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel,taxYear,"employmentId",true)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
    "redirect to employer reference page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel,taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/employer-paye-reference?employmentId=employmentId"
    }
    "redirect to start date page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(employerRef = Some("123/12345"))),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/employment-start-date?employmentId=employmentId"
    }
    "redirect to pay page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01")
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/how-much-pay?employmentId=employmentId"
    }
    "redirect to tax page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1)
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/uk-tax?employmentId=employmentId"
    }
    "redirect to check employment details page when no payroll id" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1), totalTaxToDate = Some(1)
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
    "redirect to check employment details page when no cessation date question" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1), totalTaxToDate = Some(1), payrollId = Some("id")
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
    "redirect to employment end date page when no cessation date" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1), totalTaxToDate = Some(1), payrollId = Some("id"), cessationDateQuestion = Some(true)
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/employment-end-date?employmentId=employmentId"
    }
    "redirect to check employment details page when no cessation date but the cessation question is no" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1), totalTaxToDate = Some(1), payrollId = Some("id"), cessationDateQuestion = Some(false)
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
    "redirect to check employment details page when all filled in" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1), totalTaxToDate = Some(1), payrollId = Some("id"), cessationDateQuestion = Some(true),cessationDate = Some("2020-11-01")
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
  }

}
