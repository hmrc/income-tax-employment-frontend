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

package models.mongo

import controllers.employment.routes.{OtherPaymentsAmountController, CheckEmploymentDetailsController, OtherPaymentsController}
import models.question.{Question, QuestionsJourney}
import models.question.Question.{WithDependency, WithoutDependency}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, Json}
import play.api.mvc.Call
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class EmploymentUserData(sessionId: String,
                              mtdItId: String,
                              nino: String,
                              taxYear: Int,
                              employmentId: String,
                              isPriorSubmission: Boolean,
                              employment: EmploymentCYAModel,
                              lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object EmploymentUserData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val formats: Format[EmploymentUserData] = Json.format[EmploymentUserData]

  def journey(taxYear: Int, employmentId: String): QuestionsJourney[EmploymentUserData] = new QuestionsJourney[EmploymentUserData] {
    override def firstPage: Call = CheckEmploymentDetailsController.show(taxYear, employmentId)

    override def questions(m: EmploymentUserData): Set[Question] = {
      val model = m.employment.employmentDetails
      Set(
        WithoutDependency(model.tipsAndOtherPaymentsQuestion, OtherPaymentsController.show(taxYear, employmentId)),
        WithDependency(model.tipsAndOtherPayments, model.tipsAndOtherPaymentsQuestion,
          OtherPaymentsAmountController.show(taxYear, employmentId), OtherPaymentsController.show(taxYear, employmentId))
      )
    }
  }
}
