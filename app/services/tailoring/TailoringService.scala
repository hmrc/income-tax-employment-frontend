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

package services.tailoring

import connectors.TailoringDataConnector
import connectors.parsers.ClearExcludedJourneysHttpParser.ClearExcludedJourneysResponse
import connectors.parsers.GetExcludedJourneysHttpParser.ExcludedJourneysResponse
import connectors.parsers.PostExcludedJourneyHttpParser.PostExcludedJourneyResponse
import models.employment.AllEmploymentData
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import services.DeleteOrIgnoreExpensesService
import services.employment.RemoveEmploymentService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TailoringService @Inject()(tailoringDataConnector: TailoringDataConnector,
                                 removeEmploymentService: RemoveEmploymentService,
                                 deleteOrIgnoreExpensesService: DeleteOrIgnoreExpensesService,
                                 implicit val ec: ExecutionContext) {

  def getExcludedJourneys(taxYear: Int, nino: String, mtditid: String)(implicit hc: HeaderCarrier): Future[ExcludedJourneysResponse] = {
    tailoringDataConnector.getExcludedJourneys(taxYear, nino)(hc.withExtraHeaders("mtditid" -> mtditid))
  }

  def clearExcludedJourney(taxYear: Int, nino: String, mtditid: String)
                          (implicit hc: HeaderCarrier): Future[ClearExcludedJourneysResponse] = {
    tailoringDataConnector.clearExcludedJourney(taxYear, nino)(hc.withExtraHeaders("mtditid" -> mtditid))
  }

  def postExcludedJourney(taxYear: Int, nino: String, mtditid: String)
                          (implicit hc: HeaderCarrier): Future[PostExcludedJourneyResponse] = {
    tailoringDataConnector.postExcludedJourney(taxYear, nino)(hc.withExtraHeaders("mtditid" -> mtditid))
  }

  def deleteOrIgnoreAllEmployment(employmentData: AllEmploymentData, taxYear: Int, user: User)
                                 (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Unit]] = {
    val AllEmploymentSourceIds: Seq[String] = employmentData.latestEOYEmployments.map(_.employmentId).distinct
    val eventualResult: Future[Seq[Either[APIErrorModel, Unit]]] = Future.sequence(AllEmploymentSourceIds.map { sourceId =>
      removeEmploymentService.deleteOrIgnoreEmployment(employmentData, taxYear, sourceId, user)
      deleteOrIgnoreExpensesService.deleteOrIgnoreExpenses(user, employmentData, taxYear)
    })

    eventualResult.map { results =>
      if (results.exists(_.isLeft)) {
        Left(APIErrorModel(INTERNAL_SERVER_ERROR,
          APIErrorBodyModel("[TailoringService] Delete/Ignore ALL Employment Error", "Error Deleting/Ignore all Employment/ExpensesData")))
      } else {
        Right(())
      }
    }
  }

}
