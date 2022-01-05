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

import controllers.employment.routes.EmploymentSummaryController
import forms.YesNoForm
import models.IncomeTaxUserData
import models.employment.{AllEmploymentData, EmploymentSource}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class RemoveEmploymentControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId: String = "001"
  val employerName: String = "maggie"

  private def url(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/remove-employment?employmentId=$employmentId"

  private def continueLink(taxYear: Int, employmentId: String): String =
    s"/update-and-submit-income-tax-return/employment-income/$taxYear/remove-employment?employmentId=$employmentId"

  val model: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      EmploymentSource(employmentId = "002", employerName = "apple", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "003", employerName = "google", None, None, None, None, None, None, None, None)
    ),
    hmrcExpenses = None,
    customerEmploymentData = Seq(
      EmploymentSource(employmentId = "001", employerName = "maggie", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "004", employerName = "microsoft", None, None, None, None, None, None, None, None),
      EmploymentSource(employmentId = "005", employerName = "name", None, None, None, None, None, None, None, None)
    ),
    customerExpenses = None
  )

  val modelToDelete: AllEmploymentData = model.copy(
    hmrcEmploymentData = Seq(),
    customerEmploymentData = Seq(
      EmploymentSource(employmentId = "001", employerName = "maggie", None, None, None, None, None, None, None, None))
  )

  object Selectors {
    val paragraphTextSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val radioButtonSelector = "#main-content > div > div > form > div > fieldset > div"
    val yesRadioButtonSelector = "#value"
    val noRadioButtonSelector = "#value-no"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedRemoveAccountText: String
    val expectedLastAccountText: String
    val continueButton: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedRemoveAccountText = "This will also remove any benefits you told us about for this employment."
    val expectedLastAccountText = "This will remove all your employment for this tax year."
    val continueButton = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedRemoveAccountText = "This will also remove any benefits you told us about for this employment."
    val expectedLastAccountText = "This will remove all your employment for this tax year."
    val continueButton = "Continue"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedHeading: String
    val expectedErrorNoEntry: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHeading = s"Are you sure you want to remove $employerName?"
    val expectedErrorNoEntry = "Select yes if you want to remove this employment"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHeading = s"Are you sure you want to remove $employerName?"
    val expectedErrorNoEntry = "Select yes if you want to remove this employment"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHeading = s"Are you sure you want to remove $employerName?"
    val expectedErrorNoEntry = "Select yes if you want to remove this employment"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHeading = s"Are you sure you want to remove $employerName?"
    val expectedErrorNoEntry = "Select yes if you want to remove this employment"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

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
            userDataStub(IncomeTaxUserData(Some(model)), nino, taxYearEOY)
            urlGet(url(taxYearEOY, employmentId), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK ($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedHeading)
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveAccountText, paragraphTextSelector)
          radioButtonCheck("Yes", 1, checked = false)
          radioButtonCheck("No", 2, checked = false)
          buttonCheck(common.continueButton, continueButtonSelector)
        }

        "render the remove employment page for when it's the last employment" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(modelToDelete)), nino, taxYearEOY)
            urlGet(url(taxYearEOY, employmentId), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK ($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedHeading)
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedLastAccountText, paragraphTextSelector)
          radioButtonCheck("Yes", 1, checked = false)
          radioButtonCheck("No", 2, checked = false)
          buttonCheck(common.continueButton, continueButtonSelector)
        }

        "redirect to the overview page" when {

          "it is not end of year" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(modelToDelete)), nino, taxYear)
              urlGet(url(taxYear, employmentId), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
            }

            s"has a SEE_OTHER ($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
            }
          }

          "the user does not have employment data with that employmentId" which {

            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(modelToDelete)), nino, taxYearEOY)
              urlGet(url(taxYearEOY, "123"), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
            }

            s"has a SEE_OTHER ($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYearEOY/view")
            }

          }
        }
      }
    }

  }

  ".submit" should {

    import Selectors._

    userScenarios.foreach { user =>

      val common = user.commonExpectedResults
      val specific = user.specificExpectedResults.get

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect the user to the overview page" when {

          "it is not end of year" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYear, employmentId), body = "", user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }
            s"has a SEE_OTHER ($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
            }

          }

          "the user does not have employment data with that employmentId" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(model)), nino, taxYearEOY)
              urlPost(url(taxYearEOY, "123"), body = "", user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has a SEE_OTHER ($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYearEOY/view")
            }

          }

          "there is no employment data found" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
              urlPost(url(taxYearEOY, employmentId), body = "", user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has a SEE_OTHER ($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYearEOY/view")
            }

          }

        }

        "redirect to the employment summary page" when {

          "an employment is removed" which {
            val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(model)), nino, taxYearEOY)
              userDataStubDeleteOrIgnoreEmployment(IncomeTaxUserData(Some(modelToDelete)), nino, taxYearEOY, employmentId, "CUSTOMER")
              urlPost(url(taxYearEOY, employmentId), body = form, user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "redirects to the employment summary page" in {
              result.status shouldBe SEE_OTHER
              result.header(HeaderNames.LOCATION) shouldBe Some(EmploymentSummaryController.show(taxYearEOY).url)

            }

          }

          "the 'no' radio button is selected " which {

            val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(model)), nino, taxYearEOY)
              urlPost(url(taxYearEOY, employmentId), body = form, user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has a SEE_OTHER ($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header(HeaderNames.LOCATION) shouldBe Some(EmploymentSummaryController.show(taxYearEOY).url)
            }
          }
        }

        s"return an error when there is no entry" which {

          val form: Map[String, String] = Map[String, String]()

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(model)), nino, taxYearEOY)
            urlPost(url(taxYearEOY, employmentId), body = form, user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }
          s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)
          titleCheck(specific.expectedErrorTitle)
          h1Check(specific.expectedHeading)
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedRemoveAccountText, paragraphTextSelector)
          buttonCheck(common.continueButton)
          errorSummaryCheck(specific.expectedErrorNoEntry, yesRadioButtonSelector)
          formPostLinkCheck(continueLink(taxYearEOY, employmentId), formSelector)

        }

      }

    }

  }
}
