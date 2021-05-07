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
import models.{EmployerModel, EmploymentModel, GetEmploymentDataModel, PayModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}

class EmploymentDetailsControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val taxYear = 2022
  val url =
    s"http://localhost:$port/income-through-software/return/employment-income/$taxYear/check-your-employment-details"

  val headingSelector = "#main-content > div > div > header > h1"
  val captionSelector = "#main-content > div > div > header > p"
  val contentTextSelector = "#main-content > div > div > p"
  val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
  val summaryListSelector = "#main-content > div > div > dl"

  private def summaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dt"

  private def summaryListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dd"

  object ContentEN {
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
    
  }

  object ContentCY {
    val h1ExpectedAgent = "Check your client’s employment details"
    val titleExpectedAgent = "Check your client’s employment details"
    val h1ExpectedIndividual = "Check your employment details"
    val titleExpectedIndividual = "Check your employment details"
    val captionExpected = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val contentExpectedAgent = "Your client’s employment details are based on the information we already hold about them."
    val contentExpectedIndividual = "Your employment details are based on the information we already hold about you."
    val insetTextExpectedAgent = s"You cannot update your client’s employment details until 6 April $taxYear."
    val insetTextExpectedIndividual = s"You cannot update your employment details until 6 April $taxYear."

    val employeeFieldName1 = "Welsh Employer"
    val employeeFieldName2 = "PAYE reference"
    val employeeFieldName3 = "Director role end date"
    val employeeFieldName4 = "Close company"
    val employeeFieldName5 = "Pay received"
    val employeeFieldName6 = "UK tax taken from pay"
    val employeeFieldName7Individual = "Payments not on your P60"
    val employeeFieldName7Agent = "Payments not on P60"
    
  }
  
  object ContentValues{
    val employeeFieldValue1 = "maggie"
    val employeeFieldValue2 = "223/AB12399"
    val employeeFieldValue3 = "12 February 2020"
    val employeeFieldValue4 = "No"
    val employeeFieldValue5 = "£34234.15"
    val employeeFieldValue6 = "£6782.92"
    val employeeFieldValue7 = "£67676"
  }

  object FullModel {
    val payModel: PayModel = PayModel(34234.15, 6782.92, Some(67676), "CALENDAR MONTHLY", "2020-04-23", Some(32), Some(2))
    val employerModel: EmployerModel = EmployerModel(Some("223/AB12399"), "maggie")
    val employmentModel: EmploymentModel = EmploymentModel(Some("1002"), Some("123456789999"), Some(true), Some(false), Some("2020-02-12"),
      Some("2019-04-21"), Some("2020-03-11"), Some(false), Some(false), employerModel, payModel)
    val getEmploymentDataModel: GetEmploymentDataModel = GetEmploymentDataModel("2020-01-04T05:01:01Z", Some("CUSTOMER"),
      Some("2020-04-04T01:01:01Z"), Some("2020-04-04T01:01:01Z"), employmentModel)
  }

  object MinModel {
    val payModel: PayModel = PayModel(34234.15, 6782.92, None, "CALENDAR MONTHLY", "2020-04-23", None, None)
    val employerModel: EmployerModel = EmployerModel(None, "maggie")
    val employmentModel: EmploymentModel = EmploymentModel(None, None, None, None, None, None, None, None, None, employerModel, payModel)
    val getEmploymentDataModel: GetEmploymentDataModel = GetEmploymentDataModel("2020-01-04T05:01:01Z", None, None, None, employmentModel)
  }

  object SomeModelWithInvalidDate  {
    val payModel: PayModel = PayModel(34234.15, 6782.92, None, "CALENDAR MONTHLY", "2020-04-23", None, None)
    val employerModel: EmployerModel = EmployerModel(None, "maggie")
    val employmentModel: EmploymentModel = EmploymentModel(None, None, Some(true), None, Some("14/07/1990"), None, None, None, None, employerModel, payModel)
    val getEmploymentDataModel: GetEmploymentDataModel = GetEmploymentDataModel("2020-01-04T05:01:01Z", None, None, None, employmentModel)
  }


  "as an individual in english" when{

    ".show" should {

        "return a fully populated page when all the fields are populated" which {

          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(Json.toJson(FullModel.getEmploymentDataModel))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
          }


          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ContentEN.titleExpectedIndividual)
          h1Check(ContentEN.h1ExpectedIndividual)
          textOnPageCheck(ContentEN.captionExpected, captionSelector)

          textOnPageCheck(ContentEN.contentExpectedIndividual, contentTextSelector)
          textOnPageCheck(ContentEN.insetTextExpectedIndividual, insetTextSelector)

          textOnPageCheck(ContentEN.employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

          textOnPageCheck(ContentEN.employeeFieldName2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(ContentValues.employeeFieldValue2, summaryListRowFieldAmountSelector(2))

          textOnPageCheck(ContentEN.employeeFieldName3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(ContentValues.employeeFieldValue3, summaryListRowFieldAmountSelector(3))

          textOnPageCheck(ContentEN.employeeFieldName4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(ContentValues.employeeFieldValue4, summaryListRowFieldAmountSelector(4))

          textOnPageCheck(ContentEN.employeeFieldName5, summaryListRowFieldNameSelector(5))
          textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(5))

          textOnPageCheck(ContentEN.employeeFieldName6, summaryListRowFieldNameSelector(6))
          textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(6))

          textOnPageCheck(ContentEN.employeeFieldName7Individual, summaryListRowFieldNameSelector(7))
          textOnPageCheck(ContentValues.employeeFieldValue7, summaryListRowFieldAmountSelector(7))

        welshToggleCheck(ENGLISH)


      }

      "return a filtered list on page when minimum data is in session" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(
            Json.toJson(MinModel.getEmploymentDataModel)
          )
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpectedIndividual)
        h1Check(ContentEN.h1ExpectedIndividual)
        textOnPageCheck(ContentEN.captionExpected, captionSelector)

        textOnPageCheck(ContentEN.contentExpectedIndividual, contentTextSelector)
        textOnPageCheck(ContentEN.insetTextExpectedIndividual, insetTextSelector)


        textOnPageCheck(ContentEN.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentEN.employeeFieldName5, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentEN.employeeFieldName6, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))

        welshToggleCheck(ENGLISH)

        }


      "return an action when some model with invalid date is in session" when {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(
            Json.toJson(SomeModelWithInvalidDate.getEmploymentDataModel)
          )
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpectedIndividual)
        h1Check(ContentEN.h1ExpectedIndividual)
        textOnPageCheck(ContentEN.captionExpected, captionSelector)

        textOnPageCheck(ContentEN.contentExpectedIndividual, contentTextSelector)
        textOnPageCheck(ContentEN.insetTextExpectedIndividual, insetTextSelector)


        textOnPageCheck(ContentEN.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentEN.employeeFieldName5, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentEN.employeeFieldName6, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))

        welshToggleCheck(ENGLISH)

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
  }

  "as an agent in english" when{

    ".show" should {

      "return a fully populated page when all the fields are populated" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(Json.toJson(FullModel.getEmploymentDataModel))
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpectedAgent)
        h1Check(ContentEN.h1ExpectedAgent)
        textOnPageCheck(ContentEN.captionExpected, captionSelector)

        textOnPageCheck(ContentEN.contentExpectedAgent, contentTextSelector)
        textOnPageCheck(ContentEN.insetTextExpectedAgent, insetTextSelector)

        textOnPageCheck(ContentEN.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentEN.employeeFieldName2, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue2, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentEN.employeeFieldName3, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue3, summaryListRowFieldAmountSelector(3))

        textOnPageCheck(ContentEN.employeeFieldName4, summaryListRowFieldNameSelector(4))
        textOnPageCheck(ContentValues.employeeFieldValue4, summaryListRowFieldAmountSelector(4))

        textOnPageCheck(ContentEN.employeeFieldName5, summaryListRowFieldNameSelector(5))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(5))

        textOnPageCheck(ContentEN.employeeFieldName6, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(6))

        textOnPageCheck(ContentEN.employeeFieldName7Agent, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.employeeFieldValue7, summaryListRowFieldAmountSelector(7))

        welshToggleCheck(ENGLISH)


      }

      "return a filtered list on page when minimum data is in session" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(
            Json.toJson(MinModel.getEmploymentDataModel)
          )
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpectedAgent)
        h1Check(ContentEN.h1ExpectedAgent)
        textOnPageCheck(ContentEN.captionExpected, captionSelector)

        textOnPageCheck(ContentEN.contentExpectedAgent, contentTextSelector)
        textOnPageCheck(ContentEN.insetTextExpectedAgent, insetTextSelector)


        textOnPageCheck(ContentEN.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentEN.employeeFieldName5, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentEN.employeeFieldName6, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))

        welshToggleCheck(ENGLISH)

      }


      "return an action when some model with invalid date is in session" when {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(
            Json.toJson(SomeModelWithInvalidDate.getEmploymentDataModel)
          )
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpectedAgent)
        h1Check(ContentEN.h1ExpectedAgent)
        textOnPageCheck(ContentEN.captionExpected, captionSelector)

        textOnPageCheck(ContentEN.contentExpectedAgent, contentTextSelector)
        textOnPageCheck(ContentEN.insetTextExpectedAgent, insetTextSelector)


        textOnPageCheck(ContentEN.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentEN.employeeFieldName5, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentEN.employeeFieldName6, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))

        welshToggleCheck(ENGLISH)

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
  }

  "as an individual in welsh" when{

    ".show" should {

      "return a fully populated page when all the fields are populated" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(Json.toJson(FullModel.getEmploymentDataModel))
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpectedIndividual)
        h1Check(ContentCY.h1ExpectedIndividual)
        textOnPageCheck(ContentCY.captionExpected, captionSelector)

        textOnPageCheck(ContentCY.contentExpectedIndividual, contentTextSelector)
        textOnPageCheck(ContentCY.insetTextExpectedIndividual, insetTextSelector)

        textOnPageCheck(ContentCY.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentCY.employeeFieldName2, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue2, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentCY.employeeFieldName3, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue3, summaryListRowFieldAmountSelector(3))

        textOnPageCheck(ContentCY.employeeFieldName4, summaryListRowFieldNameSelector(4))
        textOnPageCheck(ContentValues.employeeFieldValue4, summaryListRowFieldAmountSelector(4))

        textOnPageCheck(ContentCY.employeeFieldName5, summaryListRowFieldNameSelector(5))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(5))

        textOnPageCheck(ContentCY.employeeFieldName6, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(6))

        textOnPageCheck(ContentCY.employeeFieldName7Individual, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.employeeFieldValue7, summaryListRowFieldAmountSelector(7))

      welshToggleCheck(WELSH)


      }

      "return a filtered list on page when minimum data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(Json.toJson(MinModel.getEmploymentDataModel))
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpectedIndividual)
        h1Check(ContentCY.h1ExpectedIndividual)
        textOnPageCheck(ContentCY.captionExpected, captionSelector)

        textOnPageCheck(ContentCY.contentExpectedIndividual, contentTextSelector)
        textOnPageCheck(ContentCY.insetTextExpectedIndividual, insetTextSelector)


        textOnPageCheck(ContentCY.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentCY.employeeFieldName5, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentCY.employeeFieldName6, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))

        welshToggleCheck(WELSH)

      }


      "return an action when some model with invalid date is in session" when {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(Json.toJson(SomeModelWithInvalidDate.getEmploymentDataModel))
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpectedIndividual)
        h1Check(ContentCY.h1ExpectedIndividual)
        textOnPageCheck(ContentCY.captionExpected, captionSelector)

        textOnPageCheck(ContentCY.contentExpectedIndividual, contentTextSelector)
        textOnPageCheck(ContentCY.insetTextExpectedIndividual, insetTextSelector)


        textOnPageCheck(ContentCY.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentCY.employeeFieldName5, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentCY.employeeFieldName6, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))

        welshToggleCheck(WELSH)

      }
    }
  }

  "as an agent in welsh" when{

    ".show" should {

      "return a fully populated page when all the fields are populated" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(Json.toJson(FullModel.getEmploymentDataModel))
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpectedAgent)
        h1Check(ContentCY.h1ExpectedAgent)
        textOnPageCheck(ContentCY.captionExpected, captionSelector)

        textOnPageCheck(ContentCY.contentExpectedAgent, contentTextSelector)
        textOnPageCheck(ContentCY.insetTextExpectedAgent, insetTextSelector)

        textOnPageCheck(ContentCY.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentCY.employeeFieldName2, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue2, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentCY.employeeFieldName3, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue3, summaryListRowFieldAmountSelector(3))

        textOnPageCheck(ContentCY.employeeFieldName4, summaryListRowFieldNameSelector(4))
        textOnPageCheck(ContentValues.employeeFieldValue4, summaryListRowFieldAmountSelector(4))

        textOnPageCheck(ContentCY.employeeFieldName5, summaryListRowFieldNameSelector(5))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(5))

        textOnPageCheck(ContentCY.employeeFieldName6, summaryListRowFieldNameSelector(6))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(6))

        textOnPageCheck(ContentCY.employeeFieldName7Agent, summaryListRowFieldNameSelector(7))
        textOnPageCheck(ContentValues.employeeFieldValue7, summaryListRowFieldAmountSelector(7))

        welshToggleCheck(WELSH)


      }

      "return a filtered list on page when minimum data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(Json.toJson(MinModel.getEmploymentDataModel))
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpectedAgent)
        h1Check(ContentCY.h1ExpectedAgent)
        textOnPageCheck(ContentCY.captionExpected, captionSelector)

        textOnPageCheck(ContentCY.contentExpectedAgent, contentTextSelector)
        textOnPageCheck(ContentCY.insetTextExpectedAgent, insetTextSelector)


        textOnPageCheck(ContentCY.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentCY.employeeFieldName5, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentCY.employeeFieldName6, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))

        welshToggleCheck(WELSH)

      }


      "return an action when some model with invalid date is in session" when {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_DATA -> Json.prettyPrint(Json.toJson(SomeModelWithInvalidDate.getEmploymentDataModel))
        ))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpectedAgent)
        h1Check(ContentCY.h1ExpectedAgent)
        textOnPageCheck(ContentCY.captionExpected, captionSelector)

        textOnPageCheck(ContentCY.contentExpectedAgent, contentTextSelector)
        textOnPageCheck(ContentCY.insetTextExpectedAgent, insetTextSelector)


        textOnPageCheck(ContentCY.employeeFieldName1, summaryListRowFieldNameSelector(1))
        textOnPageCheck(ContentValues.employeeFieldValue1, summaryListRowFieldAmountSelector(1))

        textOnPageCheck(ContentCY.employeeFieldName5, summaryListRowFieldNameSelector(2))
        textOnPageCheck(ContentValues.employeeFieldValue5, summaryListRowFieldAmountSelector(2))

        textOnPageCheck(ContentCY.employeeFieldName6, summaryListRowFieldNameSelector(3))
        textOnPageCheck(ContentValues.employeeFieldValue6, summaryListRowFieldAmountSelector(3))

        welshToggleCheck(WELSH)

      }
    }
  }


}
