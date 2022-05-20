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

package controllers.employment

import models.employment.AllEmploymentData
import models.mongo.EmploymentUserData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentFinancialDataBuilder.aHmrcEmploymentFinancialData
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{checkYourDetailsUrl, fullUrl, howMuchTaxUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmploymentTaxControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"
  private val amountInputName = "amount"

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hint: String
    val continue: String
    val expectedPTextWithData: String
    val expectedErrorInvalidFormat: String
    val expectedErrorMaxLimit: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorTitle: String
    val expectedErrorNoEntry: String
    val expectedPTextNoData: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hint: String = "For example, £193.52"
    val continue: String = "Continue"
    val expectedPTextWithData: String = s"If £200 was not taken in UK tax, tell us the correct amount."
    val expectedErrorInvalidFormat = "Enter the amount of UK tax in the correct format"
    val expectedErrorMaxLimit = "The amount of UK tax must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment details for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hint: String = "Er enghraifft, £193.52"
    val continue: String = "Yn eich blaen"
    val expectedPTextWithData: String = s"Os na chafodd £200 ei thynnu fel treth y DU, rhowch wybod i ni beth ywír swm cywir."
    val expectedErrorInvalidFormat = "Nodwch y swm o dreth y DU yn y fformat cywir"
    val expectedErrorMaxLimit = "Maeín rhaid iír swm o dreth y DU fod yn llai na £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your earnings?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of UK tax taken from your earnings"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your P60."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = s"How much UK tax was taken from your client’s maggie earnings?"
    val expectedTitle: String = s"How much UK tax was taken from your client’s earnings?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorNoEntry = "Enter the amount of UK tax taken from your client’s earnings"
    val expectedPTextNoData: String = "You can usually find this amount in the ‘Pay and Income Tax details’ section of your client’s P60."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = s"Faint o dreth y DU a gafodd ei thynnu oích enillion maggie?"
    val expectedTitle: String = s"Faint o dreth y DU a gafodd ei thynnu oích enillion?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch y swm o dreth y DU a dynnwyd oích enillion"
    val expectedPTextNoData: String = "Fel arfer, maeír swm hwn iíw weld yn adran ëManylion Cyflog a Threth Incwmí eich P60."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = s"Faint o dreth y DU a gafodd ei thynnu o enillion maggie eich cleient?"
    val expectedTitle: String = s"Faint o dreth y DU a gafodd ei thynnu o enillion eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorNoEntry = "Nodwch y swm o dreth y DU a dynnwyd o enillion eich cleient"
    val expectedPTextNoData: String = "Fel arfer, maeír swm hwn iíw weld yn yr adran ëManylion Cyflog a Threth Incwmí ar P60 eich cleient."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  object Selectors {
    val pText = "#main-content > div > div > p.govuk-body"
    val hintText = "#amount-hint"
    val continueButton = "#continue"
    val inputAmountField = "#amount"
  }

  private def cya(taxToDate: Option[BigDecimal] = Some(200), isPriorSubmission: Boolean = true): EmploymentUserData =
    anEmploymentUserData.copy(
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = isPriorSubmission,
      hasPriorStudentLoans = isPriorSubmission,
      employment = anEmploymentCYAModel.copy(anEmploymentDetails.copy("maggie", totalTaxToDate = taxToDate, currentDataIsHmrcHeld = false))
    )

  val multipleEmployments: AllEmploymentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(
    aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None))),
    aHmrcEmploymentSource.copy(employmentId = "002", hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None)))
  ))

  ".show" when {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        //noinspection ScalaStyle
        "for end of year return a page with prior data" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(cya())
            urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.commonExpectedResults.expectedPTextWithData, pText)
          buttonCheck(user.commonExpectedResults.continue, continueButton)
          textOnPageCheck(user.commonExpectedResults.hint, hintText)
          inputFieldValueCheck(amountInputName, inputAmountField, "")
        }

        //noinspection ScalaStyle
        "for end of year return a page without prior data" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(cya(None))
            urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          welshToggleCheck(user.isWelsh)
          textOnPageCheck(user.specificExpectedResults.get.expectedPTextNoData, pText)
          buttonCheck(user.commonExpectedResults.continue, continueButton)
          textOnPageCheck(user.commonExpectedResults.hint, hintText)
          inputFieldValueCheck(amountInputName, inputAmountField, "")
        }


        "The input field" should {

          "be empty" when {
            "there is cya data with taxToDate field empty and no prior(i.e. user is adding a new employment)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(None, isPriorSubmission = false))
                urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              lazy val document = Jsoup.parse(result.body)

              implicit def documentSupplier: () => Document = () => document

              inputFieldValueCheck(amountInputName, inputAmountField, "")
            }

            "cya data and prior data are the same(i.e. user has clicked on change link)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYearEOY)
                insertCyaData(cya())
                urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              lazy val document = Jsoup.parse(result.body)

              implicit def documentSupplier: () => Document = () => document

              inputFieldValueCheck(amountInputName, inputAmountField, "")
            }
          }

          "be filled" when {
            "cya data and prior data differ (i.e user has updated their pay)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                userDataStub(anIncomeTaxUserData.copy(Some(multipleEmployments)), nino, taxYearEOY)
                insertCyaData(cya(Some(100.00)))
                urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              lazy val document = Jsoup.parse(result.body)

              implicit def documentSupplier: () => Document = () => document

              inputFieldValueCheck(amountInputName, inputAmountField, "100")
            }

            "cya amount field is filled and prior data is none (i.e user has added a new employment and updated their tax but now want to change it)" when {
              implicit lazy val result: WSResponse = {
                authoriseAgentOrIndividual(user.isAgent)
                dropEmploymentDB()
                noUserDataStub(nino, taxYearEOY)
                insertCyaData(cya(Some(100.00), isPriorSubmission = false))
                urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              lazy val document = Jsoup.parse(result.body)

              implicit def documentSupplier: () => Document = () => document

              inputFieldValueCheck(amountInputName, inputAmountField, "100")
            }
          }
        }

      }
    }
    "redirect to the CheckYourEmploymentDetails page there is no CYA data" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        urlGet(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
      }

      "redirect to CheckYourEmploymentDetails page" in {
        result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "redirect to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlGet(fullUrl(howMuchTaxUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

  }

  ".submit" should {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "an empty form is submitted" which {
            implicit lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(cya())
              urlPost(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(user.commonExpectedResults.expectedPTextWithData, pText)
            buttonCheck(user.commonExpectedResults.continue, continueButton)
            textOnPageCheck(user.commonExpectedResults.hint, hintText)
            inputFieldValueCheck(amountInputName, inputAmountField, "")

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorNoEntry, inputAmountField)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorNoEntry, Some(amountInputName))
            welshToggleCheck(user.isWelsh)

          }

          "a form is submitted with an invalid amount" which {
            implicit lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(cya())
              urlPost(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), body = Map("amount" -> "abc123"),
                welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(user.commonExpectedResults.expectedPTextWithData, pText)
            buttonCheck(user.commonExpectedResults.continue, continueButton)
            textOnPageCheck(user.commonExpectedResults.hint, hintText)
            inputFieldValueCheck(amountInputName, inputAmountField, "abc123")

            errorSummaryCheck(expectedErrorInvalidFormat, inputAmountField)
            errorAboveElementCheck(expectedErrorInvalidFormat, Some(amountInputName))
            welshToggleCheck(user.isWelsh)

          }

          "a form is submitted with an amount over the maximum limit" which {
            implicit lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(cya())
              urlPost(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), body = Map("amount" -> "9999999999999999999999999999"),
                welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(user.commonExpectedResults.expectedPTextWithData, pText)
            buttonCheck(user.commonExpectedResults.continue, continueButton)
            textOnPageCheck(user.commonExpectedResults.hint, hintText)
            inputFieldValueCheck(amountInputName, inputAmountField, "9999999999999999999999999999")

            errorSummaryCheck(expectedErrorMaxLimit, inputAmountField)
            errorAboveElementCheck(expectedErrorMaxLimit, Some(amountInputName))
            welshToggleCheck(user.isWelsh)

          }
        }

      }
    }

    "redirect to check employment details page when a valid form is submitted" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlPost(fullUrl(howMuchTaxUrl(taxYearEOY, employmentId)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map("amount" -> "100"))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentDetails.totalTaxToDate shouldBe Some(100)
      }
    }

    "redirect to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(cya())
        urlPost(fullUrl(howMuchTaxUrl(taxYear, employmentId)), body = Map("amount" -> "100"), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

  }
}
