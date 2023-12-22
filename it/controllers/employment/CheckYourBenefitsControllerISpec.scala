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
import helpers.SessionCookieCrumbler.getSessionMap
import models.benefits.{Benefits, BenefitsViewModel}
import models.details.EmploymentDetails
import models.employment.createUpdate.{CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import models.employment.{Deductions, EmploymentBenefits}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import play.api.{Environment, Mode}
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import support.builders.models.employment.EmploymentBenefitsBuilder.anEmploymentBenefits
import support.builders.models.employment.EmploymentFinancialDataBuilder.aHmrcEmploymentFinancialData
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import support.builders.models.employment.StudentLoansBuilder.aStudentLoans
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class CheckYourBenefitsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  private lazy val filteredBenefits: Some[EmploymentBenefits] = Some(EmploymentBenefits(
    submittedOn = s"${taxYearEOY - 1}-02-12",
    benefits = Some(Benefits(
      van = Some(3.00),
      vanFuel = Some(4.00),
      mileage = Some(5.00),
    ))
  ))

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {
    "return a fully populated page when all the fields are populated for in year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "return only the relevant data on the page when only certain data items are in mongodb for EOY" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentSources = Seq(
          aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = filteredBenefits))))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = employmentSources))), nino, taxYear - 1)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "render Unauthorised user error page when the user is unauthorized" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        unauthorisedAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

    "return a redirect at the end of the year when id is not found" in {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentSources = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = Some(anEmploymentBenefits)))))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = employmentSources))), nino, taxYear)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, "0022")), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYearEOY)) shouldBe true
    }

    "redirect to the Did you receive any benefits page when its EOY and theres no benefits model in the session data" in {
      def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
        EmploymentUserData(sessionId, mtditid, nino, taxYear - 1, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employmentCyaModel)

      def cyaModel(employerName: String, hmrc: Boolean): EmploymentCYAModel =
        EmploymentCYAModel(
          EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc),
          None
        )

      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(employmentUserData(isPrior = false, cyaModel("test", hmrc = true)))
        authoriseAgentOrIndividual(isAgent = false)
        val employmentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData =
          Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None)))))
        userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, taxYear - 1)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(companyBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }

    "redirect to the Did you receive any benefits page when its EOY and theres no benefits model in the mongodb data" in {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val employmentData = anAllEmploymentData.copy(hmrcEmploymentData = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData =
          Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None)))))
        userDataStub(anIncomeTaxUserData.copy(Some(employmentData)), nino, taxYear)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(companyBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
    }

    "redirect to the Did you receive any benefits page when its EOY and the benefits journey is not finished" in {
      def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
        EmploymentUserData(sessionId, mtditid, nino, taxYear - 1, employmentId, isPriorSubmission = isPrior, hasPriorBenefits = isPrior, hasPriorStudentLoans = isPrior, employmentCyaModel)

      val cyaModel: EmploymentCYAModel = EmploymentCYAModel(
        EmploymentDetails("employerName", currentDataIsHmrcHeld = true),
        Some(aBenefitsViewModel.copy(utilitiesAndServicesModel = None, isBenefitsReceived = true)),
      )

      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(employmentUserData(isPrior = false, cyaModel))
        urlGet(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(utilitiesOrGeneralServicesBenefitsUrl(taxYearEOY, employmentId)) shouldBe true

    }

    "redirect to overview page when theres no benefits and in year" in {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val hmrcEmploymentData = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None))))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = hmrcEmploymentData))), nino, taxYear)
        urlGet(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }

    "redirect to overview page when theres no benefits and in year but employmentEOYEnabled is false" in {
      implicit lazy val result: Future[Result] = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        val request = FakeRequest("GET", checkYourBenefitsUrl(taxYearEOY, employmentId)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY))
        route(GuiceApplicationBuilder().in(Environment.simple(mode = Mode.Dev))
          .configure(config() + ("feature-switch.employmentEOYEnabled" -> "false"))
          .build(),
          request,
          "{}").get
      }

      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }
  }

  ".submit" when {
    "return a redirect when in year" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = true)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlPost(fullUrl(checkYourBenefitsUrl(taxYear, employmentId)), body = "{}", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "return internal server error page whilst not implemented" in {
      val employmentData = anEmploymentCYAModel().copy(employmentBenefits = None)
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(anEmploymentUserData.copy(employment = employmentData).copy(employmentId = employmentId))
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return a redirect to show method when at end of year and no cya data" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear - 1)
        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      "has a url of benefits show method" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    "create a model when adding employment benefits for the first time (adding new employment journey)" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val customerEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = customerEmploymentData))), nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(hasPriorBenefits = false, employment = anEmploymentCYAModel()).copy(employmentId = employmentId))

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
          None,
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))).get,
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))).get
              ),
              deductions = Some(Deductions(Some(aStudentLoans))),
              benefitsInKind = anEmploymentCYAModel().employmentBenefits.map(_.asBenefits),
              offPayrollWorker = anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.offPayrollWorker))
            )
          )
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY,
          extraData = Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))))
      }

      "return a redirect to the check studentLoans page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(studentLoansCyaPage(taxYearEOY, employmentId)) shouldBe true
        getSessionMap(result, "mdtp").get("TEMP_NEW_EMPLOYMENT_ID") shouldBe Some(employmentId)
      }
    }
    "create a model when adding employment benefits for the first time (adding new employment journey - studentLoans Disabled)" which {
      lazy val result: Result = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val customerEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = customerEmploymentData))), nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(hasPriorBenefits = false, employment = anEmploymentCYAModel()).copy(employmentId = employmentId))

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
          None,
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))).get,
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))).get
              ),
              deductions = Some(Deductions(Some(aStudentLoans))),
              benefitsInKind = anEmploymentCYAModel().employmentBenefits.map(_.asBenefits)
            )
          )
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val newHeaders = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY,
          extraData = Map(SessionValues.TEMP_NEW_EMPLOYMENT_ID -> employmentId))) ++ Seq("Csrf-Token" -> "nocheck")

        val request = FakeRequest("POST", checkYourBenefitsUrl(taxYearEOY, employmentId)).withHeaders(newHeaders: _*)
        await(route(appWithFeatureSwitchesOff, request, "{}").get)

      }

      "return a redirect to the check expenses page" in {
        result.header.headers("Location") shouldBe controllers.expenses.routes.CheckEmploymentExpensesController.show(taxYearEOY).toString
      }
    }

    "create a model when adding employment benefits for the first time but employment existed before" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val customerEmploymentData = Seq(anEmploymentSource.copy(employmentBenefits = None))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = customerEmploymentData))), nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(hasPriorBenefits = false, employment = anEmploymentCYAModel()).copy(employmentId = employmentId))

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
          None,
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))).get,
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))).get
              ),
              deductions = Some(Deductions(Some(aStudentLoans))),
              benefitsInKind = anEmploymentCYAModel().employmentBenefits.map(_.asBenefits),
              offPayrollWorker = anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.offPayrollWorker))
            )
          ),
          isHmrcEmploymentId = Some(true)
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "return a redirect to the check employment information page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerInformationUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
    "create a model when adding employment benefits for the first time but hmrc employment existed before" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        val hmrcEmploymentData = Seq(aHmrcEmploymentSource.copy(hmrcEmploymentFinancialData = Some(aHmrcEmploymentFinancialData.copy(employmentBenefits = None))))
        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = hmrcEmploymentData))), nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(hasPriorBenefits = false, employment = anEmploymentCYAModel()).copy(employmentId = employmentId))

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
          None,
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))).get,
                anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))).get
              ),
              deductions = Some(Deductions(Some(aStudentLoans))),
              benefitsInKind = anEmploymentCYAModel().employmentBenefits.map(_.asBenefits),
              offPayrollWorker = anAllEmploymentData.eoyEmploymentSourceWith(employmentId).flatMap(_.employmentSource.employmentData.flatMap(_.offPayrollWorker))
            )
          ),
          isHmrcEmploymentId = Some(true)
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", CREATED,
          Json.toJson(model).toString(), """{"employmentId":"id"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "return a redirect to the check employment information page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerInformationUrl(taxYearEOY, "id")) shouldBe true
      }
    }

    "update model and return a redirect when the user has prior benefits" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)

        val customerEmploymentData = Seq(anEmploymentSource.copy(employmentData = anEmploymentSource.employmentData.map(_.copy(
          pay = Some(anEmploymentSource.employmentData.flatMap(_.pay).get.copy(taxablePayToDate = Some(34786788.77), totalTaxToDate = Some(35553311.89))
          )))))

        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = customerEmploymentData))), nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel(), hasPriorBenefits = true))

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
          None,
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                34786788.77, 35553311.89
              ),
              deductions = Some(Deductions(Some(aStudentLoans))),
              benefitsInKind = Some(anEmploymentCYAModel().employmentBenefits.get.asBenefits),
              offPayrollWorker = Some(true)
            )
          )
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the employment summary page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerInformationUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
    "return a redirect when the user has prior benefits but is removing them" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)

        val customerEmploymentData = Seq(anEmploymentSource.copy(employmentData = anEmploymentSource.employmentData.map(_.copy(
          pay = Some(anEmploymentSource.employmentData.flatMap(_.pay).get.copy(taxablePayToDate = Some(34786788.77), totalTaxToDate = Some(35553311.89))
          )))))

        userDataStub(anIncomeTaxUserData.copy(Some(anAllEmploymentData.copy(hmrcEmploymentData = Seq(), customerEmploymentData = customerEmploymentData))), nino, taxYearEOY)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentBenefits = Some(
          BenefitsViewModel(
            isUsingCustomerData = true, isBenefitsReceived = true
          )
        )), hasPriorBenefits = true))

        val model = CreateUpdateEmploymentRequest(
          Some(employmentId),
          None,
          Some(
            CreateUpdateEmploymentData(
              pay = CreateUpdatePay(
                34786788.77, 35553311.89
              ),
              deductions = Some(Deductions(Some(aStudentLoans))),
              benefitsInKind = None,
              offPayrollWorker = Some(true)
            )
          )
        )

        stubPostWithHeadersCheck(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYearEOY", NO_CONTENT,
          Json.toJson(model).toString(), "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        urlPost(fullUrl(checkYourBenefitsUrl(taxYearEOY, employmentId)), body = "{}", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirect to the employer information page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(employerInformationUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
}