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

package models.pages

import controllers.employment.routes
import models.benefits.pages.TaxableLumpSumListPage
import models.otheremployment.session.{OtherEmploymentIncomeCYAModel, TaxableLumpSum}
import play.api.i18n.Messages
import support.ViewUnitTest
import support.builders.models.otheremployment.session.OtherEmploymentIncomeCYAModelBuilder.anOtherEmploymentIncomeCYAModel

class TaxableLumpSumListPageSpec extends ViewUnitTest {
  override protected val userScenarios: Seq[UserScenario[_, _]] = Seq.empty


  "TaxableLumpSumListPage" should {
    "transform taxableLumpSumViewModel into the correct number of rows, with the correct content " in {
      implicit val messages: Messages = getMessages(false)

      val table  = TaxableLumpSumListPage(anOtherEmploymentIncomeCYAModel, taxYearEOY)
      table.rows.length shouldBe 3
      table.rows.head.amount shouldBe "£100"
      table.rows.head.call shouldBe routes.EmploymentSummaryController.show(taxYearEOY) //todo redirect to appropriate page
      table.rows(1).amount shouldBe "£99"
      table.rows(1).call shouldBe routes.EmploymentSummaryController.show(taxYearEOY) //todo redirect to appropriate page
      table.rows(2).amount shouldBe "£98"
      table.rows(2).call shouldBe routes.EmploymentSummaryController.show(taxYearEOY) //todo redirect to appropriate page
    }

    "show no table when user has no lump sums" in {
      implicit val messages: Messages = getMessages(false)

      val table = TaxableLumpSumListPage(OtherEmploymentIncomeCYAModel(Seq.empty[TaxableLumpSum]), taxYearEOY)
      table.rows.length shouldBe 0
    }
  }
}
