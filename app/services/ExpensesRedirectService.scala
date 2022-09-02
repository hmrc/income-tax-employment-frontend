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

package services

import controllers.expenses.routes._
import models.expenses.ExpensesViewModel
import models.mongo.{ExpensesCYAModel, ExpensesUserData}
import models.redirects.ConditionalRedirect
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

import scala.concurrent.Future

// TODO: This should be made a class to allow for easier testing
object ExpensesRedirectService extends Logging {

  def commonExpensesRedirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    val claimingExpenses = cya.expenses.claimingEmploymentExpenses

    Seq(
      ConditionalRedirect(!claimingExpenses, CheckEmploymentExpensesController.show(taxYear), hasPrior = Some(false)),
      ConditionalRedirect(!claimingExpenses, CheckEmploymentExpensesController.show(taxYear), hasPrior = Some(true))
    )
  }

  def jobExpensesRedirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    commonExpensesRedirects(cya, taxYear)
  }

  def jobExpensesAmountRedirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    val jobExpensesQuestion = cya.expenses.jobExpensesQuestion

    jobExpensesRedirects(cya, taxYear) ++
      Seq(
        ConditionalRedirect(jobExpensesQuestion.isEmpty, BusinessTravelOvernightExpensesController.show(taxYear)),
        ConditionalRedirect(jobExpensesQuestion.contains(false), UniformsOrToolsExpensesController.show(taxYear), hasPrior = Some(false)),
        ConditionalRedirect(jobExpensesQuestion.contains(false), CheckEmploymentExpensesController.show(taxYear), hasPrior = Some(true))
      )
  }

  def flatRateRedirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    val jobExpensesSectionFinished = cya.expenses.jobExpensesSectionFinished(taxYear)

    commonExpensesRedirects(cya, taxYear) ++ Seq(jobExpensesSectionFinished.map(ConditionalRedirect(_))).flatten
  }

  def flatRateAmountRedirect(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    val flatRateQuestion = cya.expenses.flatRateJobExpensesQuestion

    flatRateRedirects(cya, taxYear) ++
      Seq(
        ConditionalRedirect(flatRateQuestion.isEmpty, UniformsOrToolsExpensesController.show(taxYear)),
        ConditionalRedirect(flatRateQuestion.contains(false), ProfessionalFeesAndSubscriptionsExpensesController.show(taxYear), hasPrior = Some(false)),
        ConditionalRedirect(flatRateQuestion.contains(false), CheckEmploymentExpensesController.show(taxYear), hasPrior = Some(true))
      )
  }

  def professionalSubscriptionsRedirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    val jobExpensesSectionFinished = cya.expenses.jobExpensesSectionFinished(taxYear)
    val flatRateSectionFinished = cya.expenses.flatRateSectionFinished(taxYear)

    commonExpensesRedirects(cya, taxYear) ++ Seq(
      jobExpensesSectionFinished.map(ConditionalRedirect(_)),
      flatRateSectionFinished.map(ConditionalRedirect(_))
    ).flatten
  }

  def professionalSubscriptionsAmountRedirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    val professionalSubscriptionsQuestion = cya.expenses.professionalSubscriptionsQuestion

    professionalSubscriptionsRedirects(cya, taxYear) ++
      Seq(
        ConditionalRedirect(professionalSubscriptionsQuestion.isEmpty, ProfessionalFeesAndSubscriptionsExpensesController.show(taxYear)),
        ConditionalRedirect(professionalSubscriptionsQuestion.contains(false), OtherEquipmentController.show(taxYear), hasPrior = Some(false)),
        ConditionalRedirect(professionalSubscriptionsQuestion.contains(false), CheckEmploymentExpensesController.show(taxYear), hasPrior = Some(true))
      )
  }

  def otherAllowancesRedirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    val jobExpensesSectionFinished = cya.expenses.jobExpensesSectionFinished(taxYear)
    val flatRateSectionFinished = cya.expenses.flatRateSectionFinished(taxYear)
    val profSubscriptionsSectionFinished = cya.expenses.professionalSubscriptionsSectionFinished(taxYear)

    commonExpensesRedirects(cya, taxYear) ++ Seq(
      jobExpensesSectionFinished.map(ConditionalRedirect(_)),
      flatRateSectionFinished.map(ConditionalRedirect(_)),
      profSubscriptionsSectionFinished.map(ConditionalRedirect(_))
    ).flatten
  }

  def otherAllowanceAmountRedirects(cya: ExpensesCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    val otherAllowanceQuestion = cya.expenses.otherAndCapitalAllowancesQuestion

    otherAllowancesRedirects(cya, taxYear) ++
      Seq(
        ConditionalRedirect(otherAllowanceQuestion.isEmpty, OtherEquipmentController.show(taxYear)),
        ConditionalRedirect(otherAllowanceQuestion.contains(false), CheckEmploymentExpensesController.show(taxYear), hasPrior = Some(false)),
        ConditionalRedirect(otherAllowanceQuestion.contains(false), CheckEmploymentExpensesController.show(taxYear), hasPrior = Some(true))
      )
  }

  def redirectBasedOnCurrentAnswers(taxYear: Int, data: Option[ExpensesUserData])
                                   (cyaConditions: ExpensesCYAModel => Seq[ConditionalRedirect])
                                   (block: ExpensesUserData => Future[Result]): Future[Result] = {
    val redirect = calculateRedirect(taxYear, data, cyaConditions)

    redirect match {
      case Left(redirect) => Future.successful(redirect)
      case Right(cya) => block(cya)
    }
  }

  private def calculateRedirect(taxYear: Int, data: Option[ExpensesUserData],
                                cyaConditions: ExpensesCYAModel => Seq[ConditionalRedirect]): Either[Result, ExpensesUserData] = {
    data match {
      case Some(cya) =>

        val possibleRedirects = cyaConditions(cya.expensesCya)

        val redirect = possibleRedirects.collectFirst {
          case ConditionalRedirect(condition, result, Some(hasPriorExpenses)) if condition && hasPriorExpenses == cya.hasPriorExpenses => Redirect(result)
          case ConditionalRedirect(condition, result, None) if condition => Redirect(result)
        }

        redirect match {
          case Some(redirect) =>
            logger.info(s"[ExpensesRedirectService][calculateRedirect]" +
              s" Some data is missing / in the wrong state for the requested page. Routing to ${redirect.header.headers.getOrElse("Location", "")}")
            Left(redirect)

          case None => Right(cya)
        }

      case None => Left(Redirect(CheckEmploymentExpensesController.show(taxYear)))
    }
  }

  def expensesSubmitRedirect(cya: ExpensesCYAModel, nextPage: Call)(_taxYear: Int): Result = {
    implicit val taxYear: Int = _taxYear

    val expensesViewModel: ExpensesViewModel = cya.expenses
    val expensesFinished = expensesViewModel.expensesIsFinished

    if (expensesFinished.isEmpty) {
      logger.info("[ExpensesRedirectService][expensesSubmitRedirect] User has completed all sections - Routing to expenses CYA page")
      Redirect(CheckEmploymentExpensesController.show(taxYear))
    } else {
      logger.info(s"[ExpensesRedirectService][expensesSubmitRedirect] User has not yet completed all sections - Routing to next page: ${nextPage.url}")
      Redirect(nextPage)
    }

  }
}
