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

package services.benefits.accommodation

import models.benefits.{AccommodationRelocationModel, BenefitsViewModel}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.{EmploymentUserDataRequest, User}
import org.joda.time.DateTimeZone
import repositories.EmploymentUserDataRepository
import utils.Clock

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccommodationService @Inject()(employmentUserDataRepository: EmploymentUserDataRepository,
                                     implicit val ec: ExecutionContext) {

  def updateSessionData(originalEmploymentUserData: EmploymentUserDataRequest[_],
                        employmentId: String,
                        taxYear: Int,
                        yesNo: Boolean)(implicit clock: Clock): Future[Either[Unit, EmploymentUserData]] = {
    implicit val user: User[_] = originalEmploymentUserData.user

    val originalCyaData: EmploymentCYAModel = originalEmploymentUserData.employmentUserData.employment
    val benefits: Option[BenefitsViewModel] = originalCyaData.employmentBenefits
    val accommodationRelocation: Option[AccommodationRelocationModel] = originalCyaData.employmentBenefits.flatMap(_.accommodationRelocationModel)

    val updatedCyaModel: EmploymentCYAModel = {
      accommodationRelocation match {
        case Some(accommodationRelocationModel) if yesNo =>
          originalCyaData.copy(employmentBenefits = benefits.map(_.copy(accommodationRelocationModel =
            Some(accommodationRelocationModel.copy(sectionQuestion = Some(true))))))
        case Some(_) =>
          originalCyaData.copy(employmentBenefits = benefits.map(_.copy(accommodationRelocationModel =
            Some(AccommodationRelocationModel.clear))))
        case _ =>
          originalCyaData.copy(employmentBenefits = benefits.map(_.copy(accommodationRelocationModel =
            Some(AccommodationRelocationModel(sectionQuestion = Some(yesNo))))))
      }
    }

    val updatedEmploymentUserData = EmploymentUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      employmentId,
      originalEmploymentUserData.employmentUserData.isPriorSubmission,
      hasPriorBenefits = originalEmploymentUserData.employmentUserData.hasPriorBenefits,
      updatedCyaModel,
      clock.now(DateTimeZone.UTC)
    )

    employmentUserDataRepository.createOrUpdate(updatedEmploymentUserData).map {
      case Right(_) => Right(updatedEmploymentUserData)
      case Left(_) => Left()
    }
  }
}
