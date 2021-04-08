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

package utils

import play.api.i18n.Lang
import play.api.i18n.Messages
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.language.Language

import java.util.Locale
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.language.{Cy, En}

object LanguageUtil {
  def checkLanguage(implicit messages: Messages): Language ={
    messages.lang match {
      case Lang(Locale.ENGLISH) => En
      case _ => Cy
    }
  }
}
