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

package controllers.studentLoans

import forms.studentLoans.StudentLoanQuestionForm
import models.IncomeTaxUserData
import models.employment._
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class StudentLoansQuestionControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {


  val employmentId: String = "1234567890-0987654321"
  val startDate = s"$taxYear-04-01"

  def url(taxYearUnique: Int): String = fullUrl(studentLoansQuestionPage(taxYearUnique, employmentId))

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
  }

  object ExpectedResultsEnglish extends CommonExpectedResults {
    override val title: String = "Did you repay any student loan?"
    override val heading: String = "Did you repay any student loan while employed by Whiterun Guards?"
    override val caption: String = s"Student loans for 6 April ${taxYear - 1} to 5 April $taxYear"
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
  }


  object ExpectedResultsEnglishAgent extends CommonExpectedResults {
    override val title: String = "Did your client repay any student loan?"
    override val heading: String = "Did your client repay any student loan while employed by Whiterun Guards?"
    override val caption: String = s"Student loans for 6 April ${taxYear - 1} to 5 April $taxYear"
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
  }


  object Selectors {
    val paragraphSelector = "#main-content > div > div > p:nth-child(2)"
    val paragraphSelector_2 = "#main-content > div > div > p:nth-child(3)"
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
  )


  ".show" should {

    "immediately redirect the user to the overview page" when {

      "the student loans feature switch is off" in {

        val request = FakeRequest("GET", studentLoansQuestionPage(taxYear, employmentId)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear))

        val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }

        status(result) shouldBe SEE_OTHER
        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
      "a user has no cyadata" in {

        val result = {
          dropEmploymentDB()
          authoriseIndividual()
          userDataStub(IncomeTaxUserData(), nino, taxYear)

          urlGet(url(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").headOption shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    userScenarios.foreach { scenarioData =>
      import scenarioData.commonExpectedResults._
      s"render the page for isAgent: ${scenarioData.isAgent} isWelsh: ${scenarioData.isWelsh} content" when {
        "there is cya data in session with previous studentLoans" which {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
                          sessionId,
                          mtditid,
                          nino,
                          taxYear,
                          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                          EmploymentCYAModel(
                            EmploymentDetails(
                              employerName = "Whiterun Guards",
                              employerRef = Some("223/AB12399"),
                              startDate = Some(startDate),
                              didYouLeaveQuestion = Some(false),
                              taxablePayToDate = Some(3000.00),
                              totalTaxToDate = Some(300.00),
                              currentDataIsHmrcHeld = false
                            ),
                            studentLoans = Some(StudentLoansCYAModel(
                              uglDeduction = true, Some(1000.22), pglDeduction = true, Some(3000.22)
                            ))
                          )
                        ))
            userDataStub(IncomeTaxUserData(), nino, taxYear)


            urlGet(url(taxYear), scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(title)
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
      }
    }
    "render the page" when {
      "there is cya in session without studentLoans" which {
        lazy val result = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(EmploymentUserData(
                      sessionId,
                      mtditid,
                      nino,
                      taxYear,
                      employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                      EmploymentCYAModel(
                        EmploymentDetails(
                          employerName = "Whiterun Guards",
                          employerRef = Some("223/AB12399"),
                          startDate = Some(startDate),
                          didYouLeaveQuestion = Some(false),
                          taxablePayToDate = Some(3000.00),
                          totalTaxToDate = Some(300.00),
                          currentDataIsHmrcHeld = false
                        ),
                      )
                    ))

          urlGet(url(taxYear), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ExpectedResultsEnglish.title)
      }
    }

  }

  ".submit" should {

    "immediately redirect the user to the overview page" when {

      "The user is taken to the overview page when the student loans feature switch is off" in {
        val headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck")


        val request = FakeRequest("POST", studentLoansQuestionPage(taxYear, employmentId)).withHeaders(headers: _*)

        val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }

        status(result) shouldBe SEE_OTHER
        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)

      }
      "there is no cya data in session and the form is valid" in {

        lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq("ugl"))

        lazy val result = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(), nino, taxYear)

          urlPost(url(taxYear), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

      }

    }

    "redirect and update cya data correctly" when {
      "there is cya data in session with previous amounts" in {
        lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq("ugl"))
        lazy val result = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(EmploymentUserData(
                      sessionId,
                      mtditid,
                      nino,
                      taxYear,
                      employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                      EmploymentCYAModel(
                        EmploymentDetails(
                          employerName = "Whiterun Guards",
                          employerRef = Some("223/AB12399"),
                          startDate = Some(startDate),
                          didYouLeaveQuestion = Some(false),
                          taxablePayToDate = Some(3000.00),
                          totalTaxToDate = Some(300.00),
                          currentDataIsHmrcHeld = false
                        ),
                        studentLoans = Some(StudentLoansCYAModel(
                          uglDeduction = true, Some(1000.22), pglDeduction = true, Some(3000.22)
                        ))
                      )
                    ))
          userDataStub(IncomeTaxUserData(), nino, taxYear)

          urlPost(url(taxYear), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(studentLoansCyaPage(taxYear, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYear, employmentId, anAuthorisationRequest).get
        cyaModel.employment.studentLoans.get shouldBe
          StudentLoansCYAModel(uglDeduction = true, Some(1000.22), pglDeduction = false, None)

      }
      "there is cya data in session without StudentLoans (ugl)" in {
        lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq("ugl"))
        lazy val result = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(EmploymentUserData(
                      sessionId,
                      mtditid,
                      nino,
                      taxYear,
                      employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                      EmploymentCYAModel(
                        EmploymentDetails(
                          employerName = "Whiterun Guards",
                          employerRef = Some("223/AB12399"),
                          startDate = Some(startDate),
                          didYouLeaveQuestion = Some(false),
                          taxablePayToDate = Some(3000.00),
                          totalTaxToDate = Some(300.00),
                          currentDataIsHmrcHeld = false
                        )
                      )
                    ))
          userDataStub(IncomeTaxUserData(), nino, taxYear)

          urlPost(url(taxYear), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(studentLoansUglAmountUrl(taxYear, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYear, employmentId, anAuthorisationRequest).get
        cyaModel.employment.studentLoans.get shouldBe
          StudentLoansCYAModel(uglDeduction = true, None, pglDeduction = false, None)

      }
      "there is cya data in session without StudentLoans (pgl)" in {
        lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq("pgl"))
        lazy val result = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(EmploymentUserData(
                      sessionId,
                      mtditid,
                      nino,
                      taxYear,
                      employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                      EmploymentCYAModel(
                        EmploymentDetails(
                          employerName = "Whiterun Guards",
                          employerRef = Some("223/AB12399"),
                          startDate = Some(startDate),
                          didYouLeaveQuestion = Some(false),
                          taxablePayToDate = Some(3000.00),
                          totalTaxToDate = Some(300.00),
                          currentDataIsHmrcHeld = false
                        )
                      )
                    ))
          userDataStub(IncomeTaxUserData(), nino, taxYear)
          urlPost(url(taxYear), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(pglAmountUrl(taxYear, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYear, employmentId, anAuthorisationRequest).get
        cyaModel.employment.studentLoans.get shouldBe
          StudentLoansCYAModel(uglDeduction = false, None, pglDeduction = true, None)

      }
      "there is cya data in session without StudentLoans (none)" in {
        lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq("none"))
        lazy val result = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(EmploymentUserData(
                      sessionId,
                      mtditid,
                      nino,
                      taxYear,
                      employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                      EmploymentCYAModel(
                        EmploymentDetails(
                          employerName = "Whiterun Guards",
                          employerRef = Some("223/AB12399"),
                          startDate = Some(startDate),
                          didYouLeaveQuestion = Some(false),
                          taxablePayToDate = Some(3000.00),
                          totalTaxToDate = Some(300.00),
                          currentDataIsHmrcHeld = false
                        )
                      )
                    ))
          userDataStub(IncomeTaxUserData(), nino, taxYear)


          urlPost(url(taxYear), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(studentLoansCyaPage(taxYear, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYear, employmentId, anAuthorisationRequest).get
        cyaModel.employment.studentLoans.get shouldBe
          StudentLoansCYAModel(uglDeduction = false, None, pglDeduction = false, None)

      }
      "there is cya data in session without StudentLoans (ugl, pgl)" in {
        lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq("ugl", "pgl"))
        lazy val result = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(EmploymentUserData(
                      sessionId,
                      mtditid,
                      nino,
                      taxYear,
                      employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                      EmploymentCYAModel(
                        EmploymentDetails(
                          employerName = "Whiterun Guards",
                          employerRef = Some("223/AB12399"),
                          startDate = Some(startDate),
                          didYouLeaveQuestion = Some(false),
                          taxablePayToDate = Some(3000.00),
                          totalTaxToDate = Some(300.00),
                          currentDataIsHmrcHeld = false
                        )
                      )
                    ))
          userDataStub(IncomeTaxUserData(), nino, taxYear)


          urlPost(url(taxYear), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(studentLoansUglAmountUrl(taxYear, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYear, employmentId, anAuthorisationRequest).get
        cyaModel.employment.studentLoans.get shouldBe
          StudentLoansCYAModel(uglDeduction = true, None, pglDeduction = true, None)

      }
    }

    userScenarios.foreach { scenarioData =>
      import scenarioData.commonExpectedResults._
      "return form errors" when {
        s"no option has been selected for isAgent: ${scenarioData.isAgent} isWelsh: ${scenarioData.isWelsh}" which {
          lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq())
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
                          sessionId,
                          mtditid,
                          nino,
                          taxYear,
                          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                          EmploymentCYAModel(
                            EmploymentDetails(
                              employerName = "Whiterun Guards",
                              employerRef = Some("223/AB12399"),
                              startDate = Some(startDate),
                              didYouLeaveQuestion = Some(false),
                              taxablePayToDate = Some(3000.00),
                              totalTaxToDate = Some(300.00),
                              currentDataIsHmrcHeld = false
                            )
                          )
                        ))
            userDataStub(IncomeTaxUserData(), nino, taxYear)
            urlPost(url(taxYear), form, welsh = scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "returns the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(errorPrefix + title)
          errorSummaryCheck(errorEmpty, Selectors.checkboxHref)
          errorAboveElementCheck(errorEmpty)


        }
        s"all options have been selected for isAgent: ${scenarioData.isAgent} isWelsh: ${scenarioData.isWelsh}" which {
          lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq("ugl", "pgl", "none"))
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
                          sessionId,
                          mtditid,
                          nino,
                          taxYear,
                          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                          EmploymentCYAModel(
                            EmploymentDetails(
                              employerName = "Whiterun Guards",
                              employerRef = Some("223/AB12399"),
                              startDate = Some(startDate),
                              didYouLeaveQuestion = Some(false),
                              taxablePayToDate = Some(3000.00),
                              totalTaxToDate = Some(300.00),
                              currentDataIsHmrcHeld = false
                            )
                          )
                        ))
            userDataStub(IncomeTaxUserData(), nino, taxYear)


            urlPost(url(taxYear), form, welsh = scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "returns the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(errorPrefix + title)
          errorSummaryCheck(errorEmpty, Selectors.checkboxHref)
          errorAboveElementCheck(errorEmpty)


        }
      }
    }

  }


}
