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

import controllers.employment.routes.CheckEmploymentDetailsController
import models.question.{Question, QuestionsJourney}
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.Call
import uk.gov.hmrc.crypto.EncryptedValue
import utils.{AesGcmAdCrypto, MongoJavaDateTimeFormats}
import java.time.{LocalDateTime, ZoneId}
case class EmploymentUserData(sessionId: String,
                              mtdItId: String,
                              nino: String,
                              taxYear: Int,
                              employmentId: String,
                              isPriorSubmission: Boolean,
                              hasPriorBenefits: Boolean,
                              hasPriorStudentLoans: Boolean,
                              employment: EmploymentCYAModel,
                              lastUpdated: LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedEmploymentUserData = EncryptedEmploymentUserData(
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    employmentId = employmentId,
    isPriorSubmission = isPriorSubmission,
    hasPriorBenefits = hasPriorBenefits,
    hasPriorStudentLoans = hasPriorStudentLoans,
    employment = employment.encrypted,
    lastUpdated = lastUpdated
  )
}

object EmploymentUserData extends MongoJavaDateTimeFormats {

  implicit val mongoJodaDateTimeFormats: Format[LocalDateTime] = localDateTimeFormat

  implicit val formats: Format[EmploymentUserData] = Json.format[EmploymentUserData]

  def journey(taxYear: Int, employmentId: String): QuestionsJourney[EmploymentUserData] = new QuestionsJourney[EmploymentUserData] {
    override def firstPage: Call = CheckEmploymentDetailsController.show(taxYear, employmentId)

    override def questions(m: EmploymentUserData): Set[Question] = {
      Set(
      )
    }
  }
}

case class EncryptedEmploymentUserData(sessionId: String,
                                       mtdItId: String,
                                       nino: String,
                                       taxYear: Int,
                                       employmentId: String,
                                       isPriorSubmission: Boolean,
                                       hasPriorBenefits: Boolean,
                                       hasPriorStudentLoans: Boolean,
                                       employment: EncryptedEmploymentCYAModel,
                                       lastUpdated: LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EmploymentUserData = EmploymentUserData(
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    employmentId = employmentId,
    isPriorSubmission = isPriorSubmission,
    hasPriorBenefits = hasPriorBenefits,
    hasPriorStudentLoans = hasPriorStudentLoans,
    employment = employment.decrypted,
    lastUpdated = lastUpdated
  )
}

object EncryptedEmploymentUserData extends MongoJavaDateTimeFormats {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val mongoJodaDateTimeFormats: Format[LocalDateTime] = localDateTimeFormat

  implicit val formats: Format[EncryptedEmploymentUserData] = Json.format[EncryptedEmploymentUserData]
}
