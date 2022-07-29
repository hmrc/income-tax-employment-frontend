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

import controllers.benefits.assets.routes._
import controllers.employment.routes.CheckYourBenefitsController
import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.benefits.AssetsModelBuilder.anAssetsModel
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher, TaxYearHelper}

class AssetsModelSpec extends UnitTest
  with TaxYearHelper
  with MockFactory {

  private val employmentId = "employmentId"

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedSectionQuestion = EncryptedValue("encryptedSectionQuestion", "some-nonce")
  private val encryptedAssetsQuestion = EncryptedValue("encryptedAssetsQuestion", "some-nonce")
  private val encryptedAssets = EncryptedValue("encryptedAssets", "some-nonce")
  private val encryptedAssetTransferQuestion = EncryptedValue("encryptedAssetTransferQuestion", "some-nonce")
  private val encryptedAssetTransfer = EncryptedValue("encryptedAssetTransfer", "some-nonce")

  "AssetsModel.isFinished" should {
    "return assetsAndAssetsTransfer yes no page" in {
      anAssetsModel.copy(sectionQuestion = None).isFinished(taxYear, employmentId) shouldBe
        Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }
    "return none when section is finished" in {
      anAssetsModel.copy(sectionQuestion = Some(false)).isFinished(taxYear, "employmentId") shouldBe None
      anAssetsModel.isFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "AssetsModel.assetsSectionFinished" should {
    "return None when assetsQuestion is true and assets amount is defined" in {
      anAssetsModel.copy(assetsQuestion = Some(true), assets = Some(10.00)).assetsSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return assets amount page when assetsQuestion is true and no amount" in {
      anAssetsModel.copy(assetsQuestion = Some(true), assets = None).assetsSectionFinished(taxYear, employmentId) shouldBe
        Some(AssetsBenefitsAmountController.show(taxYear, employmentId))
    }

    "return None when assetsQuestion is false" in {
      anAssetsModel.copy(assetsQuestion = Some(false)).assetsSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return assets yes no page when assetsQuestion is None" in {
      anAssetsModel.copy(assetsQuestion = None).assetsSectionFinished(taxYear, employmentId) shouldBe
        Some(AssetsBenefitsController.show(taxYear, employmentId))
    }
  }

  "AssetsModel.assetTransferSectionFinished" should {
    "return None when assetTransferQuestion is true and asset transfer amount is defined" in {
      anAssetsModel.copy(assetTransferQuestion = Some(true), assetTransfer = Some(10.00)).assetTransferSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return assetTransfer amount page when assetTransferQuestion is true and asset transfer amount is None" in {
      anAssetsModel.copy(assetTransferQuestion = Some(true), assetTransfer = None).assetTransferSectionFinished(taxYear, employmentId) shouldBe
        Some(AssetsTransfersBenefitsAmountController.show(taxYear, employmentId))
    }

    "return None when assetTransferQuestion is false" in {
      anAssetsModel.copy(assetTransferQuestion = Some(false)).assetTransferSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return assetTransfer yes no page" in {
      anAssetsModel.copy(assetTransferQuestion = None).assetTransferSectionFinished(taxYear, employmentId) shouldBe
        Some(AssetTransfersBenefitsController.show(taxYear, employmentId))
    }
  }

  "AssetsModel.clear" should {
    "clear the model" in {
      AssetsModel.clear shouldBe AssetsModel(sectionQuestion = Some(false))
    }
  }

  "AssetsModel.encrypted" should {
    " return EncryptedAssetsModel instance" in {
      val underTest = anAssetsModel

      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(anAssetsModel.sectionQuestion.get, textAndKey).returning(encryptedSectionQuestion)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(anAssetsModel.assetsQuestion.get, textAndKey).returning(encryptedAssetsQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(anAssetsModel.assets.get, textAndKey).returning(encryptedAssets)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(anAssetsModel.assetTransferQuestion.get, textAndKey).returning(encryptedAssetTransferQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(anAssetsModel.assetTransfer.get, textAndKey).returning(encryptedAssetTransfer)

      underTest.encrypted shouldBe EncryptedAssetsModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        assetsQuestion = Some(encryptedAssetsQuestion),
        assets = Some(encryptedAssets),
        assetTransferQuestion = Some(encryptedAssetTransferQuestion),
        assetTransfer = Some(encryptedAssetTransfer)
      )
    }
  }

  "EncryptedAssetsModel.decrypted" should {
    "return AssetsModel instance" in {
      val underTest = EncryptedAssetsModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        assetsQuestion = Some(encryptedAssetsQuestion),
        assets = Some(encryptedAssets),
        assetTransferQuestion = Some(encryptedAssetTransferQuestion),
        assetTransfer = Some(encryptedAssetTransfer)
      )

      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedSectionQuestion.value, encryptedSectionQuestion.nonce, textAndKey, *).returning(value = anAssetsModel.sectionQuestion.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedAssetsQuestion.value, encryptedAssetsQuestion.nonce, textAndKey, *).returning(value = anAssetsModel.assetsQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedAssets.value, encryptedAssets.nonce, textAndKey, *).returning(value = anAssetsModel.assets.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedAssetTransferQuestion.value, encryptedAssetTransferQuestion.nonce, textAndKey, *).returning(value = anAssetsModel.assetTransferQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedAssetTransfer.value, encryptedAssetTransfer.nonce, textAndKey, *).returning(value = anAssetsModel.assetTransfer.get)

      underTest.decrypted shouldBe anAssetsModel
    }
  }
}
