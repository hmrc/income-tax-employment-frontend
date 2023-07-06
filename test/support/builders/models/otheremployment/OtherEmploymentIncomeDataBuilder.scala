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

package support.builders.models.otheremployment

import models.otheremployment.api.{LumpSum, OtherEmploymentIncome, TaxableLumpSumsAndCertainIncome}
import support.TaxYearUtils.taxYearEOY
import support.builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource

import java.time.{LocalDateTime, ZoneOffset}

object OtherEmploymentIncomeDataBuilder {

  val anOtherEmploymentIncome: OtherEmploymentIncome = OtherEmploymentIncome(
    submittedOn = Some(LocalDateTime.of(taxYearEOY -1 , 2, 12, 10, 0, 0).toInstant(ZoneOffset.UTC)),
    lumpSums = Some(Set(LumpSum(
      employerName = aHmrcEmploymentSource.employerName,
      employerRef = aHmrcEmploymentSource.employerRef.get,
      taxableLumpSumsAndCertainIncome = Some(TaxableLumpSumsAndCertainIncome(
        amount = 123
      )),
      benefitFromEmployerFinancedRetirementScheme = None,
      redundancyCompensationPaymentsOverExemption = None,
      redundancyCompensationPaymentsUnderExemption = None
    )))
  )
}
