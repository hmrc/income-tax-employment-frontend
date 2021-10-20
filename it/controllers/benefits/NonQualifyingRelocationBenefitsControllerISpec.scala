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

package controllers.benefits

import forms.YesNoForm
import models.User
import models.employment.{AccommodationRelocationModel, BenefitsViewModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class NonQualifyingRelocationBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = 2021
  val employmentId = "001"

  def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/non-qualifying-relocation?employmentId=$employmentId"

  val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/non-qualifying-relocation?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)


  def benefits(accModel: AccommodationRelocationModel): BenefitsViewModel =
    BenefitsViewModel(accommodationRelocationModel = Some(accModel), isBenefitsReceived = true, isUsingCustomerData = true)

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val contentSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val contentExample1Selector = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(1)"
    val contentExample2Selector = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(2)"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val yesText: String
    val noText: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
    val expectedContent: String
    val expectedExample1: String
    val expectedExample2: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val yesText = "Yes"
    val noText = "No"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val yesText = "Yes"
    val noText = "No"
    val continueButtonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any non-qualifying relocation benefits?"
    val expectedH1 = "Did you get any non-qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got non-qualifying relocation benefits"
    val expectedContent = "These are relocation costs that your employer has paid for, or reimbursed you for. Examples include:"
    val expectedExample1 = "mortgage or housing payments if you’re moving to a more expensive area"
    val expectedExample2 = "compensation if you lose money when selling your home"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get any non-qualifying relocation benefits?"
    val expectedH1 = "Did you get any non-qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got non-qualifying relocation benefits"
    val expectedContent = "These are relocation costs that your employer has paid for, or reimbursed you for. Examples include:"
    val expectedExample1 = "mortgage or housing payments if you’re moving to a more expensive area"
    val expectedExample2 = "compensation if you lose money when selling your home"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any non-qualifying relocation benefits?"
    val expectedH1 = "Did your client get any non-qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got non-qualifying relocation benefits"
    val expectedContent = "These are relocation costs that their employer has paid for, or reimbursed them for. Examples include:"
    val expectedExample1 = "mortgage or housing payments if they’re moving to a more expensive area"
    val expectedExample2 = "compensation if they lose money when selling their home"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any non-qualifying relocation benefits?"
    val expectedH1 = "Did your client get any non-qualifying relocation benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got non-qualifying relocation benefits"
    val expectedContent = "These are relocation costs that their employer has paid for, or reimbursed them for. Examples include:"
    val expectedExample1 = "mortgage or housing payments if they’re moving to a more expensive area"
    val expectedExample2 = "compensation if they lose money when selling their home"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the non-qualifying relocation benefits question page with no pre-filled radio buttons" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }
          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, contentSelector)
          textOnPageCheck(get.expectedExample1, contentExample1Selector)
          textOnPageCheck(get.expectedExample2, contentExample2Selector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)

          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
        }

        "render the non-qualifying relocation benefits question page with cya data and 'yes' radio selected" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              Some(benefits(fullAccommodationRelocationModel.copy(nonQualifyingRelocationExpensesQuestion = Some(true)))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }
          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, contentSelector)
          textOnPageCheck(get.expectedExample1, contentExample1Selector)
          textOnPageCheck(get.expectedExample2, contentExample2Selector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)

          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
        }
      }
    }

    "redirect user to check employment benefits page" when {

      "user has no benefits" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }
        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe
            Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
        }
      }

      "user has no accommodation relocation benefits and it's a prior submission" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            Some(benefits(emptyAccommodationRelocationModel)))), userRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }
        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe
            Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
        }
      }
    }

    "redirect user to tax overview page if it's not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(url(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }
  }


  ".submit" should {
    userScenarios.foreach {
      user =>
        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

          s"return a BAD_REQUEST($BAD_REQUEST) if no value is submitted" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
                Some(benefits(fullAccommodationRelocationModel)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYearEOY), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }
            "has a BAD_REQUEST status" in {
              result.status shouldBe BAD_REQUEST
            }

            import Selectors._
            import user.commonExpectedResults._
            import user.specificExpectedResults._

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(expectedCaption)
            textOnPageCheck(get.expectedContent, contentSelector)
            textOnPageCheck(get.expectedExample1, contentExample1Selector)
            textOnPageCheck(get.expectedExample2, contentExample2Selector)
            welshToggleCheck(user.isWelsh)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, formSelector)

            radioButtonCheck(yesText, 1, Some(false))
            radioButtonCheck(noText, 2, Some(false))
            errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
          }
        }
    }

    "redirect to check employment benefits page when user selects Yes and it's a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel)))), userRequest)
        urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }

      "updates non-QualifyingRelocation Expenses Question to Yes" in {
        lazy val cya = findCyaData(taxYearEOY, employmentId, userRequest).get
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpensesQuestion)) shouldBe Some(true)
      }
    }

    "redirect to check employment benefits page and wipe the non-Qualifying Relocation Amount if user selects No" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel)))), userRequest)
        urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }

      "updates non-QualifyingRelocation Expenses Question to No and removes non-QualifyingRelocation Expenses amount" in {
        lazy val cya = findCyaData(taxYearEOY, employmentId, userRequest).get
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationRelocationQuestion)) shouldBe Some(true)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationQuestion)) shouldBe Some(true)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation)) shouldBe Some(100.00)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpensesQuestion)) shouldBe Some(true)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpenses)) shouldBe Some(200.00)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpensesQuestion)) shouldBe Some(false)
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpenses)) shouldBe None
      }
    }

    "redirect to check employment benefits page if valid form is submitted and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(fullAccommodationRelocationModel)))), userRequest)
        urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }

      "updates non-QualifyingRelocation Expenses Question to Yes" in {
        lazy val cya = findCyaData(taxYearEOY, employmentId, userRequest).get
        cya.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpensesQuestion)) shouldBe Some(true)
      }
    }

    "redirect to check employment benefits page when there is no CYA data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe
          Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
      }
    }

    "redirect to tax overview page if it's not EOY" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(url(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }
    }
  }
}







