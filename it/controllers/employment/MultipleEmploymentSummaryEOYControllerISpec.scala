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

import forms.YesNoForm
import models.IncomeTaxUserData
import models.employment.{AllEmploymentData, EmploymentData, EmploymentSource, Pay}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class MultipleEmploymentSummaryEOYControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val validTaxYear2021 = 2021

  object Selectors {
    val valueHref = "#value"

    def yourEmpInfoSelector(nthChild: Int) = s"#main-content > div > div > p:nth-child($nthChild)"

    def employerName1Selector(nthChild: Int) = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(1) > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"

    def changeLink1Selector(nthChild: Int) = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(1) > span.hmrc-add-to-a-list__change > a"

    def removeLink1Selector(nthChild: Int) = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(1) > span.hmrc-add-to-a-list__remove > a"

    def employerName2Selector(nthChild: Int) = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(2) > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"

    def changeLink2Selector(nthChild: Int) = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(2) > span.hmrc-add-to-a-list__change > a"

    def removeLink2Selector(nthChild: Int) = s"#main-content > div > div > div:nth-child($nthChild) > ul > li:nth-child(2) > span.hmrc-add-to-a-list__remove > a"

    val doYouNeedAnotherSelector = "#main-content > div > div > form > div > fieldset > legend"
    val youMustTellSelector = "#value-hint"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val formRadioButtonValueSelector = "#value"
    val expensesHeaderSelector = "#main-content > div > div > p.govuk-label--m"

    def thisIsATotalSelector(nthChild: Int) = s"#main-content > div > div > p:nth-child($nthChild)"

    def expensesSelector(nthChild: Int) = s"#main-content > div > div > div:nth-child($nthChild) > ul > li > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"

    def changeExpensesSelector(nthChild: Int) = s"#main-content > div > div > div:nth-child($nthChild) > ul > li > span.hmrc-add-to-a-list__change > a"

    def removeExpensesSelector(nthChild: Int) = s"#main-content > div > div > div:nth-child($nthChild) > ul > li > span.hmrc-add-to-a-list__remove > a"
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
    val change: String
    val remove: String
    val employerName: String
    val employerName2: String
    val thisIsATotal: String
    val expensesText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${validTaxYear2021 - 1} to 5 April $validTaxYear2021"
    val doYouNeedAnother: String = "Do you need to add another employment?"
    val yesText: String = "Yes"
    val noText: String = "No"
    val change: String = "Change"
    val remove: String = "Remove"
    val employerName: String = "Maggie"
    val employerName2: String = "Argos"
    val thisIsATotal: String = "This is a total of expenses from all employment in the tax year."
    val expensesText: String = "Expenses"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val continueButton: String = "Continue"
    val expectedCaption = s"Employment for 6 April ${validTaxYear2021 - 1} to 5 April $validTaxYear2021"
    val doYouNeedAnother: String = "Do you need to add another employment?"
    val yesText: String = "Yes"
    val noText: String = "No"
    val change: String = "Change"
    val remove: String = "Remove"
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

  val employmentId1 = "001"
  val employmentId2 = "002"

  def changeLinkHref(empId: String) = s"/income-through-software/return/employment-income/$validTaxYear2021/employer-details-and-benefits?employmentId=$empId"

  def removeLinkHref(empId: String) = s"/income-through-software/return/employment-income/$validTaxYear2021/remove-employment?employmentId=$empId"

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
            val taxYear = validTaxYear2021
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(multipleEmploymentModel)), nino, taxYear)
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

            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(2))
            textOnPageCheck(employerName, employerName1Selector(3))
            linkCheck(change, changeLink1Selector(3), changeLinkHref(employmentId1))
            linkCheck(remove, removeLink1Selector(3), removeLinkHref(employmentId1))
            textOnPageCheck(employerName2, employerName2Selector(3))
            linkCheck(change, changeLink2Selector(3), changeLinkHref(employmentId2))
            linkCheck(remove, removeLink2Selector(3), removeLinkHref(employmentId2))
            textOnPageCheck(expensesText, expensesHeaderSelector)
            textOnPageCheck(thisIsATotal, thisIsATotalSelector(5))
            "has an expenses section" should {
              textOnPageCheck(expensesText, expensesSelector(6))
              linkCheck(change, changeExpensesSelector(6), "/income-through-software/return/employment-income/2021/check-employment-expenses")
              linkCheck(remove, removeExpensesSelector(6), "/income-through-software/return/employment-income/2021/check-employment-expenses")
            }
            textOnPageCheck(doYouNeedAnother, doYouNeedAnotherSelector)
            textOnPageCheck(specific.youMustTell, youMustTellSelector)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueButton)

            formPostLinkCheck(s"/income-through-software/return/employment-income/$validTaxYear2021/employment-summary", formSelector)
            formRadioValueCheck(selected = true, formRadioButtonValueSelector)
          }

          "there are 3 employments for EOY, but one has an ignored date, page shows 2 employments" which {
            val taxYear = validTaxYear2021
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(multipleEmploymentWithOneIgnoredModel)), nino, taxYear)
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

            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(2))
            textOnPageCheck(employerName, employerName1Selector(3))
            linkCheck(change, changeLink1Selector(3), changeLinkHref(employmentId1))
            linkCheck(remove, removeLink1Selector(3), removeLinkHref(employmentId1))
            textOnPageCheck(employerName2, employerName2Selector(3))
            linkCheck(change, changeLink2Selector(3), changeLinkHref(employmentId2))
            linkCheck(remove, removeLink2Selector(3), removeLinkHref(employmentId2))
            textOnPageCheck(expensesText, expensesHeaderSelector)
            textOnPageCheck(thisIsATotal, thisIsATotalSelector(5))
            "has an expenses section" should {
              textOnPageCheck(expensesText, expensesSelector(6))
              linkCheck(change, changeExpensesSelector(6), "/income-through-software/return/employment-income/2021/check-employment-expenses")
              linkCheck(remove, removeExpensesSelector(6), "/income-through-software/return/employment-income/2021/check-employment-expenses")
            }
            textOnPageCheck(doYouNeedAnother, doYouNeedAnotherSelector)
            textOnPageCheck(specific.youMustTell, youMustTellSelector)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueButton)

            formPostLinkCheck(s"/income-through-software/return/employment-income/$validTaxYear2021/employment-summary", formSelector)
            formRadioValueCheck(selected = true, formRadioButtonValueSelector)
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
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(multipleEmploymentModel)), nino, taxYear)
              urlPost(url(taxYear), yesNoFormYes, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to the employer name page" in {
              result.header(HeaderNames.LOCATION).getOrElse("") contains
                "/income-through-software/return/employment-income/2021/employer-name?employmentId=" shouldBe true
            }
          }

          "radio button is NO and employment data is present" which {
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(multipleEmploymentModel)), nino, taxYear)
              urlPost(url(taxYear), yesNoFormNo, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "status SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to the overview page" in {
              result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
            }
          }
        }

        "return BAD_REQUEST and render correct errors" when {
          "the yes/no radio button has not been selected" which {
            val taxYear = validTaxYear2021
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(IncomeTaxUserData(Some(multipleEmploymentModel)), nino, taxYear)
              urlPost(url(taxYear), yesNoFormEmpty, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
            linkCheck(change, changeLink1Selector(4), changeLinkHref(employmentId1))
            linkCheck(remove, removeLink1Selector(4), removeLinkHref(employmentId1))
            textOnPageCheck(employerName2, employerName2Selector(4))
            linkCheck(change, changeLink2Selector(4), changeLinkHref(employmentId2))
            linkCheck(remove, removeLink2Selector(4), removeLinkHref(employmentId2))
            textOnPageCheck(expensesText, expensesHeaderSelector)
            textOnPageCheck(thisIsATotal, thisIsATotalSelector(6))
            "has an expenses section" should {
              textOnPageCheck(expensesText, expensesSelector(7))
              linkCheck(change, changeExpensesSelector(7), "/income-through-software/return/employment-income/2021/check-employment-expenses")
              linkCheck(remove, removeExpensesSelector(7), "/income-through-software/return/employment-income/2021/check-employment-expenses")
            }
            textOnPageCheck(doYouNeedAnother, doYouNeedAnotherSelector)
            textOnPageCheck(specific.youMustTell, youMustTellSelector)
            errorSummaryCheck(specific.expectedErrorText, valueHref)
            errorAboveElementCheck(specific.expectedErrorText)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueButton)

            formPostLinkCheck(s"/income-through-software/return/employment-income/$validTaxYear2021/employment-summary", formSelector)
            formRadioValueCheck(selected = true, formRadioButtonValueSelector)
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
