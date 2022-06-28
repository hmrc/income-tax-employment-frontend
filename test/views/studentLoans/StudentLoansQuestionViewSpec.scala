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

package views.studentLoans

import forms.studentLoans.StudentLoanQuestionForm
import forms.studentLoans.StudentLoanQuestionForm.StudentLoansQuestionModel
import models.AuthorisationRequest
import models.employment.StudentLoansCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.studentLoans.StudentLoansQuestionView

class StudentLoansQuestionViewSpec extends ViewUnitTest {

  private val employmentId: String = "1234567890-0987654321"
  private val employmentName: String = "Whiterun Guards"

  trait CommonExpectedResults {
    val title: String
    val heading: String
    val caption: String
    val paragraphText_1: String
    val paragraphText_2: String

    val checkboxHint: String
    val checkboxUgl: String
    val checkboxUglHint: String
    val checkboxPgl: String
    val checkboxPglHint: String
    val checkboxNo: String
    val buttonText: String
    val errorEmpty: String
    val errorAll: String
    val expectedErrorTitle: String
  }

  object ExpectedResultsEnglish extends CommonExpectedResults {
    override val title: String = "Did you repay any student loan?"
    override val heading: String = "Did you repay any student loan while employed by Whiterun Guards?"
    override val caption: String = s"Student loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val paragraphText_1: String = "We only need to know about payments your employer deducted from your salary."
    override val paragraphText_2: String = "The Student Loans Company would have told you. Check your payslips or P60 for student loan deductions."
    override val checkboxHint: String = "Select all that apply."
    override val checkboxUgl: String = "Yes, undergraduate repayments"
    override val checkboxUglHint: String = "This covers courses like undergraduate degrees (BA, BSc), foundation degrees or Certificates of Higher Education (CertHE)."
    override val checkboxPgl: String = "Yes, postgraduate repayments"
    override val checkboxPglHint: String = "This covers courses like master’s or doctorates."
    override val checkboxNo: String = "No"
    override val buttonText: String = "Save and continue"
    override val errorEmpty: String = "Select the types of student loan you repaid, or select \"No\""
    override val errorAll: String = "Select the types of student loan you repaid, or select \"No\""
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsEnglishAgent extends CommonExpectedResults {
    override val title: String = "Did your client repay any student loan?"
    override val heading: String = "Did your client repay any student loan while employed by Whiterun Guards?"
    override val caption: String = s"Student loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val paragraphText_1: String = "We only need to know about payments their employer deducted from their salary."
    override val paragraphText_2: String = "The Student Loans Company would have told your client. Check your client’s payslips or P60 for student loan deductions."
    override val checkboxHint: String = "Select all that apply."
    override val checkboxUgl: String = "Yes, undergraduate repayments"
    override val checkboxUglHint: String = "This covers courses like undergraduate degrees (BA, BSc), foundation degrees or Certificates of Higher Education (CertHE)."
    override val checkboxPgl: String = "Yes, postgraduate repayments"
    override val checkboxPglHint: String = "This covers courses like master’s or doctorates."
    override val checkboxNo: String = "No"
    override val buttonText: String = "Save and continue"
    override val errorEmpty: String = "Select the types of student loan your client repaid, or select \"No\""
    override val errorAll: String = "Select the types of student loan your client repaid, or select \"No\""
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsWelsh extends CommonExpectedResults {
    override val title: String = "A wnaethoch ad-dalu unrhyw fenthyciad myfyriwr?"
    override val heading: String = "A wnaethoch ad-dalu unrhyw fenthyciad myfyriwr tra’ch bod wedi’ch cyflogi gan Whiterun Guards?"
    override val caption: String = s"Benthyciadau Myfyrwyr ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    override val paragraphText_1: String = "Rydym ond angen gwybod am daliadau y gwnaeth eich cyflogwr eu didynnu o’ch cyflog."
    override val paragraphText_2: String = "Byddai’r Cwmni Benthyciadau Myfyrwyr wedi rhoi gwybod i chi am hyn. Gwiriwch eich slipiau cyflog neu P60 am ddidyniadau benthyciad myfyrwyr."
    override val checkboxHint: String = "Dewiswch bob un sy’n berthnasol."
    override val checkboxUgl: String = "Iawn, ad-daliadau israddedig"
    override val checkboxUglHint: String = "Mae hyn yn cynnwys cyrsiau fel graddau israddedig (BA, BSc), graddau sylfaen neu Dystysgrifau Addysg Uwch (CertHE)."
    override val checkboxPgl: String = "Iawn, ad-daliadau ôl-raddedig"
    override val checkboxPglHint: String = "Mae hyn yn cynnwys cyrsiau fel graddau meistr neu ddoethuriaeth."
    override val checkboxNo: String = "Na"
    override val buttonText: String = "Cadw ac yn eich blaen"
    override val errorEmpty: String = "Dewiswch y mathau o fenthyciad myfyriwr a ad-dalwyd gennych, neu dewiswch \"Na\""
    override val errorAll: String = "Dewiswch y mathau o fenthyciad myfyriwr a ad-dalwyd gennych, neu dewiswch \"Na\""
    override val expectedErrorTitle: String = s"Gwall: $title"
  }

  object ExpectedResultsWelshAgent extends CommonExpectedResults {
    override val title: String = "A wnaeth eich cleient ad-dalu unrhyw fenthyciad myfyriwr?"
    override val heading: String = "A wnaeth eich cleient ad-dalu unrhyw fenthyciad myfyriwr tra ei fod wedi’i gyflogi gan Whiterun Guards?"
    override val caption: String = s"Benthyciadau Myfyrwyr ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    override val paragraphText_1: String = "Rydym ond angen gwybod am daliadau y gwnaeth ei gyflogwr eu didynnu o’i gyflog."
    override val paragraphText_2: String = "Byddai’r Cwmni Benthyciadau Myfyrwyr wedi rhoi gwybod i’ch cleient am hyn. Gwiriwch slipiau cyflog " +
      "neu P60 eich cleient am ddidyniadau benthyciadau myfyrwyr."
    override val checkboxHint: String = "Dewiswch bob un sy’n berthnasol."
    override val checkboxUgl: String = "Iawn, ad-daliadau israddedig"
    override val checkboxUglHint: String = "Mae hyn yn cynnwys cyrsiau fel graddau israddedig (BA, BSc), graddau sylfaen neu Dystysgrifau Addysg Uwch (CertHE)."
    override val checkboxPgl: String = "Iawn, ad-daliadau ôl-raddedig"
    override val checkboxPglHint: String = "Mae hyn yn cynnwys cyrsiau fel graddau meistr neu ddoethuriaeth."
    override val checkboxNo: String = "Na"
    override val buttonText: String = "Cadw ac yn eich blaen"
    override val errorEmpty: String = "Dewiswch y mathau o fenthyciad myfyriwr a ad-dalwyd gan eich cleient, neu dewiswch \"Na\""
    override val errorAll: String = "Dewiswch y mathau o fenthyciad myfyriwr a ad-dalwyd gan eich cleient, neu dewiswch \"Na\""
    override val expectedErrorTitle: String = s"Gwall: $title"
  }

  object Selectors {
    val paragraphSelector = "#main-content > div > div > p:nth-child(2)"
    val paragraphSelector_2 = "#main-content > div > div > p:nth-child(3)"
    val paragraphSelectorError = "#main-content > div > div > p:nth-child(3)"
    val paragraphSelectorError_2 = "#main-content > div > div > p:nth-child(4)"
    val checkboxHint = "#studentLoans-hint"

    val checkboxUgl = "#studentLoans"
    val checkboxUglText = "#main-content > div > div > form > div > div.govuk-checkboxes > div:nth-child(1) > label"
    val checkboxUglHint = "#studentLoans-item-hint"

    val checkboxPgl = "#studentLoans-2"
    val checkboxPglText = "#main-content > div > div > form > div > div.govuk-checkboxes > div:nth-child(2) > label"
    val checkboxPglHint = "#studentLoans-2-item-hint"

    val checkboxN0 = "#studentLoans-4"
    val checkboxN0Text = "#main-content > div > div > form > div > div.govuk-checkboxes > div:nth-child(4) > label"

    val checkboxHref = "#studentLoans"
  }

  override val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedResultsEnglish),
    UserScenario(isWelsh = false, isAgent = true, ExpectedResultsEnglishAgent),
    UserScenario(isWelsh = true, isAgent = false, ExpectedResultsWelsh),
    UserScenario(isWelsh = true, isAgent = true, ExpectedResultsWelshAgent)
  )

  private def form(isAgent: Boolean): Form[StudentLoansQuestionModel] = StudentLoanQuestionForm.studentLoanForm(isAgent)

  private lazy val underTest = inject[StudentLoansQuestionView]

  userScenarios.foreach { scenarioData =>

    import scenarioData.commonExpectedResults._
    s"The language is ${welshTest(scenarioData.isWelsh)} and the request is from an ${scenarioData.isAgent}" should {

      "render the loans question page" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employmentName, form(scenarioData.isAgent))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(title, scenarioData.isWelsh)
        h1Check(heading)
        captionCheck(caption)
        textOnPageCheck(paragraphText_1, Selectors.paragraphSelector)
        textOnPageCheck(paragraphText_2, Selectors.paragraphSelector_2)
        hintTextCheck(checkboxHint, Selectors.checkboxHint)
        textOnPageCheck(checkboxUgl, Selectors.checkboxUglText)
        inputFieldValueCheck("studentLoans[]", Selectors.checkboxUgl, "ugl")
        hintTextCheck(checkboxUglHint, Selectors.checkboxUglHint)
        textOnPageCheck(checkboxPgl, Selectors.checkboxPglText)
        inputFieldValueCheck("studentLoans[]", Selectors.checkboxPgl, "pgl")
        hintTextCheck(checkboxPglHint, Selectors.checkboxPglHint)
        textOnPageCheck(checkboxNo, Selectors.checkboxN0Text)
        inputFieldValueCheck("studentLoans[]", Selectors.checkboxN0, "none")
        buttonCheck(buttonText)
      }

      "render the loans question page when the form has been prefilled with ugl and pgl values" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employmentName, form(scenarioData.isAgent), Some(StudentLoansCYAModel(uglDeduction = true, pglDeduction = true)))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(title, scenarioData.isWelsh)
        h1Check(heading)
        captionCheck(caption)
        textOnPageCheck(paragraphText_1, Selectors.paragraphSelector)
        textOnPageCheck(paragraphText_2, Selectors.paragraphSelector_2)
        hintTextCheck(checkboxHint, Selectors.checkboxHint)
        textOnPageCheck(checkboxUgl, Selectors.checkboxUglText)
        hintTextCheck(checkboxUglHint, Selectors.checkboxUglHint)
        checkBoxCheck("studentLoans[]", Selectors.checkboxUgl, value = "ugl", checked = true)
        textOnPageCheck(checkboxPgl, Selectors.checkboxPglText)
        checkBoxCheck("studentLoans[]", Selectors.checkboxPgl, value = "pgl", checked = true)
        hintTextCheck(checkboxPglHint, Selectors.checkboxPglHint)
        textOnPageCheck(checkboxNo, Selectors.checkboxN0Text)
        checkBoxCheck("studentLoans[]", Selectors.checkboxN0, value = "none", checked = false)
        buttonCheck(buttonText)
      }

      "render the loans question page when the form has been prefilled with the No value" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employmentName, form(scenarioData.isAgent), Some(StudentLoansCYAModel(uglDeduction = false, pglDeduction = false)))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(title, scenarioData.isWelsh)
        h1Check(heading)
        captionCheck(caption)
        textOnPageCheck(paragraphText_1, Selectors.paragraphSelector)
        textOnPageCheck(paragraphText_2, Selectors.paragraphSelector_2)
        hintTextCheck(checkboxHint, Selectors.checkboxHint)
        textOnPageCheck(checkboxUgl, Selectors.checkboxUglText)
        hintTextCheck(checkboxUglHint, Selectors.checkboxUglHint)
        checkBoxCheck("studentLoans[]", Selectors.checkboxUgl, value = "ugl", checked = false)
        textOnPageCheck(checkboxPgl, Selectors.checkboxPglText)
        checkBoxCheck("studentLoans[]", Selectors.checkboxPgl, value = "pgl", checked = false)
        hintTextCheck(checkboxPglHint, Selectors.checkboxPglHint)
        textOnPageCheck(checkboxNo, Selectors.checkboxN0Text)
        checkBoxCheck("studentLoans[]", Selectors.checkboxN0, value = "none", checked = true)
        buttonCheck(buttonText)
      }

      "render the loans question page with errors when the form is empty" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employmentName, form(scenarioData.isAgent).bind(Map("studentLoans[]" -> "")))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedErrorTitle, scenarioData.isWelsh)
        h1Check(heading)
        captionCheck(caption)
        textOnPageCheck(paragraphText_1, Selectors.paragraphSelectorError)
        textOnPageCheck(paragraphText_2, Selectors.paragraphSelectorError_2)
        hintTextCheck(checkboxHint, Selectors.checkboxHint)
        textOnPageCheck(checkboxUgl, Selectors.checkboxUglText)
        hintTextCheck(checkboxUglHint, Selectors.checkboxUglHint)
        checkBoxCheck("studentLoans[]", Selectors.checkboxUgl, value = "ugl", checked = false)
        textOnPageCheck(checkboxPgl, Selectors.checkboxPglText)
        checkBoxCheck("studentLoans[]", Selectors.checkboxPgl, value = "pgl", checked = false)
        hintTextCheck(checkboxPglHint, Selectors.checkboxPglHint)
        textOnPageCheck(checkboxNo, Selectors.checkboxN0Text)
        checkBoxCheck("studentLoans[]", Selectors.checkboxN0, value = "none", checked = false)
        buttonCheck(buttonText)
        errorSummaryCheck(errorEmpty, Selectors.checkboxHref)
        errorAboveElementCheck(errorEmpty)
      }

      "render the loans question page with errors when the form contains all the options" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearEOY, employmentId, employmentName, form(scenarioData.isAgent).bind(Map("studentLoans[]" -> Seq("ugl", "pgl", "none").toString())))

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedErrorTitle, scenarioData.isWelsh)
        h1Check(heading)
        captionCheck(caption)
        textOnPageCheck(paragraphText_1, Selectors.paragraphSelectorError)
        textOnPageCheck(paragraphText_2, Selectors.paragraphSelectorError_2)
        hintTextCheck(checkboxHint, Selectors.checkboxHint)
        textOnPageCheck(checkboxUgl, Selectors.checkboxUglText)
        hintTextCheck(checkboxUglHint, Selectors.checkboxUglHint)
        checkBoxCheck("studentLoans[]", Selectors.checkboxUgl, value = "ugl", checked = false)
        textOnPageCheck(checkboxPgl, Selectors.checkboxPglText)
        checkBoxCheck("studentLoans[]", Selectors.checkboxPgl, value = "pgl", checked = false)
        hintTextCheck(checkboxPglHint, Selectors.checkboxPglHint)
        textOnPageCheck(checkboxNo, Selectors.checkboxN0Text)
        checkBoxCheck("studentLoans[]", Selectors.checkboxN0, value = "none", checked = false)
        buttonCheck(buttonText)
        errorSummaryCheck(errorAll, Selectors.checkboxHref)
        errorAboveElementCheck(errorAll)
      }
    }
  }
}
