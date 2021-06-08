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

package audit

import models.employment.{EmploymentData, Pay}
import utils.UnitTestWithApp
import play.api.libs.json.Json

class ViewEmploymentDetailsSpec extends UnitTestWithApp{

  "writes" when {
    "passed a ViewEmploymentDetailsModel" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |"taxYear": 2020,
             |"affinityGroup": "Individual",
             |"nino":"AA12343AA",
             |"mtditid":"mtditid",
             |"employerName":"Dave",
             |"employerRef":"reference",
             |"employmentData":{
             |  "submittedOn":"2020-02-12",
             |  "employmentSequenceNumber":"123456789999",
             |  "companyDirector": true,
             |  "closeCompany":false,
             |  "directorshipCeasedDate":"2020-02-12",
             |  "occPen":false,
             |  "disguisedRemuneration":false,
             |  "pay": {
             |    "taxablePayToDate":34234.15,
             |    "totalTaxToDate":6782.92,
             |    "tipsAndOtherPayments":67676,
             |    "payFrequency":"CALENDAR MONTHLY",
             |    "paymentDate":"2020-04-23",
             |    "taxWeekNo":32,
             |    "taxMonthNo":2
             |  }
             |}
             |}""".stripMargin)

        val auditModel = ViewEmploymentDetailsAudit(2020, "Individual", "AA12343AA",
          "mtditid", "Dave", Some("reference"),
          Some(EmploymentData(
            submittedOn = ("2020-02-12"),
            employmentSequenceNumber = Some("123456789999"),
            companyDirector = Some(true),
            closeCompany = Some(false),
            directorshipCeasedDate = Some("2020-02-12"),
            occPen = Some(false),
            disguisedRemuneration = Some(false),
            pay = Pay(34234.15, 6782.92, Some(67676), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))
          )))
        Json.toJson(auditModel) shouldBe json
      }
    }
  }
}
