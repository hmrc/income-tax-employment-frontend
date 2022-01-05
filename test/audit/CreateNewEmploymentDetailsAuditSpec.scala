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

package audit

import play.api.libs.json.Json
import utils.UnitTestWithApp

class CreateNewEmploymentDetailsAuditSpec extends UnitTestWithApp{

  "writes" when {
    "passed a CreateNewEmploymentDetails model" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |	"taxYear": 2020,
             |	"userType": "individual",
             |	"nino": "AA12343AA",
             |	"mtditid": "mtditid",
             |	"employmentData": {
             |		"employerName": "Name",
             |		"employerRef": "123/12345",
             |		"startDate": "10-10-2000",
             |		"cessationDate": "10-10-2000",
             |		"taxablePayToDate": 55,
             |		"totalTaxToDate": 55,
             |		"payrollId": "1235"
             |	},
             |	"existingEmployments": [{
             |		"employerName": "Wow Name",
             |		"employerRef": "123/12345"
             |	}, {
             |		"employerName": "Wow Name 2",
             |		"employerRef": "222/12345"
             |	}]
             |}""".stripMargin)

        //scalastyle:off
        val auditModel = CreateNewEmploymentDetailsAudit(2020, "individual", "AA12343AA", "mtditid",
          employmentData = AuditNewEmploymentData(
            employerName = Some("Name"),
            employerRef = Some("123/12345"),
            startDate = Some("10-10-2000"),
            cessationDate = Some("10-10-2000"),
            taxablePayToDate = Some(55),
            totalTaxToDate = Some(55),
            payrollId = Some("1235")
          ),
          existingEmployments = Seq(
            PriorEmploymentAuditInfo(
              "Wow Name", Some("123/12345")
            ),PriorEmploymentAuditInfo(
              "Wow Name 2", Some("222/12345")
            )
          )
        )

        Json.toJson(auditModel) shouldBe json
      }
    }
  }
}
