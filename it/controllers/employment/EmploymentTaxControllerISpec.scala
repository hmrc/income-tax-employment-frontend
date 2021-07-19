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

import models.User
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmploymentTaxControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val taxYear = 2021
  val url = s"$appUrl/$taxYear/uk-tax?employmentId=001"

  trait CommonExpectedResults {
    val hint: String
    val continue: String
    val amount: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedP1: String
    val expectedP2: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val hint: String = "For example, £600 or £193.54"
    val continue: String = "Continue"
    val amount: String = "amount"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val hint: String = "For example, £600 or £193.54"
    val continue: String = "Continue"
    val amount: String = "amount"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your earnings?"
    val expectedP1: String = "You can usually find this amount in the Pay and Income Tax details section of your P60."
    val expectedP2: String = s"If £6,782.92 was not taken in UK tax, tell us the correct amount."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your clients maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your client’s earnings?"
    val expectedP1: String = "You can usually find this amount in the Pay and Income Tax details section of your clients P60."
    val expectedP2: String = s"If £6,782.92 was not taken in UK tax, tell us the correct amount."

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your earnings?"
    val expectedP1: String = "You can usually find this amount in the Pay and Income Tax details section of your P60."
    val expectedP2: String = s"If £6,782.92 was not taken in UK tax, tell us the correct amount."

  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your clients maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your client’s earnings?"
    val expectedP1: String = "You can usually find this amount in the Pay and Income Tax details section of your clients P60."
    val expectedP2: String = s"If £6,782.92 was not taken in UK tax, tell us the correct amount."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  object Selectors {
    val p1 = "#main-content > div > div > form > div > label > p:nth-child(2)"
    val p2 = "#main-content > div > div > form > div > label > p:nth-child(3)"
    val hintText = "#amount-hint"
    val currencyBox = "#amount"
    val continueButton = "#continue"
    val caption = "#main-content > div > div > form > div > label > header > p"
  }

  object Model {

    val employmentSource1 = EmploymentDetails(
      "Mishima Zaibatsu",
      employerRef = Some("223/AB12399"),
      startDate = Some("2019-04-21"),
      currentDataIsHmrcHeld = true
    )
    val employmentId = "223/AB12399"
    val employmentCyaModel = EmploymentCYAModel(employmentSource1)
    val employmentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYear, employmentId, false, employmentCyaModel)
  }

  ".show" when {
    import Selectors._

    userScenarios.foreach{ user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._

        //noinspection ScalaStyle
        "for end of year return a page with filled box" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              taxYear,
              "001",
              isPriorSubmission = true,
              EmploymentCYAModel(
                fullEmploymentsModel(None).hmrcEmploymentData.head.toEmploymentDetails(false),
                None
              )
            ),User(mtditid,if(user.isAgent) Some("12345678") else None,nino,sessionId,if(user.isAgent) "Agent" else "Individual")(fakeRequest))
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }
          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2)
          buttonCheck(user.commonExpectedResults.continue, continueButton)
          textOnPageCheck(user.commonExpectedResults.hint, hintText)
          inputFieldCheck(user.commonExpectedResults.amount, currencyBox)
        }
      }
    }
  }

}
