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

trait Decryptable[A] {
  def decrypt(encryptedValue: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): A
}

object DecryptorInstances {
  implicit val booleanDecryptor: Decryptable[Boolean] = new Decryptable[Boolean] {
    override def decrypt(encryptedValue: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): Boolean =
      secureGCMCipher.decrypt[Boolean](encryptedValue.value, encryptedValue.nonce)
  }

  implicit val stringDecryptor: Decryptable[String] = new Decryptable[String] {
    override def decrypt(encryptedValue: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): String =
      secureGCMCipher.decrypt[String](encryptedValue.value, encryptedValue.nonce)
  }

  implicit val bigDecimalDecryptor: Decryptable[BigDecimal] = new Decryptable[BigDecimal] {
    override def decrypt(encryptedValue: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): BigDecimal =
      secureGCMCipher.decrypt[BigDecimal](encryptedValue.value, encryptedValue.nonce)
  }
}

object DecryptableSyntax {
  implicit class DecryptableOps(encryptedValue: EncryptedValue) {
    def decrypted[A](implicit d: Decryptable[A], secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): A = d.decrypt(encryptedValue)
  }
}
