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

package builders.models.benefits

import builders.models.benefits.AccommodationRelocationModelBuilder.aAccommodationRelocationModel
import builders.models.benefits.AssetsModelBuilder.anAssetsModel
import builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import builders.models.benefits.IncomeTaxAndCostsModelBuilder.aIncomeTaxAndCostsModel
import builders.models.benefits.MedicalChildcareEducationModelBuilder.aMedicalChildcareEducationModel
import builders.models.benefits.ReimbursedCostsVouchersAndNonCashModelBuilder.aReimbursedCostsVouchersAndNonCashModel
import builders.models.benefits.TravelEntertainmentModelBuilder.aTravelEntertainmentModel
import builders.models.benefits.UtilitiesAndServicesModelBuilder.aUtilitiesAndServicesModel
import models.benefits.BenefitsViewModel

object BenefitsViewModelBuilder {

  val aBenefitsViewModel: BenefitsViewModel = BenefitsViewModel(
    carVanFuelModel = Some(aCarVanFuelModel),
    accommodationRelocationModel = Some(aAccommodationRelocationModel),
    travelEntertainmentModel = Some(aTravelEntertainmentModel),
    utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel),
    isUsingCustomerData = true,
    isBenefitsReceived = true,
    medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel),
    incomeTaxAndCostsModel = Some(aIncomeTaxAndCostsModel),
    reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel),
    assetsModel = Some(anAssetsModel))
}
