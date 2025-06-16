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

package actions

import common.{EnrolmentIdentifiers, EnrolmentKeys}
import config.{AppConfig, ErrorHandler}
import controllers.errors.routes.{AgentAuthErrorController, UnauthorisedUserErrorController}
import models.errors.MissingAgentClientDetails
import models.{AuthorisationRequest, User}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import services.{AuthService, SessionDataService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.RequestUtils.getTrueUserAgent
import utils.{EnrolmentHelper, SessionHelper}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject()(val appConfig: AppConfig,
                                 errorHandler: ErrorHandler,
                                 authService: AuthService,
                                 sessionDataService: SessionDataService)
                                (implicit mcc: MessagesControllerComponents)
  extends ActionBuilder[AuthorisationRequest, AnyContent] with I18nSupport with SessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  implicit val messagesApi: MessagesApi = mcc.messagesApi

  override def parser: BodyParser[AnyContent] = mcc.parsers.default

  private val minimumConfidenceLevel: Int = ConfidenceLevel.L250.level

  override def invokeBlock[A](request: Request[A], block: AuthorisationRequest[A] => Future[Result]): Future[Result] = {
    implicit val req: Request[A] = request
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    withSessionId { sessionId =>
      authService.authorised().retrieve(affinityGroup) {
        case Some(AffinityGroup.Agent) => agentAuthentication(block, sessionId)(request, headerCarrier)
        case Some(affinityGroup) => individualAuthentication(block, affinityGroup, sessionId)(request, headerCarrier)
      } recover {
        case _: NoActiveSession => Redirect(appConfig.signInUrl)
        case _: AuthorisationException => Redirect(UnauthorisedUserErrorController.show)
        case e =>
          logger.error(s"[AuthorisedAction][invokeBlock] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
          errorHandler.internalServerError()(request)
      }
    }
  }

  def individualAuthentication[A](block: AuthorisationRequest[A] => Future[Result],
                                  affinityGroup: AffinityGroup,
                                  sessionId: String
                                 )(implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {

    authService.authorised().retrieve(allEnrolments and confidenceLevel) {

      case enrolments ~ userConfidence if userConfidence.level >= minimumConfidenceLevel =>
        (
          EnrolmentHelper.enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments),
          EnrolmentHelper.enrolmentGetIdentifierValue(EnrolmentKeys.nino, EnrolmentIdentifiers.nino, enrolments)
        ) match {
          case (Some(mtdItId), Some(nino)) =>
            block(AuthorisationRequest(User(mtdItId, None, nino, sessionId, affinityGroup.toString), request))
          case (_, None) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - No active session. Redirecting to ${appConfig.signInUrl}")
            Future.successful(Redirect(appConfig.signInUrl))
          case (None, _) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - User has no MTD IT enrolment. Redirecting user to sign up for MTD.")
            Future.successful(Redirect(controllers.errors.routes.IndividualAuthErrorController.show))
        }
      case _ =>
        logger.warn("[AuthorisedAction][individualAuthentication] User has confidence level below 250, routing user to IV uplift.")
        Future(Redirect(appConfig.incomeTaxSubmissionIvRedirect))
    }
  }

  private val agentAuthLogString: String = "[AuthorisedAction][agentAuthentication]"

  private val signInRedirectFutureResult: Future[Result] = Future(Redirect(appConfig.signInUrl))

  private def agentRecovery[A](block: AuthorisationRequest[A] => Future[Result],
                               mtdItId: String,
                               nino: String,
                               sessionId: String
                              )(implicit request: Request[A], hc: HeaderCarrier): PartialFunction[Throwable, Future[Result]] = {
    case _: NoActiveSession =>
      logger.info(s"$agentAuthLogString - No active session. Redirecting to ${appConfig.signInUrl}")
      signInRedirectFutureResult
    case _: AuthorisationException =>
      authService.authorised(EnrolmentHelper.secondaryAgentPredicate(mtdItId))
        .retrieve(allEnrolments)(
          enrolments => handleForValidAgent(block, mtdItId, nino, sessionId, enrolments, isSupportingAgent = true)
        )
        .recover {
          case _: AuthorisationException =>
            logger.warn(s"$agentAuthLogString - Agent does not have delegated authority for Client.")
            Redirect(AgentAuthErrorController.show)
          case e =>
            logger.error(s"$agentAuthLogString - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
            errorHandler.internalServerError()
        }
    case e =>
      logger.error(s"$agentAuthLogString - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
      Future(errorHandler.internalServerError())
  }

  private def handleForValidAgent[A](block: AuthorisationRequest[A] => Future[Result],
                                     mtdItId: String,
                                     nino: String,
                                     sessionId: String,
                                     enrolments: Enrolments,
                                     isSupportingAgent: Boolean)
                                    (implicit request: Request[A]): Future[Result] = {
    if (isSupportingAgent) {
      logger.warn(s"$agentAuthLogString - Secondary agent unauthorised")
      Future.successful(Redirect(controllers.errors.routes.SupportingAgentAuthErrorController.show))
    } else {
      EnrolmentHelper.enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
        case Some(arn) =>
          block(AuthorisationRequest(User(mtdItId, Some(arn), nino, sessionId, affinityGroup.toString, getTrueUserAgent, isSupportingAgent), request))
        case None =>
          logger.info(s"$agentAuthLogString - Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
          Future.successful(Redirect(controllers.errors.routes.YouNeedAgentServicesController.show))
      }
    }
  }

  private[actions] def agentAuthentication[A](block: AuthorisationRequest[A] => Future[Result],
                                              sessionId: String)
                                             (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    sessionDataService.getSessionData(sessionId).flatMap { sessionData =>
      authService
        .authorised(EnrolmentHelper.agentAuthPredicate(sessionData.mtditid))
        .retrieve(allEnrolments) {
          handleForValidAgent(block, sessionData.mtditid, sessionData.nino, sessionId, _, isSupportingAgent = false)
        }
        .recoverWith(agentRecovery(block, sessionData.mtditid, sessionData.nino, sessionId))
    }.recover {
      case _: MissingAgentClientDetails =>
        Redirect(appConfig.viewAndChangeEnterUtrUrl)
    }
  }
}
