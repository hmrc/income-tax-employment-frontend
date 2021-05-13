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
import models.employment.{AllEmploymentData, Benefits, EmploymentBenefits, EmploymentData, EmploymentSource, Pay}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}


class EmploymentDetailsAndBenefitsControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private val taxYear = 2022

  private val url = s"$startUrl/2022/employer-details-and-benefits?employmentId=001"

  val headingSelector = "#main-content > div > div > header > h1"
  val subHeadingSelector = "#main-content > div > div > header > p"
  val p1Selector = "#main-content > div > div > p"
  val buttonSelector = "#employmentSummaryBtn"

  val employmentDetailsLinkSelector = "#employment-details_link"
  val employmentBenefitsLinkSelector = "#employment-benefits_link"

  private def taskListRowFieldNameSelector(i: Int) = s"#main-content > div > div > ul > li:nth-child($i) > span.app-task-list__task-name"

  private def taskListRowFieldAmountSelector(i: Int) = s"#main-content > div > div > ul > li:nth-child($i) > span.hmrc-status-tag"

  private val employmentDetailsUrl = "/income-through-software/return/employment-income/2022/check-your-employment-details?employmentId=001"
  private val employmentBenefitsUrl = "/income-through-software/return/employment-income/2022/check-your-employment-benefits?employmentId=001"

  object ContentEN {
    val h1Expected = "maggie"
    val titleExpected = "Employment details and benefits"
    val captionExpected = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val p1ExpectedAgent = "You cannot update your client’s employment information until 6 April 2022."
    val p1ExpectedIndividual = "You cannot update your employment information until 6 April 2022."

    val fieldNames = List("Employment details",
      "Benefits")

    val buttonText = "Return to employment summary"
  }

  object ContentCY {
    val h1Expected = "maggie"
    val titleExpected= "Employment details and benefits"
    val captionExpected = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val p1ExpectedAgent = "You cannot update your client’s employment information until 6 April 2022."
    val p1ExpectedIndividual = "You cannot update your employment information until 6 April 2022."

    val fieldNames = List("Employment details",
      "Benefits")

    val buttonText = "Return to employment summary"
  }

  val amount: BigDecimal = 100

  val BenefitsModel: Benefits = Benefits(
    Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount),
    Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount),
    Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount)
  )

  def fullModel(benefitsModel:Option[EmploymentBenefits]):AllEmploymentData = {
      AllEmploymentData(
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
          employmentBenefits = benefitsModel
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }


  "as an individual in English" when {

    ".show" should {

      "return an action with benefits status Cannot update when no benefits data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(Json.toJson(fullModel(None))
          )))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpected)
        h1Check(ContentEN.h1Expected)
        captionCheck(ContentEN.captionExpected)

        textOnPageCheck(ContentEN.p1ExpectedIndividual, p1Selector)

        "has an employment details section" which {
          linkCheck(ContentEN.fieldNames(0), employmentDetailsLinkSelector, employmentDetailsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
        }

        "has a benefits section" which {
          textOnPageCheck(ContentEN.fieldNames(1), taskListRowFieldNameSelector(2))
          textOnPageCheck("Cannot update", taskListRowFieldAmountSelector(2))
        }
        buttonCheck(ContentEN.buttonText, buttonSelector)

        welshToggleCheck(ENGLISH)
      }

      "return an action with benefits status Updated when benefits data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(Json.toJson(fullModel(Some(EmploymentBenefits("2020-04-04T01:01:01Z",Some(BenefitsModel)))
          )))))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpected)
        h1Check(ContentEN.h1Expected)
        captionCheck(ContentEN.captionExpected)

        textOnPageCheck(ContentEN.p1ExpectedIndividual, p1Selector)

        "has an employment details section" which {
          linkCheck(ContentEN.fieldNames(0), employmentDetailsLinkSelector, employmentDetailsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
        }

        "has a benefits section" which {
          linkCheck(ContentEN.fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(2))
        }

        buttonCheck(ContentEN.buttonText, buttonSelector)

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

      "return an action with benefits status Cannot update when no benefits data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(Json.toJson(fullModel(None)
        ))))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpected)
        h1Check(ContentEN.h1Expected)
        captionCheck(ContentEN.captionExpected)

        textOnPageCheck(ContentEN.p1ExpectedAgent, p1Selector)

        "has an employment details section" which {
          linkCheck(ContentEN.fieldNames(0), employmentDetailsLinkSelector, employmentDetailsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
        }

        "has a benefits section" which {
          textOnPageCheck(ContentEN.fieldNames(1), taskListRowFieldNameSelector(2))
          textOnPageCheck("Cannot update", taskListRowFieldAmountSelector(2))
        }

        buttonCheck(ContentEN.buttonText, buttonSelector)

        welshToggleCheck(ENGLISH)
      }

      "return an action with benefits status Updated when benefits data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(Json.toJson(fullModel(Some(EmploymentBenefits("2020-04-04T01:01:01Z",Some(BenefitsModel))
          ))))))

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentEN.titleExpected)
        h1Check(ContentEN.h1Expected)
        captionCheck(ContentEN.captionExpected)

        textOnPageCheck(ContentEN.p1ExpectedAgent, p1Selector)

        "has an employment details section" which {
          linkCheck(ContentEN.fieldNames(0), employmentDetailsLinkSelector, employmentDetailsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
        }

        "has a benefits section" which {
          linkCheck(ContentEN.fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(2))
        }

        buttonCheck(ContentEN.buttonText, buttonSelector)

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

      "return an action with benefits status Cannot update when no benefits data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(Json.toJson(fullModel(None)
          ))))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpected)
        h1Check(ContentCY.h1Expected)
        captionCheck(ContentCY.captionExpected)

        textOnPageCheck(ContentCY.p1ExpectedIndividual, p1Selector)

        "has an employment details section" which {
          linkCheck(ContentCY.fieldNames(0), employmentDetailsLinkSelector, employmentDetailsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
        }

        "has a benefits section" which {
          textOnPageCheck(ContentCY.fieldNames(1), taskListRowFieldNameSelector(2))
          textOnPageCheck("Cannot update", taskListRowFieldAmountSelector(2))
        }

        buttonCheck(ContentEN.buttonText, buttonSelector)

        welshToggleCheck(WELSH)
      }

      "return an action with benefits status Updated when benefits data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(Json.toJson(fullModel(Some(EmploymentBenefits("2020-04-04T01:01:01Z",Some(BenefitsModel))
          ))))))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpected)
        h1Check(ContentCY.h1Expected)
        captionCheck(ContentCY.captionExpected)

        textOnPageCheck(ContentCY.p1ExpectedIndividual, p1Selector)

        "has an employment details section" which {
          linkCheck(ContentCY.fieldNames(0), employmentDetailsLinkSelector, employmentDetailsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
        }

        "has a benefits section" which {
          linkCheck(ContentCY.fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(2))
        }

        buttonCheck(ContentEN.buttonText, buttonSelector)

        welshToggleCheck(WELSH)
      }
    }
  }

  "as an agent in Welsh" when {

    ".show" should {

      "return an action with benefits status Cannot update when no benefits data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(Json.toJson(fullModel(None)))))


        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpected)
        h1Check(ContentCY.h1Expected)
        captionCheck(ContentCY.captionExpected)

        textOnPageCheck(ContentCY.p1ExpectedAgent, p1Selector)

        "has an employment details section" which {
          linkCheck(ContentCY.fieldNames(0), employmentDetailsLinkSelector, employmentDetailsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
        }

        "has a benefits section" which {
          textOnPageCheck(ContentCY.fieldNames(1), taskListRowFieldNameSelector(2))
          textOnPageCheck("Cannot update", taskListRowFieldAmountSelector(2))
        }

        buttonCheck(ContentCY.buttonText, buttonSelector)

        welshToggleCheck(WELSH)
      }

      "return an action with benefits status Updated when benefits data is in session" which {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(Json.toJson(fullModel(Some(EmploymentBenefits("2020-04-04T01:01:01Z",Some(BenefitsModel))
          ))))))


        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy",
            HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
        }


        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ContentCY.titleExpected)
        h1Check(ContentCY.h1Expected)
        captionCheck(ContentCY.captionExpected)

        textOnPageCheck(ContentCY.p1ExpectedAgent, p1Selector)

        "has an employment details section" which {
          linkCheck(ContentCY.fieldNames(0), employmentDetailsLinkSelector, employmentDetailsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(1))
        }

        "has a benefits section" which {
          linkCheck(ContentCY.fieldNames(1), employmentBenefitsLinkSelector, employmentBenefitsUrl)
          textOnPageCheck("Updated", taskListRowFieldAmountSelector(2))
        }

        buttonCheck(ContentCY.buttonText, buttonSelector)

        welshToggleCheck(WELSH)
      }
    }
  }

}
