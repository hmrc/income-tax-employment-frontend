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
import models.benefits._
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.{JsValue, Json}
import utils.UnitTest

class EmploymentBenefitsModelSpec extends UnitTest {
  SharedMetricRegistries.clear()

  val amount: BigDecimal = 100

  val model: Benefits = Benefits(
    Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount),
    Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount),
    Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount)
  )

  val benefitsViewModel: BenefitsViewModel = BenefitsViewModel(
    Some(CarVanFuelModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount), Some(true),
      Some(amount), Some(true), Some(amount))),
    Some(AccommodationRelocationModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount))),
    Some(TravelEntertainmentModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount))),
    Some(UtilitiesAndServicesModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount))),
    Some(MedicalChildcareEducationModel(Some(true), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount), Some(true), Some(amount))),
    Some(IncomeTaxAndCostsModel(Some(true), Some(true), Some(amount), Some(true), Some(amount))),
    Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount), Some(amount),
    Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(true),
    None, isUsingCustomerData = true, isBenefitsReceived = true)

  val jsonModel: JsValue = Json.obj(
    "accommodation" -> 100,
    "assets" -> 100,
    "assetTransfer" -> 100,
    "beneficialLoan" -> 100,
    "car" -> 100,
    "carFuel" -> 100,
    "educationalServices" -> 100,
    "entertaining" -> 100,
    "expenses" -> 100,
    "medicalInsurance" -> 100,
    "telephone" -> 100,
    "service" -> 100,
    "taxableExpenses" -> 100,
    "van" -> 100,
    "vanFuel" -> 100,
    "mileage" -> 100,
    "nonQualifyingRelocationExpenses" -> 100,
    "nurseryPlaces" -> 100,
    "otherItems" -> 100,
    "paymentsOnEmployeesBehalf" -> 100,
    "personalIncidentalExpenses" -> 100,
    "qualifyingRelocationExpenses" -> 100,
    "employerProvidedProfessionalSubscriptions" -> 100,
    "employerProvidedServices" -> 100,
    "incomeTaxPaidByDirector" -> 100,
    "travelAndSubsistence" -> 100,
    "vouchersAndCreditCards" -> 100,
    "nonCash" -> 100
  )

  "EmploymentBenefits" should {

    "parse to Json" in {
      Json.toJson(model) mustBe jsonModel
    }

    "parse from Json" in {
      jsonModel.as[Benefits] mustBe model
    }

    "parse from a benefits model to a benefitsViewModel" in {
      val modelToBenefitsModel: BenefitsViewModel = model.toBenefitsViewModel(isUsingCustomerData = true)
      modelToBenefitsModel mustBe benefitsViewModel
    }

    "parse from a benefitsView model to a benefits model" in {
      val modelToBenefitsModel: Benefits = benefitsViewModel.toBenefits
      modelToBenefitsModel mustBe model
    }
  }
}