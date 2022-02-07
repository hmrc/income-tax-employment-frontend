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

import models.employment._
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import models.{IncomeTaxUserData, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import utils.PageUrls.{employerInformationUrl, fullUrl, pglAmountUrl, studentLoansCyaPage}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class PglAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val employmentId: String = "1234567890-0987654321"

  def url(taxYearUnique: Int): String = fullUrl(pglAmountUrl(taxYearUnique, employmentId))

  val pglDeductionAmount: BigDecimal = 22500.00

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > label > header > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val inputSelector = "#amount"
    val hintTextSelector = "#amount-hint"
    val paragraphSelector = "#main-content > div > div > form > div > label > p"
    val headingSelector = "#main-content > div > div > form > div > label > header > h1"
    val errorSummarySelector = "#main-content > div > div > div.govuk-error-summary > div > ul > li > a"
    val errorMessageSelector = "#amount-error"
  }

  trait CommonExpectedResults {
    val title: String
    val expectedH1: String
    val expectedCaption: String
    val expectedButtonText: String
    val expectedParagraphText: String
    val hintText: String
    val taxYearEOY: Int
    val inputFieldName: String
    val errorSummaryText: String
    val noEntryError: String
    val invalidFormatError: String
    val expectedErrorTitle: String
  }

  object ExpectedResultsIndividualEN extends CommonExpectedResults {
    override val expectedCaption: String = "Student Loans for 6 April 2020 to 5 April 2021"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £600 or £193.54"
    override val taxYearEOY: Int = 2021
    override val title: String = "How much postgraduate loan did you repay while employed by Whiterun Guards?"
    override val expectedH1: String = "How much postgraduate loan did you repay while employed by Whiterun Guards?"
    override val expectedParagraphText: String = "Check with the Student Loans Company, your payslips or P60."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of postgraduate loan you repaid while employed by Whiterun Guards"
    override val noEntryError: String = "Enter the amount of postgraduate loan you repaid while employed by Whiterun Guards"
    override val invalidFormatError: String = "Enter the amount of postgraduate loan in the correct format"
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsIndividualCY extends CommonExpectedResults {
    override val expectedCaption: String = "Student Loans for 6 April 2020 to 5 April 2021"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £600 or £193.54"
    override val taxYearEOY: Int = 2021
    override val title: String = "How much postgraduate loan did you repay while employed by Whiterun Guards?"
    override val expectedH1: String = "How much postgraduate loan did you repay while employed by Whiterun Guards?"
    override val expectedParagraphText: String = "Check with the Student Loans Company, your payslips or P60."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of postgraduate loan you repaid while employed by Whiterun Guards"
    override val noEntryError: String = "Enter the amount of postgraduate loan you repaid while employed by Whiterun Guards"
    override val invalidFormatError: String = "Enter the amount of postgraduate loan in the correct format"
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsAgentEN extends CommonExpectedResults {
    override val expectedCaption: String = "Student Loans for 6 April 2020 to 5 April 2021"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £600 or £193.54"
    override val taxYearEOY: Int = 2021
    override val title: String = "How much postgraduate loan did your client repay while employed by Whiterun Guards?"
    override val expectedH1: String = "How much postgraduate loan did your client repay while employed by Whiterun Guards?"
    override val expectedParagraphText: String = "Check with the Student Loans Company, your client’s payslips or P60."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of postgraduate loan your client repaid while employed by Whiterun Guards"
    override val noEntryError: String = "Enter the amount of postgraduate loan your client repaid while employed by Whiterun Guards"
    override val invalidFormatError: String = "Enter the amount of postgraduate loan in the correct format"
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsAgentCY extends CommonExpectedResults {
    override val expectedCaption: String = "Student Loans for 6 April 2020 to 5 April 2021"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £600 or £193.54"
    override val taxYearEOY: Int = 2021
    override val title: String = "How much postgraduate loan did your client repay while employed by Whiterun Guards?"
    override val expectedH1: String = "How much postgraduate loan did your client repay while employed by Whiterun Guards?"
    override val expectedParagraphText: String = "Check with the Student Loans Company, your client’s payslips or P60."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of postgraduate loan your client repaid while employed by Whiterun Guards"
    override val noEntryError: String = "Enter the amount of postgraduate loan your client repaid while employed by Whiterun Guards"
    override val invalidFormatError: String = "Enter the amount of postgraduate loan in the correct format"
    override val expectedErrorTitle: String = s"Error: $title"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedResultsIndividualEN),
    UserScenario(isWelsh = false, isAgent = true, ExpectedResultsAgentEN),
    UserScenario(isWelsh = true, isAgent = false, ExpectedResultsIndividualCY),
    UserScenario(isWelsh = true, isAgent = true, ExpectedResultsAgentCY)
  )


  ".show" should {

    "redirect to the overview page" when {

      "the student loans feature switch is off" in {
        val request = FakeRequest("GET", pglAmountUrl(ExpectedResultsIndividualEN.taxYearEOY, employmentId))
          .withHeaders(HeaderNames.COOKIE -> playSessionCookies(ExpectedResultsIndividualEN.taxYearEOY))

        lazy val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }

        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(ExpectedResultsIndividualEN.taxYearEOY)
      }
    }

    userScenarios.foreach { scenarioData =>

      s"The language is ${welshTest(scenarioData.isWelsh)} and the request is from an ${scenarioData.isAgent}" should {
        import Selectors._
        import scenarioData.commonExpectedResults._


        def user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId, affinityGroup)(FakeRequest())

        "render the postgraduate amount page when there is no prior or cya data" which {


          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYearEOY,
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
                  uglDeduction = false, uglDeductionAmount = None, pglDeduction = true, pglDeductionAmount = None))
              )), user)
            userDataStub(IncomeTaxUserData(), nino, scenarioData.commonExpectedResults.taxYearEOY)

            urlGet(url(scenarioData.commonExpectedResults.taxYearEOY), scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYearEOY)))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(title)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphText, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputFieldName, inputSelector, "")

          buttonCheck(expectedButtonText, continueButtonSelector)

        }

        "render the Postgraduate amount page with no value when there is prior data and no cya data" which {

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYearEOY,
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
                  uglDeduction = false, uglDeductionAmount = None, pglDeduction = true, pglDeductionAmount = Some(100.00)))
              )), user)
            userDataStub(IncomeTaxUserData(), nino, scenarioData.commonExpectedResults.taxYearEOY)

            urlGet(url(scenarioData.commonExpectedResults.taxYearEOY), scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYearEOY)))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(title)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphText, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputFieldName, inputSelector, "100")

          buttonCheck(expectedButtonText, continueButtonSelector)
        }

        "render the postgraduate amount page when there is cya data for student loans but no prior data" which {

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYearEOY,
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
                  uglDeduction = true, Some(1000.22), pglDeduction = true, Some(3000.22)
                ))
              )), user)
            userDataStub(IncomeTaxUserData(), nino, scenarioData.commonExpectedResults.taxYearEOY)

            urlGet(url(scenarioData.commonExpectedResults.taxYearEOY), scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYearEOY)))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(title)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphText, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck("amount", inputSelector, "3,000.22")

          buttonCheck(expectedButtonText, continueButtonSelector)
        }

        "redirect to student loans cya page when there is no student loans data" in {

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYearEOY,
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
                )
              )), user)
            userDataStub(IncomeTaxUserData(), nino, scenarioData.commonExpectedResults.taxYearEOY)

            urlGet(url(scenarioData.commonExpectedResults.taxYearEOY), follow = false, welsh = scenarioData.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYearEOY)))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").headOption shouldBe Some(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYearEOY, employmentId).url)

        }


        "redirect to student loans cya page when there is no employment user data returned" in {

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)

            urlGet(url(scenarioData.commonExpectedResults.taxYearEOY), follow = false, welsh = scenarioData.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYearEOY)))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").headOption shouldBe Some(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYearEOY, employmentId).url)

        }
      }

    }
  }

  ".submit" should {

    userScenarios.foreach { scenarioData =>

      s"The language is ${welshTest(scenarioData.isWelsh)} and the request is from an ${scenarioData.isAgent}" should {
        import Selectors._
        import scenarioData.commonExpectedResults._


        def user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId, affinityGroup)(FakeRequest())

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

        "redirect to the student loans cya page when submission is successful" when {

          "the submission is successful" in {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(scenarioData.isAgent)
              insertCyaData(EmploymentUserData(
                sessionId,
                mtditid,
                nino,
                scenarioData.commonExpectedResults.taxYearEOY,
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
                  studentLoans = Some(StudentLoansCYAModel(
                    uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                  ))
                )
              ), user)

              userDataStub(incomeTaxUserData, nino, scenarioData.commonExpectedResults.taxYearEOY)
              urlPost(
                url(scenarioData.commonExpectedResults.taxYearEOY),
                body = Map("amount" -> pglDeductionAmount.toString),
                scenarioData.isWelsh,
                follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYearEOY))
              )
            }
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(studentLoansCyaPage(taxYearEOY, employmentId))
          }
        }

        "render the postgraduate loans repayment amount page with an error when there is no entry in the amount field" when {

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYearEOY,
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
                studentLoans = Some(StudentLoansCYAModel(
                  uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                ))
              )
            ), user)

            userDataStub(incomeTaxUserData, nino, scenarioData.commonExpectedResults.taxYearEOY)
            urlPost(
              url(scenarioData.commonExpectedResults.taxYearEOY),
              body = Map("amount" -> ""),
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYearEOY))
            )
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphText, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(noEntryError, errorSummarySelector)

          buttonCheck(expectedButtonText, continueButtonSelector)

          "result should be BadRequest" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "render the postgraduate loans repayment amount page when there is an invalid format entry in the amount field" when {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              scenarioData.commonExpectedResults.taxYearEOY,
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
                studentLoans = Some(StudentLoansCYAModel(
                  uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                ))
              )
            ), user)

            userDataStub(incomeTaxUserData, nino, scenarioData.commonExpectedResults.taxYearEOY)
            urlPost(
              url(scenarioData.commonExpectedResults.taxYearEOY),
              body = Map("amount" -> "abc"),
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(scenarioData.commonExpectedResults.taxYearEOY))
            )
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphText, paragraphSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(invalidFormatError, errorSummarySelector)

          buttonCheck(expectedButtonText, continueButtonSelector)

          "result should be BadRequest" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "The user is taken to the overview page when the student loans feature switch is off" in {
          val headers = if (scenarioData.isWelsh) {
            Seq(HeaderNames.COOKIE -> playSessionCookies(ExpectedResultsIndividualEN.taxYearEOY), HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
          } else {
            Seq(HeaderNames.COOKIE -> playSessionCookies(ExpectedResultsIndividualEN.taxYearEOY), "Csrf-Token" -> "nocheck")
          }

          val request = FakeRequest("POST", pglAmountUrl(ExpectedResultsIndividualEN.taxYearEOY, employmentId)).withHeaders(headers: _*)

          val result: Future[Result] = {
            dropEmploymentDB()
            authoriseIndividual()
            route(appWithFeatureSwitchesOff, request, "{}").get
          }

          status(result) shouldBe SEE_OTHER
          await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(ExpectedResultsIndividualEN.taxYearEOY)

        }
      }
    }
  }
}
