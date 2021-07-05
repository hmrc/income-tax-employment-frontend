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
import utils.UnitTest

import java.time.LocalDateTime

class InYearActionSpec extends UnitTest {
  val year2021: Int = 2021
  val year2022: Int = 2022
  val month4: Int = 4
  val day5: Int = 5
  val day6: Int = 6
  val hour23: Int = 23
  val minute59: Int = 59


  implicit lazy val mockedConfig: AppConfig = mock[AppConfig]

  def newDate(year: Int, month: Int, day: Int): LocalDateTime = LocalDateTime.of(year, month, day, 0, 0)

  "InYearAction.inYear" when {

    "return true when taxYear is 2022 and current date is 5th April 2022 and time is just before midnight" in {
      val currentDate: LocalDateTime = LocalDateTime.of(year2022, month4, day5, hour23, minute59)
      inYearAction.inYear(year2022, currentDate) shouldBe true
    }

    "return false when taxYear is 2022 and current date is 6th April 2022 at midnight" in {
      val currentDate: LocalDateTime = newDate(year2022, month4, day6)
      inYearAction.inYear(year2022, currentDate) shouldBe false
    }

    "return false when taxYear is 2022 and current date is 6th April 2022 at one minute past midnight" in {
      val currentDate: LocalDateTime = LocalDateTime.of(year2022, month4, day6, 0, 1)
      inYearAction.inYear(year2022, currentDate) shouldBe false
    }

    "return false when taxYear is 2022 and current date is 6th April 2022 at one hour past midnight" in {
      val currentDate: LocalDateTime = LocalDateTime.of(year2022, month4, day6, 1, 1)
      inYearAction.inYear(year2022, currentDate) shouldBe false
    }
  }
}
