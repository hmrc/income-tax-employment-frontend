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

import models.benefits.CarVanFuelModel

object CarVanFuelModelBuilder {

  val aCarVanFuelModel: CarVanFuelModel = CarVanFuelModel(
    carVanFuelQuestion = Some(true),
    carQuestion = Some(true),
    car = Some(100.00),
    carFuelQuestion = Some(true),
    carFuel = Some(200.00),
    vanQuestion = Some(true),
    van = Some(300.00),
    vanFuelQuestion = Some(true),
    vanFuel = Some(400.00),
    mileageQuestion = Some(true),
    mileage = Some(400.00)
  )
}
