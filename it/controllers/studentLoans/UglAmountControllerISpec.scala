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

import models.IncomeTaxUserData
import models.employment._
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import utils.PageUrls.{fullUrl, pglAmountUrl, studentLoansCyaPage, studentLoansUglAmountUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class UglAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val employmentId: String = "1234567890-0987654321"

  def url(taxYearUnique: Int): String = fullUrl(studentLoansUglAmountUrl(taxYearUnique, employmentId))

  val uglDeductionAmount: BigDecimal = 117
  val startDate = s"$taxYear-04-01"
  val paymentDate = s"$taxYear-01-01"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > label > header > p"
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val inputSelector = "#amount"
    val hintTextSelector = "#amount-hint"
    val paragraphCheckSelector = "#main-content > div > div > form > div > label > p:nth-child(2)"
    val headingSelector = "#main-content > div > div > form > div > label > header > h1"
    val errorSummarySelector = "#main-content > div > div > div.govuk-error-summary > div > ul > li > a"
    val errorMessageSelector = "#amount-error"
  }

  trait CommonExpectedResults {
    val title: String
    val expectedH1: String
    val expectedCaption: String
    val expectedButtonText: String
    val expectedParagraphCheckText: String
    val hintText: String
    val inputFieldName: String
    val errorSummaryText: String
    val noEntryError: String
    val invalidFormatError: String
    val expectedErrorTitle: String
  }

  object ExpectedResultsIndividualEN extends CommonExpectedResults {
    override val expectedCaption: String = s"Student loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £193.52"
    override val title: String = "How much undergraduate loan did you repay?"
    override val expectedH1: String = "How much undergraduate loan did you repay while employed by Falador Knights?"
    override val expectedParagraphCheckText: String = "Check with the Student Loans Company, your payslips or P60."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of undergraduate loan you repaid while employed by Falador Knights"
    override val noEntryError: String = "Enter the amount of undergraduate loan you repaid while employed by Falador Knights"
    override val invalidFormatError: String = "Enter the amount of undergraduate loan in the correct format"
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsIndividualCY extends CommonExpectedResults {
    override val expectedCaption: String = s"Student loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £193.52"
    override val title: String = "How much undergraduate loan did you repay?"
    override val expectedH1: String = "How much undergraduate loan did you repay while employed by Falador Knights?"
    override val expectedParagraphCheckText: String = "Check with the Student Loans Company, your payslips or P60."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of undergraduate loan you repaid while employed by Falador Knights"
    override val noEntryError: String = "Enter the amount of undergraduate loan you repaid while employed by Falador Knights"
    override val invalidFormatError: String = "Enter the amount of undergraduate loan in the correct format"
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsAgentEN extends CommonExpectedResults {
    override val expectedCaption: String = s"Student loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £193.52"
    override val title: String = "How much undergraduate loan did your client repay?"
    override val expectedH1: String = "How much undergraduate loan did your client repay while employed by Falador Knights?"
    override val expectedParagraphCheckText: String = "Check with the Student Loans Company, your client’s payslips or P60."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of undergraduate loan your client repaid while employed by Falador Knights"
    override val noEntryError: String = "Enter the amount of undergraduate loan your client repaid while employed by Falador Knights"
    override val invalidFormatError: String = "Enter the amount of undergraduate loan in the correct format"
    override val expectedErrorTitle: String = s"Error: $title"
  }

  object ExpectedResultsAgentCY extends CommonExpectedResults {
    override val expectedCaption: String = s"Student loans for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val expectedButtonText: String = "Continue"
    override val hintText: String = "For example, £193.52"
    override val title: String = "How much undergraduate loan did your client repay?"
    override val expectedH1: String = "How much undergraduate loan did your client repay while employed by Falador Knights?"
    override val expectedParagraphCheckText: String = "Check with the Student Loans Company, your client’s payslips or P60."
    override val inputFieldName: String = "amount"
    override val errorSummaryText: String = "Enter the amount of undergraduate loan your client repaid while employed by Falador Knights"
    override val noEntryError: String = "Enter the amount of undergraduate loan your client repaid while employed by Falador Knights"
    override val invalidFormatError: String = "Enter the amount of undergraduate loan in the correct format"
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
        val request = FakeRequest("GET", studentLoansUglAmountUrl(taxYearEOY, employmentId))
          .withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))

        lazy val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }

        await(result).header.headers("Location") shouldBe
          appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
      }
    }

    userScenarios.foreach { scenarioData =>

      s"The language is ${welshTest(scenarioData.isWelsh)} and the request is from an ${scenarioData.isAgent}" should {
        import Selectors._
        import scenarioData.commonExpectedResults._

        "render the undergraduate amount page when there is no prior or cya data" which {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
                          sessionId,
                          mtditid,
                          nino,
                          taxYearEOY,
                          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                          EmploymentCYAModel(
                            EmploymentDetails(
                              employerName = "Falador Knights",
                              employerRef = Some("223/AB12399"),
                              startDate = Some(startDate),
                              didYouLeaveQuestion = Some(false),
                              taxablePayToDate = Some(3000.00),
                              totalTaxToDate = Some(300.00),
                              currentDataIsHmrcHeld = false
                            ),
                            studentLoans = Some(StudentLoansCYAModel(
                              uglDeduction = true, uglDeductionAmount = None, pglDeduction = false, pglDeductionAmount = None))
                          )))
            userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

            urlGet(url(taxYearEOY), scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(title)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphCheckText, paragraphCheckSelector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputFieldName, inputSelector, "")

          buttonCheck(expectedButtonText, continueButtonSelector)

        }

        "render the undergraduate amount page with no value when there is prior data and no cya data" which {

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
                          sessionId,
                          mtditid,
                          nino,
                          taxYearEOY,
                          employmentId, isPriorSubmission = true, hasPriorBenefits = false, hasPriorStudentLoans = false,
                          EmploymentCYAModel(
                            EmploymentDetails(
                              employerName = "Falador Knights",
                              employerRef = Some("223/AB12399"),
                              startDate = Some(startDate),
                              didYouLeaveQuestion = Some(false),
                              taxablePayToDate = Some(3000.00),
                              totalTaxToDate = Some(300.00),
                              currentDataIsHmrcHeld = false
                            ),
                            studentLoans = Some(StudentLoansCYAModel(
                              uglDeduction = true, uglDeductionAmount = Some(100.00), pglDeduction = false, pglDeductionAmount = None))
                          )))
            userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

            urlGet(url(taxYearEOY), scenarioData.isWelsh, headers =
              Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(title)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphCheckText, paragraphCheckSelector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputFieldName, inputSelector, "100")

          buttonCheck(expectedButtonText, continueButtonSelector)
        }

        "render the undergraduate amount page when there is cya data for student loans but no prior data" which {

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
                          sessionId,
                          mtditid,
                          nino,
                          taxYearEOY,
                          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
                          EmploymentCYAModel(
                            EmploymentDetails(
                              employerName = "Falador Knights",
                              employerRef = Some("223/AB12399"),
                              startDate = Some(startDate),
                              didYouLeaveQuestion = Some(false),
                              taxablePayToDate = Some(3000.00),
                              totalTaxToDate = Some(300.00),
                              currentDataIsHmrcHeld = false
                            ),
                            studentLoans = Some(StudentLoansCYAModel(
                              uglDeduction = true, uglDeductionAmount = Some(84.73), pglDeduction = true, pglDeductionAmount = Some(1000.00)))
                          )))
            userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

            urlGet(url(taxYearEOY), scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(title)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphCheckText, paragraphCheckSelector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck("amount", inputSelector, "84.73")

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
                          taxYearEOY,
                          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = true,
                          EmploymentCYAModel(
                            EmploymentDetails(
                              employerName = "Falador Knights",
                              employerRef = Some("223/AB12399"),
                              startDate = Some(startDate),
                              didYouLeaveQuestion = Some(false),
                              taxablePayToDate = Some(3000.00),
                              totalTaxToDate = Some(300.00),
                              currentDataIsHmrcHeld = false
                            )
                          )))
            userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

            urlGet(url(taxYearEOY), follow = false, welsh = scenarioData.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").headOption shouldBe Some(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYearEOY, employmentId).url)

        }


        "redirect to student loans cya page when there is no employment user data returned" in {

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)

            urlGet(url(taxYearEOY), follow = false, welsh = scenarioData.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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

        lazy val incomeTaxUserData = IncomeTaxUserData(Some(AllEmploymentData(
          Seq(),
          None,
          Seq(EmploymentSource(
            employmentId,
            "Falador Knights",
            None, None, Some(paymentDate), None, None, None, Some(EmploymentData(
              startDate, None, None, None, None, None, None,
              Some(Pay(Some(100.00), Some(12), Some("WEEKLY"), Some(startDate), Some(3), Some(3))),
              Some(Deductions(Some(StudentLoans(Some(6300.00), Some(91.0)))))
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
                              taxYearEOY,
                              employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = true,
                              EmploymentCYAModel(
                                EmploymentDetails(
                                  employerName = "Falador Knights",
                                  employerRef = Some("223/AB12399"),
                                  startDate = Some(startDate),
                                  didYouLeaveQuestion = Some(false),
                                  taxablePayToDate = Some(3000.00),
                                  totalTaxToDate = Some(300.00),
                                  currentDataIsHmrcHeld = false
                                ),
                                studentLoans = Some(StudentLoansCYAModel(
                                  uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                                ))
                              )
                            ))

              userDataStub(incomeTaxUserData, nino, taxYearEOY)
              urlPost(
                url(taxYearEOY),
                body = Map("amount" -> uglDeductionAmount.toString),
                scenarioData.isWelsh,
                follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
              )
            }
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(studentLoansCyaPage(taxYearEOY, employmentId))
          }
        }
        "redirect to the student loans postgraduate amount page when submission is successful" when {

          "the submission is successful" in {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(scenarioData.isAgent)
              insertCyaData(EmploymentUserData(
                sessionId,
                mtditid,
                nino,
                taxYearEOY,
                employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = true,
                EmploymentCYAModel(
                  EmploymentDetails(
                    employerName = "Falador Knights",
                    employerRef = Some("223/AB12399"),
                    startDate = Some(startDate),
                    didYouLeaveQuestion = Some(false),
                    taxablePayToDate = Some(90000.00),
                    totalTaxToDate = Some(111),
                    currentDataIsHmrcHeld = false
                  ),
                  studentLoans = Some(StudentLoansCYAModel(
                    uglDeduction = true, Some(20000), pglDeduction = true, None
                  ))
                )
              ))

              userDataStub(incomeTaxUserData, nino, taxYearEOY)
              urlPost(
                url(taxYearEOY),
                body = Map("amount" -> uglDeductionAmount.toString),
                scenarioData.isWelsh,
                follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
              )
            }
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(pglAmountUrl(taxYearEOY, employmentId))
          }
        }

        "render the undergraduate loans repayment amount page with an error when there is no entry in the amount field" when {

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
                          sessionId,
                          mtditid,
                          nino,
                          taxYearEOY,
                          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = true,
                          EmploymentCYAModel(
                            EmploymentDetails(
                              employerName = "Falador Knights",
                              employerRef = Some("223/AB12399"),
                              startDate = Some(startDate),
                              didYouLeaveQuestion = Some(false),
                              taxablePayToDate = Some(90000.00),
                              totalTaxToDate = Some(111),
                              currentDataIsHmrcHeld = false
                            ),
                            studentLoans = Some(StudentLoansCYAModel(
                              uglDeduction = true, Some(20000), pglDeduction = true, Some(60000)
                            ))
                          )
                        ))

            userDataStub(incomeTaxUserData, nino, taxYearEOY)
            urlPost(
              url(taxYearEOY),
              body = Map("amount" -> ""),
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
            )
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphCheckText, paragraphCheckSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(noEntryError, errorSummarySelector)

          buttonCheck(expectedButtonText, continueButtonSelector)

          "result should be BadRequest" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "render the undergraduate loans repayment amount page when there is an invalid format entry in the amount field" when {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
                          sessionId,
                          mtditid,
                          nino,
                          taxYearEOY,
                          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = true,
                          EmploymentCYAModel(
                            EmploymentDetails(
                              employerName = "Falador Knights",
                              employerRef = Some("223/AB12399"),
                              startDate = Some(startDate),
                              didYouLeaveQuestion = Some(false),
                              taxablePayToDate = Some(9000.00),
                              totalTaxToDate = Some(3),
                              currentDataIsHmrcHeld = false
                            ),
                            studentLoans = Some(StudentLoansCYAModel(
                              uglDeduction = true, Some(420.00), pglDeduction = true, Some(39000)
                            ))
                          )
                        ))

            userDataStub(incomeTaxUserData, nino, taxYearEOY)
            urlPost(
              url(taxYearEOY),
              body = Map("amount" -> "abc"),
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
            )
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(expectedParagraphCheckText, paragraphCheckSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(invalidFormatError, errorSummarySelector)

          buttonCheck(expectedButtonText, continueButtonSelector)

          "result should be BadRequest" in {
            result.status shouldBe BAD_REQUEST
          }
        }
        "The user is taken to the overview page when the student loans feature switch is off" in {
          val headers = if (scenarioData.isWelsh) {
            Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
          } else {
            Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), "Csrf-Token" -> "nocheck")
          }

          val request = FakeRequest("POST", studentLoansUglAmountUrl(taxYearEOY, employmentId)).withHeaders(headers: _*)

          val result: Future[Result] = {
            dropEmploymentDB()
            authoriseIndividual()
            route(appWithFeatureSwitchesOff, request, "{}").get
          }

          status(result) shouldBe SEE_OTHER
          await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)

        }
      }
    }
  }

}