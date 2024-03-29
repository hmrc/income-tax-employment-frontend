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

package controllers.details

import forms.details.DateForm
import models.details.EmploymentDetails
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{checkYourDetailsUrl, didYouLeaveUrl, employmentStartDateUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import java.time.LocalDate

class EmployerStartDateControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val employerName: String = "HMRC"
  private val employmentStartDate: String = s"${taxYearEOY - 1}-01-01"
  private val employmentId: String = "employmentId"

  private def cyaModel(employerName: String) = EmploymentCYAModel(EmploymentDetails(
    employerName,
    employerRef = Some("123/12345"),
    didYouLeaveQuestion = Some(false),
    currentDataIsHmrcHeld = true,
    payrollId = Some("12345"),
    taxablePayToDate = Some(5),
    totalTaxToDate = Some(5)
  ))

  object CyaModel {
    val cya: EmploymentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = true, hasPriorStudentLoans = true,
      EmploymentCYAModel(
        EmploymentDetails(employerName, startDate = Some(s"${taxYearEOY - 1}-01-01"), currentDataIsHmrcHeld = false),
        None
      )
    )
  }

  ".show" should {

    "render the 'start date' page with the correct content" which {
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect the user to the overview page when it is not end of year" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employmentStartDateUrl(taxYear, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }


  ".submit" should {

    s"return a BAD_REQUEST($BAD_REQUEST) status" when {

      "the day is empty" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> s"${taxYearEOY - 1}", DateForm.month -> "01",
          DateForm.day -> "")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the month is empty" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> s"${taxYearEOY - 1}", DateForm.month -> "",
          DateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the year is empty" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> "", DateForm.month -> "01",
          DateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the day and month are empty" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> s"${taxYearEOY - 1}", DateForm.month -> "",
          DateForm.day -> "")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the day and year are empty" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> "", DateForm.month -> "01",
          DateForm.day -> "")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the year and month are empty" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> "", DateForm.month -> "",
          DateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the day, month and year are empty" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> "", DateForm.month -> "",
          DateForm.day -> "")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the day is invalid" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> s"${taxYearEOY - 1}", DateForm.month -> "01",
          DateForm.day -> "abc")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the month is invalid" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> s"${taxYearEOY - 1}", DateForm.month -> "abc",
          DateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the year is invalid" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> "abc", DateForm.month -> "01",
          DateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the date is an invalid date i.e. month is set to 13" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> s"${taxYearEOY - 1}", DateForm.month -> "13",
          DateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the date is not after 1st January 1900" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> "1900", DateForm.month -> "1",
          DateForm.day -> "1")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the date is after 5th April" which {
        lazy val form: Map[String, String] = Map(DateForm.year -> taxYearEOY.toString, DateForm.month -> "04",
          DateForm.day -> "06")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the date is not in the past" which {
        val nowDatePlusOne = LocalDate.now().plusDays(1)
        lazy val form: Map[String, String] = Map(
          DateForm.year -> nowDatePlusOne.getYear.toString,
          DateForm.month -> nowDatePlusOne.getMonthValue.toString,
          DateForm.day -> nowDatePlusOne.getDayOfMonth.toString)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }
    }
  }

  "redirect the user to the overview page when it is not end of year" which {
    lazy val result: WSResponse = {
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(employmentStartDateUrl(taxYear, employmentId)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
    }

    "has an SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }
  }

  "create a new cya model with the employer start date" which {
    lazy val form: Map[String, String] = Map(DateForm.year -> s"${taxYearEOY - 1}", DateForm.month -> "01", DateForm.day -> "01")
    lazy val result: WSResponse = {
      dropEmploymentDB()
      insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel().copy(employmentDetails = anEmploymentDetails.copy(didYouLeaveQuestion = None))))
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
    }

    "redirects to the 'Did you leave employer?' page" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(didYouLeaveUrl(taxYearEOY, employmentId))
      lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
      cyaModel.employment.employmentDetails.startDate shouldBe Some(employmentStartDate)
    }
  }

  "update an existing cya model with the employer start date" which {
    lazy val form: Map[String, String] = Map(DateForm.year -> s"${taxYearEOY - 1}", DateForm.month -> "01", DateForm.day -> "01")
    lazy val result: WSResponse = {
      dropEmploymentDB()
      insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName)))
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
    }

    "redirects to the 'Check your details' page" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(checkYourDetailsUrl(taxYearEOY, employmentId))
      lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
      cyaModel.employment.employmentDetails.startDate shouldBe Some(employmentStartDate)
    }
  }
}
