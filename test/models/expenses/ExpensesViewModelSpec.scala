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

import org.scalamock.scalatest.MockFactory
import support.TaxYearUtils.taxYearEOY
import support.UnitTest
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

class ExpensesViewModelSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

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

      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.claimingEmploymentExpenses.toString, associatedText).returning(encryptedClaimingEmploymentExpenses)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.jobExpensesQuestion.get.toString, associatedText).returning(encryptedJobExpensesQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.jobExpenses.get.toString(), associatedText).returning(encryptedJobExpenses)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.flatRateJobExpensesQuestion.get.toString, associatedText).returning(encryptedFlatRateJobExpensesQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.flatRateJobExpenses.get.toString(), associatedText).returning(encryptedFlatRateJobExpenses)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.professionalSubscriptionsQuestion.get.toString, associatedText).returning(encryptedProfessionalSubscriptionsQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.professionalSubscriptions.get.toString(), associatedText).returning(encryptedProfessionalSubscriptions)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.otherAndCapitalAllowancesQuestion.get.toString, associatedText).returning(encryptedOtherAndCapitalAllowancesQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.otherAndCapitalAllowances.get.toString(), associatedText).returning(encryptedOtherAndCapitalAllowances)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.businessTravelCosts.get.toString(), associatedText).returning(encryptedBusinessTravelCosts)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.hotelAndMealExpenses.get.toString(), associatedText).returning(encryptedHotelAndMealExpenses)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.vehicleExpenses.get.toString(), associatedText).returning(encryptedVehicleExpenses)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.mileageAllowanceRelief.get.toString(), associatedText).returning(encryptedMileageAllowanceRelief)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.submittedOn.get, associatedText).returning(encryptedSubmittedOn)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(underTest.isUsingCustomerData.toString, associatedText).returning(encryptedIsUsingCustomerData)

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

      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedClaimingEmploymentExpenses, associatedText).returning(value = anExpensesViewModel.claimingEmploymentExpenses.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedJobExpensesQuestion, associatedText).returning(value = anExpensesViewModel.jobExpensesQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedJobExpenses, associatedText).returning(value = anExpensesViewModel.jobExpenses.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedFlatRateJobExpensesQuestion, associatedText).returning(value = anExpensesViewModel.flatRateJobExpensesQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedFlatRateJobExpenses, associatedText).returning(value = anExpensesViewModel.flatRateJobExpenses.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedProfessionalSubscriptionsQuestion, associatedText)
        .returning(value = anExpensesViewModel.professionalSubscriptionsQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedProfessionalSubscriptions, associatedText).returning(value = anExpensesViewModel.professionalSubscriptions.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedOtherAndCapitalAllowancesQuestion, associatedText)
        .returning(value = anExpensesViewModel.otherAndCapitalAllowancesQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedOtherAndCapitalAllowances, associatedText).returning(value = anExpensesViewModel.otherAndCapitalAllowances.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedBusinessTravelCosts, associatedText).returning(value = anExpensesViewModel.businessTravelCosts.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedHotelAndMealExpenses, associatedText).returning(value = anExpensesViewModel.hotelAndMealExpenses.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedVehicleExpenses, associatedText).returning(value = anExpensesViewModel.vehicleExpenses.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedMileageAllowanceRelief, associatedText).returning(value = anExpensesViewModel.mileageAllowanceRelief.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedSubmittedOn, associatedText).returning(value = s"${taxYearEOY - 1}-01-04T05:01:01Z")
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedIsUsingCustomerData, associatedText).returning(value = anExpensesViewModel.isUsingCustomerData.toString)

      underTest.decrypted shouldBe anExpensesViewModel.copy(submittedOn = Some(s"${taxYearEOY - 1}-01-04T05:01:01Z"))
    }
  }
}
