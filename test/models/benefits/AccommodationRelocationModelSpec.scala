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

package models.benefits

import controllers.benefits.accommodation.routes._
import org.scalamock.scalatest.MockFactory
import support.builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

class AccommodationRelocationModelSpec extends UnitTest
  with TaxYearProvider
  with MockFactory {

  private val employmentId = "employmentId"

  implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  implicit val associatedText: String = "some-associated-text"

  private val encryptedSectionQuestion = EncryptedValue("encryptedSectionQuestion", "some-nonce")
  private val encryptedAccommodationQuestion = EncryptedValue("encryptedAccommodationQuestion", "some-nonce")
  private val encryptedAccommodation = EncryptedValue("encryptedAccommodation", "some-nonce")
  private val encryptedQualifyingRelocationExpensesQuestion = EncryptedValue("encryptedQualifyingRelocationExpensesQuestion", "some-nonce")
  private val encryptedQualifyingRelocationExpenses = EncryptedValue("encryptedQualifyingRelocationExpenses", "some-nonce")
  private val encryptedNonQualifyingRelocationExpensesQuestion = EncryptedValue("encryptedNonQualifyingRelocationExpensesQuestion", "some-nonce")
  private val encryptedNonQualifyingRelocationExpenses = EncryptedValue("encryptedNonQualifyingRelocationExpenses", "some-nonce")

  "AccommodationRelocationModel.qualifyingRelocationSectionFinished" should {
    "return None when qualifyingRelocationExpensesQuestion is true and qualifyingRelocationExpenses is defined" in {
      val underTest = AccommodationRelocationModel(qualifyingRelocationExpensesQuestion = Some(true), qualifyingRelocationExpenses = Some(1))

      underTest.qualifyingRelocationSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to QualifyingRelocationBenefitsAmountController when qualifyingRelocationExpensesQuestion and qualifyingRelocationExpenses not defined" in {
      val underTest = AccommodationRelocationModel(qualifyingRelocationExpensesQuestion = Some(true), qualifyingRelocationExpenses = None)

      underTest.qualifyingRelocationSectionFinished(taxYearEOY, employmentId) shouldBe
        Some(QualifyingRelocationBenefitsAmountController.show(taxYearEOY, employmentId))
    }

    "return None when qualifyingRelocationExpensesQuestion is false" in {
      val underTest = AccommodationRelocationModel(qualifyingRelocationExpensesQuestion = Some(false))

      underTest.qualifyingRelocationSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to QualifyingRelocationBenefitsController when qualifyingRelocationExpensesQuestion is None" in {
      val underTest = AccommodationRelocationModel(qualifyingRelocationExpensesQuestion = None)

      underTest.qualifyingRelocationSectionFinished(taxYearEOY, employmentId) shouldBe Some(QualifyingRelocationBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "AccommodationRelocationModel.accommodationSectionFinished" should {
    "return None when accommodationQuestion is true and accommodation is defined" in {
      val underTest = AccommodationRelocationModel(accommodationQuestion = Some(true), accommodation = Some(1))

      underTest.accommodationSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to LivingAccommodationBenefitAmountController when accommodationQuestion and accommodation not defined" in {
      val underTest = AccommodationRelocationModel(accommodationQuestion = Some(true), accommodation = None)

      underTest.accommodationSectionFinished(taxYearEOY, employmentId) shouldBe Some(LivingAccommodationBenefitAmountController.show(taxYearEOY, employmentId))
    }

    "return None when accommodationQuestion is false" in {
      val underTest = AccommodationRelocationModel(accommodationQuestion = Some(false))

      underTest.accommodationSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to AccommodationRelocationBenefitsController when accommodationQuestion is None" in {
      val underTest = AccommodationRelocationModel(accommodationQuestion = None)

      underTest.accommodationSectionFinished(taxYearEOY, employmentId) shouldBe Some(LivingAccommodationBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "AccommodationRelocationModel.nonQualifyingRelocationSectionFinished" should {
    "return None when nonQualifyingRelocationExpensesQuestion is true and nonQualifyingRelocationExpenses is defined" in {
      val underTest = AccommodationRelocationModel(nonQualifyingRelocationExpensesQuestion = Some(true), nonQualifyingRelocationExpenses = Some(1))

      underTest.nonQualifyingRelocationSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to AccommodationRelocationBenefitsController when nonQualifyingRelocationExpensesQuestion and nonQualifyingRelocationExpenses not defined" in {
      val underTest = AccommodationRelocationModel(nonQualifyingRelocationExpensesQuestion = Some(true), nonQualifyingRelocationExpenses = None)

      underTest.nonQualifyingRelocationSectionFinished(taxYearEOY, employmentId) shouldBe
        Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId))
    }

    "return None when nonQualifyingRelocationExpensesQuestion is false" in {
      val underTest = AccommodationRelocationModel(nonQualifyingRelocationExpensesQuestion = Some(false))

      underTest.nonQualifyingRelocationSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to NonQualifyingRelocationBenefitsController when nonQualifyingRelocationExpensesQuestion is None" in {
      val underTest = AccommodationRelocationModel(nonQualifyingRelocationExpensesQuestion = None)

      underTest.nonQualifyingRelocationSectionFinished(taxYearEOY, employmentId) shouldBe
        Some(NonQualifyingRelocationBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "AccommodationRelocationModel.isFinished" should {
    "return result of accommodationSectionFinished when accommodationRelocationQuestion is true and accommodationSectionFinished is not None" in {
      val underTest = AccommodationRelocationModel(sectionQuestion = Some(true),
        accommodationQuestion = None,
        qualifyingRelocationExpensesQuestion = Some(false),
        nonQualifyingRelocationExpensesQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.accommodationSectionFinished(taxYearEOY, employmentId)
    }

    "return result of qualifyingRelocationSectionFinished when accommodationRelocationQuestion is true and " +
      "qualifyingRelocationExpensesQuestion is not None" in {
      val underTest = AccommodationRelocationModel(sectionQuestion = Some(true),
        accommodationQuestion = Some(false),
        qualifyingRelocationExpensesQuestion = None,
        nonQualifyingRelocationExpensesQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.qualifyingRelocationSectionFinished(taxYearEOY, employmentId)
    }

    "return result of nonQualifyingRelocationSectionFinished when accommodationRelocationQuestion is true and " +
      "nonQualifyingRelocationExpensesQuestion is not None" in {
      val underTest = AccommodationRelocationModel(sectionQuestion = Some(true),
        accommodationQuestion = Some(false),
        qualifyingRelocationExpensesQuestion = Some(false),
        nonQualifyingRelocationExpensesQuestion = None)

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.nonQualifyingRelocationSectionFinished(taxYearEOY, employmentId)
    }

    "return None when accommodationRelocationQuestion is true and accommodationSectionFinished, " +
      "qualifyingRelocationSectionFinished, nonQualifyingRelocationSectionFinished are None" in {
      val underTest = AccommodationRelocationModel(sectionQuestion = Some(true),
        accommodationQuestion = Some(false),
        qualifyingRelocationExpensesQuestion = Some(false),
        nonQualifyingRelocationExpensesQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return None when accommodationRelocationQuestion is false" in {
      val underTest = AccommodationRelocationModel(sectionQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to AccommodationRelocationBenefitsController when accommodationRelocationQuestion is None" in {
      val underTest = AccommodationRelocationModel(sectionQuestion = None)

      underTest.isFinished(taxYearEOY, employmentId) shouldBe Some(AccommodationRelocationBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "AccommodationRelocationModel.encrypted" should {
    "return AccommodationRelocationModel instance" in {
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(anAccommodationRelocationModel.sectionQuestion.get.toString, associatedText).returning(encryptedSectionQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(anAccommodationRelocationModel.accommodationQuestion.get.toString, associatedText).returning(encryptedAccommodationQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(anAccommodationRelocationModel.accommodation.get.toString(), associatedText).returning(encryptedAccommodation)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(anAccommodationRelocationModel.qualifyingRelocationExpensesQuestion.get.toString, associatedText)
        .returning(encryptedQualifyingRelocationExpensesQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String))
        .expects(anAccommodationRelocationModel.qualifyingRelocationExpenses.get.toString(), associatedText).returning(encryptedQualifyingRelocationExpenses)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(anAccommodationRelocationModel.nonQualifyingRelocationExpensesQuestion.get.toString, associatedText)
        .returning(encryptedNonQualifyingRelocationExpensesQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(anAccommodationRelocationModel.nonQualifyingRelocationExpenses.get.toString(), associatedText)
        .returning(encryptedNonQualifyingRelocationExpenses)

      anAccommodationRelocationModel.encrypted shouldBe EncryptedAccommodationRelocationModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        accommodationQuestion = Some(encryptedAccommodationQuestion),
        accommodation = Some(encryptedAccommodation),
        qualifyingRelocationExpensesQuestion = Some(encryptedQualifyingRelocationExpensesQuestion),
        qualifyingRelocationExpenses = Some(encryptedQualifyingRelocationExpenses),
        nonQualifyingRelocationExpensesQuestion = Some(encryptedNonQualifyingRelocationExpensesQuestion),
        nonQualifyingRelocationExpenses = Some(encryptedNonQualifyingRelocationExpenses)
      )
    }
  }

  "EncryptedAccommodationRelocationModel.decrypted" should {
    "return AccommodationRelocationModel instance" in {
      val underTest = EncryptedAccommodationRelocationModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        accommodationQuestion = Some(encryptedAccommodationQuestion),
        accommodation = Some(encryptedAccommodation),
        qualifyingRelocationExpensesQuestion = Some(encryptedQualifyingRelocationExpensesQuestion),
        qualifyingRelocationExpenses = Some(encryptedQualifyingRelocationExpenses),
        nonQualifyingRelocationExpensesQuestion = Some(encryptedNonQualifyingRelocationExpensesQuestion),
        nonQualifyingRelocationExpenses = Some(encryptedNonQualifyingRelocationExpenses)
      )

            (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
              .expects(encryptedSectionQuestion, associatedText).returning(anAccommodationRelocationModel.sectionQuestion.get.toString)
            (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
              .expects(encryptedAccommodationQuestion, associatedText).returning(value = anAccommodationRelocationModel.accommodationQuestion.get.toString)
            (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
              .expects(encryptedAccommodation, associatedText).returning(value = anAccommodationRelocationModel.accommodation.get.toString())
            (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
              .expects(encryptedQualifyingRelocationExpensesQuestion, associatedText)
              .returning(value = anAccommodationRelocationModel.qualifyingRelocationExpensesQuestion.get.toString)
            (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
              .expects(encryptedQualifyingRelocationExpenses, associatedText)
              .returning(value = anAccommodationRelocationModel.qualifyingRelocationExpenses.get.toString())
            (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
              .expects(encryptedNonQualifyingRelocationExpensesQuestion, associatedText)
              .returning(value = anAccommodationRelocationModel.nonQualifyingRelocationExpensesQuestion.get.toString)
            (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
              .expects(encryptedNonQualifyingRelocationExpenses, associatedText)
              .returning(value = anAccommodationRelocationModel.nonQualifyingRelocationExpenses.get.toString())


      underTest.decrypted shouldBe anAccommodationRelocationModel
    }
  }
}
