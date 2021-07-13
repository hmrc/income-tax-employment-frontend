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

import config.{AppConfig, ErrorHandler}
import connectors.IncomeTaxUserDataConnector
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import javax.inject.{Inject, Singleton}
import models.{IncomeTaxUserData, User}
import models.employment.{AllEmploymentData, EmploymentExpenses, EmploymentSource}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import repositories.EmploymentUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmploymentSessionService @Inject()(employmentUserDataRepository: EmploymentUserDataRepository,
                                         incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                         implicit private val appConfig: AppConfig,
                                         val messagesApi: MessagesApi,
                                         errorHandler: ErrorHandler) extends Logging {

  def findPreviousEmploymentUserData(user: User[_], taxYear: Int)(result: AllEmploymentData => Result)
                                    (implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    getPriorData(taxYear)(user,hc).map {
      case Right(IncomeTaxUserData(Some(employmentData))) => result(employmentData)
      case Right(IncomeTaxUserData(None)) =>
        logger.info(s"[IncomeTaxUserDataService][findUserData] No employment data found for user. SessionId: ${user.sessionId}")
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

  //scalastyle:off
  def updateSessionData[A](employmentId: String, cyaModel: EmploymentCYAModel, taxYear: Int,
                           needsCreating: Boolean, isPriorSubmission: Boolean)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = EmploymentUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      employmentId,
      isPriorSubmission = isPriorSubmission,
      cyaModel,
      DateTime.now(DateTimeZone.UTC)
    )

    //TODO See if we can remove the create / update and have one method
    if(needsCreating){
      employmentUserDataRepository.create(userData).map {
        case true => onSuccess
        case false => onFail
      }
    } else {
      employmentUserDataRepository.update(userData).map {
        case true => onSuccess
        case false => onFail
      }
    }
  }

  def getAndHandle(taxYear: Int, employmentId: String)(block: (Option[EmploymentCYAModel], AllEmploymentData) => Future[Result])
                     (implicit executionContext: ExecutionContext, user: User[_], hc: HeaderCarrier): Future[Result] = {
    val result = for {
      optionalCya <- getSessionData(taxYear, employmentId)
      priorDataResponse <- getPriorData(taxYear)
    } yield {

      val employmentDataResponse = priorDataResponse.map(_.employment)

      employmentDataResponse match {
        case Right(Some(employmentData)) => block(optionalCya.map(_.employment), employmentData)
        case Right(None) =>
          logger.info(s"[IncomeTaxUserDataService][findUserData] No employment data found for user. SessionId: ${user.sessionId}")
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case Left(error) => Future(errorHandler.handleError(error.status))
      }
    }

    result.flatten
  }

  def clear[R](taxYear: Int, employmentId: String)(onFail: R)(onSuccess: R)(implicit user: User[_], ec: ExecutionContext): Future[R] = {
    employmentUserDataRepository.clear(taxYear, employmentId).map {
      case true => onSuccess
      case false => onFail
    }
  }

  def customerData(allEmploymentData: AllEmploymentData, employmentId: String): Option[EmploymentSource] = allEmploymentData.customerEmploymentData.find(source => source.employmentId.equals(employmentId))
  def shouldUseCustomerData(allEmploymentData: AllEmploymentData, employmentId: String, isInYear: Boolean): Boolean = customerData(allEmploymentData, employmentId).isDefined && !isInYear
  def shouldUseCustomerExpenses(allEmploymentData: AllEmploymentData, isInYear: Boolean): Boolean = allEmploymentData.customerExpenses.isDefined && !isInYear

  def employmentExpensesToUse(allEmploymentData: AllEmploymentData, isInYear: Boolean): Option[EmploymentExpenses] = {
    if(shouldUseCustomerExpenses(allEmploymentData, isInYear)){
      allEmploymentData.customerExpenses
    } else {
      allEmploymentData.hmrcExpenses
    }
  }

  def employmentSourceToUse(allEmploymentData: AllEmploymentData, employmentId: String, isInYear: Boolean): Option[EmploymentSource] = {
    if(shouldUseCustomerData(allEmploymentData, employmentId, isInYear)){
      customerData(allEmploymentData,employmentId)
    } else {
      allEmploymentData.hmrcEmploymentData.find(source => source.employmentId.equals(employmentId))
    }
  }

}


