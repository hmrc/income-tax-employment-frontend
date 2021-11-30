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

package models.benefits

import controllers.employment.routes.CheckYourBenefitsController
import controllers.benefits.assets.routes._
import utils.UnitTest

class AssetsModelSpec extends UnitTest {

  private val employmentId = "id"
  private val taxYear = 2022
  private val model = AssetsModel(
    assetsAndAssetsTransferQuestion = Some(true),
    assetsQuestion = Some(true),
    assets = Some(55.55),
    assetTransferQuestion = Some(true),
    assetTransfer = Some(55.55)
  )

  "isFinished" should {
    "return assetsAndAssetsTransfer yes no page" in {
      model.copy(assetsAndAssetsTransferQuestion = None).isFinished(taxYear, employmentId) shouldBe
        Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }
    "return none when section is finished" in {
      model.copy(assetsAndAssetsTransferQuestion = Some(false)).isFinished(taxYear, "employmentId") shouldBe None
      model.isFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "assetsSectionFinished" should {
    "return None when assetsQuestion is true and assets amount is defined" in {
      model.copy(assetsQuestion = Some(true), assets = Some(10.00)).assetsSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return assets amount page when assetsQuestion is true and no amount" in {
      model.copy(assetsQuestion = Some(true), assets = None).assetsSectionFinished(taxYear, employmentId) shouldBe
        Some(AssetsBenefitsAmountController.show(taxYear, employmentId))
    }

    "return None when assetsQuestion is false" in {
      model.copy(assetsQuestion = Some(false)).assetsSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return assets yes no page when assetsQuestion is None" in {
      model.copy(assetsQuestion = None).assetsSectionFinished(taxYear, employmentId) shouldBe
        Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }
  }

  "assetTransferSectionFinished" should {
    "return None when assetTransferQuestion is true and asset transfer amount is defined" in {
      model.copy(assetTransferQuestion = Some(true), assetTransfer = Some(10.00)).assetTransferSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return assetTransfer amount page when assetTransferQuestion is true and asset transfer amount is None" in {
      model.copy(assetTransferQuestion = Some(true), assetTransfer = None).assetTransferSectionFinished(taxYear, employmentId) shouldBe
        Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }

    "return None when assetTransferQuestion is false" in {
      model.copy(assetTransferQuestion = Some(false)).assetTransferSectionFinished(taxYear, employmentId) shouldBe None
    }

    "return assetTransfer yes no page" in {
      model.copy(assetTransferQuestion = None).assetTransferSectionFinished(taxYear, employmentId) shouldBe
        Some(CheckYourBenefitsController.show(taxYear, employmentId))
    }
  }

  "clear" should {
    "clear the model" in {
      AssetsModel.clear shouldBe AssetsModel(assetsAndAssetsTransferQuestion = Some(false))
    }
  }
}
