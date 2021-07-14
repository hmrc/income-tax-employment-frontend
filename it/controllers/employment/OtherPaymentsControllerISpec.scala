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

package controllers.employment

import common.UUID
import controllers.employment.routes.{CheckEmploymentDetailsController, OtherPaymentsController}
import forms.YesNoForm
import models.User
import models.employment.{EmploymentData, EmploymentSource, Pay}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherPaymentsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val validTaxYear2021 = 2021

  object Selectors {
    val valueHref = "#value"
    val headingSelector = "#main-content > div > div > header > h1"
    val captionSelector = ".govuk-caption-l"
    val detailsSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val formSelector = "#main-content > div > div > form"
    val formRadioButtonValueSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val detailsContent: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val continueButton: String
    val expectedCaption: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${validTaxYear2021 - 1} to 5 April $validTaxYear2021"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${validTaxYear2021 - 1} to 5 April $validTaxYear2021"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "Did you receive any payments that are not on your P60?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val detailsContent: String = "This includes any tips. (A small gift of money for a service you provided.)"
    val expectedErrorText = "Select yes if you received any payments that are not on your P60"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "Did your client receive any payments that are not on their P60?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val detailsContent: String = "This includes any tips. (A small gift of money for a service your client provided.)"
    val expectedErrorText = "Select yes if your client received any payments that are not on their P60"

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Did you receive any payments that are not on your P60?"
    val expectedTitle: String = "Did you receive any payments that are not on your P60?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val detailsContent: String = "This includes any tips. (A small gift of money for a service you provided.)"
    val expectedErrorText = "Select yes if you received any payments that are not on your P60"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String =  "Did your client receive any payments that are not on their P60?"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val detailsContent: String = "This includes any tips. (A small gift of money for a service your client provided.)"
    val expectedErrorText = "Select yes if your client received any payments that are not on their P60"
  }

  private val employmentId = UUID().randomUUID
  private def url(taxYear: Int) = s"$appUrl/$taxYear/payments-not-on-p60?employmentId=$employmentId"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)
  private def employmentUserData(employmentCyaModel: EmploymentCYAModel) =
    EmploymentUserData(sessionId, mtditid, nino, validTaxYear2021, employmentId, false, employmentCyaModel)

  val cyaWithTipQuestionAnsweredYes =
    EmploymentCYAModel(EmploymentDetails("employer1", tipsAndOtherPaymentsQuestion = Some(true), tipsAndOtherPayments = Some(1), currentDataIsHmrcHeld = false))
  val cyaWithTipQuestionAnsweredNo =
    EmploymentCYAModel(EmploymentDetails("employer1", tipsAndOtherPaymentsQuestion = Some(false), currentDataIsHmrcHeld = false))
  val cyaWithTipQuestionUnanswered =
    EmploymentCYAModel(EmploymentDetails("employer1", currentDataIsHmrcHeld = false))


  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      import user.commonExpectedResults._

      val specific = user.specificExpectedResults.get

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return question page" when {

          "employment cya data is present in mongo" which {
            val taxYear = validTaxYear2021
            implicit lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(cyaWithTipQuestionAnsweredYes), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(url(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(specific.expectedTitle)
            h1Check(specific.expectedH1)
            textOnPageCheck(specific.detailsContent, detailsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            buttonCheck(continueButton)
            formPostLinkCheck(s"/income-through-software/return/employment-income${OtherPaymentsController.submit(taxYear, employmentId).url}", formSelector)
            formRadioValueCheck(selected = true, formRadioButtonValueSelector)
          }

        }

        "redirect" when {
          "the request tax year in in the future (2023)" which {
            val  taxYear = 2023
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(url(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to Check Employment Details page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
            }
          }

          "the request tax year is in year (2022)" which {
            val  taxYear = 2022
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(url(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to Check Employment Details page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
            }
          }

          "employment cya data does not exist in mongo" which {
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(url(taxYear), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to Check Employment Details page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(CheckEmploymentDetailsController.show(taxYear, employmentId).url)
            }
          }
        }

        "returns authorization failure" when {
          "user is unauthorised" which {
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              unauthorisedAgentOrIndividual(user.isAgent)
              urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "has an UNAUTHORIZED(401) status" in {
              result.status shouldBe UNAUTHORIZED
            }
          }

        }

      }
    }
  }

  ".submit" when {
    import Selectors._

    val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

    userScenarios.foreach { user =>
      import user.commonExpectedResults._

      val specific = user.specificExpectedResults.get

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect" when {

          "radio button is YES and employment cya data is present in mongo" which {
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(cyaWithTipQuestionAnsweredYes), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYear), yesNoFormYes, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to Other Payments Amount page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some("/other-payments-p60-amount-TO-BE-DEFINED")
            }

            "cyaModel is updated and tipsAndOtherPaymentsQuestion is set to Some(true)" in {
              val cyaModel = findCyaData(taxYear, employmentId, userRequest).get
              cyaModel.employment.employmentDetails.tipsAndOtherPaymentsQuestion shouldBe Some(true)
            }
          }

          "radio button is NO and employment cya data is present in mongo" which {
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(cyaWithTipQuestionAnsweredYes), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYear), yesNoFormNo, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to Check Employment Details page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(CheckEmploymentDetailsController.show(taxYear, employmentId).url)
            }

            "cyaModel is updated and tipsAndOtherPaymentsQuestion is set to Some(false)" in {
              val cyaModel = findCyaData(taxYear, employmentId, userRequest).get
              cyaModel.employment.employmentDetails.tipsAndOtherPaymentsQuestion shouldBe Some(false)
            }

            "cyaModel is updated and tipsAndOtherPayments amount is set to None" in {
              val cyaModel = findCyaData(taxYear, employmentId, userRequest).get
              cyaModel.employment.employmentDetails.tipsAndOtherPayments shouldBe None
            }
          }

          "the request tax year in in the future (2023)" which {
            val taxYear = 2023
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYear), yesNoFormYes, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to Check Employment Details page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
            }
          }

          "the request tax year is in year (2022)" which {
            val taxYear = 2022
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYear), yesNoFormYes, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to Check Employment Details page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
            }
          }

          "employment cya data does not exist in mongo" which {
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYear), yesNoFormYes, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to Check Employment Details page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(CheckEmploymentDetailsController.show(taxYear, employmentId).url)
            }
          }
        }

        "return BAD_REQUEST and render correct errors" when {
          "the yes/no radio button has not been selected" which {
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(cyaWithTipQuestionUnanswered), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url(taxYear), yesNoFormEmpty, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            s"has an BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            welshToggleCheck(user.isWelsh)
            titleCheck(specific.expectedErrorTitle)
            h1Check(specific.expectedH1)
            errorSummaryCheck(specific.expectedErrorText, valueHref)
            errorAboveElementCheck(specific.expectedErrorText)

            textOnPageCheck(specific.detailsContent, detailsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            buttonCheck(continueButton)
            formPostLinkCheck(s"/income-through-software/return/employment-income${OtherPaymentsController.submit(taxYear, employmentId).url}", formSelector)
          }
        }

        "returns authorization failure" when {
          "user is unauthorised" which {
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              unauthorisedAgentOrIndividual(user.isAgent)
              urlPost(url(taxYear), yesNoFormYes, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "has an UNAUTHORIZED(401) status" in {
              result.status shouldBe UNAUTHORIZED
            }
          }

        }

      }
    }
  }
}
