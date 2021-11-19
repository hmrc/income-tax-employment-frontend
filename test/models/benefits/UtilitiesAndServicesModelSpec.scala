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

package models.benefits

import play.api.mvc.Call
import utils.UnitTest

class UtilitiesAndServicesModelSpec extends UnitTest {

  private val taxYear = 2022
  private val model = UtilitiesAndServicesModel(
    utilitiesAndServicesQuestion = Some(true),
    telephoneQuestion = Some(true),
    telephone = Some(55.55),
    employerProvidedServicesQuestion = Some(true),
    employerProvidedServices = Some(55.55),
    employerProvidedProfessionalSubscriptionsQuestion = Some(true),
    employerProvidedProfessionalSubscriptions = Some(55.55),
    serviceQuestion = Some(true),
    service = Some(55.55)
  )

  private def result(url: String): Option[Call] = Some(Call("GET", url))

  "isFinished" should {
    "return utilities and services yes no page" in {
      model.copy(utilitiesAndServicesQuestion = None).isFinished(taxYear, "id") shouldBe
        result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/utility-general-service?employmentId=id")
    }
    "return none when section is finished" in {
      model.copy(utilitiesAndServicesQuestion = Some(false)).isFinished(taxYear, "employmentId") shouldBe None
      model.isFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "telephoneSectionFinished" should {
    "return telephone yes no page" in {
      model.copy(telephoneQuestion = None).telephoneSectionFinished(taxYear, "id") shouldBe
        result("/update-and-submit-income-tax-return/employment-income/2022/benefits/telephone?employmentId=id")
    }

    "return telephone amount page" in {
      model.copy(telephone = None).telephoneSectionFinished(taxYear, "id") shouldBe
        result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/check-employment-benefits?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(telephoneQuestion = Some(false)).telephoneSectionFinished(taxYear, "employmentId") shouldBe None
      model.telephoneSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "employerProvidedServicesSectionFinished" should {

    "return employerProvidedServices yes no page" in {
      model.copy(employerProvidedServicesQuestion = None).employerProvidedServicesSectionFinished(taxYear, "id") shouldBe
        result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/employer-provided-services?employmentId=id")
    }

    "return employerProvidedServices amount page" in {
      model.copy(employerProvidedServices = None).employerProvidedServicesSectionFinished(taxYear, "id") shouldBe
        result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/employer-provided-services-amount?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(employerProvidedServicesQuestion = Some(false)).employerProvidedServicesSectionFinished(taxYear, "employmentId") shouldBe None
      model.employerProvidedServicesSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "employerProvidedProfessionalSubscriptionsSectionFinished" should {

    "return employerProvidedProfessionalSubscriptions yes no page" in {
      model.copy(employerProvidedProfessionalSubscriptionsQuestion = None).employerProvidedProfessionalSubscriptionsSectionFinished(taxYear, "id") shouldBe
        result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/professional-fees-or-subscriptions?employmentId=id")
    }

    "return employerProvidedProfessionalSubscriptions amount page" in {
      model.copy(employerProvidedProfessionalSubscriptions = None).employerProvidedProfessionalSubscriptionsSectionFinished(taxYear, "id") shouldBe
        result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/professional-fees-or-subscriptions-amount?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false)).employerProvidedProfessionalSubscriptionsSectionFinished(taxYear, "employmentId") shouldBe None
      model.employerProvidedProfessionalSubscriptionsSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "serviceSectionFinished" should {
    "return service yes no page" in {
      model.copy(serviceQuestion = None).serviceSectionFinished(taxYear, "id") shouldBe
        result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/benefits/other-services?employmentId=id")
    }

    "return service amount page" in {
      model.copy(service = None).serviceSectionFinished(taxYear, "id") shouldBe
        result(s"/update-and-submit-income-tax-return/employment-income/$taxYear/check-employment-benefits?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(serviceQuestion = Some(false)).serviceSectionFinished(taxYear, "employmentId") shouldBe None
      model.serviceSectionFinished(taxYear, "employmentId") shouldBe None
    }
  }

  "clear" should {
    "clear the model" in {
      UtilitiesAndServicesModel.clear shouldBe UtilitiesAndServicesModel(utilitiesAndServicesQuestion = Some(false))
    }
  }
}
