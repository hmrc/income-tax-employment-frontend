
package views.lumpSum

import controllers.employment.routes
import models.UserSessionDataRequest
import models.benefits.pages.TaxableLumpSumListPage
import models.employment.{TaxableLumpSumItemModel, TaxableLumpSumViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.UserSessionDataRequestBuilder.aUserSessionDataRequest
import support.builders.models.employment.TaxableLumpSumDataBuilder.aTaxableLumpSumData
import views.html.taxableLumpSum.TaxableLumpSumListView

class TaxableLumpSumListSpec extends ViewUnitTest {
  override protected val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  private val underTest = inject[TaxableLumpSumListView]


  "taxableLumpSumListView" should {
    "show populated table when user has lump sums" in {
      implicit val request: UserSessionDataRequest[AnyContent] = aUserSessionDataRequest
      implicit val messages: Messages = getMessages(false)
      val htmlFormat = underTest(TaxableLumpSumListPage(aTaxableLumpSumData, taxYear))
      implicit val document: Document = Jsoup.parse(htmlFormat.body)

      val table = document.getElementById("taxableLumpSumList")
      rowCheck("", "£100",  routes.EmploymentSummaryController.show(taxYear).url)
      rowCheck("", "£99",  routes.EmploymentSummaryController.show(taxYear).url)
      rowCheck("", "£98",  routes.EmploymentSummaryController.show(taxYear).url)

      table.html().length shouldBe 3
    }

    "show no table when user has no lump sums" in {
      implicit val request: UserSessionDataRequest[AnyContent] = aUserSessionDataRequest
      implicit val messages: Messages = getMessages(false)
      val htmlFormat = underTest(TaxableLumpSumListPage(TaxableLumpSumViewModel(Seq.empty[TaxableLumpSumItemModel]), taxYear))
      implicit val document: Document = Jsoup.parse(htmlFormat.body)

      elementsNotOnPageCheck("#taxableLumpSumList")
    }
  }
}
