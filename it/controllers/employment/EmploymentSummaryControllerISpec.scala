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

import controllers.employment.EmploymentSummaryControllerISpec.FullModel._
import controllers.employment.routes._
import models.IncomeTaxUserData
import models.employment._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class EmploymentSummaryControllerISpec extends IntegrationTest with ViewHelpers {

  val url = s"$appUrl/$taxYear/employment-summary"

  object Selectors {

    val headingSelector = "#main-content > div > div > header > h1"
    val captionSelector = "#main-content > div > div > header > p"
    val employmentSummaryParagraphSelector = "#main-content > div > div > p:nth-child(2)"
    val employmentDetailsRowSelector: String =
      s"#main-content > div > div > div.govuk-form-group > ul > li:nth-child(1) > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"
    val employmentBenefitsRowSelector: String =
      s"#main-content > div > div > div.govuk-form-group > ul > li:nth-child(2) > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"
    val employmentExpensesRowSelector: String =
      s"#main-content > div > div > div.govuk-form-group > ul > li:nth-child(3) > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val expensesParagraphHeadingSelector = "#main-content > div > div > p.govuk-label--m"
    val expensesParagraphSubheadingSelector = "#main-content > div > div > p:nth-child(6)"
    val expensesSummaryListRowFieldNameSelector = s"#main-content > div > div > div:nth-child(7) > ul > li > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light"
    val expensesSummaryListRowFieldActionSelector = s"#main-content > div > div > div:nth-child(7) > ul > li > span.hmrc-add-to-a-list__change"
    def employmentDetailsRowLinkSelector: String = s"$employmentDetailsRowSelector > a"
    def employmentBenefitsRowLinkSelector: String = s"$employmentBenefitsRowSelector > a"
    def employmentExpensesRowLinkSelector: String = s"$employmentExpensesRowSelector > a"
    def employerSummaryListRowFieldNameSelector(i: Int) = s"#main-content > div > div > div:nth-child(4) > ul > li:nth-child($i) > span.hmrc-add-to-a-list__identifier.hmrc-add-to-a-list__identifier--light > a"
    def employerSummaryListRowFieldActionSelector(i: Int) = s"#main-content > div > div > div:nth-child(4) > ul > li:nth-child($i) > span.hmrc-add-to-a-list__change"
    def expensesSummaryListRowFieldNameLinkSelector = s"$expensesSummaryListRowFieldNameSelector > a"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedContent: String
    val expectedInsetText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val employmentDetails: String
    val benefits: String
    val expenses: String
    val button: String
    val updated: String
    val cannotUpdate: String
    val expensesContent: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val employmentDetails = "Employment details"
    val benefits = "Benefits"
    val expenses = "Expenses"
    val button = "Return to overview"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
    val expensesContent = "This is a total of expenses from all employment in the tax year."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
    val employmentDetails = "Employment details"
    val benefits = "Benefits"
    val expenses = "Expenses"
    val button = "Return to overview"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
    val expensesContent = "This is a total of expenses from all employment in the tax year."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = "Employment"
    val expectedContent: String = "Your employment information is based on the information we already hold about you."
    val expectedInsetText: String = s"You cannot update your employment information until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = "Employment"
    val expectedContent: String = "Your client’s employment information is based on the information we already hold about them."
    val expectedInsetText: String = s"You cannot update your client’s employment information until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = "Employment"
    val expectedContent: String = "Your employment information is based on the information we already hold about you."
    val expectedInsetText: String = s"You cannot update your employment information until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Employment"
    val expectedTitle: String = "Employment"
    val expectedContent: String = "Your client’s employment information is based on the information we already hold about them."
    val expectedInsetText: String = s"You cannot update your client’s employment information until 6 April $taxYear."
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

        "return single employment summary view when there is only one employment without Expenses and Benefits" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(singleEmploymentModel)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "status OK" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(employerName1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, employmentSummaryParagraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          linkCheck(user.commonExpectedResults.employmentDetails, employmentDetailsRowLinkSelector, CheckEmploymentDetailsController.show(taxYear, employmentId1).url)
          textOnPageCheck(user.commonExpectedResults.benefits, employmentBenefitsRowSelector)
          textOnPageCheck(user.commonExpectedResults.expenses, employmentExpensesRowSelector)
          buttonCheck(user.commonExpectedResults.button)
          welshToggleCheck(user.isWelsh)
        }

        "redirect when there is employment data returned but no hmrc employment data" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(AllEmploymentData(Seq(), None, Seq(employmentSource), None))), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "status OK" in {
            result.status shouldBe SEE_OTHER
          }
        }

        "return single employment summary view when there is only one employment with Expenses and Benefits" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(singleEmploymentWithExpensesAndBenefitsModel)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "status OK" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(employerName3)
          textOnPageCheck(user.commonExpectedResults.expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, employmentSummaryParagraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          linkCheck(user.commonExpectedResults.employmentDetails, employmentDetailsRowLinkSelector, CheckEmploymentDetailsController.show(taxYear, employmentId3).url)
          linkCheck(user.commonExpectedResults.benefits, employmentBenefitsRowLinkSelector, CheckYourBenefitsController.show(taxYear, employmentId3).url)
          linkCheck(user.commonExpectedResults.expenses, employmentExpensesRowLinkSelector, CheckEmploymentExpensesController.show(taxYear).url)
          buttonCheck(user.commonExpectedResults.button)
          welshToggleCheck(user.isWelsh)
        }

        "render multiple employment summary view when there are two employments without Expenses and Benefits" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(multipleEmploymentModel)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "status OK" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, employmentSummaryParagraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          linkCheck(employerName1, employerSummaryListRowFieldNameSelector(1), EmploymentDetailsAndBenefitsController.show(taxYear, employmentId1).url)
          textOnPageCheck(user.commonExpectedResults.updated, employerSummaryListRowFieldActionSelector(1), s"for $employerName1 row")
          linkCheck(employerName2, employerSummaryListRowFieldNameSelector(2), EmploymentDetailsAndBenefitsController.show(taxYear, employmentId2).url)
          textOnPageCheck(user.commonExpectedResults.updated, employerSummaryListRowFieldActionSelector(2), s"for $employerName2 row")
          textOnPageCheck(user.commonExpectedResults.expenses, expensesParagraphHeadingSelector, "for Expenses heading")
          textOnPageCheck(user.commonExpectedResults.expensesContent, expensesParagraphSubheadingSelector)
          textOnPageCheck(user.commonExpectedResults.expenses, expensesSummaryListRowFieldNameSelector, "for Expenses row")
          textOnPageCheck(user.commonExpectedResults.cannotUpdate, expensesSummaryListRowFieldActionSelector)
          buttonCheck(user.commonExpectedResults.button)
          welshToggleCheck(user.isWelsh)
        }

        "render multiple employment summary view when there are two employments with Expenses and Benefits" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(multipleEmploymentWithExpensesModel)), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "status OK" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(user.commonExpectedResults.expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent, employmentSummaryParagraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInsetText, insetTextSelector)
          linkCheck(employerName1, employerSummaryListRowFieldNameSelector(1), EmploymentDetailsAndBenefitsController.show(taxYear, employmentId1).url)
          textOnPageCheck(user.commonExpectedResults.updated, employerSummaryListRowFieldActionSelector(1), s"for $employerName1 row")
          linkCheck(employerName3, employerSummaryListRowFieldNameSelector(2), EmploymentDetailsAndBenefitsController.show(taxYear, employmentId3).url)
          textOnPageCheck(user.commonExpectedResults.updated, employerSummaryListRowFieldActionSelector(2), s"for $employerName3 row")
          textOnPageCheck(user.commonExpectedResults.expenses, expensesParagraphHeadingSelector)
          textOnPageCheck(user.commonExpectedResults.expensesContent, expensesParagraphSubheadingSelector)
          linkCheck(user.commonExpectedResults.expenses, expensesSummaryListRowFieldNameLinkSelector, CheckEmploymentExpensesController.show(taxYear).url)
          textOnPageCheck(user.commonExpectedResults.updated, expensesSummaryListRowFieldActionSelector)
          buttonCheck(user.commonExpectedResults.button)
          welshToggleCheck(user.isWelsh)
        }

        "redirect the User to the Overview page no data in session" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(), nino, taxYear)
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }

        }

        "returns an action when auth call fails" which {
          lazy val result: WSResponse = {
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(url)
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }
}

object EmploymentSummaryControllerISpec {

  object FullModel {
    val employerName1 = "Maggie"
    val employmentId1 = "001"

    val employerName2 = "Argos"
    val employmentId2 = "002"

    val employerName3 = "Tesco"
    val employmentId3 = "003"

    val employmentBenefits = EmploymentBenefits(
      "2020-04-04T01:01:01Z",
      Some(
        Benefits(
          accommodation = Some(100),
          assets = Some(1000)
        )
      )
    )

    val employmentExpenses = EmploymentExpenses(
      Some("2020-04-04T01:01:01Z"),
      Some("2020-04-04T01:01:01Z"),
      totalExpenses = Some(100),
      Some(Expenses(businessTravelCosts = Some(100), None, None, None, None, None, None, None))
    )

    val employmentSource = EmploymentSource(
      employmentId = employmentId1,
      employerName = employerName1,
      employerRef = Some("223/AB12399"),
      payrollId = Some("123456789999"),
      startDate = Some("2019-04-21"),
      cessationDate = Some("2020-03-11"),
      dateIgnored = Some("2020-04-04T01:01:01Z"),
      submittedOn = Some("2020-01-04T05:01:01Z"),
      employmentData = Some(EmploymentData(
        submittedOn = ("2020-02-12"),
        employmentSequenceNumber = Some("123456789999"),
        companyDirector = Some(true),
        closeCompany = Some(false),
        directorshipCeasedDate = Some("2020-02-12"),
        occPen = Some(false),
        disguisedRemuneration = Some(false),
        pay = Some(Pay(Some(34234.15), Some(6782.92), Some(67676), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
        Some(Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(100.00),
            pglDeductionAmount = Some(100.00)
          ))
        ))
      )),
      None
    )

    val employmentSourceWithoutBenefits = EmploymentSource(
      employmentId = employmentId2,
      employerName = employerName2,
      employerRef = Some("223/AB12399"),
      payrollId = Some("123456789999"),
      startDate = Some("2019-04-21"),
      cessationDate = Some("2020-03-11"),
      dateIgnored = Some("2020-04-04T01:01:01Z"),
      submittedOn = Some("2020-01-04T05:01:01Z"),
      employmentData = Some(EmploymentData(
        submittedOn = ("2020-02-12"),
        employmentSequenceNumber = Some("123456789999"),
        companyDirector = Some(true),
        closeCompany = Some(false),
        directorshipCeasedDate = Some("2020-02-12"),
        occPen = Some(false),
        disguisedRemuneration = Some(false),
        pay = Some(Pay(Some(34234.15), Some(6782.92), Some(67676), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
        Some(Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(100.00),
            pglDeductionAmount = Some(100.00)
          ))
        ))
      )),
      None
    )

    val employmentSourceWithBenefits = EmploymentSource(
      employmentId = employmentId3,
      employerName = employerName3,
      employerRef = Some("223/AB12399"),
      payrollId = Some("123456789999"),
      startDate = Some("2019-04-21"),
      cessationDate = Some("2020-03-11"),
      dateIgnored = Some("2020-04-04T01:01:01Z"),
      submittedOn = Some("2020-01-04T05:01:01Z"),
      employmentData = Some(EmploymentData(
        submittedOn = ("2020-02-12"),
        employmentSequenceNumber = Some("123456789999"),
        companyDirector = Some(true),
        closeCompany = Some(false),
        directorshipCeasedDate = Some("2020-02-12"),
        occPen = Some(false),
        disguisedRemuneration = Some(false),
        pay = Some(Pay(Some(34234.15), Some(6782.92), Some(67676), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
        Some(Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(100.00),
            pglDeductionAmount = Some(100.00)
          ))
        ))
      )),
      Some(employmentBenefits)
    )

    val singleEmploymentModel: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(employmentSource),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )

    val singleEmploymentWithExpensesAndBenefitsModel: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(employmentSourceWithBenefits),
      hmrcExpenses = Some(employmentExpenses),
      customerEmploymentData = Seq(),
      customerExpenses = None
    )

    val multipleEmploymentModel: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(employmentSource, employmentSourceWithoutBenefits),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )

    val multipleEmploymentWithExpensesModel: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(employmentSource, employmentSourceWithBenefits),
      hmrcExpenses = Some(employmentExpenses),
      customerEmploymentData = Seq(),
      customerExpenses = None
    )

  }

}