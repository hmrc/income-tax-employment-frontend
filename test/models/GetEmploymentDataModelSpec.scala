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

package models


import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.{JsObject, Json}
import utils.UnitTest


class GetEmploymentDataModelSpec  extends UnitTest{
  SharedMetricRegistries.clear()

  object FullModel {
    val payModel: PayModel = PayModel(34234.15, 6782.92, Some(67676), "CALENDAR MONTHLY", "2020-04-23", Some(32), Some(2))
    val employerModel: EmployerModel = EmployerModel(Some("223/AB12399"), "maggie")
    val employmentModel: EmploymentModel = EmploymentModel(Some("1002"), Some("123456789999"), Some(true), Some(false), Some("2020-02-12"),
      Some("2019-04-21"), Some("2020-03-11"), Some(false), Some(false), employerModel, payModel)
    val getEmploymentDataModel: GetEmploymentDataModel = GetEmploymentDataModel("2020-01-04T05:01:01Z", Some("CUSTOMER"),
      Some("2020-04-04T01:01:01Z"), Some("2020-04-04T01:01:01Z"), employmentModel)
  }

  object MinModel {
    val payModel: PayModel = PayModel(34234.15, 6782.92, None, "CALENDAR MONTHLY", "2020-04-23", None, None)
    val employerModel: EmployerModel = EmployerModel(None, "maggie")
    val employmentModel: EmploymentModel = EmploymentModel(None, None, None, None, None, None, None, None, None, employerModel, payModel)
    val getEmploymentDataModel: GetEmploymentDataModel = GetEmploymentDataModel("2020-01-04T05:01:01Z", None, None, None, employmentModel)
  }

  val jsonModel: JsObject = Json.obj(
    "source" -> "CUSTOMER",
    "submittedOn" -> "2020-01-04T05:01:01Z",
    "customerAdded" -> "2020-04-04T01:01:01Z",
    "dateIgnored" -> "2020-04-04T01:01:01Z",
    "employment" -> Json.obj(
      "employmentSequenceNumber" -> "1002",
      "payrollId" -> "123456789999",
      "companyDirector" -> true,
      "closeCompany" -> false,
      "directorshipCeasedDate" -> "2020-02-12",
      "startDate" -> "2019-04-21",
      "cessationDate" -> "2020-03-11",
      "occPen" -> false,
      "disguisedRemuneration" -> false,
      "employer" -> Json.obj(
        "employerRef" -> "223/AB12399",
        "employerName" -> "maggie"
      ),
      "pay" -> Json.obj(
        "taxablePayToDate" -> 34234.15,
        "totalTaxToDate" -> 6782.92,
        "tipsAndOtherPayments" -> 67676,
        "payFrequency" -> "CALENDAR MONTHLY",
        "paymentDate" -> "2020-04-23",
        "taxWeekNo" -> 32,
        "taxMonthNo" -> 2
      )
    )
  )

  val jsonModelWithOnlyRequired: JsObject = Json.obj(
    "submittedOn" -> "2020-01-04T05:01:01Z",
    "employment" -> Json.obj(
      "employer" -> Json.obj(
        "employerName" -> "maggie"
      ),
      "pay" -> Json.obj(
        "taxablePayToDate" -> 34234.15,
        "totalTaxToDate" -> 6782.92,
        "payFrequency" -> "CALENDAR MONTHLY",
        "paymentDate" -> "2020-04-23"
      )
    )
  )

  "GetEmploymentDataModel with all values" should {

    "parse to Json" in {
      Json.toJson(FullModel.getEmploymentDataModel) mustBe jsonModel
    }

    "parse from Json" in {
      jsonModel.as[GetEmploymentDataModel]
    }
  }

  "GetEmploymentDataModel with only the required values" should {

    "parse to Json" in {
      Json.toJson(MinModel.getEmploymentDataModel) mustBe jsonModelWithOnlyRequired
    }

    "parse from Json" in {
      jsonModelWithOnlyRequired.as[GetEmploymentDataModel]
    }
  }

}
