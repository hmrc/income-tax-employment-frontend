package connectors

import config.AppConfig
import connectors.parsers.ExcludeJourneyHttpParser.{ExcludeJourneyResponse, ExcludeJourneyResponseReads}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExcludeJourneyConnector @Inject()(
                                         http: HttpClient,
                                         appConfig: AppConfig
                                       )(implicit ec: ExecutionContext) {

  def excludeJourney(journeyKey: String, taxYear: Int, nino: String)(implicit hc: HeaderCarrier): Future[ExcludeJourneyResponse] = {
    http.POST[JsObject, ExcludeJourneyResponse](
      appConfig.incomeTaxSubmissionBEBaseUrl + s"/income-tax/nino/$nino/sources/exclude-journey/$taxYear", Json.obj("journey" -> journeyKey)
    )
  }

}