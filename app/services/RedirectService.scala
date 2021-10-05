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
import models.employment.BenefitsViewModel
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

import scala.concurrent.Future

object RedirectService {

  trait EmploymentType
  case object EmploymentDetailsType extends EmploymentType
  case object EmploymentBenefitsType extends EmploymentType

  case class ConditionalRedirect(condition: Boolean, redirect: Call, isPriorSubmission: Option[Boolean] = None)

  //ALL PAGES
  def commonBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val benefitsReceived = cya.employmentBenefits.map(_.isBenefitsReceived)

    Seq(
      ConditionalRedirect(benefitsReceived.isEmpty, ReceiveAnyBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
      ConditionalRedirect(benefitsReceived.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true)),
      ConditionalRedirect(benefitsReceived.contains(false), CheckYourBenefitsController.show(taxYear, employmentId))
    )
  }

  def priorSubmissionBenefitsRouting(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Call ={
    cya match {
      case EmploymentCYAModel(_,None) => CheckYourBenefitsController.show(taxYear, employmentId)
      case EmploymentCYAModel(_,Some(b: BenefitsViewModel)) if !b.isBenefitsReceived => CheckYourBenefitsController.show(taxYear, employmentId)
      case EmploymentCYAModel(_,Some(b: BenefitsViewModel)) if b.isBenefitsReceived && b.carVanFuelModel.isEmpty => CarVanFuelBenefitsController.show(taxYear, employmentId)
    }
  }

  def accommodationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carVanFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion))

    commonBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(carVanFuelQuestion.isEmpty, CarVanFuelBenefitsController.show(taxYear, employmentId)),
        //TODO go to accommodation section
        ConditionalRedirect(carVanFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(carVanFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true))
      )
  }

  //ALL CAR VAN PAGES
  def commonCarVanFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carVanFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion))

    commonBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(carVanFuelQuestion.isEmpty, CarVanFuelBenefitsController.show(taxYear, employmentId)),
        //TODO go to accommodation section
        ConditionalRedirect(carVanFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(carVanFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true))
      )
  }

  def commonCarBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(carQuestion.isEmpty, CompanyCarBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(carQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(carQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true))
      )
  }



  def commonCarAmountBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carAmountQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.car))

    commonCarBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO GO TO CAR AMOUNT
        ConditionalRedirect(carAmountQuestion.isEmpty, CompanyCarBenefitsController.show(taxYear, employmentId))
      )
  }

  def commonCarFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion))

    commonCarAmountBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO GO TO CAR fuel yes no
        ConditionalRedirect(carFuelQuestion.isEmpty, CompanyCarFuelBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(carFuelQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(carFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true)),
      )
  }

  def commonCarFuelAmountBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carFuelAmountQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuel))

    commonCarAmountBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO GO TO CAR fuel amount page
        ConditionalRedirect(carFuelAmountQuestion.isEmpty, CompanyCarFuelBenefitsController.show(taxYear, employmentId))
      )
  }

  def commonVanBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(vanQuestion.isEmpty, CompanyCarBenefitsController.show(taxYear, employmentId)),
        //TODO GO TO MILEAGE YES NO QUESTION
        ConditionalRedirect(vanQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(vanQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true))
      )
  }

  def commonVanAmountBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanAmountQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.van))

    commonVanBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO GO TO VAN AMOUNT QUESTION
        ConditionalRedirect(vanAmountQuestion.isEmpty, CompanyCarBenefitsController.show(taxYear, employmentId))
      )
  }

  def commonVanFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion))

    commonVanAmountBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO GO TO VAN fuel yes no
        ConditionalRedirect(vanFuelQuestion.isEmpty, CompanyCarFuelBenefitsController.show(taxYear, employmentId)),
        //TODO GO TO MILEAGE YES / NO QUESTION
        ConditionalRedirect(vanFuelQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(vanFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true)),
      )
  }

  def commonVanFuelAmountBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanFuelAmountQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuel))

    commonCarAmountBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO GO TO VAN fuel amount page
        ConditionalRedirect(vanFuelAmountQuestion.isEmpty, CompanyCarFuelBenefitsController.show(taxYear, employmentId))
      )
  }

  def mileageBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val cyaMileageQuestion: Option[Boolean] = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(
      //TODO GO TO MILEAGE YES / NO QUESTION
      ConditionalRedirect(cyaMileageQuestion.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId)),
      //TODO GO TO Accommodation or relocation QUESTION
      ConditionalRedirect(cyaMileageQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
      ConditionalRedirect(cyaMileageQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true))
    )
  }

  def mileageAmountBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val mileageAmountQuestion: Option[BigDecimal] = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage))

    mileageBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(
      ConditionalRedirect(mileageAmountQuestion.isEmpty, MileageBenefitAmountController.show(taxYear, employmentId))
    )
  }

  def redirectBasedOnCurrentAnswers(taxYear: Int, employmentId: String, data: Option[EmploymentUserData], employmentType: EmploymentType)
                                   (cyaConditions: EmploymentCYAModel => Seq[ConditionalRedirect])
                                   (block: EmploymentUserData => Future[Result]): Future[Result] ={

    val redirect = calculateRedirect(taxYear,employmentId,data,employmentType,cyaConditions)

    redirect match {
      case Left(redirect) => Future.successful(redirect)
      case Right(cya) => block(cya)
    }
  }

  private def calculateRedirect(taxYear: Int, employmentId: String, data: Option[EmploymentUserData], employmentType: EmploymentType,
                                cyaConditions: EmploymentCYAModel => Seq[ConditionalRedirect]): Either[Result, EmploymentUserData] ={
    data match {
      case Some(cya) =>

        val possibleRedirects = cyaConditions(cya.employment)

        val redirect = possibleRedirects.collectFirst {
          case ConditionalRedirect(condition, result, Some(isPriorSubmission)) if condition && isPriorSubmission == cya.isPriorSubmission => Redirect(result)
          case ConditionalRedirect(condition, result, None) if condition => Redirect(result)
        }

        redirect match {
          case Some(redirect) => Left(redirect)
          case None => Right(cya)
        }

      case None => employmentTypeRedirect(employmentType, taxYear, employmentId)
    }
  }

  private def employmentTypeRedirect(employmentType: EmploymentType, taxYear: Int, employmentId: String): Either[Result, EmploymentUserData] ={
    employmentType match {
      case EmploymentBenefitsType => Left(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
      case EmploymentDetailsType => Left(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
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