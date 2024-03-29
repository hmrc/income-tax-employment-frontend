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

package utils

import controllers.benefits.accommodation._
import controllers.benefits.assets._
import controllers.benefits.fuel._
import models.mongo.EmploymentCYAModel
import models.redirects.ConditionalRedirect
import services.RedirectService

import javax.inject.{Inject, Singleton}

@Singleton
class RedirectsMapper @Inject()(redirectService: RedirectService) {

  //scalastyle:off
  def mapToRedirects(clazz: Class[_],
                     taxYear: Int,
                     employmentId: String,
                     employmentCYAModel: EmploymentCYAModel): Seq[ConditionalRedirect] = clazz match {
    // Accommodation redirects
    case _ if clazz == classOf[AccommodationRelocationBenefitsController] =>
      redirectService.accommodationRelocationBenefitsRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[LivingAccommodationBenefitsController] =>
      redirectService.commonAccommodationBenefitsRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[LivingAccommodationBenefitAmountController] =>
      redirectService.accommodationBenefitsAmountRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[NonQualifyingRelocationBenefitsController] =>
      redirectService.nonQualifyingRelocationBenefitsRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[NonQualifyingRelocationBenefitsAmountController] =>
      redirectService.nonQualifyingRelocationBenefitsAmountRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[QualifyingRelocationBenefitsController] =>
      redirectService.qualifyingRelocationBenefitsRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[QualifyingRelocationBenefitsAmountController] =>
      redirectService.qualifyingRelocationBenefitsAmountRedirects(employmentCYAModel, taxYear, employmentId)

    // Assets redirects
    case _ if clazz == classOf[AssetsOrAssetTransfersBenefitsController] =>
      redirectService.assetsRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[AssetsBenefitsController] =>
      redirectService.commonAssetsModelRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[AssetsBenefitsAmountController] =>
      redirectService.assetsAmountRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[AssetTransfersBenefitsController] =>
      redirectService.assetTransferRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[AssetTransfersBenefitsAmountController] =>
      redirectService.assetTransferAmountRedirects(employmentCYAModel, taxYear, employmentId)

    // Fuel redirects
    case _ if clazz == classOf[CarVanFuelBenefitsController] =>
      redirectService.commonBenefitsRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[CarFuelBenefitsAmountController] =>
      redirectService.carFuelBenefitsAmountRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[CompanyCarBenefitsController] =>
      redirectService.carBenefitsRedirects(employmentCYAModel, taxYear, employmentId)
    case _ if clazz == classOf[CompanyCarBenefitsAmountController] =>
      redirectService.carBenefitsAmountRedirects(employmentCYAModel, taxYear, employmentId)

    case _ => throw new IllegalArgumentException(s"${clazz.toString} could not be matched with redirects.")
  }
  //scalastyle:on
}
