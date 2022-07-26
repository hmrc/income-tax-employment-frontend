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

import controllers.benefits.fuel.routes._
import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import play.api.mvc.Call
import support.builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import support.{TaxYearHelper, UnitTest}
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher}

class CarVanFuelModelSpec extends UnitTest
  with TaxYearHelper
  with MockFactory {

  private val employmentId = "employmentId"

  private val encryptedSectionQuestion = EncryptedValue("encryptedSectionQuestion", "some-nonce")
  private val encryptedCarQuestion = EncryptedValue("encryptedCarQuestion", "some-nonce")
  private val encryptedCar = EncryptedValue("encryptedCar", "some-nonce")
  private val encryptedCarFuelQuestion = EncryptedValue("encryptedCarFuelQuestion", "some-nonce")
  private val encryptedCarFuel = EncryptedValue("encryptedCarFuel", "some-nonce")
  private val encryptedVanQuestion = EncryptedValue("encryptedVanQuestion", "some-nonce")
  private val encryptedVan = EncryptedValue("encryptedVan", "some-nonce")
  private val encryptedVanFuelQuestion = EncryptedValue("encryptedVanFuelQuestion", "some-nonce")
  private val encryptedVanFuel = EncryptedValue("encryptedVanFuel", "some-nonce")
  private val encryptedMileageQuestion = EncryptedValue("encryptedMileageQuestion", "some-nonce")
  private val encryptedMileage = EncryptedValue("encryptedMileage", "some-nonce")

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private def result(url: String): Option[Call] = Some(Call("GET", url))

  "CarVanFuelModel.isFinished" should {
    "return car yes no page" in {
      aCarVanFuelModel.copy(carQuestion = None).isFinished(taxYear, employmentId) shouldBe result(CompanyCarBenefitsController.show(taxYear, employmentId).url)
    }
    "return none when section is finished" in {
      aCarVanFuelModel.copy(carQuestion = Some(false)).isFinished(taxYear, employmentId) shouldBe None
      aCarVanFuelModel.isFinished(taxYear, employmentId) shouldBe None
    }
  }

  "CarVanFuelModel.fullCarSectionFinished" should {
    "return car fuel yes no page" in {
      aCarVanFuelModel.copy(carFuelQuestion = None).fullCarSectionFinished(taxYear, employmentId) shouldBe result(CompanyCarFuelBenefitsController.show(taxYear, employmentId).url)
    }

    "return car amount page" in {
      aCarVanFuelModel.copy(car = None).fullCarSectionFinished(taxYear, employmentId) shouldBe result(CompanyCarBenefitsAmountController.show(taxYear, employmentId).url)
    }

    "return none when section is finished" in {
      aCarVanFuelModel.copy(carFuelQuestion = Some(false)).fullCarSectionFinished(taxYear, employmentId) shouldBe None
      aCarVanFuelModel.fullCarSectionFinished(taxYear, employmentId) shouldBe None
    }
  }

  "CarVanFuelModel.carFuelSectionFinished" should {
    "return car fuel yes no page" in {
      aCarVanFuelModel.copy(carFuelQuestion = None).carFuelSectionFinished(taxYear, employmentId) shouldBe result(CompanyCarFuelBenefitsController.show(taxYear, employmentId).url)
    }

    "return none when section is finished" in {
      aCarVanFuelModel.copy(carQuestion = Some(false), carFuelQuestion = None).carFuelSectionFinished(taxYear, employmentId) shouldBe None
      aCarVanFuelModel.carFuelSectionFinished(taxYear, employmentId) shouldBe None
    }
  }

  "CarVanFuelModel.vanSectionFinished" should {
    "return van yes no page" in {
      aCarVanFuelModel.copy(vanQuestion = None).vanSectionFinished(taxYear, employmentId) shouldBe result(CompanyVanBenefitsController.show(taxYear, employmentId).url)
    }
    "return van amount page" in {
      aCarVanFuelModel.copy(van = None).vanSectionFinished(taxYear, employmentId) shouldBe result(CompanyVanBenefitsAmountController.submit(taxYear, employmentId).url)
    }

    "return none when section is finished" in {
      aCarVanFuelModel.copy(vanQuestion = Some(false), vanFuelQuestion = None).vanSectionFinished(taxYear, employmentId) shouldBe None
      aCarVanFuelModel.vanSectionFinished(taxYear, employmentId) shouldBe None
    }
  }

  "CarVanFuelModel.clear" should {
    "clear the model" in {
      CarVanFuelModel.clear shouldBe CarVanFuelModel(sectionQuestion = Some(false))
    }
  }

  "CarVanFuelModel.encrypted" should {
    "return EncryptedCarVanFuelModel instance" in {
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(aCarVanFuelModel.sectionQuestion.get, textAndKey).returning(encryptedSectionQuestion)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(aCarVanFuelModel.carQuestion.get, textAndKey).returning(encryptedCarQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aCarVanFuelModel.car.get, textAndKey).returning(encryptedCar)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(aCarVanFuelModel.carFuelQuestion.get, textAndKey).returning(encryptedCarFuelQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aCarVanFuelModel.carFuel.get, textAndKey).returning(encryptedCarFuel)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(aCarVanFuelModel.vanQuestion.get, textAndKey).returning(encryptedVanQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aCarVanFuelModel.van.get, textAndKey).returning(encryptedVan)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(aCarVanFuelModel.vanFuelQuestion.get, textAndKey).returning(encryptedVanFuelQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aCarVanFuelModel.vanFuel.get, textAndKey).returning(encryptedVanFuel)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(aCarVanFuelModel.mileageQuestion.get, textAndKey).returning(encryptedMileageQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aCarVanFuelModel.mileage.get, textAndKey).returning(encryptedMileage)

      aCarVanFuelModel.encrypted shouldBe EncryptedCarVanFuelModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        carQuestion = Some(encryptedCarQuestion),
        car = Some(encryptedCar),
        carFuelQuestion = Some(encryptedCarFuelQuestion),
        carFuel = Some(encryptedCarFuel),
        vanQuestion = Some(encryptedVanQuestion),
        van = Some(encryptedVan),
        vanFuelQuestion = Some(encryptedVanFuelQuestion),
        vanFuel = Some(encryptedVanFuel),
        mileageQuestion = Some(encryptedMileageQuestion),
        mileage = Some(encryptedMileage)
      )
    }
  }

  "EncryptedCarVanFuelModel.decrypted" should {
    "return CarVanFuelModel instance" in {
      val underTest = EncryptedCarVanFuelModel(
        sectionQuestion = Some(encryptedSectionQuestion),
        carQuestion = Some(encryptedCarQuestion),
        car = Some(encryptedCar),
        carFuelQuestion = Some(encryptedCarFuelQuestion),
        carFuel = Some(encryptedCarFuel),
        vanQuestion = Some(encryptedVanQuestion),
        van = Some(encryptedVan),
        vanFuelQuestion = Some(encryptedVanFuelQuestion),
        vanFuel = Some(encryptedVanFuel),
        mileageQuestion = Some(encryptedMileageQuestion),
        mileage = Some(encryptedMileage)
      )

      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedSectionQuestion.value, encryptedSectionQuestion.nonce, textAndKey, *).returning(value = aCarVanFuelModel.sectionQuestion.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedCarQuestion.value, encryptedCarQuestion.nonce, textAndKey, *).returning(value = aCarVanFuelModel.carQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedCar.value, encryptedCar.nonce, textAndKey, *).returning(value = aCarVanFuelModel.car.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedCarFuelQuestion.value, encryptedCarFuelQuestion.nonce, textAndKey, *).returning(value = aCarVanFuelModel.carFuelQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedCarFuel.value, encryptedCarFuel.nonce, textAndKey, *).returning(value = aCarVanFuelModel.carFuel.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedVanQuestion.value, encryptedVanQuestion.nonce, textAndKey, *).returning(value = aCarVanFuelModel.vanQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedVan.value, encryptedVan.nonce, textAndKey, *).returning(value = aCarVanFuelModel.van.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedVanFuelQuestion.value, encryptedVanFuelQuestion.nonce, textAndKey, *).returning(value = aCarVanFuelModel.vanFuelQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedVanFuel.value, encryptedVanFuel.nonce, textAndKey, *).returning(value = aCarVanFuelModel.vanFuel.get)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedMileageQuestion.value, encryptedMileageQuestion.nonce, textAndKey, *).returning(value = aCarVanFuelModel.mileageQuestion.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedMileage.value, encryptedMileage.nonce, textAndKey, *).returning(value = aCarVanFuelModel.mileage.get)

      underTest.decrypted shouldBe aCarVanFuelModel
    }
  }
}
