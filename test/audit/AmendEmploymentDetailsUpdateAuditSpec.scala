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

import models.employment.{Deductions, EmploymentData, EmploymentSource, Pay, StudentLoans}
import models.employment.createUpdate.{CreateUpdateEmployment, CreateUpdateEmploymentData, CreateUpdateEmploymentRequest, CreateUpdatePay}
import play.api.libs.json.Json
import utils.UnitTestWithApp

class AmendEmploymentDetailsUpdateAuditSpec extends UnitTestWithApp{

  "toAmendAuditModel" should {
    "create the audit model when only updating employment data" in {

      val model: CreateUpdateEmploymentRequest = CreateUpdateEmploymentRequest(
        Some("id"),
        None,
        Some(
          CreateUpdateEmploymentData(
            pay = CreateUpdatePay(
              4354,
              564
            ),
            deductions = Some(
              Deductions(
                Some(StudentLoans(
                  Some(100),
                  Some(100)
                ))
              )
            )
          )
        ),
        Some("001")
      )

      val employmentSource1 = EmploymentSource(
        employmentId = "001",
        employerName = "Mishima Zaibatsu",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = None,
        cessationDate = Some("2020-03-11"),
        dateIgnored = None,
        submittedOn = Some("2020-01-04T05:01:01Z"),
        employmentData = Some(EmploymentData(
          submittedOn = "2020-02-12",
          employmentSequenceNumber = Some("123456789999"),
          companyDirector = Some(true),
          closeCompany = Some(false),
          directorshipCeasedDate = Some("2020-02-12"),
          occPen = Some(false),
          disguisedRemuneration = Some(false),
          pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
          Some(Deductions(
            studentLoans = Some(StudentLoans(
              uglDeductionAmount = Some(100.00),
              pglDeductionAmount = Some(100.00)
            ))
          ))
        )),
        None
      )

      model.toAmendAuditModel("id",2021,employmentSource1) shouldBe AmendEmploymentDetailsUpdateAudit(
        2021,"individual","AA123456A","1234567890",AuditEmploymentData("Mishima Zaibatsu",Some("223/AB12399"),"001",None,Some("2020-03-11"),Some(34234.15),Some(6782.92),Some("123456789999")),
        AuditEmploymentData("Mishima Zaibatsu",Some("223/AB12399"),"id",None,Some("2020-03-11"),Some(4354),Some(564),Some("123456789999")))
    }
  }

  "writes" when {
    "passed a AmendEmploymentDetailsUpdate model" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |	"taxYear": 2020,
             |	"userType": "individual",
             |	"nino": "AA12343AA",
             |	"mtditid": "mtditid",
             |	"priorEmploymentData": {
             |		"employerName": "Name",
             |		"employerRef": "123/12345",
             |		"employmentId": "12345",
             |		"startDate": "10-10-2000",
             |		"cessationDate": "10-10-2000",
             |		"taxablePayToDate": 55,
             |		"totalTaxToDate": 55,
             |		"payrollId": "1235"
             |	},
             |	"employmentData": {
             |		"employerName": "Name 2",
             |		"employerRef": "123/12345",
             |		"employmentId": "12345",
             |		"startDate": "10-12-2000",
             |		"cessationDate": "10-10-2000",
             |		"taxablePayToDate": 552,
             |		"totalTaxToDate": 552,
             |		"payrollId": "12356"
             |	}
             |}""".stripMargin)

        //scalastyle:off
        val auditModel = AmendEmploymentDetailsUpdateAudit(2020, "individual", "AA12343AA", "mtditid",
          priorEmploymentData = AuditEmploymentData(
            employerName = "Name",
            employerRef = Some("123/12345"),
            employmentId = "12345",
            startDate = Some("10-10-2000"),
            cessationDate = Some("10-10-2000"),
            taxablePayToDate = Some(55),
            totalTaxToDate = Some(55),
            payrollId = Some("1235")
          ),
          employmentData = AuditEmploymentData(
            employerName = "Name 2",
            employerRef = Some("123/12345"),
            employmentId = "12345",
            startDate = Some("10-12-2000"),
            cessationDate = Some("10-10-2000"),
            taxablePayToDate = Some(552),
            totalTaxToDate = Some(552),
            payrollId = Some("12356")
          )
        )

        Json.toJson(auditModel) shouldBe json
      }
    }
  }
}