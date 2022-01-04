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

package controllers.predicates

import common.{EnrolmentIdentifiers, EnrolmentKeys, SessionValues}
import config.AppConfig
import javax.inject.Inject
import models.User
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject()(appConfig: AppConfig)
                                (implicit val authService: AuthService,
                                 val mcc: MessagesControllerComponents
                                ) extends ActionBuilder[User, AnyContent] with I18nSupport {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  lazy val logger: Logger = Logger.apply(this.getClass)
  implicit val config: AppConfig = appConfig
  implicit val messagesApi: MessagesApi = mcc.messagesApi

  override def parser: BodyParser[AnyContent] = mcc.parsers.default

  val minimumConfidenceLevel: Int = ConfidenceLevel.L200.level

  override def invokeBlock[A](request: Request[A], block: User[A] => Future[Result]): Future[Result] = {

    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request,request.session)

    authService.authorised.retrieve(affinityGroup) {
      case Some(AffinityGroup.Agent) => agentAuthentication(block)(request, headerCarrier)
      case Some(affinityGroup) => individualAuthentication(block, affinityGroup)(request, headerCarrier)
    } recover {
      case _: NoActiveSession =>
        logger.info(s"[AuthorisedAction][invokeBlock] - No active session. Redirecting to ${appConfig.signInUrl}")
        Redirect(appConfig.signInUrl)
      case _: AuthorisationException =>
        logger.info(s"[AuthorisedAction][invokeBlock] - User failed to authenticate")
        Redirect(controllers.errors.routes.UnauthorisedUserErrorController.show)
    }
  }

  def sessionId(implicit request: Request[_], hc: HeaderCarrier): Option[String] = {
    lazy val key = "sessionId"
    if (hc.sessionId.isDefined) {
      hc.sessionId.map(_.value)
    } else if (request.headers.get(key).isDefined) {
      request.headers.get(key)
    } else {
      None
    }
  }

  def individualAuthentication[A](block: User[A] => Future[Result], affinityGroup: AffinityGroup)
                                 (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {

    authService.authorised.retrieve(allEnrolments and confidenceLevel) {

      case enrolments ~ userConfidence if userConfidence.level >= minimumConfidenceLevel =>
        val optionalMtdItId: Option[String] = enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments)
        val optionalNino: Option[String] = enrolmentGetIdentifierValue(EnrolmentKeys.nino, EnrolmentIdentifiers.nino, enrolments)

        (optionalMtdItId, optionalNino) match {
          case (Some(mtdItId), Some(nino)) =>

            sessionId.fold {
              logger.info(s"[AuthorisedAction][individualAuthentication] - No session id in request")
              Future.successful(Redirect(appConfig.signInUrl))
            } { sessionId =>
              block(User(mtdItId, None, nino, sessionId, affinityGroup.toString))
            }

          case (_, None) =>
            logger.info(s"[AuthorisedAction][individualAuthentication] - No active session. Redirecting to ${appConfig.signInUrl}")
            Future.successful(Redirect(appConfig.signInUrl))
          case (None, _) =>
            logger.info(s"[AuthorisedAction][individualAuthentication] - User has no MTD IT enrolment. Redirecting user to sign up for MTD.")
            Future.successful(Redirect(controllers.errors.routes.IndividualAuthErrorController.show))
        }
      case _ =>
        logger.info("[AuthorisedAction][individualAuthentication] User has confidence level below 200, routing user to IV uplift.")
        Future(Redirect(appConfig.incomeTaxSubmissionIvRedirect))
    }
  }

  private[predicates] def agentAuthentication[A](block: User[A] => Future[Result])
                                                (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {

    lazy val agentDelegatedAuthRuleKey = "mtd-it-auth"

    lazy val agentAuthPredicate: String => Enrolment = identifierId =>
      Enrolment(EnrolmentKeys.Individual)
        .withIdentifier(EnrolmentIdentifiers.individualId, identifierId)
        .withDelegatedAuthRule(agentDelegatedAuthRuleKey)

    val optionalNino = request.session.get(SessionValues.CLIENT_NINO)
    val optionalMtdItId = request.session.get(SessionValues.CLIENT_MTDITID)

    (optionalMtdItId, optionalNino) match {
      case (Some(mtdItId), Some(nino)) =>
        authService
          .authorised(agentAuthPredicate(mtdItId))
          .retrieve(allEnrolments) { enrolments =>

            enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
              case Some(arn) =>

                sessionId.fold {
                  logger.info(s"[AuthorisedAction][agentAuthentication] - No session id in request")
                  Future(Redirect(appConfig.signInUrl))
                } { sessionId =>
                  block(User(mtdItId, Some(arn), nino, sessionId, AffinityGroup.Agent.toString))
                }

              case None =>
                logger.info("[AuthorisedAction][agentAuthentication] Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
                Future.successful(Redirect(controllers.errors.routes.YouNeedAgentServicesController.show))
            }
          } recover {
          case _: NoActiveSession =>
            logger.info(s"[AuthorisedAction][agentAuthentication] - No active session. Redirecting to ${appConfig.signInUrl}")
            Redirect(appConfig.signInUrl)
          case ex: AuthorisationException =>
            logger.info(s"[AuthorisedAction][agentAuthentication] - Agent does not have delegated authority for Client.")
            Redirect(controllers.errors.routes.AgentAuthErrorController.show)
        }
      case (mtditid, nino) =>
        logger.info(s"[AuthorisedAction][agentAuthentication] - Agent does not have session key values. " +
          s"Redirecting to view & change. MTDITID missing:${mtditid.isEmpty}, NINO missing:${nino.isEmpty}")
        Future.successful(Redirect(appConfig.viewAndChangeEnterUtrUrl))
    }
  }

  private[predicates] def enrolmentGetIdentifierValue(
                                                       checkedKey: String,
                                                       checkedIdentifier: String,
                                                       enrolments: Enrolments
                                                     ): Option[String] = enrolments.enrolments.collectFirst {
    case Enrolment(`checkedKey`, enrolmentIdentifiers, _, _) => enrolmentIdentifiers.collectFirst {
      case EnrolmentIdentifier(`checkedIdentifier`, identifierValue) => identifierValue
    }
  }.flatten

}
