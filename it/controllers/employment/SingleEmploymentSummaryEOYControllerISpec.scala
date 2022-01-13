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

import forms.YesNoForm
import models.IncomeTaxUserData
import models.employment.{AllEmploymentData, EmploymentData, EmploymentSource, Pay}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
import utils.PageUrls.{employerNameUrlWithoutEmploymentId, overviewUrl}

class SingleEmploymentSummaryEOYControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY = taxYear - 1

  object Selectors {
    val valueHref = "#value"
    val yourEmpInfoSelector = "p.govuk-body"
    val employerNameSelector = "span.hmrc-add-to-a-list__identifier"
    val changeLinkSelector = "span.hmrc-add-to-a-list__change > a"
    val removeLinkSelector = "span.hmrc-add-to-a-list__remove > a"
    val doYouNeedAnotherSelector = "#main-content > div > div > form > div > fieldset > legend"
    val youMustTellSelector = "#value-hint"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val formRadioButtonValueSelector = "#value"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val youMustTell: String
    val expectedErrorText: String
    val yourEmpInfo: String
  }

  trait CommonExpectedResults {
    val continueButton: String
    val expectedCaption: String
    val doYouNeedAnother: String
    val yesText: String
    val noText: String
    val name: String
    val change: String
    val remove: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val doYouNeedAnother: String = "Do you need to add another employment?"
    val yesText: String = "Yes"
    val noText: String = "No"
    val name: String = "Maggie"
    val change: String = s"Change Change employment information for $name"
    //change is included twice because selector is for the whole link. First change is the text/link, second change is part of hidden text
    val remove: String = s"Remove Remove employment information for $name"
    //remove is included twice because selector is for the whole link. First remove is the text/link, second remove is part of hidden text
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val doYouNeedAnother: String = "Do you need to add another employment?"
    val yesText: String = "Yes"
    val noText: String = "No"
    val name: String = "Maggie"
    val change: String = s"Change Change employment information for $name" //First change is the text/link, second change is part of hidden text
    val remove: String = s"Remove Remove employment information for $name" //First remove is the text/link, second remove is part of hidden text
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you need to add another employment"
    val yourEmpInfo: String = "Your employment information is based on the information we already hold about you."
    val youMustTell: String = "You must tell us about all your employment."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you need to add another employment"
    val yourEmpInfo: String = "Your client’s employment information is based on the information we already hold about them."
    val youMustTell: String = "You must tell us about all your client’s employment."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you need to add another employment"
    val yourEmpInfo: String = "Your employment information is based on the information we already hold about you."
    val youMustTell: String = "You must tell us about all your employment."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you need to add another employment"
    val yourEmpInfo: String = "Your client’s employment information is based on the information we already hold about them."
    val youMustTell: String = "You must tell us about all your client’s employment."
  }

  private def url(taxYear: Int) = s"$appUrl/$taxYear/employment-summary"
  val employmentId = "001"
  val changeLinkHref = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/employer-information?employmentId=$employmentId"
  val removeLinkHref = s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/remove-employment?employmentId=$employmentId"

  val employmentSource: EmploymentSource = EmploymentSource(
    employmentId = employmentId,
    employerName = "Maggie",
    employerRef = Some("223/AB12399"),
    payrollId = Some("123456789999"),
    startDate = Some("2019-04-21"),
    cessationDate = Some("2020-03-11"),
    dateIgnored = None,
    submittedOn = Some("2020-01-04T05:01:01Z"),
    employmentData = Some(EmploymentData(
      submittedOn = "2020-02-12",
      employmentSequenceNumber = Some("123456789999"),
      companyDirector = Some(true),
      closeCompany = Some(false),
      directorshipCeasedDate = Some("2020-02-12"),
      occPen = Some(false),
      disguisedRemuneration = Some(false),
      pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
      None
    )),
    None
  )

  val singleEmploymentModel: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(employmentSource),
    hmrcExpenses = None,
    customerEmploymentData = Seq(),
    customerExpenses = None
  )

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

        "return the single employment summary EOY page" when {

          "there is only one employment and its the EOY" which {
            val taxYear = taxYearEOY
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(singleEmploymentModel)), nino, taxYear)
              urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(specific.expectedTitle)
            h1Check(specific.expectedH1)
            captionCheck(expectedCaption)

            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector)
            textOnPageCheck(name, employerNameSelector)
            linkCheck(change, changeLinkSelector, changeLinkHref)
            linkCheck(remove, removeLinkSelector, removeLinkHref)
            textOnPageCheck(doYouNeedAnother, doYouNeedAnotherSelector)
            textOnPageCheck(specific.youMustTell, youMustTellSelector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(continueButton)

            formPostLinkCheck(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/employment-summary", formSelector)
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

          "radio button is YES and employment data is present" which {
            val taxYear = taxYearEOY
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(singleEmploymentModel)), nino, taxYear)
              urlPost(url(taxYear), yesNoFormYes, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to the employer name page" in {
              result.header(HeaderNames.LOCATION).get contains employerNameUrlWithoutEmploymentId(taxYear).get shouldBe true
            }
          }

          "radio button is NO and employment data is present" which {
            val taxYear = taxYearEOY
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(singleEmploymentModel)), nino, taxYear)
              urlPost(url(taxYear), yesNoFormNo, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to the overview page" in {
              result.header(HeaderNames.LOCATION) shouldBe overviewUrl(taxYear)
            }
          }
        }

        "return BAD_REQUEST and render correct errors" when {
          "the yes/no radio button has not been selected" which {
            val taxYear = taxYearEOY
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(singleEmploymentModel)), nino, taxYear)
              urlPost(url(taxYear), yesNoFormEmpty, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            s"has an BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            welshToggleCheck(user.isWelsh)
            titleCheck(specific.expectedErrorTitle)
            h1Check(specific.expectedH1)
            captionCheck(expectedCaption)

            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector)
            textOnPageCheck(name, employerNameSelector)
            linkCheck(change, changeLinkSelector, changeLinkHref)
            linkCheck(remove, removeLinkSelector, removeLinkHref)
            textOnPageCheck(doYouNeedAnother, doYouNeedAnotherSelector)
            textOnPageCheck(specific.youMustTell, youMustTellSelector)
            errorSummaryCheck(specific.expectedErrorText, valueHref)
            errorAboveElementCheck(specific.expectedErrorText)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(continueButton)

            formPostLinkCheck(s"/update-and-submit-income-tax-return/employment-income/$taxYearEOY/employment-summary", formSelector)
          }
        }

        "returns authorization failure" when {
          "user is unauthorised" which {
            val taxYear = taxYearEOY
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
