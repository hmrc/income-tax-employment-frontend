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

package controllers.benefits.fuel

import forms.YesNoForm
import models.User
import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CompanyCarBenefitsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper{

  override val taxYear = 2021
  val url = s"$appUrl/$taxYear/benefits/company-car?employmentId=001"

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYear, "001", isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  def benefits(carModel: CarVanFuelModel): BenefitsViewModel = BenefitsViewModel(carVanFuelModel=Some(carModel), isUsingCustomerData = true, isBenefitsReceived = true)

  object Selectors {
    val yesSelector = "#value"
    val noSelector = "#value-no"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedError: String
  }


  trait CommonExpectedResults {
    val expectedCaption: String
    val radioTextYes: String
    val radioTextNo: String
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
    val expectedH1: String =  "Did your client get a company car benefit?"
    val expectedError: String = "Select yes if your client got a company car benefit"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client get a company car benefit?"
    val expectedH1: String =  "Did your client get a company car benefit?"
    val expectedError: String = "Select yes if your client got a company car benefit"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = "Employment for 6 April 2020 to 5 April 2021"
    val radioTextYes: String = "Yes"
    val radioTextNo: String = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = "Employment for 6 April 2020 to 5 April 2021"
    val radioTextYes: String = "Yes"
    val radioTextNo: String = "No"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return a radio button page when not in year" which {

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(carQuestion = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(user.commonExpectedResults.expectedCaption)
            radioButtonCheck(user.commonExpectedResults.radioTextYes, 1)

          radioButtonCheck(user.commonExpectedResults.radioTextNo, 2)
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
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel.copy(carQuestion = None))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(userData(fullEmploymentsModel(hmrcEmployment = Seq(employmentDetailsAndBenefits(fullBenefits)))), nino, taxYear)
            urlPost(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = form)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          titleCheck("Error: " + user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
          radioButtonCheck(user.commonExpectedResults.radioTextYes, 1)
          radioButtonCheck(user.commonExpectedResults.radioTextNo, 2)
        }
      }
    }
  }
}
