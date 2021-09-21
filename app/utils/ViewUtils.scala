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

import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.util.Try

object ViewUtils {

  case class EmploymentDataForView(fieldHeadings: String, fieldValues: Option[String], changeLink: Call, hiddenText: String)

  def convertBoolToYesOrNo(employmentField: Option[Boolean])(implicit messages: Messages): Option[String] = {
    employmentField.map {
      case true => messages("common.yes")
      case false => messages("common.no")
    }
  }

  def dateFormatter(date: String): Option[String] = {
    try {
      Some(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.UK))
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)))
    }
    catch {
      case _: Exception => None
    }
  }

  def getAgentDynamicContent(msgKey:String, isAgent:Boolean): String ={
    s"$msgKey.${if (isAgent) "agent" else "individual"}"
  }

  def summaryListRow(key: HtmlContent,
                     value: HtmlContent,
                     keyClasses: String = "govuk-!-width-one-third",
                     valueClasses: String = "govuk-!-width-one-third",
                     actionClasses: String = "govuk-!-width-one-third",
                     actions: Seq[(Call, String, Option[String])]): SummaryListRow = {
    SummaryListRow(

      key = Key(
        content = key,
        classes = keyClasses
      ),
      value = Value(
        content = value,
        classes = valueClasses
      ),
      actions = Some(Actions(
        items = actions.map { case (call, linkText, visuallyHiddenText) => ActionItem(
          href = call.url,
          content = ariaHiddenChangeLink(linkText),
          visuallyHiddenText = visuallyHiddenText
        )
        },
        classes = actionClasses
      ))
    )
  }

  def ariaHiddenChangeLink(linkText: String): HtmlContent = {
    HtmlContent(
      s"""<span aria-hidden="true">$linkText</span>"""
    )
  }

  def bigDecimalCurrency(value: String, currencySymbol: String = "£"): String = {
    Try(BigDecimal(value))
      .map(amount => currencySymbol + f"$amount%1.2f".replace(".00", ""))
      .getOrElse(value)
  }
}
