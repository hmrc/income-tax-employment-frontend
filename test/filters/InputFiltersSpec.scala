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

package filters

import utils.UnitTest

class InputFiltersSpec extends UnitTest with InputFilters {

  "Input filter" must {
    "filter out those hackers" in {

      filter("<script>(.*?)</script>") shouldBe  ""
      filter("<script(.*?)>") shouldBe ""
      filter("</script>") shouldBe  ""
      filter("javascript:") shouldBe ""
      filter("vbscript:") shouldBe ""
      filter("onload(.*?)=") shouldBe ""
      filter("eval((.*?)") shouldBe ""
      filter("expression((.*?)") shouldBe ""
      filter("abc|bcd") shouldBe "abcbcd"

    }
  }

}
