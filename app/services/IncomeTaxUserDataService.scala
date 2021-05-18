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

package services

import config.AppConfig
import controllers.Assets.Redirect
import javax.inject.{Inject, Singleton}
import models.employment.AllEmploymentData
import models.User
import models.mongo.UserData
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import repositories.IncomeTaxUserDataRepository

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class IncomeTaxUserDataService @Inject()(incomeTaxUserDataRepository: IncomeTaxUserDataRepository,
                                         implicit private val appConfig: AppConfig,
                                         val messagesApi: MessagesApi) extends Logging {

  def findUserData(user: User[_], taxYear: Int, result: AllEmploymentData => Result)
                  (implicit ec: ExecutionContext): Future[Result] = {
    incomeTaxUserDataRepository.find(user, taxYear).map {
      case Some(UserData(_,_,_,_,Some(employmentData),_)) => result(employmentData)
      case _ =>
        logger.info(s"[IncomeTaxUserDataService][findUserData] " +
          s"No employment data found for user. SessionId: ${user.sessionId}")
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }
}
