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

package controllers.employment

import common.SessionValues
import controllers.details.routes.EmployerStartDateController
import models.employment._
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import play.api.{Environment, Mode}
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentFinancialDataBuilder.aHmrcEmploymentFinancialData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import support.builders.models.employment.PayBuilder.aPay
import support.builders.models.employment.StudentLoansBuilder.aStudentLoans
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class CheckEmploymentDetailsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  object MinModel {
    val miniData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(
        HmrcEmploymentSource(
          employmentId = employmentId,
          employerName = "maggie",
          employerRef = None,
          payrollId = None,
          startDate = None,
          cessationDate = None,
          dateIgnored = None,
          submittedOn = None,
          hmrcEmploymentFinancialData = Some(
            EmploymentFinancialData(
              employmentData = Some(EmploymentData(
                submittedOn = s"${taxYearEOY - 1}-02-12",
                employmentSequenceNumber = None,
                companyDirector = None,
                closeCompany = None,
                directorshipCeasedDate = None,
                disguisedRemuneration = None,
                pay = Some(aPay),
                Some(Deductions(
                  studentLoans = Some(StudentLoans(
                    uglDeductionAmount = Some(100.00),
                    pglDeductionAmount = Some(100.00)
                  ))
                ))
              )),
              None
            )
          ),
          None
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  object CustomerMinModel {
    val miniData: AllEmploymentData = AllEmploymentData(MinModel.miniData.hmrcEmploymentData, None,
      customerEmploymentData = Seq(
        EmploymentSource(
          employmentId = employmentId,
          employerName = "maggie",
          employerRef = None,
          payrollId = None,
          startDate = None,
          cessationDate = None,
          dateIgnored = None,
          submittedOn = None,
          employmentData = Some(EmploymentData(
            submittedOn = s"${taxYearEOY - 1}-02-12",
            employmentSequenceNumber = None,
            companyDirector = None,
            closeCompany = None,
            directorshipCeasedDate = None,
            disguisedRemuneration = None,
            pay = Some(Pay(Some(34234.50), Some(6782.90), None, None, None, None)),
            Some(Deductions(
              studentLoans = Some(StudentLoans(
                uglDeductionAmount = Some(100.00),
                pglDeductionAmount = Some(100.00)
              ))
            ))
          )),
          None
        )
      ),
      customerExpenses = None
    )
  }

  object SomeModelWithInvalidDateFormat {
    val invalidData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(
        HmrcEmploymentSource(
          employmentId = employmentId,
          employerName = "maggie",
          employerRef = None,
          payrollId = None,
          startDate = None,
          cessationDate = None,
          dateIgnored = None,
          submittedOn = None,
          hmrcEmploymentFinancialData = Some(
            EmploymentFinancialData(
              employmentData = Some(EmploymentData(
                submittedOn = s"${taxYearEOY - 1}-02-12",
                employmentSequenceNumber = None,
                companyDirector = Some(true),
                closeCompany = Some(true),
                directorshipCeasedDate = Some("14/07/1990"),
                disguisedRemuneration = None,
                pay = Some(Pay(Some(100), Some(200), None, None, None, None)),
                Some(Deductions(
                  studentLoans = Some(StudentLoans(
                    uglDeductionAmount = Some(100.00),
                    pglDeductionAmount = Some(100.00)
                  ))
                ))
              )),
              None
            )
          ),
          None
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  ".show" when {
    "return page when in year and all the fields are populated" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "for in year with multiple employment sources, return a fully populated page when all fields are populated" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val multipleSources: Seq[HmrcEmploymentSource] = Seq(
          aHmrcEmploymentSource,
          aHmrcEmploymentSource.copy(
            employmentId = "002",
            employerName = "dave",
            payrollId = Some("12345693"),
            startDate = Some("2018-04-18"),
          ))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = multipleSources))), nino, taxYear)
        urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "for end of year when data not submittable" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        val employmentDetails = anEmploymentDetails.copy(employerRef = None, startDate = None, payrollId = None, didYouLeaveQuestion = Some(false), taxablePayToDate = None, totalTaxToDate = None)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(employmentDetails = employmentDetails)))
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    "for end of year return a fully populated page when cya data exists" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(EmploymentUserData(
          sessionId,
          "1234567890",
          "AA123456A",
          taxYearEOY,
          employmentId,
          isPriorSubmission = true,
          hasPriorBenefits = true,
          hasPriorStudentLoans = true,
          EmploymentCYAModel(
            anEmploymentSource.toEmploymentDetails(isUsingCustomerData = false).copy(didYouLeaveQuestion = Some(true)),
            None
          )
        ))
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "for end of year return a fully populated page, with change links, when all the fields are populated" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "for end of year return a fully populated page, with change links when minimum data is returned" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(MinModel.miniData)), nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "for end of year return customer employment data if there is both HMRC and customer Employment Data " +
      "and render page without filtering when minimum data is returned" when {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(CustomerMinModel.miniData)), nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect to the overview page when prior data exists but employmentEOYEnabled is false" in {
      implicit lazy val result: Future[Result] = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val request = FakeRequest("GET", checkYourDetailsUrl(taxYearEOY, employmentId)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
        route(GuiceApplicationBuilder().in(Environment.simple(mode = Mode.Dev))
          .configure(config() + ("feature-switch.employmentEOYEnabled" -> "false"))
          .build(),
          request,
          "{}").get
      }

      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }

    "redirect to the overview page when prior data exists but not matching the id" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, "001")), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the overview page when no data exists" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        noUserDataStub(nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYearEOY)) shouldBe true
      }
    }

    "for end of year return a redirect when cya data exists but not finished when its a new employment" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(EmploymentUserData(
          sessionId,
          "1234567890",
          "AA123456A",
          taxYearEOY,
          employmentId,
          isPriorSubmission = false,
          hasPriorBenefits = true,
          hasPriorStudentLoans = false,
          EmploymentCYAModel(anEmploymentSource.toEmploymentDetails(false).copy(startDate = None), None)
        ))
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlGet(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        unauthorisedAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

    "redirect to overview page when theres no details" in {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq.empty))), nino, taxYear)
        urlGet(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }
  }

  ".submit" when {
    "redirect when in year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, 2022)
        urlPost(fullUrl(checkYourDetailsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect when at the end of the year when no cya data" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "create the model to update the data and return the correct redirect when there is a hmrc employment to ignore" which {
      val employmentData: EmploymentCYAModel = anEmploymentCYAModel.copy(employmentBenefits = None)
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = employmentData).copy(employmentId = employmentId, hasPriorBenefits = false))
        val hmrcEmploymentData = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None))))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = hmrcEmploymentData))), nino, taxYearEOY)

        val model = CreateUpdateEmploymentRequest(
          None,
          Some(
            CreateUpdateEmployment(
              employmentData.employmentDetails.employerRef,
              employmentData.employmentDetails.employerName,
              employmentData.employmentDetails.startDate.get,
              payrollId = anEmploymentCYAModel.employmentDetails.payrollId
            )
          ),
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                employmentData.employmentDetails.taxablePayToDate.get,
                employmentData.employmentDetails.totalTaxToDate.get,
              ),
              deductions = Some(Deductions(Some(aStudentLoans)))
            )
          ),
          Some(employmentId)
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", CREATED,
          Json.toJson(model).toString(), """{"employmentId":"id"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerInformationUrl(taxYearEOY, "id")) shouldBe true
        findCyaData(taxYear, employmentId, anAuthorisationRequest) shouldBe None
      }
    }
    "create the model to update the data and return the correct redirect and return to employer information page" which {
      val employmentData: EmploymentCYAModel = anEmploymentCYAModel.copy(employmentBenefits = None)
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = employmentData).copy(employmentId = employmentId, hasPriorBenefits = false))
        val hmrcEmploymentData = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None))))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = hmrcEmploymentData))), nino, taxYearEOY)

        val model = CreateUpdateEmploymentRequest(
          None,
          Some(
            CreateUpdateEmployment(
              employmentData.employmentDetails.employerRef,
              employmentData.employmentDetails.employerName,
              employmentData.employmentDetails.startDate.get,
              payrollId = anEmploymentCYAModel.employmentDetails.payrollId
            )
          ),
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                employmentData.employmentDetails.taxablePayToDate.get,
                employmentData.employmentDetails.totalTaxToDate.get,
              ),
              deductions = Some(Deductions(Some(aStudentLoans)))
            )
          ),
          Some(employmentId)
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerInformationUrl(taxYearEOY, employmentId)) shouldBe true
        findCyaData(taxYear, employmentId, anAuthorisationRequest) shouldBe None
      }
    }

    "create the model to create the data and return the correct redirect" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(hasPriorBenefits = false))
        noUserDataStub(nino, taxYearEOY)

        val model = CreateUpdateEmploymentRequest(
          None,
          Some(CreateUpdateEmployment(
            anEmploymentCYAModel.employmentDetails.employerRef,
            anEmploymentCYAModel.employmentDetails.employerName,
            anEmploymentCYAModel.employmentDetails.startDate.get,
            payrollId = anEmploymentCYAModel.employmentDetails.payrollId
          )),
          Some(CreateUpdateEmploymentData(
            pay = CreateUpdatePay(
              anEmploymentCYAModel.employmentDetails.taxablePayToDate.get,
              anEmploymentCYAModel.employmentDetails.totalTaxToDate.get,
            )
          ))
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", CREATED,
          Json.toJson(model).toString(), """{"employmentId": "id"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY,
          extraData = Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, "id")) shouldBe true
        findCyaData(taxYear, employmentId, anAuthorisationRequest) shouldBe None
      }
    }

    "create the model to update the data and return the correct redirect when not all data is present" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        urlPost(fullUrl(checkYourDetailsUrl(taxYearEOY, "001")), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "{}")
      }

      "has an SEE OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourDetailsUrl(taxYearEOY, "001")) shouldBe true
      }
    }
  }
}