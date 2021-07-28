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

import java.time.ZonedDateTime

import config.{AppConfig, ErrorHandler}
import connectors.IncomeTaxUserDataConnector
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.employment.{AllEmploymentData, EmploymentExpenses, EmploymentSource}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.{IncomeTaxUserData, User}
import org.joda.time.DateTimeZone
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import repositories.EmploymentUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmploymentSessionService @Inject()(employmentUserDataRepository: EmploymentUserDataRepository,
                                         incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                         implicit private val appConfig: AppConfig,
                                         val messagesApi: MessagesApi,
                                         errorHandler: ErrorHandler,
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

  private def getPriorData(taxYear: Int)(implicit user: User[_], hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def getSessionData(taxYear: Int, employmentId: String)(implicit user: User[_]): Future[Option[EmploymentUserData]] = {
    employmentUserDataRepository.find(taxYear, employmentId)
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

  def getAndHandle(taxYear: Int, employmentId: String)(block: (Option[EmploymentCYAModel], AllEmploymentData) => Future[Result])
                     (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    val result = for {
      optionalCya <- getSessionData(taxYear, employmentId)
      priorDataResponse <- getPriorData(taxYear)
    } yield {

      if(optionalCya.isEmpty) logger.info(s"[EmploymentSessionService][getAndHandle] No employment CYA data found for user. SessionId: ${user.sessionId}")

      val employmentDataResponse = priorDataResponse.map(_.employment)

      employmentDataResponse match {
        case Right(Some(employmentData)) => block(optionalCya.map(_.employment), employmentData)
        case Right(None) =>
          logger.info(s"[EmploymentSessionService][getAndHandle] No employment data found for user." +
            s"Redirecting to overview page. SessionId: ${user.sessionId}")
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case Left(error) => Future(errorHandler.handleError(error.status))
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

  //Returns latest employment source and whether it is customer data
  def employmentSourceToUse(allEmploymentData: AllEmploymentData, employmentId: String, isInYear: Boolean): Option[(EmploymentSource,Boolean)] = {
    if(isInYear){
      allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId)).map((_,false))
    } else {

      val hmrcRecord = allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId))
      val customerRecord = allEmploymentData.customerEmploymentData.find(source => source.employmentId.equals(employmentId))

      (hmrcRecord, customerRecord) match {
        case (hmrc@Some(_), None) => hmrc.map((_,false))
        case (None, customer@Some(_)) => customer.map((_,true))
        case (Some(hmrcData), Some(customerData)) => Some(latestEmploymentSource((hmrcData,customerData)))
        case (None, None) => None
      }
    }
  }

  def getLatestEmploymentData(allEmploymentData: AllEmploymentData, isInYear: Boolean): Seq[EmploymentSource] ={
    (if(isInYear){
      allEmploymentData.hmrcEmploymentData
    } else {
      //Filters out hmrc data that has been ignored
      val hmrcData: Seq[EmploymentSource] = allEmploymentData.hmrcEmploymentData.filterNot(_.dateIgnored.isDefined)
      val customerData: Seq[EmploymentSource] = allEmploymentData.customerEmploymentData

      (hmrcData.nonEmpty, customerData.nonEmpty) match {
        case (true, false) => hmrcData
        case (false, true) => customerData
        case (true, true) =>

          val hmrcEmploymentIds = hmrcData.map(_.employmentId)
          val customerOverrideData: Seq[EmploymentSource] = customerData.filter(customerEmployment => hmrcEmploymentIds.contains(customerEmployment.employmentId))
          lazy val newCustomerData: Seq[EmploymentSource] = customerData.filterNot(customerEmployment => hmrcEmploymentIds.contains(customerEmployment.employmentId))

          if (customerOverrideData.nonEmpty) {

            val employmentIdsWithBothCustomerAndHmrcData = customerOverrideData.map(_.employmentId)

            val hmrcOverriddenData: Seq[EmploymentSource] = hmrcData.filter(hmrcEmployment => employmentIdsWithBothCustomerAndHmrcData.contains(hmrcEmployment.employmentId))
            val onlyHmrcData: Seq[EmploymentSource] = hmrcData.filterNot(hmrcEmployment => employmentIdsWithBothCustomerAndHmrcData.contains(hmrcEmployment.employmentId))

            val hmrcAndCustomerDataWithSameIds: Seq[(EmploymentSource, EmploymentSource)] = hmrcOverriddenData.map {
              hmrcData =>
                (hmrcData, customerOverrideData.find(_.employmentId.equals(hmrcData.employmentId)).get)
            }

            latestEmploymentSources(hmrcAndCustomerDataWithSameIds) ++ onlyHmrcData ++ newCustomerData

          } else {
            hmrcData ++ customerData
          }

        case (false, false) => Seq()
      }
    }).sorted(Ordering.by((_: EmploymentSource).submittedOn).reverse)
  }

  def latestEmploymentSources(hmrcAndCustomerDataSet: Seq[(EmploymentSource,EmploymentSource)]): Seq[EmploymentSource] ={
    hmrcAndCustomerDataSet.map(latestEmploymentSource).map(_._1)
  }

  //Gets the latest employment source data looking at the top level submitted on timestamp from two sources with the same employment id
  //Returned boolean is whether it is customer data or not
  def latestEmploymentSource(hmrcAndCustomerDataSet: (EmploymentSource, EmploymentSource)): (EmploymentSource, Boolean) = {
    val (hmrc, customer) = hmrcAndCustomerDataSet

    val hmrcTimestamp: Option[ZonedDateTime] = hmrc.getSubmittedOnDateTime
    val customerTimestamp: Option[ZonedDateTime] = customer.getSubmittedOnDateTime

    val shouldUseHmrcData = {
      (hmrcTimestamp, customerTimestamp) match {
        case (Some(_), None) => true
        case (None, Some(_)) => false
        case (Some(hmrcData), Some(customerData)) => hmrcData.isAfter(customerData)
        case _ => false
      }
    }

    if (shouldUseHmrcData) (hmrc, false) else (customer,true)
  }

  def getLatestExpenses(allEmploymentData: AllEmploymentData, isInYear: Boolean): Option[EmploymentExpenses] ={
    if(isInYear){
      allEmploymentData.hmrcExpenses
    } else {
      (allEmploymentData.hmrcExpenses, allEmploymentData.customerExpenses) match {
        case (hmrc@Some(_), None) => hmrc
        case (None, customer@Some(_)) => customer
        case (Some(hmrcData), Some(customerData)) =>

          val hmrcTimestamp: Option[ZonedDateTime] = hmrcData.getSubmittedOnDateTime
          val customerTimestamp: Option[ZonedDateTime] = customerData.getSubmittedOnDateTime

          val shouldUseHmrcExpenses = {
            (hmrcTimestamp, customerTimestamp) match {
              case (Some(_), None) => true
              case (None, Some(_)) => false
              case (Some(hmrcData), Some(customerData)) => hmrcData.isAfter(customerData)
              case _ => false
            }
          }

          if (shouldUseHmrcExpenses) Some(hmrcData) else Some(customerData)

        case _ => None
      }
    }
  }
}


