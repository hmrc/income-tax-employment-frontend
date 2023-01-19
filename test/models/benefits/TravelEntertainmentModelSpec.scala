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

import controllers.benefits.travel.routes._
import controllers.employment.routes.CheckYourBenefitsController
import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import support.builders.models.benefits.TravelEntertainmentModelBuilder.aTravelEntertainmentModel
import support.{TaxYearProvider, UnitTest}
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher}

class TravelEntertainmentModelSpec extends UnitTest
  with TaxYearProvider
  with MockFactory {

  private val employmentId = "some-employment-id"

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedSectionQuestion = EncryptedValue("encryptedSectionQuestion", "some-nonce")
  private val encryptedTravelAndSubsistenceQuestion = EncryptedValue("encryptedTravelAndSubsistenceQuestion", "some-nonce")
  private val encryptedTravelAndSubsistence = EncryptedValue("encryptedTravelAndSubsistence", "some-nonce")
  private val encryptedPersonalIncidentalExpensesQuestion = EncryptedValue("encryptedPersonalIncidentalExpensesQuestion", "some-nonce")
  private val encryptedPersonalIncidentalExpenses = EncryptedValue("encryptedPersonalIncidentalExpenses", "some-nonce")
  private val encryptedEntertainingQuestion = EncryptedValue("encryptedEntertainingQuestion", "some-nonce")
  private val encryptedEntertaining = EncryptedValue("encryptedEntertaining", "some-nonce")

  "TravelEntertainmentModel.travelSectionFinished" should {
    "return None when travelAndSubsistenceQuestion is true and travelAndSubsistence is defined" in {
      val underTest = TravelEntertainmentModel(travelAndSubsistenceQuestion = Some(true), travelAndSubsistence = Some(1))

      underTest.travelSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to TravelOrSubsistenceBenefitsAmountController when travelAndSubsistenceQuestion is true" in {
      val underTest = TravelEntertainmentModel(travelAndSubsistenceQuestion = Some(true))

      underTest.travelSectionFinished(taxYearEOY, employmentId) shouldBe Some(TravelOrSubsistenceBenefitsAmountController.show(taxYearEOY, employmentId))
    }

    "return None when travelAndSubsistenceQuestion is false" in {
      val underTest = TravelEntertainmentModel(travelAndSubsistenceQuestion = Some(false))

      underTest.travelSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to TravelAndSubsistenceBenefitsController when travelAndSubsistenceQuestion is None" in {
      val underTest = TravelEntertainmentModel(travelAndSubsistenceQuestion = None)

      underTest.travelSectionFinished(taxYearEOY, employmentId) shouldBe Some(TravelAndSubsistenceBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "TravelEntertainmentModel.personalIncidentalSectionFinished" should {
    "return None when personalIncidentalExpensesQuestion is true and personalIncidentalExpenses is defined" in {
      val underTest = TravelEntertainmentModel(personalIncidentalExpensesQuestion = Some(true), personalIncidentalExpenses = Some(1))

      underTest.personalIncidentalSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to IncidentalCostsBenefitsAmountController when personalIncidentalExpensesQuestion is true" in {
      val underTest = TravelEntertainmentModel(personalIncidentalExpensesQuestion = Some(true))

      underTest.personalIncidentalSectionFinished(taxYearEOY, employmentId) shouldBe Some(IncidentalCostsBenefitsAmountController.show(taxYearEOY, employmentId))
    }

    "return None when personalIncidentalExpensesQuestion is false" in {
      val underTest = TravelEntertainmentModel(personalIncidentalExpensesQuestion = Some(false))

      underTest.personalIncidentalSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to CheckYourBenefitsController when personalIncidentalExpensesQuestion is None" in {
      val underTest = TravelEntertainmentModel(personalIncidentalExpensesQuestion = None)

      underTest.personalIncidentalSectionFinished(taxYearEOY, employmentId) shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "TravelEntertainmentModel.entertainingSectionFinished" should {
    "return None when entertainingQuestion is true and entertaining is defined" in {
      val underTest = TravelEntertainmentModel(entertainingQuestion = Some(true), entertaining = Some(1))

      underTest.entertainingSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to CheckYourBenefitsController when entertainingQuestion is true" in {
      val underTest = TravelEntertainmentModel(entertainingQuestion = Some(true))

      underTest.entertainingSectionFinished(taxYearEOY, employmentId) shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId))
    }

    "return None when entertainingQuestion is false" in {
      val underTest = TravelEntertainmentModel(entertainingQuestion = Some(false))

      underTest.entertainingSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to EntertainingBenefitsController when entertainingQuestion is None" in {
      val underTest = TravelEntertainmentModel(entertainingQuestion = None)

      underTest.entertainingSectionFinished(taxYearEOY, employmentId) shouldBe Some(EntertainingBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "TravelEntertainmentModel.isFinished" should {
    "return result of travelSectionFinished when travelEntertainmentQuestion is true and travelAndSubsistenceQuestion is not None" in {
      val underTest = TravelEntertainmentModel(sectionQuestion = Some(true), travelAndSubsistenceQuestion = None)

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.travelSectionFinished(taxYearEOY, employmentId)
    }

    "return result of personalIncidentalSectionFinished when travelEntertainmentQuestion is true and travelAndSubsistenceQuestion is false" in {
      val underTest = TravelEntertainmentModel(sectionQuestion = Some(true), travelAndSubsistenceQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.personalIncidentalSectionFinished(taxYearEOY, employmentId)
    }

    "return result of entertainingSectionFinished when travelEntertainmentQuestion is true and " +
      "travelAndSubsistenceQuestion is false and personalIncidentalExpensesQuestion is false" in {
      val underTest = TravelEntertainmentModel(sectionQuestion = Some(true),
        travelAndSubsistenceQuestion = Some(false),
        personalIncidentalExpensesQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.entertainingSectionFinished(taxYearEOY, employmentId)
    }

    "return None when travelEntertainmentQuestion is true and travelAndSubsistenceQuestion, " +
      "personalIncidentalExpensesQuestion, entertainingQuestion are false" in {
      val underTest = TravelEntertainmentModel(sectionQuestion = Some(true),
        travelAndSubsistenceQuestion = Some(false),
        personalIncidentalExpensesQuestion = Some(false),
        entertainingQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return None when travelEntertainmentQuestion is false" in {
      val underTest = TravelEntertainmentModel(sectionQuestion = Some(false))

      underTest.isFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to TravelOrEntertainmentBenefitsController when travelEntertainmentQuestion is None" in {
      val underTest = TravelEntertainmentModel(sectionQuestion = None)

      underTest.isFinished(taxYearEOY, employmentId) shouldBe Some(TravelOrEntertainmentBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "TravelEntertainmentModel.encrypted" should {
    "return EncryptedTravelEntertainmentModel instance" in {
      val underTest = aTravelEntertainmentModel

      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.sectionQuestion.get, textAndKey).returning(encryptedSectionQuestion)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.travelAndSubsistenceQuestion.get, textAndKey).returning(encryptedTravelAndSubsistenceQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.travelAndSubsistence.get, textAndKey).returning(encryptedTravelAndSubsistence)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.personalIncidentalExpensesQuestion.get, textAndKey).returning(encryptedPersonalIncidentalExpensesQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.personalIncidentalExpenses.get, textAndKey).returning(encryptedPersonalIncidentalExpenses)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.entertainingQuestion.get, textAndKey).returning(encryptedEntertainingQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.entertaining.get, textAndKey).returning(encryptedEntertaining)

      underTest.encrypted shouldBe EncryptedTravelEntertainmentModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        travelAndSubsistenceQuestion = Some(encryptedTravelAndSubsistenceQuestion),
        travelAndSubsistence = Some(encryptedTravelAndSubsistence),
        personalIncidentalExpensesQuestion = Some(encryptedPersonalIncidentalExpensesQuestion),
        personalIncidentalExpenses = Some(encryptedPersonalIncidentalExpenses),
        entertainingQuestion = Some(encryptedEntertainingQuestion),
        entertaining = Some(encryptedEntertaining)
      )
    }
  }

  "EncryptedTravelEntertainmentModel.decrypted" should {
    "return TravelEntertainmentModel instance" in {
      val underTest = EncryptedTravelEntertainmentModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        travelAndSubsistenceQuestion = Some(encryptedTravelAndSubsistenceQuestion),
        travelAndSubsistence = Some(encryptedTravelAndSubsistence),
        personalIncidentalExpensesQuestion = Some(encryptedPersonalIncidentalExpensesQuestion),
        personalIncidentalExpenses = Some(encryptedPersonalIncidentalExpenses),
        entertainingQuestion = Some(encryptedEntertainingQuestion),
        entertaining = Some(encryptedEntertaining)
      )

      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedSectionQuestion.value, encryptedSectionQuestion.nonce, textAndKey, *).returning(value = aTravelEntertainmentModel.sectionQuestion.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedTravelAndSubsistenceQuestion.value, encryptedTravelAndSubsistenceQuestion.nonce, textAndKey, *).returning(value = aTravelEntertainmentModel.travelAndSubsistenceQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedTravelAndSubsistence.value, encryptedTravelAndSubsistence.nonce, textAndKey, *).returning(value = aTravelEntertainmentModel.travelAndSubsistence.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedPersonalIncidentalExpensesQuestion.value, encryptedPersonalIncidentalExpensesQuestion.nonce, textAndKey, *)
        .returning(value = aTravelEntertainmentModel.personalIncidentalExpensesQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedPersonalIncidentalExpenses.value, encryptedPersonalIncidentalExpenses.nonce, textAndKey, *).returning(value = aTravelEntertainmentModel.personalIncidentalExpenses.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedEntertainingQuestion.value, encryptedEntertainingQuestion.nonce, textAndKey, *).returning(value = aTravelEntertainmentModel.entertainingQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedEntertaining.value, encryptedEntertaining.nonce, textAndKey, *).returning(value = aTravelEntertainmentModel.entertaining.get)

      underTest.decrypted shouldBe aTravelEntertainmentModel
    }
  }
}
