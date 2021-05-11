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
import models.employment.{AllEmploymentData, EmploymentData, EmploymentSource, Pay}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest

class EmploymentDetailsControllerISpec extends IntegrationTest with ViewTestHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private val taxYear = 2022

  private val checkEmploymentDetailsUrl = s"$startUrl/2022/check-your-employment-details?employmentId=001"

  val headingSelector = "#main-content > div > div > header > h1"
  val subHeadingSelector = "#main-content > div > div > header > p"
  val contentTextSelector = "#main-content > div > div > p"
  val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
  val summaryListSelector = "#main-content > div > div > dl"

  private def summaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dt"

  private def summaryListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dd"

  object Content {
    val h1ExpectedAgent = "Check your client’s employment details"
    val titleExpectedAgent = "Check your client’s employment details"
    val h1ExpectedIndividual = "Check your employment details"
    val titleExpectedIndividual = "Check your employment details"
    val captionExpected = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val contentExpectedAgent = "Your client’s employment details are based on the information we already hold about them."
    val contentExpectedIndividual = "Your employment details are based on the information we already hold about you."
    val insetTextExpectedAgent = s"You cannot update your client’s employment details until 6 April $taxYear."
    val insetTextExpectedIndividual = s"You cannot update your employment details until 6 April $taxYear."

    val employeeFieldName1 = "Employer"
    val employeeFieldName2 = "PAYE reference"
    val employeeFieldName3 = "Director role end date"
    val employeeFieldName4 = "Close company"
    val employeeFieldName5 = "Pay received"
    val employeeFieldName6 = "UK tax taken from pay"
    val employeeFieldName7Individual = "Payments not on your P60"
    val employeeFieldName7Agent = "Payments not on P60"

    val employeeFieldValue1 = "maggie"
    val employeeFieldValue2 = "223/AB12399"
    val employeeFieldValue3 = "12 February 2020"
    val employeeFieldValue4 = "No"
    val employeeFieldValue5 = "£34234.15"
    val employeeFieldValue6 = "£6782.92"
    val employeeFieldValue7 = "£67676"
  }

  object FullModel {
    val allData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(
        EmploymentSource(
          employmentId = "001",
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
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  object MinModel {
    val miniData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(
        EmploymentSource(
          employmentId = "001",
          employerName = "maggie",
          employerRef = None,
          payrollId = None,
          startDate = None,
          cessationDate = None,
          dateIgnored = None,
          submittedOn = None,
          employmentData = Some(EmploymentData(
            submittedOn = "2020-02-12",
            employmentSequenceNumber = None,
            companyDirector = None,
            closeCompany = None,
            directorshipCeasedDate = None,
            occPen = None,
            disguisedRemuneration = None,
            pay = Pay(34234.15, 6782.92, None, "CALENDAR MONTHLY", "2020-04-23", None, None)
          )),
          None
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  object SomeModelWithInvalidData {
    val invalidData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(
        EmploymentSource(
          employmentId = "001",
          employerName = "maggie",
          employerRef = None,
          payrollId = None,
          startDate = None,
          cessationDate = None,
          dateIgnored = None,
          submittedOn = None,
          employmentData = Some(EmploymentData(
            submittedOn = "2020-02-12",
            employmentSequenceNumber = None,
            companyDirector = Some(true),
            closeCompany = None,
            directorshipCeasedDate = Some("14/07/1990"),
            occPen = None,
            disguisedRemuneration = None,
            pay = Pay(34234.15, 6782.92, None, "CALENDAR MONTHLY", "2020-04-23", None, None)
          )),
          None
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }


  "as an individual" when{

    ".show" should {

      "returns an action without data in session" which {
        val expectedRedirectBody = "something"
        lazy val result: WSResponse = {
          stubGet(s"/income-through-software/return/$taxYear/view", OK, expectedRedirectBody)
          authoriseIndividual()
          await(wsClient.url(checkEmploymentDetailsUrl).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
          result.body shouldBe expectedRedirectBody
        }

      }

      "return an action when full data is in session" when {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
            Json.toJson(FullModel.allData)
          )
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(checkEmploymentDetailsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "return an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the correct full content" in {
          lazy implicit val document:Document = Jsoup.parse(result.body)

          assertTitle(s"Check your employment details - $serviceName - $govUkExtension")
          element(headingSelector).text() shouldBe Content.h1ExpectedIndividual
          element(subHeadingSelector).text() shouldBe Content.captionExpected

          element(contentTextSelector).text() shouldBe Content.contentExpectedIndividual
          element(insetTextSelector).text() shouldBe Content.insetTextExpectedIndividual

          elements(summaryListRowFieldNameSelector(1)).text shouldBe Content.employeeFieldName1
          elements(summaryListRowFieldAmountSelector(1)).text shouldBe Content.employeeFieldValue1

          elements(summaryListRowFieldNameSelector(2)).text shouldBe Content.employeeFieldName2
          elements(summaryListRowFieldAmountSelector(2)).text shouldBe Content.employeeFieldValue2

          elements(summaryListRowFieldNameSelector(3)).text shouldBe Content.employeeFieldName3
          elements(summaryListRowFieldAmountSelector(3)).text shouldBe Content.employeeFieldValue3

          elements(summaryListRowFieldNameSelector(4)).text shouldBe Content.employeeFieldName4
          elements(summaryListRowFieldAmountSelector(4)).text shouldBe Content.employeeFieldValue4

          elements(summaryListRowFieldNameSelector(5)).text shouldBe Content.employeeFieldName5
          elements(summaryListRowFieldAmountSelector(5)).text shouldBe Content.employeeFieldValue5

          elements(summaryListRowFieldNameSelector(6)).text shouldBe Content.employeeFieldName6
          elements(summaryListRowFieldAmountSelector(6)).text shouldBe Content.employeeFieldValue6

          elements(summaryListRowFieldNameSelector(7)).text shouldBe Content.employeeFieldName7Individual
          elements(summaryListRowFieldAmountSelector(7)).text shouldBe Content.employeeFieldValue7

        }
      }

      "return an action when minimum data is in session" when {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
            Json.toJson(MinModel.miniData)
          )
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(checkEmploymentDetailsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "return an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the correct minimum content" in {
          lazy implicit val document:Document = Jsoup.parse(result.body)

          assertTitle(s"Check your employment details - $serviceName - $govUkExtension")
          element(headingSelector).text() shouldBe Content.h1ExpectedIndividual
          element(subHeadingSelector).text() shouldBe Content.captionExpected

          element(contentTextSelector).text() shouldBe Content.contentExpectedIndividual
          element(insetTextSelector).text() shouldBe Content.insetTextExpectedIndividual

          elements(summaryListRowFieldNameSelector(1)).text shouldBe Content.employeeFieldName1
          elements(summaryListRowFieldAmountSelector(1)).text shouldBe Content.employeeFieldValue1

          elements(summaryListRowFieldNameSelector(2)).text shouldBe Content.employeeFieldName5
          elements(summaryListRowFieldAmountSelector(2)).text shouldBe Content.employeeFieldValue5

          elements(summaryListRowFieldNameSelector(3)).text shouldBe Content.employeeFieldName6
          elements(summaryListRowFieldAmountSelector(3)).text shouldBe Content.employeeFieldValue6

        }
      }

      "return an action when some model with invalid date is in session" when {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
            Json.toJson(SomeModelWithInvalidData.invalidData)
          )
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(checkEmploymentDetailsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "return an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the correct content" in {
          lazy implicit val document:Document = Jsoup.parse(result.body)

          assertTitle(s"Check your employment details - $serviceName - $govUkExtension")
          element(headingSelector).text() shouldBe Content.h1ExpectedIndividual
          element(subHeadingSelector).text() shouldBe Content.captionExpected

          element(contentTextSelector).text() shouldBe Content.contentExpectedIndividual
          element(insetTextSelector).text() shouldBe Content.insetTextExpectedIndividual

          elements(summaryListRowFieldNameSelector(1)).text shouldBe Content.employeeFieldName1
          elements(summaryListRowFieldAmountSelector(1)).text shouldBe Content.employeeFieldValue1

          elements(summaryListRowFieldNameSelector(2)).text shouldBe Content.employeeFieldName5
          elements(summaryListRowFieldAmountSelector(2)).text shouldBe Content.employeeFieldValue5

          elements(summaryListRowFieldNameSelector(3)).text shouldBe Content.employeeFieldName6
          elements(summaryListRowFieldAmountSelector(3)).text shouldBe Content.employeeFieldValue6

        }
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(checkEmploymentDetailsUrl).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }
  }

  "as an agent" when{

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
          await(wsClient.url(checkEmploymentDetailsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
          result.body shouldBe expectedRedirectBody
        }

      }

      "return an action when full data is in session" when {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
            Json.toJson(FullModel.allData)
          ),
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result: WSResponse = {

          authoriseAgent()
          await(wsClient.url(checkEmploymentDetailsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "return an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the correct full content" in {
          lazy implicit val document:Document = Jsoup.parse(result.body)

          assertTitle(s"Check your client’s employment details - $serviceName - $govUkExtension")
          element(headingSelector).text() shouldBe Content.h1ExpectedAgent
          element(subHeadingSelector).text() shouldBe Content.captionExpected

          element(contentTextSelector).text() shouldBe Content.contentExpectedAgent
          element(insetTextSelector).text() shouldBe Content.insetTextExpectedAgent

          elements(summaryListRowFieldNameSelector(1)).text shouldBe Content.employeeFieldName1
          elements(summaryListRowFieldAmountSelector(1)).text shouldBe Content.employeeFieldValue1

          elements(summaryListRowFieldNameSelector(2)).text shouldBe Content.employeeFieldName2
          elements(summaryListRowFieldAmountSelector(2)).text shouldBe Content.employeeFieldValue2

          elements(summaryListRowFieldNameSelector(3)).text shouldBe Content.employeeFieldName3
          elements(summaryListRowFieldAmountSelector(3)).text shouldBe Content.employeeFieldValue3

          elements(summaryListRowFieldNameSelector(4)).text shouldBe Content.employeeFieldName4
          elements(summaryListRowFieldAmountSelector(4)).text shouldBe Content.employeeFieldValue4

          elements(summaryListRowFieldNameSelector(5)).text shouldBe Content.employeeFieldName5
          elements(summaryListRowFieldAmountSelector(5)).text shouldBe Content.employeeFieldValue5

          elements(summaryListRowFieldNameSelector(6)).text shouldBe Content.employeeFieldName6
          elements(summaryListRowFieldAmountSelector(6)).text shouldBe Content.employeeFieldValue6

          elements(summaryListRowFieldNameSelector(7)).text shouldBe Content.employeeFieldName7Agent
          elements(summaryListRowFieldAmountSelector(7)).text shouldBe Content.employeeFieldValue7

        }
      }

      "return an action when minimum data is in session" when {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
            Json.toJson(MinModel.miniData)
          ),
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(checkEmploymentDetailsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "return an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the correct minimum content" in {
          lazy implicit val document:Document = Jsoup.parse(result.body)

          assertTitle(s"Check your client’s employment details - $serviceName - $govUkExtension")
          element(headingSelector).text() shouldBe Content.h1ExpectedAgent
          element(subHeadingSelector).text() shouldBe Content.captionExpected

          element(contentTextSelector).text() shouldBe Content.contentExpectedAgent
          element(insetTextSelector).text() shouldBe Content.insetTextExpectedAgent

          elements(summaryListRowFieldNameSelector(1)).text shouldBe Content.employeeFieldName1
          elements(summaryListRowFieldAmountSelector(1)).text shouldBe Content.employeeFieldValue1

          elements(summaryListRowFieldNameSelector(2)).text shouldBe Content.employeeFieldName5
          elements(summaryListRowFieldAmountSelector(2)).text shouldBe Content.employeeFieldValue5

          elements(summaryListRowFieldNameSelector(3)).text shouldBe Content.employeeFieldName6
          elements(summaryListRowFieldAmountSelector(3)).text shouldBe Content.employeeFieldValue6

        }
      }

      "return an action when some model with invalid date is in session" when {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
            Json.toJson(SomeModelWithInvalidData.invalidData)
          ),
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(checkEmploymentDetailsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "return an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the correct content" in {
          lazy implicit val document:Document = Jsoup.parse(result.body)

          assertTitle(s"Check your client’s employment details - $serviceName - $govUkExtension")
          element(headingSelector).text() shouldBe Content.h1ExpectedAgent
          element(subHeadingSelector).text() shouldBe Content.captionExpected

          element(contentTextSelector).text() shouldBe Content.contentExpectedAgent
          element(insetTextSelector).text() shouldBe Content.insetTextExpectedAgent

          elements(summaryListRowFieldNameSelector(1)).text shouldBe Content.employeeFieldName1
          elements(summaryListRowFieldAmountSelector(1)).text shouldBe Content.employeeFieldValue1

          elements(summaryListRowFieldNameSelector(2)).text shouldBe Content.employeeFieldName5
          elements(summaryListRowFieldAmountSelector(2)).text shouldBe Content.employeeFieldValue5

          elements(summaryListRowFieldNameSelector(3)).text shouldBe Content.employeeFieldName6
          elements(summaryListRowFieldAmountSelector(3)).text shouldBe Content.employeeFieldValue6

        }
      }
      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(wsClient.url(checkEmploymentDetailsUrl).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }
  }
}
