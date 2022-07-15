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
import controllers.employment.routes._
import utils.ViewUtils.dateFormatter
import views.html.employment.RemoveEmploymentView

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
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String

    def expectedHeading(employerName: String): String

    val expectedCaption: String
    val expectedRemoveAccountText: String
    val expectedLastAccountText: String
    val expectedRemoveEmployerButton: String
    val expectedCancelLink: String
    val infoWeHold: String
    val infoEmploymentStarted: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val employerName = "apple"

    def expectedHeading(employerName: String): String = s"Are you sure you want to remove $employerName?"

    val expectedCaption = s"PAYE employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedRemoveAccountText: String = "This is information we already hold about you.If the information is incorrect,speak to the employer." +
      " If you remove this period of employment, you’ll also remove" +
      " You must remove any expenses from the separate expenses section."
    val expectedLastAccountText = "This will also remove any benefits and expenses for this employer."
    val expectedRemoveEmployerButton = "Remove employer"
    val infoWeHold = s"$employerName Start date missing"
    val infoEmploymentStarted = s"$employerName Employment started"
    val expectedCancelLink = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "A ydych yn si?r eich bod am dynnuír gyflogaeth hon?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val employerName = "apple"

    def expectedHeading(employerName: String): String = s"A ydych yn si?r eich bod am dynnu $employerName?"

    val expectedCaption = s"Cyflogaeth TWE ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val expectedRemoveAccountText: String = "Dyma’r wybodaeth sydd eisoes gennym amdanoch. Os yw’r wybodaeth yn anghywir, siaradwch â’r cyflogwr." +
      " Os byddwch yn dileu’r cyfnod hwn o gyflogaeth, byddwch hefyd yn dileu" +
      " Mae’n rhaid i chi dynnu unrhyw dreuliau o’r adran treuliau ar wahân."
    val expectedLastAccountText = "Bydd hyn hefyd yn dileu unrhyw fuddiannau a threuliau ar gyfer y cyflogwr hwn."
    val expectedRemoveEmployerButton = "Dileu’r cyflogwr"
    val infoWeHold = s"$employerName Dyddiad dechrau ar goll"
    val infoEmploymentStarted = s"$employerName Dechrau’r gyflogaeth"
    val expectedCancelLink = "Canslo"
  }

  private val underTest = inject[RemoveEmploymentView]

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  ".show" should {
    import Selectors._
    userScenarios.foreach { userScenario =>
      val common = userScenario.commonExpectedResults
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the remove employment page for when it isn't the last employment without start date" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val htmlFormat = underTest(taxYearEOY, employmentId, employerName, lastEmployment = false, isHmrcEmployment = false, startDate = "")

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          welshToggleCheck(userScenario.isWelsh)

          titleCheck(common.expectedTitle, userScenario.isWelsh)
          h1Check(common.expectedHeading(employerName))
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveAccountText, paragraphTextSelector)
          buttonCheck(common.expectedRemoveEmployerButton, removeEmployerButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl)
          formPostLinkCheck(RemoveEmploymentController.submit(taxYearEOY, employmentId).url, formSelector)
        }

        "render the remove employment page for when it isn't the last employment and removing a hmrc employment without start date" which {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val htmlFormat = underTest(taxYearEOY, employmentId = "002", employerName = "apple", lastEmployment = false, isHmrcEmployment = false, startDate = "")

          implicit val document: Document = Jsoup.parse(htmlFormat.body)
          welshToggleCheck(userScenario.isWelsh)

          titleCheck(common.expectedTitle, userScenario.isWelsh)
          h1Check(common.expectedHeading("apple"))
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveAccountText, paragraphTextSelector)
          textOnPageCheck(common.infoWeHold, insetTextSelector)
          buttonCheck(common.expectedRemoveEmployerButton, removeEmployerButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl)
          formPostLinkCheck(RemoveEmploymentController.submit(taxYearEOY, "002").url, formSelector)
        }

        "render the remove employment page for when it's the last employment" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val htmlFormat = underTest(taxYearEOY, employmentId, employerName, lastEmployment = true, isHmrcEmployment = true, startDate = "")

          implicit val document: Document = Jsoup.parse(htmlFormat.body)
          welshToggleCheck(userScenario.isWelsh)

          titleCheck(common.expectedTitle, userScenario.isWelsh)
          h1Check(common.expectedHeading(employerName))
          captionCheck(common.expectedCaption)
          buttonCheck(common.expectedRemoveEmployerButton, removeEmployerButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl)
          formPostLinkCheck(RemoveEmploymentController.submit(taxYearEOY, employmentId).url, formSelector)
        }

        "render the remove employment page for when it isn't the last employment with employment start date" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val startDate = "2020-01-01"
          val startDateFormatted = dateFormatter(startDate).get
          val employerName = "apple"
          val infoEmploymentStartedMessage = s"${common.infoEmploymentStarted} $startDateFormatted"
          val htmlFormat = underTest(taxYearEOY, employmentId, employerName , lastEmployment = false, isHmrcEmployment = false, startDate )

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          welshToggleCheck(userScenario.isWelsh)
          textOnPageCheck(infoEmploymentStartedMessage, insetTextSelector)
        }
      }
    }
  }
}
