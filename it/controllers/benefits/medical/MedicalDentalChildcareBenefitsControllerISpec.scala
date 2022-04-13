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

import forms.YesNoForm
import models.benefits.MedicalChildcareEducationModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.MedicalChildcareEducationModelBuilder.aMedicalChildcareEducationModel
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import utils.PageUrls.{checkYourBenefitsUrl, companyBenefitsUrl, fullUrl, incomeTaxOrIncurredCostsBenefitsUrl, medicalDentalBenefitsUrl, medicalDentalChildcareLoansBenefitsUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class MedicalDentalChildcareBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "employmentId"

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
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
    val expectedTitle = "Did you get any medical, dental, childcare, education benefits or loans from this company?"
    val expectedHeading = "Did you get any medical, dental, childcare, education benefits or loans from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you got medical, dental, childcare, education benefits or loans"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get any medical, dental, childcare, education benefits or loans from this company?"
    val expectedHeading = "Did you get any medical, dental, childcare, education benefits or loans from this company?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Select yes if you got medical, dental, childcare, education benefits or loans"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any medical, dental, childcare, education benefits or loans from this company?"
    val expectedHeading = "Did your client get any medical, dental, childcare, education benefits or loans from this company?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client got medical, dental, childcare, education benefits or loans"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get any medical, dental, childcare, education benefits or loans from this company?"
    val expectedHeading = "Did your client get any medical, dental, childcare, education benefits or loans from this company?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Select yes if your client got medical, dental, childcare, education benefits or loans"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment benefits for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
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
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render 'Did you get any medical, dental, childcare, education benefits or loans from this company?' " +
          "page with the correct content with no pre-filling" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = aBenefitsViewModel.copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(sectionQuestion = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          radioButtonCheck(yesText, radioNumber = 1, checked = false)
          radioButtonCheck(noText, radioNumber = 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'Did you get any medical, dental, childcare, education benefits or loans from this company?' " +
          "page with the correct content with cya data and the yes value pre-filled" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(anEmploymentUserData)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to another page when the request is valid but they aren't allowed to view the page and" should {
      "redirect the user to the check employment benefits page when theres no benefits and prior submission" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          urlGet(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the benefits received page when theres no benefits and not prior submission" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false, employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          urlGet(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(companyBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        }
      }

      "redirect the user to the check employment benefits page when theres no session data for that user" which {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          urlGet(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          urlGet(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
  }

  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        s"return a BAD_REQUEST($BAD_REQUEST) status" when {
          "the value is empty" which {
            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(anEmploymentUserData)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false,
                welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText, Some("value"))
          }
        }
      }
    }

    "redirect to another page when a valid request is made and then" should {
      "redirect to income tax section, update the medicalChildcareEducationQuestion to no and wipe the medical, child care and" +
        " education data when the user chooses no" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel.copy(incomeTaxAndCostsModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the income tax section" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(incomeTaxOrIncurredCostsBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get

          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel).get shouldBe MedicalChildcareEducationModel(Some(false))
        }
      }

      "redirect to medical dental page and update the medicalChildcareEducationQuestion to yes when the user chooses yes" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          val benefitsViewModel = aBenefitsViewModel
            .copy(medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel.copy(sectionQuestion = Some(false))))
            .copy(incomeTaxAndCostsModel = None)
          insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "redirects to the medical dental page" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(medicalDentalBenefitsUrl(taxYearEOY, employmentId)) shouldBe true

          lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsuranceQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance)) shouldBe Some(100)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlacesQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlaces)) shouldBe Some(200)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.educationalServicesQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.educationalServices)) shouldBe Some(300)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoanQuestion)) shouldBe Some(true)
          cyaModel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoan)) shouldBe Some(400)
        }
      }

      "redirect the user to the overview page when it is in year" which {
        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

      "redirect to the check employment benefits page when theres no CYA data" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentBenefits = None)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(medicalDentalChildcareLoansBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
  }
}