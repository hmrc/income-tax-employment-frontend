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

package models.otheremployment.pages

import models.UserSessionDataRequest

case class TaxableLumpSumAmountPage(individualOrClient: String,
                                    employerName: String,
                                    employerID: String,
                                    taxYear: Int,
                                    index: Option[Int],
                                    amount: Option[BigDecimal])

object TaxableLumpSumAmountPage {

  def apply(request: UserSessionDataRequest[_], index: Option[Int]): TaxableLumpSumAmountPage = {
    val validIndex = if (index.flatMap(i =>
      request.employmentUserData.employment.otherEmploymentIncome.flatMap(oEI =>
        oEI.taxableLumpSums.lift(i))).isDefined) {
      index
    } else {
      None
    }


    TaxableLumpSumAmountPage(
      if (request.user.isAgent) "agent" else "individual",
      request.employmentUserData.employment.employmentDetails.employerName,
      request.employmentUserData.employmentId,
      request.employmentUserData.taxYear,
      validIndex,
      index.flatMap(i =>
        request.employmentUserData.employment.otherEmploymentIncome.flatMap(oEI =>
          oEI.taxableLumpSums.lift(i).map(tLS =>
            tLS.amount)))
    )
  }
}