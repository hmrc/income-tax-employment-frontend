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

package models.expenses

import controllers.expenses.routes._
import models.mongo.TextAndKey
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OFormat, __}
import play.api.mvc.Call
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, stringEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

case class ExpensesViewModel(claimingEmploymentExpenses: Boolean = false,
                             jobExpensesQuestion: Option[Boolean] = None,
                             jobExpenses: Option[BigDecimal] = None,
                             flatRateJobExpensesQuestion: Option[Boolean] = None,
                             flatRateJobExpenses: Option[BigDecimal] = None,
                             professionalSubscriptionsQuestion: Option[Boolean] = None,
                             professionalSubscriptions: Option[BigDecimal] = None,
                             otherAndCapitalAllowancesQuestion: Option[Boolean] = None,
                             otherAndCapitalAllowances: Option[BigDecimal] = None,
                             businessTravelCosts: Option[BigDecimal] = None,
                             hotelAndMealExpenses: Option[BigDecimal] = None,
                             vehicleExpenses: Option[BigDecimal] = None,
                             mileageAllowanceRelief: Option[BigDecimal] = None,
                             submittedOn: Option[String] = None,
                             isUsingCustomerData: Boolean) {

  // TODO: Needs testing
  def toExpenses: Expenses = {
    Expenses(
      businessTravelCosts, jobExpenses, flatRateJobExpenses, professionalSubscriptions, hotelAndMealExpenses,
      otherAndCapitalAllowances, vehicleExpenses, mileageAllowanceRelief
    )
  }

  // TODO: Needs testing
  def expensesIsFinished(implicit taxYear: Int): Option[Call] = {
    if (claimingEmploymentExpenses) {
      (jobExpensesSectionFinished, flatRateSectionFinished, professionalSubscriptionsSectionFinished, otherAndCapitalAllowancesSectionFinished) match {
        case (call@Some(_), _, _, _) => call
        case (_, call@Some(_), _, _) => call
        case (_, _, call@Some(_), _) => call
        case (_, _, _, call@Some(_)) => call
        case _ => None
      }
    } else {
      None
    }
  }

  // TODO: Needs testing
  def jobExpensesSectionFinished(implicit taxYear: Int): Option[Call] = {
    jobExpensesQuestion match {
      case Some(true) => if (jobExpenses.isDefined) None else Some(TravelAndOvernightAmountController.show(taxYear))
      case Some(false) => None
      case None => Some(BusinessTravelOvernightExpensesController.show(taxYear))
    }
  }

  // TODO: Needs testing
  def flatRateSectionFinished(implicit taxYear: Int): Option[Call] = {
    flatRateJobExpensesQuestion match {
      case Some(true) => if (flatRateJobExpenses.isDefined) None else Some(UniformsOrToolsExpensesAmountController.show(taxYear))
      case Some(false) => None
      case None => Some(UniformsOrToolsExpensesController.show(taxYear))
    }
  }

  // TODO: Needs testing
  def professionalSubscriptionsSectionFinished(implicit taxYear: Int): Option[Call] = {
    professionalSubscriptionsQuestion match {
      case Some(true) => if (professionalSubscriptions.isDefined) None else Some(ProfFeesAndSubscriptionsExpensesAmountController.show(taxYear))
      case Some(false) => None
      case None => Some(ProfessionalFeesAndSubscriptionsExpensesController.show(taxYear))
    }
  }

  // TODO: Needs testing
  def otherAndCapitalAllowancesSectionFinished(implicit taxYear: Int): Option[Call] = {
    otherAndCapitalAllowancesQuestion match {
      case Some(true) => if (otherAndCapitalAllowances.isDefined) None else Some(OtherEquipmentAmountController.show(taxYear))
      case Some(false) => None
      case None => Some(OtherEquipmentController.show(taxYear))
    }
  }

  def encrypted(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedExpensesViewModel = EncryptedExpensesViewModel(
    claimingEmploymentExpenses = claimingEmploymentExpenses.encrypted,
    jobExpensesQuestion = jobExpensesQuestion.map(_.encrypted),
    jobExpenses = jobExpenses.map(_.encrypted),
    flatRateJobExpensesQuestion = flatRateJobExpensesQuestion.map(_.encrypted),
    flatRateJobExpenses = flatRateJobExpenses.map(_.encrypted),
    professionalSubscriptionsQuestion = professionalSubscriptionsQuestion.map(_.encrypted),
    professionalSubscriptions = professionalSubscriptions.map(_.encrypted),
    otherAndCapitalAllowancesQuestion = otherAndCapitalAllowancesQuestion.map(_.encrypted),
    otherAndCapitalAllowances = otherAndCapitalAllowances.map(_.encrypted),
    businessTravelCosts = businessTravelCosts.map(_.encrypted),
    hotelAndMealExpenses = hotelAndMealExpenses.map(_.encrypted),
    vehicleExpenses = vehicleExpenses.map(_.encrypted),
    mileageAllowanceRelief = mileageAllowanceRelief.map(_.encrypted),
    submittedOn = submittedOn.map(_.encrypted),
    isUsingCustomerData = isUsingCustomerData.encrypted)
}

object ExpensesViewModel {

  def clear(isUsingCustomerData: Boolean): ExpensesViewModel = ExpensesViewModel(isUsingCustomerData = isUsingCustomerData)

  val firstSetOfFields: OFormat[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])] = (
    (__ \ "businessTravelCosts").formatNullable[BigDecimal] and
      (__ \ "jobExpenses").formatNullable[BigDecimal] and
      (__ \ "flatRateJobExpenses").formatNullable[BigDecimal] and
      (__ \ "professionalSubscriptions").formatNullable[BigDecimal] and
      (__ \ "hotelAndMealExpenses").formatNullable[BigDecimal] and
      (__ \ "otherAndCapitalAllowances").formatNullable[BigDecimal] and
      (__ \ "vehicleExpenses").formatNullable[BigDecimal] and
      (__ \ "mileageAllowanceRelief").formatNullable[BigDecimal]
    ).tupled

  val secondSetOfFields: OFormat[(Option[Boolean], Option[Boolean], Option[Boolean],
    Option[Boolean], Option[String], Boolean, Boolean)] = (
    (__ \ "jobExpensesQuestion").formatNullable[Boolean] and
      (__ \ "flatRateJobExpensesQuestion").formatNullable[Boolean] and
      (__ \ "professionalSubscriptionsQuestion").formatNullable[Boolean] and
      (__ \ "otherAndCapitalAllowancesQuestion").formatNullable[Boolean] and
      (__ \ "submittedOn").formatNullable[String] and
      (__ \ "isUsingCustomerData").format[Boolean] and
      (__ \ "claimEmploymentExpenses").format[Boolean]
    ).tupled

  implicit val format: OFormat[ExpensesViewModel] = {
    (firstSetOfFields and secondSetOfFields).apply({
      case (
        (businessTravelCosts, jobExpenses, flatRateJobExpenses, professionalSubscriptions, hotelAndMealExpenses,
        otherAndCapitalAllowances, vehicleExpenses, mileageAllowanceRelief),
        (jobExpensesQuestion, flatRateJobExpensesQuestion, professionalSubscriptionsQuestion,
        otherAndCapitalAllowancesQuestion, submittedOn, isUsingCustomerData, claimEmploymentExpenses)
        ) => ExpensesViewModel(
        claimEmploymentExpenses,
        jobExpensesQuestion,
        jobExpenses,
        flatRateJobExpensesQuestion,
        flatRateJobExpenses,
        professionalSubscriptionsQuestion,
        professionalSubscriptions,
        otherAndCapitalAllowancesQuestion,
        otherAndCapitalAllowances,
        businessTravelCosts,
        hotelAndMealExpenses,
        vehicleExpenses,
        mileageAllowanceRelief,
        submittedOn,
        isUsingCustomerData)
    }: ((Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
      Option[BigDecimal]), (Option[Boolean], Option[Boolean], Option[Boolean], Option[Boolean], Option[String], Boolean, Boolean)) => ExpensesViewModel, {
      expenses =>
        (
          (expenses.businessTravelCosts, expenses.jobExpenses, expenses.flatRateJobExpenses, expenses.professionalSubscriptions,
            expenses.hotelAndMealExpenses, expenses.otherAndCapitalAllowances, expenses.vehicleExpenses, expenses.mileageAllowanceRelief),
          (expenses.jobExpensesQuestion, expenses.flatRateJobExpensesQuestion, expenses.professionalSubscriptionsQuestion,
            expenses.otherAndCapitalAllowancesQuestion, expenses.submittedOn, expenses.isUsingCustomerData, expenses.claimingEmploymentExpenses)
        )
    })
  }
}

case class EncryptedExpensesViewModel(claimingEmploymentExpenses: EncryptedValue,
                                      jobExpensesQuestion: Option[EncryptedValue] = None,
                                      jobExpenses: Option[EncryptedValue] = None,
                                      flatRateJobExpensesQuestion: Option[EncryptedValue] = None,
                                      flatRateJobExpenses: Option[EncryptedValue] = None,
                                      professionalSubscriptionsQuestion: Option[EncryptedValue] = None,
                                      professionalSubscriptions: Option[EncryptedValue] = None,
                                      otherAndCapitalAllowancesQuestion: Option[EncryptedValue] = None,
                                      otherAndCapitalAllowances: Option[EncryptedValue] = None,
                                      businessTravelCosts: Option[EncryptedValue] = None,
                                      hotelAndMealExpenses: Option[EncryptedValue] = None,
                                      vehicleExpenses: Option[EncryptedValue] = None,
                                      mileageAllowanceRelief: Option[EncryptedValue] = None,
                                      submittedOn: Option[EncryptedValue],
                                      isUsingCustomerData: EncryptedValue) {

  def decrypted(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): ExpensesViewModel = ExpensesViewModel(
    claimingEmploymentExpenses = claimingEmploymentExpenses.decrypted[Boolean],
    jobExpensesQuestion = jobExpensesQuestion.map(_.decrypted[Boolean]),
    jobExpenses = jobExpenses.map(_.decrypted[BigDecimal]),
    flatRateJobExpensesQuestion = flatRateJobExpensesQuestion.map(_.decrypted[Boolean]),
    flatRateJobExpenses = flatRateJobExpenses.map(_.decrypted[BigDecimal]),
    professionalSubscriptionsQuestion = professionalSubscriptionsQuestion.map(_.decrypted[Boolean]),
    professionalSubscriptions = professionalSubscriptions.map(_.decrypted[BigDecimal]),
    otherAndCapitalAllowancesQuestion = otherAndCapitalAllowancesQuestion.map(_.decrypted[Boolean]),
    otherAndCapitalAllowances = otherAndCapitalAllowances.map(_.decrypted[BigDecimal]),
    businessTravelCosts = businessTravelCosts.map(_.decrypted[BigDecimal]),
    hotelAndMealExpenses = hotelAndMealExpenses.map(_.decrypted[BigDecimal]),
    vehicleExpenses = vehicleExpenses.map(_.decrypted[BigDecimal]),
    mileageAllowanceRelief = mileageAllowanceRelief.map(_.decrypted[BigDecimal]),
    submittedOn = submittedOn.map(_.decrypted[String]),
    isUsingCustomerData = isUsingCustomerData.decrypted[Boolean]
  )
}

object EncryptedExpensesViewModel {

  val firstSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue])] = (
    (__ \ "businessTravelCosts").formatNullable[EncryptedValue] and
      (__ \ "jobExpenses").formatNullable[EncryptedValue] and
      (__ \ "flatRateJobExpenses").formatNullable[EncryptedValue] and
      (__ \ "professionalSubscriptions").formatNullable[EncryptedValue] and
      (__ \ "hotelAndMealExpenses").formatNullable[EncryptedValue] and
      (__ \ "otherAndCapitalAllowances").formatNullable[EncryptedValue] and
      (__ \ "vehicleExpenses").formatNullable[EncryptedValue] and
      (__ \ "mileageAllowanceRelief").formatNullable[EncryptedValue]
    ).tupled

  val secondSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], EncryptedValue, EncryptedValue)] = (
    (__ \ "jobExpensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "flatRateJobExpensesQuestion").formatNullable[EncryptedValue] and
      (__ \ "professionalSubscriptionsQuestion").formatNullable[EncryptedValue] and
      (__ \ "otherAndCapitalAllowancesQuestion").formatNullable[EncryptedValue] and
      (__ \ "submittedOn").formatNullable[EncryptedValue] and
      (__ \ "isUsingCustomerData").format[EncryptedValue] and
      (__ \ "claimEmploymentExpenses").format[EncryptedValue]
    ).tupled

  implicit val format: OFormat[EncryptedExpensesViewModel] = {
    (firstSetOfFields and secondSetOfFields).apply({
      case (
        (businessTravelCosts, jobExpenses, flatRateJobExpenses, professionalSubscriptions, hotelAndMealExpenses,
        otherAndCapitalAllowances, vehicleExpenses, mileageAllowanceRelief),
        (jobExpensesQuestion, flatRateJobExpensesQuestion, professionalSubscriptionsQuestion,
        otherAndCapitalAllowancesQuestion, submittedOn, isUsingCustomerData, claimEmploymentExpenses)
        ) => EncryptedExpensesViewModel(claimEmploymentExpenses, jobExpensesQuestion, jobExpenses, flatRateJobExpensesQuestion, flatRateJobExpenses,
        professionalSubscriptionsQuestion, professionalSubscriptions, otherAndCapitalAllowancesQuestion, otherAndCapitalAllowances, businessTravelCosts,
        hotelAndMealExpenses, vehicleExpenses, mileageAllowanceRelief, submittedOn, isUsingCustomerData)
    }: ((Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
      Option[EncryptedValue], Option[EncryptedValue]), (Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
      Option[EncryptedValue], Option[EncryptedValue], EncryptedValue, EncryptedValue)) => EncryptedExpensesViewModel, {
      expenses =>
        (
          (expenses.businessTravelCosts, expenses.jobExpenses, expenses.flatRateJobExpenses, expenses.professionalSubscriptions,
            expenses.hotelAndMealExpenses, expenses.otherAndCapitalAllowances, expenses.vehicleExpenses, expenses.mileageAllowanceRelief),
          (expenses.jobExpensesQuestion, expenses.flatRateJobExpensesQuestion, expenses.professionalSubscriptionsQuestion,
            expenses.otherAndCapitalAllowancesQuestion, expenses.submittedOn, expenses.isUsingCustomerData, expenses.claimingEmploymentExpenses)
        )
    })
  }
}



