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

import common.SessionValues._
import config.AppConfig
import models.AuthorisationRequest
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxYearAction @Inject()(taxYear: Int, missingTaxYearReset: Boolean)(
  implicit val appConfig: AppConfig
) extends ActionRefiner[AuthorisationRequest, AuthorisationRequest]{

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  lazy val logger: Logger = Logger.apply(this.getClass)

  override def refine[A](request: AuthorisationRequest[A]): Future[Either[Result, AuthorisationRequest[A]]] = {
    implicit val implicitUser: AuthorisationRequest[A] = request

    val validClientTaxYears = request.session.get(VALID_TAX_YEARS)
    lazy val validTaxYears = validClientTaxYears.get.split(",").toSeq.map(_.toInt)

    if (validClientTaxYears.isDefined) {
      if (!appConfig.taxYearErrorFeature || validTaxYears.contains(taxYear)) {
        val sameTaxYear = request.session.get(TAX_YEAR).exists(_.toInt == taxYear)
        if (sameTaxYear || !missingTaxYearReset) {
          Future.successful(Right(request))
        } else {
          logger.info("[TaxYearAction][refine] Tax year provided is different than that in session. Redirecting to overview.")
          Future.successful(Left(
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
              .addingToSession(TAX_YEAR -> taxYear.toString)
          ))
        }
      } else {
        logger.info(s"[TaxYearAction][refine] Invalid tax year, redirecting to error page")
        Future.successful(Left(Redirect(controllers.errors.routes.TaxYearErrorController.show)))
      }
    } else {
      logger.info(s"[TaxYearAction][refine] Valid Tax Year list not in Session, return to start page")
      Future.successful(Left(Redirect(appConfig.incomeTaxSubmissionStartUrl(taxYear))))
    }
  }
}

object TaxYearAction {
  def taxYearAction(taxYear: Int, missingTaxYearReset: Boolean = true)(implicit appConfig: AppConfig): TaxYearAction =
    new TaxYearAction(taxYear, missingTaxYearReset)
}
