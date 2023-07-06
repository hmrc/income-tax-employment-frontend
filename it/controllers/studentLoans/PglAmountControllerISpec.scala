/*
 * Copyright 2023 HM Revenue & Customs
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
import models.details.EmploymentDetails
import models.employment._
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import utils.PageUrls.{fullUrl, pglAmountUrl, studentLoansCyaPage}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class PglAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  val employmentId: String = "1234567890-0987654321"

  def url(taxYearUnique: Int): String = fullUrl(pglAmountUrl(taxYearUnique, employmentId))

  val pglDeductionAmount: BigDecimal = 22500.00

  ".show" should {
    "redirect to the overview page" when {
      "the student loans feature switch is off" in {
        val request = FakeRequest("GET", pglAmountUrl(taxYearEOY, employmentId))
          .withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))

        lazy val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }

        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
      }
    }

    "render the postgraduate amount page when there is no prior or cya data" in {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(EmploymentUserData(
          sessionId,
          mtditid,
          nino,
          taxYearEOY,
          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
          EmploymentCYAModel(
            EmploymentDetails(
              employerName = "Whiterun Guards",
              employerRef = Some("223/AB12399"),
              startDate = Some("2022-04-01"),
              didYouLeaveQuestion = Some(false),
              taxablePayToDate = Some(3000.00),
              totalTaxToDate = Some(300.00),
              currentDataIsHmrcHeld = false
            ),
            studentLoans = Some(StudentLoansCYAModel(
              uglDeduction = false, uglDeductionAmount = None, pglDeduction = true, pglDeductionAmount = None))
          )))
        userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

        urlGet(url(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }

    "render the Postgraduate amount page when there is prior data and no cya data" in {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(EmploymentUserData(
          sessionId,
          mtditid,
          nino,
          taxYearEOY,
          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
          EmploymentCYAModel(
            EmploymentDetails(
              employerName = "Whiterun Guards",
              employerRef = Some("223/AB12399"),
              startDate = Some("2022-04-01"),
              didYouLeaveQuestion = Some(false),
              taxablePayToDate = Some(3000.00),
              totalTaxToDate = Some(300.00),
              currentDataIsHmrcHeld = false
            ),
            studentLoans = Some(StudentLoansCYAModel(
              uglDeduction = false, uglDeductionAmount = None, pglDeduction = true, pglDeductionAmount = Some(100.00)))
          )))
        userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

        urlGet(url(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }

    "render the postgraduate amount page when there is cya data for student loans but no prior data" in {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(EmploymentUserData(
          sessionId,
          mtditid,
          nino,
          taxYearEOY,
          employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false,
          EmploymentCYAModel(
            EmploymentDetails(
              employerName = "Whiterun Guards",
              employerRef = Some("223/AB12399"),
              startDate = Some("2022-04-01"),
              didYouLeaveQuestion = Some(false),
              taxablePayToDate = Some(3000.00),
              totalTaxToDate = Some(300.00),
              currentDataIsHmrcHeld = false
            ),
            studentLoans = Some(StudentLoansCYAModel(
              uglDeduction = true, Some(1000.22), pglDeduction = true, Some(3000.22)
            ))
          )))
        userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

        urlGet(url(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }

    "redirect to student loans cya page when there is no student loans data" in {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(EmploymentUserData(
          sessionId,
          mtditid,
          nino,
          taxYearEOY,
          employmentId, isPriorSubmission = true, hasPriorBenefits = false, hasPriorStudentLoans = true,
          EmploymentCYAModel(
            EmploymentDetails(
              employerName = "Whiterun Guards",
              employerRef = Some("223/AB12399"),
              startDate = Some("2022-04-01"),
              didYouLeaveQuestion = Some(false),
              taxablePayToDate = Some(3000.00),
              totalTaxToDate = Some(300.00),
              currentDataIsHmrcHeld = false
            )
          )))
        userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

        urlGet(url(taxYearEOY), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").headOption shouldBe Some(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYearEOY, employmentId).url)
    }

    "redirect to student loans cya page when there is no employment user data returned" in {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)

        urlGet(url(taxYearEOY), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").headOption shouldBe Some(controllers.studentLoans.routes.StudentLoansCYAController.show(taxYearEOY, employmentId).url)
    }
  }

  ".submit" should {

    lazy val incomeTaxUserData = IncomeTaxUserData(Some(AllEmploymentData(
      Seq(),
      None,
      Seq(EmploymentSource(
        employmentId,
        "Whiterun Guard",
        None, None, Some("2022-01-01"), None, None, None, Some(EmploymentData(
          "2022-04-01", None, None, None, None, None,
          Some(Pay(Some(3000.00), Some(300.00), Some("WEEKLY"), Some("2022-01-01"), Some(3), Some(3))),
          Some(Deductions(Some(StudentLoans(Some(1000.00), Some(3000.00)))))
        )), None
      )),
      None,
      None
    )))

    "redirect to the student loans cya page when submission is successful" when {
      "the submission is successful" in {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(EmploymentUserData(
            sessionId,
            mtditid,
            nino,
            taxYearEOY,
            employmentId, isPriorSubmission = true, hasPriorBenefits = false, hasPriorStudentLoans = true,
            EmploymentCYAModel(
              EmploymentDetails(
                employerName = "Whiterun Guards",
                employerRef = Some("223/AB12399"),
                startDate = Some("2022-04-01"),
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
            body = Map("amount" -> pglDeductionAmount.toString),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
          )
        }
        result.status shouldBe SEE_OTHER
        result.headers("Location").headOption shouldBe Some(studentLoansCyaPage(taxYearEOY, employmentId))
      }
    }

    "render the postgraduate loans repayment amount page with an error when there is no entry in the amount field" when {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(EmploymentUserData(
          sessionId,
          mtditid,
          nino,
          taxYearEOY,
          employmentId, isPriorSubmission = true, hasPriorBenefits = false, hasPriorStudentLoans = true,
          EmploymentCYAModel(
            EmploymentDetails(
              employerName = "Whiterun Guards",
              employerRef = Some("223/AB12399"),
              startDate = Some("2022-04-01"),
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
          body = Map("amount" -> ""),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
        )
      }

      "result should be BadRequest" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "render the postgraduate loans repayment amount page when there is an invalid format entry in the amount field" when {
      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(EmploymentUserData(
          sessionId,
          mtditid,
          nino,
          taxYearEOY,
          employmentId, isPriorSubmission = true, hasPriorBenefits = false, hasPriorStudentLoans = true,
          EmploymentCYAModel(
            EmploymentDetails(
              employerName = "Whiterun Guards",
              employerRef = Some("223/AB12399"),
              startDate = Some("2022-04-01"),
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
          body = Map("amount" -> "abc"),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
        )
      }

      "result should be BadRequest" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "The user is taken to the overview page when the student loans feature switch is off" in {
      val headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), "Csrf-Token" -> "nocheck")
      val request = FakeRequest("POST", pglAmountUrl(taxYearEOY, employmentId)).withHeaders(headers: _*)

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
