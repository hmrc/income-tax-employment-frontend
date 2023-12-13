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

package services

import common.{EmploymentDetailsSection, EmploymentSection}
import config.{AppConfig, ErrorHandler}
import connectors.parsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import connectors.{CreateUpdateEmploymentDataConnector, IncomeSourceConnector, IncomeTaxUserDataConnector}
import models.benefits.Benefits
import models.employment._
import models.employment.createUpdate._
import models.mongo._
import models.{AuthorisationRequest, IncomeTaxUserData, User}
import org.joda.time.DateTimeZone
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import repositories.{EmploymentUserDataRepository, ExpensesUserDataRepository}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Clock, InYearUtil}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class EmploymentSessionService @Inject()(employmentUserDataRepository: EmploymentUserDataRepository,
                                         expensesUserDataRepository: ExpensesUserDataRepository,
                                         incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                         incomeSourceConnector: IncomeSourceConnector,
                                         messagesApi: MessagesApi,
                                         errorHandler: ErrorHandler,
                                         createUpdateEmploymentDataConnector: CreateUpdateEmploymentDataConnector,
                                         clock: Clock,
                                         inYearUtil: InYearUtil)
                                        (implicit appConfig: AppConfig, ec: ExecutionContext) extends Logging {

  def findPreviousEmploymentUserData(user: User, taxYear: Int, overrideRedirect: Option[Result] = None)
                                    (result: AllEmploymentData => Result)
                                    (implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {

    getPriorData(user, taxYear)(hc).map {
      case Right(IncomeTaxUserData(Some(employmentData))) => result(employmentData)
      case Right(IncomeTaxUserData(None)) =>
        logger.info(s"[EmploymentSessionService][findPreviousEmploymentUserData] No employment data found for user. SessionId: ${user.sessionId}")
        overrideRedirect.fold(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))(redirect => redirect)
      case Left(error) => errorHandler.handleError(error.status)
    }
  }

  def getPriorData(user: User, taxYear: Int)(implicit hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.get(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def findEmploymentUserData(taxYear: Int, employmentId: String, user: User): Future[Either[Unit, Option[EmploymentUserData]]] = {
    employmentUserDataRepository.find(taxYear, employmentId, user).map {
      case Left(_) => Left(())
      case Right(data) => Right(data)
    }
  }

  def getSessionData(taxYear: Int,
                     employmentId: String,
                     user: User): Future[Either[DatabaseError, Option[EmploymentUserData]]] = {
    employmentUserDataRepository.find(taxYear, employmentId, user)
  }

  def getExpensesSessionDataResult(taxYear: Int)(result: Option[ExpensesUserData] => Future[Result])
                                  (implicit authorisationRequest: AuthorisationRequest[_]): Future[Result] = {
    expensesUserDataRepository.find(taxYear, authorisationRequest.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => result(value)
    }
  }

  def getSessionDataAndReturnResult(taxYear: Int, employmentId: String)
                                   (redirectUrl: String = appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
                                   (result: EmploymentUserData => Future[Result])
                                   (implicit authorisationRequest: AuthorisationRequest[_]): Future[Result] = {
    employmentUserDataRepository.find(taxYear, employmentId, authorisationRequest.user).flatMap {
      case Right(Some(employmentUserData: EmploymentUserData)) => result(employmentUserData)
      case Right(None) => Future.successful(Redirect(redirectUrl))
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
    }
  }

  def isExistingEmployment(taxYear: Int, employmentId: String)(
    implicit authorisationRequest: AuthorisationRequest[_], hc: HeaderCarrier): Future[Either[Result, Boolean]] = {
    getPriorData(authorisationRequest.user, taxYear)(hc).map {
      case Right(IncomeTaxUserData(Some(priorData))) => priorData
        Right(priorData.hmrcEmploymentSourceWith(employmentId).isDefined || priorData.customerEmploymentSourceWith(employmentId).isDefined)
      case Right(IncomeTaxUserData(None)) =>
        logger.info(s"[EmploymentSessionService][findPreviousEmploymentUserData] No employment data found for user. SessionId: " +
          s"${authorisationRequest.user.sessionId}")
        Right(false)
      case Left(error) => Left(errorHandler.handleError(error.status))
    }
  }

  //scalastyle:off
  def createOrUpdateSessionData[A](user: User,
                                   taxYear: Int,
                                   employmentId: String,
                                   cyaModel: EmploymentCYAModel,
                                   isPriorSubmission: Boolean,
                                   hasPriorBenefits: Boolean,
                                   hasPriorStudentLoans: Boolean)
                                  (onFail: A)(onSuccess: A): Future[A] = {
    val userData = EmploymentUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      employmentId,
      isPriorSubmission,
      hasPriorBenefits = hasPriorBenefits,
      hasPriorStudentLoans = hasPriorStudentLoans,
      cyaModel,
      clock.now(DateTimeZone.UTC)
    )

    employmentUserDataRepository.createOrUpdate(userData).map {
      case Right(_) => onSuccess
      case Left(_) => onFail
    }
  }

  def createOrUpdateEmploymentUserData(user: User,
                                       taxYear: Int,
                                       employmentId: String,
                                       originalEmploymentUserData: EmploymentUserData,
                                       employment: EmploymentCYAModel): Future[Either[Unit, EmploymentUserData]] = {
    val employmentUserData = EmploymentUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      employmentId,
      originalEmploymentUserData.isPriorSubmission,
      originalEmploymentUserData.hasPriorBenefits,
      originalEmploymentUserData.hasPriorStudentLoans,
      employment = employment,
      clock.now(DateTimeZone.UTC)
    )

    employmentUserDataRepository.createOrUpdate(employmentUserData).map {
      case Right(_) => Right(employmentUserData)
      case Left(_) => Left(())
    }
  }

  def createOrUpdateExpensesSessionData[A](cyaModel: ExpensesCYAModel,
                                           taxYear: Int,
                                           isPriorSubmission: Boolean,
                                           hasPriorExpenses: Boolean,
                                           user: User)
                                          (onFail: A)(onSuccess: A): Future[A] = {
    val userData = ExpensesUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      isPriorSubmission,
      hasPriorExpenses,
      cyaModel,
      clock.now(DateTimeZone.UTC)
    )

    expensesUserDataRepository.createOrUpdate(userData).map {
      case Right(_) => onSuccess
      case Left(_) => onFail
    }
  }

  def createOrUpdateExpensesUserData(user: User,
                                     taxYear: Int,
                                     isPriorSubmission: Boolean,
                                     hasPriorExpenses: Boolean,
                                     expensesCYAModel: ExpensesCYAModel): Future[Either[Unit, ExpensesUserData]] = {
    val expensesUserData = ExpensesUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      isPriorSubmission,
      hasPriorExpenses,
      expensesCYAModel,
      clock.now(DateTimeZone.UTC)
    )

    expensesUserDataRepository.createOrUpdate(expensesUserData).map {
      case Right(_) => Right(expensesUserData)
      case Left(_) => Left(())
    }
  }

  def createModelOrReturnError(user: User,
                               cya: EmploymentUserData,
                               prior: Option[AllEmploymentData],
                               section: EmploymentSection): Either[CreateUpdateEmploymentRequestError, CreateUpdateEmploymentRequest] = {

    cyaAndPriorToCreateUpdateEmploymentRequest(user, cya, prior, section) match {
      case Left(NothingToUpdate) =>
        logger.info("[createModelOrReturnResult] Nothing to update as there have been no updates to data.")
        Left(NothingToUpdate)
      case Left(JourneyNotFinished) => Left(JourneyNotFinished)
      case Right(model) => Right(model)
    }
  }

  private def cyaAndPriorToCreateUpdateEmploymentRequest(user: User,
                                                         cya: EmploymentUserData,
                                                         prior: Option[AllEmploymentData],
                                                         section: EmploymentSection): Either[CreateUpdateEmploymentRequestError, CreateUpdateEmploymentRequest] = {

    val hmrcEmploymentId: Option[String] = prior.flatMap(_.hmrcEmploymentData.find(_.employmentId == cya.employmentId).map(_.employmentId))
    val customerEmploymentId: Option[String] = {

      if (appConfig.mimicEmploymentAPICalls && section != EmploymentDetailsSection) {
        Some(cya.employmentId)
      } else {
        prior.flatMap(_.customerEmploymentData.find(_.employmentId == cya.employmentId).map(_.employmentId))
      }
    }

    Try {
      val employment = formCreateUpdateEmployment(cya, prior, section)
      val employmentData = formCreateUpdateEmploymentData(cya, prior, section)
      (employment.dataHasNotChanged, employmentData.dataHasNotChanged, customerEmploymentId.isDefined, hmrcEmploymentId.isDefined) match {
        case (true, true, _, _) => CreateUpdateEmploymentRequest()
        case (true, false, _, true) =>
          CreateUpdateEmploymentRequest(
            employmentId = hmrcEmploymentId,
            employment = None,
            employmentData = Some(employmentData.data),
            hmrcEmploymentIdToIgnore = None,
            isHmrcEmploymentId = Some(true)
          )

        case (_, _, false, _) => CreateUpdateEmploymentRequest(
          employmentId = None,
          employment = Some(employment.data),
          employmentData = Some(employmentData.data),
          hmrcEmploymentIdToIgnore = hmrcEmploymentId
        )

        case (employmentHasNotChanged, employmentDataHasNotChanged, _, _) => CreateUpdateEmploymentRequest(
          employmentId = customerEmploymentId,
          employment = if (employmentHasNotChanged) None else Some(employment.data),
          employmentData = if (employmentDataHasNotChanged) None else Some(employmentData.data)
        )
      }
    }.toEither match {
      case Left(error: NoSuchElementException) =>
        val additionalContext = if (error.getMessage.contains("None.get")) "Pay fields must be present." else ""
        logger.warn(s"[EmploymentSessionService][cyaAndPriorToCreateUpdateEmploymentRequest] " +
          s"Could not create request model. Journey is not finished. $additionalContext Exception: $error. SessionId: ${user.sessionId}.")
        Left(JourneyNotFinished)
      case Right(CreateUpdateEmploymentRequest(_, None, None, _, _)) =>
        logger.info(s"[EmploymentSessionService][cyaAndPriorToCreateUpdateEmploymentRequest] " +
          s"Data to be submitted matched the prior data exactly. Nothing to update. SessionId: ${user.sessionId}")
        Left(NothingToUpdate)
      case Right(model) => Right(model)
    }
  }

  private def formCreateUpdateEmployment(cya: EmploymentUserData,
                                         prior: Option[AllEmploymentData],
                                         section: EmploymentSection): EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmployment] = {
    val priorEmployment = prior.flatMap(_.eoyEmploymentSourceWith(cya.employmentId).map(_.employmentSource))

    lazy val cyaCreateUpdateEmployment = {
      CreateUpdateEmployment(
        cya.employment.employmentDetails.employerRef,
        cya.employment.employmentDetails.employerName,
        cya.employment.employmentDetails.startDate.get, //.get on purpose to provoke no such element exception and route them to finish journey.
        cya.employment.employmentDetails.cessationDate,
        cya.employment.employmentDetails.payrollId
      )
    }

    lazy val newCreateUpdateEmployment = {
      if (section == EmploymentDetailsSection) {
        cyaCreateUpdateEmployment
      } else {
        if (appConfig.mimicEmploymentAPICalls) {
          cyaCreateUpdateEmployment
        } else {
          priorEmployment.fold {
            throw new NoSuchElementException("A prior employment is needed to make amendments to employment sections after the employment details section")
          } {
            priorEmployment =>
              CreateUpdateEmployment(
                priorEmployment.employerRef,
                priorEmployment.employerName,
                priorEmployment.startDate.get, //.get on purpose to provoke no such element exception and route them to finish journey.
                priorEmployment.cessationDate,
                priorEmployment.payrollId
              )
          }
        }
      }
    }

    lazy val default = EmploymentDataAndDataRemainsUnchanged(newCreateUpdateEmployment, dataHasNotChanged = false)

    priorEmployment match {
      case Some(prior) => EmploymentDataAndDataRemainsUnchanged(newCreateUpdateEmployment, prior.dataHasNotChanged(newCreateUpdateEmployment))
      case None => default
    }
  }

  private def formCreateUpdateEmploymentData(cya: EmploymentUserData, prior: Option[AllEmploymentData], section: EmploymentSection): EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmploymentData] = {

    val priorEmployment: Option[EmploymentSource] = prior.flatMap(_.eoyEmploymentSourceWith(cya.employmentId).map(_.employmentSource))

    def priorBenefits: Option[Benefits] = priorEmployment.flatMap(_.employmentBenefits.flatMap(_.benefits))

    def priorStudentLoans: Option[Deductions] = priorEmployment.flatMap(_.employmentData.flatMap(_.deductions))

    def priorTaxablePayToDate: BigDecimal = priorEmployment.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))).get //.get on purpose to provoke no such element exception and route them to finish journey.

    def priorTotalTaxToDate: BigDecimal = priorEmployment.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))).get //.get on purpose to provoke no such element exception and route them to finish journey

    def priorPayData = CreateUpdatePay(taxablePayToDate = priorTaxablePayToDate, totalTaxToDate = priorTotalTaxToDate)

    lazy val cyaPay = CreateUpdatePay(
      taxablePayToDate = cya.employment.employmentDetails.taxablePayToDate.get, //.get on purpose to provoke no such element exception and route them to finish journey.
      totalTaxToDate = cya.employment.employmentDetails.totalTaxToDate.get //.get on purpose to provoke no such element exception and route them to finish journey.
    )

    lazy val cyaBenefits = cya.employment.employmentBenefits.map(_.asBenefits)
    lazy val cyaStudentLoans = cya.employment.studentLoans.flatMap(_.asDeductions)

    lazy val createUpdateEmploymentData = {
      section match {
        case common.EmploymentDetailsSection =>
          CreateUpdateEmploymentData(cyaPay, benefitsInKind = priorBenefits, deductions = priorStudentLoans)

        case common.EmploymentBenefitsSection =>

          if (appConfig.mimicEmploymentAPICalls) {
            CreateUpdateEmploymentData(cyaPay, benefitsInKind = if (cyaBenefits.exists(_.hasBenefitsPopulated)) cyaBenefits else None, deductions = cyaStudentLoans)
          } else {
            CreateUpdateEmploymentData(priorPayData, benefitsInKind = if (cyaBenefits.exists(_.hasBenefitsPopulated)) cyaBenefits else None, deductions = priorStudentLoans)
          }

        case common.StudentLoansSection =>

          if (appConfig.mimicEmploymentAPICalls) {
            CreateUpdateEmploymentData(cyaPay, benefitsInKind = if (cyaBenefits.exists(_.hasBenefitsPopulated)) cyaBenefits else None, deductions = cyaStudentLoans)
          } else {
            CreateUpdateEmploymentData(priorPayData, benefitsInKind = priorBenefits, deductions = cya.employment.studentLoans.flatMap(_.asDeductions))
          }
      }
    }

    lazy val default = EmploymentDataAndDataRemainsUnchanged(createUpdateEmploymentData, dataHasNotChanged = false)

    def dataHasNotChanged(prior: EmploymentSource): Boolean = {
      section match {
        case common.EmploymentDetailsSection => prior.employmentData.exists(_.payDataHasNotChanged(createUpdateEmploymentData.pay))
        case common.EmploymentBenefitsSection => prior.employmentBenefits.exists(_.benefitsDataHasNotChanged(createUpdateEmploymentData.benefitsInKind))
        case common.StudentLoansSection => prior.employmentData.exists(_.studentLoansDataHasNotChanged(createUpdateEmploymentData.deductions))
      }
    }

    priorEmployment match {
      case Some(prior) => EmploymentDataAndDataRemainsUnchanged(createUpdateEmploymentData, dataHasNotChanged(prior))
      case None => default
    }
  }

  def createOrUpdateEmploymentResult(taxYear: Int, employmentRequest: CreateUpdateEmploymentRequest)
                                    (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Either[Result, Option[String]]] = {
    val headerCarrier = hc.withExtraHeaders("mtditid" -> request.user.mtditid)
    val createUpdateEmploymentResponse = createUpdateEmploymentDataConnector.createUpdateEmploymentData(request.user.nino, taxYear, employmentRequest)(headerCarrier)

    createUpdateEmploymentResponse.map {
      case Left(error) => Left(errorHandler.handleError(error.status))
      case Right(employmentId) => Right(employmentId)
    }
  }

  @deprecated("Use getOptionalCYAAndPrior or other methods (e.g. getCYAAndPriorForEndOfYear) instead")
  def getAndHandle(taxYear: Int, employmentId: String, redirectWhenNoPrior: Boolean = false)
                  (block: (Option[EmploymentUserData], Option[AllEmploymentData]) => Future[Result])
                  (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Result] = {
    val result = for {
      optionalCya <- getSessionData(taxYear, employmentId, request.user)
      priorDataResponse <- getPriorData(request.user, taxYear)
    } yield {
      if (optionalCya.isRight) {
        if (optionalCya.toOption.get.isEmpty) logger.info(s"[EmploymentSessionService][getAndHandle] No employment CYA data found for user. SessionId: ${request.user.sessionId}")
      }

      val employmentDataResponse = priorDataResponse.map(_.employment)

      (optionalCya, employmentDataResponse) match {
        case (Right(None), Right(None)) if redirectWhenNoPrior => logger.info(s"[EmploymentSessionService][getAndHandle] No employment data found for user." +
          s"Redirecting to overview page. SessionId: ${request.user.sessionId}")
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case (Right(optionalCya), Right(employmentData)) =>
          block(optionalCya, employmentData)
        case (_, Left(error)) => Future(errorHandler.handleError(error.status))
        case (Left(_), _) => Future(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      }
    }

    result.flatten
  }

  def getOptionalCYAAndPrior(taxYear: Int, employmentId: String, redirectWhenNoPrior: Boolean = false)
                            (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Either[Result, OptionalCyaAndPrior]] = {

    val result = for {
      optionalCya <- getSessionData(taxYear, employmentId, request.user)
      priorDataResponse <- getPriorData(request.user, taxYear)
    } yield {
      if (optionalCya.isRight) {
        if (optionalCya.toOption.get.isEmpty) logger.info(s"[EmploymentSessionService][getAndHandle] No employment CYA data found for user. SessionId: ${request.user.sessionId}")
      }

      val employmentDataResponse = priorDataResponse.map(_.employment)

      (optionalCya, employmentDataResponse) match {
        case (Right(None), Right(None)) if redirectWhenNoPrior => logger.info(s"[EmploymentSessionService][getAndHandle] No employment data found for user." +
          s"Redirecting to overview page. SessionId: ${request.user.sessionId}")
          Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case (Right(optionalCya), Right(employmentData)) => Right(OptionalCyaAndPrior(optionalCya, employmentData))
        case (_, Left(error)) => Left(errorHandler.handleError(error.status))
        case (Left(_), _) => Left(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      }
    }

    result
  }

  def getCYAAndPriorForEndOfYear(taxYear: Int, employmentId: String)
                                (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Either[Result, CyaAndPrior]] = {

    val overviewRedirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

    if (!inYearUtil.inYear(taxYear)) {
      getCYAAndPrior(taxYear, employmentId)
    } else {
      Future.successful(Left(overviewRedirect))
    }
  }

  def getOptionalCYAAndPriorForEndOfYear(taxYear: Int, employmentId: String)
                                        (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Either[Result, OptionalCyaAndPrior]] = {

    val overviewRedirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

    if (!inYearUtil.inYear(taxYear)) {
      getOptionalCYAAndPrior(taxYear, employmentId)
    } else {
      Future.successful(Left(overviewRedirect))
    }
  }

  def getCYAAndPrior(taxYear: Int, employmentId: String)
                    (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Either[Result, CyaAndPrior]] = {

    getOptionalCYAAndPrior(taxYear, employmentId).map {
      case Left(result) => Left(result)
      case Right(OptionalCyaAndPrior(employment, allEmploymentData)) =>

        employment.fold[Either[Result, CyaAndPrior]](Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))))(
          employmentData => {
            Right(CyaAndPrior(employmentData, allEmploymentData))
          }
        )
    }
  }

  def submitAndClear(taxYear: Int, employmentId: String, model: CreateUpdateEmploymentRequest, cya: EmploymentUserData,
                     prior: Option[AllEmploymentData],
                     auditFunction: Option[(String, Int, CreateUpdateEmploymentRequest, Option[AllEmploymentData], AuthorisationRequest[_]) => Unit] = None)
                    (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Either[Result, (Option[String], EmploymentUserData)]] = {

    createOrUpdateEmploymentResult(taxYear, model).flatMap {
      case Left(result) => Future.successful(Left(result))
      case Right(returnedEmploymentId) =>

        auditFunction.foreach(function => function(employmentId, taxYear, model, prior, request))

        clear(request.user, taxYear, employmentId).map {
          case Left(_) => Left(errorHandler.internalServerError())
          case Right(_) => Right((returnedEmploymentId, cya))
        }
    }
  }

  def getAndHandleExpenses(taxYear: Int)(block: (Option[ExpensesUserData], Option[AllEmploymentData]) => Future[Result])
                          (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Result] = {
    val result = for {
      optExpensesUserData <- getExpensesSessionData(request.user, taxYear)
      priorDataResponse <- getPriorData(request.user, taxYear)
    } yield {
      if (optExpensesUserData.isRight) {
        if (optExpensesUserData.toOption.get.isEmpty) logger.info(s"[EmploymentSessionService][getAndHandleExpenses] No employment expenses CYA data found for user. SessionId: ${request.user.sessionId}")
      }

      val employmentDataResponse = priorDataResponse.map(_.employment)

      (optExpensesUserData, employmentDataResponse) match {
        case (Right(optionalCya), Right(employmentData)) => block(optionalCya, employmentData)
        case (_, Left(error)) => Future(errorHandler.handleError(error.status))
        case (Left(_), _) => Future(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      }
    }

    result.flatten
  }

  private def getExpensesSessionData(user: User, taxYear: Int)(implicit request: AuthorisationRequest[_]): Future[Either[Result, Option[ExpensesUserData]]] = {
    expensesUserDataRepository.find(taxYear, user).map {
      case Left(_) => Left(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => Right(value)
    }
  }

  def clear(user: User, taxYear: Int, employmentId: String, clearCYA: Boolean = true)
           (implicit hc: HeaderCarrier): Future[Either[Unit, Unit]] = {
    incomeSourceConnector.put(taxYear, user.nino)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(Left(()))
      case _ =>
        if (clearCYA) {
          employmentUserDataRepository.clear(taxYear, employmentId, user).map {
            case true => Right(())
            case false => Left(())
          }
        } else {
          Future.successful(Right(()))
        }
    }
  }

  def clearExpenses(taxYear: Int)(onSuccess: Result)(implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[Result] = {
    incomeSourceConnector.put(taxYear, request.user.nino)(hc.withExtraHeaders("mtditid" -> request.user.mtditid)).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case _ => expensesUserDataRepository.clear(taxYear, request.user).map {
        case true => onSuccess
        case false => errorHandler.internalServerError()
      }
    }
  }
}
