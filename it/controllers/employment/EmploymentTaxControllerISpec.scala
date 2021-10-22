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

import controllers.employment.routes.CheckEmploymentDetailsController
import models.User
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
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
    val expectedPTextNoData: String
    val expectedPTextWithData: String
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
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your P60."
    val expectedPTextWithData: String = s"If £6,782.92 was not taken in UK tax, tell us the correct amount."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your client’s maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your client’s earnings?"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your client’s P60."
    val expectedPTextWithData: String = s"If £6,782.92 was not taken in UK tax, tell us the correct amount."

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your earnings?"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your P60."
    val expectedPTextWithData: String = s"If £6,782.92 was not taken in UK tax, tell us the correct amount."

  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your client’s maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your client’s earnings?"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your client’s P60."
    val expectedPTextWithData: String = s"If £6,782.92 was not taken in UK tax, tell us the correct amount."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  object Selectors {
    val pText = "#main-content > div > div > form > div > label > p:nth-child(2)"
    val hintText = "#amount-hint"
    val currencyBox = "#amount"
    val continueButton = "#continue"
    val caption = "#main-content > div > div > form > div > label > header > p"
    val inputAmountField = "#amount"
  }



    def cya(taxToDate:Option[BigDecimal] =Some(6782.92), isPriorSubmission:Boolean=true): EmploymentUserData =
      EmploymentUserData (sessionId, mtditid,nino, taxYear, "001", isPriorSubmission, hasPriorBenefits = isPriorSubmission,
      EmploymentCYAModel(
        EmploymentDetails("maggie", totalTaxToDate = taxToDate, currentDataIsHmrcHeld = false),
        None
      )
    )


  val multipleEmployments = fullEmploymentsModel(Seq(employmentDetailsAndBenefits(employmentId = "002"), employmentDetailsAndBenefits()))

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._

        //noinspection ScalaStyle
        "for end of year return a page with prior data" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
            insertCyaData(cya(), User(mtditid, None, nino, sessionId, "test")(fakeRequest))
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.specificExpectedResults.get.expectedPTextWithData, pText)
          buttonCheck(user.commonExpectedResults.continue, continueButton)
          textOnPageCheck(user.commonExpectedResults.hint, hintText)
          inputFieldCheck(user.commonExpectedResults.amount, currencyBox)
        }

        //noinspection ScalaStyle
        "for end of year return a page without prior data" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(cya(None), User(mtditid, None, nino, sessionId, "test")(fakeRequest))
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.specificExpectedResults.get.expectedPTextNoData, pText)
          buttonCheck(user.commonExpectedResults.continue, continueButton)
          textOnPageCheck(user.commonExpectedResults.hint, hintText)
          inputFieldCheck(user.commonExpectedResults.amount, currencyBox)
        }


        "The input field" should {

          "be empty" when {
            "there is cya data with taxToDate field empty and no prior(i.e. user is adding a new employment)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYear)
                insertCyaData(cya(None, isPriorSubmission = false), User(mtditid, None, nino, sessionId, "test")(fakeRequest))
                urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck("", inputAmountField)

            }


            "cya data and prior data are the same(i.e. user has clicked on change link)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(userData(multipleEmployments), nino, taxYear)
                insertCyaData(cya(), User(mtditid, None, nino, sessionId, "test")(fakeRequest))
                urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck("", inputAmountField)

            }
          }

          "be filled" when {
            "cya data and prior data differ (i.e user has updated their pay)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(userData(multipleEmployments), nino, taxYear)
                insertCyaData(cya(Some(100.00)), User(mtditid, None, nino, sessionId, "test")(fakeRequest))
                urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck("100", inputAmountField)
            }

            "cya amount field is filled and prior data is none (i.e user has added a new employment and updated their tax but now want to change it)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYear)
                insertCyaData(cya(Some(100.00), isPriorSubmission = false), User(mtditid, None, nino, sessionId, "agent")(fakeRequest))
                urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
              }
              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck("100", inputAmountField)
            }
          }
        }
        "redirect to the CheckYourEmploymentDetails page there is no CYA data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to OtherPayments not on P60 page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(CheckEmploymentDetailsController.show(taxYear, "001").url)
          }
        }
      }
    }
  }
}
