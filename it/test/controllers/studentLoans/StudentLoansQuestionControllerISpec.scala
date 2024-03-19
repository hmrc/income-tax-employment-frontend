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

import forms.studentLoans.StudentLoanQuestionForm
import models.IncomeTaxUserData
import models.details.EmploymentDetails
import models.employment._
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
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

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  private val employmentId: String = "1234567890-0987654321"
  private val startDate = s"$taxYear-04-01"

  private def url(taxYearUnique: Int): String = fullUrl(studentLoansQuestionPage(taxYearUnique, employmentId))

  ".show" should {

    "immediately redirect the user to the overview page" when {

      "the student loans feature switch is off" in {
        val request = FakeRequest("GET", studentLoansQuestionPage(taxYearEOY, employmentId)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear))

        val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }

        status(result) shouldBe SEE_OTHER
        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
      }

      "a user is accessing the page in year" in {
        val request = FakeRequest("GET", studentLoansQuestionPage(taxYear, employmentId)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYear))

        val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(app, request, "{}").get
        }

        status(result) shouldBe SEE_OTHER
        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

      "a user has no cyadata" in {
        val result = {
          dropEmploymentDB()
          authoriseIndividual()
          userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

          urlGet(url(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").headOption shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
      }
    }

    "there is cya data in session with previous studentLoans" in {
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
        userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

        urlGet(url(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }

    "render the page" in {
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
              startDate = Some(startDate),
              didYouLeaveQuestion = Some(false),
              taxablePayToDate = Some(3000.00),
              totalTaxToDate = Some(300.00),
              currentDataIsHmrcHeld = false
            ),
          )
        ))

        urlGet(url(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }
  }

  ".submit" should {

    "immediately redirect the user to the overview page" when {

      "The user is taken to the overview page when the student loans feature switch is off" in {
        val headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY), "Csrf-Token" -> "nocheck")
        val request = FakeRequest("POST", studentLoansQuestionPage(taxYearEOY, employmentId)).withHeaders(headers: _*)

        val result: Future[Result] = {
          dropEmploymentDB()
          authoriseIndividual()
          route(appWithFeatureSwitchesOff, request, "{}").get
        }

        status(result) shouldBe SEE_OTHER
        await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)

      }
      "there is no cya data in session and the form is valid" in {

        lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq("ugl"))

        lazy val result = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

          urlPost(url(taxYearEOY), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
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
            taxYearEOY,
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
          userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

          urlPost(url(taxYearEOY), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(studentLoansCyaPage(taxYearEOY, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
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
            taxYearEOY,
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
          userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

          urlPost(url(taxYearEOY), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(studentLoansUglAmountUrl(taxYearEOY, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
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
            taxYearEOY,
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
          userDataStub(IncomeTaxUserData(), nino, taxYearEOY)
          urlPost(url(taxYearEOY), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(pglAmountUrl(taxYearEOY, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
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
            taxYearEOY,
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
          userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

          urlPost(url(taxYearEOY), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(studentLoansCyaPage(taxYearEOY, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
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
            taxYearEOY,
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
          userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

          urlPost(url(taxYearEOY), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION).contains(studentLoansUglAmountUrl(taxYearEOY, employmentId)) shouldBe true

        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.studentLoans.get shouldBe
          StudentLoansCYAModel(uglDeduction = true, None, pglDeduction = true, None)
      }
    }

    "return bad request" when {
      s"no option has been selected" in {
        lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq())
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
                startDate = Some(startDate),
                didYouLeaveQuestion = Some(false),
                taxablePayToDate = Some(3000.00),
                totalTaxToDate = Some(300.00),
                currentDataIsHmrcHeld = false
              )
            )
          ))
          userDataStub(IncomeTaxUserData(), nino, taxYearEOY)
          urlPost(url(taxYearEOY), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe BAD_REQUEST
      }

      s"all options have been selected" in {
        lazy val form = Map(s"${StudentLoanQuestionForm.studentLoans}[]" -> Seq("ugl", "pgl", "none"))
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
                startDate = Some(startDate),
                didYouLeaveQuestion = Some(false),
                taxablePayToDate = Some(3000.00),
                totalTaxToDate = Some(300.00),
                currentDataIsHmrcHeld = false
              )
            )
          ))
          userDataStub(IncomeTaxUserData(), nino, taxYearEOY)

          urlPost(url(taxYearEOY), form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        result.status shouldBe BAD_REQUEST
      }
    }
  }
}
