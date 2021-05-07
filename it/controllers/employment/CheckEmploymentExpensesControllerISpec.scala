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

package controllers.employment

import common.SessionValues
import helpers.PlaySessionCookieBaker
import models.{ExpensesType, GetEmploymentExpensesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}


class CheckEmploymentExpensesControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val taxYear = 2022
  val url =
    s"http://localhost:$port/income-through-software/return/employment-income/$taxYear/check-your-employment-expenses"

  val headingSelector = "#main-content > div > div > header > h1"
  val subHeadingSelector = "#main-content > div > div > header > p"
  val contentSelector = "#main-content > div > div > p"
  val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
  val summaryListSelector = "#main-content > div > div > dl"

  object ContentEN {
    val h1ExpectedAgent = "Check your client’s employment expenses"
    val titleExpectedAgent = "Check your client’s employment expenses"
    val h1ExpectedIndividual = "Check your employment expenses"
    val titleExpectedIndividual = "Check your employment expenses"
    val captionExpected = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val contentExpectedAgent = "Your client’s employment expenses are based on information we already hold about them. " +
      "This is a total of expenses from all employment in the tax year."
    val contentExpectedIndividual = "Your employment expenses are based on the information we already hold about you. " +
      "This is a total of expenses from all employment in the tax year."
    val insetTextExpectedAgent = s"You cannot update your client’s employment expenses until 6 April $taxYear."
    val insetTextExpectedIndividual = s"You cannot update your employment expenses until 6 April $taxYear."

    val fieldNames = List("Amount for business travel and subsistence expenses",
      "Job expenses",
      "Uniform, work cloths and tools (Flat rate expenses)",
      "Professional fees and subscriptions",
      "Hotel and meal expenses",
      "Other expenses and capital allowances",
      "Vehicle expense",
      "Mileage allowance relief")
  }

  object ContentCY {
    val h1ExpectedAgent = "Check your client’s employment expenses"
    val titleExpectedAgent = "Check your client’s employment expenses"
    val h1ExpectedIndividual = "Check your employment expenses"
    val titleExpectedIndividual = "Check your employment expenses"
    val captionExpected = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val contentExpectedAgent = "Your client’s employment expenses are based on information we already hold about them. " +
      "This is a total of expenses from all employment in the tax year."
    val contentExpectedIndividual = "Your employment expenses are based on the information we already hold about you. " +
      "This is a total of expenses from all employment in the tax year."
    val insetTextExpectedAgent = s"You cannot update your client’s employment expenses until 6 April $taxYear."
    val insetTextExpectedIndividual = s"You cannot update your employment expenses until 6 April $taxYear."

    val fieldNames = List("Amount for business travel and subsistence expenses Welsh",
      "Job expenses",
      "Uniform, work cloths and tools (Flat rate expenses)",
      "Professional fees and subscriptions",
      "Hotel and meal expenses",
      "Other expenses and capital allowances",
      "Vehicle expense",
      "Mileage allowance relief")
  }


  val expensesType = Some(ExpensesType(Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)))
  val expensesModel = GetEmploymentExpensesModel(None, None, None, None, expensesType)

  private def summaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dt"

  private def summaryListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dd"

  "as an individual in English" when {

    ".show" should {

      "returns an action when data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EXPENSES_CYA -> Json.prettyPrint(Json.toJson(expensesModel))
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpectedIndividual)
        h1Check(ContentEN.h1ExpectedIndividual)
        captionCheck(ContentEN.captionExpected)

        textOnPageCheck(ContentEN.contentExpectedIndividual, contentSelector)
        textOnPageCheck(ContentEN.insetTextExpectedIndividual, insetTextSelector)

        textOnPageCheck(ContentEN.fieldNames(0), summaryListRowFieldNameSelector(1))
        textOnPageCheck("£1", summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentEN.fieldNames(1), summaryListRowFieldNameSelector(2))
        textOnPageCheck("£2", summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentEN.fieldNames(2), summaryListRowFieldNameSelector(3))
        textOnPageCheck("£3", summaryListRowFieldAmountSelector(3))


        textOnPageCheck(ContentEN.fieldNames(3), summaryListRowFieldNameSelector(4))
        textOnPageCheck("£4", summaryListRowFieldAmountSelector(4))

        textOnPageCheck(ContentEN.fieldNames(4), summaryListRowFieldNameSelector(5))
        textOnPageCheck("£5", summaryListRowFieldAmountSelector(5))

        textOnPageCheck(ContentEN.fieldNames(5), summaryListRowFieldNameSelector(6))
        textOnPageCheck("£6", summaryListRowFieldAmountSelector(6))

        textOnPageCheck(ContentEN.fieldNames(6), summaryListRowFieldNameSelector(7))
        textOnPageCheck("£7", summaryListRowFieldAmountSelector(7))

        textOnPageCheck(ContentEN.fieldNames(7), summaryListRowFieldNameSelector(8))
        textOnPageCheck("£8", summaryListRowFieldAmountSelector(8))

        welshToggleCheck(ENGLISH)
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        await(wsClient.url(url).get())
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  "as an agent in English" when {

    ".show" should {

      "returns an action when data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EXPENSES_CYA -> Json.prettyPrint(Json.toJson(expensesModel))
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpectedAgent)
        h1Check(ContentEN.h1ExpectedAgent)
        captionCheck(ContentEN.captionExpected)

        textOnPageCheck(ContentEN.contentExpectedAgent, contentSelector)
        textOnPageCheck(ContentEN.insetTextExpectedAgent, insetTextSelector)

        textOnPageCheck(ContentEN.fieldNames(0), summaryListRowFieldNameSelector(1))
        textOnPageCheck("£1", summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentEN.fieldNames(1), summaryListRowFieldNameSelector(2))
        textOnPageCheck("£2", summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentEN.fieldNames(2), summaryListRowFieldNameSelector(3))
        textOnPageCheck("£3", summaryListRowFieldAmountSelector(3))


        textOnPageCheck(ContentEN.fieldNames(3), summaryListRowFieldNameSelector(4))
        textOnPageCheck("£4", summaryListRowFieldAmountSelector(4))

        textOnPageCheck(ContentEN.fieldNames(4), summaryListRowFieldNameSelector(5))
        textOnPageCheck("£5", summaryListRowFieldAmountSelector(5))

        textOnPageCheck(ContentEN.fieldNames(5), summaryListRowFieldNameSelector(6))
        textOnPageCheck("£6", summaryListRowFieldAmountSelector(6))

        textOnPageCheck(ContentEN.fieldNames(6), summaryListRowFieldNameSelector(7))
        textOnPageCheck("£7", summaryListRowFieldAmountSelector(7))

        textOnPageCheck(ContentEN.fieldNames(7), summaryListRowFieldNameSelector(8))
        textOnPageCheck("£8", summaryListRowFieldAmountSelector(8))

        welshToggleCheck(ENGLISH)
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseAgentUnauthorized()
        await(wsClient.url(url).get())
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  "as an individual in Welsh" when {

    ".show" should {

      "returns an action when data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EXPENSES_CYA -> Json.prettyPrint(Json.toJson(expensesModel))
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpectedIndividual)
        h1Check(ContentCY.h1ExpectedIndividual)
        captionCheck(ContentCY.captionExpected)

        textOnPageCheck(ContentCY.contentExpectedIndividual, contentSelector)
        textOnPageCheck(ContentCY.insetTextExpectedIndividual, insetTextSelector)

        textOnPageCheck(ContentCY.fieldNames(0), summaryListRowFieldNameSelector(1))
        textOnPageCheck("£1", summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentCY.fieldNames(1), summaryListRowFieldNameSelector(2))
        textOnPageCheck("£2", summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentCY.fieldNames(2), summaryListRowFieldNameSelector(3))
        textOnPageCheck("£3", summaryListRowFieldAmountSelector(3))


        textOnPageCheck(ContentCY.fieldNames(3), summaryListRowFieldNameSelector(4))
        textOnPageCheck("£4", summaryListRowFieldAmountSelector(4))

        textOnPageCheck(ContentCY.fieldNames(4), summaryListRowFieldNameSelector(5))
        textOnPageCheck("£5", summaryListRowFieldAmountSelector(5))

        textOnPageCheck(ContentCY.fieldNames(5), summaryListRowFieldNameSelector(6))
        textOnPageCheck("£6", summaryListRowFieldAmountSelector(6))

        textOnPageCheck(ContentCY.fieldNames(6), summaryListRowFieldNameSelector(7))
        textOnPageCheck("£7", summaryListRowFieldAmountSelector(7))

        textOnPageCheck(ContentCY.fieldNames(7), summaryListRowFieldNameSelector(8))
        textOnPageCheck("£8", summaryListRowFieldAmountSelector(8))

        welshToggleCheck(WELSH)
      }
    }
  }

  "as an agent in Welsh" when {

    ".show" should {

      "returns an action when data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EXPENSES_CYA -> Json.prettyPrint(Json.toJson(expensesModel))
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpectedAgent)
        h1Check(ContentCY.h1ExpectedAgent)
        captionCheck(ContentCY.captionExpected)

        textOnPageCheck(ContentCY.contentExpectedAgent, contentSelector)
        textOnPageCheck(ContentCY.insetTextExpectedAgent, insetTextSelector)

        textOnPageCheck(ContentCY.fieldNames(0), summaryListRowFieldNameSelector(1))
        textOnPageCheck("£1", summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentCY.fieldNames(1), summaryListRowFieldNameSelector(2))
        textOnPageCheck("£2", summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentCY.fieldNames(2), summaryListRowFieldNameSelector(3))
        textOnPageCheck("£3", summaryListRowFieldAmountSelector(3))


        textOnPageCheck(ContentCY.fieldNames(3), summaryListRowFieldNameSelector(4))
        textOnPageCheck("£4", summaryListRowFieldAmountSelector(4))

        textOnPageCheck(ContentCY.fieldNames(4), summaryListRowFieldNameSelector(5))
        textOnPageCheck("£5", summaryListRowFieldAmountSelector(5))

        textOnPageCheck(ContentCY.fieldNames(5), summaryListRowFieldNameSelector(6))
        textOnPageCheck("£6", summaryListRowFieldAmountSelector(6))

        textOnPageCheck(ContentCY.fieldNames(6), summaryListRowFieldNameSelector(7))
        textOnPageCheck("£7", summaryListRowFieldAmountSelector(7))

        textOnPageCheck(ContentCY.fieldNames(7), summaryListRowFieldNameSelector(8))
        textOnPageCheck("£8", summaryListRowFieldAmountSelector(8))

        welshToggleCheck(WELSH)
      }
    }
    }

}

