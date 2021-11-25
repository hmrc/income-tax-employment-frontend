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

package controllers.benefits.income

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import controllers.benefits.income.routes.IncomeTaxBenefitsController
import controllers.benefits.reimbursed.routes.ReimbursedCostsVouchersAndNonCashBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.benefits.{BenefitsViewModel, IncomeTaxAndCostsModel}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class IncomeTaxOrIncurredCostsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId: String = "employmentId"
  private val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/employer-income-tax-or-incurred-costs?employmentId=$employmentId"

  private def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/employer-income-tax-or-incurred-costs?employmentId=$employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employment = employmentCyaModel)

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
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your employer pay any of your Income Tax or incurred costs?"
    val expectedH1 = "Did your employer pay any of your Income Tax or incurred costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your employer paid any of your Income Tax or incurred costs"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did your employer pay any of your Income Tax or incurred costs?"
    val expectedH1 = "Did your employer pay any of your Income Tax or incurred costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your employer paid any of your Income Tax or incurred costs"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer pay any of their Income Tax or incurred costs?"
    val expectedH1 = "Did your client’s employer pay any of their Income Tax or incurred costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client’s employer paid any of their Income Tax or incurred costs"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer pay any of their Income Tax or incurred costs?"
    val expectedH1 = "Did your client’s employer pay any of their Income Tax or incurred costs?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client’s employer paid any of their Income Tax or incurred costs"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "Render the Income Tax or Incurred Costs Benefits Question page with no pre-filled data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)))), aUserRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "Render the Income Tax or Incurred Costs Question page with 'yes' as pre-filled data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "Render the Income Tax or Incurred Costs Question page with 'no' as pre-filled data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(IncomeTaxAndCostsModel(sectionQuestion = Some(false))))
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "Redirect to the Check Employment Benefits page when there is no in-session data for the user" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "Redirect to the Check Employment Benefits page when the benefits received flag is false" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(BenefitsViewModel(isUsingCustomerData = true)))), aUserRequest)
        urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }

    "Redirect to the overview page if it is not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(url(taxYear), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }
      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some("http://localhost:11111/update-and-submit-income-tax-return/2022/view")
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "Render the Income Tax or Incurred Costs Benefits Question page with an error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "returns a bad request" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }

    "Redirect to overview page if it is not EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(url(taxYear), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some("http://localhost:11111/update-and-submit-income-tax-return/2022/view")
      }
    }

    "Update the Income Tax Or Costs Question to 'Yes' when user selects yes and there is previous cya data, redirects to income tax question" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = Some(IncomeTaxAndCostsModel(sectionQuestion = Some(false))))
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the income tax question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(IncomeTaxBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "update the income tax or incurred costs question to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel).flatMap(_.sectionQuestion) shouldBe Some(true)
      }
    }

    "Create a new IncomeTaxAndCostsModel and redirect to income tax question page when user selects 'yes' and no prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the income tax question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(IncomeTaxBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "creates a new incomeTaxAndCostsModel with the sectionQuestion as true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel).get shouldBe IncomeTaxAndCostsModel(sectionQuestion = Some(true))
      }
    }

    "Update the Income Tax Or Costs Question to 'No' and remove income tax data when user selects no, redirects to cya if prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "updates the Income tax or costs question to false and wipes the other values" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel).get shouldBe IncomeTaxAndCostsModel(sectionQuestion = Some(false))
      }
    }

    "Update the Income Tax Or Costs Question to 'No' and remove income tax data when user selects no, redirects to reimbursed section if no prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = None)
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(url(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the reimbursed section page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYearEOY, employmentId).url)
      }

      "updates the Income tax or costs question to false and wipes the other values" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.incomeTaxAndCostsModel).get shouldBe IncomeTaxAndCostsModel(sectionQuestion = Some(false))
      }
    }
  }
}
