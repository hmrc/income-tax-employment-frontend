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

package controllers.benefits.medical

import controllers.benefits.medical.routes._
import controllers.employment.routes._
import forms.AmountForm
import models.User
import models.benefits.{BenefitsViewModel, MedicalChildcareEducationModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class MedicalOrDentalBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId = "001"

  val insuranceAmount: BigDecimal = 100
  val newAmount: BigDecimal = 250
  val maxLimit: String = "100,000,000,000"
  val poundPrefixText = "£"
  val amountField = "#amount"
  val amountFieldName = "amount"


  def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/medical-dental-amount?employmentId=$employmentId"

  def continueLink(taxYear: Int) = s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/medical-dental-amount?employmentId=$employmentId"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId,
      isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(medicalChildcareModel: MedicalChildcareEducationModel): BenefitsViewModel =
    BenefitsViewModel(carVanFuelModel = Some(fullCarVanFuelModel), accommodationRelocationModel = Some(fullAccommodationRelocationModel),
      travelEntertainmentModel = Some(fullTravelOrEntertainmentModel), utilitiesAndServicesModel = Some(fullUtilitiesAndServicesModel),
      isUsingCustomerData = true, isBenefitsReceived = true, medicalChildcareEducationModel = Some(medicalChildcareModel))

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"
    val hintTextSelector = "#amount-hint"
    val amountFieldSelector = "#amount"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val poundPrefixSelector = ".govuk-input__prefix"
  }


  trait CommonExpectedResults  {
    def expectedCaption(taxYear: Int): String
    val continueButtonText: String
    val hintText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    def expectedPreAmountParagraph(amount: BigDecimal): String
    val expectedParagraph: String
    val expectedParagraphforForm: String
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedWrongFormatErrorMessage: String
    val expectedMaxErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £600 or £193.54"
    val continueButtonText = "Continue"
  }
  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £600 or £193.54"
    val continueButtonText = "Continue"
  }
  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your medical or dental benefit in total?"
    val expectedHeading = "How much was your medical or dental benefit in total?"
    def expectedPreAmountParagraph(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedParagraph = "This is the total sum of medical or dental insurance your employer paid for."
    val expectedParagraphforForm = "You can find this information on your P11D form in section I, box 11."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your medical or dental benefit amount"
    val expectedWrongFormatErrorMessage = "Enter your medical or dental benefit amount in the correct format"
    val expectedMaxErrorMessage = "Your medical or dental benefit must be less than £100,000,000,000"
  }
  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much was your medical or dental benefit in total?"
    val expectedHeading = "How much was your medical or dental benefit in total?"
    def expectedPreAmountParagraph(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedParagraph = "This is the total sum of medical or dental insurance your employer paid for."
    val expectedParagraphforForm = "You can find this information on your P11D form in section I, box 11."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your medical or dental benefit amount"
    val expectedWrongFormatErrorMessage = "Enter your medical or dental benefit amount in the correct format"
    val expectedMaxErrorMessage = "Your medical or dental benefit must be less than £100,000,000,000"
  }
  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s medical or dental benefit in total?"
    val expectedHeading = "How much was your client’s medical or dental benefit in total?"
    def expectedPreAmountParagraph(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedParagraph = "This is the total sum of medical or dental insurance your client’s employer paid for."
    val expectedParagraphforForm = "You can find this information on your client’s P11D form in section I, box 11."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your client’s medical or dental benefit amount"
    val expectedWrongFormatErrorMessage = "Enter your client’s medical or dental benefit amount in the correct format"
    val expectedMaxErrorMessage = "Your client’s medical or dental benefit must be less than £100,000,000,000"
  }
  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s medical or dental benefit in total?"
    val expectedHeading = "How much was your client’s medical or dental benefit in total?"
    def expectedPreAmountParagraph(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."
    val expectedParagraph = "This is the total sum of medical or dental insurance your client’s employer paid for."
    val expectedParagraphforForm = "You can find this information on your client’s P11D form in section I, box 11."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your client’s medical or dental benefit amount"
    val expectedWrongFormatErrorMessage = "Enter your client’s medical or dental benefit amount in the correct format"
    val expectedMaxErrorMessage = "Your client’s medical or dental benefit must be less than £100,000,000,000"
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
        "render the medical or dental benefits amount page with no pre-filled amount" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(
              _.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(medicalInsurance = None))))
            ))))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsurance = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphforForm, paragraphTextSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("", amountField)
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the medical or dental benefits amount page with pre-filled cya data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPreAmountParagraph(insuranceAmount), paragraphTextSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphforForm, paragraphTextSelector(4))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("100", amountField)
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the medical or dental benefits amount page with prior submitted data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPreAmountParagraph(insuranceAmount), paragraphTextSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphforForm, paragraphTextSelector(4))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("100", amountField)
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {

      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))

      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(url(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "Redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(
            fullBenefits.map(_.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(telephone = None))))))))), nino, taxYear)
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlGet(url(taxYear), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
        }
      }

      "redirect to the medical insurance question page when there is a medical insurance amount but no medicalInsuranceQuestion" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None))))), userRequest)
          urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe
            Some(MedicalDentalBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect to the check employment benefits page when benefits has medicalInsuranceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = Some(false), medicalInsurance = None))))), userRequest)
          urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect the user to the medical insurance question page when there's no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None, medicalInsurance = None))))), userRequest)
          urlGet(url(taxYearEOY), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(MedicalDentalBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }
    }
  }

  ".submit" should {

    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when the medical or dental benefits amount is too large" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> "2353453425345234")

          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(
              _.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(medicalInsurance = None))))
            ))))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsurance = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYearEOY), form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphforForm, paragraphTextSelector(3))
          errorAboveElementCheck(user.specificExpectedResults.get.expectedMaxErrorMessage, Some(amountFieldName))
          errorSummaryCheck(user.specificExpectedResults.get.expectedMaxErrorMessage, amountField)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("2353453425345234", amountField)
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "return an error when the medical or dental benefits amount is in the wrong format" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> "AAAA")

          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(
              _.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(medicalInsurance = None))))
            ))))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsurance = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYearEOY), form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphforForm, paragraphTextSelector(3))
          errorAboveElementCheck(user.specificExpectedResults.get.expectedWrongFormatErrorMessage, Some(amountFieldName))
          errorSummaryCheck(user.specificExpectedResults.get.expectedWrongFormatErrorMessage, amountField)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("AAAA", amountField)
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "return an error when no medical or dental benefits amount is submitted" which {

          lazy val form: Map[String, String] = Map(AmountForm.amount -> "")

          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits.map(
              _.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(medicalInsurance = None))))
            ))))), nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsurance = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYearEOY), form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphTextSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphforForm, paragraphTextSelector(3))
          errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, Some(amountFieldName))
          errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, amountField)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck("", amountField)
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }

      }
    }
    "redirect to another page when request is valid" should {

      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))

      lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      "Redirect to child care page when all is valid in previous data flow" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(url(taxYearEOY), form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ChildcareBenefitsController.show(taxYearEOY, employmentId).url)
          lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsuranceQuestion)) shouldBe Some(true)
          cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance)) shouldBe Some(newAmount)
        }

      }

      "Redirect to child care page when all is valid in new data flow" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(url(taxYearEOY), form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ChildcareBenefitsController.show(taxYearEOY, employmentId).url)
          lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
          cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsuranceQuestion)) shouldBe Some(true)
          cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance)) shouldBe Some(newAmount)
        }

      }
    }
    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {

      val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))

      lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())


      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(url(taxYearEOY), form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "Redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(
            fullBenefits.map(_.copy(benefits = fullBenefits.flatMap(_.benefits.map(_.copy(telephone = None))))))))), nino, taxYearEOY)
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
          authoriseAgentOrIndividual(user.isAgent)
          urlPost(url(taxYear), form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
        }
      }

      "redirect to the medical insurance question page when there is a medical insurance amount but no medicalInsuranceQuestion" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = false, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None))))), userRequest)
          urlPost(url(taxYearEOY), form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(MedicalDentalBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect to the check employment benefits page when benefits has medicalInsuranceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = Some(false), medicalInsurance = None))))), userRequest)
          urlPost(url(taxYearEOY), form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect the user to the medical insurance question page when there's no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(user.isAgent)
          dropEmploymentDB()
          insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true,
            Some(benefits(fullMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None, medicalInsurance = None))))), userRequest)
          urlPost(url(taxYearEOY), form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(MedicalDentalBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }
    }
  }

}
