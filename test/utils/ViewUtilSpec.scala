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

package utils

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import utils.ViewUtils.ariaVisuallyHiddenText

import java.time.LocalDate

class ViewUtilSpec extends UnitTest with GuiceOneAppPerSuite with ViewTest {

  "calling method convertBoolToYesNo" should {
    "return yes when employment field is true" in {
      ViewUtils.convertBoolToYesOrNo(Some(true)).get shouldBe "Yes"
    }

    "return no when employment field is false" in {
      ViewUtils.convertBoolToYesOrNo(Some(false)).get shouldBe "No"
    }
  }

  "calling method DateFormatter" should {
    "reformat date when valid date is passed" in {
      ViewUtils.dateFormatter("2022-03-10").get shouldBe "10 March 2022"
    }

    "return None when invalid date is passed" in {
      ViewUtils.dateFormatter("10-03-2022") shouldBe None
      ViewUtils.dateFormatter("2022/03/10") shouldBe None
      ViewUtils.dateFormatter("01 March 2022") shouldBe None
    }
  }

  "ariaVisuallyHiddenText" should {
    "return visually hidden element containing given text" in {
      ariaVisuallyHiddenText("some-text") shouldBe HtmlContent("""<span class="govuk-visually-hidden">some-text</span>""")
    }
  }

  "bigDecimalCurrency" should {
    "Place comma in appropriate place when given amount over 999" in {
      ViewUtils.bigDecimalCurrency("45000.10") shouldBe "£45,000.10"
    }
  }

  "translatedDateFormatter" should {
    "return date in Welsh or English" in {
      ViewUtils.translatedDateFormatter(LocalDate.parse("2021-04-01"))(getMessages(isWelsh = true)) shouldBe "1 Ebrill 2021"
      ViewUtils.translatedDateFormatter(LocalDate.parse("2021-04-01"))(getMessages(isWelsh = false)) shouldBe "1 April 2021"
    }
  }

  "employmentDatesFormatter" should {
    "return date in Welsh or English" in {
      ViewUtils.employmentDatesFormatter(Some("2022-02-01"),
        Some("2023-04-03"))(getMessages(isWelsh = false)) shouldBe Some("1 February 2022 to 3 April 2023")
      ViewUtils.employmentDatesFormatter(Some("2022-05-02"),
        Some("2023-06-04"))(getMessages(isWelsh = true)) shouldBe Some("2 Mai 2022 i 4 Mehefin 2023")
    }

    "return a string when start date is not defined" in {
      ViewUtils.employmentDatesFormatter(None, Some("2022-03-04")) shouldBe Some(" to 4 March 2022")
    }

    "return a string when end date is not defined" in {
      ViewUtils.employmentDatesFormatter(Some("2022-10-01"), None) shouldBe Some("1 October 2022 to ")
    }
  }
}
