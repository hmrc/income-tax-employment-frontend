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

package controllers.employment

import common.SessionValues
import models.employment.{AllEmploymentData, EmploymentData, EmploymentSource, Pay}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import utils.UnitTestWithApp
import views.html.employment.EmploymentDetailsView

import scala.concurrent.Future

class EmploymentDetailsControllerSpec extends UnitTestWithApp {

  object FullModel {
    val allData: AllEmploymentData = AllEmploymentData(
      hmrcEmploymentData = Seq(
        EmploymentSource(
          employmentId = "223/AB12399",
          employerName = "maggie",
          employerRef = Some("223/AB12399"),
          payrollId = Some("123456789999"),
          startDate = Some("2019-04-21"),
          cessationDate = Some("2020-03-11"),
          dateIgnored = Some("2020-04-04T01:01:01Z"),
          submittedOn = Some("2020-01-04T05:01:01Z"),
          employmentData = Some(EmploymentData(
            submittedOn = ("2020-02-12"),
            employmentSequenceNumber = Some("123456789999"),
            companyDirector = Some(true),
            closeCompany = Some(false),
            directorshipCeasedDate = Some("2020-02-12"),
            occPen = Some(false),
            disguisedRemuneration = Some(false),
            pay = Pay(34234.15, 6782.92, Some(67676), "CALENDAR MONTHLY", "2020-04-23", Some(32), Some(2))
          )),
          None
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  lazy val controller = new EmploymentDetailsController()(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[EmploymentDetailsView],
    mockAppConfig
  )

  val taxYear = mockAppConfig.defaultTaxYear
  val employmentId = "223/AB12399"

  ".show" should {

    "return a result when GetEmploymentDataModel is in Session" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear, employmentId = employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(
            Json.toJson(FullModel.allData)
          )))

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page no data in session" which {

      s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth{
        val result: Future[Result] = controller.show(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }
    }
  }

}
