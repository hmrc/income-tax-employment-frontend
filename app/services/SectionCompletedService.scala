/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.SectionCompletedConnector
import models.mongo.JourneyAnswers
import org.apache.pekko.Done
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class SectionCompletedService @Inject()(connector: SectionCompletedConnector) {
  def get(mtdItId: String, taxYear: Int, journey: String)(implicit hc: HeaderCarrier): Future[Option[JourneyAnswers]] = {
    connector.get(mtdItId, taxYear, journey)
  }

  def set(answers: JourneyAnswers)(implicit hc: HeaderCarrier): Future[Done] = {
    connector.set(answers)
  }

  def keepAlive(mtdItId: String, taxYear: Int, journey: String)(implicit hc: HeaderCarrier): Future[Done] = {
    connector.keepAlive(mtdItId, taxYear, journey)
  }
}