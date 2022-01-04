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
import builders.models.benefits.UtilitiesAndServicesModelBuilder.aUtilitiesAndServicesModel
import builders.models.mongo.EmploymentUserDataBuilder.{anEmploymentUserData, anEmploymentUserDataWithBenefits}
import config.MockEmploymentSessionService
import models.benefits.UtilitiesAndServicesModel
import utils.UnitTest

class UtilitiesServiceSpec extends UnitTest with MockEmploymentSessionService {

  private val taxYear = 2021
  private val employmentId = "some-employment-id"

  private val underTest = new UtilitiesService(mockEmploymentSessionService, mockExecutionContext)

  "updateSectionQuestion" should {
    "update utilities model and set section question to true when true value passed" in {
      val benefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(sectionQuestion = Some(false))))
      val employmentUserData = anEmploymentUserDataWithBenefits(benefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(sectionQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(taxYear, employmentId, employmentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "clear utilities model when sectionQuestion is set to false" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel)
      val expectedBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(UtilitiesAndServicesModel.clear))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "add utilities model and set sectionQuestion given model not initially present" in {
      val employmentUserData = anEmploymentUserDataWithBenefits(aBenefitsViewModel.copy(utilitiesAndServicesModel = None))
      val expectedBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(UtilitiesAndServicesModel(sectionQuestion = Some(false))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateSectionQuestion(taxYear, employmentId, employmentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateTelephoneQuestion" should {
    "set telephoneQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(telephoneQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(telephoneQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTelephoneQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set telephoneQuestion to false and telephone value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(telephoneQuestion = Some(false), telephone = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTelephoneQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateTelephone" should {
    "set telephone amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(telephone = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateTelephone(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateEmployerProvidedServicesQuestion" should {
    "set employerProvidedServicesQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedServicesQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedServicesQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEmployerProvidedServicesQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set employerProvidedServicesQuestion to false and employerProvidedServices value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedServicesQuestion = Some(false), employerProvidedServices = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEmployerProvidedServicesQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateEmployerProvidedServices" should {
    "set employerProvidedServices amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedServices = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEmployerProvidedServices(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateEmployerProvidedProfessionalSubscriptionsQuestion" should {
    "set employerProvidedProfessionalSubscriptionsQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEmployerProvidedProfessionalSubscriptionsQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set employerProvidedProfessionalSubscriptionsQuestion to false and employerProvidedProfessionalSubscriptions value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false), employerProvidedProfessionalSubscriptions = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEmployerProvidedProfessionalSubscriptionsQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateEmployerProvidedProfessionalSubscriptions" should {
    "set employerProvidedProfessionalSubscriptions amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(employerProvidedProfessionalSubscriptions = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateEmployerProvidedProfessionalSubscriptions(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }

  "updateServiceQuestion" should {
    "set serviceQuestion to true when true" in {
      val givenBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(serviceQuestion = Some(false))))
      val givenEmploymentUserData = anEmploymentUserDataWithBenefits(givenBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefits = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(serviceQuestion = Some(true))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefits).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateServiceQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = true)) shouldBe
        Right(expectedEmploymentUserData)
    }

    "set serviceQuestion to false and service value is cleared when false" in {
      val givenEmploymentUserData = anEmploymentUserData
      val expectedBenefitsViewModel = aBenefitsViewModel
        .copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(serviceQuestion = Some(false), service = None)))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = true, hasPriorBenefits = true, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateServiceQuestion(taxYear, employmentId, givenEmploymentUserData, questionValue = false)) shouldBe
        Right(expectedEmploymentUserData)
    }
  }

  "updateService" should {
    "set service amount" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val expectedBenefitsViewModel = aBenefitsViewModel.copy(utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel.copy(service = Some(123))))
      val expectedEmploymentUserData = anEmploymentUserDataWithBenefits(expectedBenefitsViewModel).copy(isPriorSubmission = false, hasPriorBenefits = false)

      mockCreateOrUpdateUserDataWith(taxYear, employmentId, isPriorSubmission = false, hasPriorBenefits = false, expectedEmploymentUserData.employment, Right(expectedEmploymentUserData))

      await(underTest.updateService(taxYear, employmentId, givenEmploymentUserData, amount = 123)) shouldBe Right(expectedEmploymentUserData)
    }
  }
}
