
package controllers.lumpSum

import models.employment.TaxableLumpSumViewModel
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import utils.PageUrls.{fullUrl, taxableLumpSumListUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class TaxableLumpSumListControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  ".show" when {
    "return a fully populated page when all user has lump sums" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserData)
        urlGet(fullUrl(taxableLumpSumListUrl(taxYear, "employmentId")), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "return an empty page when all user has no lump sums" which {
      implicit lazy val result: WSResponse = {
        dropEmploymentDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertCyaData(anEmploymentUserData.copy(employment = anEmploymentCYAModel.copy(additionalInfoViewModel = None)))

        urlGet(fullUrl(taxableLumpSumListUrl(taxYear, "employmentId")), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }
  }

}
