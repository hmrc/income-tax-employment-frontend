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

package controllers.benefits.assets

import forms.AmountForm
import models.benefits.AssetsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.AssetsModelBuilder.anAssetsModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{assetsToKeepBenefitsAmountUrl, checkYourBenefitsUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class AssetsTransfersBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"
  private val amount: BigDecimal = 200
  private val amountFieldName = "amount"
  private val expectedErrorHref = "#amount"

  object Selectors {
    val hintTextSelector = "#amount-hint"
    val currencyPrefixSelector = "#main-content > div > div > form > div > div.govuk-input__wrapper > div"
    val amountFieldSelector = "#amount"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val youCanSelector = "#you-can-text"
    val enterTotalSelector = "#enter-total-text"
    val previousAmountSelector = "#previous-amount-text"
  }

  trait CommonExpectedResults {
    val expectedCaption: String

    def optionalParagraphText(amount: BigDecimal): String

    val expectedHintText: String
    val currencyPrefix: String
    val continueButtonText: String
    val enterTotalText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedYouCanText: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedErrorIncorrectFormat: String
    val expectedErrorOverMaximum: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"

    def optionalParagraphText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
    val enterTotalText = "Enter the total."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"

    def optionalParagraphText(amount: BigDecimal): String = s"If it was not £$amount, tell us the correct amount."

    val expectedHintText = "For example, £193.52"
    val currencyPrefix = "£"
    val continueButtonText = "Continue"
    val enterTotalText = "Enter the total."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much were the assets your employer gave you to keep?"
    val expectedHeading = "How much were the assets your employer gave you to keep?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedYouCanText: String = "You can find this information on your P11D form in section A, box 13."
    val expectedErrorNoEntry = "Enter the amount for assets your employer gave you to keep"
    val expectedErrorIncorrectFormat = "Enter the amount for assets your employer gave you to keep in the correct format"
    val expectedErrorOverMaximum = "The total amount for assets your employer gave you to keep must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much were the assets your employer gave you to keep?"
    val expectedHeading = "How much were the assets your employer gave you to keep?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedYouCanText: String = "You can find this information on your P11D form in section A, box 13."
    val expectedErrorNoEntry = "Enter the amount for assets your employer gave you to keep"
    val expectedErrorIncorrectFormat = "Enter the amount for assets your employer gave you to keep in the correct format"
    val expectedErrorOverMaximum = "The total amount for assets your employer gave you to keep must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much were the assets your client’s employer gave them to keep?"
    val expectedHeading = "How much were the assets your client’s employer gave them to keep?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedYouCanText: String = "You can find this information on your client’s P11D form in section A, box 13."
    val expectedErrorNoEntry = "Enter the amount for assets your client’s employer gave them to keep"
    val expectedErrorIncorrectFormat = "Enter the amount for assets your client’s employer gave them to keep in the correct format"
    val expectedErrorOverMaximum = "The total amount for assets your client’s employer gave them to keep must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much were the assets your client’s employer gave them to keep?"
    val expectedHeading = "How much were the assets your client’s employer gave them to keep?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedYouCanText: String = "You can find this information on your client’s P11D form in section A, box 13."
    val expectedErrorNoEntry = "Enter the amount for assets your client’s employer gave them to keep"
    val expectedErrorIncorrectFormat = "Enter the amount for assets your client’s employer gave them to keep in the correct format"
    val expectedErrorOverMaximum = "The total amount for assets your client’s employer gave them to keep must be less than £100,000,000,000"
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
        "render the assets amount page with no prefilled data" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransfer = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlGet(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          elementsNotOnPageCheck(previousAmountSelector)
          textOnPageCheck(enterTotalText, enterTotalSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanText, youCanSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the assets amount page with a pre-filled amount field when there's cya data and prior benefits" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserData)
            urlGet(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(optionalParagraphText(amount), previousAmountSelector)
          textOnPageCheck(enterTotalText, enterTotalSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanText, youCanSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, amount.toString())
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the assets amount page with a pre-filled amount field when there's cya data and no prior benefits" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
            urlGet(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(optionalParagraphText(amount), previousAmountSelector)
          textOnPageCheck(enterTotalText, enterTotalSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanText, youCanSelector)
          textOnPageCheck(expectedHintText, hintTextSelector)
          textOnPageCheck(currencyPrefix, currencyPrefixSelector)
          inputFieldValueCheck(amountFieldName, amountFieldSelector, amount.toString())
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the overview page when the tax year isn't end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlGet(fullUrl(assetsToKeepBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check your benefits page when there is no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the assets transfer question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransfer = None, assetTransferQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the assets section question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(sectionQuestion = Some(false), assetsQuestion = None,
          assets = None, assetTransferQuestion = None, assetTransfer = None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the benefits received question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(isBenefitsReceived = false)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the assets amount page with an error" when {
          "a form is submitted with no entry" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserData)
              urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(optionalParagraphText(amount), previousAmountSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedYouCanText, youCanSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldValueCheck(amountFieldName, amountFieldSelector, "")
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Some(amountFieldName))

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted with an incorrectly formatted amount" which {
            val incorrectFormatAmount = "abc"
            val form: Map[String, String] = Map(AmountForm.amount -> incorrectFormatAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserData)
              urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(optionalParagraphText(amount), previousAmountSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedYouCanText, youCanSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldValueCheck(amountFieldName, amountFieldSelector, incorrectFormatAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorIncorrectFormat, Some(amountFieldName))

            welshToggleCheck(user.isWelsh)
          }

          "a form is submitted and the amount is over the maximum limit" which {
            val overMaximumAmount = "100,000,000,000,000,000,000"
            val form: Map[String, String] = Map(AmountForm.amount -> overMaximumAmount)

            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              dropEmploymentDB()
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(anEmploymentUserData)
              urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has an BAD REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(optionalParagraphText(amount), previousAmountSelector)
            textOnPageCheck(enterTotalText, enterTotalSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedYouCanText, youCanSelector)
            textOnPageCheck(expectedHintText, hintTextSelector)
            textOnPageCheck(currencyPrefix, currencyPrefixSelector)
            inputFieldValueCheck(amountFieldName, amountFieldSelector, overMaximumAmount)
            buttonCheck(continueButtonText, continueButtonSelector)
            formPostLinkCheck(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, expectedErrorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorOverMaximum, Some(amountFieldName))

            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "update the assets value when a user submits a valid form and has prior benefits, redirects to CYA" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to check your benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update the asset transfers value to the new amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assetTransfer)) shouldBe Some(newAmount)
      }
    }

    "update the assets value when a user submits a valid form and has no prior benefits, redirects to CYA page" which {
      val newAmount: BigDecimal = 123.45
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val assetsModel = AssetsModel(sectionQuestion = Some(true), assetsQuestion = Some(false), assetTransferQuestion = Some(true))
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(assetsModel))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the CYA page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update the assets value to the new amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.assetTransfer)) shouldBe Some(newAmount)
      }
    }

    "redirect to the overview page when the tax year isn't end of year" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check your benefits page when there is no cya data found" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the assets transfer question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(assetTransferQuestion = Some(false))))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the assets section question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(sectionQuestion = Some(false), None, None, None, None)))
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the check your benefits page when the benefits received question is set to false" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val benefitsViewModel = aBenefitsViewModel.copy(isBenefitsReceived = false)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(assetsToKeepBenefitsAmountUrl(taxYearEOY, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}