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

import java.time.{LocalDateTime, ZoneId}

import config.AppConfig
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect

import scala.concurrent.Future

class InYearAction @Inject()(implicit val appConfig: AppConfig) {

  lazy val logger: Logger = Logger.apply(this.getClass)

  private val taxYearStartDay = 6
  private val taxYearStartMonth = 4
  private val taxYearStartHour = 0
  private val taxYearStartMinute = 0

  def inYear(taxYear: Int, now: LocalDateTime = LocalDateTime.now): Boolean = {
    def zonedDateTime(time: LocalDateTime): LocalDateTime = time.atZone(ZoneId.of("Europe/London")).toLocalDateTime
    val endOfYearCutOffDate: LocalDateTime = LocalDateTime.of(taxYear, taxYearStartMonth, taxYearStartDay, taxYearStartHour ,taxYearStartMinute)
    val isNowBefore: Boolean = zonedDateTime(now).isBefore(zonedDateTime(endOfYearCutOffDate))

    if(isNowBefore) {
      logger.info(s"[InYearAction][inYear] Employment pages for this request will be in year.")
    } else {
      logger.info(s"[InYearAction][inYear] Employment pages for this request will not be in year.")
    }

    isNowBefore
  }

  def notInYear(taxYear: Int, now: LocalDateTime = LocalDateTime.now)(block: Future[Result]): Future[Result] = {
    if(!inYear(taxYear, now)){
      block
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

}
