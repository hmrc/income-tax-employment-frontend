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

import models.benefits.UtilitiesAndServicesModel
import play.api.mvc.Call
import utils.UnitTest

class UtilitiesAndServicesModelSpec extends UnitTest {

  val model = UtilitiesAndServicesModel(
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

  def result(url: String): Option[Call] = Some(Call("GET",url))

  "isFinished" should {
    "return utilities and services yes no page" in {
      model.copy(utilitiesAndServicesQuestion = None).isFinished(2022, "id") shouldBe result("/income-through-software/return/employment-income/2022/benefits/accommodation-relocation?employmentId=id")
    }
    "return none when section is finished" in {
      model.copy(utilitiesAndServicesQuestion = Some(false)).isFinished(2022, "employmentId") shouldBe None
      model.isFinished(2022, "employmentId") shouldBe None
    }
  }

  "telephoneSectionFinished" should {
    "return telephone yes no page" in {
      model.copy(telephoneQuestion = None).telephoneSectionFinished(2022, "id") shouldBe result("/income-through-software/return/employment-income/2022/benefits/accommodation-relocation?employmentId=id")
    }

    "return telephone amount page" in {
      model.copy(telephone = None).telephoneSectionFinished(2022, "id") shouldBe result("/income-through-software/return/employment-income/2022/benefits/accommodation-relocation?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(telephoneQuestion = Some(false)).telephoneSectionFinished(2022, "employmentId") shouldBe None
      model.telephoneSectionFinished(2022, "employmentId") shouldBe None
    }
  }

  "employerProvidedServicesSectionFinished" should {

    "return employerProvidedServices yes no page" in {
      model.copy(employerProvidedServicesQuestion = None).employerProvidedServicesSectionFinished(2022, "id") shouldBe result("/income-through-software/return/employment-income/2022/benefits/accommodation-relocation?employmentId=id")
    }

    "return employerProvidedServices amount page" in {
      model.copy(employerProvidedServices = None).employerProvidedServicesSectionFinished(2022, "id") shouldBe result("/income-through-software/return/employment-income/2022/benefits/accommodation-relocation?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(employerProvidedServicesQuestion = Some(false)).employerProvidedServicesSectionFinished(2022, "employmentId") shouldBe None
      model.employerProvidedServicesSectionFinished(2022, "employmentId") shouldBe None
    }
  }

  "employerProvidedProfessionalSubscriptionsSectionFinished" should {

    "return employerProvidedProfessionalSubscriptions yes no page" in {
      model.copy(employerProvidedProfessionalSubscriptionsQuestion = None).employerProvidedProfessionalSubscriptionsSectionFinished(2022, "id") shouldBe result("/income-through-software/return/employment-income/2022/benefits/accommodation-relocation?employmentId=id")
    }

    "return employerProvidedProfessionalSubscriptions amount page" in {
      model.copy(employerProvidedProfessionalSubscriptions = None).employerProvidedProfessionalSubscriptionsSectionFinished(2022, "id") shouldBe result("/income-through-software/return/employment-income/2022/benefits/accommodation-relocation?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(employerProvidedProfessionalSubscriptionsQuestion = Some(false)).employerProvidedProfessionalSubscriptionsSectionFinished(2022, "employmentId") shouldBe None
      model.employerProvidedProfessionalSubscriptionsSectionFinished(2022, "employmentId") shouldBe None
    }
  }

  "serviceSectionFinished" should {
    "return service yes no page" in {
      model.copy(serviceQuestion = None).serviceSectionFinished(2022, "id") shouldBe result("/income-through-software/return/employment-income/2022/benefits/accommodation-relocation?employmentId=id")
    }

    "return service amount page" in {
      model.copy(service = None).serviceSectionFinished(2022, "id") shouldBe result("/income-through-software/return/employment-income/2022/benefits/accommodation-relocation?employmentId=id")
    }

    "return none when section is finished" in {
      model.copy(serviceQuestion = Some(false)).serviceSectionFinished(2022, "employmentId") shouldBe None
      model.serviceSectionFinished(2022, "employmentId") shouldBe None
    }
  }

  "clear" should {
    "clear the model" in {
      UtilitiesAndServicesModel.clear shouldBe UtilitiesAndServicesModel(utilitiesAndServicesQuestion = Some(false))
    }
  }
}
