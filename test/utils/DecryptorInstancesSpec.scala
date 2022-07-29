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

package utils

import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.TypeCaster.Converter
import utils.TypeCaster.Converter.{bigDecimalLoader, booleanLoader, stringLoader}

class DecryptorInstancesSpec extends support.UnitTest
  with MockFactory {

  private val encryptedValue = EncryptedValue("some-value", "some-nonce")

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  "booleanDecryptor" should {
    "decrypt to boolean values" in {
      val booleanValue = true

      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedValue.value, encryptedValue.nonce, textAndKey, booleanLoader).returning(booleanValue)

      booleanDecryptor.decrypt(encryptedValue) shouldBe booleanValue
    }
  }

  "stringDecryptor" should {
    "decrypt to string values" in {
      val stringValue = "some-string-value"

      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedValue.value, encryptedValue.nonce, textAndKey, stringLoader).returning(stringValue)

      stringDecryptor.decrypt(encryptedValue) shouldBe stringValue
    }
  }

  "bigDecimalDecryptor" should {
    "decrypt to BigDecimal values" in {
      val bigDecimalValue: BigDecimal = 50.1

      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedValue.value, encryptedValue.nonce, textAndKey, bigDecimalLoader).returning(bigDecimalValue)

      bigDecimalDecryptor.decrypt(encryptedValue) shouldBe bigDecimalValue
    }
  }
}
