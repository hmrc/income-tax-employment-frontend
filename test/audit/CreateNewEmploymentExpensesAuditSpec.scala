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

import play.api.libs.json.Json
import utils.UnitTestWithApp

class CreateNewEmploymentExpensesAuditSpec extends UnitTestWithApp{

  "writes" when {
    "passed a CreateNewEmploymentExpenses model" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |	"taxYear": 2020,
             |	"userType": "individual",
             |	"nino": "AA12343AA",
             |	"mtditid": "mtditid",
             |	"employmentExpensesData": {
             |		"jobExpenses": 150,
             |		"flatRateJobExpenses": 200,
             |		"professionalSubscriptions": 250,
             |		"otherAndCapitalAllowances": 350
             |	}
             |}""".stripMargin)

        //scalastyle:off magic.number
        val auditModel = CreateNewEmploymentExpensesAudit(2020, "individual", "AA12343AA", "mtditid",
          employmentExpensesData = AuditNewEmploymentExpensesData(
            jobExpenses = Some(150.00),
            flatRateJobExpenses = Some(200.00),
            professionalSubscriptions = Some(250.00),
            otherAndCapitalAllowances = Some(350.00),
          )
        )
        //scalastyle:on magic.number

        Json.toJson(auditModel) shouldBe json
      }
    }
  }
}