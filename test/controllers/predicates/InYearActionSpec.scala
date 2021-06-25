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

package controllers.predicates

import config.AppConfig
import org.joda.time.DateTime
import utils.UnitTest

class InYearActionSpec extends UnitTest {
  val currentYear = DateTime.now.getYear
  val lastYear: Int = DateTime.now.getYear - 1
  val nextYear: Int = DateTime.now.getYear + 1

  implicit lazy val mockedConfig: AppConfig = mock[AppConfig]
  val inYearAction: InYearAction = new InYearAction

  "InYearAction.inYear" when {

    "taxYearErrorFeature is enabled" should {
      "return false when taxYear is 2021 and current date is 5th April 2022" in {
        val beforeApril = new DateTime(2022, 4, 5, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning true
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2021, beforeApril) shouldBe false
      }

      "return false when taxYear is 2021 and current date is 6th April 2022" in {
        val currentDate = new DateTime(2022, 4, 6, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning true
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2021, currentDate) shouldBe false
      }

      "return true when taxYear is 2022 and current date is 5th April 2022" in {
        val currentDate = new DateTime(2022, 4, 5, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning true
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2022, currentDate) shouldBe true
      }

      "return false when taxYear is 2022 and current date is 6th April 2022" in {
        val currentDate = new DateTime(2022, 4, 6, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning true
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2022, currentDate) shouldBe false
      }

      "return false when taxYear is 2023 and current date is 5th April 2023" in {
        val currentDate = new DateTime(2023, 4, 5, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning true
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2023, currentDate) shouldBe false
      }

      "return false when taxYear is 2023 and current date is 6th April 2023" in {
        val currentDate = new DateTime(2023, 4, 5, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning true
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2023, currentDate) shouldBe false
      }
    }

    "taxYearErrorFeature is disabled" should {
      "return true when taxYear is 2021 and current date is 5th April 2022" in {
        val beforeApril = new DateTime(2022, 4, 5, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning false
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2021, beforeApril) shouldBe true
      }

      "return true when taxYear is 2021 and current date is 6th April 2022" in {
        val currentDate = new DateTime(2022, 4, 6, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning false
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2021, currentDate) shouldBe true
      }

      "return true when taxYear is 2022 and current date is 5th April 2022" in {
        val currentDate = new DateTime(2022, 4, 5, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning false
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2022, currentDate) shouldBe true
      }

      // TODO - is this correct
      "return false when taxYear is 2022 and current date is 6th April 2022" in {
        val currentDate = new DateTime(2022, 4, 6, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning false
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2022, currentDate) shouldBe false
      }

      "return false when taxYear is 2023 and current date is 5th April 2023" in {
        val currentDate = new DateTime(2023, 4, 6, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning false
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2023, currentDate) shouldBe false
      }

      "return true when taxYear is 2023 and current date is 6th April 2023" in {
        val currentDate = new DateTime(2023, 4, 5, 0, 0)
        mockedConfig.taxYearErrorFeature _ expects() returning false
        mockedConfig.defaultTaxYear _ expects() returning 2022
        inYearAction.inYear(2023, currentDate) shouldBe true
      }
    }

  }
}
