/*
 * Copyright 2023 HM Revenue & Customs
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

package services.employment

import models.User
import models.mongo.EmploymentUserData
import models.otheremployment.session.{OtherEmploymentIncomeCYAModel, TaxableLumpSum}
import services.EmploymentSessionService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OtherEmploymentInfoService @Inject()(employmentSessionService: EmploymentSessionService,
                                           implicit val ec: ExecutionContext) {

  def updateLumpSums(user: User,
                   taxYear: Int,
                   employmentId: String,
                   originalEmploymentUserData: EmploymentUserData,
                   newLumSum: Seq[TaxableLumpSum]): Future[Either[Unit, EmploymentUserData]] = {

    val updatedEmployment: EmploymentUserData = originalEmploymentUserData.copy(
      employment = originalEmploymentUserData.employment.copy(
        otherEmploymentIncome = Some(OtherEmploymentIncomeCYAModel(newLumSum))))

    employmentSessionService.createOrUpdateEmploymentUserData(user, taxYear, employmentId, originalEmploymentUserData, updatedEmployment.employment)

  }
}
