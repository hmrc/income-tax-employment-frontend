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

package controllers

import common.SessionValues
import helpers.{PlaySessionCookieBaker, ViewTestHelper}
import models.{ExpensesType, GetEmploymentExpensesModel}
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest
import play.api.http.Status._
import play.api.libs.json.Json

class CheckEmploymentExpensesControllerISpec extends IntegrationTest with ViewTestHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private val taxYear = 2022
  private val checkExpensesUrl = startUrl + s"/$taxYear/check-your-employment-expenses"

  val headingSelector = "#main-content > div > div > header > h1"
  val subHeadingSelector = "#main-content > div > div > header > p"
  val contentSelector = "#main-content > div > div > p"
  val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
  val summaryListSelector = "#main-content > div > div > dl"

  private def summaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dt"
  private def summaryListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dd"

//  "as an individual" when {
//
//    ".show" should {
//
//      "returns an action without data in session" which {
//        val expectedRedirectBody = "something"
//        lazy val result: WSResponse = {
//          stubGet(s"/income-through-software/return/$taxYear/view", OK, expectedRedirectBody)
//          authoriseIndividual()
//          await(wsClient.url(checkExpensesUrl).get())
//        }
//
//        "has an OK(200) status" in {
//          result.status shouldBe OK
//          result.body shouldBe expectedRedirectBody
//        }
//
//      }
//      "returns an action when data is in session" which {
//        val expensesType = Some(ExpensesType(Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)))
//        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
//          SessionValues.EXPENSES_CYA -> Json.prettyPrint(
//            Json.toJson(GetEmploymentExpensesModel(None, None, None, None, expensesType))
//          )
//        ))
//
//        lazy val result: WSResponse = {
//          authoriseAgent()
//          await(wsClient.url(checkExpensesUrl)
//            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
//        }
//
//        "has an OK(200) status" in {
//          result.status shouldBe OK
//        }
//
//        "has the correct content" in {
//          lazy implicit val document = Jsoup.parse(result.body)
//
//          assertTitle(s"Check employment expenses - $serviceName - $govUkExtension")
//          element(headingSelector).text() shouldBe "Check employment expenses"
//          element(subHeadingSelector).text() shouldBe s"Employment for 6 April ${taxYear-1} to 5 April $taxYear"
//
//          element(contentSelector).text() shouldBe "Your employment expenses are based on the information we already hold about you. This is a total of expenses from all employment in the tax year."
//
//          element(insetTextSelector).text() shouldBe s"You cannot update your employment details until 6 April $taxYear."
//
//          elements(summaryListRowFieldNameSelector(1)).text shouldBe "Amount for business travel and subsistence expenses"
//          elements(summaryListRowFieldAmountSelector(1)).text shouldBe "£1"
//
//          elements(summaryListRowFieldNameSelector(2)).text shouldBe "Job expenses"
//          elements(summaryListRowFieldAmountSelector(2)).text shouldBe "£2"
//
//          elements(summaryListRowFieldNameSelector(3)).text shouldBe "Uniform, work cloths and tools (Flat rate expenses)"
//          elements(summaryListRowFieldAmountSelector(3)).text shouldBe "£3"
//
//          elements(summaryListRowFieldNameSelector(4)).text shouldBe "Professional fees and subscriptions"
//          elements(summaryListRowFieldAmountSelector(4)).text shouldBe "£4"
//
//          elements(summaryListRowFieldNameSelector(5)).text shouldBe "Hotel and meal expenses"
//          elements(summaryListRowFieldAmountSelector(5)).text shouldBe "£5"
//
//          elements(summaryListRowFieldNameSelector(6)).text shouldBe "Other expenses and capital allowances"
//          elements(summaryListRowFieldAmountSelector(6)).text shouldBe "£6"
//
//          elements(summaryListRowFieldNameSelector(7)).text shouldBe "Vehicle expense"
//          elements(summaryListRowFieldAmountSelector(7)).text shouldBe "£7"
//
//          elements(summaryListRowFieldNameSelector(8)).text shouldBe "Mileage allowance relief"
//          elements(summaryListRowFieldAmountSelector(8)).text shouldBe "£8"
//        }
//
//      }
//      "returns an action when auth call fails" which {
//        lazy val result: WSResponse = {
//          authoriseIndividualUnauthorized()
//          await(wsClient.url(checkExpensesUrl).get())
//        }
//        "has an UNAUTHORIZED(401) status" in {
//          result.status shouldBe UNAUTHORIZED
//        }
//      }
//
//    }
//  }

  "as an agent" when {

    ".show" should {

      "returns an action without data in session" which {
        val expectedRedirectBody = "something"
        lazy val result: WSResponse = {
          stubGet(s"/income-through-software/return/$taxYear/view", OK, expectedRedirectBody)
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(checkExpensesUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
          result.body shouldBe expectedRedirectBody
        }

      }
      "returns an action when data is in session" which {
        val expensesType = Some(ExpensesType(Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)))
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.EXPENSES_CYA -> Json.prettyPrint(
            Json.toJson(GetEmploymentExpensesModel(None, None, None, None, expensesType))
          ),
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(checkExpensesUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the correct content" in {
          lazy implicit val document = Jsoup.parse(result.body)

          assertTitle(s"Check employment expenses - $serviceName - $govUkExtension")
          element(headingSelector).text() shouldBe "Check employment expenses"
          element(subHeadingSelector).text() shouldBe s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"

          element(contentSelector).text() shouldBe "Your client`s employment expenses are based on information we already hold about them. This is a total of expenses from all employment in the tax year."

          element(insetTextSelector).text() shouldBe s"You cannot update your client`s employment expenses until 6 April $taxYear."

          elements(summaryListRowFieldNameSelector(1)).text shouldBe "Amount for business travel and subsistence expenses"
          elements(summaryListRowFieldAmountSelector(1)).text shouldBe "£1"

          elements(summaryListRowFieldNameSelector(2)).text shouldBe "Job expenses"
          elements(summaryListRowFieldAmountSelector(2)).text shouldBe "£2"

          elements(summaryListRowFieldNameSelector(3)).text shouldBe "Uniform, work cloths and tools (Flat rate expenses)"
          elements(summaryListRowFieldAmountSelector(3)).text shouldBe "£3"

          elements(summaryListRowFieldNameSelector(4)).text shouldBe "Professional fees and subscriptions"
          elements(summaryListRowFieldAmountSelector(4)).text shouldBe "£4"

          elements(summaryListRowFieldNameSelector(5)).text shouldBe "Hotel and meal expenses"
          elements(summaryListRowFieldAmountSelector(5)).text shouldBe "£5"

          elements(summaryListRowFieldNameSelector(6)).text shouldBe "Other expenses and capital allowances"
          elements(summaryListRowFieldAmountSelector(6)).text shouldBe "£6"

          elements(summaryListRowFieldNameSelector(7)).text shouldBe "Vehicle expense"
          elements(summaryListRowFieldAmountSelector(7)).text shouldBe "£7"

          elements(summaryListRowFieldNameSelector(8)).text shouldBe "Mileage allowance relief"
          elements(summaryListRowFieldAmountSelector(8)).text shouldBe "£8"
        }

      }
      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(wsClient.url(checkExpensesUrl).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }
  }
}
