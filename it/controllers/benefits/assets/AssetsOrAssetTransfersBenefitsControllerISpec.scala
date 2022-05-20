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

import forms.YesNoForm
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
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{assetsBenefitsUrl, assetsForUseBenefitsUrl, checkYourBenefitsUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class AssetsOrAssetTransfersBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  object Selectors {
    val paragraphSelector: Int => String = (index: Int) => s"#main-content > div > div > p.govuk-body:nth-child($index)"
    val bullet1Selector: String = "#main-content > div > div > ul.govuk-list > li:nth-child(1)"
    val bullet2Selector: String = "#main-content > div > div > ul.govuk-list > li:nth-child(2)"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedIncludesParagraph: String
    val expectedBullet2: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedDescriptionParagraph: String
    val expectedBullet1: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get any assets from this company?"
    val expectedHeading = "Did you get any assets from this company?"
    val expectedIncludesParagraph = "Include assets that your employer let you:"
    val expectedBullet2 = "keep for yourself"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got assets"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch unrhyw asedion gan y cwmni hwn?"
    val expectedHeading = "A gawsoch unrhyw asedion gan y cwmni hwn?"
    val expectedIncludesParagraph = "Dylech gynnwys asedion y gwnaeth eich cyflogwr eu rhoi dros dro i chi:"
    val expectedBullet2 = "iíw cadw eich hun"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ëIawní os cawsoch asedion"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any assets from this company?"
    val expectedHeading = "Did your client get any assets from this company?"
    val expectedIncludesParagraph = "Include assets that their employer let them:"
    val expectedBullet2 = "keep for themselves"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got assets"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient unrhyw asedion gan y cwmni hwn?"
    val expectedHeading = "A gafodd eich cleient unrhyw asedion gan y cwmni hwn?"
    val expectedIncludesParagraph = "Dylech gynnwys asedion y gwnaeth ei gyflogwr eu rhoi dros dro iddo:"
    val expectedBullet2 = "iíw cadw iíw hun"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ëIawní os cafodd eich cleient asedion"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedDescriptionParagraph = "Assets are things like computers, televisions or bicycles."
    val expectedBullet1 = "use"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedDescriptionParagraph = "Asedion yw pethau fel cyfrifiaduron, setiau teledu neu feiciau."
    val expectedBullet1 = "defnyddio"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    import Selectors._
    userScenarios.foreach { user =>
      import user.commonExpectedResults._
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render 'assets from company' page with the correct content with no pre-filling" which {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(sectionQuestion = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlGet(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedDescriptionParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedIncludesParagraph, paragraphSelector(3))
          textOnPageCheck(expectedBullet1, bullet1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bullet2Selector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(assetsBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'assets from company' page with the correct content with yes pre-filled" which {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(sectionQuestion = Some(true))))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlGet(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedDescriptionParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedIncludesParagraph, paragraphSelector(3))
          textOnPageCheck(expectedBullet1, bullet1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bullet2Selector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(assetsBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'assets from company' page with the correct content with no pre-filled" which {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = Some(anAssetsModel.copy(sectionQuestion = Some(false))))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            urlGet(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedDescriptionParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedIncludesParagraph, paragraphSelector(3))
          textOnPageCheck(expectedBullet1, bullet1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bullet2Selector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(assetsBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(isBenefitsReceived = false)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlGet(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the check employment benefits page when theres no session data for that user" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect the user to the overview page when the request is in year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData)
        urlGet(fullUrl(assetsBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check employment benefits page when theres no CYA data" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "doesn't create any benefits data" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits shouldBe None
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
            val form = Map(YesNoForm.yesNo -> "")
            lazy val result = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(anEmploymentUserData)
              urlPost(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            textOnPageCheck(expectedDescriptionParagraph, paragraphSelector(3))
            textOnPageCheck(user.specificExpectedResults.get.expectedIncludesParagraph, paragraphSelector(4))
            textOnPageCheck(expectedBullet1, bullet1Selector)
            textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bullet2Selector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(assetsBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText, Some("value"))
          }
        }
      }
    }

    "Update the assets section question to no, clear assetModel when the user chooses no and redirects to CYA when prior submission" which {
      val form = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "clears the assets model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.assetsModel) shouldBe Some(AssetsModel(sectionQuestion = Some(false)))
      }
    }

    "Update the assets section question to no, clear assetModel when the user chooses no and redirects to CYA when not prior submission" which {
      val form = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "clears the assets model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.assetsModel) shouldBe Some(AssetsModel(sectionQuestion = Some(false)))
      }
    }

    "Update assets section question to yes, redirects to the assets question page when user chooses yes" which {
      val form = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result = {
        dropEmploymentDB()
        val emptyAssets = Some(AssetsModel(sectionQuestion = Some(false)))
        val benefitsViewModel = aBenefitsViewModel.copy(assetsModel = emptyAssets)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the assets question page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(assetsForUseBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "updates vouchers or credit cards question to yes" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.assetsModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
      }
    }

    "redirect the user to the overview page when it is in year" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsBenefitsUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the check employment benefits page when theres no CYA data" which {
      val form = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check your details page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "doesn't create any benefits data" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits shouldBe None
      }
    }

    "redirect the user to the check employment benefits page when the benefitsReceived question is false" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val benefitsViewModel = aBenefitsViewModel.copy(isBenefitsReceived = false)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
        urlPost(fullUrl(assetsBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}
