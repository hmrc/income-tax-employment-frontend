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

import models.employment.{AllEmploymentData, EmploymentData, EmploymentSource, Pay}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}

class EmploymentDetailsControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val url = s"${appUrl(port)}/$taxYear/check-employment-details?employmentId=001"

  object Selectors {
    val headingSelector = "#main-content > div > div > header > h1"
    val captionSelector = "#main-content > div > div > header > p"
    val contentTextSelector = "#main-content > div > div > p"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val summaryListSelector = "#main-content > div > div > dl"
    def summaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dt"
    def summaryListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > dl > div:nth-child($i) > dd"
  }

  object ExpectedResults {

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

      val employeeFieldName1 = "Employer"
      val employeeFieldName2 = "PAYE reference"
      val employeeFieldName3 = "Director role end date"
      val employeeFieldName4 = "Close company"
      val employeeFieldName5 = "Pay received"
      val employeeFieldName6 = "UK tax taken from pay"
      val employeeFieldName7Individual = "Payments not on your P60"
      val employeeFieldName7Agent = "Payments not on P60"
    }

    object ContentValues {
      val employeeFieldValue1 = "maggie"
      val employeeFieldValue2 = "223/AB12399"
      val employeeFieldValue3 = "12 February 2020"
      val employeeFieldValue4 = "No"
      val employeeFieldValue5 = "£34234.15"
      val employeeFieldValue6 = "£6782.92"
      val employeeFieldValue7 = "£67676"
    }
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

  "in english" when {

    import ExpectedResults.ContentEN._
    import Selectors._
    import ExpectedResults.ContentValues._

    "the user is an individual" when {

      ".show" should {

        "return a fully populated page when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(FullModel.allData), nino, taxYear)
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedIndividual)
          h1Check(h1ExpectedIndividual)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedIndividual, contentTextSelector)
          textOnPageCheck(insetTextExpectedIndividual, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue2, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue3, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(employeeFieldName4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(employeeFieldValue4, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(5))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(6))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(employeeFieldName7Individual, summaryListRowFieldNameSelector(7))
          textOnPageCheck(employeeFieldValue7, summaryListRowFieldAmountSelector(7))
          welshToggleCheck(ENGLISH)
        }

        "redirect to overview page when theres no details" in {

          lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(
              FullModel.allData.copy(hmrcEmploymentData = Seq.empty)
            ), nino, taxYear)
            await(wsClient.url(url).withHttpHeaders(
              HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck"
            ).withFollowRedirects(false).get())
          }

          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }

        "return a filtered list on page when minimum data is in mongo" which {

          implicit lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(MinModel.miniData), nino, taxYear)
            await(wsClient.url(url)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedIndividual)
          h1Check(h1ExpectedIndividual)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedIndividual, contentTextSelector)
          textOnPageCheck(insetTextExpectedIndividual, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(3))
          welshToggleCheck(ENGLISH)
        }


        "return an action when some model with invalid date is in mongo" when {

          implicit lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(SomeModelWithInvalidData.invalidData), nino, taxYear)
            await(wsClient.url(url)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedIndividual)
          h1Check(h1ExpectedIndividual)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedIndividual, contentTextSelector)
          textOnPageCheck(insetTextExpectedIndividual, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(3))
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

    "the user is an agent" when {

      ".show" should {

        "return a fully populated page when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(FullModel.allData), nino, taxYear)
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedAgent)
          h1Check(h1ExpectedAgent)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedAgent, contentTextSelector)
          textOnPageCheck(insetTextExpectedAgent, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue2, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue3, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(employeeFieldName4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(employeeFieldValue4, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(5))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(6))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(employeeFieldName7Agent, summaryListRowFieldNameSelector(7))
          textOnPageCheck(employeeFieldValue7, summaryListRowFieldAmountSelector(7))
          welshToggleCheck(ENGLISH)
        }

        "return a filtered list on page when minimum data is in mongo" which {

          implicit lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(MinModel.miniData), nino, taxYear)
            await(wsClient.url(url)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedAgent)
          h1Check(h1ExpectedAgent)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedAgent, contentTextSelector)
          textOnPageCheck(insetTextExpectedAgent, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(3))
          welshToggleCheck(ENGLISH)
        }

        "return an action when some model with invalid date is in mongo" when {

          implicit lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(SomeModelWithInvalidData.invalidData), nino, taxYear)
            await(wsClient.url(url)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedAgent)
          h1Check(h1ExpectedAgent)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedAgent, contentTextSelector)
          textOnPageCheck(insetTextExpectedAgent, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(3))
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
  }

  "in welsh" when {

    import ExpectedResults.ContentCY._
    import Selectors._
    import ExpectedResults.ContentValues._

    "the user is an individual" when {

      ".show" should {

        "return a fully populated page when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(FullModel.allData), nino, taxYear)
            await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
              HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedIndividual)
          h1Check(h1ExpectedIndividual)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedIndividual, contentTextSelector)
          textOnPageCheck(insetTextExpectedIndividual, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue2, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue3, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(employeeFieldName4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(employeeFieldValue4, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(5))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(6))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(employeeFieldName7Individual, summaryListRowFieldNameSelector(7))
          textOnPageCheck(employeeFieldValue7, summaryListRowFieldAmountSelector(7))
          welshToggleCheck(WELSH)
        }

        "return a filtered list on page when minimum data is in mongo" which {

          implicit lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(MinModel.miniData), nino, taxYear)

            await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
              HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedIndividual)
          h1Check(h1ExpectedIndividual)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedIndividual, contentTextSelector)
          textOnPageCheck(insetTextExpectedIndividual, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(3))
          welshToggleCheck(WELSH)
        }

        "return an action when some model with invalid date is in mongo" when {

         implicit lazy val result: WSResponse = {
            authoriseIndividual()
            userDataStub(userData(SomeModelWithInvalidData.invalidData), nino, taxYear)

            await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
              HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedIndividual)
          h1Check(h1ExpectedIndividual)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedIndividual, contentTextSelector)
          textOnPageCheck(insetTextExpectedIndividual, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(3))
          welshToggleCheck(WELSH)

        }
      }
    }

    "as an agent in welsh" when {

      ".show" should {

        "return a fully populated page when all the fields are populated" which {

          implicit lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(FullModel.allData), nino, taxYear)
            await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
              HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedAgent)
          h1Check(h1ExpectedAgent)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedAgent, contentTextSelector)
          textOnPageCheck(insetTextExpectedAgent, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName2, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue2, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName3, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue3, summaryListRowFieldAmountSelector(3))
          textOnPageCheck(employeeFieldName4, summaryListRowFieldNameSelector(4))
          textOnPageCheck(employeeFieldValue4, summaryListRowFieldAmountSelector(4))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(5))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(5))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(6))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(6))
          textOnPageCheck(employeeFieldName7Agent, summaryListRowFieldNameSelector(7))
          textOnPageCheck(employeeFieldValue7, summaryListRowFieldAmountSelector(7))
          welshToggleCheck(WELSH)
        }

        "return a filtered list on page when minimum data is in mongo" which {

          implicit lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(MinModel.miniData), nino, taxYear)
            await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
              HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedAgent)
          h1Check(h1ExpectedAgent)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedAgent, contentTextSelector)
          textOnPageCheck(insetTextExpectedAgent, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(3))
          welshToggleCheck(WELSH)
        }

        "return an action when some model with invalid date is in session" when {

          implicit lazy val result: WSResponse = {
            authoriseAgent()
            userDataStub(userData(SomeModelWithInvalidData.invalidData), nino, taxYear)
            await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
              HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck").get())
          }

          titleCheck(titleExpectedAgent)
          h1Check(h1ExpectedAgent)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(contentExpectedAgent, contentTextSelector)
          textOnPageCheck(insetTextExpectedAgent, insetTextSelector)
          textOnPageCheck(employeeFieldName1, summaryListRowFieldNameSelector(1))
          textOnPageCheck(employeeFieldValue1, summaryListRowFieldAmountSelector(1))
          textOnPageCheck(employeeFieldName5, summaryListRowFieldNameSelector(2))
          textOnPageCheck(employeeFieldValue5, summaryListRowFieldAmountSelector(2))
          textOnPageCheck(employeeFieldName6, summaryListRowFieldNameSelector(3))
          textOnPageCheck(employeeFieldValue6, summaryListRowFieldAmountSelector(3))
          welshToggleCheck(WELSH)
        }
      }
    }
  }
}