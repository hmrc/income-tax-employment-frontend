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

import common.SessionValues
import config.AppConfig
import controllers.Assets.Redirect
import models.User
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxYearAction @Inject()(taxYear: Int)(
  appConfig: AppConfig,
  val mcc: MessagesControllerComponents
) extends ActionRefiner[User, User] with I18nSupport {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  lazy val logger: Logger = Logger.apply(this.getClass)
  implicit val config: AppConfig = appConfig
  implicit val messagesApi: MessagesApi = mcc.messagesApi

  override def refine[A](request: User[A]): Future[Either[Result, User[A]]] = {
    Future.successful(
      if (taxYear == appConfig.defaultTaxYear || !appConfig.taxYearErrorFeature) {
        Right(request)
      } else {
        logger.info(s"Invalid tax year, adding default tax year to session")
        Left(Redirect(controllers.errors.routes.TaxYearErrorController.show())
          .addingToSession(SessionValues.TAX_YEAR -> config.defaultTaxYear.toString)(request))
      }
    )
  }

}

object TaxYearAction {
  def taxYearAction(taxYear: Int)(
    implicit appConfig: AppConfig, messages: MessagesControllerComponents
  ): TaxYearAction = new TaxYearAction(taxYear)(appConfig, messages)
}



