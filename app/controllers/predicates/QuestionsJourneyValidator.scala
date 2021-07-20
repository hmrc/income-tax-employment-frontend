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
import models.question.Question.{Redirect, WithDependency}
import models.question.QuestionsJourney
import play.api.mvc.{Call, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class QuestionsJourneyValidator @Inject()(appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def validate[M : QuestionsJourney](currentPage: Call, cyaOpt: Future[Option[M]])(redirect: String)(block: M => Future[Result]): Future[Result] = {

    cyaOpt.flatMap {
      case Some(cya) =>
        QuestionsJourney[M].questions(cya).find(_.expectedPage == currentPage)
          .collect { case question: WithDependency if !question.isValid => Future(Redirect(question.redirectPage)) }
          .getOrElse(block(cya))
      case None => Future(Redirect(redirect))
    }
  }

}
