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

package utils

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class DateTimeUtilSpec extends UnitTest with GuiceOneAppPerSuite {

  "calling the DateTimeUtil object" when {
    "a valid timestamp" must {
      "return a zoned date time" in {
        val time = Some("2020-01-04T05:01:01Z")
        DateTimeUtil.getSubmittedOnDateTime(time).toString shouldBe "Some(2020-01-04T05:01:01Z)"
      }

      "return an exception when its not a valid timestamp" in {
        val time = Some("time")
        DateTimeUtil.getSubmittedOnDateTime(time) shouldBe None
      }
    }
  }
}
