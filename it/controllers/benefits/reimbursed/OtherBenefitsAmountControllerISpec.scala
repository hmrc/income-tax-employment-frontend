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

package controllers.benefits.reimbursed

import models.benefits.{BenefitsViewModel, ReimbursedCostsVouchersAndNonCashModel}
import models.mongo.EmploymentCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.ReimbursedCostsVouchersAndNonCashModelBuilder.aReimbursedCostsVouchersAndNonCashModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{assetsBenefitsUrl, checkYourBenefitsUrl, fullUrl, otherBenefitsAmountUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel) =
    anEmploymentUserData.copy(isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employment = employmentCyaModel)

  object Selectors {
    val previousAmountTextSelector = "#previous-amount-text"
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
    val maxAmountErrorText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val amountHint: String = "For example, £193.52"
    val expectedCaption: String = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue: String = "Continue"
    val previousExpectedContent: String = "If it was not £500, tell us the correct amount."
    val maxAmountErrorText: String = "The amount for other benefits must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val amountHint: String = "Er enghraifft, £193.52"
    val expectedCaption: String = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continue: String = "Yn eich blaen"
    val previousExpectedContent: String = "Rhowch wybod y swm cywir os nad oedd yn £500."
    val maxAmountErrorText: String = "Maeín rhaid iír swm a nodwyd ar gyfer buddiannau eraill fod yn llai na £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did you get in total for other benefits?"
    val expectedHeading: String = "How much did you get in total for other benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount you got for other benefits"
    val invalidFormatErrorText: String = "Enter the amount you got for other benefits in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cawsoch i gyd ar gyfer buddiannau eraill?"
    val expectedHeading: String = "Faint y cawsoch i gyd ar gyfer buddiannau eraill?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch y swm a gawsoch ar gyfer buddiannau eraill"
    val invalidFormatErrorText: String = "Nodwch y swm a gawsoch ar gyfer buddiannau eraill yn y fformat cywir"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "How much did your client get in total for other benefits?"
    val expectedHeading: String = "How much did your client get in total for other benefits?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val emptyErrorText: String = "Enter the amount your client got for other benefits"
    val invalidFormatErrorText: String = "Enter the amount your client got for other benefits in the correct format"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Faint y cafodd eich cleient i gyd ar gyfer buddiannau eraill?"
    val expectedHeading: String = "Faint y cafodd eich cleient i gyd ar gyfer buddiannau eraill?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val emptyErrorText: String = "Nodwch y swm a gafodd eich cleient ar gyfer buddiannau eraill"
    val invalidFormatErrorText: String = "Nodwch y swm a gafodd eich cleient ar gyfer buddiannau eraill yn y fformat cywir"
  }

  private val benefitsWithNoBenefitsReceived: Option[BenefitsViewModel] = Some(BenefitsViewModel(isUsingCustomerData = true))

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        import user.commonExpectedResults._
        "render the other benefits amount page without pre-filled form" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItems = None)))
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          elementsNotOnPageCheck(previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(otherBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the other benefits amount page with pre-filled form and no prior submitted data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "500")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(otherBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the other benefits amount page with pre-filled form and prior submitted data" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "500")
          buttonCheck(continue, continueButtonSelector)
          formPostLinkCheck(otherBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect user to the check your benefits page with no cya data in session" in {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }

    "redirect user to the tax overview page when in year" in {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = benefitsWithNoBenefitsReceived)))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(otherBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }

    "redirect to Assets section question page when the otherItemsQuestion value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(false))))
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(assetsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }

    "redirect to Assets section question page when the reimbursedCostsVouchersAndNonCashQuestion value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))))
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlGet(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(assetsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }

    "redirect to cya page when the benefitsReceived value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = benefitsWithNoBenefitsReceived)))
        urlGet(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        import user.commonExpectedResults._

        "should render the amount page with empty value error text when there is no input" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
            urlPost(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map[String, String]())
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(otherBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(user.specificExpectedResults.get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(user.specificExpectedResults.get.emptyErrorText)
        }

        "should render the amount page with invalid format text when input is in incorrect format" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
            urlPost(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "abc"))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "abc")
          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(otherBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(user.specificExpectedResults.get.invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(user.specificExpectedResults.get.invalidFormatErrorText)
        }

        "should render the amount page with max error when input > 99,999,999,999" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
            urlPost(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "100,000,000,000"))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(previousExpectedContent, previousAmountTextSelector)
          textOnPageCheck(amountHint, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "100,000,000,000")
          buttonCheck(user.commonExpectedResults.continue, continueButtonSelector)
          formPostLinkCheck(otherBenefitsAmountUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(maxAmountErrorText)
        }
      }
    }

    "redirect to check employment benefits page when a valid form is submitted and a prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        urlPost(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "123.45"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the CYA model with the new value" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        val otherBenefits = cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.otherItems))
        otherBenefits shouldBe Some(123.45)
      }
    }

    "redirect to the first assets section question page when a valid form is submitted and not prior submission" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = None)
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "234.56"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(assetsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the CYA model with the new value" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        val otherBenefits = cyaModel.employment.employmentBenefits.flatMap(_.reimbursedCostsVouchersAndNonCashModel.flatMap(_.otherItems))
        otherBenefits shouldBe Some(234.56)
      }
    }

    "redirect user to the check your benefits page with no cya data" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "345.67"))
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
        insertCyaData(employmentUserData(isPrior = true, anEmploymentCYAModel.copy(employmentBenefits = Some(aBenefitsViewModel))))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(otherBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100.50"))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to Assets section question page when the otherItemsQuestion value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(false))))
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "123.45"))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(assetsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }

    "redirect to Assets section question page when the reimbursedCostsVouchersAndNonCashQuestion value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(reimbursedCostsVouchersAndNonCashModel = Some(ReimbursedCostsVouchersAndNonCashModel(sectionQuestion = Some(false))))
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = Some(benefitsViewModel))))
        urlPost(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "123.45"))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(assetsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }

    "redirect to cya page when the benefitsReceived value is false" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, anEmploymentCYAModel.copy(employmentBenefits = benefitsWithNoBenefitsReceived)))
        urlPost(fullUrl(otherBenefitsAmountUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "123.45"))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }
  }
}
