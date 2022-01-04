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

package services.benefits

import builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import config.MockEmploymentSessionService
import models.benefits.CarVanFuelModel
import utils.UnitTest

class FuelServiceSpec extends UnitTest with MockEmploymentSessionService {

  private val taxYear = 2021
  private val employmentId = "some-employment-id"

  private val underTest = new FuelService(mockEmploymentSessionService, mockExecutionContext)

  "updateSectionQuestion" should {
    "update fuel model and set section question to true when true value passed" in {
      val benefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(sectionQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(sectionQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(taxYear, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "clear fuel model when sectionQuestion is set to false" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(CarVanFuelModel.clear))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "add fuel model and set sectionQuestion given model not initially present" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel.copy(carVanFuelModel = None))
      val expectedBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(CarVanFuelModel(sectionQuestion = Some(false))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateCarQuestion" should {
    "set carQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits, isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCarQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set carQuestion to false and car, carFuelQuestion, carFuel values are cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carQuestion = Some(false), car = None, carFuelQuestion = None, carFuel = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCarQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateCar" should {
    "set car amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(car = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCar(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateCarFuelQuestion" should {
    "set carFuelQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carFuelQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carFuelQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCarFuelQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set carFuelQuestion to false and carFuel value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carFuelQuestion = Some(false), carFuel = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCarFuelQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateCarFuel" should {
    "set carFuel amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(carFuel = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateCarFuel(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateVanQuestion" should {
    "set vanQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateVanQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set vanQuestion to false and van, vanFuelQuestion, vanFuel values are cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanQuestion = Some(false), van = None, vanFuelQuestion = None, vanFuel = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateVanQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateVan" should {
    "set van amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(van = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateVan(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateVanFuelQuestion" should {
    "set vanFuelQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanFuelQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanFuelQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateVanFuelQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set vanFuelQuestion to false and vanFuel value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanFuelQuestion = Some(false), vanFuel = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateVanFuelQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateVanFuel" should {
    "set vanFuel amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(vanFuel = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateVanFuel(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateMileageQuestion" should {
    "set mileageQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileageQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileageQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateMileageQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set mileageQuestion to false and mileage value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileageQuestion = Some(false), mileage = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateMileageQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateMileage" should {
    "set mileage amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(carVanFuelModel = Some(aCarVanFuelModel.copy(mileage = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateMileage(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }
}
