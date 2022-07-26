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
import models.IncomeTaxUserData
import models.details.EmploymentDetails
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentDataBuilder.anEmploymentData
import support.builders.models.employment.EmploymentFinancialDataBuilder.aHmrcEmploymentFinancialData
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class StudentLoansCYAControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId: String = "1234567890-0987654321"
  private val startDate = s"$taxYear-04-01"
  private val submittedOnDate = s"$taxYear-01-01"
  private val default = "{}"

  private def url(taxYearUnique: Int): String = fullUrl(studentLoansCyaPage(taxYearUnique, employmentId))

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
    val hasPrior: Boolean
  }

  object ExpectedResultsEnglishEOY extends CommonExpectedResults {
    override val isEndOfYear: Boolean = true
    override val hasPrior: Boolean = true
  }

  object ExpectedResultsEnglishInYear extends CommonExpectedResults {
    override lazy val isEndOfYear: Boolean = false
    override lazy val hasPrior: Boolean = false
  }

  override val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedResultsEnglishEOY),
    UserScenario(isWelsh = false, isAgent = false, ExpectedResultsEnglishInYear),
  )

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
      val taxYearInUse = if(scenarioData.commonExpectedResults.isEndOfYear) taxYearEOY else taxYear
      val welshLang = if(scenarioData.isWelsh) "Welsh" else "English"

      s"render the page for $inYearText, for an $affinityText when there is $prior in $welshLang" when {

        "there is CYA data in session" in {
          import scenarioData.commonExpectedResults._

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              taxYearInUse,
              employmentId, isPriorSubmission = hasPrior, hasPriorBenefits = false, hasPriorStudentLoans = true,
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
            userDataStub(IncomeTaxUserData(), nino, if (isEndOfYear) taxYearEOY else taxYear)

            urlGet(url(taxYearInUse), scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearInUse)))
          }

          result.status shouldBe OK
        }
      }

      if (scenarioData.commonExpectedResults.isEndOfYear && scenarioData.commonExpectedResults.hasPrior) {
        s"render page for EOY when not submittable for an $affinityText in $welshLang" in {
          import scenarioData.commonExpectedResults._

          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            val employmentData = anEmploymentData.copy(pay = None)
            val hmrcEmploymentSource = aHmrcEmploymentSource.copy(employmentId = employmentId,
              hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentData = Some(employmentData))))
            userDataStub(anIncomeTaxUserData.copy(employment = Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(hmrcEmploymentSource)))), nino, if (isEndOfYear) taxYearEOY else taxYear)
            urlGet(url(taxYearInUse), scenarioData.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearInUse)))
          }

          result.status shouldBe OK
        }
      }

      s"redirect the user to the select student loans contributions for $inYearText for an $affinityText when there is $prior in $welshLang" when {

        if (scenarioData.commonExpectedResults.hasPrior) {
          "there is no CYA data but there is Prior data that does not contain Student Loans information" in {
            lazy val result = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(scenarioData.isAgent)
              val hmrcEmploymentFinancialData = aHmrcEmploymentFinancialData.copy(employmentData = Some(anEmploymentData.copy(deductions = None)))
              userDataStub(IncomeTaxUserData(Some(AllEmploymentData(
                Seq(aHmrcEmploymentSource.copy(employmentId = employmentId, employerName = "Whiterun Guards", hmrcEmploymentFinancialData = Some(hmrcEmploymentFinancialData))), None,
                Seq(), None
              ))), nino, taxYearInUse)

              urlGet(
                url(taxYearInUse), scenarioData.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearInUse))
              )
            }

            result.headers("Location").headOption shouldBe Some(studentLoansQuestionPage(taxYearInUse, employmentId))
          }
        }

        "there is CYA data but it does not contain Student Loans information" in {
          lazy val result = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            insertCyaData(EmploymentUserData(
              sessionId,
              mtditid,
              nino,
              taxYearInUse,
              employmentId, isPriorSubmission = true, hasPriorBenefits = false, hasPriorStudentLoans = true,
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
                studentLoans = None
              )
            ))
            userDataStub(IncomeTaxUserData(Some(AllEmploymentData(
              Seq(aHmrcEmploymentSource.copy(employmentId = employmentId, employerName = "Whiterun Guards")), None,
              Seq(), None
            ))), nino, taxYearInUse)

            urlGet(
              url(taxYearInUse), scenarioData.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearInUse))
            )
          }

          result.headers("Location").headOption shouldBe Some(studentLoansQuestionPage(taxYearInUse, employmentId))
        }
      }

      s"redirect the user to the overview page for $inYearText for an $affinityText when there is $prior in $welshLang" when {
        "there is no data in session" in {
          val request = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(scenarioData.isAgent)
            userDataStub(IncomeTaxUserData(), nino, taxYearInUse)
            urlGet(
              url(taxYearInUse),
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearInUse))
            )
          }
          request.headers("Location").headOption shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearInUse))
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

    userScenarios.filter(_.commonExpectedResults.isEndOfYear).foreach { scenarioData =>
      val inYearText = if (scenarioData.commonExpectedResults.isEndOfYear) "end of year" else "in year"
      val affinityText = if (scenarioData.isAgent) "agent" else "individual"
      val prior = if (scenarioData.commonExpectedResults.hasPrior) "prior data" else "no prior data"
      val welshLang = if(scenarioData.isWelsh) "Welsh" else "English"

      lazy val incomeTaxUserData = IncomeTaxUserData(Some(AllEmploymentData(
        Seq(),
        None,
        Seq(EmploymentSource(
          employmentId,
          "Whiterun Guard",
          None, None, Some(startDate), None, None, None, Some(EmploymentData(
            startDate, None, None, None, None, None,
            Some(Pay(Some(3000.00), Some(300.00), Some("WEEKLY"), Some(submittedOnDate), Some(3), Some(3))),
            Some(Deductions(Some(StudentLoans(Some(1000.00), Some(3000.00)))))
          )), None
        )),
        None
      )))

      val hmrcData = HmrcEmploymentSource(
        employmentId,
        "Whiterun Guard",
        None, None, Some(startDate), None, None, None, Some(EmploymentFinancialData(Some(EmploymentData(
          startDate, None, None, None, None, None,
          Some(Pay(Some(3000.00), Some(300.00), Some("WEEKLY"), Some(submittedOnDate), Some(3), Some(3))),
          Some(Deductions(Some(StudentLoans(Some(1000.00), Some(3000.00)))))
        )), None)), None
      )

      s"redirect to the employment information overview page for $inYearText for an $affinityText when there is $prior in $welshLang" when {

        "the student loans submission is successful" in {
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
              taxYearEOY,
              employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = true,
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
                  uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                ))
              )
            ))
            userDataStub(incomeTaxUserData, nino, taxYearEOY)
            stubEmploymentPost(createUpdateRequest, taxYearEOY)

            urlPost(
              url(taxYearEOY),
              "{}",
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
            )
          }

          result.headers("Location").headOption shouldBe Some(employerInformationUrl(taxYearEOY, employmentId))
        }
      }

      s"redirect to the employment expenses page for $inYearText for an $affinityText when there is $prior in $welshLang" when {

        "the student loans submission is successful" in {
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
                  uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                ))
              )
            ))
            userDataStub(incomeTaxUserData, nino, taxYearEOY)
            stubEmploymentPost(createUpdateRequest, taxYearEOY)

            urlPost(
              url(taxYearEOY),
              "{}",
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, extraData = Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId)))
            )
          }

          result.headers("Location").headOption shouldBe
            Some(
              if(scenarioData.commonExpectedResults.hasPrior) claimEmploymentExpensesUrl(taxYearEOY) else checkYourExpensesUrl(taxYearEOY)
            )
        }
      }

      s"redirect to the employment information page for $inYearText for an $affinityText when there is $prior that is hmrc data $welshLang" when {

        "the student loans submission is successful" in {
          lazy val createUpdateRequest = CreateUpdateEmploymentRequest(
            Some(employmentId),
            None,
            Some(CreateUpdateEmploymentData(
              CreateUpdatePay(3000.00, 300.00),
              Some(Deductions(Some(StudentLoans(Some(2600.00), Some(4001.00)))))
            )),
            isHmrcEmploymentId = Some(true)
          )

          val result: WSResponse = {
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
                  employerName = "Whiterun Guards",
                  employerRef = Some("223/AB12399"),
                  startDate = Some(startDate),
                  didYouLeaveQuestion = Some(false),
                  taxablePayToDate = Some(3000.00),
                  totalTaxToDate = Some(300.00),
                  currentDataIsHmrcHeld = false
                ),
                studentLoans = Some(StudentLoansCYAModel(
                  uglDeduction = true, Some(2600.00), pglDeduction = true, Some(4001.00)
                ))
              )
            ))

            val data = incomeTaxUserData.copy(
              employment = incomeTaxUserData.employment.map(
                _.copy(
                  hmrcEmploymentData = Seq(hmrcData),
                  customerEmploymentData = Seq()
                )
              )
            )

            userDataStub(data, nino, taxYearEOY)
            stubEmploymentPost(createUpdateRequest, taxYearEOY, """{"employmentId":"id"}""")

            urlPost(
              url(taxYearEOY),
              "{}",
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
            )
          }

          result.headers("Location").headOption shouldBe Some(employerInformationUrl(taxYearEOY, "id"))
        }
      }

      s"redirect to the overview page for $inYearText for an $affinityText when there is $prior in $welshLang" when {

        "there is no CYA data in session" in {
          val result: WSResponse = {
            dropEmploymentDB()
            authoriseIndividual()
            userDataStub(incomeTaxUserData, nino, taxYearEOY)

            urlPost(url(taxYearEOY), "{}", scenarioData.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.headers("Location").headOption shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
        }
      }

      s"show an internal server error for $inYearText for an $affinityText when there is $prior in $welshLang" when {

        "an error is returned from the submission" in {
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
              taxYearEOY,
              employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = true,
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
                  uglDeduction = true, Some(2000.00), pglDeduction = true, Some(4000.00)
                ))
              )
            ))
            userDataStub(incomeTaxUserData, nino, taxYearEOY)
            stubEmploymentPostFailure(createUpdateRequest, taxYearEOY)

            urlPost(
              url(taxYearEOY),
              "{}",
              scenarioData.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
            )
          }

          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
