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

package controllers.benefits.travel

import builders.models.UserBuilder.aUserRequest
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import forms.YesNoForm
import models.benefits.TravelEntertainmentModel
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{checkYourBenefitsUrl, companyBenefitsUrl, fullUrl, incidentalOvernightCostsBenefitsUrl, overviewUrl,
  travelOrEntertainmentBenefitsUrl, travelSubsistenceBenefitsAmountUrl, travelSubsistenceBenefitsUrl}

class TravelAndSubsistenceBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId: String = "employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    anEmploymentUserData.copy(isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior,employment = employmentCyaModel)

  object Selectors {
    val thisIsSelector: String = "#main-content > div > div > form > div > fieldset > legend > p.govuk-body"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedError: String
    val thisIs: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any travel and subsistence benefits?"
    val expectedH1 = "Did you get any travel and subsistence benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got travel and subsistence benefits"
    val thisIs = "This is the cost of any travel and subsistence that is paid for by your employer and is not exempt from tax. This includes hotels and meals."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get any travel and subsistence benefits?"
    val expectedH1 = "Did you get any travel and subsistence benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got travel and subsistence benefits"
    val thisIs = "This is the cost of any travel and subsistence that is paid for by your employer and is not exempt from tax. This includes hotels and meals."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any travel and subsistence benefits?"
    val expectedH1 = "Did your client get any travel and subsistence benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got travel and subsistence benefits"
    val thisIs = "This is the cost of any travel and subsistence that is paid for by their employer and is not exempt from tax. This includes hotels and meals."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any travel and subsistence benefits?"
    val expectedH1 = "Did your client get any travel and subsistence benefits?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got travel and subsistence benefits"
    val thisIs = "This is the cost of any travel and subsistence that is paid for by their employer and is not exempt from tax. This includes hotels and meals."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
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
        "render the 'Did you get travel and subsistence benefits' page with the correct content with no pre-filling" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel(sectionQuestion = Some(true))))
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.thisIs, thisIsSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get travel and subsistence benefits' page with the correct content with cya data and the yes value pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.thisIs, thisIsSelector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))
      "redirect the user to the check employment benefits page when theres no benefits in CYA but has prior benefits" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the benefits received page when theres no benefits in CYA and no prior benefits" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(companyBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when theres no session data for that user" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when the travel or entertainment question is false" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel(sectionQuestion = Some(false))))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
          urlGet(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the travel or entertainment page when the travel or entertainment question is empty" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel(sectionQuestion = None)))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
          urlGet(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(travelOrEntertainmentBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the overview page when the request is in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
          urlGet(fullUrl(travelSubsistenceBenefitsUrl(taxYear, employmentId)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "doesn't create any benefits data" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
          cyaModel.employment.employmentBenefits shouldBe None
        }
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        s"return a BAD_REQUEST($BAD_REQUEST) status" when {
          "the value is empty" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)
          implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(expectedCaption)
            textOnPageCheck(user.specificExpectedResults.get.thisIs, thisIsSelector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
          }
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {
      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))
      "redirect to incidental overnight costs page and update the TravelSubsistenceQuestion to no and wipe the travel data when the user chooses no" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = None)
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the incidental costs page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(incidentalOvernightCostsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelAndSubsistenceQuestion)) shouldBe Some(false)
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelAndSubsistence)) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpenses)) shouldBe Some(200)
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertainingQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertaining)) shouldBe Some(300)
        }
      }

      "redirect to travel subsistence amount page and update the TravelSubsistenceQuestion to yes and when the user chooses yes" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel(sectionQuestion = Some(true))))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the travel and subsistence amount page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(travelSubsistenceBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelAndSubsistenceQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelAndSubsistence)) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion)) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpenses)) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertainingQuestion)) shouldBe None
          cyaModel.employment.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertaining)) shouldBe None
        }
      }

      "redirect the user to the overview page when it is in year" which {
        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(fullUrl(travelSubsistenceBenefitsUrl(taxYear, employmentId)), body = "", user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = None)), aUserRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "doesn't create any benefits data" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
          cyaModel.employment.employmentBenefits shouldBe None
        }
      }

      "redirect the user to the check employment benefits page when the travel or entertainment question is false" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel(sectionQuestion = Some(false))))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
          urlPost(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the travel or entertainment page when the travel or entertainment question is empty" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          val benefitsViewModel = aBenefitsViewModel.copy(travelEntertainmentModel = Some(TravelEntertainmentModel(sectionQuestion = None)))
          insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))), aUserRequest)
          urlPost(fullUrl(travelSubsistenceBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(travelOrEntertainmentBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }
    }
  }
}

