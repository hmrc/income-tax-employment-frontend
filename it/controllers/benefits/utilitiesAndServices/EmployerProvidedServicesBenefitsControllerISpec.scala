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

package controllers.benefits.utilitiesAndServices

import controllers.benefits.utilitiesAndServices.routes.UtilitiesOrGeneralServicesBenefitsController
import controllers.benefits.medicalChildcareEducation.routes.MedicalDentalChildcareBenefitsController
import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.User
import models.benefits.{BenefitsViewModel, UtilitiesAndServicesModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerProvidedServicesBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = 2021
  val employmentId: String = "001"

  def employerProvidedServicesPageUrl(taxYear: Int): String = s"$appUrl/$taxYear/benefits/employer-provided-services?employmentId=$employmentId"

  val continueButtonLink: String = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/employer-provided-services?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(utilitiesModel: UtilitiesAndServicesModel): BenefitsViewModel =
    BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true, utilitiesAndServicesModel = Some(utilitiesModel))

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val paragraphSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val continueButtonSelector = "#continue"
    val detailsTitleSelector = "#main-content > div > div > form > details > summary > span"
    val detailsText1Selector = "#main-content > div > div > form > details > div > p:nth-child(1)"
    val detailsText2Selector = "#main-content > div > div > form > details > div > p:nth-child(2)"
    val detailsText3Selector = "#main-content > div > div > form > details > div > p:nth-child(3)"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val yesText: String
    val noText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
    val expectedParagraphText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedHintText = "For example, subscriptions or laundry services."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get a benefit for services provided by your employer?"
    val expectedHeading = "Did you get a benefit for services provided by your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you got a benefit for services provided by your employer"
    val expectedParagraphText = "These are services you used that are not related to your job. Your employer pays for them. For example, subscriptions or laundry services."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get a benefit for services provided by your employer?"
    val expectedHeading = "Did you get a benefit for services provided by your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you got a benefit for services provided by your employer"
    val expectedParagraphText = "These are services you used that are not related to your job. Your employer pays for them. For example, subscriptions or laundry services."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a benefit for services provided by their employer?"
    val expectedHeading = "Did your client get a benefit for services provided by their employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client got a benefit for services provided by their employer"
    val expectedParagraphText = "These are services they used that are not related to their job. Their employer pays for them. For example, subscriptions or laundry services."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a benefit for services provided by their employer?"
    val expectedHeading = "Did your client get a benefit for services provided by their employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client got a benefit for services provided by their employer"
    val expectedParagraphText = "These are services they used that are not related to their job. Their employer pays for them. For example, subscriptions or laundry services."
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

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'Did you get employer provided services benefits?' page with no pre-filled radio buttons" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel.copy(employerProvidedServicesQuestion = None))))), userRequest)
            urlGet(employerProvidedServicesPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the 'Did you get employer provided services benefits?' page with the 'yes' radio button pre-filled and cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel.copy(employerProvidedServicesQuestion = Some(true)))))), userRequest)
            urlGet(employerProvidedServicesPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector)
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the 'Did you get employer provided services benefits?' page with the 'no' radio button pre-filled and cya data" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullUtilitiesAndServicesModel.copy(employerProvidedServicesQuestion = Some(false)))))), userRequest)
            urlGet(employerProvidedServicesPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(true))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, formSelector)
          welshToggleCheck(user.isWelsh)

        }

      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(employerProvidedServicesPageUrl(taxYear), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }

    }

    "redirect to the initial utilities page when utilitiesAndServicesQuestion is None" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyUtilitiesAndServicesModel.copy(utilitiesAndServicesQuestion = None))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(employerProvidedServicesPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(UtilitiesOrGeneralServicesBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the check your benefits page when utilitiesAndServicesQuestion is Some(false) and prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyUtilitiesAndServicesModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(employerProvidedServicesPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the first medical page when utilitiesAndServicesQuestion is Some(false) and no prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyUtilitiesAndServicesModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(employerProvidedServicesPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(MedicalDentalChildcareBenefitsController.show(taxYearEOY, employmentId).url)
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
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true, Some(benefits(
              fullUtilitiesAndServicesModel)))), userRequest)
            urlPost(employerProvidedServicesPageUrl(taxYearEOY), body = "", welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an BAD REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector)
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, formSelector)

          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(employerProvidedServicesPageUrl(taxYear), body = Map(YesNoForm.yesNo -> YesNoForm.no), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
      }

    }

    "redirect to the initial utilities page when utilitiesAndServicesQuestion is None" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyUtilitiesAndServicesModel.copy(utilitiesAndServicesQuestion = None))))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(employerProvidedServicesPageUrl(taxYearEOY), body = Map(YesNoForm.yesNo -> YesNoForm.no), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(UtilitiesOrGeneralServicesBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the check your benefits page when utilitiesAndServicesQuestion is Some(false) and prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyUtilitiesAndServicesModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(employerProvidedServicesPageUrl(taxYearEOY), body = Map(YesNoForm.yesNo -> YesNoForm.no), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "redirect to the first medical page when utilitiesAndServicesQuestion is Some(false) and no prior benefits exist" which {

      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(emptyUtilitiesAndServicesModel)))), userRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(employerProvidedServicesPageUrl(taxYearEOY), body = Map(YesNoForm.yesNo -> YesNoForm.no), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(MedicalDentalChildcareBenefitsController.show(taxYearEOY, employmentId).url)
      }

    }

    "update employerProvidedServicesQuestion to Some(true) when user chooses yes and has prior, redirects to the amount question" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullUtilitiesAndServicesModel.copy(employerProvidedServicesQuestion = Some(false)))))), userRequest)
        urlPost(employerProvidedServicesPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        //        TODO: This will go to the amount page when its implemented
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      s"updates the cyaModel to have the employerProvidedServicesQuestion to be true" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.utilitiesAndServicesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServicesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServices)) shouldBe Some(200.00)
      }

    }

    "update employerProvidedServicesQuestion to Some(false) and employerProvidedServices to None when user chooses no and has prior," +
      "redirects to the CYA page" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
          Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
        urlPost(employerProvidedServicesPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      s"updates the cyaModel to have the employerProvidedServicesQuestion to be false and the value to be None" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.utilitiesAndServicesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServicesQuestion)) shouldBe Some(false)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServices)) shouldBe None
      }

    }

    "adds an employerProvidedServicesQuestion to Some(true) when user chooses yes, redirects to the amount page when no prior" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
        urlPost(employerProvidedServicesPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        //        TODO: This will go to the amount page when its implemented
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      s"updates the cyaModel to have the employerProvidedServicesQuestion to be false and the value to be None" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.utilitiesAndServicesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServicesQuestion)) shouldBe Some(false)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServices)) shouldBe None
      }

    }

    "adds an employerProvidedServicesQuestion to Some(false) when user chooses no, redirects to the fees/subscriptions page when no prior" which {

      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
          Some(benefits(fullUtilitiesAndServicesModel)))), userRequest)
        urlPost(employerProvidedServicesPageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        //        TODO: This will go to the fees and subscriptions page when its implemented
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }

      s"updates the cyaModel to have the employerProvidedServicesQuestion to be false and the value to be None" in {
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.utilitiesAndServicesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServicesQuestion)) shouldBe Some(false)
        cyamodel.employment.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServices)) shouldBe None
      }

    }

  }

}
