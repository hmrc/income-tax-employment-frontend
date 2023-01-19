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

import models.benefits.MedicalChildcareEducationModel

object MedicalChildcareEducationModelBuilder {

  val aMedicalChildcareEducationModel: MedicalChildcareEducationModel = MedicalChildcareEducationModel(
    sectionQuestion = Some(true),
    medicalInsuranceQuestion = Some(true),
    medicalInsurance = Some(100.00),
    nurseryPlacesQuestion = Some(true),
    nurseryPlaces = Some(200.00),
    educationalServicesQuestion = Some(true),
    educationalServices = Some(300.00),
    beneficialLoanQuestion = Some(true),
    beneficialLoan = Some(400.00)
  )
}
