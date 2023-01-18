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

package support.builders.models.benefits

import models.benefits.BenefitsViewModel
import support.builders.models.benefits.AccommodationRelocationModelBuilder.anAccommodationRelocationModel
import support.builders.models.benefits.AssetsModelBuilder.anAssetsModel
import support.builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import support.builders.models.benefits.IncomeTaxAndCostsModelBuilder.anIncomeTaxAndCostsModel
import support.builders.models.benefits.MedicalChildcareEducationModelBuilder.aMedicalChildcareEducationModel
import support.builders.models.benefits.ReimbursedCostsVouchersAndNonCashModelBuilder.aReimbursedCostsVouchersAndNonCashModel
import support.builders.models.benefits.TravelEntertainmentModelBuilder.aTravelEntertainmentModel
import support.builders.models.benefits.UtilitiesAndServicesModelBuilder.aUtilitiesAndServicesModel

object BenefitsViewModelBuilder {

  val aBenefitsViewModel: BenefitsViewModel = BenefitsViewModel(
    carVanFuelModel = Some(aCarVanFuelModel),
    accommodationRelocationModel = Some(anAccommodationRelocationModel),
    travelEntertainmentModel = Some(aTravelEntertainmentModel),
    utilitiesAndServicesModel = Some(aUtilitiesAndServicesModel),
    medicalChildcareEducationModel = Some(aMedicalChildcareEducationModel),
    incomeTaxAndCostsModel = Some(anIncomeTaxAndCostsModel),
    reimbursedCostsVouchersAndNonCashModel = Some(aReimbursedCostsVouchersAndNonCashModel),
    assetsModel = Some(anAssetsModel),
    submittedOn = None,
    isUsingCustomerData = true,
    isBenefitsReceived = true
  )
}
