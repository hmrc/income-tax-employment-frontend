package controllers.employment

import common.SessionValues
import models.IncomeTaxUserData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class RemoveEmploymentControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY = taxYear - 1

  val employmentId = "001"
  val employment = ???

  def url(taxYear: Int, employmentId: String) = s"/income-through-software/return/employment-income/$taxYear/remove-employment?$employmentId"

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val h1Selector = "#main-content > div > div > form > div > fieldset > legend > header > h1"
    val thisWillAlsoTextSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val radioButtonSelector = "#main-content > div > div > form > div > fieldset > div"
    val yesRadioButton = "#value"
    val noRadioButton = "#value-no"

  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedParagraphText: String
    val continueButton: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val expectedParagraphText = "Are you sure you want to remove this employment?"
    val continueButton = "Continue"
  }

  object CommonExpectedCY {

  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String

  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedHeading = "Are you sure you want to remove this employment?"

  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedHeading = "Are you sure you want to remove this employment?"

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedHeading = "Are you sure you want to remove this employment?"

  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Are you sure you want to remove this employment?"
    val expectedHeading = "Are you sure you want to remove this employment?"

  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {

    import Selectors._

    userScenarios.foreach { user =>
      import user.commonExpectedResults._

      val common = user.commonExpectedResults
      val specific = user.specificExpectedResults.get

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the remove employment page" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(Some(fullEmploymentsModel(None))), nino, taxYear)
            urlGet(url(taxYearEOY, employmentId), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "status OK" in {
            result.status shouldBe 200
          }


          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(user.isWelsh)

          titleCheck(specific.expectedTitle)
          h1Check(specific.expectedHeading)
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedParagraphText, thisWillAlsoTextSelector)
          radioButtonCheck("Yes",1)
          radioButtonCheck("No",2)
        }

      }
    }
  }
}
