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

package models.mongo

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.AesGcmAdCrypto

case class ExpensesUserData(sessionId: String,
                            mtdItId: String,
                            nino: String,
                            taxYear: Int,
                            isPriorSubmission: Boolean,
                            hasPriorExpenses: Boolean,
                            expensesCya: ExpensesCYAModel,
                            lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedExpensesUserData = EncryptedExpensesUserData(
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    isPriorSubmission = isPriorSubmission,
    hasPriorExpenses = hasPriorExpenses,
    expensesCya = expensesCya.encrypted,
    lastUpdated = lastUpdated
  )
}

object ExpensesUserData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val format: OFormat[ExpensesUserData] = Json.format[ExpensesUserData]
}

case class EncryptedExpensesUserData(sessionId: String,
                                     mtdItId: String,
                                     nino: String,
                                     taxYear: Int,
                                     isPriorSubmission: Boolean,
                                     hasPriorExpenses: Boolean,
                                     expensesCya: EncryptedExpensesCYAModel,
                                     lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): ExpensesUserData = ExpensesUserData(
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    isPriorSubmission = isPriorSubmission,
    hasPriorExpenses = hasPriorExpenses,
    expensesCya = expensesCya.decrypted,
    lastUpdated = lastUpdated
  )
}

object EncryptedExpensesUserData extends MongoJodaFormats {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val format: Format[EncryptedExpensesUserData] = Json.format[EncryptedExpensesUserData]
}
