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

package models.expenses

import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher, TaxYearHelper}

class ExpensesViewModelSpec extends UnitTest
  with MockFactory
  with TaxYearHelper {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedClaimingEmploymentExpenses = EncryptedValue("encryptedClaimingEmploymentExpenses", "some-nonce")
  private val encryptedJobExpensesQuestion = EncryptedValue("encryptedJobExpensesQuestion", "some-nonce")
  private val encryptedJobExpenses = EncryptedValue("encryptedJobExpenses", "some-nonce")
  private val encryptedFlatRateJobExpensesQuestion = EncryptedValue("encryptedFlatRateJobExpensesQuestion", "some-nonce")
  private val encryptedFlatRateJobExpenses = EncryptedValue("encryptedFlatRateJobExpenses", "some-nonce")
  private val encryptedProfessionalSubscriptionsQuestion = EncryptedValue("encryptedProfessionalSubscriptionsQuestion", "some-nonce")
  private val encryptedProfessionalSubscriptions = EncryptedValue("encryptedProfessionalSubscriptions", "some-nonce")
  private val encryptedOtherAndCapitalAllowancesQuestion = EncryptedValue("encryptedOtherAndCapitalAllowancesQuestion", "some-nonce")
  private val encryptedOtherAndCapitalAllowances = EncryptedValue("encryptedOtherAndCapitalAllowances", "some-nonce")
  private val encryptedBusinessTravelCosts = EncryptedValue("encryptedBusinessTravelCosts", "some-nonce")
  private val encryptedHotelAndMealExpenses = EncryptedValue("encryptedHotelAndMealExpenses", "some-nonce")
  private val encryptedVehicleExpenses = EncryptedValue("encryptedVehicleExpenses", "some-nonce")
  private val encryptedMileageAllowanceRelief = EncryptedValue("encryptedMileageAllowanceRelief", "some-nonce")
  private val encryptedSubmittedOn = EncryptedValue("encryptedSubmittedOn", "some-nonce")
  private val encryptedIsUsingCustomerData = EncryptedValue("encryptedIsUsingCustomerData", "some-nonce")

  "ExpensesViewModel.encrypted" should {
    "return EncryptedExpensesViewModel instance" in {
      val underTest = anExpensesViewModel.copy(submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"))

      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.claimingEmploymentExpenses, textAndKey).returning(encryptedClaimingEmploymentExpenses)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.jobExpensesQuestion.get, textAndKey).returning(encryptedJobExpensesQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.jobExpenses.get, textAndKey).returning(encryptedJobExpenses)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.flatRateJobExpensesQuestion.get, textAndKey).returning(encryptedFlatRateJobExpensesQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.flatRateJobExpenses.get, textAndKey).returning(encryptedFlatRateJobExpenses)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.professionalSubscriptionsQuestion.get, textAndKey).returning(encryptedProfessionalSubscriptionsQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.professionalSubscriptions.get, textAndKey).returning(encryptedProfessionalSubscriptions)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.otherAndCapitalAllowancesQuestion.get, textAndKey).returning(encryptedOtherAndCapitalAllowancesQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.otherAndCapitalAllowances.get, textAndKey).returning(encryptedOtherAndCapitalAllowances)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.businessTravelCosts.get, textAndKey).returning(encryptedBusinessTravelCosts)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.hotelAndMealExpenses.get, textAndKey).returning(encryptedHotelAndMealExpenses)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.vehicleExpenses.get, textAndKey).returning(encryptedVehicleExpenses)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.mileageAllowanceRelief.get, textAndKey).returning(encryptedMileageAllowanceRelief)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.submittedOn.get, textAndKey).returning(encryptedSubmittedOn)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.isUsingCustomerData, textAndKey).returning(encryptedIsUsingCustomerData)

      underTest.encrypted shouldBe EncryptedExpensesViewModel(
        claimingEmploymentExpenses = encryptedClaimingEmploymentExpenses,
        jobExpensesQuestion = Some(encryptedJobExpensesQuestion),
        jobExpenses = Some(encryptedJobExpenses),
        flatRateJobExpensesQuestion = Some(encryptedFlatRateJobExpensesQuestion),
        flatRateJobExpenses = Some(encryptedFlatRateJobExpenses),
        professionalSubscriptionsQuestion = Some(encryptedProfessionalSubscriptionsQuestion),
        professionalSubscriptions = Some(encryptedProfessionalSubscriptions),
        otherAndCapitalAllowancesQuestion = Some(encryptedOtherAndCapitalAllowancesQuestion),
        otherAndCapitalAllowances = Some(encryptedOtherAndCapitalAllowances),
        businessTravelCosts = Some(encryptedBusinessTravelCosts),
        hotelAndMealExpenses = Some(encryptedHotelAndMealExpenses),
        vehicleExpenses = Some(encryptedVehicleExpenses),
        mileageAllowanceRelief = Some(encryptedMileageAllowanceRelief),
        submittedOn = Some(encryptedSubmittedOn),
        isUsingCustomerData = encryptedIsUsingCustomerData
      )
    }
  }

  "EncryptedExpensesViewModel.decrypted" should {
    "return ExpensesViewModel instance" in {
      val underTest = EncryptedExpensesViewModel(
        claimingEmploymentExpenses = encryptedClaimingEmploymentExpenses,
        jobExpensesQuestion = Some(encryptedJobExpensesQuestion),
        jobExpenses = Some(encryptedJobExpenses),
        flatRateJobExpensesQuestion = Some(encryptedFlatRateJobExpensesQuestion),
        flatRateJobExpenses = Some(encryptedFlatRateJobExpenses),
        professionalSubscriptionsQuestion = Some(encryptedProfessionalSubscriptionsQuestion),
        professionalSubscriptions = Some(encryptedProfessionalSubscriptions),
        otherAndCapitalAllowancesQuestion = Some(encryptedOtherAndCapitalAllowancesQuestion),
        otherAndCapitalAllowances = Some(encryptedOtherAndCapitalAllowances),
        businessTravelCosts = Some(encryptedBusinessTravelCosts),
        hotelAndMealExpenses = Some(encryptedHotelAndMealExpenses),
        vehicleExpenses = Some(encryptedVehicleExpenses),
        mileageAllowanceRelief = Some(encryptedMileageAllowanceRelief),
        submittedOn = Some(encryptedSubmittedOn),
        isUsingCustomerData = encryptedIsUsingCustomerData
      )

      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedClaimingEmploymentExpenses.value, encryptedClaimingEmploymentExpenses.nonce, textAndKey, *).returning(value = anExpensesViewModel.claimingEmploymentExpenses)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedJobExpensesQuestion.value, encryptedJobExpensesQuestion.nonce, textAndKey, *).returning(value = anExpensesViewModel.jobExpensesQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedJobExpenses.value, encryptedJobExpenses.nonce, textAndKey, *).returning(value = anExpensesViewModel.jobExpenses.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedFlatRateJobExpensesQuestion.value, encryptedFlatRateJobExpensesQuestion.nonce, textAndKey, *).returning(value = anExpensesViewModel.flatRateJobExpensesQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedFlatRateJobExpenses.value, encryptedFlatRateJobExpenses.nonce, textAndKey, *).returning(value = anExpensesViewModel.flatRateJobExpenses.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedProfessionalSubscriptionsQuestion.value, encryptedProfessionalSubscriptionsQuestion.nonce, textAndKey, *)
        .returning(value = anExpensesViewModel.professionalSubscriptionsQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedProfessionalSubscriptions.value, encryptedProfessionalSubscriptions.nonce, textAndKey, *).returning(value = anExpensesViewModel.professionalSubscriptions.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedOtherAndCapitalAllowancesQuestion.value, encryptedOtherAndCapitalAllowancesQuestion.nonce, textAndKey, *)
        .returning(value = anExpensesViewModel.otherAndCapitalAllowancesQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedOtherAndCapitalAllowances.value, encryptedOtherAndCapitalAllowances.nonce, textAndKey, *).returning(value = anExpensesViewModel.otherAndCapitalAllowances.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedBusinessTravelCosts.value, encryptedBusinessTravelCosts.nonce, textAndKey, *).returning(value = anExpensesViewModel.businessTravelCosts.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedHotelAndMealExpenses.value, encryptedHotelAndMealExpenses.nonce, textAndKey, *).returning(value = anExpensesViewModel.hotelAndMealExpenses.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedVehicleExpenses.value, encryptedVehicleExpenses.nonce, textAndKey, *).returning(value = anExpensesViewModel.vehicleExpenses.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedMileageAllowanceRelief.value, encryptedMileageAllowanceRelief.nonce, textAndKey, *).returning(value = anExpensesViewModel.mileageAllowanceRelief.get)
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedSubmittedOn.value, encryptedSubmittedOn.nonce, textAndKey, *).returning(value = s"${taxYearEOY - 1}-01-04T05:01:01Z")
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedIsUsingCustomerData.value, encryptedIsUsingCustomerData.nonce, textAndKey, *).returning(value = anExpensesViewModel.isUsingCustomerData)

      underTest.decrypted shouldBe anExpensesViewModel.copy(submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"))
    }
  }
}
