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
import org.joda.time.DateTime
import play.api.Logger

import javax.inject.Inject

class InYearAction @Inject()(implicit val appConfig: AppConfig) {
  lazy val logger: Logger = Logger.apply(this.getClass)

  def inYear(taxYear: Int, now: DateTime): Boolean = {
    val defaultTaxYear = appConfig.defaultTaxYear

    if(!appConfig.taxYearErrorFeature) {
      return true
    }

    if(taxYear == defaultTaxYear) {
      now.isBefore(DateTime.parse(s"$defaultTaxYear-04-06"))
    } else {
      false
    }

  }
}

