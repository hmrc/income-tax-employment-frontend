/*
 * Copyright 2023 HM Revenue & Customs
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

package views.errors

import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.errors.IndividualUnauthorisedView

class IndividualUnauthorisedViewSpec extends ViewUnitTest {

  private val signUpForMTDLink: String = "https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax"

  object Selectors {
    val paragraphSelector: String = ".govuk-body"
    val linkSelector: String = paragraphSelector + " > a"
  }

  trait CommonExpectedResults {
    val validTitle: String
    val pageContent: String
    val linkContent: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val validTitle: String = "You cannot view this page"
    val pageContent: String = "You need to sign up for Making Tax Digital for Income Tax before you can view this page."
    val linkContent: String = "sign up for Making Tax Digital for Income Tax"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val validTitle: String = "Ni allwch fwrw golwg dros y dudalen hon"
    val pageContent: String = "Mae angen cofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm cyn i chi allu bwrw golwg dros y dudalen hon."
    val linkContent: String = "cofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  private lazy val underTest = inject[IndividualUnauthorisedView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "return the AgentAuthErrorPageView with the right content" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest()

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(validTitle, userScenario.isWelsh)
        welshToggleCheck(userScenario.isWelsh)
        h1Check(validTitle, size = "xl")
        textOnPageCheck(pageContent, paragraphSelector)
        linkCheck(linkContent, linkSelector, signUpForMTDLink)
      }

    }
  }
}
