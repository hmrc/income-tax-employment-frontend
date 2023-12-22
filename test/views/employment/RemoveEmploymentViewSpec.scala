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

package views.employment

import controllers.employment.routes._
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.ViewUtils.translatedDateFormatter
import views.html.employment.RemoveEmploymentView

import java.time.LocalDate

class RemoveEmploymentViewSpec extends ViewUnitTest {

  private val employmentId: String = "employmentId"
  private val employerName: String = "maggie"

  private val appUrl = "/update-and-submit-income-tax-return/employment-income"
  private val employmentSummaryUrl = s"$appUrl/$taxYearEOY/employment-summary"


  object Selectors {
    val paragraphTextSelector = "#main-content > div > div > form > p"
    val insetTextSelector = "#main-content > div > div > form > div.govuk-inset-text"
    val removeEmployerButtonSelector = "#remove-employer-button-id"
    val cancelLinkSelector = "#cancel-link-id"
    val formSelector = "#main-content > div > div > form"
    val infoWeHoldSelector = "#main-content > div > div > form > ul"
    val infoMessageSelector = "#remove-info-id"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String

    def expectedHeading(): String
    def employerInfo(employerName: String, startDate: String): String

    val expectedCaption: String
    val expectedRemoveAccountText: String
    val expectedLastAccountText: String
    val expectedRemoveEmployerButton: String
    val expectedCancelLink: String
    val infoWeHold: String
    val infoEmploymentStarted: String
    val infoMessage: String
    val infoMessageAgent: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val employerName = "apple"

    def expectedHeading(): String = "Remove this period of employment?"

    def employerInfo(employerName: String, startDate: String): String = {
      if(startDate.isEmpty) { s"$employerName Start date missing" }
      else { s"$employerName Started $startDate"}
    }

    val expectedCaption = s"PAYE employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedRemoveAccountText: String = "If you remove this period of employment, you’ll also remove" +
      " You must remove any expenses from the separate expenses section."
    val expectedLastAccountText = "This will also remove any benefits and expenses for this employer."
    val expectedRemoveEmployerButton = "Remove employment period"
    val infoEmploymentStarted = s"$employerName Started"
    val expectedCancelLink = "Cancel"
    val infoWeHold: String = "employment benefits student loans any changes made to the information we hold"
    val infoMessage: String = "This is information we already hold about you. " +
      "If the information is incorrect, contact the employer."
    val infoMessageAgent: String = "This is information we already hold about your client." +
      " If the information is incorrect, contact the employer."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "A ydych yn siŵr eich bod am dynnu’r gyflogaeth hon?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val employerName = "apple"

    def expectedHeading(): String = "Dileu’r cyfnod hwn o gyflogaeth?"

    def employerInfo(employerName: String, startDate: String): String = {
      if(startDate.isEmpty) { s"$employerName Dyddiad dechrau ar goll" }
      else { s"$employerName Wedi dechrau ar $startDate"}
    }

    val expectedCaption = s"Cyflogaeth TWE ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedRemoveAccountText: String = "Os byddwch yn dileu’r cyfnod hwn o gyflogaeth, byddwch hefyd yn dileu" +
      " Mae’n rhaid i chi dynnu unrhyw dreuliau o’r adran treuliau ar wahân."
    val expectedLastAccountText = "Bydd hyn hefyd yn dileu unrhyw fuddiannau a threuliau ar gyfer y cyflogwr hwn."
    val expectedRemoveEmployerButton = "Dileu’r cyfnod o gyflogaeth"
    val infoEmploymentStarted = s"$employerName Wedi dechrau ar"
    val expectedCancelLink = "Canslo"
    val infoWeHold: String = "buddiannau cyflogaeth benthyciadau myfyrwyr unrhyw " +
      "newidiadau a wnaed i’r wybodaeth sydd gennym"
    val infoMessage: String = "Dyma’r wybodaeth sydd eisoes gennym amdanoch." +
      " Os yw’r wybodaeth yn anghywir, cysylltwch â’r cyflogwr."
    val infoMessageAgent: String = "Dyma’r wybodaeth sydd eisoes gennym am eich cleient." +
      " Os yw’r wybodaeth yn anghywir, cysylltwch â’r cyflogwr."
  }

  private val underTest = inject[RemoveEmploymentView]

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  val userAgentScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  ".show" should {
    import Selectors._
    userScenarios.foreach { userScenario =>
      val common = userScenario.commonExpectedResults
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the remove employment page without start date" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val htmlFormat = underTest(taxYearEOY, employmentId, employerName, isHmrcEmployment = false, startDate = "")

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          welshToggleCheck(userScenario.isWelsh)

          titleCheck(common.expectedTitle, userScenario.isWelsh)
          h1Check(common.expectedHeading())
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveAccountText, paragraphTextSelector)
          buttonCheck(common.expectedRemoveEmployerButton, removeEmployerButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl)
          formPostLinkCheck(RemoveEmploymentController.submit(taxYearEOY, employmentId).url, formSelector)
          textOnPageCheck(common.infoWeHold, infoWeHoldSelector)
          elementNotOnPageCheck(infoMessageSelector)
        }

        "render the remove employment page for removing a hmrc employment without start date" which {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val startDate = ""
          val employerName = "apple"
          val htmlFormat = underTest(taxYearEOY, employmentId = "002", employerName, isHmrcEmployment = false, startDate)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)
          welshToggleCheck(userScenario.isWelsh)

          titleCheck(common.expectedTitle, userScenario.isWelsh)
          h1Check(common.expectedHeading())
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveAccountText, paragraphTextSelector)
          textOnPageCheck(common.employerInfo(employerName, startDate), insetTextSelector)
          buttonCheck(common.expectedRemoveEmployerButton, removeEmployerButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl)
          formPostLinkCheck(RemoveEmploymentController.submit(taxYearEOY, "002").url, formSelector)
        }

        "render the remove employment page" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val htmlFormat = underTest(taxYearEOY, employmentId, employerName, isHmrcEmployment = true, startDate = "")

          implicit val document: Document = Jsoup.parse(htmlFormat.body)
          welshToggleCheck(userScenario.isWelsh)

          titleCheck(common.expectedTitle, userScenario.isWelsh)
          h1Check(common.expectedHeading())
          captionCheck(common.expectedCaption)
          buttonCheck(common.expectedRemoveEmployerButton, removeEmployerButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl)
          formPostLinkCheck(RemoveEmploymentController.submit(taxYearEOY, employmentId).url, formSelector)
        }

        "render the remove employment page for when its hrmrc employment with employment start date " which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val startDate = "2020-01-01"
          val startDateFormatted = translatedDateFormatter(LocalDate.parse(startDate))
          val employerName = "apple"
          val htmlFormat = underTest(taxYearEOY, employmentId, employerName, isHmrcEmployment = true, startDate)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)
          welshToggleCheck(userScenario.isWelsh)
          textOnPageCheck(common.employerInfo(employerName, startDateFormatted), insetTextSelector)
          textOnPageCheck(common.infoMessage, infoMessageSelector)
        }
      }
    }

    userAgentScenarios.foreach { userScenario =>
      val common = userScenario.commonExpectedResults
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the remove employment page for when (User is Agent) its hrmrc employment with employment start date " which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val startDate = "2020-01-01"
          val startDateFormatted = translatedDateFormatter(LocalDate.parse(startDate))
          val employerName = "apple"
          val htmlFormat = underTest(taxYearEOY, employmentId, employerName, isHmrcEmployment = true, startDate)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)
          welshToggleCheck(userScenario.isWelsh)
          textOnPageCheck(common.employerInfo(employerName, startDateFormatted), insetTextSelector)
          textOnPageCheck(common.infoMessageAgent, infoMessageSelector)
        }
      }
    }
  }
}
