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

package controllers.predicates

import config.AppConfig
import play.api.Logger

import java.time.{LocalDateTime, ZoneId}
import javax.inject.Inject

class InYearAction @Inject()(implicit val appConfig: AppConfig) {

  lazy val logger: Logger = Logger.apply(this.getClass)

  val cutOffDay = 6
  val cutOffMonth = 4
  val cutOffHour = 0
  val cutOffMinute = 0

  def inYear(taxYear: Int, now: LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))): Boolean = {
    def zonedDateTime(time: LocalDateTime) = time.atZone(ZoneId.of("Europe/London")).toLocalDateTime
    val endOfYearCutOffDate: LocalDateTime = LocalDateTime.of(taxYear, cutOffMonth, cutOffDay, cutOffHour ,cutOffMinute)
    val isNowBefore: Boolean = zonedDateTime(now).isBefore(zonedDateTime(endOfYearCutOffDate))

    if(isNowBefore) {
      logger.info(s"[InYearAction][inYear] Employment pages for this request will be in year")
    }
    else {
      logger.info(s"[InYearAction][inYear] Employment pages for this request will be for end of year")
    }

    isNowBefore
  }

}

