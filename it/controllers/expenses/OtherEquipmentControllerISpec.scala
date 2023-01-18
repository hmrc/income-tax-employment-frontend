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

package controllers.expenses

import forms.YesNoForm
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.expenses.ExpensesUserDataBuilder.anExpensesUserData
import support.builders.models.expenses.ExpensesViewModelBuilder.anExpensesViewModel
import support.builders.models.mongo.ExpensesCYAModelBuilder.anExpensesCYAModel
import utils.PageUrls.{checkYourExpensesUrl, fullUrl, otherEquipmentExpensesAmountUrl, otherEquipmentExpensesUrl, overviewUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherEquipmentControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    anExpensesUserData.copy(isPriorSubmission = isPrior, hasPriorExpenses = hasPriorExpenses, expensesCya = expensesCyaModel)

  def expensesViewModel(otherAndCapitalAllowancesQuestion: Option[Boolean] = None): ExpensesViewModel =
    anExpensesViewModel.copy(otherAndCapitalAllowancesQuestion = otherAndCapitalAllowancesQuestion, otherAndCapitalAllowances = None)

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render other equipment question page with no pre-filled radio buttons" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, ExpensesCYAModel(expensesViewModel())))
        urlGet(fullUrl(otherEquipmentExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }


    "redirect to tax overview page with it's not EOY" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(fullUrl(otherEquipmentExpensesUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to check employment expenses page when prior submission and if user has no expenses" which {
      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(otherEquipmentExpensesUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }
  }

  ".submit" should {
    "return an error when form is submitted with no entry" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
      lazy val result: WSResponse = {
        dropExpensesDB()
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, ExpensesCYAModel(expensesViewModel())))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(otherEquipmentExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to Other Equipment amount page when user selects 'yes' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false, ExpensesCYAModel(expensesViewModel(Some(false)))))
        urlPost(fullUrl(otherEquipmentExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(otherEquipmentExpensesAmountUrl(taxYearEOY)) shouldBe true
      }

      "updates otherAndCapitalAllowancesQuestion to Some(true)" in {
        lazy val cyaModel = findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get
        cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
        cyaModel.expensesCya.expenses.otherAndCapitalAllowancesQuestion shouldBe Some(true)
      }
    }

    "redirect to Check Employment Expenses page" when {
      "user selects no and it's a prior submission" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(anIncomeTaxUserData, nino, taxYear)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
          urlPost(fullUrl(otherEquipmentExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }

        "update otherAndCapitalAllowancesQuestion to Some(false) and wipes jobExpenses amount" in {
          lazy val cyaModel = findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get

          cyaModel.expensesCya.expenses.claimingEmploymentExpenses shouldBe true
          cyaModel.expensesCya.expenses.otherAndCapitalAllowancesQuestion shouldBe Some(false)
          cyaModel.expensesCya.expenses.otherAndCapitalAllowances shouldBe None
        }
      }

      "user has no expenses" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
        lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(otherEquipmentExpensesUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }
    }

    "redirect to tax overview page if it's not EOY" which {
      implicit lazy val result: WSResponse = {
        dropExpensesDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlPost(fullUrl(otherEquipmentExpensesUrl(taxYear)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a url of overview page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }
  }
}
