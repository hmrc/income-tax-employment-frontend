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

package views.expenses

import controllers.expenses.routes.RemoveExpensesController
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.RemoveExpensesView

class RemoveExpensesViewSpec extends ViewUnitTest {

  private val appUrl = "/update-and-submit-income-tax-return/employment-income"

  object Selectors {
    val paragraphTextSelector = "#main-content > div > div > form > p"
    val removeExpensesButtonSelector = "#remove-expenses-button-id"
    val cancelLinkSelector = "#cancel-link-id"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedRemoveExpensesText: String
    val expectedRemoveExpensesButton: String
    val expectedCancelLink: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"PAYE employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedRemoveExpensesText = "This will remove expenses for all employment in this tax year."
    val expectedRemoveExpensesButton = "Remove expenses"
    val expectedCancelLink = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Cyflogaeth TWE ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedRemoveExpensesText = "Bydd hyn yn dileu treuliau ar gyfer pob cyflogaeth yn y flwyddyn dreth hon."
    val expectedRemoveExpensesButton = "Dileu treuliau"
    val expectedCancelLink = "Canslo"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove your expenses?"
    val expectedHeading = "Are you sure you want to remove your expenses?"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove your client’s expenses?"
    val expectedHeading = "Are you sure you want to remove your client’s expenses?"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych yn siŵr eich bod am ddileu’ch treuliau?"
    val expectedHeading = "A ydych yn siŵr eich bod am ddileu’ch treuliau?"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych yn siŵr eich bod am ddileu treuliau’ch cleient?"
    val expectedHeading = "A ydych yn siŵr eich bod am ddileu treuliau’ch cleient?"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[RemoveExpensesView]

  ".show" should {
    import Selectors._
    userScenarios.foreach { userScenario =>
      val common = userScenario.commonExpectedResults
      val specific = userScenario.specificExpectedResults.get
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the remove expenses page when user has expenses" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)
          val employmentSummaryUrl = s"$appUrl/$taxYearEOY/employment-summary"

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(specific.expectedTitle, userScenario.isWelsh)
          h1Check(specific.expectedHeading)
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveExpensesText, paragraphTextSelector)
          buttonCheck(common.expectedRemoveExpensesButton, removeExpensesButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl)
          formPostLinkCheck(RemoveExpensesController.submit(taxYearEOY).url, formSelector)
        }
      }
    }
  }
}
