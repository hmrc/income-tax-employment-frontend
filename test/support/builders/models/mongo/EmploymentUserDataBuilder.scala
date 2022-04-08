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

package support.builders.models.mongo

import models.benefits.BenefitsViewModel
import models.employment.StudentLoansCYAModel
import models.mongo.{EmploymentDetails, EmploymentUserData}
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import utils.TaxYearHelper

object EmploymentUserDataBuilder extends TaxYearHelper {

  val anEmploymentUserData: EmploymentUserData = EmploymentUserData(
    sessionId = aUser.sessionId,
    mtdItId = aUser.mtditid,
    nino = aUser.nino,
    taxYear = taxYearEOY,
    employmentId = "employmentId",
    isPriorSubmission = true,
    hasPriorBenefits = true, hasPriorStudentLoans = true,
    employment = anEmploymentCYAModel
  )

  def anEmploymentUserDataWithBenefits(benefits: BenefitsViewModel,
                                       isPriorSubmission: Boolean = true,
                                       hasPriorBenefits: Boolean = true): EmploymentUserData = {
    anEmploymentUserData.copy(
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = hasPriorBenefits,
      employment = anEmploymentCYAModel.copy(employmentBenefits = Some(benefits))
    )
  }

  def anEmploymentUserDataWithDetails(employmentDetails: EmploymentDetails,
                                      isPriorSubmission: Boolean = true,
                                      hasPriorBenefits: Boolean = true): EmploymentUserData = {
    anEmploymentUserData.copy(
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = hasPriorBenefits,
      employment = anEmploymentCYAModel.copy(employmentDetails = employmentDetails)
    )
  }

  def anEmploymentUserDataWithStudentLoans(studentLoans: StudentLoansCYAModel,
                                           isPriorSubmission: Boolean = true,
                                           hasPriorBenefits: Boolean = true): EmploymentUserData = {
    anEmploymentUserData.copy(
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = hasPriorBenefits,
      employment = anEmploymentCYAModel.copy(studentLoans = Some(studentLoans))
    )
  }
}
