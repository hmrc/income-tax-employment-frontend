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
import controllers.benefits.assets.{AssetTransfersBenefitsAmountController, AssetTransfersBenefitsController}
import controllers.benefits.fuel.{CarFuelBenefitsAmountController, CarVanFuelBenefitsController}
import support.TaxYearProvider
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.mocks.MockRedirectService

class RedirectsMapperSpec extends support.UnitTest
  with MockRedirectService
  with TaxYearProvider {

  private val employmentId = "employmentId"

  private val underTest = new RedirectsMapper(mockRedirectService)

  ".mapToRedirects" should {
    "throw Exception when unknown class is passed" in {
      intercept[Exception] {
        val unknownClass = classOf[String]
        underTest.mapToRedirects(unknownClass, taxYear, employmentId = employmentId, anEmploymentCYAModel)
      }
    }

    "return redirects from accommodationRelocationBenefitsRedirects when AccommodationRelocationBenefitsController class is given" in {
      val clazz = classOf[AccommodationRelocationBenefitsController]

      mockAccommodationRelocationBenefitsRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from commonAccommodationBenefitsRedirects when LivingAccommodationBenefitsController class is given" in {
      val clazz = classOf[LivingAccommodationBenefitsController]

      mockCommonAccommodationBenefitsRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from accommodationBenefitsAmountRedirects when LivingAccommodationBenefitAmountController class is given" in {
      val clazz = classOf[LivingAccommodationBenefitAmountController]

      mockAccommodationBenefitsAmountRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from commonAccommodationBenefitsRedirects when NonQualifyingRelocationBenefitsController class is given" in {
      val clazz = classOf[NonQualifyingRelocationBenefitsController]

      mockNonQualifyingRelocationBenefitsRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from commonAccommodationBenefitsRedirects when NonQualifyingRelocationBenefitsAmountController class is given" in {
      val clazz = classOf[NonQualifyingRelocationBenefitsAmountController]

      mockNonQualifyingRelocationAmountRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from qualifyingRelocationBenefitsRedirects when QualifyingRelocationBenefitsController class is given" in {
      val clazz = classOf[QualifyingRelocationBenefitsController]

      mockQualifyingRelocationBenefitsRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from qualifyingRelocationBenefitsAmountRedirects when QualifyingRelocationBenefitsAmountController class is given" in {
      val clazz = classOf[QualifyingRelocationBenefitsAmountController]

      mockQualifyingRelocationBenefitsAmountRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from assetTransferRedirects when AssetTransfersBenefitsController class is given" in {
      val clazz = classOf[AssetTransfersBenefitsController]

      mockAssetTransferRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from assetTransferAmountRedirects when AssetsTransfersBenefitsAmountController class is given" in {
      val clazz = classOf[AssetTransfersBenefitsAmountController]

      mockAssetTransferAmountRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from commonBenefitsRedirects when CarVanFuelBenefitsController class is given" in {
      val clazz = classOf[CarVanFuelBenefitsController]

      mockCommonBenefitsRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }

    "return redirects from carFuelBenefitsAmountRedirects when CarFuelBenefitsAmountController class is given" in {
      val clazz = classOf[CarFuelBenefitsAmountController]

      mockCarFuelBenefitsAmountRedirects(anEmploymentCYAModel, taxYear, employmentId, Seq.empty)

      underTest.mapToRedirects(clazz, taxYear, employmentId, anEmploymentCYAModel)
    }
  }
}
