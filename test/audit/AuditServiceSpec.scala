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

package audit

import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.UnitTestWithApp

import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends UnitTestWithApp {

  private trait Test {
    val mockedAppName = "some-app-name"
    val mockAuditConnector: AuditConnector = mock[AuditConnector]
    val mockConfig: Configuration = mock[Configuration]

    (mockConfig.get[String](_: String)(_: play.api.ConfigLoader[String]))
      .expects(*, *)
      .returns(mockedAppName)

    lazy val target = new AuditService(mockAuditConnector, mockConfig)
  }

  "AuditService" when {
    "auditing an event" should {
      val auditType = "Type"
      val transactionName = "Name"
      val eventDetails = "Details"
      val expected: Future[AuditResult] = Future.successful(Success)
      "return a successful audit result" in new Test {

        (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(expected)

        val event = AuditModel(auditType, transactionName, eventDetails)
        target.sendAudit(event) shouldBe expected
      }



      "generates an event with the correct auditSource" in new Test {
        (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(_: HeaderCarrier, _: ExecutionContext))
          .expects(
            where {
              (eventArg: ExtendedDataEvent, _: HeaderCarrier, _: ExecutionContext) =>
                eventArg.auditSource == mockedAppName
            }
          )
          .returns(expected)

        val event = AuditModel(auditType, transactionName, eventDetails)

        target.sendAudit(event)
      }

      "generates an event with the correct auditType" in new Test {
        (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(_: HeaderCarrier, _: ExecutionContext))
          .expects(
            where {
              (eventArg: ExtendedDataEvent, _: HeaderCarrier, _: ExecutionContext) =>
                eventArg.auditType == auditType
            }
          )
          .returns(expected)

        val event = AuditModel(auditType, transactionName, eventDetails)

        target.sendAudit(event)
      }

      "generates an event with the correct details" in new Test {
        (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(_: HeaderCarrier, _: ExecutionContext))
          .expects(
            where {
              (eventArg: ExtendedDataEvent, _: HeaderCarrier, _: ExecutionContext) =>
                eventArg.detail == Json.toJson(eventDetails)
            }
          )
          .returns(expected)

        val event = AuditModel(auditType, transactionName, eventDetails)

        target.sendAudit(event)
      }

      "generates an event with the correct transactionName" in new Test {
        (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(_: HeaderCarrier, _: ExecutionContext))
          .expects(
            where {
              (eventArg: ExtendedDataEvent, _: HeaderCarrier, _: ExecutionContext) =>
                eventArg.tags.exists(tag => tag == "transactionName" -> transactionName)
            }
          )
          .returns(expected)

        val event = AuditModel(auditType, transactionName, eventDetails)

        target.sendAudit(event)
      }
    }
  }
}


