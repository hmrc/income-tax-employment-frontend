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

import common.SessionValues
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import models.{IncomeTaxUserData, User}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class StudentLoansCYAControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val employmentId: String = "1234567890-0987654321"

  def url(taxYearUnique: Int): String = fullUrl(studentLoansCyaPage(taxYearUnique, employmentId))

  val default = "{}"

  private def stubEmploymentPost(request: CreateUpdateEmploymentRequest, taxYear: Int, employmentIdResponse: String = default) =
    stubPostWithHeadersCheck(
      s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
      if (employmentIdResponse == default) NO_CONTENT else CREATED,
      Json.toJson(request).toString(),
      employmentIdResponse,
      "X-Session-ID" -> sessionId,
      "mtditid" -> mtditid
    )

  private def stubEmploymentPostFailure(request: CreateUpdateEmploymentRequest, taxYear: Int) = stubPostWithResponseBody(
    s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
    INTERNAL_SERVER_ERROR,
    Json.toJson(request).toString(),
    "{}"
  )

  trait CommonExpectedResults {
    val isEndOfYear: Boolean
    val taxYear: Int
    val hasPrior: Boolean
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
    override val taxYear: Int = 2021
    override val hasPrior: Boolean = true
    override val title: String = "Check your student loan repayment details"
    override val caption: String = "Student Loans for 6 April 2020 to 5 April 2021"
    override val paragraphText: String = "Your student loan repayment details are based on the information we already hold about you."

    override val questionStudentLoan = "Student loan repayments?"
    override val questionUndergraduateAmount = "Undergraduate repayments amount"
    override val questionPostGraduateAmount = "Postgraduate repayments amount"

    override val answerStudentLoan = "Undergraduate and Postgraduate"

    override val hiddenTextStudentLoan: String = "Change Change Student loan repayments?"
    override val hiddenTextUndergraduate: String = "Change Change Undergraduate repayments amount"
    override val hiddenTextPostgraduate: String = "Change Change Postgraduate repayments amount"

    override val insetText: String = "NOT IMPLEMENTED"

    override val buttonText: String = "Save and continue"
  }

  object ExpectedResultsEnglishInYear extends CommonExpectedResults {
    override lazy val isEndOfYear: Boolean = false
    override lazy val taxYear: Int = DateTime.now().year().get() + 1
    override lazy val hasPrior: Boolean = false

    override lazy val title: String = "Check your student loan repayment details"
    override lazy val caption: String = s"Student Loans for 6 April ${taxYear - 1} to 5 April $taxYear"
    override lazy val paragraphText: String = "NOT IMPLEMENTED"

    override lazy val questionStudentLoan = "Student loan repayments?"
    override lazy val questionUndergraduateAmount = "Undergraduate repayments amount"
    override lazy val questionPostGraduateAmount = "Postgraduate repayments amount"

    override lazy val answerStudentLoan = "Undergraduate and Postgraduate"

    override lazy val hiddenTextStudentLoan: String = "Change Change Student loan repayments?"
    override lazy val hiddenTextUndergraduate: String = "Change Change Undergraduate repayments amount"
    override lazy val hiddenTextPostgraduate: String = "Change Change Postgraduate repayments amount"

    override lazy val insetText: String = s"You cannot update your student loan details until 6 April $taxYear."

    override lazy val buttonText: String = "Return to employer"
  }

  object ExpectedResultsEnglishEOYAgent extends CommonExpectedResults {
    override val isEndOfYear: Boolean = true
    override val taxYear: Int = 2021
    override val hasPrior: Boolean = true
    override val title: String = "Check your client’s student loan repayment details"
    override val caption: String = "Student Loans for 6 April 2020 to 5 April 2021"
    override val paragraphText: String = "Your client’s student loan repayment details are based on the information we already hold about them."

    override val questionStudentLoan = "Student loan repayments?"
    override val questionUndergraduateAmount = "Undergraduate repayments amount"
    override val questionPostGraduateAmount = "Postgraduate repayments amount"

    override val answerStudentLoan = "Undergraduate and Postgraduate"

    override val hiddenTextStudentLoan: String = "Change Change Student loan repayments?"
    override val hiddenTextUndergraduate: String = "Change Change Undergraduate repayments amount"
    override val hiddenTextPostgraduate: String = "Change Change Postgraduate repayments amount"

    override val insetText: String = "NOT IMPLEMENTED"

    override val buttonText: String = "Save and continue"
  }

  object ExpectedResultsEnglishInYearAgent extends CommonExpectedResults {
    override lazy val isEndOfYear: Boolean = false
    override lazy val taxYear: Int = DateTime.now().year().get() + 1
    override lazy val hasPrior: Boolean = false

    override lazy val title: String = "Check your client’s student loan repayment details"
    override lazy val caption: String = s"Student Loans for 6 April ${taxYear - 1} to 5 April $taxYear"
    override lazy val paragraphText: String = "NOT IMPLEMENTED"

    override lazy val questionStudentLoan = "Student loan repayments?"
    override lazy val questionUndergraduateAmount = "Undergraduate repayments amount"
    override lazy val questionPostGraduateAmount = "Postgraduate repayments amount"

    override lazy val answerStudentLoan = "Undergraduate and Postgraduate"

    override lazy val hiddenTextStudentLoan: String = "Change Change Student loan repayments?"
    override lazy val hiddenTextUndergraduate: String = "Change Change Undergraduate repayments amount"
    override lazy val hiddenTextPostgraduate: String = "Change Change Postgraduate repayments amount"

    override lazy val insetText: String = s"You cannot update your client’s student loan details until 6 April $taxYear."

    override lazy val buttonText: String = "Return to employer"
  }

  object Selectors {
    val paragraphSelector = ".govuk-body"

    def column1Selector(row: Int): String = s".govuk-summary-list__row:nth-of-type($row) > .govuk-summary-list__key"

    def column2Selector(row: Int): String = s".govuk-summary-list__row:nth-of-type($row) > .govuk-summary-list__value"

    def column3Selector(row: Int): String = s".govuk-summary-list__row:nth-of-type($row) > .govuk-summary-list__actions > .govuk-link"

    val insetText: String = ".govuk-inset-text"
  }

  override val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedResultsEnglishEOY),
    UserScenario(isWelsh = false, isAgent = false, ExpectedResultsEnglishInYear),
    UserScenario(isWelsh = false, isAgent = true, ExpectedResultsEnglishEOYAgent),
    UserScenario(isWelsh = false, isAgent = true, ExpectedResultsEnglishInYearAgent)
  )

  private def answerRowInYearOrEndOfYear(keyName: String, value: String, hiddenText: String, href: String, row: Int, isEndOfYear: Boolean)(implicit document: () => Document): Unit = {
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

  ".show" should {

    "immediately redirect the user to the overview page" when {

      "the student loans feature switch is off" in {
        val request = FakeRequest("GET", studentLoansCyaPage(taxYear, employmentId)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear))

        lazy val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }

        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

    }

    userScenarios.foreach { scenarioData =>
      val inYearText = if (scenarioData.commonExpectedResults.isEndOfYear) "end of year" else "in year"
      val affinityText = if (scenarioData.isAgent) "agent" else "individual"
      val prior = if (scenarioData.commonExpectedResults.hasPrior) "prior data" else "no prior data"

      s"render the page for $inYearText, for an $affinityText when there is $prior" when {

        "there is CYA data in session" which {
          import scenarioData.commonExpectedResults._

          def user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId, affinityGroup)(FakeRequest())

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYear,
              employmentId, isPriorSubmission = hasPrior, hasPriorBenefits = false, hasPriorStudentLoans = true,
              EmploymentCYAModel(
                EmploymentDetails(
                  employerName = "Whiterun Guards",
                  employerRef = Some("223/AB12399"),
                  startDate = Some("2022-04-01"),
                  cessationDateQuestion = Some(false),
                  taxablePayToDate = Some(3000.00),
                  totalTaxToDate = Some(300.00),
                  currentDataIsHmrcHeld = false
                ),
                studentLoans = Some(StudentLoansCYAModel(
                  uglDeduction = true, Some(1000.22), pglDeduction = true, Some(3000.22)
                ))
              )
            ), user)
            userDataStub(IncomeTaxUserData(), nino, scenarioData.commonExpectedResults.taxYear)

            urlGet(url(scenarioData.commonExpectedResults.taxYear), scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYear)))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(title)
          h1Check(title)
          captionCheck(caption)
          if (hasPrior) textOnPageCheck(paragraphText, Selectors.paragraphSelector)

          answerRowInYearOrEndOfYear(questionStudentLoan, answerStudentLoan, hiddenTextStudentLoan, studentLoansQuestionPage(scenarioData.commonExpectedResults.taxYear, employmentId), 1, isEndOfYear)
          answerRowInYearOrEndOfYear(questionUndergraduateAmount, "£1,000.22", hiddenTextUndergraduate, studentLoansCyaPage(scenarioData.commonExpectedResults.taxYear, employmentId), 2, isEndOfYear)
          answerRowInYearOrEndOfYear(questionPostGraduateAmount, "£3,000.22", hiddenTextPostgraduate, studentLoansCyaPage(scenarioData.commonExpectedResults.taxYear, employmentId), 3, isEndOfYear)

          if (!isEndOfYear) {
            textOnPageCheck(insetText, Selectors.insetText)
          }

          buttonCheck(buttonText)
        }
      }

      s"redirect the user to the select student loans contributions for $inYearText for an $affinityText when there is $prior" when {

        if (scenarioData.commonExpectedResults.hasPrior) {
          "there is no CYA data but there is Prior data that does not contain Student Loans information" in {
            lazy val result = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(scenarioData.isAgent)
              userDataStub(IncomeTaxUserData(Some(AllEmploymentData(
                Seq(EmploymentSource(
                  employmentId, "Whiterun Guards", None, None, None, None, None, None, Some(EmploymentData(
                    "2022-01-01", None, None, None, None, None, None, None, None
                  )), None
                )), None,
                Seq(), None
              ))), nino, scenarioData.commonExpectedResults.taxYear)

              urlGet(
                url(scenarioData.commonExpectedResults.taxYear), scenarioData.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYear))
              )
            }

            result.headers("Location").headOption shouldBe Some(studentLoansQuestionPage(scenarioData.commonExpectedResults.taxYear, employmentId))
          }
        }

        "there is CYA data but it does not contain Student Loans information" in {
          def user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId, affinityGroup)(FakeRequest())
          
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYear,
              employmentId, isPriorSubmission = true, hasPriorBenefits = false, hasPriorStudentLoans = true,
              EmploymentCYAModel(
                EmploymentDetails(
                  employerName = "Whiterun Guards",
                  employerRef = Some("223/AB12399"),
                  startDate = Some("2022-04-01"),
                  cessationDateQuestion = Some(false),
                  taxablePayToDate = Some(3000.00),
                  totalTaxToDate = Some(300.00),
                  currentDataIsHmrcHeld = false
                ),
                studentLoans = None
              )
            ), user)
            userDataStub(IncomeTaxUserData(Some(AllEmploymentData(
              Seq(EmploymentSource(
                employmentId, "Whiterun Guards", None, None, None, None, None, None, Some(EmploymentData(
                  "2022-01-01", None, None, None, None, None, None, None, None
                )), None
              )), None,
              Seq(), None
            ))), nino, scenarioData.commonExpectedResults.taxYear)

            urlGet(
              url(scenarioData.commonExpectedResults.taxYear), scenarioData.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYear))
            )
          }

          result.headers("Location").headOption shouldBe Some(studentLoansQuestionPage(scenarioData.commonExpectedResults.taxYear, employmentId))
        }
        
      }

      s"redirect the user to the overview page for $inYearText for an $affinityText when there is $prior" when {
        
        "there is no data in session" in {
          val request = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            userDataStub(IncomeTaxUserData(), nino, scenarioData.commonExpectedResults.taxYear)
            urlGet(
              url(scenarioData.commonExpectedResults.taxYear),
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYear))
            )
          }

          request.headers("Location").headOption shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(scenarioData.commonExpectedResults.taxYear))
        }

      }
    }

  }

  ".submit" should {

    "immediately redirect the user to the overview page" when {

      "the student loans feature switch is off" in {
        val request = FakeRequest("POST", studentLoansCyaPage(taxYear, employmentId)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear), "Csrf-Token" -> "nocheck")

        lazy val result: Result = {
          dropEmploymentDB()
          authoriseIndividual()
          await(route(appWithFeatureSwitchesOff, request, "{}").get)
        }

        result.header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

    }

    userScenarios.foreach { scenarioData =>
      val inYearText = if (scenarioData.commonExpectedResults.isEndOfYear) "end of year" else "in year"
      val affinityText = if (scenarioData.isAgent) "agent" else "individual"
      val prior = if (scenarioData.commonExpectedResults.hasPrior) "prior data" else "no prior data"

      lazy val incomeTaxUserData = IncomeTaxUserData(Some(AllEmploymentData(
        Seq(),
        None,
        Seq(EmploymentSource(
          employmentId,
          "Whiterun Guard",
          None, None, Some("2022-01-01"), None, None, None, Some(EmploymentData(
            "2022-04-01", None, None, None, None, None, None,
            Some(Pay(Some(3000.00), Some(300.00), Some("WEEKLY"), Some("2022-01-01"), Some(3), Some(3))),
            Some(Deductions(Some(StudentLoans(Some(1000.00), Some(3000.00)))))
          )), None
        )),
        None
      )))

      s"redirect to the employment information overview page for $inYearText for an $affinityText when there is $prior" when {

        "the student loans submission is successful" in {
          def user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId, affinityGroup)(FakeRequest())

          lazy val createUpdateRequest = CreateUpdateEmploymentRequest(
            Some(employmentId),
            None,
            Some(CreateUpdateEmploymentData(
              CreateUpdatePay(3000.00, 300.00),
              Some(Deductions(Some(StudentLoans(Some(2000.00), Some(4000.00)))))
            )),
            None
          )

          val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYear,
              employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = true,
              EmploymentCYAModel(
                EmploymentDetails(
                  employerName = "Whiterun Guards",
                  employerRef = Some("223/AB12399"),
                  startDate = Some("2022-04-01"),
                  cessationDateQuestion = Some(false),
                  taxablePayToDate = Some(3000.00),
                  totalTaxToDate = Some(300.00),
                  currentDataIsHmrcHeld = false
                ),
                studentLoans = Some(StudentLoansCYAModel(
                  uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                ))
              )
            ), user)
            userDataStub(incomeTaxUserData, nino, scenarioData.commonExpectedResults.taxYear)
            stubEmploymentPost(createUpdateRequest, scenarioData.commonExpectedResults.taxYear)

            urlPost(
              url(scenarioData.commonExpectedResults.taxYear),
              "{}",
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYear))
            )
          }

          result.headers("Location").headOption shouldBe Some(employerInformationUrl(scenarioData.commonExpectedResults.taxYear, employmentId))
        }

      }
      s"redirect to the employment expenses page for $inYearText for an $affinityText when there is $prior" when {

        "the student loans submission is successful" in {
          def user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId, affinityGroup)(FakeRequest())

          lazy val createUpdateRequest = CreateUpdateEmploymentRequest(
            Some(employmentId),
            None,
            Some(CreateUpdateEmploymentData(
              CreateUpdatePay(3000.00, 300.00),
              Some(Deductions(Some(StudentLoans(Some(2000.00), Some(4000.00)))))
            )),
            None
          )

          val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYear,
              employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
              EmploymentCYAModel(
                EmploymentDetails(
                  employerName = "Whiterun Guards",
                  employerRef = Some("223/AB12399"),
                  startDate = Some("2022-04-01"),
                  cessationDateQuestion = Some(false),
                  taxablePayToDate = Some(3000.00),
                  totalTaxToDate = Some(300.00),
                  currentDataIsHmrcHeld = false
                ),
                studentLoans = Some(StudentLoansCYAModel(
                  uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                ))
              )
            ), user)
            userDataStub(incomeTaxUserData, nino, scenarioData.commonExpectedResults.taxYear)
            stubEmploymentPost(createUpdateRequest, scenarioData.commonExpectedResults.taxYear)

            urlPost(
              url(scenarioData.commonExpectedResults.taxYear),
              "{}",
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYear, extraData = Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId)))
            )
          }

          result.headers("Location").headOption shouldBe Some(checkYourExpensesUrl(scenarioData.commonExpectedResults.taxYear))
        }

      }
      s"redirect to the employment information page for $inYearText for an $affinityText when there is $prior that is hmrc data" when {

        "the student loans submission is successful" in {
          def user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId, affinityGroup)(FakeRequest())

          lazy val createUpdateRequest = CreateUpdateEmploymentRequest(
            None,
            Some(
              CreateUpdateEmployment(
                employerName = "Whiterun Guard",
                startDate = "2022-01-01",
                employerRef = None
              )
            ),
            Some(CreateUpdateEmploymentData(
              CreateUpdatePay(3000.00, 300.00),
              Some(Deductions(Some(StudentLoans(Some(2600.00), Some(4001.00)))))
            )),
            Some(employmentId)
          )

          val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYear,
              employmentId, isPriorSubmission = true, hasPriorBenefits = false, hasPriorStudentLoans = false,
              EmploymentCYAModel(
                EmploymentDetails(
                  employerName = "Whiterun Guards",
                  employerRef = Some("223/AB12399"),
                  startDate = Some("2022-04-01"),
                  cessationDateQuestion = Some(false),
                  taxablePayToDate = Some(3000.00),
                  totalTaxToDate = Some(300.00),
                  currentDataIsHmrcHeld = false
                ),
                studentLoans = Some(StudentLoansCYAModel(
                  uglDeduction = true, Some(2600.00), pglDeduction = true, Some(4001.00)
                ))
              )
            ), user)

            val data = incomeTaxUserData.copy(
              employment = incomeTaxUserData.employment.map(
                _.copy(
                  hmrcEmploymentData = incomeTaxUserData.employment.map(_.customerEmploymentData).get,
                  customerEmploymentData = Seq()
                )
              )
            )

            userDataStub(data, nino, scenarioData.commonExpectedResults.taxYear)
            stubEmploymentPost(createUpdateRequest, scenarioData.commonExpectedResults.taxYear, """{"employmentId":"id"}""")

            urlPost(
              url(scenarioData.commonExpectedResults.taxYear),
              "{}",
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYear))
            )
          }

          result.headers("Location").headOption shouldBe Some(employerInformationUrl(scenarioData.commonExpectedResults.taxYear, "id"))
        }

      }

      s"redirect to the overview page for $inYearText for an $affinityText when there is $prior" when {

        "there is no CYA data in session" in {
          val result: WSResponse = {
            dropEmploymentDB()
            authoriseIndividual()
            userDataStub(incomeTaxUserData, nino, scenarioData.commonExpectedResults.taxYear)

            urlPost(url(scenarioData.commonExpectedResults.taxYear), "{}", scenarioData.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.headers("Location").headOption shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(scenarioData.commonExpectedResults.taxYear))
        }

      }

      s"show an internal server error for $inYearText for an $affinityText when there is $prior" when {

        "an error is returned from the submission" in {
          def user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId, affinityGroup)(FakeRequest())

          lazy val createUpdateRequest = CreateUpdateEmploymentRequest(
            Some(employmentId),
            None,
            Some(CreateUpdateEmploymentData(
              CreateUpdatePay(3000.00, 300.00),
              Some(Deductions(Some(StudentLoans(
                Some(2000.00), Some(4000.00)
              ))))
            )),
            None
          )

          val result: WSResponse = {
            dropEmploymentDB()
            wireMockServer.resetAll()
            authoriseIndividual()
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYear,
              employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = true,
              EmploymentCYAModel(
                EmploymentDetails(
                  employerName = "Whiterun Guards",
                  employerRef = Some("223/AB12399"),
                  startDate = Some("2022-04-01"),
                  cessationDateQuestion = Some(false),
                  taxablePayToDate = Some(3000.00),
                  totalTaxToDate = Some(300.00),
                  currentDataIsHmrcHeld = false
                ),
                studentLoans = Some(StudentLoansCYAModel(
                  uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                ))
              )
            ), user)
            userDataStub(incomeTaxUserData, nino, scenarioData.commonExpectedResults.taxYear)
            stubEmploymentPostFailure(createUpdateRequest, scenarioData.commonExpectedResults.taxYear)

            urlPost(
              url(scenarioData.commonExpectedResults.taxYear),
              "{}",
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYear))
            )
          }

          result.status shouldBe INTERNAL_SERVER_ERROR
        }

      }
    }

  }

}
