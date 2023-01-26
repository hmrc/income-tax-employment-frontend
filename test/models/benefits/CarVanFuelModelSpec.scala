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

package models.benefits

import controllers.benefits.fuel.routes._
import org.scalamock.scalatest.MockFactory
import play.api.mvc.Call
import support.builders.models.benefits.CarVanFuelModelBuilder.aCarVanFuelModel
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

class CarVanFuelModelSpec extends UnitTest
  with TaxYearProvider
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

  private implicit val secureGCMCipher: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

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
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.sectionQuestion.get.toString, associatedText).returning(encryptedSectionQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.carQuestion.get.toString, associatedText).returning(encryptedCarQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.car.get.toString(), associatedText).returning(encryptedCar)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.carFuelQuestion.get.toString, associatedText).returning(encryptedCarFuelQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.carFuel.get.toString(), associatedText).returning(encryptedCarFuel)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.vanQuestion.get.toString, associatedText).returning(encryptedVanQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.van.get.toString(), associatedText).returning(encryptedVan)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.vanFuelQuestion.get.toString, associatedText).returning(encryptedVanFuelQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.vanFuel.get.toString(), associatedText).returning(encryptedVanFuel)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.mileageQuestion.get.toString, associatedText).returning(encryptedMileageQuestion)
      (secureGCMCipher.encrypt(_: String)(_: String)).expects(aCarVanFuelModel.mileage.get.toString(), associatedText).returning(encryptedMileage)

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

      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedSectionQuestion, associatedText).returning(value = aCarVanFuelModel.sectionQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedCarQuestion, associatedText).returning(value = aCarVanFuelModel.carQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedCar, associatedText).returning(value = aCarVanFuelModel.car.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedCarFuelQuestion, associatedText).returning(value = aCarVanFuelModel.carFuelQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedCarFuel, associatedText).returning(value = aCarVanFuelModel.carFuel.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedVanQuestion, associatedText).returning(value = aCarVanFuelModel.vanQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedVan, associatedText).returning(value = aCarVanFuelModel.van.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedVanFuelQuestion, associatedText).returning(value = aCarVanFuelModel.vanFuelQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedVanFuel, associatedText).returning(value = aCarVanFuelModel.vanFuel.get.toString())
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedMileageQuestion, associatedText).returning(value = aCarVanFuelModel.mileageQuestion.get.toString)
      (secureGCMCipher.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedMileage, associatedText).returning(value = aCarVanFuelModel.mileage.get.toString())

      underTest.decrypted shouldBe aCarVanFuelModel
    }
  }
}
