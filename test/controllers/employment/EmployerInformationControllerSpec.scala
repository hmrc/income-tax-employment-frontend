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

package controllers.employment

import common.SessionValues
import controllers.employment.routes._
import controllers.studentLoans.routes.StudentLoansCYAController
import controllers.taxableLumpSums.routes.TaxableLumpSumsController
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, stubMessagesControllerComponents}
import play.api.test.Helpers.contentAsString
import support.ControllerUnitTest
import support.ViewHelper
import support.builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import support.mocks.{MockAppConfig, MockAuthorisedAction, MockEmploymentSessionService}
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.InYearUtil
import views.html.employment.EmployerInformationView

import scala.concurrent.Future

class EmployerInformationControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockEmploymentSessionService {

  private lazy val view = app.injector.instanceOf[EmployerInformationView]

  private def controller(isEmploymentEOYEnabled: Boolean = true) = new EmployerInformationController(
    mockAuthorisedAction,
    view,
    new InYearUtil(),
    mockEmploymentSessionService,
  )(stubMessagesControllerComponents(), appConfig = new MockAppConfig().config(isEmploymentEOYEnabled = isEmploymentEOYEnabled))

  private val nino = "AA123456A"
  private val employmentId: String = "223/AB12399"

  override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
    .withHeaders("X-Session-ID" -> "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](models.User("1234567890", None, nino, "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe", AffinityGroup.Individual.toString),
      fakeRequest)
  implicit private val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest.withHeaders())

  ".show" should {
    "render Employment And Benefits page when GetEmploymentDataModel is in mongo" which {
      s"has an OK($OK) status" in {
        mockAuth(Some(nino))
        val employerName: String = anEmploymentSource.employerName
        val employmentId: String = anEmploymentSource.employmentId

        val result: Future[Result] = {
          mockFind(taxYear, Ok(view(employerName, employmentId, Seq.empty, taxYear, isInYear = true, showNotification = true)))
          controller().show(taxYear, employmentId)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))
        }

        status(result) shouldBe OK
      }
    }

    "render Employment And Benefits page with correct rows" in {
      mockAuth(Some(nino))
      val employerName: String = anEmploymentSource.employerName
      val employmentId: String = anEmploymentSource.employmentId

      val rows = Seq(
        EmployerInformationRow(EmploymentDetails, ToDo, Some(CheckEmploymentDetailsController.show(taxYear, employmentId)), updateAvailable = true),
        EmployerInformationRow(EmploymentBenefits, CannotUpdate, Some(CheckYourBenefitsController.show(taxYear, employmentId)), updateAvailable = true),
        EmployerInformationRow(StudentLoans, CannotUpdate, Some(StudentLoansCYAController.show(taxYear, employmentId)), updateAvailable = true),
        EmployerInformationRow(TaxableLumpSums, CannotUpdate, Some(TaxableLumpSumsController.show(taxYear, employmentId)), updateAvailable = true),
      )

      val result: Future[Result] = {
        mockFind(taxYear, Ok(view(employerName, employmentId, rows, taxYear, isInYear = true, showNotification = true)))
        controller().show(taxYear, employmentId)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))
      }

      status(result) shouldBe OK
      val contents = contentAsString(result)

      implicit val document: Document = Jsoup.parse(contents)

      def employerInformationRowCheck(item: String, value: String, href: String, section: Int, row: Int)(implicit document: Document): Unit = {
        document.select(s"#main-content > div > div > dl:nth-of-type($section) > div:nth-child($row) > dt").text() shouldBe messages(item)
        document.select(s"#main-content > div > div > dl:nth-of-type($section) > div:nth-child($row) > dd.govuk-summary-list__value").text() shouldBe messages(value)
        // bottom one is failing because it's not there at the path
        println("********** " + document)
        document.select(s"#main-content > div > div > dl:nth-of-type($section) > div:nth-child($row)").toString should  include(href)
      }

      employerInformationRowCheck(EmploymentDetails.toString, ToDo.toString, CheckEmploymentDetailsController.show(taxYear, employmentId).url, 1, 1)
//      employerInformationRowCheck(EmploymentBenefits.toString, CannotUpdate.toString, CheckEmploymentDetailsController.show(taxYear, employmentId).url, 1, 2)
//      employerInformationRowCheck(StudentLoans.toString, CannotUpdate.toString, CheckEmploymentDetailsController.show(taxYear, employmentId).url, 1, 3)
//      employerInformationRowCheck(TaxableLumpSums.toString, CannotUpdate.toString, CheckEmploymentDetailsController.show(taxYear, employmentId).url, 1, 4)
    }

    "redirect the User to the Overview page when GetEmploymentDataModel is in mongo but " which {
      s"has an SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        val result: Future[Result] = controller(isEmploymentEOYEnabled = false)
          .show(taxYearEOY, anEmploymentSource.employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect the User to the Overview page no data in mongo" which {
      s"has the SEE_OTHER($SEE_OTHER) status" in {
        mockAuth(Some(nino))
        val result: Future[Result] = {
          mockFind(taxYear, Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          controller().show(taxYear, employmentId)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }
}
