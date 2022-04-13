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

import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithBenefits
import utils.PageUrls.{carBenefitsUrl, fullUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CompanyCarBenefitsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  object Selectors {
    val yesSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val radioTextYes: String
    val radioTextNo: String
    val errorText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did you get a company car benefit?"
    val expectedH1: String = "Did you get a company car benefit?"
    val expectedError: String = "Select yes if you got a company car benefit"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Did you get a company car benefit?"
    val expectedH1: String = "Did you get a company car benefit?"
    val expectedError: String = "Select yes if you got a company car benefit"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client get a company car benefit?"
    val expectedH1: String = "Did your client get a company car benefit?"
    val expectedError: String = "Select yes if your client got a company car benefit"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client get a company car benefit?"
    val expectedH1: String = "Did your client get a company car benefit?"
    val expectedError: String = "Select yes if your client got a company car benefit"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val radioTextYes: String = "Yes"
    val radioTextNo: String = "No"
    val errorText: String = "Error: "
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"

    val radioTextYes: String = "Iawn"
    val radioTextNo: String = "Na"
    val errorText: String = "Gwall: "
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return a radio button page when not in year" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carQuestion = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(fullUrl(carBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          radioButtonCheck(user.commonExpectedResults.radioTextYes, 1, checked = false)
          radioButtonCheck(user.commonExpectedResults.radioTextNo, 2, checked = false)
        }
      }
    }
  }

  ".submit" when {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return a radio button page when not in year and a bad form submission" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carQuestion = None)))
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlPost(fullUrl(carBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.commonExpectedResults.errorText + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
          radioButtonCheck(user.commonExpectedResults.radioTextYes, 1, checked = false)
          radioButtonCheck(user.commonExpectedResults.radioTextNo, 2, checked = false)
        }
      }
    }
  }
}
