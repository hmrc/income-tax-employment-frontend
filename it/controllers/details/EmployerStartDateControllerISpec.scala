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

package controllers.details

import forms.details.EmploymentDateForm
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{checkYourDetailsUrl, employmentStartDateUrl, fullUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

import java.time.LocalDate

class EmployerStartDateControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private val employerName: String = "HMRC"
  private val employmentStartDate: String = s"${taxYearEOY - 1}-01-01"
  private val employmentId: String = "employmentId"

  private def cyaModel(employerName: String, hmrc: Boolean) = EmploymentCYAModel(EmploymentDetails(
    employerName,
    employerRef = Some("123/12345"),
    didYouLeaveQuestion = Some(false),
    currentDataIsHmrcHeld = hmrc,
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
        insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
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
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> s"${taxYearEOY - 1}", EmploymentDateForm.month -> "01",
          EmploymentDateForm.day -> "")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the month is empty" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> s"${taxYearEOY - 1}", EmploymentDateForm.month -> "",
          EmploymentDateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the year is empty" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "", EmploymentDateForm.month -> "01",
          EmploymentDateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the day and month are empty" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> s"${taxYearEOY - 1}", EmploymentDateForm.month -> "",
          EmploymentDateForm.day -> "")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the day and year are empty" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "", EmploymentDateForm.month -> "01",
          EmploymentDateForm.day -> "")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the year and month are empty" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "", EmploymentDateForm.month -> "",
          EmploymentDateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the day, month and year are empty" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "", EmploymentDateForm.month -> "",
          EmploymentDateForm.day -> "")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the day is invalid" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> s"${taxYearEOY - 1}", EmploymentDateForm.month -> "01",
          EmploymentDateForm.day -> "abc")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the month is invalid" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> s"${taxYearEOY - 1}", EmploymentDateForm.month -> "abc",
          EmploymentDateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the year is invalid" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "abc", EmploymentDateForm.month -> "01",
          EmploymentDateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the date is an invalid date i.e. month is set to 13" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> s"${taxYearEOY - 1}", EmploymentDateForm.month -> "13",
          EmploymentDateForm.day -> "01")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the data is too long ago (must be after 1st January 1900)" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> "1900", EmploymentDateForm.month -> "1",
          EmploymentDateForm.day -> "1")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the date is a too recent date i.e. after 5thApril" which {
        lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> taxYearEOY.toString, EmploymentDateForm.month -> "04",
          EmploymentDateForm.day -> "06")

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
      }

      "the date is not in the past" which {
        val nowDatePlusOne = LocalDate.now().plusDays(1)
        lazy val form: Map[String, String] = Map(
          EmploymentDateForm.year -> nowDatePlusOne.getYear.toString,
          EmploymentDateForm.month -> nowDatePlusOne.getMonthValue.toString,
          EmploymentDateForm.day -> nowDatePlusOne.getDayOfMonth.toString)

        lazy val result: WSResponse = {
          dropEmploymentDB()
          insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
      urlPost(fullUrl(employmentStartDateUrl(taxYear, employmentId)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
    }

    "has an SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }
  }

  "create a new cya model with the employer start date" which {

    lazy val form: Map[String, String] = Map(EmploymentDateForm.year -> s"${taxYearEOY - 1}", EmploymentDateForm.month -> "01",
      EmploymentDateForm.day -> "01")

    lazy val result: WSResponse = {
      dropEmploymentDB()
      insertCyaData(anEmploymentUserData.copy(employment = cyaModel(employerName, hmrc = true)))
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(employmentStartDateUrl(taxYearEOY, employmentId)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
    }

    "redirects to the check your details page" in {
      result.status shouldBe SEE_OTHER
      result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
      lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
      cyaModel.employment.employmentDetails.startDate shouldBe Some(employmentStartDate)
    }
  }
}

