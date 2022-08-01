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

package controllers.expenses

import forms.AmountForm
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
import utils.PageUrls.{checkYourExpensesUrl, fullUrl, otherEquipmentExpensesUrl, overviewUrl, professionalFeesExpensesAmountUrl, professionalFeesExpensesUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ProfFeesAndSubscriptionsExpensesAmountControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val newAmount: BigDecimal = 100

  private def expensesUserData(isPrior: Boolean, hasPriorExpenses: Boolean, expensesCyaModel: ExpensesCYAModel): ExpensesUserData =
    anExpensesUserData.copy(isPriorSubmission = isPrior, hasPriorExpenses = hasPriorExpenses, expensesCya = expensesCyaModel)

  private def expensesViewModel(profFeesAndSubscriptions: Option[BigDecimal] = None): ExpensesViewModel =
    anExpensesViewModel.copy(professionalSubscriptions = profFeesAndSubscriptions, otherAndCapitalAllowancesQuestion = None, otherAndCapitalAllowances = None)

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the professional fees and subscriptions expenses amount page with an empty amount field" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          ExpensesCYAModel(expensesViewModel(profFeesAndSubscriptions = None))))
        urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK($OK) status" in {
        getInputFieldValue() shouldBe ""
        result.status shouldBe OK
      }
    }

    "render the professional fees and subscriptions expenses amount page with an non-empty amount field" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          ExpensesCYAModel(expensesViewModel(profFeesAndSubscriptions = Some(200)))))
        urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has an OK($OK) status" in {
        getInputFieldValue() shouldBe "200"
        result.status shouldBe OK
      }
    }

    "redirect to tax overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
        urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to check employment expenses page" when {
      "there is no expenses cya data" which {
        implicit lazy val result: WSResponse = {
          dropExpensesDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has an SEE OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }

      "professionSubscriptionsQuestion is set to Some(false)" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropExpensesDB()
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true,
            anExpensesCYAModel.copy(expensesViewModel(profFeesAndSubscriptions = None).copy(professionalSubscriptionsQuestion = Some(false)))))
          urlGet(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }
        s"has an SEE OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
        }
      }
    }

  }

  ".submit" should {
    "return an error" when {
      "the form is submitted with no entry" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropExpensesDB()
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
          urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = "", headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        s"has an BAD REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
      }
    }

    "redirect to tax overview page when it's not EOY" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, anExpensesCYAModel))
        urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYear)), body = "", follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      s"has an SEE OTHER($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to Check Employment Expenses page when there is no cya data" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString())
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to Professional Subscriptions Question page if professionalSubscriptionsQuestion is None" which {
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString)
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        insertExpensesCyaData(expensesUserData(isPrior = false, hasPriorExpenses = false,
          anExpensesCYAModel.copy(expenses = anExpensesViewModel.copy(professionalSubscriptionsQuestion = None))))
        urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(professionalFeesExpensesUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to next page when valid form is submitted" which {
      val form: Map[String, String] = Map(AmountForm.amount -> newAmount.toString)
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropExpensesDB()
        insertExpensesCyaData(expensesUserData(isPrior = true, hasPriorExpenses = true, ExpensesCYAModel(expensesViewModel())))
        urlPost(fullUrl(professionalFeesExpensesAmountUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(otherEquipmentExpensesUrl(taxYearEOY)) shouldBe true
      }

      "updates professionalSubscriptions to the new value" in {
        lazy val cyaModel = findExpensesCyaData(taxYearEOY, anAuthorisationRequest).get
        cyaModel.expensesCya.expenses.professionalSubscriptions shouldBe Some(newAmount)
      }
    }
  }
}
