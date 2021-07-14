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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.WireMockHelper
import org.jsoup.nodes.Document
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.HeaderNames
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

trait ViewHelpers { self: AnyWordSpec with Matchers with WireMockHelper =>

  val serviceName = "Update and submit an Income Tax Return"
  val govUkExtension = "GOV.UK"

  val ENGLISH = "English"
  val WELSH = "Welsh"

  def welshTest(isWelsh: Boolean): String = if (isWelsh) "Welsh" else "English"
  def agentTest(isAgent: Boolean): String = if (isAgent) "Agent" else "Individual"

  def authoriseAgentOrIndividual(isAgent: Boolean, nino: Boolean = true): StubMapping = if (isAgent) authoriseAgent() else authoriseIndividual(nino)
  def unauthorisedAgentOrIndividual(isAgent: Boolean): StubMapping = if (isAgent) authoriseAgentUnauthorized() else authoriseIndividualUnauthorized()

  case class UserScenario[CommonExpectedResults,SpecificExpectedResults](isWelsh: Boolean,
                                                                         isAgent: Boolean,
                                                                         commonExpectedResults: CommonExpectedResults,
                                                                         specificExpectedResults: Option[SpecificExpectedResults] = None)

  val userScenarios: Seq[UserScenario[_, _]]

  def urlGet(url: String, welsh: Boolean = false, follow: Boolean = true, headers: Seq[(String, String)] = Seq())(implicit wsClient: WSClient): WSResponse = {

    val newHeaders = if(welsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") ++ headers else headers
    await(wsClient.url(url).withFollowRedirects(follow).withHttpHeaders(newHeaders: _*).get())
  }

  def urlPost[T](url: String,
                 body: T,
                 welsh: Boolean = false,
                 follow: Boolean = true,
                 headers: Seq[(String, String)] = Seq()
                )(implicit wsClient: WSClient, bodyWritable: BodyWritable[T]): WSResponse = {

    val headersWithNoCheck = headers ++ Seq("Csrf-Token" -> "nocheck")
    val newHeaders = if(welsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") ++ headersWithNoCheck else headersWithNoCheck
    await(wsClient.url(url).withFollowRedirects(follow).withHttpHeaders(newHeaders: _*).post(body))
  }

  def elementText(selector: String)(implicit document: () => Document): String = {
    document().select(selector).text()
  }

  def elementExist(selector: String)(implicit document: () => Document): Boolean = {
    !document().select(selector).isEmpty
  }

  def titleCheck(title: String)(implicit document: () => Document): Unit = {
    s"has a title of $title" in {
      document().title() shouldBe s"$title - $serviceName - $govUkExtension"
    }
  }

  def hintTextCheck(text: String, selector: String = ".govuk-hint")(implicit document: () => Document): Unit = {
    s"has the hint text of '$text'" in {
      elementText(selector) shouldBe text
    }
  }

  def h1Check(header: String, size: String = "l")(implicit document: () => Document): Unit = {
    s"have a page heading of '$header'" in {
      document().select(s".govuk-heading-$size").text() shouldBe header
    }
  }

  def captionCheck(caption: String, selector: String = ".govuk-caption-l")(implicit document: () => Document): Unit = {
    s"have the caption of '$caption'" in {
      document().select(selector).text() shouldBe caption
    }
  }

  def textOnPageCheck(text: String, selector: String, additionalTestText: String = "")(implicit document: () => Document): Unit = {
    s"have text on the screen of '$text' $additionalTestText" in {
      document().select(selector).text() shouldBe text
    }
  }

  def formRadioValueCheck(selected: Boolean, selector: String)(implicit document: () => Document): Unit = {
    s"have a radio button form with the value set to '$selected'" in {
      document().select(selector).attr("value") shouldBe selected.toString
    }
  }

  def formGetLinkCheck(text: String, selector: String)(implicit document: () => Document): Unit = {
    s"have a form with a GET action of '$text'" in {
      document().select(selector).attr("action") shouldBe text
      document().select(selector).attr("method") shouldBe "GET"
    }
  }

  def formPostLinkCheck(text: String, selector: String)(implicit document: () => Document): Unit = {
    s"have a form with a POST action of '$text'" in {
      document().select(selector).attr("action") shouldBe text
      document().select(selector).attr("method") shouldBe "POST"
    }
  }

  def buttonCheck(text: String, selector: String = ".govuk-button", href: Option[String] = None)(implicit document: () => Document): Unit = {
    s"have a $text button" which {
      s"has the text '$text'" in {
        document().select(selector).text() shouldBe text
      }
      s"has a class of govuk-button" in {
        document().select(selector).attr("class") should include("govuk-button")
      }

      if(href.isDefined) {
        s"has a href to '${href.get}'" in {
          document().select(selector).attr("href") shouldBe href.get
        }
      }
    }
  }

  def radioButtonCheck(text: String, radioNumber: Int)(implicit document: () => Document): Unit = {
    s"have a $text radio button" which {
      s"is of type radio button" in {
        val selector = ".govuk-radios__item > input"
        document().select(selector).get(radioNumber - 1).attr("type") shouldBe "radio"
      }
      s"has the text $text" in {
        val selector = ".govuk-radios__item > label"
        document().select(selector).get(radioNumber - 1).text() shouldBe text
      }
    }
  }

  def linkCheck(text: String, selector: String, href: String)(implicit document: () => Document): Unit = {
    s"have a $text link" which {
      s"has the text '$text' and a href to '$href'" in {
        document().select(selector).text() shouldBe text
        document().select(selector).attr("href") shouldBe href
      }
    }
  }

  def inputFieldCheck(name: String, selector: String)(implicit document: () => Document): Unit = {
    s"has a name of '$name'" in {
      document().select(selector).attr("name") shouldBe name
    }
  }

  def errorSummaryCheck(text: String, href: String)(implicit document: () => Document): Unit = {
    "contains an error summary" in {
      elementExist(".govuk-error-summary")
    }
    "contains the text 'There is a problem'" in {
      document().select(".govuk-error-summary__title").text() shouldBe "There is a problem"
    }
    s"has a $text error in the error summary" which {
      s"has the text '$text'" in {
        document().select(".govuk-error-summary__body").text() shouldBe text
      }
      s"has a href to '$href'" in {
        document().select(".govuk-error-summary__body > ul > li > a").attr("href") shouldBe href
      }
    }
  }

  def errorAboveElementCheck(text: String)(implicit document: () => Document): Unit = {
    s"has a $text error above the element" which {
      s"has the text '$text'" in {
        document().select(".govuk-error-message").text() shouldBe s"Error: $text"
      }
    }
  }

  def welshToggleCheck(isWelsh: Boolean)(implicit document: () => Document): Unit ={
    welshToggleCheck(if(isWelsh) WELSH else ENGLISH)
  }

  def welshToggleCheck(activeLanguage: String)(implicit document: () => Document): Unit = {
    val otherLanguage = if (activeLanguage == "English") "Welsh" else "English"

    def selector = Map("English" -> 0, "Welsh" -> 1)

    def linkLanguage = Map("English" -> "English", "Welsh" -> "Cymraeg")

    def linkText = Map("English" -> "Change the language to English English",
      "Welsh" -> "Newid yr iaith ir Gymraeg Cymraeg")

    s"have the language toggle already set to $activeLanguage" which {
      s"has the text '$activeLanguage" in {
        document().select(".hmrc-language-select__list-item").get(selector(activeLanguage)).text() shouldBe linkLanguage(activeLanguage)
      }
    }
    s"has a link to change the language to $otherLanguage" which {
      s"has the text '${linkText(otherLanguage)}" in {
        document().select(".hmrc-language-select__list-item").get(selector(otherLanguage)).text() shouldBe linkText(otherLanguage)
      }
      s"has a link to change the language" in {
        document().select(".hmrc-language-select__list-item > a").attr("href") shouldBe
          s"/income-through-software/return/employment-income/language/${linkLanguage(otherLanguage).toLowerCase}"
      }
    }
  }

}
