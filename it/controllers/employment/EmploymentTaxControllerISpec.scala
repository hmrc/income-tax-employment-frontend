/*
 * Copyright 2022 HM Revenue & Customs
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

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import models.employment.AllEmploymentData
import models.mongo.EmploymentUserData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{checkYourDetailsUrl, fullUrl, howMuchTaxUrl}

class EmploymentTaxControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId = "employmentId"
  private val amountInputName = "amount"

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
    val expectedPTextWithData: String = s"If £200 was not taken in UK tax, tell us the correct amount."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your client’s maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your client’s earnings?"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your client’s P60."
    val expectedPTextWithData: String = s"If £200 was not taken in UK tax, tell us the correct amount."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your earnings?"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your P60."
    val expectedPTextWithData: String = s"If £200 was not taken in UK tax, tell us the correct amount."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your client’s maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your client’s earnings?"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your client’s P60."
    val expectedPTextWithData: String = s"If £200 was not taken in UK tax, tell us the correct amount."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  object Selectors {
    val pText = "#main-content > div > div > form > div > label > p:nth-child(2)"
    val hintText = "#amount-hint"
    val currencyBox = "#amount"
    val continueButton = "#continue"
    val caption = "#main-content > div > div > form > div > label > header > p"
    val inputAmountField = "#amount"
  }

  private def cya(taxToDate: Option[BigDecimal] = Some(200), isPriorSubmission: Boolean = true): EmploymentUserData =
    anEmploymentUserData.copy(
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = isPriorSubmission,
      employment = anEmploymentCYAModel.copy(anEmploymentDetails.copy("maggie", totalTaxToDate = taxToDate, currentDataIsHmrcHeld = false))
    )

  val multipleEmployments: AllEmploymentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(
    anEmploymentSource.copy(employmentBenefits = None),
    anEmploymentSource.copy(employmentId = "002", employmentBenefits = None)
  ))

  ".show" when {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        //noinspection ScalaStyle
        "for end of year return a page with prior data" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(cya(), aUserRequest)
            urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          inputFieldValueCheck(amountInputName, inputAmountField, "")
        }

        //noinspection ScalaStyle
        "for end of year return a page without prior data" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(cya(None), aUserRequest)
            urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          inputFieldValueCheck(amountInputName, inputAmountField, "")
        }


        "The input field" should {

          "be empty" when {
            "there is cya data with taxToDate field empty and no prior(i.e. user is adding a new employment)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(None, isPriorSubmission = false), aUserRequest)
                urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputAmountField, "")
            }

            "cya data and prior data are the same(i.e. user has clicked on change link)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYearEOY)
                insertCyaData(cya(), aUserRequest)
                urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputAmountField, "")
            }
          }

          "be filled" when {
            "cya data and prior data differ (i.e user has updated their pay)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYearEOY)
                insertCyaData(cya(Some(100.00)), aUserRequest)
                urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputAmountField, "100")
            }

            "cya amount field is filled and prior data is none (i.e user has added a new employment and updated their tax but now want to change it)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(Some(100.00), isPriorSubmission = false), aUserRequest)
                urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              inputFieldValueCheck(amountInputName, inputAmountField, "100")
            }
          }
        }
        "redirect to the CheckYourEmploymentDetails page there is no CYA data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to OtherPayments not on P60 page" in {
            result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
          }
        }
      }
    }
  }
}
