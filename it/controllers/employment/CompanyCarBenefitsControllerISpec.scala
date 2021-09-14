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

import models.User
import models.employment.BenefitsViewModel
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
    EmploymentUserData(sessionId, mtditid, nino, taxYear, "001", isPriorSubmission = isPrior, employmentCyaModel)

  def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
    EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

  object Selectors {

  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String

  }


  trait CommonExpectedResults {
    val expectedCaption: String
    val radioTextYes: String
    val radioTextNo: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did you receive a benefit for a company car?"
    val expectedH1: String = "Did you receive a benefit for a company car?"

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Did you receive a benefit for a company car?"
    val expectedH1: String = "Did you receive a benefit for a company car?"

  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client receive a benefit for a company car?"
    val expectedH1: String = "Did your client receive a benefit for a company car?"

  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client receive a benefit for a company car?"
    val expectedH1: String = "Did your client receive a benefit for a company car?"

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
    import Selectors._

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return a radio button page when not in year" which {


          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
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
}
