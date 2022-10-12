/*
 * Copyright 2022 HM Revenue & Customs
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

package views.expenses

import controllers.expenses.routes.BusinessTravelOvernightExpensesController
import forms.YesNoForm
import forms.expenses.ExpensesFormsProvider
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.expenses.BusinessTravelOvernightExpensesView

class BusinessTravelOvernightExpensesViewSpec extends ViewUnitTest {

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val detailsSelector: String = s"#main-content > div > div > form > details > summary > span"
    val h2Selector: String = s"#main-content > div > div > form > details > div > h2"

    def h3Selector(index: Int): String = s"#main-content > div > div > form > details > div > h3:nth-child($index)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"

    def bulletListSelector(index: Int): String = s"#main-content > div > div > ul > li:nth-child($index)"

    def detailsParagraphSelector(index: Int): String = s"#main-content > div > div > form > details > div > p:nth-child($index)"

    def detailsBulletList(index: Int): String = s"#main-content > div > div > form > details > div > ol > li:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedParagraphText: String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedExample1: String
    val expectedExample2: String
    val expectedExample3: String
    val expectedExample4: String
    val expectedExample5: String
    val expectedExample6: String
    val expectedDetailsTitle: String
    val expectedDetails2: String
    val expectedApprovedMileageHeading: String
    val expectedCarVanHeading: String
    val expectedCarVanText1: String
    val expectedCarVanText2: String
    val expectedMotorcycleHeading: String
    val expectedMotorcycleText: String
    val expectedBicycleHeading: String
    val expectedBicycleText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
    val expectedDoNotInclude: String
    val expectedDetailsInfo: String
    val expectedDetails1: String
    val expectedDetails3: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment expenses for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedParagraphText = "These expenses are things like:"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedExample1 = "public transport costs"
    val expectedExample2 = "using a vehicle for business travel"
    val expectedExample3 = "hotel accommodation if you have to stay overnight"
    val expectedExample4 = "food and drink"
    val expectedExample5 = "congestion charges, tolls and parking fees"
    val expectedExample6 = "business phone calls and printing costs"
    val expectedDetailsTitle = "Using your own vehicle for business travel"
    val expectedDetails2 = "multiply the mileage by the approved mileage allowance"
    val expectedApprovedMileageHeading = "Approved mileage allowance"
    val expectedCarVanHeading = "Car and vans"
    val expectedCarVanText1 = "45p for the first 10,000 miles"
    val expectedCarVanText2 = "25p for every mile over 10,000"
    val expectedMotorcycleHeading = "Motorcycle"
    val expectedMotorcycleText = "24p a mile"
    val expectedBicycleHeading = "Bicycle"
    val expectedBicycleText = "20p a mile"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Treuliau cyflogaeth ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedParagraphText = "Mae’r treuliau hyn yn bethau fel:"
    val yesText = "Iawn"
    val noText = "Na"
    val buttonText = "Yn eich blaen"
    val expectedExample1 = "costau trafnidiaeth cyhoeddus"
    val expectedExample2 = "defnyddio cerbyd ar gyfer teithiau busnes"
    val expectedExample3 = "llety mewn gwesty os oes rhaid i chi aros dros nos"
    val expectedExample4 = "bwyd a diod"
    val expectedExample5 = "taliadau atal tagfeydd, tollau a ffioedd parcio"
    val expectedExample6 = "galwadau Ffôn a chostau argraffu’r busnes"
    val expectedDetailsTitle = "Os ydych yn defnyddio’ch cerbyd eich hun ar gyfer teithiau busnes"
    val expectedDetails2 = "lluoswch y milltiroedd â’r lwfans milltiroedd cymeradwy"
    val expectedApprovedMileageHeading = "Lwfans milltiroedd cymeradwy"
    val expectedCarVanHeading = "Ceir a faniau"
    val expectedCarVanText1 = "45c am y 10,000 milltir gyntaf"
    val expectedCarVanText2 = "25c am bob milltir dros 10,000"
    val expectedMotorcycleHeading = "Beic modur"
    val expectedMotorcycleText = "24c y filltir"
    val expectedBicycleHeading = "Beic"
    val expectedBicycleText = "20c y filltir"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim business travel and overnight expenses?"
    val expectedHeading = "Do you want to claim business travel and overnight expenses?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim travel and overnight stays"
    val expectedDoNotInclude = "Do not include your usual travel to work costs."
    val expectedDetailsInfo = "To work out how much you can claim for the tax year, you’ll need to:"
    val expectedDetails1 = "add up the mileage for each vehicle type you’ve used for work"
    val expectedDetails3 = "take away any amount your employer paid you towards your costs"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am hawlio unrhyw dreuliau teithio busness ac aros dros nos?"
    val expectedHeading = "A ydych am hawlio unrhyw dreuliau teithio busness ac aros dros nos?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ i hawlio ar gyfer costau teithio ac aros dros nos"
    val expectedDoNotInclude = "Peidiwch â chynnwys eich costau teithio i’r gwaith arferol."
    val expectedDetailsInfo = "Er mwyn cyfrifo faint y gallwch ei hawlio ar gyfer y flwyddyn dreth, bydd angen i chi wneud y canlynol:"
    val expectedDetails1 = "adiwch y milltiroedd at ei gilydd ar gyfer pob math o gerbyd rydych wedi ei ddefnyddio ar gyfer gwaith"
    val expectedDetails3 = "tynnwch unrhyw swm a dalodd eich cyflogwr tuag at eich costau"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Do you want to claim your client’s business travel and overnight expenses?"
    val expectedHeading = "Do you want to claim your client’s business travel and overnight expenses?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for your client’s travel and overnight stays"
    val expectedDoNotInclude = "Do not include your client’s usual travel to work costs."
    val expectedDetailsInfo = "To work out how much your client can claim for the tax year, you’ll need to:"
    val expectedDetails1 = "add up the mileage for each vehicle type your client used for work"
    val expectedDetails3 = "take away any amount your client’s employer paid them towards their costs"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A ydych am hawlio ar gyfer treuliau teithio busnes ac aros dros nos eich cleient?"
    val expectedHeading = "A ydych am hawlio ar gyfer treuliau teithio busnes ac aros dros nos eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ i hawlio ar gyfer costau teithio ac aros dros nos eich cleient"
    val expectedDoNotInclude = "Peidiwch â chynnwys costau teithio i’r gwaith arferol eich cleient."
    val expectedDetailsInfo = "Er mwyn cyfrifo faint y gall eich cleient ei hawlio ar gyfer pob blwyddyn dreth, bydd angen i chi wneud y canlynol:"
    val expectedDetails1 = "adiwch y milltiroedd at ei gilydd ar gyfer pob math o gerbyd y mae’ch cleient yn ei ddefnyddio ar gyfer gwaith"
    val expectedDetails3 = "tynnwch unrhyw swm a dalodd cyflogwr eich cleient tuag at ei gostau"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def form(isAgent: Boolean): Form[Boolean] = new ExpensesFormsProvider().businessTravelExpensesForm(isAgent)

  private lazy val underTest = inject[BusinessTravelOvernightExpensesView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no pre-filled radio buttons" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(expectedParagraphText, paragraphSelector(index = 2))
        textOnPageCheck(expectedExample1, bulletListSelector(index = 1))
        textOnPageCheck(expectedExample2, bulletListSelector(index = 2))
        textOnPageCheck(expectedExample3, bulletListSelector(index = 3))
        textOnPageCheck(expectedExample4, bulletListSelector(index = 4))
        textOnPageCheck(expectedExample5, bulletListSelector(index = 5))
        textOnPageCheck(expectedExample6, bulletListSelector(index = 6))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDoNotInclude, paragraphSelector(4))
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)

        textOnPageCheck(expectedDetailsTitle, detailsSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsInfo, detailsParagraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails1, detailsBulletList(1))
        textOnPageCheck(expectedDetails2, detailsBulletList(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails3, detailsBulletList(3))
        textOnPageCheck(expectedApprovedMileageHeading, h2Selector)
        textOnPageCheck(expectedCarVanHeading, h3Selector(4))
        textOnPageCheck(s"$expectedCarVanText1", detailsParagraphSelector(5))
        textOnPageCheck(s"$expectedCarVanText2", detailsParagraphSelector(6))
        textOnPageCheck(expectedMotorcycleHeading, h3Selector(7))
        textOnPageCheck(expectedMotorcycleText, detailsParagraphSelector(8))
        textOnPageCheck(expectedBicycleHeading, h3Selector(9))
        textOnPageCheck(expectedBicycleText, detailsParagraphSelector(10))
        formPostLinkCheck(BusinessTravelOvernightExpensesController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'Yes' pre-filled and CYA data exists" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = true), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(expectedParagraphText, paragraphSelector(2))
        textOnPageCheck(expectedExample1, bulletListSelector(1))
        textOnPageCheck(expectedExample2, bulletListSelector(2))
        textOnPageCheck(expectedExample3, bulletListSelector(3))
        textOnPageCheck(expectedExample4, bulletListSelector(4))
        textOnPageCheck(expectedExample5, bulletListSelector(5))
        textOnPageCheck(expectedExample6, bulletListSelector(6))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDoNotInclude, paragraphSelector(4))
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)

        textOnPageCheck(expectedDetailsTitle, detailsSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsInfo, detailsParagraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails1, detailsBulletList(1))
        textOnPageCheck(expectedDetails2, detailsBulletList(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails3, detailsBulletList(3))
        textOnPageCheck(expectedApprovedMileageHeading, h2Selector)
        textOnPageCheck(expectedCarVanHeading, h3Selector(4))
        textOnPageCheck(s"$expectedCarVanText1", detailsParagraphSelector(5))
        textOnPageCheck(s"$expectedCarVanText2", detailsParagraphSelector(6))
        textOnPageCheck(expectedMotorcycleHeading, h3Selector(7))
        textOnPageCheck(expectedMotorcycleText, detailsParagraphSelector(8))
        textOnPageCheck(expectedBicycleHeading, h3Selector(9))
        textOnPageCheck(expectedBicycleText, detailsParagraphSelector(10))
        formPostLinkCheck(BusinessTravelOvernightExpensesController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render page with 'No' pre-filled and not a prior submission" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).fill(value = false), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(expectedParagraphText, paragraphSelector(2))
        textOnPageCheck(expectedExample1, bulletListSelector(1))
        textOnPageCheck(expectedExample2, bulletListSelector(2))
        textOnPageCheck(expectedExample3, bulletListSelector(3))
        textOnPageCheck(expectedExample4, bulletListSelector(4))
        textOnPageCheck(expectedExample5, bulletListSelector(5))
        textOnPageCheck(expectedExample6, bulletListSelector(6))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDoNotInclude, paragraphSelector(4))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(buttonText, continueButtonSelector)

        textOnPageCheck(expectedDetailsTitle, detailsSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsInfo, detailsParagraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails1, detailsBulletList(1))
        textOnPageCheck(expectedDetails2, detailsBulletList(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails3, detailsBulletList(3))
        textOnPageCheck(expectedApprovedMileageHeading, h2Selector)
        textOnPageCheck(expectedCarVanHeading, h3Selector(4))
        textOnPageCheck(s"$expectedCarVanText1", detailsParagraphSelector(5))
        textOnPageCheck(s"$expectedCarVanText2", detailsParagraphSelector(6))
        textOnPageCheck(expectedMotorcycleHeading, h3Selector(7))
        textOnPageCheck(expectedMotorcycleText, detailsParagraphSelector(8))
        textOnPageCheck(expectedBicycleHeading, h3Selector(9))
        textOnPageCheck(expectedBicycleText, detailsParagraphSelector(10))
        formPostLinkCheck(BusinessTravelOvernightExpensesController.submit(taxYearEOY).url, formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(form(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))
        textOnPageCheck(expectedParagraphText, paragraphSelector(index = 3))
        textOnPageCheck(expectedExample1, bulletListSelector(index = 1))
        textOnPageCheck(expectedExample2, bulletListSelector(index = 2))
        textOnPageCheck(expectedExample3, bulletListSelector(index = 3))
        textOnPageCheck(expectedExample4, bulletListSelector(index = 4))
        textOnPageCheck(expectedExample5, bulletListSelector(index = 5))
        textOnPageCheck(expectedExample6, bulletListSelector(index = 6))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDoNotInclude, paragraphSelector(index = 5))
        radioButtonCheck(yesText, radioNumber = 1, checked = false)
        radioButtonCheck(noText, radioNumber = 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)

        textOnPageCheck(expectedDetailsTitle, detailsSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetailsInfo, detailsParagraphSelector(index = 1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails1, detailsBulletList(index = 1))
        textOnPageCheck(expectedDetails2, detailsBulletList(index = 2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedDetails3, detailsBulletList(index = 3))
        textOnPageCheck(expectedApprovedMileageHeading, h2Selector)
        textOnPageCheck(expectedCarVanHeading, h3Selector(index = 4))
        textOnPageCheck(s"$expectedCarVanText1", detailsParagraphSelector(index = 5))
        textOnPageCheck(s"$expectedCarVanText2", detailsParagraphSelector(index = 6))
        textOnPageCheck(expectedMotorcycleHeading, h3Selector(index = 7))
        textOnPageCheck(expectedMotorcycleText, detailsParagraphSelector(index = 8))
        textOnPageCheck(expectedBicycleHeading, h3Selector(index = 9))
        textOnPageCheck(expectedBicycleText, detailsParagraphSelector(index = 10))
        formPostLinkCheck(BusinessTravelOvernightExpensesController.submit(taxYearEOY).url, formSelector)

        welshToggleCheck(userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Some("value"))
      }
    }
  }
}
