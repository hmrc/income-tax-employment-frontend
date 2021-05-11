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
import models.employment.{AllEmploymentData, Benefits, EmploymentBenefits, EmploymentData, EmploymentSource, Pay}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import utils.UnitTestWithApp
import views.html.employment.CheckYourBenefitsView

import scala.concurrent.Future

class CheckYourBenefitsControllerSpec extends UnitTestWithApp {

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
            submittedOn = "2020-02-12",
            employmentSequenceNumber = Some("123456789999"),
            companyDirector = Some(true),
            closeCompany = Some(false),
            directorshipCeasedDate = Some("2020-02-12"),
            occPen = Some(false),
            disguisedRemuneration = Some(false),
            pay = Pay(34234.15, 6782.92, Some(67676), "CALENDAR MONTHLY", "2020-04-23", Some(32), Some(2))
          )),
          Some(EmploymentBenefits(
            submittedOn = "2020-02-12",
            benefits = Some(allBenefits)
          ))
        )
      ),
      hmrcExpenses = None,
      customerEmploymentData = Seq(),
      customerExpenses = None
    )
  }

  val allBenefits: Benefits = Benefits(
    car = Some(1.23),
    carFuel = Some(2.00),
    van = Some(3.00),
    vanFuel = Some(4.00),
    mileage = Some(5.00),
    accommodation = Some(6.00),
    qualifyingRelocationExpenses = Some(7.00),
    nonQualifyingRelocationExpenses = Some(8.00),
    travelAndSubsistence = Some(9.00),
    personalIncidentalExpenses = Some(10.00),
    entertaining = Some(11.00),
    telephone = Some(12.00),
    employerProvidedServices = Some(13.00),
    employerProvidedProfessionalSubscriptions = Some(14.00),
    service = Some(15.00),
    medicalInsurance = Some(16.00),
    nurseryPlaces = Some(17.00),
    beneficialLoan = Some(18.00),
    educationalServices = Some(19.00),
    incomeTaxPaidByDirector = Some(20.00),
    paymentsOnEmployeesBehalf = Some(21.00),
    expenses = Some(22.00),
    taxableExpenses = Some(23.00),
    vouchersAndCreditCards = Some(24.00),
    nonCash = Some(25.00),
    otherItems = Some(26.00),
    assets = Some(27.00),
    assetTransfer = Some(280000.00)
  )

  lazy val controller = new CheckYourBenefitsController(
    authorisedAction,
    mockMessagesControllerComponents,
    mockAppConfig,
    app.injector.instanceOf[CheckYourBenefitsView]
  )

  val taxYear: Int = mockAppConfig.defaultTaxYear
  val employmentId = "223/AB12399"

  ".show" should {

    "return a result when all data is in Session" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.EMPLOYMENT_PRIOR_SUB -> Json.prettyPrint(Json.toJson(FullModel.allData))
        ))

        status(result) shouldBe OK
      }
    }

    "redirect the User to the Overview page no data in session" which {

      s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth{
        val result: Future[Result] = controller.show(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe "/overview"
      }
    }
  }

}
