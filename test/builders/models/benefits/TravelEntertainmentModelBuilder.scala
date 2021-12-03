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

package builders.models.benefits

import models.benefits.TravelEntertainmentModel

object TravelEntertainmentModelBuilder {

  val aTravelEntertainmentModel: TravelEntertainmentModel = TravelEntertainmentModel(
    sectionQuestion = Some(true),
    travelAndSubsistenceQuestion = Some(true),
    travelAndSubsistence = Some(100.00),
    personalIncidentalExpensesQuestion = Some(true),
    personalIncidentalExpenses = Some(200.00),
    entertainingQuestion = Some(true),
    entertaining = Some(300.00),
  )
}