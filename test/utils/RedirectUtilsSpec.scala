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

class RedirectUtilsSpec extends UnitTest with GuiceOneAppPerSuite {

  "calling the RedirectUtils object" when {
    "asRelativeUrl" must {
      "return a relative url" in {
        val url = "www.fakenotrealtestingwebsite.co.uk/part-one/part-two"
        RedirectUtils.asRelativeUrl(url) shouldBe Some(url)
      }

      "return a relative url with a query and fragment" in {
        val url = "www.fakenotrealtestingwebsite.co.uk/part-one/part-two?q=test#test"
        RedirectUtils.asRelativeUrl(url) shouldBe Some(url)
      }
    }
  }
}
