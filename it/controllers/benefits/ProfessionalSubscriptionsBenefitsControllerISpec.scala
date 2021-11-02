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

import models.User
import models.benefits.UtilitiesAndServicesModel
import models.employment.BenefitsViewModel
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm

class ProfessionalSubscriptionsBenefitsControllerISpec extends IntegrationTest with EmploymentDatabaseHelper with ViewHelpers {

  val taxYearEOY: Int = taxYear - 1
  val employmentId: String = "001"

  def professionalSubscriptionsBenefitsPageUrl(taxYear: Int): String =
    s"$appUrl/$taxYear/benefits/professional-fees-or-subscriptions?employmentId=$employmentId"

  val formLink: String =
    s"/income-through-software/return/employment-income/$taxYearEOY/benefits/professional-fees-or-subscriptions?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(utilitiesAndServicesModel: UtilitiesAndServicesModel): BenefitsViewModel =
    BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true, utilitiesAndServicesModel = Some(utilitiesAndServicesModel))

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val paragraphTextSelector = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(2)"
    val checkWithEmployerSelector = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(3)"
    val yesRadioSelector = "#value"
    val noRadioSelector = "#value-no"
    val continueButtonSelector = "#main-content > div > div > form > button"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedParagraphText: String
    val checkWithEmployerText: String
    val expectedErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueButtonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your employer cover costs for any professional fees or subscriptions?"
    val expectedHeading = "Did your employer cover costs for any professional fees or subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedParagraphText: String = "Your employer may have covered fees you must pay to be able to do your job. " +
      "This includes annual subscriptions to approved professional bodies that are relevant to your work."
    val checkWithEmployerText = "Check with your employer if you are unsure."
    val expectedErrorMessage = "Select yes if your employer covered costs for any professional fees or subscriptions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did your employer cover costs for any professional fees or subscriptions?"
    val expectedHeading = "Did your employer cover costs for any professional fees or subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedParagraphText: String = "Your employer may have covered fees you must pay to be able to do your job. " +
      "This includes annual subscriptions to approved professional bodies that are relevant to your work."
    val checkWithEmployerText = "Check with your employer if you are unsure."
    val expectedErrorMessage = "Select yes if your employer covered costs for any professional fees or subscriptions"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer cover costs for any professional fees or subscriptions?"
    val expectedHeading = "Did your client’s employer cover costs for any professional fees or subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedParagraphText: String = "Your client’s employer may have covered fees they must pay to be able to do their job. " +
      "This includes annual subscriptions to approved professional bodies that are relevant to their work."
    val checkWithEmployerText = "Check with your client’s employer if you are unsure."
    val expectedErrorMessage = "Select yes if your client’s employer covered costs for any professional fees or subscriptions"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client’s employer cover costs for any professional fees or subscriptions?"
    val expectedHeading = "Did your client’s employer cover costs for any professional fees or subscriptions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedParagraphText: String = "Your client’s employer may have covered fees they must pay to be able to do their job. " +
      "This includes annual subscriptions to approved professional bodies that are relevant to their work."
    val checkWithEmployerText = "Check with your client’s employer if you are unsure."
    val expectedErrorMessage = "Select yes if your client’s employer covered costs for any professional fees or subscriptions"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  ".show" should {

    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the employer professional subscriptions page with no pre-filled radio buttons" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptionsQuestion = None))))), userRequest)
            urlGet(professionalSubscriptionsBenefitsPageUrl(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.checkWithEmployerText, checkWithEmployerSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(formLink, formSelector)

          welshToggleCheck(user.isWelsh)

        }

        "render the employer professional subscriptions page with the yes radio button pre-filled and the user has cya data and prior benefits" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
            urlGet(professionalSubscriptionsBenefitsPageUrl(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.checkWithEmployerText, checkWithEmployerSelector)
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(formLink, formSelector)

          welshToggleCheck(user.isWelsh)

        }

        "render the employer professional subscriptions page with the no radio button pre-filled and the user has cya data but no prior benefits" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false)))))), userRequest)
            urlGet(professionalSubscriptionsBenefitsPageUrl(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.checkWithEmployerText, checkWithEmployerSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(true))
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(formLink, formSelector)

          welshToggleCheck(user.isWelsh)

        }

      }
    }

    "redirect to the overview page when the tax year isn't end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
        urlGet(professionalSubscriptionsBenefitsPageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

    }

    "redirect to the check your benefits page when there is no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(professionalSubscriptionsBenefitsPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }
  }

  ".submit" should {

    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when a form is submitted with no entry" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
              fullUtilitiesAndServicesModel)))), userRequest)
            urlPost(professionalSubscriptionsBenefitsPageUrl(taxYearEOY), body = "", welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an BAD REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.checkWithEmployerText, checkWithEmployerSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, yesRadioSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))
          formPostLinkCheck(formLink, formSelector)
        }
      }
    }

    "successfully submit a form when a user answers no and has prior benefits" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
          fullUtilitiesAndServicesModel)))), userRequest)
        urlPost(professionalSubscriptionsBenefitsPageUrl(taxYearEOY), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"update the utilitiesAndServicesModel model and redirect to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.utilitiesAndServicesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptionsQuestion)) shouldBe Some(false)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptions)) shouldBe None

      }

    }

    "successfully submit a form when a user answers yes and doesn't have prior benefits" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true, Some(benefits(
          fullUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptionsQuestion = None))))), userRequest)
        urlPost(professionalSubscriptionsBenefitsPageUrl(taxYearEOY), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"update the utilitiesAndServicesModel model and redirect to the professional subscriptions amount page" in {
        result.status shouldBe SEE_OTHER
        //TODO - update to the professional subscriptions amount page when available
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.utilitiesAndServicesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptionsQuestion)) shouldBe Some(true)
      }

    }

    "redirect to the overview page when the tax year isn't end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
        urlPost(professionalSubscriptionsBenefitsPageUrl(taxYear), body = "", follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

    }

    "redirect to the check your benefits page when there is no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(professionalSubscriptionsBenefitsPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

  }

}
