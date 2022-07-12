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

package actions

import config.AppConfig
import models.AuthorisationRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import utils.InYearUtil

import scala.concurrent.{ExecutionContext, Future}

case class InYearFilterAction(taxYear: Int,
                              inYearUtil: InYearUtil,
                              appConfig: AppConfig)
                             (implicit ec: ExecutionContext) extends ActionFilter[AuthorisationRequest] {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](request: AuthorisationRequest[A]): Future[Option[Result]] = Future.successful {
    if (!inYearUtil.inYear(taxYear)) {
      Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      None
    }
  }
}
