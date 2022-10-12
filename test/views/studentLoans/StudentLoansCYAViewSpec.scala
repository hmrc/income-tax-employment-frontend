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

import models.AuthorisationRequest
import models.employment.StudentLoansCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.studentLoans.StudentLoansCYAView

class StudentLoansCYAViewSpec extends ViewUnitTest {

  val appUrl = "/update-and-submit-income-tax-return/employment-income"

  def checkYourDetailsUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/check-employment-details?employmentId=$employmentId"

  def pglAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/student-loans/postgraduate-repayment-amount?employmentId=$employmentId"

  def studentLoansQuestionPage(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/student-loans/repayments?employmentId=$employmentId"

  def studentLoansUglAmountUrl(taxYear: Int, employmentId: String): String = s"$appUrl/$taxYear/student-loans/undergraduate-repayment-amount?employmentId=$employmentId"

  private val employmentId: String = "1234567890-0987654321"

  trait CommonExpectedResults {
    val isEndOfYear: Boolean
    val hasPrior: Boolean
    val bannerParagraph: String
    val bannerLinkText: String
    val title: String
    val caption: String
    val paragraphText: String

    val questionStudentLoan: String
    val questionUndergraduateAmount: String
    val questionPostGraduateAmount: String

    val answerStudentLoan: String

    val hiddenTextStudentLoan: String
    val hiddenTextUndergraduate: String
    val hiddenTextPostgraduate: String

    val insetText: String

    val buttonText: String
  }

  object ExpectedResultsEnglishEOY extends CommonExpectedResults {
    override val isEndOfYear: Boolean = true
    override val hasPrior: Boolean = true
    override val bannerParagraph: String = "You cannot update student loans until you add missing employment details."
    override val bannerLinkText: String = "add missing employment details."
    override val title: String = "Check your student loan repayment details"
    override lazy val caption: String = s"Student loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val paragraphText: String = "Your student loan repayment details are based on the information we already hold about you."

    override val questionStudentLoan = "Student loan repayments"
    override val questionUndergraduateAmount = "Undergraduate repayments amount"
    override val questionPostGraduateAmount = "Postgraduate repayments amount"

    override val answerStudentLoan = "Undergraduate and Postgraduate"

    override val hiddenTextStudentLoan: String = "Change Change Student loan repayments"
    override val hiddenTextUndergraduate: String = "Change Change Undergraduate repayments amount"
    override val hiddenTextPostgraduate: String = "Change Change Postgraduate repayments amount"

    override val insetText: String = "NOT IMPLEMENTED"
    override val buttonText: String = "Save and continue"
  }

  object ExpectedResultsEnglishInYear extends CommonExpectedResults {
    override lazy val isEndOfYear: Boolean = false
    override lazy val hasPrior: Boolean = false

    override val bannerParagraph: String = ""
    override val bannerLinkText: String = ""
    override lazy val title: String = "Check your student loan repayment details"
    override lazy val caption: String = s"Student loans for 6 April ${taxYear - 1} to 5 April $taxYear"
    override lazy val paragraphText: String = "Your student loan repayment details are based on the information we already hold about you."

    override lazy val questionStudentLoan = "Student loan repayments"
    override lazy val questionUndergraduateAmount = "Undergraduate repayments amount"
    override lazy val questionPostGraduateAmount = "Postgraduate repayments amount"

    override lazy val answerStudentLoan = "Undergraduate and Postgraduate"

    override lazy val hiddenTextStudentLoan: String = "Change Change Student loan repayments"
    override lazy val hiddenTextUndergraduate: String = "Change Change Undergraduate repayments amount"
    override lazy val hiddenTextPostgraduate: String = "Change Change Postgraduate repayments amount"

    override lazy val insetText: String = s"You cannot update your student loan details until 6 April $taxYear."
    override lazy val buttonText: String = "Return to employer"
  }

  object ExpectedResultsEnglishEOYAgent extends CommonExpectedResults {
    override val isEndOfYear: Boolean = true
    override val hasPrior: Boolean = true
    override val bannerParagraph: String = "You cannot update student loans until you add missing employment details."
    override val bannerLinkText: String = "add missing employment details."
    override val title: String = "Check your client’s student loan repayment details"
    override lazy val caption: String = s"Student loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val paragraphText: String = "Your client’s student loan repayment details are based on the information we already hold about them."

    override val questionStudentLoan = "Student loan repayments"
    override val questionUndergraduateAmount = "Undergraduate repayments amount"
    override val questionPostGraduateAmount = "Postgraduate repayments amount"

    override val answerStudentLoan = "Undergraduate and Postgraduate"

    override val hiddenTextStudentLoan: String = "Change Change Student loan repayments"
    override val hiddenTextUndergraduate: String = "Change Change Undergraduate repayments amount"
    override val hiddenTextPostgraduate: String = "Change Change Postgraduate repayments amount"

    override val insetText: String = "NOT IMPLEMENTED"
    override val buttonText: String = "Save and continue"
  }

  object ExpectedResultsEnglishInYearAgent extends CommonExpectedResults {
    override lazy val isEndOfYear: Boolean = false
    override lazy val hasPrior: Boolean = false

    override val bannerParagraph: String = ""
    override val bannerLinkText: String = ""
    override lazy val title: String = "Check your client’s student loan repayment details"
    override lazy val caption: String = s"Student loans for 6 April ${taxYear - 1} to 5 April $taxYear"
    override lazy val paragraphText: String = "Your client’s student loan repayment details are based on the information we already hold about them."

    override lazy val questionStudentLoan = "Student loan repayments"
    override lazy val questionUndergraduateAmount = "Undergraduate repayments amount"
    override lazy val questionPostGraduateAmount = "Postgraduate repayments amount"

    override lazy val answerStudentLoan = "Undergraduate and Postgraduate"

    override lazy val hiddenTextStudentLoan: String = "Change Change Student loan repayments"
    override lazy val hiddenTextUndergraduate: String = "Change Change Undergraduate repayments amount"
    override lazy val hiddenTextPostgraduate: String = "Change Change Postgraduate repayments amount"

    override lazy val insetText: String = s"You cannot update your client’s student loan details until 6 April $taxYear."
    override lazy val buttonText: String = "Return to employer"
  }

  object ExpectedResultsWelshEOY extends CommonExpectedResults {
    override val isEndOfYear: Boolean = true
    override val hasPrior: Boolean = true
    override val bannerParagraph: String = "Ni allwch ddiweddaru benthyciadau myfyrwyr hyd nes eich bod yn ychwanegu manylion cyflogaeth sydd ar goll."
    override val bannerLinkText: String = "ychwanegu manylion cyflogaeth sydd ar goll."
    override val title: String = "Gwiriwch fanylion ad-dalu’ch benthyciad myfyriwr"
    override lazy val caption: String = s"Benthyciadau Myfyrwyr ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    override val paragraphText: String = "Mae’ch manylion ad-dalu benthyciad myfyriwr yn seiliedig ar yr wybodaeth sydd eisoes gennym amdanoch."

    override val questionStudentLoan = "Ad-daliadau Benthyciad Myfyriwr"
    override val questionUndergraduateAmount = "Swm ad-daliadau israddedig"
    override val questionPostGraduateAmount = "Swm ad-daliadau ôl-raddedig"

    override val answerStudentLoan = "Israddedig a Ôl-raddedig"

    override val hiddenTextStudentLoan: String = "Newid Newid Ad-daliadau Benthyciad Myfyriwr"
    override val hiddenTextUndergraduate: String = "Newid Newid Swm ad-daliadau israddedig"
    override val hiddenTextPostgraduate: String = "Newid Newid Swm ad-daliadau ôl-raddedig"

    override val insetText: String = "NOT IMPLEMENTED"
    override val buttonText: String = "Cadw ac yn eich blaen"
  }

  object ExpectedResultsWelshInYear extends CommonExpectedResults {
    override lazy val isEndOfYear: Boolean = false
    override lazy val hasPrior: Boolean = false

    override val bannerParagraph: String = ""
    override val bannerLinkText: String = ""
    override lazy val title: String = "Gwiriwch fanylion ad-dalu’ch benthyciad myfyriwr"
    override lazy val caption: String = s"Benthyciadau Myfyrwyr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override lazy val paragraphText: String = "Mae’ch manylion ad-dalu benthyciad myfyriwr yn seiliedig ar yr wybodaeth sydd eisoes gennym amdanoch."

    override val questionStudentLoan = "Ad-daliadau Benthyciad Myfyriwr"
    override val questionUndergraduateAmount = "Swm ad-daliadau israddedig"
    override val questionPostGraduateAmount = "Swm ad-daliadau ôl-raddedig"

    override lazy val answerStudentLoan = "Israddedig a Ôl-raddedig"

    override val hiddenTextStudentLoan: String = "Newid Newid Ad-daliadau Benthyciad Myfyriwr"
    override val hiddenTextUndergraduate: String = "Newid Newid Swm ad-daliadau israddedig"
    override val hiddenTextPostgraduate: String = "Newid Newid Swm ad-daliadau ôl-raddedig"

    override lazy val insetText: String = s"Ni allwch ddiweddaru’ch manylion benthyciad myfyriwr tan 6 Ebrill $taxYear."
    override lazy val buttonText: String = "Dychwelyd at y cyflogwr"
  }

  object ExpectedResultsWelshEOYAgent extends CommonExpectedResults {
    override val isEndOfYear: Boolean = true
    override val hasPrior: Boolean = true
    override val bannerParagraph: String = "Ni allwch ddiweddaru benthyciadau myfyrwyr hyd nes eich bod yn ychwanegu manylion cyflogaeth sydd ar goll."
    override val bannerLinkText: String = "ychwanegu manylion cyflogaeth sydd ar goll."
    override val title: String = "Gwiriwch fanylion ad-dalu benthyciad myfyriwr eich cleient"
    override lazy val caption: String = s"Benthyciadau Myfyrwyr ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    override val paragraphText: String = "Mae manylion ad-dalu benthyciad myfyriwr eich cleient yn seiliedig ar yr wybodaeth sydd eisoes gennym amdano."

    override val questionStudentLoan = "Ad-daliadau Benthyciad Myfyriwr"
    override val questionUndergraduateAmount = "Swm ad-daliadau israddedig"
    override val questionPostGraduateAmount = "Swm ad-daliadau ôl-raddedig"

    override val answerStudentLoan = "Israddedig a Ôl-raddedig"

    override val hiddenTextStudentLoan: String = "Newid Newid Ad-daliadau Benthyciad Myfyriwr"
    override val hiddenTextUndergraduate: String = "Newid Newid Swm ad-daliadau israddedig"
    override val hiddenTextPostgraduate: String = "Newid Newid Swm ad-daliadau ôl-raddedig"

    override val insetText: String = "NOT IMPLEMENTED"
    override val buttonText: String = "Cadw ac yn eich blaen"
  }

  object ExpectedResultsWelshInYearAgent extends CommonExpectedResults {
    override lazy val isEndOfYear: Boolean = false
    override lazy val hasPrior: Boolean = false

    override val bannerParagraph: String = ""
    override val bannerLinkText: String = ""
    override lazy val title: String = "Gwiriwch fanylion ad-dalu benthyciad myfyriwr eich cleient"
    override lazy val caption: String = s"Benthyciadau Myfyrwyr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override lazy val paragraphText: String = "Mae manylion ad-dalu benthyciad myfyriwr eich cleient yn seiliedig ar yr wybodaeth sydd eisoes gennym amdano."

    override val questionStudentLoan = "Ad-daliadau Benthyciad Myfyriwr"
    override val questionUndergraduateAmount = "Swm ad-daliadau israddedig"
    override val questionPostGraduateAmount = "Swm ad-daliadau ôl-raddedig"

    override lazy val answerStudentLoan = "Israddedig a Ôl-raddedig"

    override val hiddenTextStudentLoan: String = "Newid Newid Ad-daliadau Benthyciad Myfyriwr"
    override val hiddenTextUndergraduate: String = "Newid Newid Swm ad-daliadau israddedig"
    override val hiddenTextPostgraduate: String = "Newid Newid Swm ad-daliadau ôl-raddedig"

    override lazy val insetText: String = s"Ni allwch ddiweddaru manylion benthyciad myfyriwr eich cleient tan 6 Ebrill $taxYear."
    override lazy val buttonText: String = "Dychwelyd at y cyflogwr"
  }

  object Selectors {
    val bannerParagraphSelector: String = ".govuk-notification-banner__heading"
    val bannerLinkSelector: String = ".govuk-notification-banner__link"
    val paragraphSelector = ".govuk-body"
    val insetText: String = ".govuk-inset-text"

    def column1Selector(row: Int): String = s".govuk-summary-list__row:nth-of-type($row) > .govuk-summary-list__key"

    def column2Selector(row: Int): String = s".govuk-summary-list__row:nth-of-type($row) > .govuk-summary-list__value"

    def column3Selector(row: Int): String = s".govuk-summary-list__row:nth-of-type($row) > .govuk-summary-list__actions > .govuk-link"
  }

  override val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedResultsEnglishEOY),
    UserScenario(isWelsh = false, isAgent = false, ExpectedResultsEnglishInYear),
    UserScenario(isWelsh = false, isAgent = true, ExpectedResultsEnglishEOYAgent),
    UserScenario(isWelsh = false, isAgent = true, ExpectedResultsEnglishInYearAgent),
    UserScenario(isWelsh = true, isAgent = false, ExpectedResultsWelshEOY),
    UserScenario(isWelsh = true, isAgent = false, ExpectedResultsWelshInYear),
    UserScenario(isWelsh = true, isAgent = true, ExpectedResultsWelshEOYAgent),
    UserScenario(isWelsh = true, isAgent = true, ExpectedResultsWelshInYearAgent)
  )

  private def answerRowInYearOrEndOfYear(keyName: String, value: String, hiddenText: String, href: String, row: Int, isEndOfYear: Boolean)(implicit document: Document): Unit = {
    if (isEndOfYear) {
      changeAmountRowCheck(
        keyName,
        value,
        Selectors.column1Selector(row),
        Selectors.column2Selector(row),
        Selectors.column3Selector(row),
        hiddenText, href
      )
    } else {
      textOnPageCheck(keyName, Selectors.column1Selector(row))
      textOnPageCheck(value, Selectors.column2Selector(row))
      s"and the third column for '$keyName' does not exist" in {
        elementExist(Selectors.column3Selector(row)) shouldBe false
      }
    }
  }

  private lazy val underTest = inject[StudentLoansCYAView]

  userScenarios.foreach { scenarioData =>
    val inYearText = if (scenarioData.commonExpectedResults.isEndOfYear) "end of year" else "in year"
    val affinityText = if (scenarioData.isAgent) "agent" else "individual"
    val prior = if (scenarioData.commonExpectedResults.hasPrior) "prior data" else "no prior data"
    val taxYearInUse = if (scenarioData.commonExpectedResults.isEndOfYear) taxYearEOY else taxYear
    val welshLang = if (scenarioData.isWelsh) "Welsh" else "English"
    import scenarioData.commonExpectedResults._

    s"render the page for $inYearText, for an $affinityText when there is $prior in $welshLang" when {
      "there is CYA data in session" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearInUse, employmentId, StudentLoansCYAModel(
          uglDeduction = true, Some(1000.22), pglDeduction = true, Some(3000.22)
        ), isCustomerHeld = false, !isEndOfYear, showNotification = false)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(title, scenarioData.isWelsh)
        h1Check(title)
        captionCheck(caption)
        if (hasPrior) textOnPageCheck(paragraphText, Selectors.paragraphSelector)

        answerRowInYearOrEndOfYear(questionStudentLoan, answerStudentLoan, hiddenTextStudentLoan, studentLoansQuestionPage(taxYearInUse, employmentId), 1, isEndOfYear)
        answerRowInYearOrEndOfYear(questionUndergraduateAmount, "£1,000.22", hiddenTextUndergraduate, studentLoansUglAmountUrl(taxYearInUse, employmentId), 2, isEndOfYear)
        answerRowInYearOrEndOfYear(questionPostGraduateAmount, "£3,000.22", hiddenTextPostgraduate, pglAmountUrl(taxYearInUse, employmentId), 3, isEndOfYear)

        if (!isEndOfYear) {
          textOnPageCheck(insetText, Selectors.insetText)
        }

        buttonCheck(buttonText)
      }
    }

    if (scenarioData.commonExpectedResults.isEndOfYear && scenarioData.commonExpectedResults.hasPrior) {
      s"render page for EOY when not submittable for an $affinityText in $welshLang" when {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(scenarioData.isAgent)
        implicit val messages: Messages = getMessages(scenarioData.isWelsh)

        val htmlFormat = underTest(taxYearInUse, employmentId, StudentLoansCYAModel(
          uglDeduction = true, Some(100), pglDeduction = true, Some(200)
        ), isCustomerHeld = false, !isEndOfYear, showNotification = true)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        "has a Notification banner" which {
          textOnPageCheck(scenarioData.commonExpectedResults.bannerParagraph, Selectors.bannerParagraphSelector)
          linkCheck(scenarioData.commonExpectedResults.bannerLinkText, Selectors.bannerLinkSelector, checkYourDetailsUrl(taxYearEOY, employmentId))
        }

        titleCheck(title, scenarioData.isWelsh)
        h1Check(title)
        captionCheck(caption)
        if (hasPrior) textOnPageCheck(paragraphText, Selectors.paragraphSelector)

        textOnPageCheck(questionStudentLoan, Selectors.column1Selector(1))
        textOnPageCheck(answerStudentLoan, Selectors.column2Selector(1))
        textOnPageCheck(questionUndergraduateAmount, Selectors.column1Selector(2))
        textOnPageCheck("£100", Selectors.column2Selector(2))
        textOnPageCheck(questionPostGraduateAmount, Selectors.column1Selector(3))
        textOnPageCheck("£200", Selectors.column2Selector(3))

        if (!isEndOfYear) {
          buttonCheck(buttonText)
        }
      }
    }
  }
}
