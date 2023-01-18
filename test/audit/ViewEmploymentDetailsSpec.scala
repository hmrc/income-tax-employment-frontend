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

package audit

import models.employment.EmploymentDetailsViewModel
import play.api.libs.json.Json
import support.{TaxYearProvider, UnitTest}

class ViewEmploymentDetailsSpec extends UnitTest with TaxYearProvider {

  "writes" when {
    "passed a ViewEmploymentDetailsModel" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |	"taxYear": ${taxYearEOY - 1},
             |	"userType": "individual",
             |	"nino": "AA12343AA",
             |	"mtditid": "mtditid",
             |	"employmentData": {
             |		"employerName": "Dave",
             |		"employerRef": "reference",
             |		"payrollId": "12345678",
             |		"employmentId": "id",
             |		"startDate": "${taxYearEOY - 1}-02-12",
             |		"didYouLeaveQuestion": true,
             |		"cessationDate": "${taxYearEOY - 1}-02-12",
             |		"taxablePayToDate": 34234.15,
             |		"totalTaxToDate": 6782.92,
             |		"isUsingCustomerData": false
             |	}
             |}""".stripMargin)

        //scalastyle:off
        val auditModel = ViewEmploymentDetailsAudit(taxYearEOY - 1, "individual", "AA12343AA",
          "mtditid",
          EmploymentDetailsViewModel(
            employerName = "Dave",
            employerRef = Some("reference"),
            payrollId = Some("12345678"),
            employmentId = "id",
            startDate = Some(s"${taxYearEOY - 1}-02-12"),
            didYouLeaveQuestion = Some(true),
            cessationDate = Some(s"${taxYearEOY - 1}-02-12"),
            taxablePayToDate = Some(34234.15),
            totalTaxToDate = Some(6782.92),
            isUsingCustomerData = false
          ))
        Json.toJson(auditModel) shouldBe json
      }
    }
  }
}
