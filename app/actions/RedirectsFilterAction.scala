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

import models.UserSessionDataRequest
import models.redirects.ConditionalRedirect
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import utils.RedirectsMatcherUtils

import scala.concurrent.{ExecutionContext, Future}

case class RedirectsFilterAction(redirectsMatcherUtils: RedirectsMatcherUtils,
                                 controllerName: String,
                                 taxYear: Int,
                                 employmentId: String)
                                (implicit ec: ExecutionContext) extends ActionFilter[UserSessionDataRequest] with Logging {
  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](input: UserSessionDataRequest[A]): Future[Option[Result]] = Future.successful {
    val redirects = redirectsMatcherUtils.matchToRedirects(controllerName, taxYear, employmentId, input.employmentUserData.employment)
    val optionalRedirect = redirects.collectFirst {
      case ConditionalRedirect(condition, result, Some(hasPriorBenefits))
        if condition && hasPriorBenefits == input.employmentUserData.hasPriorBenefits => Redirect(result)
      case ConditionalRedirect(condition, result, None) if condition => Redirect(result)
    }

    if (optionalRedirect.isDefined) {
      val redirectLocation = optionalRedirect.get.header.headers.getOrElse("Location", "")
      logger.info(message = s"[calculateRedirect] Some data is missing / in the wrong state for the requested page. Routing to $redirectLocation")
    }

    optionalRedirect
  }
}
