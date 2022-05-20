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

import models.benefits.BenefitsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.MedicalChildcareEducationModelBuilder.aMedicalChildcareEducationModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{beneficialLoansBenefitsUrl, checkYourBenefitsUrl, educationalServicesBenefitsAmountUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EducationalServicesBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  object Selectors {
    val contentSelector = "#main-content > div > div > p"
    val hintTextSelector = "#amount-hint"
    val inputSelector = "#amount"
    val poundPrefixSelector = ".govuk-input__prefix"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#amount"
  }


  trait CommonExpectedResults {
    val expectedCaption: String
    val amountHint: String
    val continue: String
    val previousExpectedContent: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val amountHint: String = "For example, £193.52"
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Continue"
    val previousExpectedContent: String = "If it was not £300, tell us the correct amount."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "Er enghraifft, £193.52"
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue = "Yn eich blaen"
    val previousExpectedContent: String = "Rhowch wybod y swm cywir os nad oedd yn £300."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for educational services?"
    val expectedHeading: String = "How much did you get in total for educational services?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount you got for educational services"
    val invalidFormatErrorText: String = "Enter the amount you got for educational services in the correct format"
    val maxAmountErrorText: String = "Your educational services benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cawsoch i gydaddysg ar gyfer gwasanaethau addysg?"
    val expectedHeading: String = "Faint y cawsoch i gydaddysg ar gyfer gwasanaethau addysg?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch y swm a gawsoch ar gyfer gwasanaethau addysg"
    val invalidFormatErrorText: String = "Nodwch y swm a gawsoch ar gyfer gwasanaethau addysg yn y fformat cywir"
    val maxAmountErrorText: String = "Maeín rhaid iích buddiant ar gyfer gwasanaethau addysg fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for educational services?"
    val expectedHeading: String = "How much did your client get in total for educational services?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount your client got for educational services"
    val invalidFormatErrorText: String = "Enter the amount your client got for educational services in the correct format"
    val maxAmountErrorText: String = "Your client’s educational services benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cafodd eich cleient i gyd ar gyfer gwasanaethau addysg?"
    val expectedHeading: String = "Faint y cafodd eich cleient i gyd ar gyfer gwasanaethau addysg?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch y swm a gafodd eich cleient ar gyfer gwasanaethau addysg"
    val invalidFormatErrorText: String = "Nodwch y swm a gafodd eich cleient ar gyfer gwasanaethau addysg yn y fformat cywir"
    val maxAmountErrorText: String = "Maeín rhaid i fuddiant eich cleient ar gyfer gwasanaethau addysg fod yn llai na £100,000,000,000"
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
        "render the educational services amount page without pre-filled form" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(educationalServices = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document
          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          elementsNotOnPageCheck(contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the educational services amount page with pre-filled form and no prior submitted data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "300")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the educational services amount page with pre-filled form and prior submitted data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserData)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, contentSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "300")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "Redirect user to the check your benefits page with no cya data in session" in {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "Redirect user to the tax overview page when in year" in {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          val benefitsViewModel = BenefitsViewModel(isUsingCustomerData = true)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(educationalServicesBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }

      "Redirect to educational services question page when there is educational services amount but has no educationalServicesQuestion" in {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val medicalChildcareEducationModel = aMedicalChildcareEducationModel.copy(educationalServicesQuestion = None, educationalServices = Some(11.0))
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(medicalChildcareEducationModel))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          urlGet(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        // TODO: Should go to educational services question page
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render the amount page with empty value error text when there is no input" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData)
            urlPost(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(get.emptyErrorText)
        }

        "should render the amount page with invalid format text when input is in incorrect format" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData)
            urlPost(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "abc"))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "abc")
          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(get.invalidFormatErrorText)
        }

        "should render the amount page with max error when input > 99,999,999,999" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData)
            urlPost(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "100,000,000,000"))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption)
          textOnPageCheck(user.commonExpectedResults.previousExpectedContent, contentSelector)
          textOnPageCheck(user.commonExpectedResults.amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "100,000,000,000")
          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(get.maxAmountErrorText)
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {
      "redirect to beneficial loans page when a valid form is submitted and a prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          urlPost(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "123.45"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER

          result.header("location").contains(beneficialLoansBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "updates the CYA model with the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          val educationalServices = cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.educationalServices))
          educationalServices shouldBe Some(123.45)
        }
      }

      "redirect to to Beneficial loans yes/no page when a valid form is submitted and not prior submission" when {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          urlPost(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "234.56"))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(beneficialLoansBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

        "updates the CYA model with the new value" in {
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          val educationalServices = cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.educationalServices))
          educationalServices shouldBe Some(234.56)
        }
      }

      "redirect user to the check your benefits page with no cya data" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "345.67"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect user to the tax overview page when in year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertCyaData(anEmploymentUserData)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(educationalServicesBenefitsAmountUrl(taxYear, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.0"))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to educational services question page when there is educational services amount but has no educationalServicesQuestion" in {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropEmploymentDB()
          val medicalChildcareEducationModel = aMedicalChildcareEducationModel.copy(educationalServicesQuestion = None)
          val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(medicalChildcareEducationModel))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          urlPost(fullUrl(educationalServicesBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
        }

        result.status shouldBe SEE_OTHER
        // TODO: Should go to educational services question page
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
