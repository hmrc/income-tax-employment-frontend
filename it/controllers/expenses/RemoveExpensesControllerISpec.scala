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

package controllers.expenses

import models.IncomeTaxUserData
import models.employment.{AllEmploymentData, EmploymentSource}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import utils.PageUrls.{employmentSummaryUrl, fullUrl, overviewUrl, removeExpensesUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class RemoveExpensesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1


  private val model: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      EmploymentSource(employmentId = "002", employerName = "apple", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "003", employerName = "google", None, None, None, None, None, None, None, None)
    ),
    hmrcExpenses = Some(anEmploymentExpenses),
    customerEmploymentData = Seq(
      EmploymentSource(employmentId = "001", employerName = "maggie", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "004", employerName = "microsoft", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "005", employerName = "name", None, None, None, None, None, None, None, None)
    ),
    customerExpenses = Some(anEmploymentExpenses)
  )

  private val modelToDelete: AllEmploymentData = model.copy(
    hmrcExpenses = Some(anEmploymentExpenses),
    customerExpenses = Some(anEmploymentExpenses)

  )

  object Selectors {
    val paragraphTextSelector = "#main-content > div > div > form > p"
    val removeExpensesButtonSelector = "#remove-expenses-button-id"
    val cancelLinkSelector = "#cancel-link-id"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedRemoveExpensesText: String
    val expectedRemoveExpensesButton: String
    val expectedCancelLink: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedRemoveExpensesText = "This will remove expenses for all employment in this tax year."
    val expectedRemoveExpensesButton = "Remove expenses"
    val expectedCancelLink = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedRemoveExpensesText = "This will remove expenses for all employment in this tax year."
    val expectedRemoveExpensesButton = "Remove expenses"
    val expectedCancelLink = "Cancel"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove your expenses?"
    val expectedHeading = "Are you sure you want to remove your expenses?"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove your client’s expenses?"
    val expectedHeading = "Are you sure you want to remove your client’s expenses?"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove your expenses?"
    val expectedHeading = "Are you sure you want to remove your expenses?"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove your client’s expenses?"
    val expectedHeading = "Are you sure you want to remove your client’s expenses?"
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
        "render the remove expenses page when user has expenses" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(model)), nino, taxYearEOY)
            urlGet(fullUrl(removeExpensesUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK ($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)
          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedHeading)
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveExpensesText, paragraphTextSelector)
          buttonCheck(common.expectedRemoveExpensesButton, removeExpensesButtonSelector)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, employmentSummaryUrl(taxYearEOY))
          formPostLinkCheck(removeExpensesUrl(taxYearEOY), formSelector)
        }
      }
    }

    "redirect to the overview page" when {
      "it is not end of year" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          userDataStub(IncomeTaxUserData(Some(modelToDelete)), nino, taxYear)
          urlGet(fullUrl(removeExpensesUrl(taxYear)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }


    }
  }

  ".submit" should {

    "redirect the user to the overview page" when {
      "it is not end of year" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          urlPost(fullUrl(removeExpensesUrl(taxYear)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "there is no employment data found" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
          urlPost(fullUrl(removeExpensesUrl(taxYearEOY)), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYearEOY)) shouldBe true
        }
      }
    }

    "redirect to the employment summary page" when {
      "expenses is removed" which {
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = true)
          userDataStub(IncomeTaxUserData(Some(modelToDelete)), nino, taxYearEOY)
          userDataStubDeleteExpenses(IncomeTaxUserData(Some(modelToDelete)), nino, taxYearEOY, "ALL")
          urlPost(fullUrl(removeExpensesUrl(taxYearEOY)), body = "", follow = false,
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
