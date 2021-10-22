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

package models.employment

import controllers.benefits.routes.{TravelAndSubsistenceBenefitsController, TravelOrEntertainmentBenefitsController, TravelOrSubsistenceBenefitsAmountController}
import controllers.employment.routes.CheckYourBenefitsController
import utils.UnitTest

class TravelEntertainmentModelSpec extends UnitTest {

  private val taxYear = 2021
  private val employmentId = "some-employment-id"

  "travelSectionFinished" should {
    "return None when travelAndSubsistenceQuestion is true and travelAndSubsistence is defined" in {
      val underTest = TravelEntertainmentModel(travelAndSubsistenceQuestion = Some(true), travelAndSubsistence = Some(1))

      underTest.travelSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return call to TravelOrSubsistenceBenefitsAmountController when travelAndSubsistenceQuestion is true" in {
      val underTest = TravelEntertainmentModel(travelAndSubsistenceQuestion = Some(true))

      underTest.travelSectionFinished(taxYear, employmentId) shouldBe Some(TravelOrSubsistenceBenefitsAmountController.show(taxYear, employmentId))
    }

    "return None when travelAndSubsistenceQuestion is false" in {
      val underTest = TravelEntertainmentModel(travelAndSubsistenceQuestion = Some(false))

      underTest.travelSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return call to TravelAndSubsistenceBenefitsController when travelAndSubsistenceQuestion is None" in {
      val underTest = TravelEntertainmentModel(travelAndSubsistenceQuestion = None)

      underTest.travelSectionFinished(taxYear, employmentId) shouldBe Some(TravelAndSubsistenceBenefitsController.show(taxYear, employmentId))
    }
  }

  "personalIncidentalSectionFinished" should {
    "return None when personalIncidentalExpensesQuestion is true and personalIncidentalExpenses is defined" in {
      val underTest = TravelEntertainmentModel(personalIncidentalExpensesQuestion = Some(true), personalIncidentalExpenses = Some(1))

      underTest.personalIncidentalSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return call to CheckYourBenefitsController when personalIncidentalExpensesQuestion is true" in {
      val underTest = TravelEntertainmentModel(personalIncidentalExpensesQuestion = Some(true))

      underTest.personalIncidentalSectionFinished(taxYear, employmentId) shouldBe Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }

    "return None when personalIncidentalExpensesQuestion is false" in {
      val underTest = TravelEntertainmentModel(personalIncidentalExpensesQuestion = Some(false))

      underTest.personalIncidentalSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return call to CheckYourBenefitsController when personalIncidentalExpensesQuestion is None" in {
      val underTest = TravelEntertainmentModel(personalIncidentalExpensesQuestion = None)

      underTest.personalIncidentalSectionFinished(taxYear, employmentId) shouldBe Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }
  }

  "entertainingSectionFinished" should {
    "return None when entertainingQuestion is true and entertaining is defined" in {
      val underTest = TravelEntertainmentModel(entertainingQuestion = Some(true), entertaining = Some(1))

      underTest.entertainingSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return call to CheckYourBenefitsController when entertainingQuestion is true" in {
      val underTest = TravelEntertainmentModel(entertainingQuestion = Some(true))

      underTest.entertainingSectionFinished(taxYear, employmentId) shouldBe Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }

    "return None when entertainingQuestion is false" in {
      val underTest = TravelEntertainmentModel(entertainingQuestion = Some(false))

      underTest.entertainingSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return call to CheckYourBenefitsController when entertainingQuestion is None" in {
      val underTest = TravelEntertainmentModel(entertainingQuestion = None)

      underTest.entertainingSectionFinished(taxYear, employmentId) shouldBe Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }
  }

  "isFinished" should {
    "return result of travelSectionFinished when travelEntertainmentQuestion is true and travelAndSubsistenceQuestion is not None" in {
      val underTest = TravelEntertainmentModel(travelEntertainmentQuestion = Some(true), travelAndSubsistenceQuestion = None)

      underTest.isFinished(taxYear, employmentId) shouldBe underTest.travelSectionFinished(taxYear, employmentId)
    }

    "return result of personalIncidentalSectionFinished when travelEntertainmentQuestion is true and travelAndSubsistenceQuestion is false" in {
      val underTest = TravelEntertainmentModel(travelEntertainmentQuestion = Some(true), travelAndSubsistenceQuestion = Some(false))

      underTest.isFinished(taxYear, employmentId) shouldBe underTest.personalIncidentalSectionFinished(taxYear, employmentId)
    }

    "return result of entertainingSectionFinished when travelEntertainmentQuestion is true and " +
      "travelAndSubsistenceQuestion is false and personalIncidentalExpensesQuestion is false" in {
      val underTest = TravelEntertainmentModel(travelEntertainmentQuestion = Some(true),
        travelAndSubsistenceQuestion = Some(false),
        personalIncidentalExpensesQuestion = Some(false))

      underTest.isFinished(taxYear, employmentId) shouldBe underTest.entertainingSectionFinished(taxYear, employmentId)
    }

    "return None when travelEntertainmentQuestion is true and travelAndSubsistenceQuestion, " +
      "personalIncidentalExpensesQuestion, entertainingQuestion are false" in {
      val underTest = TravelEntertainmentModel(travelEntertainmentQuestion = Some(true),
        travelAndSubsistenceQuestion = Some(false),
        personalIncidentalExpensesQuestion = Some(false),
        entertainingQuestion = Some(false))

      underTest.isFinished(taxYear, employmentId) shouldBe None
    }

    "return None when travelEntertainmentQuestion is false" in {
      val underTest = TravelEntertainmentModel(travelEntertainmentQuestion = Some(false))

      underTest.isFinished(taxYear, employmentId) shouldBe None
    }

    "return call to TravelOrEntertainmentBenefitsController when travelEntertainmentQuestion is None" in {
      val underTest = TravelEntertainmentModel(travelEntertainmentQuestion = None)

      underTest.isFinished(taxYear, employmentId) shouldBe Some(TravelOrEntertainmentBenefitsController.show(taxYear, employmentId))
    }
  }
}
