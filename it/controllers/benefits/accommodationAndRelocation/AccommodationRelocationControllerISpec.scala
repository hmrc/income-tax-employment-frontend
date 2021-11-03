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

package controllers.benefits.accommodationAndRelocation

import controllers.benefits.accommodationAndRelocation.routes._
import controllers.benefits.travelAndEntertainment.routes._
import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.User
import models.employment.{AccommodationRelocationModel, BenefitsViewModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}


class AccommodationRelocationControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId: String = "001"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/accommodation-relocation?employmentId=$employmentId"

  val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/accommodation-relocation?employmentId=$employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def filledAccModel(yesNo: Boolean): AccommodationRelocationModel =
    AccommodationRelocationModel(
      accommodationRelocationQuestion = Some(yesNo)
    )

  def fullAccModel: AccommodationRelocationModel =
    AccommodationRelocationModel(
      accommodationRelocationQuestion = Some(true),
      accommodationQuestion = Some(true),
      accommodation = Some(100),
      qualifyingRelocationExpensesQuestion = Some(true),
      qualifyingRelocationExpenses = Some(200),
      nonQualifyingRelocationExpensesQuestion = Some(true),
      nonQualifyingRelocationExpenses = Some(300),
    )

  def benefits(accModel: Option[AccommodationRelocationModel]): BenefitsViewModel =
    BenefitsViewModel(accommodationRelocationModel = accModel, isBenefitsReceived = true, isUsingCustomerData = true)

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
    val expectedTitle = "Did you get accommodation or relocation benefits from this company?"
    val expectedH1 = "Did you get accommodation or relocation benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got accommodation or relocation benefits"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get accommodation or relocation benefits from this company?"
    val expectedH1 = "Did you get accommodation or relocation benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got accommodation or relocation benefits"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get accommodation or relocation benefits from this company?"
    val expectedH1 = "Did your client get accommodation or relocation benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got accommodation or relocation benefits"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get accommodation or relocation benefits from this company?"
    val expectedH1 = "Did your client get accommodation or relocation benefits from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got accommodation or relocation benefits"
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

        "Render the Accommodation Relocation Benefits Question page with no pre-filled data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(None)))), userRequest)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
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
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

        }

        "Render the Accommodation Relocation Benefits Question page with 'yes' as pre-filled data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(Some(filledAccModel(true)))))), userRequest)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
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
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "Redirect to Check Employment Benefits page" when {
          "there is no in-session data for the user" which {
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              urlGet(url(taxYearEOY), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }
            s"has an SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
            }
          }

          "there is in-session data but user has no benefits" which {
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
              urlGet(url(taxYearEOY), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }
            s"has an SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
            }
          }

        }
        "Redirect to overview page if it is not EOY" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
          }
          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
          }
        }
      }
    }


    ".submit" should {
      userScenarios.foreach { user =>
        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

          "Redirect to overview page if it is not EOY" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYear), body = "", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
            }
            "has an SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
            }
          }

          "Update the Accommodation or Relocation Question to 'Yes' when user selects yes and there is previous cya data" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(Some(filledAccModel(false)))))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "redirects to living accommodation Benefits page" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url)
              lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.accommodationRelocationQuestion) shouldBe Some(true)
            }
          }

          "Update the Accommodation or Relocation Question to 'No' and remove accommodation/relocation data when user selects no" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(Some(fullAccModel))))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "redirects to travel entertainment page" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url)
              lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.accommodationRelocationQuestion) shouldBe Some(false)
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.accommodationQuestion) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.accommodation) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.qualifyingRelocationExpensesQuestion) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.qualifyingRelocationExpenses) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.nonQualifyingRelocationExpensesQuestion) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.nonQualifyingRelocationExpenses) shouldBe None
            }

          }

          "Create new AccommodationRelocationModel and redirect to Check Employment Benefits page when user selects 'yes' and no prior benefits" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(None)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has an SEE_OTHER(303) status and redirects to living accommodation page" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url)
            }

            "updates only accommodationRelocationQuestion to yes" in {
              lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.accommodationRelocationQuestion) shouldBe Some(true)
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.accommodationQuestion) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.accommodation) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.qualifyingRelocationExpensesQuestion) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.qualifyingRelocationExpenses) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.nonQualifyingRelocationExpensesQuestion) shouldBe None
              cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel).flatMap(_.nonQualifyingRelocationExpenses) shouldBe None

            }
          }

          "Return BAD_REQUEST when no value is submitted" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(None)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYearEOY), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
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
