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

package controllers.benefits.fuel

import forms.AmountForm
import models.IncomeTaxUserData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{accommodationRelocationBenefitsUrl, carBenefitsUrl, carFuelBenefitsUrl, checkYourBenefitsUrl, fullUrl, mileageBenefitsAmountUrl, mileageBenefitsUrl, overviewUrl, vanBenefitsUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class MileageBenefitAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  object Selectors {
    val formSelector = "#main-content > div > div > form"
    val continueButtonSelector = "#continue"
    val contentSelector = "#main-content > div > div > p"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val continueButtonText: String
    val hintText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraph: String
    val expectedParagraphWithPrefill: String
    val expectedErrorTitle: String
    val expectedNoEntryErrorMessage: String
    val expectedWrongFormatErrorMessage: String
    val expectedMaxErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val hintText = "For example, £193.52"
    val continueButtonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val hintText = "Er enghraifft, £193.52"
    val continueButtonText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much mileage benefit did you get in total for using your own car?"
    val expectedHeading = "How much mileage benefit did you get in total for using your own car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of mileage benefit you got for using your own car"
    val expectedParagraph: String = "You can find this information on your P11D form in section E, box 12."
    val expectedParagraphWithPrefill: String = "If it was not £500.0, tell us the correct amount. You can find this information on your P11D form in section E, box 12."
    val expectedWrongFormatErrorMessage: String = "Enter the amount of mileage benefit you got in the correct format"
    val expectedMaxErrorMessage: String = "Your mileage benefit must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o fuddiant milltiroedd a gawsoch i gyd am ddefnyddio eich car eich hun?"
    val expectedHeading = "Faint o fuddiant milltiroedd a gawsoch i gyd am ddefnyddio eich car eich hun?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm y buddiant milltiroedd a gawsoch am ddefnyddio eich car eich hun"
    val expectedParagraph: String = "Maeír wybodaeth hon ar gael yn adran E, blwch 12 ar eich ffurflen P11D."
    val expectedParagraphWithPrefill: String = "Rhowch wybod y swm cywir os nad oedd yn £500.0. Maeír wybodaeth hon ar gael yn adran E, blwch 12 ar eich ffurflen P11D."
    val expectedWrongFormatErrorMessage: String = "Nodwch swm y buddiant milltiroedd a gawsoch chi yn y fformat cywir"
    val expectedMaxErrorMessage: String = "Maeín rhaid iích buddiant milltiroedd fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much mileage benefit did your client get in total for using their own car?"
    val expectedHeading = "How much mileage benefit did your client get in total for using their own car?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedNoEntryErrorMessage = "Enter the amount of mileage benefit your client got for using their own car"
    val expectedParagraph: String = "You can find this information on your client’s P11D form in section E, box 12."
    val expectedParagraphWithPrefill: String = "If it was not £500.0, tell us the correct amount. You can find this information on your client’s P11D form in section E, box 12."
    val expectedWrongFormatErrorMessage: String = "Enter the amount of mileage benefit your client got in the correct format"
    val expectedMaxErrorMessage: String = "Your client’s mileage benefit must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o fuddiant milltiroedd a gafodd eich cleient i gyd am ddefnyddio ei gar ei hun?"
    val expectedHeading = "Faint o fuddiant milltiroedd a gafodd eich cleient i gyd am ddefnyddio ei gar ei hun?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedNoEntryErrorMessage = "Nodwch swm y buddiant milltiroedd a gafodd eich cleient am ddefnyddio ei gar ei hun"
    val expectedParagraph: String = "Maeír wybodaeth hon ar gael yn adran E, blwch 12 ar ffurflen P11D eich cleient."
    val expectedParagraphWithPrefill: String = "Rhowch wybod y swm cywir os nad oedd yn £500.0. Maeír wybodaeth hon ar gael yn adran E, blwch 12 ar ffurflen P11D eich cleient."
    val expectedWrongFormatErrorMessage: String = "Nodwch swm y buddiant milltiroedd a gafodd eich cleient yn y fformat cywir"
    val expectedMaxErrorMessage: String = "Maeín rhaid i fuddiant milltiroedd eich cleient fod yn llai na £100,000,000,000"
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
        "render the mileage amount page with no pre-filled amount" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileage = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, contentSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, Selectors.inputSelector, "")
          formPostLinkCheck(mileageBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the mileage amount page with amount pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserData)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          import Selectors._
          import user.commonExpectedResults._

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          buttonCheck(user.commonExpectedResults.continueButtonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphWithPrefill, contentSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, Selectors.inputSelector, "500")
          formPostLinkCheck(mileageBenefitsAmountUrl(taxYearEOY, employmentId), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect" when {
      "redirect to accommodation relocation page when the mileage question is no" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileageQuestion = Some(false))))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

      }
      "mileage question is empty" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileageQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(mileageBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "car section isn't finished" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(carBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "van section isn't finished" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(vanBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }

      }
      "car fuel section isn't finished" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carFuelQuestion = None)))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(carFuelBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "there is no data in session for that user" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYearEOY)
          userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "it is not EOY" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          noUserDataStub(nino, taxYear)
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(mileageBenefitsAmountUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), follow = false)
        }

        s"has an SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error where there is no entry" which {
          val form: Map[String, String] = Map[String, String]()
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage, Selectors.inputSelector)
          inputFieldValueCheck(amountInputName, Selectors.inputSelector, "")
          errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryErrorMessage)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when it is the wrong format" which {
          lazy val form: Map[String, String] = Map(AmountForm.amount -> "abc")
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          errorSummaryCheck(user.specificExpectedResults.get.expectedWrongFormatErrorMessage, Selectors.inputSelector)
          inputFieldValueCheck(amountInputName, Selectors.inputSelector, "abc")
          errorAboveElementCheck(user.specificExpectedResults.get.expectedWrongFormatErrorMessage)

          welshToggleCheck(user.isWelsh)
        }

        "return an error when the value is too large" which {
          lazy val form: Map[String, String] = Map(AmountForm.amount -> "2353453425345234")
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          errorSummaryCheck(user.specificExpectedResults.get.expectedMaxErrorMessage, Selectors.inputSelector)
          inputFieldValueCheck(amountInputName, Selectors.inputSelector, "2353453425345234")
          errorAboveElementCheck(user.specificExpectedResults.get.expectedMaxErrorMessage)

          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect" when {
      "there is no cya data in session for that user" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel) shouldBe None
        }
      }

      "redirect to accommodation relocation page when the mileage question is no" which {
        lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")
        lazy val result: WSResponse = {
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileageQuestion = Some(false))))
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "it isn't end of year" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(mileageBenefitsAmountUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }
    }

    "update mileage amount to 200 when the user submits and prior benefits exist, redirects to accommodation page" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the accommodation page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the mileage benefit to be 200" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe Some(200.00)
      }
    }

    "update mileage amount to 200 when the user submits and no prior benefits exist, redirects to the accommodation relocation page" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> "200.00")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val benefitsViewModel = aBenefitsViewModel.copy(accommodationRelocationModel = None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the accommodation relocation controller page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates the mileage benefit to be 200" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe Some(200.00)
      }
    }
  }
}