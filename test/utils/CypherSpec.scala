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

import org.scalamock.scalatest.MockFactory
import support.UnitTest
import uk.gov.hmrc.crypto.EncryptedValue
import utils.Cypher.{bigDecimalCypher, booleanCypher, monthCypher, stringCypher}

import java.time.Month

class CypherSpec extends UnitTest
  with MockFactory {

  private val encryptedBoolean = mock[EncryptedValue]
  private val encryptedString = mock[EncryptedValue]
  private val encryptedBigDecimal = mock[EncryptedValue]
  private val encryptedMonth = mock[EncryptedValue]
  private val encryptedValue = EncryptedValue("some-value", "some-nonce")

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  "stringCypher" should {
    val stringValue = "some-string-value"
    "encrypt string values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(stringValue, associatedText).returning(encryptedString)

      stringCypher.encrypt(stringValue) shouldBe encryptedString
    }

    "decrypt to string values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(stringValue)

      stringCypher.decrypt(encryptedValue) shouldBe stringValue
    }
  }

  "booleanCypher" should {
    val someBoolean = true
    "encrypt boolean values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(someBoolean.toString, associatedText).returning(encryptedBoolean)

      booleanCypher.encrypt(someBoolean) shouldBe encryptedBoolean
    }

    "decrypt to boolean values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(someBoolean.toString)

      booleanCypher.decrypt(encryptedValue) shouldBe someBoolean
    }
  }

  "bigDecimalCypher" should {
    val bigDecimalValue: BigDecimal = 500.0
    "encrypt BigDecimal values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(bigDecimalValue.toString, associatedText).returning(encryptedBigDecimal)

      bigDecimalCypher.encrypt(bigDecimalValue) shouldBe encryptedBigDecimal
    }

    "decrypt to BigDecimal values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(bigDecimalValue.toString)

      bigDecimalCypher.decrypt(encryptedValue) shouldBe bigDecimalValue
    }
  }


  "monthCypher" should {
    val monthValue: Month = Month.APRIL
    "encrypt Month values" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(monthValue.toString, associatedText).returning(encryptedMonth)

      monthCypher.encrypt(monthValue) shouldBe encryptedMonth
    }

    "decrypt to Month values" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedValue, associatedText).returning(monthValue.toString)

      monthCypher.decrypt(encryptedValue) shouldBe monthValue
    }
  }
}
