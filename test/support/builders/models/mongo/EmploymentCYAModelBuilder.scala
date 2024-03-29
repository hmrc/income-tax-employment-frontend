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

package support.builders.models.mongo

import models.benefits.BenefitsViewModel
import models.details.EmploymentDetails
import models.employment.StudentLoansCYAModel
import models.mongo.EmploymentCYAModel
import models.otheremployment.session.OtherEmploymentIncomeCYAModel
import support.builders.models.benefits.BenefitsViewModelBuilder.aBenefitsViewModel
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.employment.StudentLoansBuilder.aStudentLoans

object EmploymentCYAModelBuilder {

  def anEmploymentCYAModel(employmentDetails: EmploymentDetails = anEmploymentDetails,
                           employmentBenefits: Option[BenefitsViewModel] = Some(aBenefitsViewModel),
                           studentLoans: Option[StudentLoansCYAModel] = Some(aStudentLoans.toStudentLoansCYAModel),
                           otherEmploymentIncome: Option[OtherEmploymentIncomeCYAModel] = None): EmploymentCYAModel = EmploymentCYAModel(
    employmentDetails = employmentDetails,
    employmentBenefits = employmentBenefits,
    studentLoans = studentLoans,
    otherEmploymentIncome = otherEmploymentIncome
  )
}
