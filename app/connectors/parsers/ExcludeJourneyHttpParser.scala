package connectors.parsers

import models.APIErrorModel
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object ExcludeJourneyHttpParser extends APIParser {

  type ExcludeJourneyResponse = Either[APIErrorModel, Int]

  override val parserName: String = "ExcludeJourneyHttpParser"
  override val service: String = "income-tax-submission"

  implicit object ExcludeJourneyResponseReads extends HttpReads[ExcludeJourneyResponse] {
    override def read(method: String, url: String, response: HttpResponse): ExcludeJourneyResponse = {
      response.status match  {
        case NO_CONTENT => Right(NO_CONTENT)
        case BAD_REQUEST => handleAPIError(response)
        case INTERNAL_SERVER_ERROR => handleAPIError(response)
        case _ => handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}