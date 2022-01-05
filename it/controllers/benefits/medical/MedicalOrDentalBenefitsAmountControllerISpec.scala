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

package controllers.benefits.medical

import builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.models.UserBuilder.aUserRequest
import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.benefits.MedicalChildcareEducationModelBuilder.aMedicalChildcareEducationModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import controllers.benefits.medical.routes._
import controllers.employment.routes._
import forms.AmountForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class MedicalOrDentalBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val employmentId = "employmentId"
  private val insuranceAmount: BigDecimal = 100
  private val newAmount: BigDecimal = 250
  private val poundPrefixText = "£"
  private val amountField = "#amount"
  private val amountInputName = "amount"

  private def url(taxYear: Int): String = s"$appUrl/$taxYear/benefits/medical-dental-amount?employmentId=$employmentId"

  private def continueLink(taxYear: Int) = s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/medical-dental-amount?employmentId=$employmentId"

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"

    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"

    val hintTextSelector = "#amount-hint"
    val inputSelector = "#amount"
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val poundPrefixSelector = ".govuk-input__prefix"
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val continueButtonText: String
    val hintText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String

    def expectedPreAmountParagraph(amount: BigDecimal): String

    val expectedParagraph: String
    val expectedParagraphForForm: String
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
    val expectedParagraphForForm = "You can find this information on your P11D form in section I, box 11."
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
    val expectedParagraphForForm = "You can find this information on your P11D form in section I, box 11."
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
    val expectedParagraphForForm = "You can find this information on your client’s P11D form in section I, box 11."
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
    val expectedParagraphForForm = "You can find this information on your client’s P11D form in section I, box 11."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter your client’s medical or dental benefit amount"
    val expectedWrongFormatErrorMessage = "Enter your client’s medical or dental benefit amount in the correct format"
    val expectedMaxErrorMessage = "Your client’s medical or dental benefit must be less than £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the medical or dental benefits amount page with no pre-filled amount" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsurance = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the medical or dental benefits amount page with pre-filled cya data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserData, aUserRequest)
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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(4))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "100")
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the medical or dental benefits amount page with prior submitted data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserData, aUserRequest)
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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(4))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "100")
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "Redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(url(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
        }
      }

      "redirect to the medical insurance question page when there is a medical insurance amount but no medicalInsuranceQuestion" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
          urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(MedicalDentalBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect to the check employment benefits page when benefits has medicalInsuranceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = Some(false), medicalInsurance = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect the user to the medical insurance question page when there's no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None, medicalInsurance = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsurance = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(3))
          errorAboveElementCheck(user.specificExpectedResults.get.expectedMaxErrorMessage, Some(amountInputName))
          errorSummaryCheck(user.specificExpectedResults.get.expectedMaxErrorMessage, amountField)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "2353453425345234")
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when the medical or dental benefits amount is in the wrong format" which {
          lazy val form: Map[String, String] = Map(AmountForm.amount -> "abc")
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsurance = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(3))
          errorAboveElementCheck(user.specificExpectedResults.get.expectedWrongFormatErrorMessage, Some(amountInputName))
          errorSummaryCheck(user.specificExpectedResults.get.expectedWrongFormatErrorMessage, amountField)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "abc")
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when no medical or dental benefits amount is submitted" which {
          lazy val form: Map[String, String] = Map(AmountForm.amount -> "")
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsurance = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphForForm, paragraphTextSelector(3))
          errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, Some(amountInputName))
          errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, amountField)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          formPostLinkCheck(continueLink(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when request is valid" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      "Redirect to child care page when all is valid in previous data flow" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url(taxYearEOY), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ChildcareBenefitsController.show(taxYearEOY, employmentId).url)
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsuranceQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance)) shouldBe Some(newAmount)
        }
      }

      "Redirect to child care page when all is valid in new data flow" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url(taxYearEOY), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ChildcareBenefitsController.show(taxYearEOY, employmentId).url)
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, aUserRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsuranceQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance)) shouldBe Some(newAmount)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      "Redirect user to the check your benefits page with no cya data in session" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url(taxYearEOY), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "Redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url(taxYear), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
        }
      }

      "redirect to the medical insurance question page when there is a medical insurance amount but no medicalInsuranceQuestion" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false), aUserRequest)
          urlPost(url(taxYearEOY), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(MedicalDentalBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect to the check employment benefits page when benefits has medicalInsuranceQuestion set to false and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = Some(false), medicalInsurance = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          urlPost(url(taxYearEOY), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }

      "redirect the user to the medical insurance question page when there's no benefits and prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(medicalInsuranceQuestion = None, medicalInsurance = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel), aUserRequest)
          urlPost(url(taxYearEOY), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(MedicalDentalBenefitsController.show(taxYearEOY, employmentId).url)
        }
      }
    }
  }
}
