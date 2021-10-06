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
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

import scala.concurrent.Future

object RedirectService extends Logging {

  trait EmploymentType
  case object EmploymentDetailsType extends EmploymentType
  case object EmploymentBenefitsType extends EmploymentType

  case class ConditionalRedirect(condition: Boolean, redirect: Call, isPriorSubmission: Option[Boolean] = None)

  def toConditionalRedirect(call: Option[Call]): Option[ConditionalRedirect] ={
    call.map(ConditionalRedirect(true, _))
  }

  //ALL PAGES
  def commonBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val benefitsReceived = cya.employmentBenefits.map(_.isBenefitsReceived)

    Seq(
      ConditionalRedirect(benefitsReceived.isEmpty, ReceiveAnyBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
      ConditionalRedirect(benefitsReceived.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true)),
      ConditionalRedirect(benefitsReceived.contains(false), CheckYourBenefitsController.show(taxYear, employmentId))
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

  def carBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId)
  }

  def carBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(carQuestion.isEmpty, CompanyCarBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(carQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(carQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true))
      )
  }

  def carFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carSectionFinished(taxYear, employmentId))))

    carBenefitsAmountRedirects(cya, taxYear, employmentId) ++ Seq(carSectionFinished).flatten
  }

  def carFuelBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion))

    carFuelBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO GO TO CAR fuel yes no
        ConditionalRedirect(carFuelQuestion.isEmpty, CompanyCarFuelBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(carFuelQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(carFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true)),
      )
  }

  def vanBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val fullCarSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.fullCarSectionFinished(taxYear, employmentId))))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(fullCarSectionFinished).flatten
  }

  def vanBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion))

    vanBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(vanQuestion.isEmpty, CompanyCarBenefitsController.show(taxYear, employmentId)),
        //TODO GO TO MILEAGE YES NO QUESTION
        ConditionalRedirect(vanQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(vanQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true))
      )
  }

  def vanFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanSectionFinished(taxYear, employmentId))))

    vanBenefitsAmountRedirects(cya, taxYear, employmentId) ++ Seq(vanSectionFinished).flatten
  }

  def vanFuelBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion))

    vanFuelBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO GO TO VAN fuel yes no
        ConditionalRedirect(vanFuelQuestion.isEmpty, CompanyCarFuelBenefitsController.show(taxYear, employmentId)),
        //TODO GO TO MILEAGE YES / NO QUESTION
        ConditionalRedirect(vanFuelQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(vanFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true)),
      )
  }

  def mileageBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val fullCarSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.fullCarSectionFinished(taxYear, employmentId))))

    val fullVanSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.fullVanSectionFinished(taxYear, employmentId))))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(fullCarSectionFinished,fullVanSectionFinished).flatten
  }

  def mileageBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val cyaMileageQuestion: Option[Boolean] = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion))

    mileageBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO GO TO MILEAGE YES / NO QUESTION
        ConditionalRedirect(cyaMileageQuestion.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId)),
        //TODO GO TO Accommodation or relocation QUESTION
        ConditionalRedirect(cyaMileageQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(false)),
        ConditionalRedirect(cyaMileageQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), isPriorSubmission = Some(true))
      )
  }

  def accommodationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val fullCarVanFuelFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.isFinished(taxYear, employmentId))))

    commonBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(fullCarVanFuelFinished).flatten
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
          case Some(redirect) =>
            logger.info(s"[RedirectService][calculateRedirect]" +
              s" Some data is missing / in the wrong state for the requested page. Routing to ${redirect.header.headers.getOrElse("Location", "")}")
            Left(redirect)
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