
package models.pages

import controllers.employment.routes
import models.benefits.pages.TaxableLumpSumListPage
import models.employment.{TaxableLumpSumItemModel, TaxableLumpSumViewModel}
import play.api.i18n.Messages
import support.ViewUnitTest
import support.builders.models.employment.TaxableLumpSumDataBuilder.aTaxableLumpSumData

class TaxableLumpSumListPageSpec extends ViewUnitTest {
  override protected val userScenarios: Seq[UserScenario[_, _]] = Seq.empty


  "TaxableLumpSumListPage" should {
    "transform taxableLumpSumViewModel into the correct number of rows, with the correct content " in {
      implicit val messages: Messages = getMessages(false)

      val table  = TaxableLumpSumListPage(aTaxableLumpSumData, taxYearEOY)
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

      val table = TaxableLumpSumListPage(TaxableLumpSumViewModel(Seq.empty[TaxableLumpSumItemModel]), taxYearEOY)
      table.rows.length shouldBe 0
    }
  }
}
