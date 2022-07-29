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

import controllers.benefits.income.routes._
import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.benefits.IncomeTaxAndCostsModelBuilder.anIncomeTaxAndCostsModel
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher, TaxYearHelper}

class IncomeTaxAndCostsModelSpec extends UnitTest
  with TaxYearHelper
  with MockFactory {

  private val employmentId = "some-employment-id"

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedSectionQuestion = EncryptedValue("encryptedSectionQuestion", "some-nonce")
  private val encryptedIncomeTaxPaidByDirectorQuestion = EncryptedValue("encryptedIncomeTaxPaidByDirectorQuestion", "some-nonce")
  private val encryptedIncomeTaxPaidByDirector = EncryptedValue("encryptedIncomeTaxPaidByDirector", "some-nonce")
  private val encryptedPaymentsOnEmployeesBehalfQuestion = EncryptedValue("encryptedPaymentsOnEmployeesBehalfQuestion", "some-nonce")
  private val encryptedPaymentsOnEmployeesBehalf = EncryptedValue("encryptedPaymentsOnEmployeesBehalf", "some-nonce")

  "IncomeTaxAndCostsModel.incomeTaxPaidByDirectorSectionFinished" should {
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

  "IncomeTaxAndCostsModel.paymentsOnEmployeesBehalfSectionFinished" should {
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

  "IncomeTaxAndCostsModel.isFinished" should {
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

  "IncomeTaxAndCostsModel.clear" should {
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

  "IncomeTaxAndCostsModel.encrypted" should {
    "return EncryptedIncomeTaxAndCostsModel instance" in {
      val underTest = anIncomeTaxAndCostsModel

      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.sectionQuestion.get, textAndKey).returning(encryptedSectionQuestion)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.incomeTaxPaidByDirectorQuestion.get, textAndKey).returning(encryptedIncomeTaxPaidByDirectorQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.incomeTaxPaidByDirector.get, textAndKey).returning(encryptedIncomeTaxPaidByDirector)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(underTest.paymentsOnEmployeesBehalfQuestion.get, textAndKey).returning(encryptedPaymentsOnEmployeesBehalfQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(underTest.paymentsOnEmployeesBehalf.get, textAndKey).returning(encryptedPaymentsOnEmployeesBehalf)

      underTest.encrypted shouldBe EncryptedIncomeTaxAndCostsModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        incomeTaxPaidByDirectorQuestion = Some(encryptedIncomeTaxPaidByDirectorQuestion),
        incomeTaxPaidByDirector = Some(encryptedIncomeTaxPaidByDirector),
        paymentsOnEmployeesBehalfQuestion = Some(encryptedPaymentsOnEmployeesBehalfQuestion),
        paymentsOnEmployeesBehalf = Some(encryptedPaymentsOnEmployeesBehalf)
      )
    }
  }

  "EncryptedIncomeTaxAndCostsModel.decrypted" should {
    "return IncomeTaxAndCostsModel instance" in {
      val underTest = EncryptedIncomeTaxAndCostsModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        incomeTaxPaidByDirectorQuestion = Some(encryptedIncomeTaxPaidByDirectorQuestion),
        incomeTaxPaidByDirector = Some(encryptedIncomeTaxPaidByDirector),
        paymentsOnEmployeesBehalfQuestion = Some(encryptedPaymentsOnEmployeesBehalfQuestion),
        paymentsOnEmployeesBehalf = Some(encryptedPaymentsOnEmployeesBehalf)
      )

      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedSectionQuestion.value, encryptedSectionQuestion.nonce, textAndKey, *).returning(value = anIncomeTaxAndCostsModel.sectionQuestion.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedIncomeTaxPaidByDirectorQuestion.value, encryptedIncomeTaxPaidByDirectorQuestion.nonce, textAndKey, *)
        .returning(value = anIncomeTaxAndCostsModel.incomeTaxPaidByDirectorQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedIncomeTaxPaidByDirector.value, encryptedIncomeTaxPaidByDirector.nonce, textAndKey, *).returning(value = anIncomeTaxAndCostsModel.incomeTaxPaidByDirector.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedPaymentsOnEmployeesBehalfQuestion.value, encryptedPaymentsOnEmployeesBehalfQuestion.nonce, textAndKey, *)
        .returning(value = anIncomeTaxAndCostsModel.paymentsOnEmployeesBehalfQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedPaymentsOnEmployeesBehalf.value, encryptedPaymentsOnEmployeesBehalf.nonce, textAndKey, *).returning(value = anIncomeTaxAndCostsModel.paymentsOnEmployeesBehalf.get)

      underTest.decrypted shouldBe anIncomeTaxAndCostsModel
    }
  }
}
