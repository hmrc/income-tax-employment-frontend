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

package services

import controllers.employment.routes._
import models.mongo.{EmploymentCYAModel, EmploymentDetails}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect

object RedirectService {

  def employmentDetailsRedirect(cya: EmploymentCYAModel, taxYear: Int, employmentId: String, isPriorSubmission: Boolean): Result ={
    Redirect(if(isPriorSubmission){
      CheckEmploymentDetailsController.show(taxYear, employmentId)
    } else {
      cya match {
        case EmploymentCYAModel(EmploymentDetails(_,employerRef@None,_,_,_,_,_,_,_,_,_,_),_) => PayeRefController.show(taxYear,employmentId)
        case EmploymentCYAModel(EmploymentDetails(_,_,startDate@None,_,_,_,_,_,_,_,_,_),_) => EmployerStartDateController.show(taxYear,employmentId)
        case EmploymentCYAModel(EmploymentDetails(_,_,_,_,cessationDateQuestion@None,_,_,_,_,_,_,_),_) =>
          StillWorkingForEmployerController.show(taxYear, employmentId)
        case EmploymentCYAModel(EmploymentDetails(_,_,_,_,Some(false),cessationDate@None,_,_,_,_,_,_),_) =>
          EmployerLeaveDateController.show(taxYear, employmentId)
        case EmploymentCYAModel(EmploymentDetails(_,_,_,_,_,_,_,_,_,taxablePayToDate@None,_,_),_) => EmployerPayAmountController.show(taxYear,employmentId)
        case EmploymentCYAModel(EmploymentDetails(_,_,_,_,_,_,_,_,_,_,totalTaxToDate@None,_),_) => EmploymentTaxController.show(taxYear,employmentId)
        case EmploymentCYAModel(EmploymentDetails(_,_,_,payrollId@None,_,_,_,_,_,_,_,_),_) =>
          CheckEmploymentDetailsController.show(taxYear, employmentId) //TODO Payroll page
        case _ => CheckEmploymentDetailsController.show(taxYear, employmentId)
      }
    })
  }
}