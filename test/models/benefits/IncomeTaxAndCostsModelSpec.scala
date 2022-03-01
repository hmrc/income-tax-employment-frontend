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

package models.benefits

import controllers.employment.routes.CheckYourBenefitsController
import controllers.benefits.income.routes._
import utils.UnitTest

class IncomeTaxAndCostsModelSpec extends UnitTest {

  private val employmentId = "some-employment-id"

  "incomeTaxPaidByDirectorSectionFinished" should {
    "return None when incomeTaxPaidByDirectorQuestion is true and incomeTaxPaidByDirector amount is defined" in {
      val underTest = IncomeTaxAndCostsModel(incomeTaxPaidByDirectorQuestion = Some(true), incomeTaxPaidByDirector = Some(1))
      underTest.incomeTaxPaidByDirectorSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Income tax paid by director amount' page when incomeTaxPaidByDirectorQuestion is true and incomeTaxPaidByDirector amount not defined" in {
      val underTest = IncomeTaxAndCostsModel(incomeTaxPaidByDirectorQuestion = Some(true), incomeTaxPaidByDirector = None)
      underTest.incomeTaxPaidByDirectorSectionFinished(taxYearEOY, employmentId) shouldBe Some(IncomeTaxBenefitsAmountController.show(taxYearEOY, employmentId))
    }

    "return None when incomeTaxPaidByDirectorQuestion is false" in {
      val underTest = IncomeTaxAndCostsModel(incomeTaxPaidByDirectorQuestion = Some(false))
      underTest.incomeTaxPaidByDirectorSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Income tax paid by director' yes/no page when incomeTaxPaidByDirectorQuestion is None" in {
      val underTest = IncomeTaxAndCostsModel(incomeTaxPaidByDirectorQuestion = None)
      underTest.incomeTaxPaidByDirectorSectionFinished(taxYearEOY, employmentId) shouldBe Some(IncomeTaxBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "paymentsOnEmployeesBehalfSectionFinished" should {
    "return None when paymentsOnEmployeesBehalfQuestion is true and paymentsOnEmployeesBehalf amount is defined" in {
      val underTest = IncomeTaxAndCostsModel(paymentsOnEmployeesBehalfQuestion = Some(true), paymentsOnEmployeesBehalf = Some(1))
      underTest.paymentsOnEmployeesBehalfSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Payments on employees behalf' amount page when paymentsOnEmployeesBehalfQuestion is true and paymentsOnEmployeesBehalf not defined" in {
      val underTest = IncomeTaxAndCostsModel(paymentsOnEmployeesBehalfQuestion = Some(true), paymentsOnEmployeesBehalf = None)
      underTest.paymentsOnEmployeesBehalfSectionFinished(taxYearEOY, employmentId) shouldBe Some(IncurredCostsBenefitsAmountController.show(taxYearEOY, employmentId))
    }

    "return None when paymentsOnEmployeesBehalfQuestion is false" in {
      val underTest = IncomeTaxAndCostsModel(paymentsOnEmployeesBehalfQuestion = Some(false))
      underTest.paymentsOnEmployeesBehalfSectionFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Payments on employees behalf' yes/no page when paymentsOnEmployeesBehalfQuestion is None" in {
      val underTest = IncomeTaxAndCostsModel(paymentsOnEmployeesBehalfQuestion = None)
      underTest.paymentsOnEmployeesBehalfSectionFinished(taxYearEOY, employmentId) shouldBe Some(IncurredCostsBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "isFinished" should {
    "return result of incomeTaxPaidByDirectorSectionFinished when incomeTaxOrCostsQuestion is true and incomeTaxPaidByDirectorQuestion is not None" in {
      val underTest = IncomeTaxAndCostsModel(sectionQuestion = Some(true), incomeTaxPaidByDirectorQuestion = None)
      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.incomeTaxPaidByDirectorSectionFinished(taxYearEOY, employmentId)
    }

    "return result of paymentsOnEmployeesBehalfSectionFinished when incomeTaxOrCostsQuestion is true and incomeTaxPaidByDirectorQuestion is false and " +
      "paymentsOnEmployeesBehalfQuestion is None" in {
      val underTest = IncomeTaxAndCostsModel(sectionQuestion = Some(true),
        incomeTaxPaidByDirectorQuestion = Some(false),
        paymentsOnEmployeesBehalfQuestion = None)

      underTest.isFinished(taxYearEOY, employmentId) shouldBe underTest.paymentsOnEmployeesBehalfSectionFinished(taxYearEOY, employmentId)
    }

    "return None when incomeTaxOrCostsQuestion is true and incomeTaxPaidByDirectorQuestion and paymentsOnEmployeesBehalfQuestion are false" in {
      val underTest = IncomeTaxAndCostsModel(sectionQuestion = Some(true),
        incomeTaxPaidByDirectorQuestion = Some(false),
        paymentsOnEmployeesBehalfQuestion = Some(false)
      )

      underTest.isFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return None when incomeTaxOrCostsQuestion is false" in {
      val underTest = IncomeTaxAndCostsModel(sectionQuestion = Some(false))
      underTest.isFinished(taxYearEOY, employmentId) shouldBe None
    }

    "return call to 'Income tax or costs' section yes/no page when incomeTaxOrCostsQuestion is None" in {
      val underTest = IncomeTaxAndCostsModel(sectionQuestion = None)
      underTest.isFinished(taxYearEOY, employmentId) shouldBe Some(IncomeTaxOrIncurredCostsBenefitsController.show(taxYearEOY, employmentId))
    }
  }

  "clear" should {
    "return empty IncomeTaxAndCostsModel with main question set to false" in {
      IncomeTaxAndCostsModel.clear shouldBe IncomeTaxAndCostsModel(
        sectionQuestion = Some(false),
        incomeTaxPaidByDirectorQuestion = None,
        incomeTaxPaidByDirector = None,
        paymentsOnEmployeesBehalfQuestion = None,
        paymentsOnEmployeesBehalf = None
      )
    }
  }
}
