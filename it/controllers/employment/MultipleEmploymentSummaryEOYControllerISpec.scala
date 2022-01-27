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
import utils.PageUrls.{checkYourExpensesUrl, employerInformationUrl, employerNameUrlWithoutEmploymentId, employmentSummaryUrl, fullUrl, overviewUrl, removeEmploymentUrl}
import org.scalatest.DoNotDiscover

@DoNotDiscover
class MultipleEmploymentSummaryEOYControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  object Selectors {
    val valueHref = "#value"

    def yourEmpInfoSelector(nthChild: Int): String = s"#main-content > div > div > p:nth-child($nthChild)"

    def employerName1Selector(nthChild: Int): String =
      s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(1) > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"

    def changeLink1Selector(nthChild: Int): String = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(1) > span.hmrc-add-to-a-list__change > a"

    def removeLink1Selector(nthChild: Int): String = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(1) > span.hmrc-add-to-a-list__remove > a"

    def employerName2Selector(nthChild: Int): String =
      s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(2) > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"

    def changeLink2Selector(nthChild: Int): String = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(2) > span.hmrc-add-to-a-list__change > a"

    def removeLink2Selector(nthChild: Int): String = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(2) > span.hmrc-add-to-a-list__remove > a"

    val doYouNeedAnotherSelector = "#main-content > div > div > form > div > fieldset > legend"
    val youMustTellSelector = "#value-hint"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val formRadioButtonValueSelector = "#value"
    val expensesHeaderSelector = "#main-content > div > div > h2.govuk-label--m"

    def thisIsATotalSelector(nthChild: Int): String = s"#main-content > div > div > p:nth-child($nthChild)"

    def expensesSelector(nthChild: Int): String = s"#main-content > div > div > div:nth-child($nthChild) > ul > li > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"

    def changeExpensesSelector(nthChild: Int): String = s"#main-content > div > div > div:nth-child($nthChild) > ul > li > span.hmrc-add-to-a-list__change > a"

    def removeExpensesSelector(nthChild: Int): String = s"#main-content > div > div > div:nth-child($nthChild) > ul > li > span.hmrc-add-to-a-list__remove > a"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val youMustTell: String
    val expectedErrorText: String
    val yourEmpInfo: String
    val changeExpenses: String
    val removeExpenses: String
  }

  trait CommonExpectedResults {
    val continueButton: String
    val expectedCaption: String
    val doYouNeedAnother: String
    val yesText: String
    val noText: String
    def change(employerName: String): String
    def remove(employerName: String): String
    val employerName: String
    val employerName2: String
    val thisIsATotal: String
    val expensesText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val doYouNeedAnother: String = "Do you need to add another employment?"
    val yesText: String = "Yes"
    val noText: String = "No"
    def change(employerName: String): String = s"Change Change employment information for $employerName"
    //change is included twice because selector is for the whole link. First change is the text/link, second change is part of hidden text
    def remove(employerName: String): String = s"Remove Remove employment information for $employerName"
    //remove is included twice because selector is for the whole link. First remove is the text/link, second remove is part of hidden text
    val changeExpenses: String = "Change"
    val removeExpenses: String = "Remove"
    val employerName: String = "Maggie"
    val employerName2: String = "Argos"
    val thisIsATotal: String = "This is a total of expenses from all employment in the tax year."
    val expensesText: String = "Expenses"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val doYouNeedAnother: String = "Do you need to add another employment?"
    val yesText: String = "Yes"
    val noText: String = "No"
    def change(employerName: String): String = s"Change Change employment information for $employerName" //First change is the text/link, second change is part of hidden text
    def remove(employerName: String): String = s"Remove Remove employment information for $employerName" //First remove is the text/link, second remove is part of hidden text
    val changeExpenses: String = "Change"
    val removeExpenses: String = "Remove"
    val employerName: String = "Maggie"
    val employerName2: String = "Argos"
    val thisIsATotal: String = "This is a total of expenses from all employment in the tax year."
    val expensesText: String = "Expenses"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you need to add another employment"
    val yourEmpInfo: String = "Your employment information is based on the information we already hold about you."
    val youMustTell: String = "You must tell us about all your employment."
    val changeExpenses: String = "Change Change your expenses from all employment this tax year" //First change is the text/link, second change is part of hidden text
    val removeExpenses: String = "Remove Remove your expenses from all employment this tax year" //First remove is the text/link, second remove is part of hidden text
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you need to add another employment"
    val yourEmpInfo: String = "Your client’s employment information is based on the information we already hold about them."
    val youMustTell: String = "You must tell us about all your client’s employment."
    val changeExpenses: String = "Change Change your client’s expenses from all employment this tax year" //First change is the text/link, second change is part of hidden text
    val removeExpenses: String = "Remove Remove your client’s expenses from all employment this tax year" //First remove is the text/link, second remove is part of hidden text
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you need to add another employment"
    val yourEmpInfo: String = "Your employment information is based on the information we already hold about you."
    val youMustTell: String = "You must tell us about all your employment."
    val changeExpenses: String = "Change Change your expenses from all employment this tax year" //First change is the text/link, second change is part of hidden text
    val removeExpenses: String = "Remove Remove your expenses from all employment this tax year" //First remove is the text/link, second remove is part of hidden text
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = expectedH1
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you need to add another employment"
    val yourEmpInfo: String = "Your client’s employment information is based on the information we already hold about them."
    val youMustTell: String = "You must tell us about all your client’s employment."
    val changeExpenses: String = "Change Change your client’s expenses from all employment this tax year" //First change is the text/link, second change is part of hidden text
    val removeExpenses: String = "Remove Remove your client’s expenses from all employment this tax year" //First remove is the text/link, second remove is part of hidden text
  }

  val employmentId1 = "001"
  val employmentId2 = "002"

  val employmentSource: EmploymentSource = EmploymentSource(
    employmentId = employmentId1,
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

  val multipleEmploymentModel: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(employmentSource, employmentSource.copy(employmentId = employmentId2, employerName = "Argos")),
    hmrcExpenses = None,
    customerEmploymentData = Seq(),
    customerExpenses = None
  )

  val multipleEmploymentWithOneIgnoredModel: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      employmentSource,
      employmentSource.copy(employmentId = "003", employerName = "Tesco", dateIgnored = Some("2020-01-04T05:01:01Z")),
      employmentSource.copy(employmentId = employmentId2, employerName = "Argos")
    ),
    hmrcExpenses = None,
    customerEmploymentData = Seq(),
    customerExpenses = None
  )

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      import user.commonExpectedResults._

      val specific = user.specificExpectedResults.get

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return the multiple employment summary EOY page" when {

          "there are 2 employments and its the EOY showing 2 employments" which {
            val taxYear = taxYearEOY
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(multipleEmploymentModel)), nino, taxYear)
              urlGet(fullUrl(employmentSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(specific.expectedTitle)
            h1Check(specific.expectedH1)
            captionCheck(expectedCaption)

            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(2))
            textOnPageCheck(employerName, employerName1Selector(3))
            linkCheck(change(employerName), changeLink1Selector(3), employerInformationUrl(taxYearEOY, employmentId1))
            linkCheck(remove(employerName), removeLink1Selector(3), removeEmploymentUrl(taxYearEOY, employmentId1))
            textOnPageCheck(employerName2, employerName2Selector(3))
            linkCheck(change(employerName2), changeLink2Selector(3), employerInformationUrl(taxYearEOY, employmentId2))
            linkCheck(remove(employerName2), removeLink2Selector(3), removeEmploymentUrl(taxYearEOY, employmentId2))
            textOnPageCheck(expensesText, expensesHeaderSelector)
            textOnPageCheck(thisIsATotal, thisIsATotalSelector(5))
            "has an expenses section" should {
              textOnPageCheck(expensesText, expensesSelector(6))
              linkCheck(specific.changeExpenses, changeExpensesSelector(6), checkYourExpensesUrl(taxYearEOY))
              linkCheck(specific.removeExpenses, removeExpensesSelector(6), checkYourExpensesUrl(taxYearEOY))
            }
            textOnPageCheck(doYouNeedAnother, doYouNeedAnotherSelector)
            textOnPageCheck(specific.youMustTell, youMustTellSelector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(continueButton)

            formPostLinkCheck(employmentSummaryUrl(taxYearEOY), formSelector)
          }

          "there are 3 employments for EOY, but one has an ignored date, page shows 2 employments" which {
            val taxYear = taxYearEOY
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(multipleEmploymentWithOneIgnoredModel)), nino, taxYear)
              urlGet(fullUrl(employmentSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(specific.expectedTitle)
            h1Check(specific.expectedH1)
            captionCheck(expectedCaption)

            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(2))
            textOnPageCheck(employerName, employerName1Selector(3))
            linkCheck(change(employerName), changeLink1Selector(3), employerInformationUrl(taxYearEOY, employmentId1))
            linkCheck(remove(employerName), removeLink1Selector(3), removeEmploymentUrl(taxYearEOY, employmentId1))
            textOnPageCheck(employerName2, employerName2Selector(3))
            linkCheck(change(employerName2), changeLink2Selector(3), employerInformationUrl(taxYearEOY, employmentId2))
            linkCheck(remove(employerName2), removeLink2Selector(3), removeEmploymentUrl(taxYearEOY, employmentId2))
            textOnPageCheck(expensesText, expensesHeaderSelector)
            textOnPageCheck(thisIsATotal, thisIsATotalSelector(5))
            "has an expenses section" should {
              textOnPageCheck(expensesText, expensesSelector(6))
              linkCheck(specific.changeExpenses, changeExpensesSelector(6), checkYourExpensesUrl(taxYearEOY))
              linkCheck(specific.removeExpenses, removeExpensesSelector(6), checkYourExpensesUrl(taxYearEOY))
            }
            textOnPageCheck(doYouNeedAnother, doYouNeedAnotherSelector)
            textOnPageCheck(specific.youMustTell, youMustTellSelector)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(continueButton)

            formPostLinkCheck(employmentSummaryUrl(taxYearEOY), formSelector)
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

        "return BAD_REQUEST and render correct errors" when {
          "the yes/no radio button has not been selected" which {
            val taxYear = taxYearEOY
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(multipleEmploymentModel)), nino, taxYear)
              urlPost(fullUrl(employmentSummaryUrl(taxYear)), yesNoFormEmpty, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            s"has an BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            welshToggleCheck(user.isWelsh)
            titleCheck(specific.expectedTitle)
            h1Check(specific.expectedH1)
            captionCheck(expectedCaption)

            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(3))
            textOnPageCheck(employerName, employerName1Selector(4))
            linkCheck(change(employerName), changeLink1Selector(4), employerInformationUrl(taxYearEOY, employmentId1))
            linkCheck(remove(employerName), removeLink1Selector(4), removeEmploymentUrl(taxYearEOY, employmentId1))
            textOnPageCheck(employerName2, employerName2Selector(4))
            linkCheck(change(employerName2), changeLink2Selector(4), employerInformationUrl(taxYearEOY, employmentId2))
            linkCheck(remove(employerName2), removeLink2Selector(4), removeEmploymentUrl(taxYearEOY, employmentId2))
            textOnPageCheck(expensesText, expensesHeaderSelector)
            textOnPageCheck(thisIsATotal, thisIsATotalSelector(6))
            "has an expenses section" should {
              textOnPageCheck(expensesText, expensesSelector(7))
              linkCheck(specific.changeExpenses, changeExpensesSelector(7), checkYourExpensesUrl(taxYearEOY))
              linkCheck(specific.removeExpenses, removeExpensesSelector(7), checkYourExpensesUrl(taxYearEOY))
            }
            textOnPageCheck(doYouNeedAnother, doYouNeedAnotherSelector)
            textOnPageCheck(specific.youMustTell, youMustTellSelector)
            errorSummaryCheck(specific.expectedErrorText, valueHref)
            errorAboveElementCheck(specific.expectedErrorText)
            radioButtonCheck(yesText, 1, checked = false)
            radioButtonCheck(noText, 2, checked = false)
            buttonCheck(continueButton)

            formPostLinkCheck(employmentSummaryUrl(taxYearEOY), formSelector)
          }
        }
      }
    }

    "redirect" when {

      "radio button is YES and employment data is present" which {
        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(multipleEmploymentModel)), nino, taxYear)
          urlPost(fullUrl(employmentSummaryUrl(taxYearEOY)), yesNoFormYes, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "status SEE_OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirect to the employer name page" in {
          result.header(HeaderNames.LOCATION).get.contains(employerNameUrlWithoutEmploymentId(taxYearEOY)) shouldBe true
        }
      }

      "radio button is NO and employment data is present" which {
        val taxYear = taxYearEOY
        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(Some(multipleEmploymentModel)), nino, taxYear)
          urlPost(fullUrl(employmentSummaryUrl(taxYear)), yesNoFormNo, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "status SEE_OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirect to the overview page" in {
          result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYear)) shouldBe true
        }
      }
    }

    "returns authorization failure" when {
      "user is unauthorised" which {
        val taxYear = taxYearEOY
        lazy val result: WSResponse = {
          unauthorisedAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentSummaryUrl(taxYear)), yesNoFormYes, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }
}
