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

package controllers.benefits.fuel

import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithBenefits
import utils.PageUrls.{carBenefitsAmountUrl, carFuelBenefitsUrl, fullUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class CompanyCarBenefitsAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  private val employmentId = "employmentId"
  private val benefitsWithNoCarAmount = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(true), carQuestion = Some(true))),
    isUsingCustomerData = true)
  private val benefitsWithCarAmount = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(true), carQuestion = Some(true),
      car = Some(100))), isUsingCustomerData = true)

  ".show" should {
    "redirect to Overview Page when in year" in {
      val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(carBenefitsAmountUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "render page successfully" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarAmount, isPriorSubmission = false, hasPriorBenefits = false))
        urlGet(fullUrl(carBenefitsAmountUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      getInputFieldValue() shouldBe ""
      result.status shouldBe OK
    }
  }

  ".submit" should {
    "redirect to Overview page when in year" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carBenefitsAmountUrl(taxYear, employmentId)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map("amount" -> "100"))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "render page with an error when validation fails" in {
      lazy val form: Map[String, String] = Map("amount" -> "")
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarAmount, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe BAD_REQUEST
    }

    "persist data and redirect to next page" which {
      lazy val form: Map[String, String] = Map("amount" -> 250.toString)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithCarAmount))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(carBenefitsAmountUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a SEE_OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carFuelBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.sectionQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion)) shouldBe Some(true)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car)) shouldBe Some(250)
      }
    }
  }
}
