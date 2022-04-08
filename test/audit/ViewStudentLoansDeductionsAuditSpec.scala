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
import play.api.libs.json.Json
import utils.UnitTestWithApp

class ViewStudentLoansDeductionsAuditSpec extends UnitTestWithApp {

  "writes" when {
    "passed a ViewStudentLoansDeductionsAudit model" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |  "taxYear": ${taxYearEOY-1},
             |  "userType": "individual",
             |  "nino": "PW106933A",
             |  "mtditid": "1234567890",
             |  "deductions": {
             |    "studentLoans": {
             |      "undergraduateLoanDeductionAmount": 13343.45,
             |      "postgraduateLoanDeductionAmount": 24242.56
             |    }
             |  }
             |}""".stripMargin)

        val auditModel = ViewStudentLoansDeductionsAudit(taxYearEOY-1, "individual", "PW106933A", "1234567890",
          Some(
            Deductions(
              Some(
                StudentLoans(uglDeductionAmount = Some(13343.45), pglDeductionAmount = Some(24242.56))
              )
            )
          )
        )

        Json.toJson(auditModel) shouldBe json
      }
    }
  }

}
