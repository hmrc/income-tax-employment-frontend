
package controllers.lumpsum

import common.SessionValues
import config.AppConfig
import controllers.employment.CheckEmploymentDetailsController
import controllers.lumpSum.TaxableLumpSumListController
import models.AuthorisationRequest
import models.benefits.pages.TaxableLumpSumListPage
import models.employment.{EmploymentDetailsType, EmploymentType, TaxableLumpSumItemModel, TaxableLumpSumViewModel}
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.ws.WSResponse
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentType, status, stubMessagesControllerComponents}
import services.DefaultRedirectService
import support.ControllerUnitTest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.mocks.{MockActionsProvider, MockAppConfig, MockAuditService, MockAuthorisedAction, MockCheckEmploymentDetailsService, MockEmploymentSessionService, MockErrorHandler, MockNrsService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.PageUrls.{fullUrl, taxableLumpSumListUrl}
import utils.{EmploymentDatabaseHelper, InYearUtil, IntegrationTest, ViewHelpers}
import views.html.taxableLumpSum.TaxableLumpSumListView

import scala.concurrent.Future

class TaxableLumpSumListControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockEmploymentSessionService
  with MockAuditService
  with MockCheckEmploymentDetailsService
  with MockErrorHandler {

  private lazy val view = app.injector.instanceOf[TaxableLumpSumListView]

  private def underTest(mimic: Boolean = false, isEmploymentEOYEnabled: Boolean = true) = new TaxableLumpSumListController(
    stubMessagesControllerComponents,
    mockActionsProvider,
    view,
    new InYearUtil,
    mockErrorHandler
  )(new MockAppConfig().config(_mimicEmploymentAPICalls = mimic, isEmploymentEOYEnabled = isEmploymentEOYEnabled), ec)

  implicit private val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest.withHeaders())
  private val employmentId = "223AB12399"

  ".show" when {
    "return a fully populated page when all user has lump sums" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData)

      val result: Future[Result] = {
        underTest().show(taxYearEOY, employmentId = employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))
      }
      result.map( res => res.header.status shouldBe OK)
    }

    "return an empty page when all user has no lump sums" in {
      mockEndOfYearSessionData(taxYearEOY, employmentId, EmploymentDetailsType, anEmploymentUserData.copy(employment =
        anEmploymentCYAModel.copy(additionalInfoViewModel = None)))

      val result: Future[Result] = {
        underTest().show(taxYearEOY, employmentId = employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYearEOY.toString
        ))
      }
      result.map( res => res.header.status shouldBe OK)
    }
  }
}
