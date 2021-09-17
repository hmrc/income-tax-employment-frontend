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

import forms.YesNoForm
import models.User
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class StillWorkingForEmployerControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employerName: String = "HMRC"
  val employmentStartDate: String = "2020-01-01"
  val employmentId: String = "001"
  val cessationDate: Option[String] = Some("2021-01-01")

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, startDate: Option[String] = Some(employmentStartDate), cessationDate: Option[String] = None,
               cessationDateQuestion: Option[Boolean] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc, employerRef = Some("123/AB456"),
      startDate = startDate, cessationDate = cessationDate, cessationDateQuestion = cessationDateQuestion), None)

  private def stillWorkingForEmployerPageUrl(taxYear: Int) = s"$appUrl/$taxYear/still-working-for-employer?employmentId=$employmentId"

  val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/still-working-for-employer?employmentId=$employmentId"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Are you still working for your employer?"
    val expectedH1 = "Are you still working at HMRC?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you are still working for your employer"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Are you still working for your employer?"
    val expectedH1 = "Are you still working at HMRC?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you are still working for your employer"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Is your client still working for their employer?"
    val expectedH1 = "Is your client still working at HMRC?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client is still working for their employer"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Is your client still working for their employer?"
    val expectedH1 = "Is your client still working at HMRC?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client is still working for their employer"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'still working for employer' page with the correct content" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName, hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(stillWorkingForEmployerPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'still working for employer' with the correct content and the yes radio populated when its already in session" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, cessationDate = Some("2021-01-01"),
              cessationDateQuestion = Some(true), hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(stillWorkingForEmployerPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'still working for employer' with the correct content and the no radio populated when its already in session" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, cessationDate = None,
              cessationDateQuestion = Some(false), hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(stillWorkingForEmployerPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'still working for employer' page for prior year with correct content and default yes value when cessation date is not present" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, cessationDateQuestion = None, hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(stillWorkingForEmployerPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'still working for employer' page for prior year with correct content and default no when cessation is present" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, cessationDate = cessationDate,
              cessationDateQuestion = None, hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(stillWorkingForEmployerPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "redirect the user to the overview page when it is not end of year" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(stillWorkingForEmployerPageUrl(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
          }

        }
      }
    }
  }

  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect the user to the overview page when it is not end of year" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(stillWorkingForEmployerPageUrl(taxYear), body = "", user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
          }
        }

        "Update the cessationDateQuestion to yes and wipe the cessationDate data when the user chooses yes" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName, cessationDate = cessationDate,
              cessationDateQuestion = None, hmrc = true)), userRequest)

            authoriseAgentOrIndividual(user.isAgent)
            urlPost(stillWorkingForEmployerPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the how much pay page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/how-much-pay?employmentId=$employmentId")
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyamodel.employment.employmentDetails.cessationDate shouldBe None
            cyamodel.employment.employmentDetails.cessationDateQuestion shouldBe Some(true)

          }

        }

        "Update the cessationDateQuestion to no and when the user chooses no and not wipe out the cessation date" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName, cessationDate = cessationDate,
              cessationDateQuestion = None, hmrc = true)), userRequest)

            authoriseAgentOrIndividual(user.isAgent)
            urlPost(stillWorkingForEmployerPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          //TODO: should navigate to cessationDate page when available
          "redirects to the how much pay details page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/how-much-pay?employmentId=$employmentId")
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyamodel.employment.employmentDetails.cessationDate shouldBe cessationDate
            cyamodel.employment.employmentDetails.cessationDateQuestion shouldBe Some(false)

          }

        }

        "Redirect to the employer details page when a previous answers is missing after updating the yes/no value" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel(employerName, startDate = None, cessationDate = cessationDate,
              cessationDateQuestion = None, hmrc = true)), userRequest)

            authoriseAgentOrIndividual(user.isAgent)
            urlPost(stillWorkingForEmployerPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the missing start date page page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/employment-start-date?employmentId=$employmentId")
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyamodel.employment.employmentDetails.cessationDate shouldBe cessationDate
            cyamodel.employment.employmentDetails.cessationDateQuestion shouldBe Some(false)

          }

        }

        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "the value is empty" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel(employerName, hmrc = true)), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(stillWorkingForEmployerPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
          }
        }

      }
    }

  }
}
