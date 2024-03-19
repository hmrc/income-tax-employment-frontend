/*
 * Copyright 2023 HM Revenue & Customs
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

package filters

import org.apache.pekko.stream.Materializer
import com.google.inject.Inject
import play.api.http.HeaderNames
import play.api.mvc._
import uk.gov.hmrc.http.{SessionKeys, HeaderNames => HMRCHeaderNames}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class SessionIdFilter(override val mat: Materializer,
                      uuid: => UUID,
                      implicit val ec: ExecutionContext,
                      val cookieBaker: SessionCookieBaker,
                      cookieHeaderEncoding: CookieHeaderEncoding
                     ) extends Filter {

  @Inject
  def this(mat: Materializer, ec: ExecutionContext, cb: SessionCookieBaker, cookieHeaderEncoding: CookieHeaderEncoding) = {
    this(mat, UUID.randomUUID(), ec, cb, cookieHeaderEncoding)
  }

  override def apply(requestToResult: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {

    lazy val sessionId: String = s"session-$uuid"

    if (request.session.get(SessionKeys.sessionId).isEmpty) {

      val cookies: String = {
        val session: Session = request.session + (SessionKeys.sessionId -> sessionId)
        val cookies = request.cookies ++ Seq(cookieBaker.encodeAsCookie(session))
        cookieHeaderEncoding.encodeCookieHeader(cookies.toSeq)
      }

      val originalRequestHeaders = request.headers.remove(HeaderNames.COOKIE).headers

      val headers = Headers(
        "sessionId" -> sessionId,
        HMRCHeaderNames.xSessionId -> sessionId,
        HeaderNames.COOKIE -> cookies
      ).add(originalRequestHeaders: _*)

      requestToResult(request.withHeaders(headers)).map(_.addingToSession(SessionKeys.sessionId -> sessionId)(request.withHeaders(headers)))
    } else {
      requestToResult(request)
    }
  }
}
