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

package controllers.expenses

import controllers.employment.routes.CheckEmploymentExpensesController
import forms.YesNoForm
import models.User
import models.expenses.{Expenses, ExpensesViewModel}
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class BusinessTravelOvernightExpensesControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  val taxYearEOY: Int = taxYear - 1

  private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    ExpensesUserData(sessionId, mtditid, nino, taxYear - 1, isPriorSubmission = isPrior, hasPriorExpenses, expensesCyaModel)

  def cyaModel(isUsingCustomerData: Boolean, expenses: Expenses): ExpensesCYAModel =
    ExpensesCYAModel.makeModel(expenses, isUsingCustomerData, submittedOn = None)

  def expensesViewModel(jobExpensesQuestion: Option[Boolean] = None): ExpensesViewModel =
    ExpensesViewModel(claimingEmploymentExpenses = true, jobExpensesQuestion = jobExpensesQuestion, isUsingCustomerData = true)

  private def pageUrl(taxYear: Int) = s"$appUrl/$taxYear/expenses/business-travel-and-overnight-expenses"

  private val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/expenses/business-travel-and-overnight-expenses"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val detailsSelector: String = s"#main-content > div > div > form > details > summary > span"
    val h2Selector: String = s"#main-content > div > div > form > details > div > h2"

    def h3Selector(index: Int): String = s"#main-content > div > div > form > details > div > h3:nth-child($index)"
    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > p:nth-child($index)"
    def bulletListSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child($index)"
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
    val expectedTitle = "Do you want to claim business travel and overnight expenses?"
    val expectedHeading = "Do you want to claim business travel and overnight expenses?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim travel and overnight stays"
    val expectedDoNotInclude = "Do not include your usual travel to work costs."
    val expectedDetailsInfo = "To work out how much you can claim for the tax year, you’ll need to:"
    val expectedDetails1 = "add up the mileage for each vehicle type you’ve used for work"
    val expectedDetails3 = "take away any amount your employer paid you towards your costs"
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
    val expectedTitle = "Do you want to claim your client’s business travel and overnight expenses?"
    val expectedHeading = "Do you want to claim your client’s business travel and overnight expenses?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes to claim for your client’s travel and overnight stays"
    val expectedDoNotInclude = "Do not include your client’s usual travel to work costs."
    val expectedDetailsInfo = "To work out how much your client can claim for the tax year, you’ll need to:"
    val expectedDetails1 = "add up the mileage for each vehicle type your client used for work"
    val expectedDetails3 = "take away any amount your client’s employer paid them towards their costs"
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

        "render the Business Travel and Overnight Amount Question page with no pre-filled radio buttons" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              emptyExpensesCYAModel.copy(expensesViewModel())), userRequest)
            urlGet(pageUrl(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(expectedExample1, bulletListSelector(1))
          textOnPageCheck(expectedExample2, bulletListSelector(2))
          textOnPageCheck(expectedExample3, bulletListSelector(3))
          textOnPageCheck(expectedExample4, bulletListSelector(4))
          textOnPageCheck(expectedExample5, bulletListSelector(5))
          textOnPageCheck(expectedExample6, bulletListSelector(6))
          textOnPageCheck(user.specificExpectedResults.get.expectedDoNotInclude, paragraphSelector(4))
          radioButtonCheck(yesText, 1, None)
          radioButtonCheck(noText, 2, None)
          buttonCheck(buttonText, continueButtonSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsInfo, detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetails1, detailsBulletList(1))
          textOnPageCheck(expectedDetails2, detailsBulletList(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetails3, detailsBulletList(3))
          textOnPageCheck(expectedApprovedMileageHeading, h2Selector)
          textOnPageCheck(expectedCarVanHeading, h3Selector(4))
          textOnPageCheck(s"$expectedCarVanText1", detailsParagraphSelector(5))
          textOnPageCheck(s"$expectedCarVanText2", detailsParagraphSelector(6))
          textOnPageCheck(expectedMotorcycleHeading, h3Selector(7))
          textOnPageCheck(expectedMotorcycleText, detailsParagraphSelector(8))
          textOnPageCheck(expectedBicycleHeading, h3Selector(9))
          textOnPageCheck(expectedBicycleText, detailsParagraphSelector(10))
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the Business Travel and Overnight Expenses Question page with 'Yes' pre-filled and CYA data exists" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              fullExpensesCYAModel), userRequest)
            urlGet(pageUrl(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(expectedExample1, bulletListSelector(1))
          textOnPageCheck(expectedExample2, bulletListSelector(2))
          textOnPageCheck(expectedExample3, bulletListSelector(3))
          textOnPageCheck(expectedExample4, bulletListSelector(4))
          textOnPageCheck(expectedExample5, bulletListSelector(5))
          textOnPageCheck(expectedExample6, bulletListSelector(6))
          textOnPageCheck(user.specificExpectedResults.get.expectedDoNotInclude, paragraphSelector(4))
          radioButtonCheck(yesText, 1, Some(true))
          radioButtonCheck(noText, 2, Some(false))
          buttonCheck(buttonText, continueButtonSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsInfo, detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetails1, detailsBulletList(1))
          textOnPageCheck(expectedDetails2, detailsBulletList(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetails3, detailsBulletList(3))
          textOnPageCheck(expectedApprovedMileageHeading, h2Selector)
          textOnPageCheck(expectedCarVanHeading, h3Selector(4))
          textOnPageCheck(s"$expectedCarVanText1", detailsParagraphSelector(5))
          textOnPageCheck(s"$expectedCarVanText2", detailsParagraphSelector(6))
          textOnPageCheck(expectedMotorcycleHeading, h3Selector(7))
          textOnPageCheck(expectedMotorcycleText, detailsParagraphSelector(8))
          textOnPageCheck(expectedBicycleHeading, h3Selector(9))
          textOnPageCheck(expectedBicycleText, detailsParagraphSelector(10))
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the Business Travel and Overnight Expenses Question page with 'No' pre-filled and not a prior submission" which {
          lazy val result: WSResponse = {
            dropExpensesDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              emptyExpensesCYAModel.copy(expensesViewModel(Some(false)))), userRequest)
            urlGet(pageUrl(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(expectedExample1, bulletListSelector(1))
          textOnPageCheck(expectedExample2, bulletListSelector(2))
          textOnPageCheck(expectedExample3, bulletListSelector(3))
          textOnPageCheck(expectedExample4, bulletListSelector(4))
          textOnPageCheck(expectedExample5, bulletListSelector(5))
          textOnPageCheck(expectedExample6, bulletListSelector(6))
          textOnPageCheck(user.specificExpectedResults.get.expectedDoNotInclude, paragraphSelector(4))
          radioButtonCheck(yesText, 1, Some(false))
          radioButtonCheck(noText, 2, Some(true))
          buttonCheck(buttonText, continueButtonSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsInfo, detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetails1, detailsBulletList(1))
          textOnPageCheck(expectedDetails2, detailsBulletList(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetails3, detailsBulletList(3))
          textOnPageCheck(expectedApprovedMileageHeading, h2Selector)
          textOnPageCheck(expectedCarVanHeading, h3Selector(4))
          textOnPageCheck(s"$expectedCarVanText1", detailsParagraphSelector(5))
          textOnPageCheck(s"$expectedCarVanText2", detailsParagraphSelector(6))
          textOnPageCheck(expectedMotorcycleHeading, h3Selector(7))
          textOnPageCheck(expectedMotorcycleText, detailsParagraphSelector(8))
          textOnPageCheck(expectedBicycleHeading, h3Selector(9))
          textOnPageCheck(expectedBicycleText, detailsParagraphSelector(10))
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to tax overview page with it's not EOY" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
        urlGet(pageUrl(taxYear), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
      }
    }

    "redirect to check employment expenses page when prior submission and if user has no expenses" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(pageUrl(taxYearEOY), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when form is submitted with no entry" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropExpensesDB()
            insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
              emptyExpensesCYAModel.copy(expensesViewModel())), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(pageUrl(taxYearEOY), body = form, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(expectedExample1, bulletListSelector(1))
          textOnPageCheck(expectedExample2, bulletListSelector(2))
          textOnPageCheck(expectedExample3, bulletListSelector(3))
          textOnPageCheck(expectedExample4, bulletListSelector(4))
          textOnPageCheck(expectedExample5, bulletListSelector(5))
          textOnPageCheck(expectedExample6, bulletListSelector(6))
          textOnPageCheck(user.specificExpectedResults.get.expectedDoNotInclude, paragraphSelector(4))
          radioButtonCheck(yesText, 1, None)
          radioButtonCheck(noText, 2, None)
          buttonCheck(buttonText, continueButtonSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsInfo, detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetails1, detailsBulletList(1))
          textOnPageCheck(expectedDetails2, detailsBulletList(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetails3, detailsBulletList(3))
          textOnPageCheck(expectedApprovedMileageHeading, h2Selector)
          textOnPageCheck(expectedCarVanHeading, h3Selector(4))
          textOnPageCheck(s"$expectedCarVanText1", detailsParagraphSelector(5))
          textOnPageCheck(s"$expectedCarVanText2", detailsParagraphSelector(6))
          textOnPageCheck(expectedMotorcycleHeading, h3Selector(7))
          textOnPageCheck(expectedMotorcycleText, detailsParagraphSelector(8))
          textOnPageCheck(expectedBicycleHeading, h3Selector(9))
          textOnPageCheck(expectedBicycleText, detailsParagraphSelector(10))
          formPostLinkCheck(continueLink, formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))
        }
      }
    }

    "redirect to Check Employment Expenses page" when {

      "user selects 'yes' and not a prior submission" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
          insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
            emptyExpensesCYAModel.copy(expensesViewModel(Some(false)))), userRequest)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }

        "updates jobExpensesQuestion to Some(true)" in {
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, userRequest).get
          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe Some(true)
        }

      }

      "user selects no and it's a prior submission" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
            fullExpensesCYAModel), userRequest)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }

        "update jobExpensesQuestion to Some(false) and wipes jobExpenses amount" in {
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, userRequest).get

          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.jobExpensesQuestion shouldBe Some(false)
          cyaModel.expensesCya.expenses.jobExpenses shouldBe None
        }
      }

      "user has no expenses" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(pageUrl(taxYearEOY), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(CheckEmploymentExpensesController.show(taxYearEOY).url)
        }
      }
    }

    "redirect to tax overview page if it's not EOY" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(userData(fullEmploymentsModel()), nino, taxYear)
        urlPost(pageUrl(taxYear), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
      }
    }

  }
}
