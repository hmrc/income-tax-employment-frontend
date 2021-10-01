/*
 * Copyright 2021 HM Revenue & Customs
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

  import forms.YesNoForm
  import models.User
  import models.employment.{BenefitsViewModel, CarVanFuelModel}
  import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
  import org.jsoup.Jsoup
  import org.jsoup.nodes.Document
  import play.api.http.HeaderNames
  import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
  import play.api.libs.ws.WSResponse
  import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}
  import controllers.employment.routes.CheckYourBenefitsController

  class ReceivedOwnCarMileageBenefitControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

    val taxYearEOY: Int = taxYear - 1
    val employmentId: String = "001"

    private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

    private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
      EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = isPrior, employmentCyaModel)

    def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
      EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

    def fullCarVanFuelModel: CarVanFuelModel =
      CarVanFuelModel(
        carVanFuelQuestion = Some(true),
        carQuestion = Some(true),
        car = Some(100),
        carFuelQuestion = Some(true),
        carFuel = Some(200),
        vanQuestion = Some(true),
        van = Some(300),
        vanFuelQuestion = Some(true),
        vanFuel = Some(400),
        mileageQuestion = Some(true),
        mileage = Some(400)
      )

    def benefits(carModel: CarVanFuelModel): BenefitsViewModel = BenefitsViewModel(Some(carModel), isUsingCustomerData = true)

    private def receivedMileageBenefitPage(taxYear: Int) = s"$appUrl/$taxYear/benefits/mileage?employmentId=$employmentId"

    val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/mileage?employmentId=$employmentId"

    object Selectors {
      val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
      val continueButtonSelector: String = "#continue"
      val continueButtonFormSelector: String = "#main-content > div > div > form"
      val yesSelector = "#value"
      val noSelector = "#value-no"
      val p1Selector = "#main-content > div > div > form > div > fieldset > legend > p.govuk-body"
      val p2Selector = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(3)"
    }

    trait SpecificExpectedResults {
      val expectedTitle: String
      val expectedH1: String
      val expectedP1: String
      val expectedP2: String
      val expectedErrorTitle: String
      val expectedError: String
    }

    trait CommonExpectedResults {
      val expectedCaption: Int => String
      val expectedButtonText: String
      val yesText: String
      val noText: String
    }

    object ExpectedIndividualEN extends SpecificExpectedResults {
      val expectedTitle = "Did you get a mileage benefit for using your own car for work?"
      val expectedH1 = "Did you get a mileage benefit for using your own car for work?"
      val expectedP1 = "We only need to know about payments made above our ‘approved amount‘. If you have payments above the ‘approved amount‘, they should be recorded in section E of your P11D form."
      val expectedP2 = "Check with your employer if you are unsure."
      val expectedErrorTitle = s"Error: $expectedTitle"
      val expectedError = "Select yes if you got a mileage benefit for using your own car for work"
    }

    object ExpectedIndividualCY extends SpecificExpectedResults {
      val expectedTitle = "Did you get a mileage benefit for using your own car for work?"
      val expectedH1 = "Did you get a mileage benefit for using your own car for work?"
      val expectedP1 = "We only need to know about payments made above our ‘approved amount‘. If you have payments above the ‘approved amount‘, they should be recorded in section E of your P11D form."
      val expectedP2 = "Check with your employer if you are unsure."
      val expectedErrorTitle = s"Error: $expectedTitle"
      val expectedError = "Select yes if you got a mileage benefit for using your own car for work"
    }

    object ExpectedAgentEN extends SpecificExpectedResults {
      val expectedTitle = "Did your client get a mileage benefit for using their own car for work?"
      val expectedH1 = "Did your client get a mileage benefit for using their own car for work?"
      val expectedP1 = "We only need to know about payments made above our ‘approved amount‘. If your client has payments above the ‘approved amount‘, they should be recorded in section E of their P11D form."
      val expectedP2 = "Check with your client’s employer if you are unsure."
      val expectedErrorTitle = s"Error: $expectedTitle"
      val expectedError = "Select yes if your client got a mileage benefit for using your own car for work"
    }

    object ExpectedAgentCY extends SpecificExpectedResults {
      val expectedTitle = "Did your client get a mileage benefit for using their own car for work?"
      val expectedH1 = "Did your client get a mileage benefit for using their own car for work?"
      val expectedP1 = "We only need to know about payments made above our ‘approved amount‘. If your client has payments above the ‘approved amount‘, they should be recorded in section E of their P11D form."
      val expectedP2 = "Check with your client’s employer if you are unsure."
      val expectedErrorTitle = s"Error: $expectedTitle"
      val expectedError = "Select yes if your client got a mileage benefit for using your own car for work"
    }

    object CommonExpectedEN extends CommonExpectedResults {
      val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
      val expectedButtonText = "Continue"
      val yesText = "Yes"
      val noText = "No"
    }

    object CommonExpectedCY extends CommonExpectedResults {
      val expectedCaption: Int => String = (taxYear: Int) => s"Employment for 6 April ${taxYear - 1} to 5 April $taxYear"
      val expectedButtonText = "Continue"
      val yesText = "Yes"
      val noText = "No"
    }

    val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
      Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
        UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
        UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
        UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
    }

    ".show" should {

      userScenarios.foreach { user =>
        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

          "Render the 'Did you get a mileage benefit' page with correct content when no benefits data and no pre-population" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(receivedMileageBenefitPage(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, Some(false))
            radioButtonCheck(noText, 2, Some(false))
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)
          }

          "Render the 'Did you get a mileage benefit' page with correct content, cya data and the yes value pre-populated" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(receivedMileageBenefitPage(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
            textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
            radioButtonCheck(yesText, 1, Some(true))
            radioButtonCheck(noText, 2, Some(false))
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)
          }

          "Render the 'Did you get a mileage benefit' page with correct content, " +
            "and maintain yes value pre-populated when chosen even when no mileage amount has yet been entered (e.g. back)" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
                Some(benefits(fullCarVanFuelModel.copy(mileage = None))))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(receivedMileageBenefitPage(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
            textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
            radioButtonCheck(yesText, 1, Some(true))
            radioButtonCheck(noText, 2, Some(false))
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)
          }

          "Render the 'Did you get a mileage benefit' page with correct content, cya data and the no value pre-populated" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
                Some(benefits(fullCarVanFuelModel.copy(mileageQuestion = Some(false), mileage = None))))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(receivedMileageBenefitPage(taxYearEOY), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            textOnPageCheck(expectedCaption(taxYearEOY), captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
            textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
            radioButtonCheck(yesText, 1, Some(false))
            radioButtonCheck(noText, 2, Some(true))
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)
          }

          "Redirect user to the check your benefits page" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(receivedMileageBenefitPage(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "has an SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
            }
          }
        }
      }
    }


    ".submit" should {

      userScenarios.foreach { user =>
        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

          "Redirect the user to the overview page when it is not end of year" which {
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(receivedMileageBenefitPage(taxYear), body = "", user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            "has an SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
            }
          }

          "Update the mileageQuestion to no and wipe out the mileage amount when the no radio button has been chosen" which {

            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(receivedMileageBenefitPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "redirects to the check your details page" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe
                Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
              lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
              cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(false)
              cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe None
            }

          }

          "Update the mileageQuestion to yes and when the user chooses yes" which {

            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true, Some(benefits(fullCarVanFuelModel)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(receivedMileageBenefitPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            "redirects to the check your benefits page" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe
                Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
              lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
              cyamodel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
            }

          }

          "Redirect to check your benefits page" which {

            lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(receivedMileageBenefitPage(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has a SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("location") shouldBe Some(CheckYourBenefitsController.show(taxYearEOY, employmentId).url)
            }

          }
          "display an error wheen no radio button is selected" which {

            val form: Map[String, String] = Map[String, String]()

            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("name", hmrc = true)), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(receivedMileageBenefitPage(taxYearEOY), body = form, user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))

            welshToggleCheck(user.isWelsh)

          }
        }
      }
    }
  }
