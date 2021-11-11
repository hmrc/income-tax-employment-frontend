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

import controllers.employment.routes.CheckYourBenefitsController
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

class ChildcareBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1
  val employmentId: String = "001"
  val amount: BigDecimal = 200
  val amountFieldName = "amount"
  val expectedErrorHref = "#amount"

  def childcareBenefitsPageUrl(taxYear: Int): String =
    s"$appUrl/$taxYear/benefits/childcare-amount?employmentId=$employmentId"

  val formLink: String =
    s"/income-through-software/return/employment-income/$taxYearEOY/benefits/childcare-amount?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(hasPriorBenefits: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = hasPriorBenefits, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(medicalChildcareEducationModel: MedicalChildcareEducationModel): BenefitsViewModel =
    BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true, medicalChildcareEducationModel = Some(medicalChildcareEducationModel))

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > label > header > p"
    val optionalParagraphTextSelector = "#main-content > div > div > form > div > label > p"
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val amountFieldSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String

    def optionalParagraphText(amount: BigDecimal): String

    val expectedHintText: String
    val currencyPrefix: String
    val continueButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedErrorIncorrectFormat: String
    val expectedErrorOverMaximum: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"

    def optionalParagraphText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"

    def optionalParagraphText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val expectedHintText = "For example, £600 or £193.54"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your total childcare benefit?"
    val expectedHeading = "How much was your total childcare benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter your childcare benefit amount"
    val expectedErrorIncorrectFormat = "Enter your childcare benefit amount in the correct format"
    val expectedErrorOverMaximum = "Your childcare benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much was your total childcare benefit?"
    val expectedHeading = "How much was your total childcare benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter your childcare benefit amount"
    val expectedErrorIncorrectFormat = "Enter your childcare benefit amount in the correct format"
    val expectedErrorOverMaximum = "Your childcare benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s total childcare benefit?"
    val expectedHeading = "How much was your client’s total childcare benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter your client’s childcare benefit amount"
    val expectedErrorIncorrectFormat = "Enter your client’s childcare benefit amount in the correct format"
    val expectedErrorOverMaximum = "Your client’s childcare benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s total childcare benefit?"
    val expectedHeading = "How much was your client’s total childcare benefit?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter your client’s childcare benefit amount"
    val expectedErrorIncorrectFormat = "Enter your client’s childcare benefit amount in the correct format"
    val expectedErrorOverMaximum = "Your client’s childcare benefit must be less than £100,000,000,000"
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
        "render the childcare benefits amount page with an empty amount field" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel.copy(nurseryPlaces = None))))), userRequest)
            urlGet(childcareBenefitsPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          elementNotOnPageCheck(optionalParagraphTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldCheck(amountFieldName, amountFieldSelector)
          inputFieldValueCheck("", amountFieldSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(formLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the childcare benefits amount page with a pre-filled amount field when there's cya data and prior benefits" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
            insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
            urlGet(childcareBenefitsPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(optionalParagraphText(amount), optionalParagraphTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldCheck(amountFieldName, amountFieldSelector)
          inputFieldValueCheck(amount.toString(), amountFieldSelector)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(formLink, formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the childcare benefits amount page with a pre-filled amount field when there's cya data and no prior benefits" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
            urlGet(childcareBenefitsPageUrl(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(optionalParagraphText(amount), optionalParagraphTextSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          inputFieldCheck(amountFieldName, amountFieldSelector)
          inputFieldValueCheck(amount.toString(), amountFieldSelector)
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
          Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
        urlGet(childcareBenefitsPageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlGet(childcareBenefitsPageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        "return an error" when {
          "a form is submitted with no entry" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
                fullMedicalChildcareEducationModel)))), userRequest)
              urlPost(childcareBenefitsPageUrl(taxYearEOY), body = "", welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(optionalParagraphText(amount), optionalParagraphTextSelector)
            hintTextCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck("", amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Some(amountFieldName))
            formPostLinkCheck(formLink, formSelector)

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted with an incorrect format" which {
            val incorrectFormatAmount = "abc"
            val form: Map[String, String] = Map(AmountForm.amount -> incorrectFormatAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
                fullMedicalChildcareEducationModel)))), userRequest)
              urlPost(childcareBenefitsPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(optionalParagraphText(amount), optionalParagraphTextSelector)
            hintTextCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(incorrectFormatAmount, amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountFieldName))
            formPostLinkCheck(formLink, formSelector)

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted and the amount is over the maximum limit" which {
            val overMaximumAmount = "100,000,000,000,000,000,000"
            val form: Map[String, String] = Map(AmountForm.amount -> overMaximumAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true, Some(benefits(
                fullMedicalChildcareEducationModel)))), userRequest)
              urlPost(childcareBenefitsPageUrl(taxYearEOY), body = form, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(optionalParagraphText(amount), optionalParagraphTextSelector)
            hintTextCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldCheck(amountFieldName, amountFieldSelector)
            inputFieldValueCheck(overMaximumAmount, amountFieldSelector)
            buttonCheck(continueButtonText, continueButtonSelector)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountFieldName))
            formPostLinkCheck(formLink, formSelector)

            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "update cya when a user submits a valid form and has prior benefits" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
        urlPost(childcareBenefitsPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"update medicalChildcareEducationModel and redirect to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalChildcareEducationQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlacesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlaces)) shouldBe Some(newAmount)
      }
    }

    "update cya when a user submits a valid form and doesn't have prior benefits" which {
      val newAmount: BigDecimal = 500.55
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(hasPriorBenefits = false, cyaModel("name", hmrc = true,
          Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
        urlPost(childcareBenefitsPageUrl(taxYearEOY), follow = false, body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"update medicalChildcareEducationModel and redirect to educational services yes no page" in {
        result.status shouldBe SEE_OTHER
        //TODO - redirect to educational services yes no question
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
        lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
        cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalChildcareEducationQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlacesQuestion)) shouldBe Some(true)
        cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlaces)) shouldBe Some(newAmount)
      }
    }

    "redirect to the overview page when the tax year isn't end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
        insertCyaData(employmentUserData(hasPriorBenefits = true, cyaModel("name", hmrc = true,
          Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
        urlPost(childcareBenefitsPageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
        urlPost(childcareBenefitsPageUrl(taxYearEOY), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
      }
    }
  }

}