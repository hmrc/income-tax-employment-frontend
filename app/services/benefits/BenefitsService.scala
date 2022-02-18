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

package services.benefits

import models.User
import models.benefits.BenefitsViewModel
import models.mongo.EmploymentUserData
import services.EmploymentSessionService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BenefitsService @Inject()(employmentSessionService: EmploymentSessionService,
                                implicit val ec: ExecutionContext) {

  def updateIsBenefitsReceived(user: User,
                               taxYear: Int,
                               employmentId: String,
                               originalEmploymentUserData: EmploymentUserData,
                               questionValue: Boolean): Future[Either[Unit, EmploymentUserData]] = {
    val updatedEmployment = if (questionValue) {
      val newBenefits = originalEmploymentUserData.employment.employmentBenefits match {
        case Some(benefits) => benefits.copy(isBenefitsReceived = true)
        case None => BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true)
      }
      originalEmploymentUserData.employment.copy(employmentBenefits = Some(newBenefits))
    } else {
      val customerData: Boolean = originalEmploymentUserData.employment.employmentBenefits.forall(_.isUsingCustomerData)
      val newBenefits = BenefitsViewModel.clear(customerData)
      originalEmploymentUserData.employment.copy(employmentBenefits = Some(newBenefits))
    }

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment)
  }
}
