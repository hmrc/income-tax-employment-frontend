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

import connectors.ExcludeJourneyConnector
import connectors.parsers.ExcludeJourneyHttpParser.ExcludeJourneyResponse
import models.User
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class ExcludeJourneyService @Inject()(connector: ExcludeJourneyConnector) {

  def excludeJourney(journeyKey: String, taxYear: Int, nino: String)(implicit user: User, hc: HeaderCarrier): Future[ExcludeJourneyResponse] = {
    connector.excludeJourney(journeyKey, taxYear, nino)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

}
