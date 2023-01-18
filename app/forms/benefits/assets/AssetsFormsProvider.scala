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

package forms.benefits.assets

import forms.{AmountForm, YesNoForm}
import play.api.data.Form

import javax.inject.Singleton

@Singleton
class AssetsFormsProvider {

  def assetsOrAssetTransfersForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.assetsOrAssetTransfers.error.${if (isAgent) "agent" else "individual"}"
  )

  def assetsForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.assets.error.${if (isAgent) "agent" else "individual"}"
  )

  def assetsAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.assetsAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.assetsAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.assetsAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def assetTransfersForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"benefits.assetTransfers.error.${if (isAgent) "agent" else "individual"}"
  )

  def assetTransfersAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"benefits.assetTransfersAmount.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"benefits.assetTransfersAmount.error.incorrectFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"benefits.assetTransfersAmount.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )
}
