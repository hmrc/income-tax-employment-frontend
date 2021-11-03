/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import models.benefits.UtilitiesAndServicesModel
import models.employment.{AccommodationRelocationModel, BenefitsViewModel, CarVanFuelModel, TravelEntertainmentModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Call
import play.api.mvc.Results.Ok
import services.RedirectService.{EmploymentBenefitsType, EmploymentDetailsType}
import utils.UnitTest

import scala.concurrent.Future

class RedirectServiceSpec extends UnitTest {

  val cyaModel: EmploymentCYAModel = EmploymentCYAModel(EmploymentDetails("employerName", currentDataIsHmrcHeld = true))
  val taxYear = 2021

  val result = Future.successful(Ok("Wow"))

  val employmentCYA: EmploymentCYAModel = {
    EmploymentCYAModel(
      employmentDetails = EmploymentDetails(
        "Employer Name",
        employerRef = Some(
          "123/12345"
        ),
        startDate = Some("2020-11-11"),
        taxablePayToDate = Some(55.99),
        totalTaxToDate = Some(3453453.00),
        employmentSubmittedOn = Some("2020-04-04T01:01:01Z"),
        employmentDetailsSubmittedOn = Some("2020-04-04T01:01:01Z"),
        currentDataIsHmrcHeld = false
      ),
      employmentBenefits = Some(
        BenefitsViewModel(
          carVanFuelModel = Some(CarVanFuelModel(
            carVanFuelQuestion = Some(true),
            carQuestion = Some(true),
            car = Some(100.00),
            carFuelQuestion = Some(true),
            carFuel = Some(100.00),
            vanQuestion = Some(true),
            van = Some(100.00),
            vanFuelQuestion = Some(true),
            vanFuel = Some(100.00),
            mileageQuestion = Some(true),
            mileage = Some(100.00)
          )),
          accommodationRelocationModel = Some(
            AccommodationRelocationModel(
              accommodationRelocationQuestion = Some(true),
              accommodationQuestion = Some(true),
              accommodation = Some(100.00),
              qualifyingRelocationExpensesQuestion = Some(true),
              qualifyingRelocationExpenses = Some(100.00),
              nonQualifyingRelocationExpensesQuestion = Some(true),
              nonQualifyingRelocationExpenses = Some(100.00)
            )
          ),
          travelEntertainmentModel = Some(
            TravelEntertainmentModel(
              travelEntertainmentQuestion = Some(true),
              travelAndSubsistenceQuestion = Some(true),
              travelAndSubsistence = Some(555.00),
              personalIncidentalExpensesQuestion = Some(true),
              personalIncidentalExpenses = Some(555.00),
              entertainingQuestion = Some(true),
              entertaining = Some(555.00)
            )
          ),
          utilitiesAndServicesModel = Some(
            UtilitiesAndServicesModel(
              utilitiesAndServicesQuestion = Some(true),
              telephoneQuestion = Some(true),
              telephone = Some(555),
              employerProvidedServicesQuestion = Some(true),
              employerProvidedServices = Some(555),
              employerProvidedProfessionalSubscriptionsQuestion = Some(true),
              employerProvidedProfessionalSubscriptions = Some(555),
              serviceQuestion = Some(true),
              service = Some(555)
            )
          ),
          assets = Some(100), submittedOn = Some("2020-02-04T05:01:01Z"), isUsingCustomerData = true,
          isBenefitsReceived = true
        )
      ))
  }

  val employmentUserData: EmploymentUserData = EmploymentUserData(sessionId, mtditid, nino, taxYear, "001", isPriorSubmission = false, hasPriorBenefits = false, employmentCYA)

  "benefitsSubmitRedirect" should {
    "redirect to the CYA page if the journey is finished" in {
      val result = Future.successful(RedirectService.benefitsSubmitRedirect(employmentCYA, Call("GET", "/next"))(taxYear, "001"))

      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
    }
    "redirect to the next page if the journey is not finished" in {
      val result = Future.successful(RedirectService.benefitsSubmitRedirect(employmentCYA.copy(
        employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = None))
      ), Call("GET", "/next"))(taxYear, "001"))

      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe "/next"
    }
  }

  "redirectBasedOnCurrentAnswers" should {
    "redirect to benefits yes no page" when {
      "it's a new submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = None))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-benefits?employmentId=001"
      }
    }
    "redirect to car van fuel yes no page" when {
      "it's a new submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carVanFuelQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.carBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/car-van-fuel?employmentId=001"
      }
      "when benefits are setup but car van fuel is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = None))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/car-van-fuel?employmentId=001"
      }
    }
    "redirect to car yes no page" when {
      "it's a new submission and attempted to view the car fuel page without carQuestion being empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.carFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-car?employmentId=001"
      }
      "it's a new submission and attempted to view the car amount page without carQuestion being empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.carBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-car?employmentId=001"
      }
    }
    "redirect to car amount page" when {
      "it's a new submission and attempted to view the car fuel page with car amount being empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(car = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.carFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-car-amount?employmentId=001"
      }
    }
    "redirect to car fuel yes no page" when {
      "it's a new submission and attempted to view the car fuel amount page but the car fuel question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carFuelQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.carFuelBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/car-fuel?employmentId=001"
      }
    }
    "redirect to van page" when {
      "it's a new submission and attempted to view the car fuel amount page with car fuel being false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carFuelQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.carFuelBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-van?employmentId=001"
      }
      "it's a new submission and attempted to view the car fuel page with car being false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.carFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-van?employmentId=001"
      }
      "it's a new submission and attempted to view the van fuel page with van being empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.vanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-van?employmentId=001"
      }
      "it's a new submission and attempted to view the van amount page with van being empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.vanBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-van?employmentId=001"
      }
    }
    "redirect to van fuel yes no page" when {
      "it's a new submission and attempted to view the van fuel amount page but the van fuel question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanFuelQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.vanFuelBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/van-fuel?employmentId=001"
      }
    }
    "redirect using accommodation methods" when {
      "it's a new submission and attempted to view the accommodation page but the car question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.accommodationRelocationBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-car?employmentId=001"
      }
      "it's a new submission and attempted to view the accommodation yes no page but the accommodation relocation question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationRelocationQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonAccommodationBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/accommodation-relocation?employmentId=001"
      }
      "it's a new submission and attempted to view the qualifying relocation page but the accommodation amount is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodation = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.qualifyingRelocationBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/living-accommodation-amount?employmentId=001"
      }
      "it's a new submission and attempted to view the qualifying relocation amount page but the qualifyingRelocationExpensesQuestion is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(qualifyingRelocationExpensesQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.qualifyingRelocationBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/qualifying-relocation?employmentId=001"
      }
      "it's a new submission and attempted to view the non qualifying relocation yes no page but the qualifyingRelocationExpensesQuestion is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(qualifyingRelocationExpensesQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.nonQualifyingRelocationBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/qualifying-relocation?employmentId=001"
      }
      "it's a new submission and attempted to view the non qualifying relocation amount page but the nonQualifyingRelocationExpensesQuestion is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(nonQualifyingRelocationExpensesQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.nonQualifyingRelocationBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/non-qualifying-relocation?employmentId=001"
      }
      "it's a new submission and attempted to view the non qualifying relocation amount page but the nonQualifyingRelocationExpensesQuestion is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(nonQualifyingRelocationExpensesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.nonQualifyingRelocationBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/travel-entertainment?employmentId=001"
      }
      "it's a prior submission and attempted to view the non qualifying relocation amount page but the nonQualifyingRelocationExpensesQuestion is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(nonQualifyingRelocationExpensesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.nonQualifyingRelocationBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/travel-entertainment?employmentId=001"
      }
      "it's a prior submission and attempted to view the travel and entertainment page but the car section is not finished" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.travelEntertainmentBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-car?employmentId=001"
      }
      "it's a prior submission and attempted to view the travel and entertainment page but the accommodation section is not finished" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.travelEntertainmentBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/living-accommodation?employmentId=001"
      }
      "it's a new submission and attempted to view the non qualifying relocation yes no page but the accommodation yes no question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.nonQualifyingRelocationBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/living-accommodation?employmentId=001"
      }
      "it's a new submission and attempted to view the qualifying relocation amount page but the qualifyingRelocationExpensesQuestion is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(qualifyingRelocationExpensesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.qualifyingRelocationBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/non-qualifying-relocation?employmentId=001"
      }
      "it's a prior submission and attempted to view the qualifying relocation amount page but the qualifyingRelocationExpensesQuestion is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(qualifyingRelocationExpensesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.qualifyingRelocationBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the accommodation amount page but the accommodation question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.accommodationBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/living-accommodation?employmentId=001"
      }
      "it's a new submission and attempted to view the accommodation amount page but the accommodation question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.accommodationBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/qualifying-relocation?employmentId=001"
      }
      "it's a prior submission and attempted to view the accommodation amount page but the accommodation question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.accommodationBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the accommodation yes no page but the accommodation relocation question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationRelocationQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonAccommodationBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/travel-entertainment?employmentId=001"
      }
      "it's a prior submission and attempted to view the accommodation yes no page but the accommodation relocation question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationRelocationQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonAccommodationBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
    }
    "redirect using travel entertainment methods" when {
      "it's a new submission and attempted to view the travel entertainment page but the accommodation question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.travelEntertainmentBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/living-accommodation?employmentId=001"
      }
      "it's a new submission and attempted to view the travel yes no page but the travel entertainment question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelEntertainmentQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonTravelEntertainmentBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/travel-entertainment?employmentId=001"
      }
      "it's a new submission and attempted to view the travel yes no page but the travel entertainment question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelEntertainmentQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonTravelEntertainmentBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/utility-general-service?employmentId=001"
      }
      "it's a prior submission and attempted to view the travel yes no page but the travel entertainment question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelEntertainmentQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonTravelEntertainmentBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/travel-entertainment?employmentId=001"
      }
      "it's a new submission and attempted to view the travel amount page but the travel yes no question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.travelSubsistenceBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/travel-subsistence?employmentId=001"
      }
      "it's a new submission and attempted to view the travel amount page but the travel yes no question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.travelSubsistenceBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/incidental-overnight-costs?employmentId=001"
      }
      "it's a prior submission and attempted to view the travel amount page but the travel yes no question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.travelSubsistenceBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the incidental costs yes no page but the travel yes no question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.incidentalCostsBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/travel-subsistence?employmentId=001"
      }
      "it's a new submission and attempted to view the incidental costs yes no page but the travel amount question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistence = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.incidentalCostsBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/travel-subsistence-amount?employmentId=001"
      }
      "it's a new submission and attempted to view the incidental costs amount page but the incidental costs question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(personalIncidentalExpensesQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.incidentalCostsBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/incidental-overnight-costs?employmentId=001"
      }
      "it's a new submission and attempted to view the incidental costs amount page but the incidental costs question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(personalIncidentalExpensesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.incidentalCostsBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/entertainment-expenses?employmentId=001"
      }
      "it's a prior submission and attempted to view the incidental costs amount page but the incidental costs question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(personalIncidentalExpensesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.incidentalCostsBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the entertainment yes no page but the incidental costs question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(personalIncidentalExpensesQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.entertainmentBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the entertainment yes no page but the travel yes no question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.entertainmentBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/travel-subsistence?employmentId=001"
      }
      "it's a new submission and attempted to view the entertainment amount page but the entertainment yes no question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(entertainingQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.entertainmentBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/entertainment-expenses?employmentId=001"
      }
      "it's a new submission and attempted to view the entertainment amount page but the entertainment yes no question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(entertainingQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.entertainmentBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/utility-general-service?employmentId=001"
      }
      "it's a prior submission and attempted to view the entertainment amount page but the entertainment yes no question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(entertainingQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.entertainmentBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the utilities page but the entertainment question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            travelEntertainmentModel = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(entertainingQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.utilitiesBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/entertainment-expenses?employmentId=001"
      }
      "it's a new submission and attempted to view the utilities page but the car question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.utilitiesBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/company-car?employmentId=001"
      }
      "it's a new submission and attempted to view the utilities page but the accommodation question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.utilitiesBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/living-accommodation?employmentId=001"
      }
    }
    "redirect to mileage benefit yes no page" when {
      "it's a new submission and attempted to view the van fuel benefit amount page but the van fuel question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanFuelQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.vanFuelBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/mileage?employmentId=001"
      }
      "it's a new submission and attempted to view the van benefit amount page but the van benefit question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.vanBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/mileage?employmentId=001"
      }
      "it's a new submission and attempted to view the mileage benefit amount page but the mileage benefit question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(mileageQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.mileageBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/mileage?employmentId=001"
      }
    }
    "redirect using utilities and services methods" when {
      "it's a new submission and attempted to view the telephone yes no page but the utilities and services question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(utilitiesAndServicesQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonUtilitiesAndServicesBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/utility-general-service?employmentId=001"
      }
      "it's a new submission and attempted to view the telephone yes no page but the utilities and services question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(utilitiesAndServicesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonUtilitiesAndServicesBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a prior submission and attempted to view the telephone yes no page but the utilities and services question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(utilitiesAndServicesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonUtilitiesAndServicesBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a prior submission and attempted to view the telephone amount page but the telephone question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(telephoneQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.telephoneBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the telephone amount page but the telephone question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(telephoneQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.telephoneBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/employer-provided-services?employmentId=001"
      }
      "it's a new submission and attempted to view the telephone amount page but the telephone question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(telephoneQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.telephoneBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/telephone?employmentId=001"
      }
      "it's a new submission and attempted to view the employer provided services yes no page but the telephone amount is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(telephone = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.employerProvidedServicesBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the employer provided services amount page but the employer provided services question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(employerProvidedServicesQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.employerProvidedServicesAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/employer-provided-services?employmentId=001"
      }
      "it's a new submission and attempted to view the employer provided services amount page but the employer provided services question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(employerProvidedServicesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.employerProvidedServicesAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/professional-fees-or-subscriptions?employmentId=001"
      }
      "it's a prior submission and attempted to view the employer provided services amount page but the employer provided services question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(employerProvidedServicesQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.employerProvidedServicesAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the employer provided subscriptions page but the employer provided services question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(
            employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
              utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(
                _.utilitiesAndServicesModel).map(_.copy(employerProvidedServicesQuestion = None))
            ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.employerProvidedSubscriptionsBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/employer-provided-services?employmentId=001"
      }
      "it's a new submission and attempted to view the employer provided subscriptions amount page" +
        " but the employer provided subscriptions question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(
            employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
              utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(
                _.utilitiesAndServicesModel).map(_.copy(employerProvidedProfessionalSubscriptionsQuestion = None))
            ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.employerProvidedSubscriptionsBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/professional-fees-or-subscriptions?employmentId=001"
      }
      "it's a new submission and attempted to view the employer provided subscriptions amount page" +
        " but the employer provided subscriptions question is false" in {
        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(
            employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
              utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(
                _.utilitiesAndServicesModel).map(_.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false)))
            ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.employerProvidedSubscriptionsBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/other-services?employmentId=001"
      }
      "it's a prior submission and attempted to view the employer provided subscriptions amount page" +
        " but the employer provided subscriptions question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(
            employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
              utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(
                _.utilitiesAndServicesModel).map(_.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false)))
            ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.employerProvidedSubscriptionsBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the services page but the employer provided subscriptions amount is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(
            employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
              utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(
                _.utilitiesAndServicesModel).map(_.copy(employerProvidedProfessionalSubscriptions = None))
            ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.servicesBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and attempted to view the services amount page but the services question is empty" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(
            employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
              utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(
                _.utilitiesAndServicesModel).map(_.copy(serviceQuestion = None))
            ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.servicesBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/other-services?employmentId=001"
      }
      "it's a new submission and attempted to view the services amount page but the services question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employmentCYA.copy(
            employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
              utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(
                _.utilitiesAndServicesModel).map(_.copy(serviceQuestion = Some(false)))
            ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.servicesBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a prior submission and attempted to view the services amount page but the services question is false" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(
            employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
              utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(
                _.utilitiesAndServicesModel).map(_.copy(serviceQuestion = Some(false)))
            ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.servicesBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
    }
    "redirect using medical benefits methods" when {
      "it's a new submission and attempted to view the medical benefits page but the utilities section is not finished" in {
        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(
            employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
              utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(
                _.utilitiesAndServicesModel).map(_.copy(serviceQuestion = None))
            ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.medicalBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/other-services?employmentId=001"
      }
    }
    "redirect to benefits CYA page" when {
      "it's a prior submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = None))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and hitting the common benefits method when benefits received is false " in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            isBenefitsReceived = false
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and hitting the common car van fuel benefits method when carVanFuel is false " in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carVanFuelQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/accommodation-relocation?employmentId=001"
      }
      "it's a prior submission and hitting the common car van fuel benefits method when carVanFuel is false " in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carVanFuelQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a prior submission and hitting the car benefits amount method when carQuestion is false " in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.carBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a prior submission and hitting the car fuel benefits amount method when carFuelQuestion is false " in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carFuelQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.carFuelBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a prior submission and hitting the van benefits amount method when vanQuestion is false " in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.vanBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a prior submission and hitting the van fuel benefits amount method when vanFuelQuestion is false " in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanFuelQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.vanFuelBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a new submission and hitting the mileage benefits amount method when mileageQuestion is false " in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(mileageQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.mileageBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/benefits/accommodation-relocation?employmentId=001"
      }
      "it's a prior submission and hitting the mileage benefits amount method when mileageQuestion is false " in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(mileageQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(
          cya => {
            RedirectService.mileageBenefitsAmountRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
    }
    "redirect when CYA is empty" when {
      "it's a benefits submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001", None, EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-benefits?employmentId=001"
      }
      "it's a employment details submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001", None, EmploymentDetailsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=001"
      }
    }
    "continue with the request when benefits are setup and car van fuel is setup" when {
      "it's a new submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe OK
        bodyOf(response) shouldBe "Wow"
      }
      "it's a prior submission" in {

        val response = RedirectService.redirectBasedOnCurrentAnswers(taxYear, "001",
          Some(employmentUserData.copy(hasPriorBenefits = true)), EmploymentBenefitsType)(
          cya => {
            RedirectService.commonCarVanFuelBenefitsRedirects(cya, taxYear, "001")
          }
        ) {
          _ => result
        }

        status(response) shouldBe OK
        bodyOf(response) shouldBe "Wow"
      }
    }
  }

  "employmentDetailsRedirect" should {
    "redirect to check employment details page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel, taxYear, "employmentId", isPriorSubmission = true)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
    "redirect to employer reference page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel, taxYear, "employmentId", isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/employer-paye-reference?employmentId=employmentId"
    }
    "redirect to start date page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(employerRef = Some("123/12345"))), taxYear, "employmentId", isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/employment-start-date?employmentId=employmentId"
    }
    "redirect to still working for employer page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), payrollId = Some("id"), startDate = Some("2020-11-01"))), taxYear, "employmentId", isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/still-working-for-employer?employmentId=employmentId"
    }
    "redirect to pay page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), cessationDateQuestion = Some(true), cessationDate = Some("2020-10-01"), payrollId = Some("id")
      )), taxYear, "employmentId", isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/how-much-pay?employmentId=employmentId"
    }
    "redirect to tax page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), cessationDateQuestion = Some(true), cessationDate = Some("2020-10-10"), payrollId = Some("id"), taxablePayToDate = Some(1)
      )), taxYear, "employmentId", isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/uk-tax?employmentId=employmentId"
    }
    "redirect to payroll id page" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), cessationDateQuestion = Some(true), cessationDate = Some("2020-10-10"), taxablePayToDate = Some(1), totalTaxToDate = Some(1)
      )), taxYear, "employmentId", isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/payroll-id?employmentId=employmentId"
    }
    "redirect to employment end date page when no cessation date" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), cessationDateQuestion = Some(false), taxablePayToDate = Some(1),
        totalTaxToDate = Some(1), payrollId = Some("id")
      )), taxYear, "employmentId", isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/employment-end-date?employmentId=employmentId"
    }
    "redirect to check employment details page when no cessation date but the cessation question is no" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1), totalTaxToDate = Some(1),
        payrollId = Some("id"), cessationDateQuestion = Some(true)
      )), taxYear, "employmentId", isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
    "redirect to check employment details page when all filled in" in {

      val response = RedirectService.employmentDetailsRedirect(cyaModel.copy(cyaModel.employmentDetails.copy(
        employerRef = Some("123/12345"), startDate = Some("2020-11-01"), taxablePayToDate = Some(1), totalTaxToDate = Some(1),
        payrollId = Some("id"), cessationDateQuestion = Some(true), cessationDate = Some("2020-11-01")
      )), taxYear, "employmentId", isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe "/income-through-software/return/employment-income/2021/check-employment-details?employmentId=employmentId"
    }
  }

}
