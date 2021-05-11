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
import helpers.{PlaySessionCookieBaker, ViewTestHelper}
import models.employment.{AllEmploymentData, EmploymentData, EmploymentExpenses, EmploymentSource, Expenses, Pay}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest

class CheckEmploymentExpensesControllerISpec extends IntegrationTest with ViewTestHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private val taxYear = 2022
  private val checkExpensesUrl = startUrl + s"/$taxYear/check-your-employment-expenses"

  val expenses: Expenses = Expenses(Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8))
  val employmentExpenses: EmploymentExpenses = EmploymentExpenses(
    submittedOn = None,
    totalExpenses = None,
    expenses = Some(expenses)
  )
  val allData: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      EmploymentSource(
        employmentId = "223/AB12399",
        employerName = "maggie",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = Some("2020-04-04T01:01:01Z"),
        submittedOn = Some("2020-01-04T05:01:01Z"),
        employmentData = Some(EmploymentData(
          submittedOn = ("2020-02-12"),
          employmentSequenceNumber = Some("123456789999"),
          companyDirector = Some(true),
          closeCompany = Some(false),
          directorshipCeasedDate = Some("2020-02-12"),
          occPen = Some(false),
          disguisedRemuneration = Some(false),
          pay = Pay(34234.15, 6782.92, Some(67676), "CALENDAR MONTHLY", "2020-04-23", Some(32), Some(2))
        )),
        None
      )
    ),
    hmrcExpenses = Some(employmentExpenses),
    customerEmploymentData = Seq(),
    customerExpenses = None
  )

  val headingSelector = "#main-content > div > div > header > h1"
  val subHeadingSelector = "#main-content > div > div > header > p"
  val contentSelector = "#main-content > div > div > p"
  val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
  val summaryListSelector = "#main-content > div > div > dl"

  private def summaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dt"
  private def summaryListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dd"

  "as an individual" when {

    ".show" should {

      "returns an action without data in session" which {
        val expectedRedirectBody = "something"
        lazy val result: WSResponse = {
          stubGet(s"/income-through-software/return/$taxYear/view", OK, expectedRedirectBody)
          authoriseIndividual()
          await(wsClient.url(checkExpensesUrl).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
          result.body shouldBe expectedRedirectBody
        }

      }
      "returns an action when data is in session" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
            Json.toJson(allData)
          )
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(checkExpensesUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the correct content" in {
          lazy implicit val document:Document = Jsoup.parse(result.body)

          assertTitle(s"Check your employment expenses - $serviceName - $govUkExtension")
          element(headingSelector).text() shouldBe "Check your employment expenses"
          element(subHeadingSelector).text() shouldBe s"Employment for 6 April ${taxYear-1} to 5 April $taxYear"

          element(contentSelector).text() shouldBe "Your employment expenses are based on the information we already hold about you. This is a total of expenses from all employment in the tax year."

          element(insetTextSelector).text() shouldBe s"You cannot update your employment expenses until 6 April $taxYear."

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
          authoriseIndividualUnauthorized()
          await(wsClient.url(checkExpensesUrl).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }
  }

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
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
            Json.toJson(allData)
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

          assertTitle(s"Check your client’s employment expenses - $serviceName - $govUkExtension")
          element(headingSelector).text() shouldBe "Check your client’s employment expenses"
          element(subHeadingSelector).text() shouldBe s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"

          element(contentSelector).text() shouldBe "Your client’s employment expenses are based on information we already hold about them. This is a total of expenses from all employment in the tax year."

          element(insetTextSelector).text() shouldBe s"You cannot update your client’s employment expenses until 6 April $taxYear."

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
