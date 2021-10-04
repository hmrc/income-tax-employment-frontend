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

import models.employment.{BenefitsViewModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Results.Ok
import services.RedirectService.{EmploymentBenefitsType, EmploymentDetailsType}
import utils.UnitTest

import scala.concurrent.Future

class RedirectServiceSpec extends UnitTest {

  val cyaModel: EmploymentCYAModel = EmploymentCYAModel(EmploymentDetails("employerName", currentDataIsHmrcHeld = true))
  val taxYear = 2021

  val employmentCYA: EmploymentCYAModel = {
    EmploymentCYAModel(
      employmentDetails = EmploymentDetails(
        "Employer Name",
        employerRef = Some(
          "123/12345"
        ),
        startDate = Some("2020-11-11"),
        taxablePayToDate = Some(55.99),
        totalTaxToDate = Some(3453453.00),
        employmentSubmittedOn = Some("2020-04-04T01:01:01Z"),
        employmentDetailsSubmittedOn = Some("2020-04-04T01:01:01Z"),
        currentDataIsHmrcHeld = false
      ),
      employmentBenefits = Some(
        BenefitsViewModel(
          carVanFuelModel = Some(CarVanFuelModel(
            carVanFuelQuestion = Some(true),
            mileageQuestion = Some(true)
          )),
          accommodation = Some(100), submittedOn = Some("2020-02-04T05:01:01Z"), isUsingCustomerData = true,
          isBenefitsReceived = true
        )
      ))
  }

  val employmentUserData: EmploymentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYear, "001", false, employmentCYA)

  "redirectBasedOnCurrentAnswers" should {
    "redirect to benefits yes no page" when {
      "its a new submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = None))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => Future.successful(Ok("Wow"))
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-benefits?employmentId=001"
      }
    }
    "redirect to benefits CYA page" when {
      "its a prior submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(isPriorSubmission = true, employment = employmentCYA.copy(employmentBenefits = None))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => Future.successful(Ok("Wow"))
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
    }
    "redirect when benefits are setup but car van fuel is empty" when {
      "its a new submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = None))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => Future.successful(Ok("Wow"))
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/car-van-fuel?employmentId=001"
      }
    }
    "redirect when CYA is empty" when {
      "its a benefits submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001", None, EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => Future.successful(Ok("Wow"))
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "its a employment details submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001", None, EmploymentDetailsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => Future.successful(Ok("Wow"))
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=001"
      }
    }
    "continue with the request when benefits are setup and car van fuel is setup" when {
      "its a new submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear,"001",
          Some(employmentUserData),EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ){
          _ => Future.successful(Ok("Wow"))
        }

        status(response) shouldBe OK
        bodyOf(response) shouldBe "Wow"
      }
      "its a prior submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear,"001",
          Some(employmentUserData.copy(isPriorSubmission = true)),EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ){
          _ => Future.successful(Ok("Wow"))
        }

        status(response) shouldBe OK
        bodyOf(response) shouldBe "Wow"
      }
    }
  }

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
    "redirect to still working for employer page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), payrollId = Some("id"), startDate = Some("2020-11-01"))),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/still-working-for-employer?employmentId=employmentId"
    }
    "redirect to pay page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), cessationDateQuestion = Some(true), cessationDate = Some("2020-10-01"),payrollId = Some("id")
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/how-much-pay?employmentId=employmentId"
    }
    "redirect to tax page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), cessationDateQuestion = Some(true), cessationDate = Some("2020-10-10"),  payrollId = Some("id"), taxablePayToDate = Some(1)
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/uk-tax?employmentId=employmentId"
    }
    "redirect to payroll id page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), cessationDateQuestion = Some(true), cessationDate = Some("2020-10-10"),taxablePayToDate = Some(1), totalTaxToDate = Some(1)
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/payroll-id?employmentId=employmentId"
    }
    "redirect to employment end date page when no cessation date" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), cessationDateQuestion = Some(false), taxablePayToDate = Some(1),
        totalTaxToDate = Some(1), payrollId = Some("id")
      )),taxYear,"employmentId",isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/employment-end-date?employmentId=employmentId"
    }
    "redirect to check employment details page when no cessation date but the cessation question is no" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1), totalTaxToDate = Some(1),
        payrollId = Some("id"), cessationDateQuestion = Some(true)
      )),taxYear,"employmentId",isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
    "redirect to check employment details page when all filled in" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1), totalTaxToDate = Some(1),
        payrollId = Some("id"), cessationDateQuestion = Some(true),cessationDate = Some("2020-11-01")
      )),taxYear,"employmentId",false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
  }

}
