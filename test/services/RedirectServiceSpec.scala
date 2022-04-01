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

package services

import controllers.benefits.accommodation.routes._
import controllers.benefits.assets.routes._
import controllers.benefits.fuel.routes._
import controllers.benefits.income.routes._
import controllers.benefits.medical.routes._
import controllers.benefits.reimbursed.routes._
import controllers.benefits.routes.ReceiveAnyBenefitsController
import controllers.benefits.travel.routes._
import controllers.benefits.utilities.routes._
import controllers.employment.routes._
import models.benefits._
import models.employment.{EmploymentBenefitsType, EmploymentDetailsType}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Call
import play.api.mvc.Results.Ok
import services.RedirectService._
import support.builders.models.benefits.AssetsModelBuilder.anAssetsModel
import support.builders.models.benefits.ReimbursedCostsVouchersAndNonCashModelBuilder.aReimbursedCostsVouchersAndNonCashModel
import utils.UnitTest

import scala.concurrent.Future

class RedirectServiceSpec extends UnitTest {

  private val cyaModel: EmploymentCYAModel = EmploymentCYAModel(EmploymentDetails("employerName", currentDataIsHmrcHeld = true))

  private val result = Future.successful(Ok("Wow"))

  private val fullMedicalModel = MedicalChildcareEducationModel(
    sectionQuestion = Some(true),
    medicalInsuranceQuestion = Some(true),
    medicalInsurance = Some(250.00),
    nurseryPlacesQuestion = Some(true),
    nurseryPlaces = Some(250.00),
    educationalServicesQuestion = Some(true),
    educationalServices = Some(250.00),
    beneficialLoanQuestion = Some(true),
    beneficialLoan = Some(250.00)
  )

  private val fullIncomeTaxAndCostsModel = IncomeTaxAndCostsModel(
    sectionQuestion = Some(true),
    incomeTaxPaidByDirectorQuestion = Some(true),
    incomeTaxPaidByDirector = Some(255.00),
    paymentsOnEmployeesBehalfQuestion = Some(true),
    paymentsOnEmployeesBehalf = Some(255.00)
  )

  private val employmentCYA: EmploymentCYAModel = {
    EmploymentCYAModel(
      employmentDetails = EmploymentDetails(
        "Employer Name",
        employerRef = Some(
          "123/12345"
        ),
        startDate = Some(s"${taxYearEOY-1}-11-11"),
        taxablePayToDate = Some(55.99),
        totalTaxToDate = Some(3453453.00),
        employmentSubmittedOn = Some(s"${taxYearEOY-1}-04-04T01:01:01Z"),
        employmentDetailsSubmittedOn = Some(s"${taxYearEOY-1}-04-04T01:01:01Z"),
        currentDataIsHmrcHeld = false
      ),
      employmentBenefits = Some(
        BenefitsViewModel(
          carVanFuelModel = Some(CarVanFuelModel(
            sectionQuestion = Some(true),
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
              sectionQuestion = Some(true),
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
              sectionQuestion = Some(true),
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
              sectionQuestion = Some(true),
              telephoneQuestion = Some(true),
              telephone = Some(555.00),
              employerProvidedServicesQuestion = Some(true),
              employerProvidedServices = Some(555.00),
              employerProvidedProfessionalSubscriptionsQuestion = Some(true),
              employerProvidedProfessionalSubscriptions = Some(555.00),
              serviceQuestion = Some(true),
              service = Some(555.00)
            )
          ),
          medicalChildcareEducationModel = Some(fullMedicalModel),
          incomeTaxAndCostsModel = Some(fullIncomeTaxAndCostsModel),
          reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel),
          assetsModel = Some(AssetsModel(Some(true), Some(true), Some(100), Some(true), Some(100))),
          submittedOn = Some(s"${taxYearEOY-1}-02-04T05:01:01Z"),
          isUsingCustomerData = true,
          isBenefitsReceived = true
        )
      ))
  }

  private val employmentId = "001"
  private val employmentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = false, hasPriorBenefits = false, hasPriorStudentLoans = false, employmentCYA)

  "benefitsSubmitRedirect" should {
    "redirect to the CYA page if the journey is finished" in {
      val result = Future.successful(RedirectService.benefitsSubmitRedirect(employmentCYA, Call("GET", "/next"))(taxYearEOY, employmentId))

      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
    }

    "redirect to the next page if the journey is not finished" in {
      val result = Future.successful(RedirectService.benefitsSubmitRedirect(employmentCYA.copy(
        employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = None))
      ), Call("GET", "/next"))(taxYearEOY, employmentId))

      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe "/next"
    }

    "redirect to the next page if the journey is not finished for travel section" in {
      val result = Future.successful(RedirectService.benefitsSubmitRedirect(employmentCYA.copy(
        employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = None))
      ), Call("GET", "/next"))(taxYearEOY, "001"))

      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe "/next"
    }

    "redirect to the next page if the journey is not finished for utilities section" in {
      val result = Future.successful(RedirectService.benefitsSubmitRedirect(employmentCYA.copy(
        employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = None))
      ), Call("GET", "/next"))(taxYearEOY, "001"))

      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe "/next"
    }
  }

  "getUnfinishedRedirects" should {
    "return an empty sequence" when {
      "the CYA data is complete" in {
        val result = RedirectService.getUnfinishedRedirects(employmentCYA, taxYearEOY, employmentId)

        result shouldBe Seq()
      }

      "the CYA data is empty and isBenefitsReceived false" in {
        val result = RedirectService.getUnfinishedRedirects(employmentCYA.copy(
          employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = None, accommodationRelocationModel = None,
            travelEntertainmentModel = None, utilitiesAndServicesModel = None, medicalChildcareEducationModel = None,
            incomeTaxAndCostsModel = None, reimbursedCostsVouchersAndNonCashModel = None, assetsModel = None, isBenefitsReceived = false))), taxYearEOY, employmentId)

        result shouldBe Seq()
      }
    }

    "return a sequence with calls in" when {
      "the CYA data is partially complete and isBenefitsReceived true" in {
        val result = RedirectService.getUnfinishedRedirects(employmentCYA.copy(
          employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = None))), taxYearEOY, employmentId)

        result.isEmpty shouldBe false
      }
    }
  }

  "redirectBasedOnCurrentAnswers" should {
    "redirect to benefits yes no page" when {
      "it's a new submission" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = None))), EmploymentBenefitsType)(
          cya => commonCarVanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ReceiveAnyBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect to car van fuel yes no page" when {
      "it's a new submission" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(sectionQuestion = None))))))),
          EmploymentBenefitsType)(cya => RedirectService.carBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CarVanFuelBenefitsController.show(taxYearEOY, employmentId).url
      }

      "when benefits are setup but car van fuel is empty" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => commonCarVanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CarVanFuelBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect to car yes no page" when {
      "it's a new submission and attempted to view the car fuel page without carQuestion being empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
          ))))), EmploymentBenefitsType)(cya => RedirectService.carFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyCarBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the car amount page without carQuestion being empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
          ))))), EmploymentBenefitsType)(cya => RedirectService.carBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyCarBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect to car amount page" when {
      "it's a new submission and attempted to view the car fuel page with car amount being empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(car = None))
          ))))), EmploymentBenefitsType)(cya => RedirectService.carFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe controllers.benefits.fuel.routes.CompanyCarBenefitsAmountController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect to car fuel yes no page" when {
      "it's a new submission and attempted to view the car fuel amount page but the car fuel question is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carFuelQuestion = None))
          ))))), EmploymentBenefitsType)(cya => RedirectService.carFuelBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyCarFuelBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect to van page" when {
      "it's a new submission and attempted to view the car fuel amount page with car fuel being false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carFuelQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(cya => RedirectService.carFuelBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyVanBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the car fuel page with car being false" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = Some(false)))
          ))))), EmploymentBenefitsType)(cya => RedirectService.carFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyVanBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the van fuel page with van being empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanQuestion = None))
          ))))), EmploymentBenefitsType)(
          cya => RedirectService.vanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyVanBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the van amount page with van being empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanQuestion = None))
          ))))), EmploymentBenefitsType)(cya => vanBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyVanBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect to van fuel yes no page" when {
      "it's a new submission and attempted to view the van fuel amount page but the van fuel question is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanFuelQuestion = None))
          ))))), EmploymentBenefitsType)(cya => vanFuelBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyVanFuelBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect using accommodation methods" when {
      "it's a new submission and attempted to view the accommodation page but the car question is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            carVanFuelModel = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
          ))))), EmploymentBenefitsType)(cya => RedirectService.accommodationRelocationBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyCarBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the accommodation yes no page but the accommodation relocation question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(sectionQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonAccommodationBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the qualifying relocation page but the accommodation amount is empty" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(
            accommodationRelocationModel = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodation = None))
          ))))), EmploymentBenefitsType)(cya => RedirectService.qualifyingRelocationBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe LivingAccommodationBenefitAmountController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the qualifying relocation amount page but the qualifyingRelocationExpensesQuestion is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(qualifyingRelocationExpensesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.qualifyingRelocationBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe QualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the non qualifying relocation yes no page but the qualifyingRelocationExpensesQuestion is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(qualifyingRelocationExpensesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.nonQualifyingRelocationBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe QualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the non qualifying relocation amount page but the nonQualifyingRelocationExpensesQuestion is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(nonQualifyingRelocationExpensesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.nonQualifyingRelocationBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe NonQualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the non qualifying relocation amount page but the nonQualifyingRelocationExpensesQuestion is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(nonQualifyingRelocationExpensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.nonQualifyingRelocationBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the non qualifying relocation amount page but the nonQualifyingRelocationExpensesQuestion is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(nonQualifyingRelocationExpensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.nonQualifyingRelocationBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the travel and entertainment page but the car section is not finished" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.travelEntertainmentBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyCarBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the travel and entertainment page but the accommodation section is not finished" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.travelEntertainmentBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the non qualifying relocation yes no page but the accommodation yes no question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.nonQualifyingRelocationBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the qualifying relocation amount page but the qualifyingRelocationExpensesQuestion is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(qualifyingRelocationExpensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.qualifyingRelocationBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe NonQualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the qualifying relocation amount page but the qualifyingRelocationExpensesQuestion is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(qualifyingRelocationExpensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.qualifyingRelocationBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the accommodation amount page but the accommodation question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.accommodationBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the accommodation amount page but the accommodation question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.accommodationBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe QualifyingRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the accommodation amount page but the accommodation question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.accommodationBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the accommodation yes no page but the accommodation relocation question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonAccommodationBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the accommodation yes no page but the accommodation relocation question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonAccommodationBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect using travel entertainment methods" when {
      "it's a new submission and attempted to view the travel entertainment page but the accommodation question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.travelEntertainmentBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the travel yes no page but the travel entertainment question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(sectionQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonTravelEntertainmentBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the travel yes no page but the travel entertainment question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonTravelEntertainmentBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UtilitiesOrGeneralServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the travel yes no page but the travel entertainment question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(sectionQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonTravelEntertainmentBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the travel amount page but the travel yes no question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.travelSubsistenceBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelAndSubsistenceBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the travel amount page but the travel yes no question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.travelSubsistenceBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncidentalOvernightCostEmploymentBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the travel amount page but the travel yes no question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.travelSubsistenceBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the incidental costs yes no page but the travel yes no question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.incidentalCostsBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelAndSubsistenceBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the incidental costs yes no page but the travel amount question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistence = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.incidentalCostsBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelOrSubsistenceBenefitsAmountController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the incidental costs amount page but the incidental costs question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(personalIncidentalExpensesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.incidentalCostsBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncidentalOvernightCostEmploymentBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the incidental costs amount page but the incidental costs question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(personalIncidentalExpensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.incidentalCostsBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe EntertainingBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the incidental costs amount page but the incidental costs question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(personalIncidentalExpensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.incidentalCostsBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the entertainment yes no page but the incidental costs question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(personalIncidentalExpensesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.entertainmentBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the entertainment yes no page but the travel yes no question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(travelAndSubsistenceQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.entertainmentBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelAndSubsistenceBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the entertainment amount page but the entertainment yes no question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(entertainingQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.entertainmentBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe EntertainingBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the entertainment amount page but the entertainment yes no question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(entertainingQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.entertainmentBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UtilitiesOrGeneralServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the entertainment amount page but the entertainment yes no question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(entertainingQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.entertainmentBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the utilities page but the entertainment question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.travelEntertainmentModel).map(_.copy(entertainingQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.utilitiesBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe EntertainingBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the utilities page but the car question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.utilitiesBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CompanyCarBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the utilities page but the accommodation question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.accommodationRelocationModel).map(_.copy(accommodationQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.utilitiesBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe LivingAccommodationBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect to mileage benefit yes no page" when {
      "it's a new submission and attempted to view the van fuel benefit amount page but the van fuel question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanFuelQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => vanFuelBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ReceiveOwnCarMileageBenefitController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the van benefit amount page but the van benefit question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => vanBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ReceiveOwnCarMileageBenefitController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the mileage benefit amount page but the mileage benefit question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(mileageQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.mileageBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ReceiveOwnCarMileageBenefitController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect using utilities and services methods" when {
      "it's a new submission and attempted to view the telephone yes no page but the utilities and services question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(sectionQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonUtilitiesAndServicesBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UtilitiesOrGeneralServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the telephone yes no page but the utilities and services question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonUtilitiesAndServicesBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe MedicalDentalChildcareBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the telephone yes no page but the utilities and services question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonUtilitiesAndServicesBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the telephone amount page but the telephone question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(telephoneQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.telephoneBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the telephone amount page but the telephone question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(telephoneQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.telephoneBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe EmployerProvidedServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the telephone amount page but the telephone question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(telephoneQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.telephoneBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TelephoneBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the employer provided services yes no page but the telephone amount is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(telephone = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.employerProvidedServicesBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the employer provided services amount page but the employer provided services question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(employerProvidedServicesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => employerProvidedServicesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe EmployerProvidedServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the employer provided services amount page but the employer provided services question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(employerProvidedServicesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => employerProvidedServicesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ProfessionalSubscriptionsBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the employer provided services amount page but the employer provided services question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(employerProvidedServicesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => employerProvidedServicesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the employer provided subscriptions page but the employer provided services question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(employerProvidedServicesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.employerProvidedSubscriptionsBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe EmployerProvidedServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the employer provided subscriptions amount page" +
        " but the employer provided subscriptions question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(employerProvidedProfessionalSubscriptionsQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => employerProvidedSubscriptionsBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ProfessionalSubscriptionsBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the employer provided subscriptions amount page" +
        " but the employer provided subscriptions question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel)
          .map(_.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => employerProvidedSubscriptionsBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the employer provided subscriptions amount page" +
        " but the employer provided subscriptions question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel)
          .map(_.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => employerProvidedSubscriptionsBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the services page but the employer provided subscriptions amount is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(employerProvidedProfessionalSubscriptions = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.servicesBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ProfessionalSubscriptionsBenefitsAmountController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the services amount page but the services question is empty" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(serviceQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.servicesBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the services amount page but the services question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(serviceQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.servicesBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe MedicalDentalChildcareBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the services amount page but the services question is false" in {
        val model = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(serviceQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.servicesBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect using medical benefits methods" when {
      "it's a new submission and attempted to view the medical benefits page but the utilities section is not finished" in {
        val utilitiesAndServicesModel = employmentCYA.employmentBenefits.flatMap(_.utilitiesAndServicesModel).map(_.copy(serviceQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits
          .map(_.copy(utilitiesAndServicesModel = utilitiesAndServicesModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId,
          Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)), EmploymentBenefitsType)(cya =>
          RedirectService.medicalBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Medical or dental insurance' yes/no page but the Medical section question is empty" in {
        val emptyModel = Some(MedicalChildcareEducationModel())
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = emptyModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)), EmploymentBenefitsType)(
          cya => commonMedicalChildcareEducationRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe MedicalDentalChildcareBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Medical or dental insurance' yes/no page but the Medical section question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)), EmploymentBenefitsType)(
          cya => commonMedicalChildcareEducationRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncomeTaxOrIncurredCostsBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the 'Medical or dental insurance' yes/no page but the Medical section question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => commonMedicalChildcareEducationRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the 'Medical or dental insurance amount' page but the 'Medical or dental insurance' question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(medicalInsuranceQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => medicalInsuranceAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Medical or dental insurance amount' page but the 'Medical or dental insurance' question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(medicalInsuranceQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => medicalInsuranceAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ChildcareBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Medical or dental insurance amount' page but the 'Medical or dental insurance' question is empty" in {
        val medicalModel = Some(fullMedicalModel.copy(medicalInsuranceQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => medicalInsuranceAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe MedicalDentalBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Childcare' yes/no page but the medical insurance amount is empty" in {
        val medicalModel = Some(fullMedicalModel.copy(medicalInsurance = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => childcareRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe MedicalOrDentalBenefitsAmountController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Childcare amount' page but the childcare question is empty" in {
        val medicalModel = Some(fullMedicalModel.copy(nurseryPlacesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => childcareAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ChildcareBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Childcare amount' page but the childcare question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(nurseryPlacesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => childcareAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the 'Childcare amount' page but the childcare question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(nurseryPlacesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => childcareAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Educational services' page but the 'Childcare' question is empty" in {
        val medicalModel = Some(fullMedicalModel.copy(nurseryPlacesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => educationalServicesRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ChildcareBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Educational services amount' page but the Educational services question is empty" in {
        val medicalModel = Some(fullMedicalModel.copy(educationalServicesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => educationalServicesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Educational services amount' page but the Educational services question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(educationalServicesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => educationalServicesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe BeneficialLoansBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the 'Educational services amount' page but the Educational services question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(educationalServicesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => educationalServicesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Beneficial loans' but the educational services amount is empty" in {
        val medicalModel = Some(fullMedicalModel.copy(educationalServices = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => beneficialLoansRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe EducationalServicesBenefitsAmountController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Beneficial loans amount' page but the Beneficial loans question is empty" in {
        val medicalModel = Some(fullMedicalModel.copy(beneficialLoanQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => beneficialLoansAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe BeneficialLoansBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Beneficial loans amount' page but the Beneficial loans question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(beneficialLoanQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => beneficialLoansAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncomeTaxOrIncurredCostsBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the 'Beneficial loans amount' page but the Beneficial loans question is false" in {
        val medicalModel = Some(fullMedicalModel.copy(beneficialLoanQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => beneficialLoansAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect using Income Tax and incurred costs methods" when {
      "it's a new submission and attempted to view the 'Income Tax or incurred costs' but the medical section is not finished" in {
        val medicalModel = Some(fullMedicalModel.copy(beneficialLoanQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = medicalModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => incomeTaxAndCostsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe BeneficialLoansBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Income Tax paid by employer' yes/no page but the Income Tax section question is empty" in {
        val emptyModel = Some(IncomeTaxAndCostsModel())
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = emptyModel)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => commonIncomeTaxAndCostsModelRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncomeTaxOrIncurredCostsBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Income Tax paid by employer' yes/no page but the Income Tax section question is false" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)), EmploymentBenefitsType)(
          cya => commonIncomeTaxAndCostsModelRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the 'Income Tax paid by employer' yes/no page but the Income Tax section question is false" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => commonIncomeTaxAndCostsModelRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the 'Income Tax paid by employer amount' page but the 'Income Tax paid by employer' question is false" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirectorQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => incomeTaxPaidByDirectorAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Income Tax paid by employer amount' page but the 'Income Tax paid by employer' question is false" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirectorQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => incomeTaxPaidByDirectorAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncurredCostsBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Income Tax paid by employer amount' page but the 'Income Tax paid by employer' question is empty" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirectorQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => incomeTaxPaidByDirectorAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncomeTaxBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Incurred costs paid by employer' yes/no page but the Income Tax paid by employer amount is empty" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(incomeTaxPaidByDirector = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => incurredCostsPaidByEmployerRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncomeTaxBenefitsAmountController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Incurred costs paid by employer amount' page " +
        "but the Incurred costs paid by employer question is empty" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(paymentsOnEmployeesBehalfQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => incurredCostsPaidByEmployerAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncurredCostsBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the 'Incurred costs paid by employer amount' page " +
        "but the Incurred costs paid by employer question is false" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(paymentsOnEmployeesBehalfQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => incurredCostsPaidByEmployerAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view the 'Incurred costs paid by employer amount' page " +
        "but the Incurred costs paid by employer question is false" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(paymentsOnEmployeesBehalfQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => incurredCostsPaidByEmployerAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect using Reimbursed costs, vouchers, and non-cash benefits methods" when {
      "it's a new submission and attempted to view the ' Vouchers, non-cash benefits or reimbursed costs' " +
        "but the income tax and incurred costs section is not finished" in {
        val model = Some(fullIncomeTaxAndCostsModel.copy(paymentsOnEmployeesBehalfQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => reimbursedCostsVouchersAndNonCashRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncurredCostsBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the ' Vouchers, non-cash benefits or reimbursed costs' " +
        "but the carVanFuel section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => reimbursedCostsVouchersAndNonCashRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CarVanFuelBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the ' Vouchers, non-cash benefits or reimbursed costs' " +
        "but the accommodationRelocation section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => reimbursedCostsVouchersAndNonCashRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the ' Vouchers, non-cash benefits or reimbursed costs' " +
        "but the travelEntertainment section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => reimbursedCostsVouchersAndNonCashRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the ' Vouchers, non-cash benefits or reimbursed costs' " +
        "but the utilitiesAndServices section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => reimbursedCostsVouchersAndNonCashRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UtilitiesOrGeneralServicesBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view the ' Vouchers, non-cash benefits or reimbursed costs' " +
        "but the medicalChildcareEducation section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => reimbursedCostsVouchersAndNonCashRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view 'Vouchers, non-cash benefits or reimbursed costs' section" +
        "but the reimbursedCostsVouchersAndNonCash Question is not answered" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(sectionQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => commonReimbursedCostsVouchersAndNonCashModelRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe ReimbursedCostsVouchersAndNonCashBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view 'Vouchers, non-cash benefits or reimbursed costs' section" +
        "but the reimbursedCostsVouchersAndNonCash Question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => commonReimbursedCostsVouchersAndNonCashModelRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AssetsOrAssetTransfersBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a prior submission and attempted to view 'Vouchers, non-cash benefits or reimbursed costs' section" +
        "but the reimbursedCostsVouchersAndNonCash Question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => commonReimbursedCostsVouchersAndNonCashModelRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a prior submission and attempted to view 'expenses amount' page" +
        "but the expenses Question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => expensesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'expenses amount' page" +
        "but the expenses Question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => expensesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TaxableCostsBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'expenses amount' page" +
        "but the expenses Question is empty" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expensesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => expensesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe NonTaxableCostsBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'taxable expenses' page" +
        "but the expenses amount is empty" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(expenses = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => taxableExpensesRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe NonTaxableCostsBenefitsAmountController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'taxable expenses amount' page" +
        "but the taxable expenses question is empty" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => taxableExpensesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TaxableCostsBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'taxable expenses amount' page" +
        "but the taxable expenses question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => taxableExpensesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe VouchersBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a prior submission and attempted to view 'taxable expenses amount' page" +
        "but the taxable expenses question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpensesQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => taxableExpensesAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'vouchers And Credit Cards' page" +
        "but the taxable expenses amount is empty" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(taxableExpenses = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => vouchersAndCreditCardsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TaxableCostsBenefitsAmountController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'vouchers And Credit Cards amount' page" +
        "but the vouchers And Credit Cards question is empty" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => vouchersAndCreditCardsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe VouchersBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'vouchers And Credit Cards amount' page" +
        "but the vouchers And Credit Cards question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => vouchersAndCreditCardsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe NonCashBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a prior submission and attempted to view 'vouchers And Credit Cards amount' page" +
        "but the vouchers And Credit Cards question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCardsQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => vouchersAndCreditCardsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'non cash question' page" +
        "but the vouchers And Credit Cards amount is empty" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(vouchersAndCreditCards = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => nonCashRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe VouchersBenefitsAmountController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'non cash amount' page" +
        "but the non cash question is empty" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => nonCashAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe NonCashBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'non cash amount' page" +
        "but the non cash question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => nonCashAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a prior submission and attempted to view 'non cash amount' page" +
        "but the non cash question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCashQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => nonCashAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and attempted to view 'other items' page" +
        "but the non cash amount is empty" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(nonCash = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => otherItemsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe NonCashBenefitsAmountController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and attempted to view 'other items amount' page" +
        "but the other items question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => otherItemsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'other items amount' page" +
        "but the other items question is false" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => otherItemsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AssetsOrAssetTransfersBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view 'other items amount' page" +
        "but the other items question is empty" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => otherItemsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect using assets benefits methods" when {
      "it's a new submission and attempted to view the 'Assets and assets transfer' page" +
        "but the reimbursed costs section is not finished" in {
        val model = Some(aReimbursedCostsVouchersAndNonCashModel.copy(otherItemsQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(reimbursedCostsVouchersAndNonCashModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe OtherBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets and assets transfer' page " +
        "but the income tax section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(incomeTaxAndCostsModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe IncomeTaxOrIncurredCostsBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets and assets transfer' page " +
        "but the medical section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(medicalChildcareEducationModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets and assets transfer' page " +
        "but the utilities section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(utilitiesAndServicesModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe UtilitiesOrGeneralServicesBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets and assets transfer' page " +
        "but the travel section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(travelEntertainmentModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets and assets transfer' page " +
        "but the accommodation section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(accommodationRelocationModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets and assets transfer' page " +
        "but the car section is not finished" in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = None)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CarVanFuelBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets and assets transfer' section" +
        "but the Assets and assets transfer question is empty" in {
        val model = Some(anAssetsModel.copy(sectionQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => commonAssetsModelRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AssetsOrAssetTransfersBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets and assets transfer' section" +
        "but the Assets and assets transfer question is false" in {
        val model = Some(anAssetsModel.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => commonAssetsModelRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a prior submission and attempted to view the 'Assets and assets transfer' section" +
        "but the Assets and assets transfer question is false" in {
        val model = Some(anAssetsModel.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => commonAssetsModelRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a prior submission and attempted to view the 'Assets amount' page" +
        "but the Assets question is false" in {
        val model = Some(anAssetsModel.copy(assetsQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => assetsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets amount' page" +
        "but the Assets question is false" in {
        val model = Some(anAssetsModel.copy(assetsQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AssetTransfersBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets amount' page" +
        "but the Assets question is empty" in {
        val model = Some(anAssetsModel.copy(assetsQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AssetsBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets transfer question' page" +
        "but the Assets amount is empty" in {
        val model = Some(anAssetsModel.copy(assets = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetTransferRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AssetsBenefitsAmountController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets transfer amount' page" +
        "but the Assets transfer question is empty" in {
        val model = Some(anAssetsModel.copy(assetTransferQuestion = None))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetTransferAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AssetTransfersBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a new submission and attempted to view the 'Assets transfer amount' page" +
        "but the Assets transfer question is false" in {
        val model = Some(anAssetsModel.copy(assetTransferQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = false, employment = employment)),
          EmploymentBenefitsType)(cya => assetTransferAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
      "it's a prior submission and attempted to view the 'Assets transfer amount' page" +
        "but the Assets transfer question is false" in {
        val model = Some(anAssetsModel.copy(assetTransferQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(assetsModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => assetTransferAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect to benefits CYA page" when {
      "it's a prior submission" in {
        val employment = employmentCYA.copy(employmentBenefits = None)
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => commonCarVanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and hitting the common benefits method when benefits received is false " in {
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(isBenefitsReceived = false)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.commonBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and hitting the common car van fuel benefits method when carVanFuel is false " in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => commonCarVanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and hitting the common car van fuel benefits method when carVanFuel is false " in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(sectionQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => commonCarVanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and hitting the car benefits amount method when carQuestion is false " in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.carBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and hitting the car fuel benefits amount method when carFuelQuestion is false " in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(carFuelQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.carFuelBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and hitting the van benefits amount method when vanQuestion is false " in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => vanBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and hitting the van fuel benefits amount method when vanFuelQuestion is false " in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(vanFuelQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => vanFuelBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a new submission and hitting the mileage benefits amount method when mileageQuestion is false " in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(mileageQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.mileageBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a prior submission and hitting the mileage benefits amount method when mileageQuestion is false " in {
        val model = employmentCYA.employmentBenefits.flatMap(_.carVanFuelModel).map(_.copy(mileageQuestion = Some(false)))
        val employment = employmentCYA.copy(employmentBenefits = employmentCYA.employmentBenefits.map(_.copy(carVanFuelModel = model)))
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true, employment = employment)),
          EmploymentBenefitsType)(cya => RedirectService.mileageBenefitsAmountRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }
    }

    "redirect when CYA is empty" when {
      "it's a benefits submission" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, None, EmploymentBenefitsType)(
          cya => commonCarVanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckYourBenefitsController.show(taxYearEOY, employmentId).url
      }

      "it's a employment details submission" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, None, EmploymentDetailsType)(cya =>
          commonCarVanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe SEE_OTHER
        redirectUrl(response) shouldBe CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url
      }
    }

    "continue with the request when benefits are setup and car van fuel is setup" when {
      "it's a new submission" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData), EmploymentBenefitsType)(cya =>
          commonCarVanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe OK
        bodyOf(response) shouldBe "Wow"
      }

      "it's a prior submission" in {
        val response = redirectBasedOnCurrentAnswers(taxYearEOY, employmentId, Some(employmentUserData.copy(hasPriorBenefits = true)),
          EmploymentBenefitsType)(cya => commonCarVanFuelBenefitsRedirects(cya, taxYearEOY, employmentId)) { _ => result }

        status(response) shouldBe OK
        bodyOf(response) shouldBe "Wow"
      }
    }
  }

  "employmentDetailsRedirect" should {
    "redirect to check employment details page" in {
      val response = employmentDetailsRedirect(cyaModel, taxYearEOY, employmentId, isPriorSubmission = true)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url
    }

    "redirect to employer reference page" in {
      val response = employmentDetailsRedirect(cyaModel, taxYearEOY, employmentId, isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe PayeRefController.show(taxYearEOY, employmentId).url
    }

    "redirect to did you leave employer page" in {
      val employment = cyaModel.copy(cyaModel.employmentDetails.copy(employerRef = Some("123/12345"), payrollId = Some("id"), startDate = Some(s"${taxYearEOY-1}-11-01")))
      val response = employmentDetailsRedirect(employment, taxYearEOY, employmentId, isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe DidYouLeaveEmployerController.show(taxYearEOY, employmentId).url
    }

    "redirect to start date page when did You Leave Question is false" in {
      val employment = cyaModel.copy(cyaModel.employmentDetails.copy(employerRef = Some("123/12345"), didYouLeaveQuestion = Some(false)))
      val response = employmentDetailsRedirect(employment, taxYearEOY, employmentId, isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe EmployerStartDateController.show(taxYearEOY, employmentId).url
    }

    "redirect to pay page" in {
      val employmentDetails = cyaModel.employmentDetails.copy(employerRef = Some("123/12345"),
        startDate = Some(s"${taxYearEOY-1}-11-01"), didYouLeaveQuestion = Some(true), cessationDate = Some(s"${taxYearEOY-1}-10-01"), payrollId = Some("id"))
      val response = employmentDetailsRedirect(cyaModel.copy(employmentDetails), taxYearEOY, employmentId, isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe EmployerPayAmountController.show(taxYearEOY, employmentId).url
    }

    "redirect to tax page" in {
      val employmentDetails = cyaModel.employmentDetails.copy(employerRef = Some("123/12345"), startDate = Some(s"${taxYearEOY-1}-11-01"),
        didYouLeaveQuestion = Some(true), cessationDate = Some(s"${taxYearEOY-1}-10-10"), payrollId = Some("id"), taxablePayToDate = Some(1))
      val response = employmentDetailsRedirect(cyaModel.copy(employmentDetails), taxYearEOY, employmentId, isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe EmploymentTaxController.show(taxYearEOY, employmentId).url
    }

    "redirect to payroll id page" in {
      val employmentDetails = cyaModel.employmentDetails.copy(employerRef = Some("123/12345"), startDate = Some(s"${taxYearEOY-1}-11-01"),
        didYouLeaveQuestion = Some(true), cessationDate = Some(s"${taxYearEOY-1}-10-10"), taxablePayToDate = Some(1), totalTaxToDate = Some(1))
      val response = employmentDetailsRedirect(cyaModel.copy(employmentDetails), taxYearEOY, employmentId, isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe EmployerPayrollIdController.show(taxYearEOY, employmentId).url
    }

    "redirect to employment dates page when no cessation date" in {
      val employmentDetails = cyaModel.employmentDetails.copy(employerRef = Some("123/12345"), startDate = Some(s"${taxYearEOY-1}-11-01"),
        didYouLeaveQuestion = Some(true), taxablePayToDate = Some(1), totalTaxToDate = Some(1), payrollId = Some("id"))
      val response = employmentDetailsRedirect(cyaModel.copy(employmentDetails), taxYearEOY, employmentId, isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe EmploymentDatesController.show(taxYearEOY, employmentId).url
    }

    "redirect to check employment details page when no cessation date but the did you leave question is no" in {
      val employmentDetails = cyaModel.employmentDetails.copy(employerRef = Some("123/12345"), startDate = Some(s"${taxYearEOY-1}-11-01"),
        taxablePayToDate = Some(1), totalTaxToDate = Some(1), payrollId = Some("id"), didYouLeaveQuestion = Some(false))
      val response = employmentDetailsRedirect(cyaModel.copy(employmentDetails), taxYearEOY, employmentId, isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url
    }

    "redirect to check employment details page when all filled in" in {
      val employmentDetails = cyaModel.employmentDetails.copy(employerRef = Some("123/12345"), startDate = Some(s"${taxYearEOY-1}-11-01"), taxablePayToDate = Some(1),
        totalTaxToDate = Some(1), payrollId = Some("id"), didYouLeaveQuestion = Some(true), cessationDate = Some(s"${taxYearEOY-1}-11-01"))
      val response = employmentDetailsRedirect(cyaModel.copy(employmentDetails), taxYearEOY, employmentId, isPriorSubmission = false)

      response.header.status shouldBe SEE_OTHER
      redirectUrl(Future(response)) shouldBe CheckEmploymentDetailsController.show(taxYearEOY, employmentId).url
    }
  }
}
