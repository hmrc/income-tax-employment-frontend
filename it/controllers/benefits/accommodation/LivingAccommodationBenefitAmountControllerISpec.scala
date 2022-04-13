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

package controllers.benefits.accommodation

import forms.AmountForm
import models.benefits.AccommodationRelocationModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{accommodationRelocationBenefitsUrl, checkYourBenefitsUrl, fullUrl, livingAccommodationBenefitsAmountUrl, livingAccommodationBenefitsUrl, overviewUrl, qualifyingRelocationBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class LivingAccommodationBenefitAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val poundPrefixText = "£"
  private val amountInputName = "amount"
  private val employmentId = "employmentId"
  private val livingAccommodationBenefitAmount: Option[BigDecimal] = Some(123.45)

  object Selectors {
    def paragraphTextSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"

    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#amount"
    val inputAmountField = "#amount"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedContent: String
    val emptyErrorText: String
    val wrongFormatErrorText: String
    val maxAmountErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val hintText: String
    val optionalText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, £193.52"
    val optionalText = s"If it was not £${livingAccommodationBenefitAmount.get}, tell us the correct amount."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Yn eich blaen"
    val hintText = "Er enghraifft, £193.52"
    val optionalText = s"If it was not £${livingAccommodationBenefitAmount.get}, tell us the correct amount."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "How much was your total living accommodation benefit?"
    val expectedTitle: String = "How much was your total living accommodation benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your P11D form in section D, box 14."
    val emptyErrorText: String = "Enter your living accommodation benefit amount"
    val wrongFormatErrorText: String = "Enter your living accommodation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your living accommodation benefit amount must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "How much was your client’s total living accommodation benefit?"
    val expectedTitle: String = "How much was your client’s total living accommodation benefit?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedContent: String = "You can find this information on your client’s P11D form in section D, box 14."
    val emptyErrorText: String = "Enter your client’s living accommodation benefit amount"
    val wrongFormatErrorText: String = "Enter your client’s living accommodation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s living accommodation benefit amount must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "How much was your total living accommodation benefit?"
    val expectedTitle: String = "How much was your total living accommodation benefit?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedContent: String = "You can find this information on your P11D form in section D, box 14."
    val emptyErrorText: String = "Enter your living accommodation benefit amount"
    val wrongFormatErrorText: String = "Enter your living accommodation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your living accommodation benefit amount must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "How much was your client’s total living accommodation benefit?"
    val expectedTitle: String = "How much was your client’s total living accommodation benefit?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedContent: String = "You can find this information on your client’s P11D form in section D, box 14."
    val emptyErrorText: String = "Enter your client’s living accommodation benefit amount"
    val wrongFormatErrorText: String = "Enter your client’s living accommodation benefit amount in the correct format"
    val maxAmountErrorText: String = "Your client’s living accommodation benefit amount must be less than £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render How much was your total living accommodation benefit? page with no value when theres no cya data" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, paragraphTextSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputAmountField, "")

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render How much was your total living accommodation benefit? page with prefilling when there is cya data" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = livingAccommodationBenefitAmount)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(optionalText, paragraphTextSelector(2))
          textOnPageCheck(get.expectedContent, paragraphTextSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputAmountField, "123.45")

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render How much was your How much was your total living accommodation benefit? page with prefilling when there is cya data and no prior benefits" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = livingAccommodationBenefitAmount)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
            urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(optionalText, paragraphTextSelector(2))
          textOnPageCheck(get.expectedContent, paragraphTextSelector(3))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputAmountField, livingAccommodationBenefitAmount.get.toString())

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the accommodation or relocation page when accommodationRelocationQuestion is None" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when accommodationRelocationQuestion is Some(false) and prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the living accommodation page when accommodationQuestion is None and prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(livingAccommodationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when accommodationQuestion is Some(false) and prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the qualifying relocation page when accommodationQuestion is Some(false) and no prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(qualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }

  ".submit" when {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render the How much was your Living accommodation benefit? page with an error when theres no input" which {
          val errorAmount = ""
          val errorForm = Map(AmountForm.amount -> errorAmount)
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlPost(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = errorForm)
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, paragraphTextSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputAmountField, errorAmount)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(get.emptyErrorText)
        }

        "should render the How much was your total living accommodation benefit? page with an error when the amount is invalid" which {
          val errorAmount = "abc"
          val errorForm: Map[String, String] = Map(AmountForm.amount -> errorAmount)
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlPost(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = errorForm)
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, paragraphTextSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputAmountField, errorAmount)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(get.wrongFormatErrorText)
        }

        "should render the How much was your total living accommodation benefit? page with an error when the amount is too big" which {
          val errorAmount = "100,000,000,000"
          val errorForm: Map[String, String] = Map(AmountForm.amount -> errorAmount)
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlPost(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = errorForm)
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.expectedContent, paragraphTextSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputAmountField, errorAmount)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(get.maxAmountErrorText)
        }

        "redirect to qualifying relocation page and update the living accommodation amount when a valid form is submitted and prior benefits exist" when {
          val newAmount = 100
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val benefitsViewModel = aBenefitsViewModel
              .copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None)))
              .copy(travelEntertainmentModel = None)
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlPost(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> newAmount.toString))
          }

          "redirects to the check your benefits page" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(qualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
          }

          "updates the CYA model with the new value" in {
            lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
            val livingAccommodationAmount: Option[BigDecimal] = cyaModel.employment.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodation))
            livingAccommodationAmount shouldBe Some(newAmount)
          }
        }
      }
    }

    "redirect to the overview page when the tax year isn't valid for EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
      }

      s"has an SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the accommodation or relocation page when accommodationRelocationQuestion is None" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when accommodationRelocationQuestion is Some(false) and prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(AccommodationRelocationModel(sectionQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the living accommodation benefits page when accommodationQuestion is None and prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(livingAccommodationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when accommodationQuestion is Some(false) and prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the qualifying relocation page when accommodationQuestion is Some(false) and no prior benefits exist" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = Some(anAccommodationRelocationModel.copy(accommodation = None, accommodationQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(livingAccommodationBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(qualifyingRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
