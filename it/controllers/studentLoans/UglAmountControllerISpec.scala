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
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.employment.StudentLoansBuilder.aStudentLoans
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithStudentLoans
import utils.PageUrls.{fullUrl, pglAmountUrl, studentLoansCyaPage, studentLoansUglAmountUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class UglAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  val employmentId: String = "1234567890-0987654321"

  def url(taxYearUnique: Int): String = fullUrl(studentLoansUglAmountUrl(taxYearUnique, employmentId))

  ".show" when {

    "should render the 'How much undergraduate loan did you repay?' page" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
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
              startDate = Some("2022-04-01"),
              didYouLeaveQuestion = Some(false),
              taxablePayToDate = Some(3000.00),
              totalTaxToDate = Some(300.00),
              currentDataIsHmrcHeld = false
            ),
            studentLoans = Some(StudentLoansCYAModel(
              uglDeduction = true, uglDeductionAmount = None, pglDeduction = false, pglDeductionAmount = None))
          )))
        insertCyaData(anEmploymentUserDataWithStudentLoans(aStudentLoans.toStudentLoansCYAModel()))
        urlGet(url(taxYearEOY), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), follow = false)
      }
      result.status shouldBe OK
    }

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

    "redirect to student loans cya page when there is no student loans data" in {

      lazy val result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(false)
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
        authoriseAgentOrIndividual(false)

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
      Seq(anEmploymentSource),
      None
    )))

    "redirect to the student loans cya page when submission is successful" when {

      "the submission is successful" in {
        lazy val result: WSResponse = {
          dropEmploymentDB()
          authoriseAgentOrIndividual(false)
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
            body = Map("amount" -> "1234"),
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
          authoriseAgentOrIndividual(false)
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
                startDate = Some("2022-04-01"),
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
            body = Map("amount" -> "1234"),
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
          )
        }
        result.status shouldBe SEE_OTHER
        result.headers("Location").headOption shouldBe Some(pglAmountUrl(taxYearEOY, employmentId))
      }
    }

    "The user is taken to the overview page when the student loans feature switch is off" in {
      val headers = if (false) {
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
