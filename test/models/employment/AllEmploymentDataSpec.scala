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

package models.employment

import models.employment.AllEmploymentData.employmentIdExists
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import support.builders.models.expenses.ExpensesBuilder.anExpenses
import support.{TaxYearProvider, UnitTest}

class AllEmploymentDataSpec extends UnitTest with TaxYearProvider {

  private val employmentId = "some-employment-id"

  private val hmrcEmployment1 = aHmrcEmploymentSource.copy(employmentId = "employment-1", employerName = "employer-name-1", submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"))
  private val hmrcEmployment2 = aHmrcEmploymentSource.copy(employmentId = "employment-2", employerName = "employer-name-2", submittedOn = Some(s"${taxYearEOY - 1}-05-04T05:01:01Z"))
  private val hmrcEmployment3 = aHmrcEmploymentSource.copy(employmentId = "employment-3", employerName = "employer-name-3", submittedOn = Some(s"${taxYearEOY - 1}-10-04T05:01:01Z"))

  private val customerEmployment1 = anEmploymentSource.copy(employmentId = "employment-4", employerName = "employer-name-4", submittedOn = Some(s"$taxYearEOY-01-04T05:01:01Z"))
  private val customerEmployment2 = anEmploymentSource.copy(employmentId = "employment-5", employerName = "employer-name-5", submittedOn = Some(s"$taxYearEOY-05-04T05:01:01Z"))
  private val customerEmployment3 = anEmploymentSource.copy(employmentId = "employment-6", employerName = "employer-name-6", submittedOn = Some(s"$taxYearEOY-10-04T05:01:01Z"))

  private val hmrcExpenses = anEmploymentExpenses.copy(expenses = Some(anExpenses.copy(businessTravelCosts = Some(10.00))))
  private val customerExpenses = anEmploymentExpenses.copy(expenses = Some(anExpenses.copy(businessTravelCosts = Some(20.00))))

  "latestInYearEmployments" should {
    "return hmrcEmploymentData only ordered by submission date in descending order" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcEmploymentData = Seq(hmrcEmployment1, hmrcEmployment2, hmrcEmployment3))
        .copy(customerEmploymentData = Seq(customerEmployment1, customerEmployment2, customerEmployment3))

      allEmploymentData.latestInYearEmployments shouldBe Seq(hmrcEmployment3.toEmploymentSource, hmrcEmployment2.toEmploymentSource, hmrcEmployment1.toEmploymentSource)
    }
  }

  "latestNotInYearEmployments" should {
    "return all employment sources ordered by submission date in descending order" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcEmploymentData = Seq(hmrcEmployment1, hmrcEmployment2, hmrcEmployment3))
        .copy(customerEmploymentData = Seq(customerEmployment1, customerEmployment2, customerEmployment3))

      allEmploymentData.latestEOYEmployments shouldBe Seq(
        customerEmployment3, customerEmployment2, customerEmployment1,
        hmrcEmployment3.toEmploymentSource, hmrcEmployment2.toEmploymentSource, hmrcEmployment1.toEmploymentSource
      )
    }
  }

  "isLastEOYEmployment" should {
    "return true when" when {
      "EOY employments contain only one item" in {
        val allEmploymentData = anAllEmploymentData
          .copy(hmrcEmploymentData = Seq())
          .copy(customerEmploymentData = Seq(customerEmployment2))

        allEmploymentData.isLastEOYEmployment shouldBe true
      }
    }

    "return false when" when {
      "EOY employments contain more than one employment" in {
        val allEmploymentData = anAllEmploymentData
          .copy(hmrcEmploymentData = Seq(hmrcEmployment1))
          .copy(customerEmploymentData = Seq(customerEmployment2))

        allEmploymentData.isLastEOYEmployment shouldBe false
      }

      "EOY employments not present" in {
        val allEmploymentData = anAllEmploymentData
          .copy(hmrcEmploymentData = Seq())
          .copy(customerEmploymentData = Seq())

        allEmploymentData.isLastEOYEmployment shouldBe false
      }
    }
  }

  "isLastInYearEmployment" should {
    "return true when" when {
      "in year employments contain only one item" in {
        val allEmploymentData = anAllEmploymentData
          .copy(hmrcEmploymentData = Seq(hmrcEmployment1))
          .copy(customerEmploymentData = Seq(customerEmployment2))

        allEmploymentData.isLastInYearEmployment shouldBe true
      }
    }

    "return false when" when {
      "in year employments contain more than one employment" in {
        val allEmploymentData = anAllEmploymentData
          .copy(hmrcEmploymentData = Seq(hmrcEmployment1, hmrcEmployment2))
          .copy(customerEmploymentData = Seq())

        allEmploymentData.isLastInYearEmployment shouldBe false
      }

      "in year employments not present" in {
        val allEmploymentData = anAllEmploymentData
          .copy(hmrcEmploymentData = Seq())
          .copy(customerEmploymentData = Seq())

        allEmploymentData.isLastInYearEmployment shouldBe false
      }
    }
  }

  "latestInYearExpenses" should {
    "return a pair of hmrc expenses and isCustomerData false when expenses exist" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcExpenses = Some(hmrcExpenses))
        .copy(customerExpenses = Some(customerExpenses))

      allEmploymentData.latestInYearExpenses shouldBe Some(LatestExpensesOrigin(hmrcExpenses, isCustomerData = false))
    }

    "return none when ignored" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcExpenses = Some(hmrcExpenses.copy(dateIgnored = Some("2020-10-10"))))
        .copy(customerExpenses = None)

      allEmploymentData.latestInYearExpenses shouldBe None
    }

    "return None when hmrc expenses do not exist" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcExpenses = None)
        .copy(customerExpenses = Some(customerExpenses))

      allEmploymentData.latestInYearExpenses shouldBe None
    }
  }

  "latestEOYExpenses" should {
    "return a pair of hmrc expenses and isCustomerData false when hmrc expenses and no customer expenses" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcExpenses = Some(hmrcExpenses))
        .copy(customerExpenses = None)

      allEmploymentData.latestEOYExpenses shouldBe Some(LatestExpensesOrigin(hmrcExpenses, isCustomerData = false))
    }
    "return none when ignored" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcExpenses = Some(hmrcExpenses.copy(dateIgnored = Some("2020-10-10"))))
        .copy(customerExpenses = None)

      allEmploymentData.latestEOYExpenses shouldBe None
    }

    "return a pair of customer expenses and isCustomerData true when customer expenses exist" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcExpenses = Some(hmrcExpenses))
        .copy(customerExpenses = Some(customerExpenses))

      allEmploymentData.latestEOYExpenses shouldBe Some(LatestExpensesOrigin(customerExpenses, isCustomerData = true))
    }

    "return None when no hmrc expenses and no customer expenses" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcExpenses = None)
        .copy(customerExpenses = None)

      allEmploymentData.latestEOYExpenses shouldBe None
    }
  }

  "hmrcEmploymentSourceWith" should {
    "return hmrc employment data with isCustomerData set to false when employment exists" in {
      val expectedEmployment = hmrcEmployment1.copy(employmentId = "some-employment-id")
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcEmploymentData = Seq(expectedEmployment, hmrcEmployment2, hmrcEmployment3))
        .copy(customerEmploymentData = Seq(customerEmployment1, customerEmployment2, customerEmployment3))

      allEmploymentData.hmrcEmploymentSourceWith("some-employment-id") shouldBe Some(EmploymentSourceOrigin(expectedEmployment.toEmploymentSource, isCustomerData = false))
    }

    "return None when employment does not exist" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcEmploymentData = Seq(hmrcEmployment1, hmrcEmployment2, hmrcEmployment3))
        .copy(customerEmploymentData = Seq(customerEmployment1, customerEmployment2, customerEmployment3))

      allEmploymentData.hmrcEmploymentSourceWith("unknown-employment-id") shouldBe None
    }
  }

  "eoyEmploymentSourceWith" should {
    "return customer employment data when employment is found" in {
      val expectedEmployment = customerEmployment1.copy(employmentId = "some-employment-id")
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcEmploymentData = Seq(hmrcEmployment1, hmrcEmployment2, hmrcEmployment3))
        .copy(customerEmploymentData = Seq(expectedEmployment, customerEmployment2, customerEmployment3))

      allEmploymentData.eoyEmploymentSourceWith("some-employment-id") shouldBe Some(EmploymentSourceOrigin(expectedEmployment, isCustomerData = true))
    }

    "return hmrc employment data when employment is found" in {
      val expectedEmployment = hmrcEmployment1.copy(employmentId = "some-employment-id")
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcEmploymentData = Seq(expectedEmployment, hmrcEmployment2, hmrcEmployment3))
        .copy(customerEmploymentData = Seq(customerEmployment1, customerEmployment2, customerEmployment3))

      allEmploymentData.eoyEmploymentSourceWith("some-employment-id") shouldBe Some(EmploymentSourceOrigin(expectedEmployment.toEmploymentSource, isCustomerData = false))
    }

    "return None when employment not found in both hmrc and customer employment data" in {
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcEmploymentData = Seq(hmrcEmployment1, hmrcEmployment2, hmrcEmployment3))
        .copy(customerEmploymentData = Seq(customerEmployment1, customerEmployment2, customerEmployment3))

      allEmploymentData.eoyEmploymentSourceWith("unknown-employment-id") shouldBe None
    }

    "return None when employment there is hmrc employment, but it is ignored" in {
      val ignoredEmployment = hmrcEmployment1.copy(employmentId = "employment-id", dateIgnored = Some(s"$taxYearEOY-03-11"))
      val allEmploymentData = anAllEmploymentData
        .copy(hmrcEmploymentData = Seq(ignoredEmployment))
        .copy(customerEmploymentData = Seq())

      allEmploymentData.eoyEmploymentSourceWith("employment-id") shouldBe None
    }
  }
  "employmentIdExists" should {
    "return true if the employmentId exists in HMRC Data only" in {
      val hmrcEmploymentSource = aHmrcEmploymentSource.copy(employmentId = employmentId)
      val employmentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(hmrcEmploymentSource))

      employmentIdExists(employmentData, Some(employmentId)) shouldBe true
    }

    "return true if the employmentId exists in Customer Data only" in {
      val customerEmploymentSource = anEmploymentSource.copy(employmentId = employmentId)
      val employmentData = anAllEmploymentData.copy(customerEmploymentData = Seq(customerEmploymentSource))

      employmentIdExists(employmentData, Some(employmentId)) shouldBe true
    }

    "return false if the employmentId does not exists in Customer and HMRC Data" in {
      val hmrcEmploymentSource = aHmrcEmploymentSource.copy(employmentId = "different-employment-Id")
      val customerEmploymentSource = anEmploymentSource.copy(employmentId = "different-employment-Id")
      val employmentData = anAllEmploymentData.copy(
        hmrcEmploymentData = Seq(hmrcEmploymentSource),
        customerEmploymentData = Seq(customerEmploymentSource)
      )

      employmentIdExists(employmentData, Some(employmentId)) shouldBe false
    }

    "return false if employmentId is not defined" in {
      val customerEmploymentSource = anEmploymentSource.copy(employmentId = employmentId)
      val employmentData = anAllEmploymentData.copy(customerEmploymentData = Seq(customerEmploymentSource))

      employmentIdExists(employmentData, None) shouldBe false
    }
  }
}
