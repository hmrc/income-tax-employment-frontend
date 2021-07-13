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

package models.mongo

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{OFormat, OWrites, Reads, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class EmploymentUserData(sessionId: String,
                              mtdItId: String,
                              nino: String,
                              taxYear: Int,
                              employmentId: String,
                              isPriorSubmission: Boolean,
                              employment: EmploymentCYAModel,
                              lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object EmploymentUserData {

  implicit lazy val formats: OFormat[EmploymentUserData] = OFormat(reads, writes)

  implicit lazy val reads: Reads[EmploymentUserData] = {

    import play.api.libs.functional.syntax._
    (
      (__ \ "sessionId").read[String] and
        (__ \ "mtdItId").read[String] and
        (__ \ "nino").read[String] and
        (__ \ "taxYear").read[Int] and
        (__ \ "employmentId").read[String] and
        (__ \ "isPriorSubmission").read[Boolean] and
        (__ \ "employment").read[EmploymentCYAModel] and
        (__ \ "lastUpdated").read(MongoJodaFormats.dateTimeReads)
      ) (EmploymentUserData.apply _)
  }

  implicit lazy val writes: OWrites[EmploymentUserData] = {

    import play.api.libs.functional.syntax._
    (
      (__ \ "sessionId").write[String] and
        (__ \ "mtdItId").write[String] and
        (__ \ "nino").write[String] and
        (__ \ "taxYear").write[Int] and
        (__ \ "employmentId").write[String] and
        (__ \ "isPriorSubmission").write[Boolean] and
        (__ \ "employment").write[EmploymentCYAModel] and
        (__ \ "lastUpdated").write(MongoJodaFormats.dateTimeWrites)
      ) (unlift(EmploymentUserData.unapply))
  }
}
