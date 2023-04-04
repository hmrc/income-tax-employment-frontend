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

package views.tailorings

import controllers.tailorings.routes.RemoveAllEmploymentController
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.tailorings.RemoveAllEmploymentView

class RemoveAllEmploymentViewSpec extends ViewUnitTest {

  private val appUrl = "/update-and-submit-income-tax-return/employment-income"
  private val employmentSummaryUrl = s"$appUrl/$taxYearEOY/employment-summary"


  object Selectors {
    val paragraphTextSelector = "#remove-info-id1"
    val bullet1Selector = "#main-content > div > div > form > ul > li:nth-child(1)"
    val bullet2Selector = "#main-content > div > div > form > ul > li:nth-child(2)"
    val paragraph2TextSelector = "#remove-info-id2"
    val remove_button = "#remove-employer-button-id"
    val cancelLinkSelector = "#cancel-link-id"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedTitle: String

    def expectedHeading(): String

    val expectedCaption: String
    val expectedP1: String
    val expectedP2: String
    val expectedBullet1: String
    val expectedBullet2: String
    val expectedCancelLink: String
    val expectedConfirmLink: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Are you sure you want to change PAYE employment details for the tax year?"
    def expectedHeading(): String = "Are you sure you want to change PAYE employment details for the tax year?"

    val expectedCaption = s"PAYE employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val expectedP1: String = "If you change your PAYE employment details, this will:"
    override val expectedP2: String = "You cannot delete information we already hold. We will not use these details to calculate your Income Tax Return for this tax year."
    override val expectedBullet1: String = "change details of your Income Tax Return including employment, benefits and student loans"
    override val expectedBullet2: String = "delete information you have entered about employers and expenses"
    override val expectedCancelLink: String = "Cancel"
    override val expectedConfirmLink: String = "Confirm"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "A ydych yn siŵr eich bod am newid manylion cyflogaeth TWE ar gyfer y flwyddyn dreth nesaf?"
    val expectedCaption = s"Cyflogaeth TWE ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"

    override def expectedHeading(): String = "A ydych yn siŵr eich bod am newid manylion cyflogaeth TWE ar gyfer y flwyddyn dreth nesaf?"

    override val expectedP1: String = "Os byddwch yn newid eich manylion cyflogaeth TWE, bydd hyn yn:"
    override val expectedP2: String = "Ni allwch ddileu’r wybodaeth sydd gennym eisoes. Ni fyddwn yn defnyddio’r manylion hyn i gyfrifo eich Ffurflen Dreth Incwm ar gyfer y flwyddyn dreth hon."
    override val expectedBullet1: String = "newid manylion eich Ffurflen Dreth Incwm gan gynnwys cyflogaeth, budd-daliadau a benthyciadau myfyriwr"
    override val expectedBullet2: String = "dileu gwybodaeth rydych wedi nodi ynghylch cyflogwyr a threuliau"
    override val expectedCancelLink: String = "Canslo"
    override val expectedConfirmLink: String = "Cadarnhau"
  }

  private val underTest = inject[RemoveAllEmploymentView]

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  ".show" should {
    import Selectors._
    userScenarios.foreach { userScenario =>
      val common = userScenario.commonExpectedResults
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the remove ALL employment page" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val htmlFormat = underTest(taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          welshToggleCheck(userScenario.isWelsh)

          titleCheck(common.expectedTitle, userScenario.isWelsh)
          h1Check(common.expectedHeading())
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedP1, paragraphTextSelector)
          textOnPageCheck(common.expectedBullet1, bullet1Selector)
          textOnPageCheck(common.expectedBullet2, bullet2Selector)
          textOnPageCheck(common.expectedP2, paragraph2TextSelector)
          buttonCheck(common.expectedConfirmLink, remove_button)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl)
          formPostLinkCheck(RemoveAllEmploymentController.submit(taxYearEOY).url, formSelector)
        }
      }
    }

  }
}
