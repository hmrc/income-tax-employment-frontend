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

package views.employment

import controllers.employment.routes.{EmployerInformationController, EmploymentSummaryController, RemoveEmploymentController}
import controllers.expenses.routes.{EmploymentExpensesController, ExpensesInterruptPageController, RemoveExpensesController}
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import utils.ViewUtils
import views.html.employment.EmploymentSummaryView

import java.time.LocalDate

class EmploymentSummaryViewSpec extends ViewUnitTest {

  private val employmentId = "employmentId"
  private val employmentId2 = "002"
  private val employerName2 = "Ken Bosford"

  object Selectors {
    val valueHref = "#value"
    val cannotUpdateInfoSelector = "#main-content > div > div > div.govuk-inset-text"
    val employersSelector = "#employer-h2"

    def yourEmpInfoSelector(id: Int): String = s"#main-content > div > div > p:nth-child($id)"

    def employerNameSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(5) > div:nth-child($id) > dt"

    val employerNameSelector = "#main-content > div > div > dl:nth-child(4) > div > dt"
    val employerNameInYearSelector = "#main-content > div > div > dl:nth-child(5) > div > dt"

    def employerNameEOYSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(4) > div:nth-child($id) > dt"

    def viewEmployerSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(5) > div:nth-child($id) > dd.govuk-summary-list__actions > a"

    def changeEmployerSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(4) > div:nth-child($id) > dd.govuk-summary-list__actions > ul > li:nth-child(1) > a"

    def removeEmployerSelector(id: Int): String = s"#main-content > div > div > dl:nth-child(4) > div:nth-child($id) > dd.govuk-summary-list__actions > ul > li:nth-child(2) > a"

    val expensesHeadingSelector = "#expenses-h2"

    def expensesLineSelector(expensesOnly: Boolean = false): String = s"#main-content > div > div > ${if (expensesOnly) "dl" else "dl:nth-child(8)"} > div > dt"

    val thisIsATotalSelector = s"#total-of-expenses"
    val noEmployersSelector = "#main-content > div > div > p:nth-child(4)"
    val viewExpensesSelector = "#main-content > div > div > dl:nth-child(8) > div > dd.govuk-summary-list__actions > a"
    val addAnotherSelector = "#main-content > div > div > p:nth-child(5) > a"

    def changeExpensesSelector(expensesOnly: Boolean = false): String =
      s"#main-content > div > div > ${if (expensesOnly) "dl" else "dl:nth-child(8)"} > div > dd.govuk-summary-list__actions > ul > li:nth-child(1) > a"

    def removeExpensesSelector(expensesOnly: Boolean = false): String =
      s"#main-content > div > div > ${if (expensesOnly) "dl" else "dl:nth-child(8)"} > div > dd.govuk-summary-list__actions > ul > li:nth-child(2) > a"

    val addExpensesSelector = s"#add-expenses"

    def addEmployerSelector = "#main-content > div > div > p.govuk-body > a"

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
    val expectedH1: String = "Cyflogaeth TWE"
    val expectedTitle: String = expectedH1

    def expectedCaption(taxYear: Int): String = s"Cyflogaeth TWE ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"

    val notificationTitle: String = "Important"
    val notificationContent: String = "Ni allwch gael treuliau heb gyflogwr. Ychwanegu cyflogwr neu dynnu treuliau."
    val noEmployers: String = "Dim cyflogwyr"
    val name: String = "maggie"
    val startedDateString: LocalDate => String = (startedOn: LocalDate) => s"Wedi dechrau ar " + ViewUtils.translatedDateFormatter(startedOn)(getMessages(isWelsh = true))
    val startedDateMissingString: String = "Dyddiad dechrau ar goll"
    val startedDateBeforeString: Int => String = (taxYear: Int) => s"Wedi dechrau cyn 6 Ebrill $taxYear"
    val change: String = s"Newid"
    val remove: String = s"Tynnu"
    val addAnother: String = "Ychwanegu cyflogwr arall"
    val thisIsATotal: String = "Dyma gyfanswm o dreuliau o bob cyflogaeth yn y flwyddyn dreth."
    val expenses: String = "Treuliau"
    val addEmployer: String = "Ychwanegu cyflogwr"
    val addExpenses: String = "Ychwanegu treuliau"
    val employer: String = "Cyflogwr"
    val employers: String = "Cyflogwyr"
    val returnToOverview: String = "Yn ôl i’r trosolwg"
    val employmentDetails: String = "Manylion Cyflogaeth"
    val benefits: String = "Buddiannau"
    val view: String = "Bwrw golwg"
    val cannotUpdate: String = "Ddim yn gallu diweddaru"
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
    val yourEmpInfo: String =
      "Mae’ch gwybodaeth cyflogaeth yn seiliedig ar yr wybodaeth sydd eisoes gennym amdanoch. Mae’n cynnwys manylion cyflogaeth, buddiannau a chyfraniadau benthyciadau myfyrwyr."
    val yourEmpInfoStudentLoansUnreleased: String = "Mae’ch gwybodaeth cyflogaeth yn seiliedig ar yr wybodaeth sydd eisoes gennym amdanoch. Mae’n cynnwys manylion cyflogaeth a buddiannau."
    val cannotUpdateInfo: String = s"Ni allwch ddiweddaru’ch manylion cyflogaeth tan 6 Ebrill $taxYear."
    val cannotAdd: String = s"Ni allwch ychwanegu treuliau tan 6 Ebrill $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val yourEmpInfo: String =
      "Mae gwybodaeth cyflogaeth eich cleient yn seiliedig ar yr wybodaeth sydd eisoes gennym amdano. Mae’n cynnwys manylion cyflogaeth, buddiannau a chyfraniadau benthyciadau myfyrwyr."
    val yourEmpInfoStudentLoansUnreleased: String = "Mae gwybodaeth cyflogaeth eich cleient yn seiliedig ar yr wybodaeth sydd eisoes gennym amdano. Mae’n cynnwys manylion cyflogaeth a buddiannau."
    val cannotUpdateInfo: String = s"Ni allwch ddiweddaru manylion cyflogaeth eich cleient tan 6 Ebrill $taxYear."
    val cannotAdd: String = s"Ni allwch ychwanegu treuliau eich cleient tan 6 Ebrill $taxYear."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val underTest = inject[EmploymentSummaryView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    val specific = userScenario.specificExpectedResults.get
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "return the multiple employment summary EOY page" when {
        "there are 2 employments and its EOY showing 2 employments with expenses" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val employments = Seq(anEmploymentSource, anEmploymentSource.copy(employerName = "Ken Bosford", employmentId = "002"))
          val htmlFormat = underTest(taxYearEOY, employments, expensesExist = true, isInYear = false, isAgent = userScenario.isAgent)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(employers, employersSelector)
          textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(3))
          textOnPageCheck(name + " " + startedDateBeforeString(taxYearEOY - 1), employerNameEOYSelector(id = 1))
          linkCheck(s"$change $change $name", changeEmployerSelector(1), EmployerInformationController.show(taxYearEOY, employmentId).url)
          linkCheck(s"$remove $remove $name", removeEmployerSelector(1), RemoveEmploymentController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(employerName2 + " " + startedDateBeforeString(taxYearEOY - 1), employerNameEOYSelector(id = 2))
          linkCheck(s"$change $change $employerName2", changeEmployerSelector(2), EmployerInformationController.show(taxYearEOY, employmentId2).url)
          linkCheck(s"$remove $remove $employerName2", removeEmployerSelector(2), RemoveEmploymentController.show(taxYearEOY, employmentId2).url)
          linkCheck(addAnother, addAnotherSelector, EmploymentSummaryController.addNewEmployment(taxYearEOY).url, isExactUrlMatch = false)
          textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
          textOnPageCheck(thisIsATotal, thisIsATotalSelector)
          textOnPageCheck(expenses, expensesLineSelector(), "as a line item")
          linkCheck(s"$change $change $expenses", changeExpensesSelector(), ExpensesInterruptPageController.show(taxYearEOY).url)
          linkCheck(s"$remove $remove $expenses", removeExpensesSelector(), RemoveExpensesController.show(taxYearEOY).url)
          buttonCheck(returnToOverview)
        }

        "there are 2 employments and its EOY showing 2 employments without expenses" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val employments = Seq(anEmploymentSource, anEmploymentSource.copy(employerName = "Ken Bosford", employmentId = "002"))
          val htmlFormat = underTest(taxYearEOY, employments, expensesExist = false, isInYear = false, isAgent = userScenario.isAgent)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(employers, employersSelector)
          textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(3))
          textOnPageCheck(name + " " + startedDateBeforeString(taxYearEOY - 1), employerNameEOYSelector(id = 1))
          linkCheck(s"$change $change $name", changeEmployerSelector(1), EmployerInformationController.show(taxYearEOY, employmentId).url)
          linkCheck(s"$remove $remove $name", removeEmployerSelector(1), RemoveEmploymentController.show(taxYearEOY, employmentId).url)
          textOnPageCheck(employerName2 + " " + startedDateBeforeString(taxYearEOY - 1), employerNameEOYSelector(id = 2))
          linkCheck(s"$change $change $employerName2", changeEmployerSelector(2), EmployerInformationController.show(taxYearEOY, employmentId2).url)
          linkCheck(s"$remove $remove $employerName2", removeEmployerSelector(2), RemoveEmploymentController.show(taxYearEOY, employmentId2).url)
          linkCheck(addAnother, addAnotherSelector, EmploymentSummaryController.addNewEmployment(taxYearEOY).url, isExactUrlMatch = false)
          linkCheck(addExpenses, addExpensesSelector, EmploymentExpensesController.show(taxYearEOY).url)
          buttonCheck(returnToOverview)
        }
      }

      "return the multiple employment summary in year page" when {
        "there are 2 employments and its in year showing 2 employments with expenses" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val employments = Seq(anEmploymentSource, anEmploymentSource.copy(employerName = "Ken Bosford", employmentId = "002"))
          val htmlFormat = underTest(taxYear, employments, expensesExist = true, isInYear = true, isAgent = userScenario.isAgent)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYear))
          textOnPageCheck(specific.cannotUpdateInfo, cannotUpdateInfoSelector)
          textOnPageCheck(employers, employersSelector)
          textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(4))
          textOnPageCheck(name + " " + startedDateBeforeString(taxYear - 1), employerNameSelector(id = 1))
          linkCheck(s"$view $view $name", viewEmployerSelector(1), EmployerInformationController.show(taxYear, employmentId).url)
          textOnPageCheck(employerName2 + " " + startedDateBeforeString(taxYear - 1), employerNameSelector(id = 2))
          linkCheck(s"$view $view $employerName2", viewEmployerSelector(2), EmployerInformationController.show(taxYear, employmentId2).url)
          textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
          textOnPageCheck(thisIsATotal, thisIsATotalSelector)
          textOnPageCheck(expenses, expensesLineSelector(), "as a line item")
          linkCheck(s"$view $view $expenses", viewExpensesSelector, ExpensesInterruptPageController.show(taxYear).url)
          buttonCheck(returnToOverview)
        }

        "there are 2 employments and its in year showing 2 employments without expenses" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val employments = Seq(anEmploymentSource, anEmploymentSource.copy(employerName = "Ken Bosford", employmentId = "002"))
          val htmlFormat = underTest(taxYear, employments, expensesExist = false, isInYear = true, isAgent = userScenario.isAgent)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedH1)
          captionCheck(expectedCaption(taxYear))
          textOnPageCheck(specific.cannotUpdateInfo, cannotUpdateInfoSelector)
          textOnPageCheck(employers, employersSelector)
          textOnPageCheck(specific.yourEmpInfo, yourEmpInfoSelector(4))
          textOnPageCheck(name + " " + startedDateBeforeString(taxYear - 1), employerNameSelector(id = 1))
          linkCheck(s"$view $view $name", viewEmployerSelector(1), EmployerInformationController.show(taxYear, employmentId).url)
          textOnPageCheck(employerName2 + " " + startedDateBeforeString(taxYear - 1), employerNameSelector(id = 2))
          linkCheck(s"$view $view $employerName2", viewEmployerSelector(2), EmployerInformationController.show(taxYear, employmentId2).url)
          buttonCheck(returnToOverview)
        }
      }

      "render template when in year without employment but has expenses" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYear, Seq.empty, expensesExist = true, isInYear = true, isAgent = userScenario.isAgent)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedH1)
        textOnPageCheck(specific.cannotUpdateInfo, cannotUpdateInfoSelector)
        captionCheck(expectedCaption(taxYear))
        textOnPageCheck(thisIsATotal, thisIsATotalSelector)
        textOnPageCheck(expenses, expensesHeadingSelector, "as a heading")
        textOnPageCheck(expenses, expensesLineSelector(true), "as a line item")
        buttonCheck(returnToOverview)
      }

      "show the summary page when no data is in session and in year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYear, Seq.empty, expensesExist = false, isInYear = true, isAgent = userScenario.isAgent)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedH1)
        captionCheck(expectedCaption(taxYear))
        textOnPageCheck(specific.cannotUpdateInfo, cannotUpdateInfoSelector)
        buttonCheck(returnToOverview)
      }

      "show the summary page when no data is in session EOY" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, Seq.empty, expensesExist = false, isInYear = false, isAgent = userScenario.isAgent)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedH1)
        captionCheck(expectedCaption(taxYearEOY))
        linkCheck(addEmployer, addEmployerSelector, EmploymentSummaryController.addNewEmployment(taxYearEOY).url, isExactUrlMatch = false)
        buttonCheck(returnToOverview)
      }
    }
  }
}
