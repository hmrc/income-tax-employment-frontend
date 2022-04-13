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

import models.IncomeTaxUserData
import models.employment.{AllEmploymentData, EmploymentSource, HmrcEmploymentSource}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import utils.PageUrls.{employmentSummaryUrl, fullUrl, overviewUrl, removeEmploymentUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class RemoveEmploymentControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"
  private val employerName: String = "maggie"

  private val modelWithMultipleSources: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      HmrcEmploymentSource(employmentId = "002", employerName = "apple", None, None, None, None, None, None, None, None),
      HmrcEmploymentSource(employmentId = "003", employerName = "google", None, None, None, None, None, None, None, None)
    ),
    hmrcExpenses = None,
    customerEmploymentData = Seq(
      EmploymentSource(employmentId = "employmentId", employerName = "maggie", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "004", employerName = "microsoft", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "005", employerName = "name", None, None, None, None, None, None, None, None)
    ),
    customerExpenses = None
  )

  object Selectors {
    val paragraphTextSelector = "#main-content > div > div > form > p"
    val insetTextSelector = "#main-content > div > div > form > div.govuk-inset-text"
    val removeEmployerButtonSelector = "#remove-employer-button-id"
    val cancelLinkSelector = "#cancel-link-id"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedRemoveAccountText: String
    val expectedLastAccountText: String
    val expectedRemoveEmployerButton: String
    val expectedCancelLink: String
    val infoWeHold: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"PAYE employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedRemoveAccountText = "If you remove this period of employment, you’ll also remove any employment benefits and student loans." +
      " You must remove any expenses from the separate expenses section."
    val expectedLastAccountText = "This will also remove any benefits and expenses for this employer."
    val expectedRemoveEmployerButton = "Remove employer"
    val infoWeHold = "This is information we hold about you. If the information is incorrect, you need to contact the employer"
    val expectedCancelLink = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"PAYE employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedRemoveAccountText = "If you remove this period of employment, you’ll also remove any employment benefits and student loans." +
      " You must remove any expenses from the separate expenses section."
    val expectedLastAccountText = "This will also remove any benefits and expenses for this employer."
    val expectedRemoveEmployerButton = "Remove employer"
    val infoWeHold = "This is information we hold about you. If the information is incorrect, you need to contact the employer"
    val expectedCancelLink = "Cancel"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    def expectedHeading(employerName: String): String
    val expectedErrorNoEntry: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    def expectedHeading(employerName: String): String = s"Are you sure you want to remove $employerName?"
    val expectedErrorNoEntry = "Select yes if you want to remove this employment"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    def expectedHeading(employerName: String): String = s"Are you sure you want to remove $employerName?"
    val expectedErrorNoEntry = "Select yes if you want to remove this employment"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    def expectedHeading(employerName: String): String = s"Are you sure you want to remove $employerName?"
    val expectedErrorNoEntry = "Select yes if you want to remove this employment"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    def expectedHeading(employerName: String): String = s"Are you sure you want to remove $employerName?"
    val expectedErrorNoEntry = "Select yes if you want to remove this employment"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {

    import Selectors._

    userScenarios.foreach { user =>

      val common = user.commonExpectedResults
      val specific = user.specificExpectedResults.get

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the remove employment page for when it isn't the last employment" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(modelWithMultipleSources)), nino, taxYearEOY)
            urlGet(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK ($OK) status" in {
            result.status shouldBe OK
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          welshToggleCheck(user.isWelsh)

          titleCheck(specific.expectedTitle, user.isWelsh)
          h1Check(specific.expectedHeading(employerName))
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveAccountText, paragraphTextSelector)
          buttonCheck(common.expectedRemoveEmployerButton, removeEmployerButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl(taxYearEOY))
          formPostLinkCheck(removeEmploymentUrl(taxYearEOY, employmentId), formSelector)
        }
        "render the remove employment page for when it isn't the last employment and removing a hmrc employment" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(modelWithMultipleSources)), nino, taxYearEOY)
            urlGet(fullUrl(removeEmploymentUrl(taxYearEOY, "002")), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK ($OK) status" in {
            result.status shouldBe OK
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          welshToggleCheck(user.isWelsh)

          titleCheck(specific.expectedTitle, user.isWelsh)
          h1Check(specific.expectedHeading("apple"))
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveAccountText, paragraphTextSelector)
          textOnPageCheck(common.infoWeHold, insetTextSelector)
          buttonCheck(common.expectedRemoveEmployerButton, removeEmployerButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl(taxYearEOY))
          formPostLinkCheck(removeEmploymentUrl(taxYearEOY, "002"), formSelector)
        }

        "render the remove employment page for when it's the last employment" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY)
            urlGet(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK ($OK) status" in {
            result.status shouldBe OK
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          welshToggleCheck(user.isWelsh)

          titleCheck(specific.expectedTitle, user.isWelsh)
          h1Check(specific.expectedHeading(employerName))
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedLastAccountText, paragraphTextSelector)
          buttonCheck(common.expectedRemoveEmployerButton, removeEmployerButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl(taxYearEOY))
          formPostLinkCheck(removeEmploymentUrl(taxYearEOY, employmentId), formSelector)
        }
      }
    }
    "redirect to the overview page" when {
      "it is not end of year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYear)
          urlGet(fullUrl(removeEmploymentUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "the user does not have employment data with that employmentId" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY)
          urlGet(fullUrl(removeEmploymentUrl(taxYearEOY, "123")), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }

  ".submit" should {

    "redirect the user to the overview page" when {
      "it is not end of year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(removeEmploymentUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "the user does not have employment data with that employmentId" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(modelWithMultipleSources)), nino, taxYearEOY)
          urlPost(fullUrl(removeEmploymentUrl(taxYearEOY, "123")), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYearEOY)) shouldBe true
        }
      }

      "there is no employment data found" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
          urlPost(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYearEOY)) shouldBe true
        }
      }
    }

    "redirect to the employment summary page" when {

      "an employment is removed" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(modelWithMultipleSources)), nino, taxYearEOY)
          userDataStubDeleteOrIgnoreEmployment(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY, employmentId, "CUSTOMER")
          urlPost(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the employment summary page" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        }
      }

      "the last employment is removed" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY)
          userDataStubDeleteOrIgnoreEmployment(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY, employmentId, "HMRC-HELD")
          userDataStubDeleteExpenses(IncomeTaxUserData(Some(anAllEmploymentData)), nino, taxYearEOY, "HMRC-HELD")
          urlPost(fullUrl(removeEmploymentUrl(taxYearEOY, employmentId)), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the employment summary page" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(employmentSummaryUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }
}
