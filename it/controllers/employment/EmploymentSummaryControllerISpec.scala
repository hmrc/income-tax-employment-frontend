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

import models.IncomeTaxUserData
import models.employment.AllEmploymentData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER, UNAUTHORIZED}
import play.api.i18n.Messages
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentFinancialDataBuilder.aHmrcEmploymentFinancialData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers, ViewUtils}

import java.time.LocalDate
import scala.concurrent.Future

class EmploymentSummaryControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"
  private val employmentId2 = "002"
  private val employerName2 = "Ken Bosford"
  private val empWithNoBenefitsOrExpenses: IncomeTaxUserData = anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcExpenses = None,
    hmrcEmploymentData = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None)))))))
  private val multipleEmpsWithExpenses: IncomeTaxUserData = anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(
    hmrcEmploymentData = Seq(aHmrcEmploymentSource, aHmrcEmploymentSource.copy(employmentId = employmentId2, employerName = employerName2)))))
  private val multipleEmpsWithoutExpenses: IncomeTaxUserData = anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(
    hmrcExpenses = None, hmrcEmploymentData = Seq(aHmrcEmploymentSource, aHmrcEmploymentSource.copy(employmentId = employmentId2, employerName = employerName2)))))

  object Selectors {
    val valueHref = "#value"
    val cannotUpdateInfoSelector = "#main-content > div > div > div.govuk-inset-text"
    val employersSelector = "#employer-h2"

    def yourEmpInfoSelector(id: Int): String = s"#main-content > div > div > p:nth-child($id)"

    def employerNameSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(5) > div:nth-child($id) > dt"

    val employerNameSelector = "#main-content > div > div > dl:nth-child(4) > div > dt"
    val employerNameInYearSelector = "#main-content > div > div > dl:nth-child(5) > div > dt"

    def employerNameEOYSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(4) > div:nth-child($id) > dt"

    def viewEmployerSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(5) > div:nth-child($id) > dd > a"

    def changeEmployerSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(4) > div:nth-child($id) > dd.govuk-summary-list__value > a"

    def removeEmployerSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(4) > div:nth-child($id) > dd.govuk-summary-list__actions > a"

    val expensesHeadingSelector = "#expenses-h2"

    def expensesLineSelector(expensesOnly: Boolean = false): String = s"#main-content > div > div > ${if (expensesOnly) "dl" else "dl:nth-child(8)"} > div > dt"

    val thisIsATotalSelector = s"#total-of-expenses"
    val noEmployersSelector = "#main-content > div > div > p:nth-child(4)"
    val viewExpensesSelector = "#main-content > div > div > dl:nth-child(8) > div > dd > a"
    val addAnotherSelector = "#main-content > div > div > p:nth-child(5) > a"

    def changeExpensesSelector(expensesOnly: Boolean = false): String = s"#main-content > div > div > ${if (expensesOnly) "dl" else "dl:nth-child(8)"} > div > dd.govuk-summary-list__value > a"

    def removeExpensesSelector(expensesOnly: Boolean = false): String = s"#main-content > div > div > ${if (expensesOnly) "dl" else "dl:nth-child(8)"} > div > dd.govuk-summary-list__actions > a"

    val addExpensesSelector = s"#add-expenses"

    def addEmployerSelector(n: Int = 3): String = s"#main-content > div > div > p:nth-child($n) > a"

    val addSelector = "#main-content > div > div > dl:nth-child(7) > div > dd > a"
    val cannotAddSelector = "#main-content > div > div > p:nth-child(7)"
    val cannotUpdateSelector = "#main-content > div > div > dl:nth-child(5) > div:nth-child(2) > dd"
  }

  trait SpecificExpectedResults {
    val yourEmpInfo: String
    val yourEmpInfoStudentLoansUnreleased: String
    val cannotUpdateInfo: String
    val cannotAdd: String
  }

  trait CommonExpectedResults {
    val expectedH1: String
    val expectedTitle: String

    def expectedCaption(taxYear: Int): String

    val notificationTitle: String
    val notificationContent: String
    val name: String
    val startedDateString: LocalDate => String
    val startedDateMissingString: String
    val startedDateBeforeString: Int => String
    val noEmployers: String
    val change: String
    val remove: String
    val addAnother: String
    val thisIsATotal: String
    val expenses: String
    val addEmployer: String
    val addExpenses: String
    val employer: String
    val employers: String
    val returnToOverview: String
    val employmentDetails: String
    val benefits: String
    val view: String
    val cannotUpdate: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedH1: String = "PAYE employment"
    val expectedTitle: String = expectedH1

    def expectedCaption(taxYear: Int): String = s"PAYE employment for 6 April ${taxYear - 1} to 5 April $taxYear"

    val notificationTitle: String = "Important"
    val notificationContent: String = "You cannot have expenses without an employer. Add an employer or remove expenses."
    val name: String = "maggie"
    val startedDateString: LocalDate => String = (startedOn: LocalDate) => s"Started " + ViewUtils.translatedDateFormatter(startedOn)(getMessages(isWelsh = false))
    val startedDateMissingString: String = "Start date missing"
    val startedDateBeforeString: Int => String = (taxYear: Int) => s"Started before 6 April $taxYear"
    val noEmployers: String = "No employers"
    val change: String = s"Change"
    val remove: String = s"Remove"
    val addAnother: String = "Add another employer"
    val thisIsATotal: String = "This is a total of expenses from all employment in the tax year."
    val expenses: String = "Expenses"
    val addEmployer: String = "Add an employer"
    val addExpenses: String = "Add expenses"
    val employer: String = "Employer"
    val employers: String = "Employers"
    val returnToOverview: String = "Return to overview"
    val employmentDetails: String = "Employment details"
    val benefits: String = "Benefits"
    val view: String = "View"
    val cannotUpdate: String = "Cannot update"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedH1: String = "PAYE employment"
    val expectedTitle: String = expectedH1

    def expectedCaption(taxYear: Int): String = s"PAYE employment for 6 April ${taxYear - 1} to 5 April $taxYear"

    val notificationTitle: String = "Important"
    val notificationContent: String = "You cannot have expenses without an employer. Add an employer or remove expenses."
    val noEmployers: String = "No employers"
    val name: String = "maggie"
    val startedDateString: LocalDate => String = (startedOn: LocalDate) => s"Started " + ViewUtils.translatedDateFormatter(startedOn)(getMessages(isWelsh = true))
    val startedDateMissingString: String = "Start date missing"
    val startedDateBeforeString: Int => String = (taxYear: Int) => s"Started before 6 April $taxYear"
    val add: String = "Add"
    val change: String = s"Newid"
    val remove: String = s"Tynnu"
    val addAnother: String = "Add another employer"
    val thisIsATotal: String = "This is a total of expenses from all employment in the tax year."
    val expenses: String = "Expenses"
    val addEmployer: String = "Add an employer"
    val addExpenses: String = "Add expenses"
    val employer: String = "Employer"
    val employers: String = "Employers"
    val returnToOverview: String = "Return to overview"
    val employmentDetails: String = "Employment details"
    val benefits: String = "Benefits"
    val view: String = "View"
    val cannotUpdate: String = "Cannot update"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val yourEmpInfo: String = "Your employment information is based on the information we already hold about you. It includes employment details, benefits and student loans contributions."
    val yourEmpInfoStudentLoansUnreleased: String = "Your employment information is based on the information we already hold about you. It includes employment details and benefits."
    val cannotUpdateInfo: String = s"You cannot update your employment information until 6 April $taxYear."
    val cannotAdd: String = s"You cannot add expenses until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val yourEmpInfo: String = "Your client’s employment information is based on the information we already hold about them. It includes employment details, benefits and student loans contributions."
    val yourEmpInfoStudentLoansUnreleased: String = "Your client’s employment information is based on the information we already hold about them. It includes employment details and benefits."
    val cannotUpdateInfo: String = s"You cannot update your client’s employment information until 6 April $taxYear."
    val cannotAdd: String = s"You cannot add your client’s expenses until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val yourEmpInfo: String = "Your employment information is based on the information we already hold about you. It includes employment details, benefits and student loans contributions."
    val yourEmpInfoStudentLoansUnreleased: String = "Your employment information is based on the information we already hold about you. It includes employment details and benefits."
    val cannotUpdateInfo: String = s"You cannot update your employment information until 6 April $taxYear."
    val cannotAdd: String = s"You cannot add expenses until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val yourEmpInfo: String = "Your client’s employment information is based on the information we already hold about them. It includes employment details, benefits and student loans contributions."
    val yourEmpInfoStudentLoansUnreleased: String = "Your client’s employment information is based on the information we already hold about them. It includes employment details and benefits."
    val cannotUpdateInfo: String = s"You cannot update your client’s employment information until 6 April $taxYear."
    val cannotAdd: String = s"You cannot add your client’s expenses until 6 April $taxYear."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
    import Selectors._

    userScenarios.foreach { user =>
      import user.commonExpectedResults._

      val specific = user.specificExpectedResults.get

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return the single employment summary EOY page" when {
          "there is only one employment and its the EOY with expenses" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              val hmrcEmploymentSource = aHmrcEmploymentSource.copy(startDate = None)
              userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(hmrcEmploymentSource)))), nino, taxYearEOY)
              urlGet(fullUrl(employmentSummaryUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(employer, employersSelector)
            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(3))
            textOnPageCheck(name + " " + startedDateMissingString, employerNameSelector)
            linkCheck(s"$change$change $name", changeEmployerSelector(1), employerInformationUrl(taxYearEOY, employmentId))
            linkCheck(s"$remove $remove $name", removeEmployerSelector(1), removeEmploymentUrl(taxYearEOY, employmentId))
            linkCheck(addAnother, addAnotherSelector, summaryAddNewEmployerUrl(taxYearEOY), isExactUrlMatch = false)
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            textOnPageCheck(expenses, expensesLineSelector(), "as a line item")
            linkCheck(s"$change$change $expenses", changeExpensesSelector(), expensesInterruptUrl(taxYearEOY))
            linkCheck(s"$remove $remove $expenses", removeExpensesSelector(), removeExpensesUrl(taxYearEOY))
            buttonCheck(returnToOverview)
          }

          "there is no employment but has expenses" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq()))), nino, taxYearEOY)
              urlGet(fullUrl(employmentSummaryUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(employers, employersSelector)
            textOnPageCheck(thisIsATotal, thisIsATotalSelector)
            notificationBannerCheck(notificationTitle, notificationContent)
            linkCheck(addEmployer, addEmployerSelector(4), summaryAddNewEmployerUrl(taxYearEOY), isExactUrlMatch = false)
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            textOnPageCheck(expenses, expensesLineSelector(true), "as a line item")
            linkCheck(s"$change$change $expenses", changeExpensesSelector(true), expensesInterruptUrl(taxYearEOY))
            linkCheck(s"$remove $remove $expenses", removeExpensesSelector(true), removeExpensesUrl(taxYearEOY))
            buttonCheck(returnToOverview)
          }

          "there is only one employment and its the EOY without expenses" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              val hmrcEmploymentSource = aHmrcEmploymentSource.copy(startDate = Some((taxYearEOY - 1).toString + "-04-05"))
              userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(hmrcEmploymentSource), hmrcExpenses = None))), nino, taxYearEOY)
              userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcExpenses = None))), nino, taxYearEOY)
              urlGet(fullUrl(employmentSummaryUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(employer, employersSelector)
            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(3))
            textOnPageCheck(name + " " + s"Started before 6 April ${taxYearEOY - 1}", employerNameSelector)
            linkCheck(s"$change$change $name", changeEmployerSelector(1), employerInformationUrl(taxYearEOY, employmentId))
            linkCheck(s"$remove $remove $name", removeEmployerSelector(1), removeEmploymentUrl(taxYearEOY, employmentId))
            linkCheck(addAnother, addAnotherSelector, summaryAddNewEmployerUrl(taxYearEOY), isExactUrlMatch = false)
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            linkCheck(addExpenses, addExpensesSelector, claimEmploymentExpensesUrl(taxYearEOY))
            buttonCheck(returnToOverview)
          }
        }

        "return the single employment summary in year page" when {
          "there is no employment but has expenses" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq()))), nino, taxYear)
              urlGet(fullUrl(employmentSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            textOnPageCheck(specific.cannotUpdateInfo, cannotUpdateInfoSelector)
            captionCheck(expectedCaption(taxYear))
            textOnPageCheck(employers, employersSelector)
            textOnPageCheck(thisIsATotal, thisIsATotalSelector)
            textOnPageCheck(noEmployers, noEmployersSelector)
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            textOnPageCheck(expenses, expensesLineSelector(true), "as a line item")
            buttonCheck(returnToOverview)
          }

          "there is only one employment and its in year with expenses" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              val hmrcEmploymentSource = aHmrcEmploymentSource.copy(startDate = Some(taxYear + "-04-05"))
              userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(hmrcEmploymentSource)))), nino, taxYear)
              urlGet(fullUrl(employmentSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYear))
            textOnPageCheck(specific.cannotUpdateInfo, cannotUpdateInfoSelector)
            textOnPageCheck(employer, employersSelector)
            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(4))
            textOnPageCheck(name + " " + startedDateString(LocalDate.parse(taxYear + "-04-05")), employerNameInYearSelector)
            linkCheck(s"$view$view $name", viewEmployerSelector(1), employerInformationUrl(taxYear, employmentId))
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            textOnPageCheck(thisIsATotal, thisIsATotalSelector)
            textOnPageCheck(expenses, expensesLineSelector(), "as a line item")
            linkCheck(s"$view$view $expenses", viewExpensesSelector, expensesInterruptUrl(taxYear))
            buttonCheck(returnToOverview)
          }

          "there is only one employment and its in year without expenses or benefits" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(empWithNoBenefitsOrExpenses, nino, taxYear)
              urlGet(fullUrl(employmentSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYear))
            textOnPageCheck(specific.cannotUpdateInfo, cannotUpdateInfoSelector)
            textOnPageCheck(employer, employersSelector)
            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(4))
            textOnPageCheck(name + " " + startedDateBeforeString(taxYear - 1), employerNameInYearSelector)
            linkCheck(s"$view$view $name", viewEmployerSelector(1), employerInformationUrl(taxYear, employmentId))
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            textOnPageCheck(specific.cannotAdd, cannotAddSelector)
            buttonCheck(returnToOverview)
          }
        }

        "return the single employment summary in year page without references to student loans" when {
          "the student loans feature switch is false" which {
            val headers = if (user.isWelsh) {
              Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear), HeaderNames.ACCEPT_LANGUAGE -> "cy")
            } else {
              Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear))
            }

            val request = FakeRequest("GET", employmentSummaryUrl(taxYear)).withHeaders(headers: _*)

            lazy val result: Future[Result] = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(anIncomeTaxUserData, nino, taxYear)
              route(appWithFeatureSwitchesOff, request, "{}").get
            }

            implicit def document: () => Document = () => Jsoup.parse(bodyOf(result))

            "status OK" in {
              status(result) shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            textOnPageCheck(specific.yourEmpInfoStudentLoansUnreleased, yourEmpInfoSelector(4))
            buttonCheck(returnToOverview)
          }
        }

        "return the multiple employment summary in year page" when {
          "there are 2 employments and its in year showing 2 employments with expenses" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(multipleEmpsWithExpenses, nino, taxYear)
              urlGet(fullUrl(employmentSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYear))
            textOnPageCheck(specific.cannotUpdateInfo, cannotUpdateInfoSelector)
            textOnPageCheck(employers, employersSelector)
            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(4))
            textOnPageCheck(name + " " + startedDateBeforeString(taxYear - 1), employerNameSelector(id = 1))
            linkCheck(s"$view$view $name", viewEmployerSelector(1), employerInformationUrl(taxYear, employmentId))
            textOnPageCheck(employerName2 + " " + startedDateBeforeString(taxYear - 1), employerNameSelector(id = 2))
            linkCheck(s"$view$view $employerName2", viewEmployerSelector(2), employerInformationUrl(taxYear, employmentId2))
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            textOnPageCheck(thisIsATotal, thisIsATotalSelector)
            textOnPageCheck(expenses, expensesLineSelector(), "as a line item")
            linkCheck(s"$view$view $expenses", viewExpensesSelector, expensesInterruptUrl(taxYear))
            buttonCheck(returnToOverview)
          }

          "there are 2 employments and its in year showing 2 employments without expenses" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(multipleEmpsWithoutExpenses, nino, taxYear)
              urlGet(fullUrl(employmentSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYear))
            textOnPageCheck(specific.cannotUpdateInfo, cannotUpdateInfoSelector)
            textOnPageCheck(employers, employersSelector)
            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(4))
            textOnPageCheck(name + " " + startedDateBeforeString(taxYear - 1), employerNameSelector(id = 1))
            linkCheck(s"$view$view $name", viewEmployerSelector(1), employerInformationUrl(taxYear, employmentId))
            textOnPageCheck(employerName2 + " " + startedDateBeforeString(taxYear - 1), employerNameSelector(id = 2))
            linkCheck(s"$view$view $employerName2", viewEmployerSelector(2), employerInformationUrl(taxYear, employmentId2))
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            textOnPageCheck(specific.cannotAdd, cannotAddSelector)
            buttonCheck(returnToOverview)
          }
        }

        "return the multiple employment summary EOY page" when {
          "there are 2 employments and its EOY showing 2 employments with expenses" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(multipleEmpsWithExpenses, nino, taxYearEOY)
              urlGet(fullUrl(employmentSummaryUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(employers, employersSelector)
            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(3))
            textOnPageCheck(name + " " + startedDateBeforeString(taxYearEOY - 1), employerNameEOYSelector(id = 1))
            linkCheck(s"$change$change $name", changeEmployerSelector(1), employerInformationUrl(taxYearEOY, employmentId))
            linkCheck(s"$remove $remove $name", removeEmployerSelector(1), removeEmploymentUrl(taxYearEOY, employmentId))
            textOnPageCheck(employerName2 + " " + startedDateBeforeString(taxYearEOY - 1), employerNameEOYSelector(id = 2))
            linkCheck(s"$change$change $employerName2", changeEmployerSelector(2), employerInformationUrl(taxYearEOY, employmentId2))
            linkCheck(s"$remove $remove $employerName2", removeEmployerSelector(2), removeEmploymentUrl(taxYearEOY, employmentId2))
            linkCheck(addAnother, addAnotherSelector, summaryAddNewEmployerUrl(taxYearEOY), isExactUrlMatch = false)
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            textOnPageCheck(thisIsATotal, thisIsATotalSelector)
            textOnPageCheck(expenses, expensesLineSelector(), "as a line item")
            linkCheck(s"$change$change $expenses", changeExpensesSelector(), expensesInterruptUrl(taxYearEOY))
            linkCheck(s"$remove $remove $expenses", removeExpensesSelector(), removeExpensesUrl(taxYearEOY))
            buttonCheck(returnToOverview)
          }

          "there are 2 employments and its EOY showing 2 employments without expenses" which {
            implicit lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(multipleEmpsWithoutExpenses, nino, taxYearEOY)
              urlGet(fullUrl(employmentSummaryUrl(taxYearEOY)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            lazy val document = Jsoup.parse(result.body)

            implicit def documentSupplier: () => Document = () => document

            "status OK" in {
              result.status shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(employers, employersSelector)
            textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(3))
            textOnPageCheck(name + " " + startedDateBeforeString(taxYearEOY - 1), employerNameEOYSelector(id = 1))
            linkCheck(s"$change$change $name", changeEmployerSelector(1), employerInformationUrl(taxYearEOY, employmentId))
            linkCheck(s"$remove $remove $name", removeEmployerSelector(1), removeEmploymentUrl(taxYearEOY, employmentId))
            textOnPageCheck(employerName2 + " " + startedDateBeforeString(taxYearEOY - 1), employerNameEOYSelector(id = 2))
            linkCheck(s"$change$change $employerName2", changeEmployerSelector(2), employerInformationUrl(taxYearEOY, employmentId2))
            linkCheck(s"$remove $remove $employerName2", removeEmployerSelector(2), removeEmploymentUrl(taxYearEOY, employmentId2))
            linkCheck(addAnother, addAnotherSelector, summaryAddNewEmployerUrl(taxYearEOY), isExactUrlMatch = false)
            textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
            linkCheck(addExpenses, addExpensesSelector, claimEmploymentExpensesUrl(taxYearEOY))
            buttonCheck(returnToOverview)
          }
        }

        "return the multiple employment summary EOY page without references to student loans" when {
          "the student loans feature switch is false" which {
            val headers = if (user.isWelsh) {
              Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), HeaderNames.ACCEPT_LANGUAGE -> "cy")
            } else {
              Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
            }

            val request = FakeRequest("GET", employmentSummaryUrl(taxYearEOY)).withHeaders(headers: _*)

            lazy val result: Future[Result] = {
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(multipleEmpsWithExpenses, nino, taxYearEOY)
              route(appWithFeatureSwitchesOff, request, "{}").get
            }

            implicit def document: () => Document = () => Jsoup.parse(bodyOf(result))

            "status OK" in {
              status(result) shouldBe OK
            }

            welshToggleCheck(user.isWelsh)
            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1)
            textOnPageCheck(specific.yourEmpInfoStudentLoansUnreleased, yourEmpInfoSelector(3))
            buttonCheck(returnToOverview)
          }
        }

        "show the summary page when no data is in session EOY" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(isAgent = true)
            userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
            urlGet(s"$appUrl/$taxYearEOY/employment-summary", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
          linkCheck(addExpenses, addExpensesSelector, claimEmploymentExpensesUrl(taxYearEOY))
          textOnPageCheck(employers, employersSelector, "as a heading")
          linkCheck(addEmployer, addEmployerSelector(), summaryAddNewEmployerUrl(taxYearEOY))
        }
      }
    }

    "redirect when there is employment data returned but no hmrc employment data" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = true)
        userDataStub(IncomeTaxUserData(Some(AllEmploymentData(Seq(), None, Seq(anEmploymentSource), None))), nino, taxYear)
        urlGet(fullUrl(employmentSummaryUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "status OK" in {
        result.status shouldBe SEE_OTHER
      }
    }

    "redirect the User to the Overview page no data in session" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = true)
        userDataStub(IncomeTaxUserData(), nino, taxYear)
        urlGet(fullUrl(employmentSummaryUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(overviewUrl(taxYear)) shouldBe true
      }

    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        unauthorisedAgentOrIndividual(isAgent = true)
        urlGet(fullUrl(employmentSummaryUrl(taxYear)))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }
}
