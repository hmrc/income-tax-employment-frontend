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

import models.employment.{Deductions, StudentLoans}
import play.api.libs.json.{JsValue, Json}
import utils.UnitTest

class AmendStudentLoansDeductionUpdateAuditSpec extends UnitTest {

  "writes" when {
    "passed a AmendStudentLoansDeductionsUpdate model" should {
      "produce valid json" in {

        val auditModel: AmendStudentLoansDeductionsUpdateAudit = AmendStudentLoansDeductionsUpdateAudit(
          taxYear = taxYearEOY,
          userType = "individual",
          nino = "AA123456A",
          mtditid = "1234567890",
          priorStudentLoanDeductionsData = Deductions(
            studentLoans = Some(StudentLoans(
              uglDeductionAmount = Some(12345.67),
              pglDeductionAmount = Some(12345.67)
            ))),
          studentLoanDeductionsData = Deductions(
            studentLoans = Some(StudentLoans(
              uglDeductionAmount = Some(2233.44),
              pglDeductionAmount = Some(2233.44)
            ))
          )
        )

        val validJson: JsValue = Json.parse(
          s"""{
            |"taxYear": $taxYearEOY,
            |"userType": "individual",
            |"nino": "AA123456A",
            |"mtditid": "1234567890",
            |"priorStudentLoanDeductionsData": {
            |   "studentLoans": {
            |       "undergraduateLoanDeductionAmount": 12345.67,
            |       "postgraduateLoanDeductionAmount": 12345.67
            |       }
            |   },
            |"studentLoanDeductionsData": {
            |   "studentLoans": {
            |      "undergraduateLoanDeductionAmount": 2233.44,
            |      "postgraduateLoanDeductionAmount": 2233.44
            |      }
            |  }
            |}""".stripMargin

        )

        Json.toJson(auditModel) shouldBe validJson
      }
    }
  }

}
