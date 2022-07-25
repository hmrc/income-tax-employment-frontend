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

package views.employment

import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.employment.CannotUpdateEmploymentView

class EmploymentCannotUpdateViewSpec extends ViewUnitTest {

  trait CommonExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val continue: String

    def expectedCaption(taxYear: Int): String
    def expectedText(taxYear: Int): String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedH1: String = "PAYE employment"
    val expectedTitle: String = expectedH1
    val continue: String = "Continue"

    def expectedCaption(taxYear: Int): String = s"PAYE employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    def expectedText(taxYear: Int): String = s"you cannot update PAYE employment information until 6 April $taxYear"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedH1: String = "Cyflogaeth TWE"
    val expectedTitle: String = expectedH1
    val continue = "Yn eich blaen"

    def expectedCaption(taxYear: Int): String = s"Cyflogaeth TWE ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    def expectedText(taxYear: Int): String = s"Ni allwch ddiweddaru gwybodaeth am Gyflogaeth TAW tan 6 Ebrill $taxYear"

  }

    protected val userScenarios: Seq[UserScenario[CommonExpectedResults, Option[CommonExpectedResults]]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None)
  )

  private val underTest = inject[CannotUpdateEmploymentView]


  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
    "render the page correctly " when {
      implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
      implicit val messages: Messages = getMessages(userScenario.isWelsh)
      val htmlFormat = underTest(taxYear)
      implicit val document: Document = Jsoup.parse(htmlFormat.body)
      welshToggleCheck(userScenario.isWelsh)
      titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
      h1Check(userScenario.commonExpectedResults.expectedH1)
      captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
      textOnPageCheck(userScenario.commonExpectedResults.expectedText(taxYear), "#cannotUpdate")
      buttonCheck(userScenario.commonExpectedResults.continue, "#returnToOverviewPageBtn")
    }
    }
  }
}
