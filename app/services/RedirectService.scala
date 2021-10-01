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

import controllers.benefits.routes._
import controllers.employment.routes.{CheckYourBenefitsController, _}
import controllers.employment.CheckYourBenefitsController
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.mvc.{Call, Result}
import play.api.mvc.Results.{Ok, Redirect}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

object RedirectService {

  def mileageRedirect(cya: EmploymentCYAModel, taxYear: Int, employmentId: String,
                      isPriorSubmission: Boolean): Result = {

    val mileageQ = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion))
    val mileage = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage))

    val redirect: Result = (mileageQ, mileage, isPriorSubmission) match {
      case (None, _, _) => //TODO GO TO MILEAGE YES / NO QUESTION
        CheckYourBenefitsController.show(taxYear, employmentId)

        Ok("Mileage yes no question")

      case (Some(true), None, _) => Redirect(MileageBenefitAmountController.show(taxYear, employmentId))
      case (Some(false), _, true) => Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
      case (Some(false), _, false) =>
        //TODO GO TO Accommodation or relocation QUESTION
        CheckYourBenefitsController.show(taxYear, employmentId)

        Ok("Accommodation or relocation QUESTION")

      case (Some(true), Some(_), false) =>
        //TODO GO TO Accommodation or relocation QUESTION
        CheckYourBenefitsController.show(taxYear, employmentId)

        Ok("Accommodation or relocation QUESTION")

      case (Some(true), Some(_), true) => Redirect(CheckYourBenefitsController.show(taxYear, employmentId))
    }

    redirect
  }

  def x(employmentCYAModel: EmploymentCYAModel, taxYear: Int, employmentId: String): Option[Result] ={

    if(employmentCYAModel.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)).contains(true)){
      None
    } else {
      //TODO GO TO MILEAGE YES / NO QUESTION
      Some(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
    }
  }

  def xx[A](page: A): Unit ={

    //pass in page - get list of pages that should be answered yes to

    page match {
      case MileageBenefitAmountController =>


    }

    ???
  }

  def test(employmentCYAModel: EmploymentCYAModel, taxYear: Int, employmentId: String, isPriorSubmission: Boolean)
          (f: List[EmploymentCYAModel => Option[Result]]): Unit ={

    f.map{
      f =>
        f(employmentCYAModel)
    }

    val mileageQ = employmentCYAModel.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion))

    val x = CheckYourBenefitsController


    //no to benefits => to benefits yes no
    //no to car van fuel => to car van fuel yes no


  }

  trait EmploymentType
  case object EmploymentDetails extends EmploymentType
  case object EmploymentBenefits extends EmploymentType

  case class ConditionalRedirect(condition: Boolean, isPriorSubmission: Boolean, redirect: Call)

  def redirectBasedOnCurrentAnswers(data: Option[EmploymentUserData], employmentType: EmploymentType /*, nextPage: Result*/)
                                   (cyaConditions: EmploymentCYAModel => Seq[ConditionalRedirect])
                                   (implicit taxYear: Int, employmentId: String): Option[Result] ={

    data match {
      case Some(cya) =>

        val possibleRedirects = cyaConditions(cya.employment)

        possibleRedirects.collectFirst {
          case ConditionalRedirect(condition, isPriorSubmission, result) if condition && isPriorSubmission == cya.isPriorSubmission => Redirect(result)
        }

      case None =>
        employmentType match {
          case EmploymentBenefits => Some(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
          case EmploymentDetails => Some(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
        }
    }
  }

  def employmentDetailsRedirect(cya: EmploymentCYAModel, taxYear: Int, employmentId: String,
                                isPriorSubmission: Boolean, isStandaloneQuestion: Boolean = true): Result ={
    Redirect(if(isPriorSubmission && isStandaloneQuestion){
      CheckEmploymentDetailsController.show(taxYear, employmentId)
    } else {
      questionRouting(cya,taxYear,employmentId)
    })
  }

  def questionRouting(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Call ={
    cya match {
      case EmploymentCYAModel(EmploymentDetails(_,employerRef@None,_,_,_,_,_,_,_,_,_,_),_) => PayeRefController.show(taxYear,employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,startDate@None,_,_,_,_,_,_,_,_,_),_) => EmployerStartDateController.show(taxYear,employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,_,cessationDateQuestion@None,_,_,_,_,_,_,_),_) =>
        StillWorkingForEmployerController.show(taxYear, employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,_,Some(false),cessationDate@None,_,_,_,_,_,_),_) =>
        EmployerLeaveDateController.show(taxYear, employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,payrollId@None,_,_,_,_,_,_,_,_),_) =>
        EmployerPayrollIdController.show(taxYear,employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,_,_,_,_,_,_,taxablePayToDate@None,_,_),_) => EmployerPayAmountController.show(taxYear,employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,_,_,_,_,_,_,_,totalTaxToDate@None,_),_) => EmploymentTaxController.show(taxYear,employmentId)
      case _ => CheckEmploymentDetailsController.show(taxYear, employmentId)
    }
  }
}
