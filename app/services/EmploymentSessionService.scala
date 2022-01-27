/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.parsers.CreateUpdateEmploymentDataHttpParser.CreateUpdateEmploymentDataResponse
import connectors.parsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import connectors.{CreateUpdateEmploymentDataConnector, IncomeSourceConnector, IncomeTaxUserDataConnector}
import controllers.employment.routes.{CheckEmploymentDetailsController, CheckYourBenefitsController, EmployerInformationController, EmploymentSummaryController}
import models.benefits.Benefits
import models.employment._
import models.employment.createUpdate._
import models.mongo.{EmploymentCYAModel, EmploymentUserData, ExpensesCYAModel, ExpensesUserData}
import models.{IncomeTaxUserData, User}
import org.joda.time.DateTimeZone
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import repositories.{EmploymentUserDataRepository, ExpensesUserDataRepository}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock
import java.util.NoSuchElementException
import common.EmploymentSection
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class EmploymentSessionService @Inject()(employmentUserDataRepository: EmploymentUserDataRepository,
                                         expensesUserDataRepository: ExpensesUserDataRepository,
                                         incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                         incomeSourceConnector: IncomeSourceConnector,
                                         implicit private val appConfig: AppConfig,
                                         val messagesApi: MessagesApi,
                                         errorHandler: ErrorHandler,
                                         createUpdateEmploymentDataConnector: CreateUpdateEmploymentDataConnector,
                                         implicit val ec: ExecutionContext) extends Logging {

  def findPreviousEmploymentUserData(user: User[_], taxYear: Int, overrideRedirect: Option[Result] = None)(result: AllEmploymentData => Result)
                                    (implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {

    getPriorData(taxYear)(user, hc).map {
      case Right(IncomeTaxUserData(Some(employmentData))) => result(employmentData)
      case Right(IncomeTaxUserData(None)) =>
        logger.info(s"[EmploymentSessionService][findPreviousEmploymentUserData] No employment data found for user. SessionId: ${user.sessionId}")
        overrideRedirect.fold(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))(redirect => redirect)
      case Left(error) => errorHandler.handleError(error.status)
    }
  }

  def getPriorData(taxYear: Int)(implicit user: User[_], hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.get(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def getSessionDataResult(taxYear: Int, employmentId: String)(result: Option[EmploymentUserData] => Future[Result])
                          (implicit user: User[_], request: Request[_]): Future[Result] = {
    employmentUserDataRepository.find(taxYear, employmentId).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => result(value)
    }
  }

  def getSessionData(taxYear: Int, employmentId: String)
                    (implicit user: User[_], request: Request[_]): Future[Either[Result, Option[EmploymentUserData]]] = {
    employmentUserDataRepository.find(taxYear, employmentId).map {
      case Left(_) => Left(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => Right(value)
    }
  }

  def getExpensesSessionData(taxYear: Int)(implicit user: User[_]): Future[Either[Result, Option[ExpensesUserData]]] = {
    expensesUserDataRepository.find(taxYear).map {
      case Left(_) => Left(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => Right(value)
    }
  }

  def getExpensesSessionDataResult(taxYear: Int)(result: Option[ExpensesUserData] => Future[Result])
                                  (implicit user: User[_], request: Request[_]): Future[Result] = {
    expensesUserDataRepository.find(taxYear).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => result(value)
    }
  }

  def getSessionDataAndReturnResult(taxYear: Int, employmentId: String)(redirectUrl: String = appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
                                   (result: EmploymentUserData => Future[Result])
                                   (implicit user: User[_]): Future[Result] = {

    employmentUserDataRepository.find(taxYear, employmentId).flatMap {
      case Right(Some(employmentUserData: EmploymentUserData)) => result(employmentUserData)
      case Right(None) => Future.successful(Redirect(redirectUrl))
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
    }
  }

  //scalastyle:off
  def createOrUpdateSessionData[A](employmentId: String, cyaModel: EmploymentCYAModel, taxYear: Int, isPriorSubmission: Boolean, hasPriorBenefits: Boolean)
                                  (onFail: A)(onSuccess: A)(implicit user: User[_], clock: Clock): Future[A] = {

    val userData = EmploymentUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      employmentId,
      isPriorSubmission,
      hasPriorBenefits = hasPriorBenefits,
      cyaModel,
      clock.now(DateTimeZone.UTC)
    )

    employmentUserDataRepository.createOrUpdate(userData).map {
      case Right(_) => onSuccess
      case Left(_) => onFail
    }
  }

  def createOrUpdateEmploymentUserDataWith(taxYear: Int,
                                           employmentId: String,
                                           isPriorSubmission: Boolean,
                                           hasPriorBenefits: Boolean,
                                           employment: EmploymentCYAModel)(implicit user: User[_], clock: Clock): Future[Either[Unit, EmploymentUserData]] = {

    val employmentUserData = EmploymentUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      employmentId,
      isPriorSubmission = isPriorSubmission,
      hasPriorBenefits = hasPriorBenefits,
      employment = employment,
      clock.now(DateTimeZone.UTC)
    )

    employmentUserDataRepository.createOrUpdate(employmentUserData).map {
      case Right(_) => Right(employmentUserData)
      case Left(_) => Left()
    }
  }

  def createOrUpdateExpensesSessionData[A](cyaModel: ExpensesCYAModel, taxYear: Int, isPriorSubmission: Boolean, hasPriorExpenses: Boolean)
                                          (onFail: A)(onSuccess: A)(implicit user: User[_], clock: Clock): Future[A] = {
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

  def createOrUpdateExpensesUserDataWith(taxYear: Int,
                                         isPriorSubmission: Boolean,
                                         hasPriorExpenses: Boolean,
                                         expensesCYAModel: ExpensesCYAModel)
                                        (implicit user: User[_], clock: Clock): Future[Either[Unit, ExpensesUserData]] = {
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
      case Left(_) => Left()
    }
  }

  def createModelAndReturnResult(cya: EmploymentUserData, prior: Option[AllEmploymentData], taxYear: Int, section: EmploymentSection.Value)
                                (result: CreateUpdateEmploymentRequest => Future[Result])(implicit user: User[_]): Future[Result] = {

    val notFinishedRedirect = {
      Future.successful(section match {
        case common.EmploymentSection.EMPLOYMENT_DETAILS => Redirect(CheckEmploymentDetailsController.show(taxYear, cya.employmentId))
        case common.EmploymentSection.EMPLOYMENT_BENEFITS => Redirect(CheckYourBenefitsController.show(taxYear, cya.employmentId))
        //TODO Update to student loans CYA page
        case common.EmploymentSection.STUDENT_LOANS => Redirect(CheckEmploymentDetailsController.show(taxYear, cya.employmentId))
      })
    }

    cyaAndPriorToCreateUpdateEmploymentRequest(cya, prior, section) match {
      case Left(NothingToUpdate) => Future.successful(Redirect(EmployerInformationController.show(taxYear, cya.employmentId)))
      case Left(JourneyNotFinished) =>
        //TODO Route to: journey not finished page / show banner saying not finished / hide submit button when not complete?
        notFinishedRedirect
      case Right(model) => result(model)
    }
  }

  private def cyaAndPriorToCreateUpdateEmploymentRequest(cya: EmploymentUserData, prior: Option[AllEmploymentData], section: EmploymentSection.Value)
                                                        (implicit user: User[_]): Either[CreateUpdateEmploymentRequestError, CreateUpdateEmploymentRequest] = {

    val hmrcEmploymentIdToIgnore: Option[String] = prior.flatMap(_.hmrcEmploymentData.find(_.employmentId == cya.employmentId).map(_.employmentId))
    val customerEmploymentId: Option[String] = prior.flatMap(_.customerEmploymentData.find(_.employmentId == cya.employmentId).map(_.employmentId))

    Try {
      val employment = formCreateUpdateEmployment(cya, prior, section)
      val employmentData = formCreateUpdateEmploymentData(cya, prior, section)

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
      case Left(error: NoSuchElementException) =>
        val additionalContext = if(error.getMessage.contains("None.get")) "Pay fields must be present." else ""
        logger.warn(s"[EmploymentSessionService][cyaAndPriorToCreateUpdateEmploymentRequest] " +
          s"Could not create request model. Journey is not finished. $additionalContext Exception: $error. SessionId: ${user.sessionId}.")
        Left(JourneyNotFinished)
      case Right(CreateUpdateEmploymentRequest(_, None, None, _)) =>
        logger.info(s"[EmploymentSessionService][cyaAndPriorToCreateUpdateEmploymentRequest] " +
          s"Data to be submitted matched the prior data exactly. Nothing to update. SessionId: ${user.sessionId}")
        Left(NothingToUpdate)
      case Right(model) => Right(model)
    }
  }

  private def formCreateUpdateEmployment(cya: EmploymentUserData, prior: Option[AllEmploymentData], section: EmploymentSection.Value): EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmployment] = {

    val priorEmployment = prior.flatMap(_.eoyEmploymentSourceWith(cya.employmentId).map(_.employmentSource))

    lazy val newCreateUpdateEmployment = {
      if(section == EmploymentSection.EMPLOYMENT_DETAILS){
        CreateUpdateEmployment(
          cya.employment.employmentDetails.employerRef,
          cya.employment.employmentDetails.employerName,
          cya.employment.employmentDetails.startDate.get, //.get on purpose to provoke no such element exception and route them to finish journey.
          cya.employment.employmentDetails.cessationDate,
          cya.employment.employmentDetails.payrollId
        )
      } else {
        priorEmployment.fold{
          throw new NoSuchElementException("A prior employment is needed to make amendments to employment sections after the employment details section")
        }{
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

    lazy val default = EmploymentDataAndDataRemainsUnchanged(newCreateUpdateEmployment, dataHasNotChanged = false)

    priorEmployment match {
      case Some(prior) => EmploymentDataAndDataRemainsUnchanged(newCreateUpdateEmployment, prior.dataHasNotChanged(newCreateUpdateEmployment))
      case None => default
    }
  }

  private def formCreateUpdateEmploymentData(cya: EmploymentUserData, prior: Option[AllEmploymentData], section: EmploymentSection.Value): EmploymentDataAndDataRemainsUnchanged[CreateUpdateEmploymentData] = {

    val priorEmployment = prior.flatMap(_.eoyEmploymentSourceWith(cya.employmentId).map(_.employmentSource))

    def priorBenefits: Option[Benefits] = priorEmployment.flatMap(_.employmentBenefits.flatMap(_.benefits))
    def priorStudentLoans: Option[Deductions] = priorEmployment.flatMap(_.employmentData.flatMap(_.deductions))
    def priorTaxablePayToDate: BigDecimal = priorEmployment.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))).get //.get on purpose to provoke no such element exception and route them to finish journey.
    def priorTotalTaxToDate: BigDecimal = priorEmployment.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))).get //.get on purpose to provoke no such element exception and route them to finish journey
    def priorPayData = CreateUpdatePay(taxablePayToDate = priorTaxablePayToDate, totalTaxToDate = priorTotalTaxToDate)

    lazy val createUpdateEmploymentData = {
      section match {
        case common.EmploymentSection.EMPLOYMENT_DETAILS =>

          CreateUpdateEmploymentData(
            CreateUpdatePay(
              taxablePayToDate = cya.employment.employmentDetails.taxablePayToDate.get, //.get on purpose to provoke no such element exception and route them to finish journey.
              totalTaxToDate = cya.employment.employmentDetails.totalTaxToDate.get //.get on purpose to provoke no such element exception and route them to finish journey.
            ),
            benefitsInKind = priorBenefits,
            deductions = priorStudentLoans
          )

        case common.EmploymentSection.EMPLOYMENT_BENEFITS =>

          val benefits = cya.employment.employmentBenefits.map(_.toBenefits)

          CreateUpdateEmploymentData(priorPayData, benefitsInKind = if(benefits.exists(_.hasBenefitsPopulated)) benefits else None, deductions = priorStudentLoans)

        case common.EmploymentSection.STUDENT_LOANS =>

          CreateUpdateEmploymentData(priorPayData, benefitsInKind = priorBenefits, deductions = cya.employment.studentLoans.flatMap(_.toDeductions))
      }
    }

    lazy val default = EmploymentDataAndDataRemainsUnchanged(createUpdateEmploymentData, dataHasNotChanged = false)

    def dataHasNotChanged(prior: EmploymentSource): Boolean = {
      section match {
        case common.EmploymentSection.EMPLOYMENT_DETAILS => prior.employmentData.exists(_.payDataHasNotChanged(createUpdateEmploymentData.pay))
        case common.EmploymentSection.EMPLOYMENT_BENEFITS => prior.employmentBenefits.exists(_.benefitsDataHasNotChanged(createUpdateEmploymentData.benefitsInKind))
        case common.EmploymentSection.STUDENT_LOANS => prior.employmentData.exists(_.studentLoansDataHasNotChanged(createUpdateEmploymentData.deductions))
      }
    }

    priorEmployment match {
      case Some(prior) => EmploymentDataAndDataRemainsUnchanged(createUpdateEmploymentData,dataHasNotChanged(prior))
      case None => default
    }
  }

  def createOrUpdateEmploymentResult(taxYear: Int, employmentRequest: CreateUpdateEmploymentRequest)
                                    (implicit user: User[_], hc: HeaderCarrier): Future[Either[Result, Result]] = {
    createOrUpdateEmployment(taxYear, employmentRequest).map {
      case Left(error) => Left(errorHandler.handleError(error.status))
      case Right(None) => employmentRequest.employmentId match {
        case Some(employmentId) => Right(Redirect(EmployerInformationController.show(taxYear, employmentId)))
        case None => Right(Redirect(EmploymentSummaryController.show(taxYear)))
      }
      case Right(Some(employmentId)) => Right(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
    }
  }

  private def createOrUpdateEmployment(taxYear: Int, employmentRequest: CreateUpdateEmploymentRequest)
                                      (implicit user: User[_], hc: HeaderCarrier): Future[CreateUpdateEmploymentDataResponse] = {
    createUpdateEmploymentDataConnector.createUpdateEmploymentData(user.nino, taxYear, employmentRequest)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def getAndHandle(taxYear: Int, employmentId: String, redirectWhenNoPrior: Boolean = false)
                  (block: (Option[EmploymentUserData], Option[AllEmploymentData]) => Future[Result])
                  (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    val result = for {
      optionalCya <- getSessionData(taxYear, employmentId)
      priorDataResponse <- getPriorData(taxYear)
    } yield {
      if (optionalCya.isRight) {
        if (optionalCya.right.get.isEmpty) logger.info(s"[EmploymentSessionService][getAndHandle] No employment CYA data found for user. SessionId: ${user.sessionId}")
      }

      val employmentDataResponse = priorDataResponse.map(_.employment)

      (optionalCya, employmentDataResponse) match {
        case (Right(None), Right(None)) if redirectWhenNoPrior => logger.info(s"[EmploymentSessionService][getAndHandle] No employment data found for user." +
          s"Redirecting to overview page. SessionId: ${user.sessionId}")
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case (Right(optionalCya), Right(employmentData)) => block(optionalCya, employmentData)
        case (_, Left(error)) => Future(errorHandler.handleError(error.status))
        case (Left(_), _) => Future(errorHandler.handleError(INTERNAL_SERVER_ERROR))
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

      if (optionalCya.isRight) {
        if (optionalCya.right.get.isEmpty) logger.info(s"[EmploymentSessionService][getAndHandleExpenses] No employment expenses CYA data found for user. SessionId: ${user.sessionId}")
      }

      val employmentDataResponse = priorDataResponse.map(_.employment)

      (optionalCya, employmentDataResponse) match {
        case (Right(optionalCya), Right(employmentData)) => block(optionalCya, employmentData)
        case (_, Left(error)) => Future(errorHandler.handleError(error.status))
        case (Left(_), _) => Future(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      }
    }

    result.flatten
  }

  def clear(taxYear: Int, employmentId: String)(implicit user: User[_], hc: HeaderCarrier): Future[Either[Unit, Unit]] = {
    incomeSourceConnector.put(taxYear, user.nino, "employment")(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(Left())
      case _ => employmentUserDataRepository.clear(taxYear, employmentId).map {
        case true => Right()
        case false => Left()
      }
    }
  }

  def clearExpenses(taxYear: Int)(onSuccess: Result)(implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    incomeSourceConnector.put(taxYear, user.nino, "employment")(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case _ => expensesUserDataRepository.clear(taxYear).map {
        case true => onSuccess
        case false => errorHandler.internalServerError()
      }
    }
  }
}


