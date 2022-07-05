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

package views.errors

import common.SessionValues
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import support.ViewUnitTest
import views.html.errors.TaxYearErrorTemplate

class TaxYearErrorTemplateSpec extends ViewUnitTest {

  private val selfAssessmentLink: String = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"

  object Selectors {
    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > div.govuk-body > p:nth-child(2)"
    val p3Selector = "#main-content > div > div > div.govuk-body > p:nth-child(3)"
    val linkSelector = "#govuk-self-assessment-link"
  }

  trait CommonExpectedResults {
    val h1Expected: String
    val p1Expected: String
    val p1ExpectedSingle: String
    val p2Expected: String
    val p3Expected: String
    val p3ExpectedLinkText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val h1Expected = "Page not found"
    val p1Expected = s"You can only enter information for the tax years ${validTaxYearList.min} to ${validTaxYearList.max}."
    val p1ExpectedSingle = "You can only enter information for a valid tax year."
    val p2Expected = "Check that you’ve entered the correct web address."
    val p3Expected: String = "If the web address is correct or you selected a link or button, you can use Self Assessment: " +
      "general enquiries (opens in new tab) to speak to someone about your income tax."
    val p3ExpectedLinkText = "Self Assessment: general enquiries (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val h1Expected = "Heb ddod o hyd i’r dudalen"
    val p1Expected = s"Dim ond gwybodaeth ar gyfer y blynyddoedd treth ${validTaxYearList.min} i ${validTaxYearList.max} y gallwch ei nodi."
    val p1ExpectedSingle = "Dim ond gwybodaeth ar gyfer blwyddyn dreth ddilys y gallwch ei nodi."
    val p2Expected = "Gwiriwch eich bod wedi nodi’r cyfeiriad gwe cywir."
    val p3Expected: String = "Os ywír cyfeiriad gwe yn gywir neu os ydych wedi dewis cysylltiad neu fotwm, gallwch ddefnyddio Hunanasesiad: " +
      "ymholiadau cyffredinol (yn agor tab newydd) i siarad ‚ rhywun am eich treth incwm."
    val p3ExpectedLinkText = "Hunanasesiad: ymholiadau cyffredinol (yn agor tab newydd)"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  private def retrieveTaxYearList(implicit request: Request[_]): Seq[Int] = {
    getFromSession(SessionValues.VALID_TAX_YEARS)(request).getOrElse("").split(',').toSeq.map(_.toInt)
  }

  private def getFromSession(key: String)(implicit request: Request[_]): Option[String] = {
    request.session.get(key)
  }

  private lazy val underTest = inject[TaxYearErrorTemplate]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the error page with the right content for multiple Tax Years" which {
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/error/wrong-tax-year").withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(",")
        )
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(retrieveTaxYearList.head, retrieveTaxYearList.last, retrieveTaxYearList.head == retrieveTaxYearList.last)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(h1Expected, userScenario.isWelsh)
        welshToggleCheck(userScenario.isWelsh)
        h1Check(h1Expected, "xl")
        textOnPageCheck(p1Expected, p1Selector)
        textOnPageCheck(p2Expected, p2Selector)
        textOnPageCheck(p3Expected, p3Selector)
        linkCheck(p3ExpectedLinkText, linkSelector, selfAssessmentLink)
      }

      "render the error page with the right content for a single TaxYear" which {
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/error/wrong-tax-year").withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.VALID_TAX_YEARS -> invalidTaxYear.toString
        )
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(retrieveTaxYearList.head, retrieveTaxYearList.last, retrieveTaxYearList.head == retrieveTaxYearList.last)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(h1Expected, userScenario.isWelsh)
        welshToggleCheck(userScenario.isWelsh)
        h1Check(h1Expected, "xl")
        textOnPageCheck(p1ExpectedSingle, p1Selector)
        textOnPageCheck(p2Expected, p2Selector)
        textOnPageCheck(p3Expected, p3Selector)
        linkCheck(p3ExpectedLinkText, linkSelector, selfAssessmentLink)
      }
    }
  }
}
