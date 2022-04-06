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

package models

import models.benefits.CarVanFuelModel
import play.api.mvc.Call
import utils.UnitTest

class CarVanFuelModelSpec extends UnitTest {

  val model = CarVanFuelModel(
    sectionQuestion = Some(true),
    carQuestion = Some(true),
    car = Some(5555),
    carFuelQuestion = Some(true),
    carFuel = Some(5555),
    vanQuestion = Some(true),
    van = Some(5555),
    vanFuelQuestion = Some(true),
    vanFuel = Some(5555),
    mileageQuestion = Some(true),
    mileage = Some(5555)
  )

  def result(url: String): Option[Call] = Some(Call("GET",url))

  "isFinished" should {
    "return car yes no page" in {
      model.copy(carQuestion = None).isFinished(taxYear, "id") shouldBe result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/company-car?employmentId=id")
    }
    "return none when section is finished" in {
      model.copy(carQuestion = Some(false)).isFinished(taxYear, "employmentId") shouldBe None
      model.isFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "fullCarSectionFinished" should {
    "return car fuel yes no page" in {
      model.copy(carFuelQuestion = None).fullCarSectionFinished(taxYear, "id") shouldBe result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/car-fuel?employmentId=id")
    }

    "return car amount page" in {
      model.copy(car = None).fullCarSectionFinished(taxYear, "id") shouldBe result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/company-car-amount?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(carFuelQuestion = Some(false)).fullCarSectionFinished(taxYear, "employmentId") shouldBe None
      model.fullCarSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "carFuelSectionFinished" should {
    "return car fuel yes no page" in {
      model.copy(carFuelQuestion = None).carFuelSectionFinished(taxYear, "id") shouldBe result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/car-fuel?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(carQuestion = Some(false), carFuelQuestion = None).carFuelSectionFinished(taxYear, "employmentId") shouldBe None
      model.carFuelSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "vanSectionFinished" should {
    "return van yes no page" in {
      model.copy(vanQuestion = None).vanSectionFinished(taxYear, "id") shouldBe result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/company-van?employmentId=id")
    }
    "return van amount page" in {
      model.copy(van = None).vanSectionFinished(taxYear, "id") shouldBe result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/company-van-amount?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(vanQuestion = Some(false), vanFuelQuestion = None).vanSectionFinished(taxYear, "employmentId") shouldBe None
      model.vanSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "clear" should {
    "clear the model" in {
      CarVanFuelModel.clear shouldBe CarVanFuelModel(sectionQuestion = Some(false))
    }
  }
}
