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

import java.util.NoSuchElementException

import config.{AppConfig, ErrorHandler}
import connectors.httpParsers.CreateUpdateEmploymentDataHttpParser.CreateUpdateEmploymentDataResponse
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import connectors.{CreateUpdateEmploymentDataConnector, IncomeTaxUserDataConnector}
import controllers.employment.routes.CheckEmploymentDetailsController
import javax.inject.{Inject, Singleton}
import models.employment.createUpdate._
import models.employment._
import models.mongo.{EmploymentCYAModel, EmploymentUserData, ExpensesCYAModel, ExpensesUserData}
import models.{IncomeTaxUserData, User}
import org.joda.time.DateTimeZone
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import repositories.{EmploymentUserDataRepository, ExpensesUserDataRepository}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class EmploymentSessionService @Inject()(employmentUserDataRepository: EmploymentUserDataRepository,
                                         expensesUserDataRepository: ExpensesUserDataRepository,
                                         incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                         implicit private val appConfig: AppConfig,
                                         val messagesApi: MessagesApi,
                                         errorHandler: ErrorHandler,
                                         createUpdateEmploymentDataConnector: CreateUpdateEmploymentDataConnector,
                                         implicit val ec: ExecutionContext) extends Logging {

  def findPreviousEmploymentUserData(user: User[_], taxYear: Int)(result: AllEmploymentData => Result)
                                    (implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {

    getPriorData(taxYear)(user,hc).map {
      case Right(IncomeTaxUserData(Some(employmentData))) => result(employmentData)
      case Right(IncomeTaxUserData(None)) =>
        logger.info(s"[EmploymentSessionService][findPreviousEmploymentUserData] No employment data found for user. SessionId: ${user.sessionId}")
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      case Left(error) => errorHandler.handleError(error.status)
    }
  }

  def getPriorData(taxYear: Int)(implicit user: User[_], hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def getSessionData(taxYear: Int, employmentId: String)(implicit user: User[_]): Future[Option[EmploymentUserData]] = {
    employmentUserDataRepository.find(taxYear, employmentId)
  }

  def getExpensesSessionData(taxYear: Int)(implicit user: User[_]): Future[Option[ExpensesUserData]] = {
    expensesUserDataRepository.find(taxYear)
  }

  def getSessionDataAndReturnResult(taxYear: Int, employmentId: String)(redirectUrl:String = appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
                                   (result: EmploymentUserData => Future[Result])
                                   (implicit user: User[_]): Future[Result] = {

    employmentUserDataRepository.find(taxYear, employmentId).flatMap {
      case Some(employmentUserData: EmploymentUserData) => result(employmentUserData)
      case None => Future(Redirect(redirectUrl))
    }
  }

  //scalastyle:off
  def createOrUpdateSessionData[A](employmentId: String, cyaModel: EmploymentCYAModel, taxYear: Int, isPriorSubmission: Boolean)
                                  (onFail: A)(onSuccess: A)(implicit user: User[_], clock: Clock): Future[A] = {

    val userData = EmploymentUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      employmentId,
      isPriorSubmission,
      cyaModel,
      clock.now(DateTimeZone.UTC)
    )

    employmentUserDataRepository.createOrUpdate(userData).map {
      case Some(_) => onSuccess
      case None => onFail
    }
  }

  //scalastyle:off
  def createOrUpdateExpensesSessionData[A](cyaModel: ExpensesCYAModel, taxYear: Int, isPriorSubmission: Boolean)
                                  (onFail: A)(onSuccess: A)(implicit user: User[_], clock: Clock): Future[A] = {
    val userData = ExpensesUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      isPriorSubmission,
      cyaModel,
      clock.now(DateTimeZone.UTC)
    )

    expensesUserDataRepository.createOrUpdate(userData).map {
      case Some(_) => onSuccess
      case None => onFail
    }
  }

  def createModelAndReturnResult(cya: EmploymentUserData, prior: Option[AllEmploymentData], taxYear: Int)
                                (result: CreateUpdateEmploymentRequest => Future[Result])(implicit user: User[_]): Future[Result] = {
    cyaAndPriorToCreateUpdateEmploymentRequest(cya, prior) match {
      case Left(NothingToUpdate) => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      case Left(JourneyNotFinished) =>
        //TODO Route to: journey not finished page / show banner saying not finished / hide submit button when not complete?
       Future.successful(Redirect(CheckEmploymentDetailsController.show(taxYear,cya.employmentId)))
      case Right(model) => result(model)
    }
  }

  def cyaAndPriorToCreateUpdateEmploymentRequest(cya: EmploymentUserData, prior: Option[AllEmploymentData])(implicit user: User[_]): Either[CreateUpdateEmploymentRequestError, CreateUpdateEmploymentRequest] = {

    val hmrcEmploymentIdToIgnore: Option[String] = prior.flatMap(_.hmrcEmploymentData.find(_.employmentId == cya.employmentId).map(_.employmentId))
    val customerEmploymentId: Option[String] = prior.flatMap(_.customerEmploymentData.find(_.employmentId == cya.employmentId).map(_.employmentId))

    Try {
      val employment = formCreateUpdateEmployment(cya, prior)
      val employmentData = formCreateUpdateEmploymentData(cya, prior)

      (employment.dataHasNotChanged, employmentData.dataHasNotChanged, customerEmploymentId.isDefined) match {
        case (true, true, _) => CreateUpdateEmploymentRequest()
        case (_, _, false) => CreateUpdateEmploymentRequest(
          employmentId = None,
          employment = Some(employment.data),
          employmentData = Some(employmentData.data),
          hmrcEmploymentIdToIgnore = hmrcEmploymentIdToIgnore
        )
        case (employmentHasNotChanged, employmentDataHasNotChanged, _) => CreateUpdateEmploymentRequest(
          employmentId = customerEmploymentId,
          employment = if (employmentHasNotChanged) None else Some(employment.data),
          employmentData = if (employmentDataHasNotChanged) None else Some(employmentData.data)
        )
      }
    }.toEither match {
      case Left(error:NoSuchElementException) =>
        logger.warn(s"[EmploymentSessionService][cyaAndPriorToCreateUpdateEmploymentRequest] " +
          s"Could not create request model. Journey is not finished. SessionId: ${user.sessionId}.")
        Left(JourneyNotFinished)
      case Right(CreateUpdateEmploymentRequest(_, None, None, _)) =>
        logger.info(s"[EmploymentSessionService][cyaAndPriorToCreateUpdateEmploymentRequest] " +
          s"Data to be submitted matched the prior data exactly. Nothing to update. SessionId: ${user.sessionId}")
        Left(NothingToUpdate)
      case Right(model) => Right(model)
    }
  }

  private def formCreateUpdateEmployment(cya: EmploymentUserData, prior: Option[AllEmploymentData]): EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmployment] = {

    lazy val newCreateUpdateEmployment = {
      CreateUpdateEmployment(
        cya.employment.employmentDetails.employerRef,
        cya.employment.employmentDetails.employerName,
        cya.employment.employmentDetails.startDate.get, //.get on purpose to provoke no such element exception and route them to finish journey.
        cya.employment.employmentDetails.cessationDate,
        cya.employment.employmentDetails.payrollId
      )
    }

    lazy val default = EmploymentDataAndDataRemainsUnchanged(newCreateUpdateEmployment,dataHasNotChanged = false)
    prior.fold[EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmployment]](default) {
      prior =>
        val priorData: Option[EmploymentSource] = employmentSourceToUse(prior, cya.employmentId, false).map(_._1)

        priorData.fold[EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmployment]](default){
          prior =>
            EmploymentDataAndDataRemainsUnchanged(newCreateUpdateEmployment, prior.dataHasNotChanged(newCreateUpdateEmployment))
        }
    }
  }

  private def formCreateUpdateEmploymentData(cya: EmploymentUserData, prior: Option[AllEmploymentData]): EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmploymentData] = {

    def createUpdateEmploymentData(benefits: Option[Benefits] = None, deductions: Option[Deductions] = None) = {
      CreateUpdateEmploymentData(
        CreateUpdatePay(
          taxablePayToDate = cya.employment.employmentDetails.taxablePayToDate.get, //.get on purpose to provoke no such element exception and route them to finish journey.
          totalTaxToDate = cya.employment.employmentDetails.totalTaxToDate.get, //.get on purpose to provoke no such element exception and route them to finish journey.
          tipsAndOtherPayments = cya.employment.employmentDetails.tipsAndOtherPayments
        ),
        benefitsInKind = benefits,
        deductions = deductions
      )
    }

    lazy val default = EmploymentDataAndDataRemainsUnchanged(createUpdateEmploymentData(),dataHasNotChanged = false)

    prior.fold[EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmploymentData]](default) {
      prior =>
        val priorData: Option[EmploymentSource] = employmentSourceToUse(prior, cya.employmentId, false).map(_._1)

        priorData.fold[EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmploymentData]](default) {
          prior =>
            prior.employmentData.fold[EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmploymentData]] {
              EmploymentDataAndDataRemainsUnchanged(createUpdateEmploymentData(prior.employmentBenefits.flatMap(_.benefits)), dataHasNotChanged = false)
            } {
              employmentData =>
                EmploymentDataAndDataRemainsUnchanged(createUpdateEmploymentData(prior.employmentBenefits.flatMap(_.benefits), employmentData.deductions),
                  employmentData.dataHasNotChanged(createUpdateEmploymentData().pay))
            }
        }
    }
  }

  def createOrUpdateEmploymentResult(taxYear: Int, employmentRequest: CreateUpdateEmploymentRequest)
                              (implicit user: User[_], hc: HeaderCarrier): Future[Either[Result,Result]] ={
    createOrUpdateEmployment(taxYear,employmentRequest).map {
      case Left(error) => Left(errorHandler.handleError(error.status))
      case Right(_) => Right(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  private def createOrUpdateEmployment(taxYear: Int, employmentRequest: CreateUpdateEmploymentRequest)
                              (implicit user: User[_], hc: HeaderCarrier): Future[CreateUpdateEmploymentDataResponse] ={
    createUpdateEmploymentDataConnector.createUpdateEmploymentData(user.nino,taxYear,employmentRequest)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def getAndHandle(taxYear: Int, employmentId: String, redirectWhenNoPrior: Boolean = false)(block: (Option[EmploymentUserData], Option[AllEmploymentData]) => Future[Result])
                  (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    val result = for {
      optionalCya <- getSessionData(taxYear, employmentId)
      priorDataResponse <- getPriorData(taxYear)
    } yield {

      if(optionalCya.isEmpty) logger.info(s"[EmploymentSessionService][getAndHandle] No employment CYA data found for user. SessionId: ${user.sessionId}")

      val employmentDataResponse = priorDataResponse.map(_.employment)

      (optionalCya,employmentDataResponse) match {
        case (None, Right(None)) if redirectWhenNoPrior => logger.info(s"[EmploymentSessionService][getAndHandle] No employment data found for user." +
          s"Redirecting to overview page. SessionId: ${user.sessionId}")
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case (_, Right(employmentData)) => block(optionalCya, employmentData)
        case (_, Left(error)) => Future(errorHandler.handleError(error.status))
      }
    }

    result.flatten
  }

  def getAndHandleExpenses(taxYear: Int)(block: (Option[ExpensesUserData], Option[AllEmploymentData]) => Future[Result])
                  (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    val result = for {
      optionalCya <- getExpensesSessionData(taxYear)
      priorDataResponse <- getPriorData(taxYear)
    } yield {

      if(optionalCya.isEmpty) logger.info(s"[EmploymentSessionService][getAndHandleExpenses] No employment expenses CYA data found for user. SessionId: ${user.sessionId}")

      val employmentDataResponse = priorDataResponse.map(_.employment)

      (optionalCya,employmentDataResponse) match {
        case (_, Right(employmentData)) => block(optionalCya, employmentData)
        case (_, Left(error)) => Future(errorHandler.handleError(error.status))
      }
    }

    result.flatten
  }

  def clear[R](taxYear: Int, employmentId: String)(onFail: R)(onSuccess: R)(implicit user: User[_]): Future[R] = {
    employmentUserDataRepository.clear(taxYear, employmentId).map {
      case true => onSuccess
      case false => onFail
    }
  }

  def employmentSourceToUse(allEmploymentData: AllEmploymentData, employmentId: String, isInYear: Boolean): Option[(EmploymentSource,Boolean)] = {
    if(isInYear){
      allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId)).map((_,false))
    } else {

      val hmrcRecord = allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId) && source.dateIgnored.isEmpty)
      val customerRecord = allEmploymentData.customerEmploymentData.find(source => source.employmentId.equals(employmentId))

      customerRecord.fold(hmrcRecord.map((_,false)))(customerRecord => Some(customerRecord,true))
    }
  }

  def getLatestEmploymentData(allEmploymentData: AllEmploymentData, isInYear: Boolean): Seq[EmploymentSource] ={
    (if(isInYear){
      allEmploymentData.hmrcEmploymentData
    } else {
      //Filters out hmrc data that has been ignored
      val hmrcData: Seq[EmploymentSource] = allEmploymentData.hmrcEmploymentData.filter(_.dateIgnored.isEmpty)
      val customerData: Seq[EmploymentSource] = allEmploymentData.customerEmploymentData
      hmrcData ++ customerData
    }).sorted(Ordering.by((_: EmploymentSource).submittedOn).reverse)
  }

  def getLatestExpenses(allEmploymentData: AllEmploymentData, isInYear: Boolean): Option[(EmploymentExpenses, Boolean)] ={
    if(isInYear){
      allEmploymentData.hmrcExpenses.map((_,false))
    } else {
      val hmrcExpenses = allEmploymentData.hmrcExpenses.filter(_.dateIgnored.isEmpty)
      val customerExpenses = allEmploymentData.customerExpenses

      if(hmrcExpenses.isDefined && customerExpenses.isDefined && hmrcExpenses.get.dateIgnored.isEmpty){
        logger.warn("[EmploymentSessionService][getLatestExpenses] Hmrc expenses and customer expenses exist but hmrc expenses have not been ignored")
      }

      allEmploymentData.customerExpenses.fold(allEmploymentData.hmrcExpenses.map((_,false)))(customerExpenses => Some(customerExpenses,true))
    }
  }
}


