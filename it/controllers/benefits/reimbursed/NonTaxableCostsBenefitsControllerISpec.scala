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

package controllers.benefits.reimbursed

import controllers.employment.routes.CheckYourBenefitsController
import forms.YesNoForm
import models.User
import models.benefits.{BenefitsViewModel, ReimbursedCostsVouchersAndNonCashModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class NonTaxableCostsBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId: String = "001"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId,
      isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(reimbursedCostsVouchersAndNonCashModel: ReimbursedCostsVouchersAndNonCashModel): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      isUsingCustomerData = true, isBenefitsReceived = true, medicalChildcareEducationModel = Some(fullMedicalChildcareEducationModel),
      incomeTaxAndCostsModel = Some(fullIncomeTaxAndCostsModel),
      reimbursedCostsVouchersAndNonCashModel = Some(reimbursedCostsVouchersAndNonCashModel))

  private def pageUrl(taxYear: Int) = s"$appUrl/$taxYear/benefits/non-taxable-costs?employmentId=$employmentId"

  val continueLink = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/benefits/non-taxable-costs?employmentId=$employmentId"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val paragraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val paragraph: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Were any of your non-taxable costs reimbursed by your employer?"
    val expectedHeading = "Were any of your non-taxable costs reimbursed by your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if any of your non-taxable costs were reimbursed by your employer"
    val paragraph = "This includes costs of equipment or supplies required to carry out your job."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Were any of your non-taxable costs reimbursed by your employer?"
    val expectedHeading = "Were any of your non-taxable costs reimbursed by your employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if any of your non-taxable costs were reimbursed by your employer"
    val paragraph = "This includes costs of equipment or supplies required to carry out your job."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Were any of your client’s non-taxable costs reimbursed by their employer?"
    val expectedHeading = "Were any of your client’s non-taxable costs reimbursed by their employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if any of your client’s non-taxable costs were reimbursed by their employer"
    val paragraph = "This includes costs of equipment or supplies required to carry out their job."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Were any of your client’s non-taxable costs reimbursed by their employer?"
    val expectedHeading = "Were any of your client’s non-taxable costs reimbursed by their employer?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if any of your client’s non-taxable costs were reimbursed by their employer"
    val paragraph = "This includes costs of equipment or supplies required to carry out their job."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
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

    import Selectors._

    userScenarios.foreach { user =>

      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render 'non taxable costs' page with the correct content with no pre-filling" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              benefits = Some(benefits(fullReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)


          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'non taxable costs' page with the correct content with yes pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              benefits = Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'non taxable costs' page with the correct content with no pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              benefits = Some(benefits(fullReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(false)))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(pageUrl(taxYearEOY), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraphSelector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {

      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

      "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            benefits = Some(BenefitsViewModel(isUsingCustomerData = true)))), userRequest)
          urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }
      }

      "redirect the user to the check employment benefits page when ReimbursedCostsVouchersAndNonCashQuestion is set to false" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            benefits = Some(benefits(emptyReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(pageUrl(taxYearEOY), user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }
      }

      "redirect the user to the check employment benefits page when theres no session data for that user" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(pageUrl(taxYearEOY), user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }
      }

      "redirect the user to the overview page when the request is in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            benefits = Some(BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true)))), userRequest)
          urlGet(pageUrl(taxYear), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(pageUrl(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }

        "doesn't create any benefits data" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits shouldBe None
        }
      }

    }

  }


  ".submit" should {

    import Selectors._

    userScenarios.foreach { user =>

      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "the value is empty" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
                Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(pageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraphSelector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText, Some("value"))
          }
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {

      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))

      "Update the non taxable costs question to no, and non taxable costs to none when the user chooses no, redirects to CYA if prior submission" which {

        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }

        "updates non taxable costs question to no and non taxable costs to none" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expensesQuestion)) shouldBe Some(false)
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expenses)) shouldBe None
        }

      }

      "Update the non taxable costs question to no, and non taxable costs to none when the user chooses no," +
        "redirects to next question if not prior submission" which {

        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
            Some(benefits(fullReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        //          TODO: This will need updating to the next page in the flow when the navigation is hooked up
        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }

        "updates non taxable costs question to no and non taxable costs to none" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expensesQuestion)) shouldBe Some(false)
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expenses)) shouldBe None
        }

      }

      "Update the non taxable costs question to yes, redirects to the non taxable costs amount page" which {

        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            Some(benefits(fullReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(false)))))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the non taxable costs amount page" in {
          result.status shouldBe SEE_OTHER
          //          TODO: This will be updated to the non taxable costs amount page when its created
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }

        "updates non taxable costs question to yes" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.expensesQuestion)) shouldBe Some(true)
        }

      }

      "redirect the user to the overview page when it is in year" which {
        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(pageUrl(taxYear), body = "", user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {

        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the check your details page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }

        "doesn't create any benefits data" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyaModel.employment.employmentBenefits shouldBe None
        }
      }

      "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {

        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            benefits = Some(BenefitsViewModel(isUsingCustomerData = true)))), userRequest)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))          }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }
      }

      "redirect the user to the check employment benefits page when reimbursed costs vouchers and non cash question is set to false" which {

        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
            benefits = Some(benefits(emptyReimbursedCostsVouchersAndNonCashModel)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY,employmentId).url)
        }
      }

    }
  }

}
