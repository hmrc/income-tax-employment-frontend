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

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import common.{SessionValues, UUID}
import controllers.employment.routes._
import forms.YesNoForm
import models.IncomeTaxUserData
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}


class AddEmploymentControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val validTaxYear2021 = 2021

  object Selectors {
    val valueHref = "#value"
    val expectedErrorHref = "#value"
    val headingSelector = "#main-content > div > div > header > h1"
    val captionSelector = ".govuk-caption-l"
    val formSelector = "#main-content > div > div > form"
    val yesRadioButton = "#value"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val continueButton: String
    val expectedCaption: String
    val yesText: String
    val noText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${validTaxYear2021 - 1} to 5 April $validTaxYear2021"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${validTaxYear2021 - 1} to 5 April $validTaxYear2021"
    val yesText = "Yes"
    val noText = "No"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "Do you want to add an employment?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to add an employment"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "Do you want to add an employment?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to add an employment"

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Do you want to add an employment?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to add an employment"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Do you want to add an employment?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you want to add an employment"

  }

  private val employmentId = UUID.randomUUID

  private def url(taxYear: Int) = s"$appUrl/$taxYear/add-employment"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, validTaxYear2021, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel = EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc))

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {
    import Selectors._


    userScenarios.foreach { user =>
      import user.commonExpectedResults._

      val specific = user.specificExpectedResults.get

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return Add an employment page" when {

          val taxYear = validTaxYear2021
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(None), nino, taxYear)
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "status OK" in {
            result.status shouldBe 200
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)
          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          buttonCheck(continueButton)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          formPostLinkCheck("/update-and-submit-income-tax-return/employment-income/2021/add-employment", formSelector)
        }

        "return Add an employment page page with yes pre-filled when there is session value employment id is defined" when {

          val taxYear = validTaxYear2021
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(IncomeTaxUserData(None), nino, taxYear)
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)), aUserRequest)
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> "fake-id"))))
          }

          "status OK" in {
            result.status shouldBe 200
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)
          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          buttonCheck(continueButton)
          formPostLinkCheck("/update-and-submit-income-tax-return/employment-income/2021/add-employment", formSelector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
        }

        "redirect to Employment Summary page when there is prior data" when {
          val taxYear = validTaxYear2021
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            urlGet(url(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to Check Employment Details page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(EmploymentSummaryController.show(taxYear).url)
          }
        }

        "redirect to Overview page when trying to hit the page in year" when {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            urlGet(url(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to Overview page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }
        }
      }
    }
  }

  ".submit" should {

    import Selectors._

    val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    val yesNoFormEmpty: Map[String, String] = Map[String, String]()

    userScenarios.foreach { user =>
      import user.commonExpectedResults._

      val specific = user.specificExpectedResults.get

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return Add Employment Page with errors when no radio button is selected" when {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(None), nino, validTaxYear2021)
            urlPost(url(validTaxYear2021), body = yesNoFormEmpty, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
          }

          "status BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)
          titleCheck(specific.expectedErrorTitle)
          h1Check(specific.expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          buttonCheck(continueButton)
          errorSummaryCheck(specific.expectedErrorText, expectedErrorHref)
          formPostLinkCheck("/update-and-submit-income-tax-return/employment-income/2021/add-employment", formSelector)
        }

        "redirect to Overview Page when radio button no is selected" when {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(None), nino, taxYear)
            urlPost(url(validTaxYear2021), follow = false, body = yesNoFormNo, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to Overview page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(validTaxYear2021))
          }
        }

        "redirect to Employer Name page when radio button yes is selected" when {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(None), nino, taxYear)
            urlPost(url(validTaxYear2021), follow = false, body = yesNoFormYes, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to employer name page" in {
            result.header(HeaderNames.LOCATION).toString.contains("/update-and-submit-income-tax-return/employment-income/2021/employer-name?employmentId=") shouldBe true
          }
        }

        "redirect to Employer Name page when radio button yes is selected when an id is in session" when {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(None), nino, taxYear - 1)
            urlPost(url(validTaxYear2021), follow = false, body = yesNoFormYes, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021,
              extraData = Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to employer name page" in {
            result.header(HeaderNames.LOCATION).toString.contains(s"/update-and-submit-income-tax-return/employment-income/2021/employer-name?employmentId=$employmentId") shouldBe true
          }
        }

        "redirect to Employer Reference page when radio button yes is selected when an id is in session and there is cya data" when {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)), aUserRequest)
            userDataStub(IncomeTaxUserData(None), nino, taxYear - 1)
            urlPost(url(validTaxYear2021), follow = false, body = yesNoFormYes, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021,
              extraData = Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to paye ref page" in {
            result.header(HeaderNames.LOCATION).get shouldBe s"/update-and-submit-income-tax-return/employment-income/2021/employer-paye-reference?employmentId=$employmentId"
          }
        }
        "redirect to overview page when radio button no is selected when an id is in session and there is cya data" when {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)), aUserRequest)
            userDataStub(IncomeTaxUserData(None), nino, taxYear - 1)
            urlPost(url(validTaxYear2021), follow = false, body = yesNoFormNo, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021,
              extraData = Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to overview page" in {
            result.header(HeaderNames.LOCATION).get shouldBe "http://localhost:11111/update-and-submit-income-tax-return/2021/view"
          }
        }

        "redirect to Employment Summary page when there is prior data" when {
          val taxYear = validTaxYear2021
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            urlPost(url(validTaxYear2021), follow = false, body = yesNoFormYes, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(validTaxYear2021)))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to Employment summary page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(EmploymentSummaryController.show(taxYear).url)
          }
        }
        "redirect to Overview page when trying to hit the page in year" when {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYear)
            urlPost(url(taxYear), follow = false, body = yesNoFormYes, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to Overview page" in {
            result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }
        }
      }
    }
  }
}
