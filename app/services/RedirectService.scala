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

package services

import controllers.employment.routes.{CheckEmploymentDetailsController, CheckYourBenefitsController}
import models.employment.{EmploymentBenefitsType, EmploymentDetailsType, EmploymentType}
import models.mongo.{EmploymentCYAModel, EmploymentUserData}
import models.redirects.ConditionalRedirect
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

import scala.concurrent.Future

//scalastyle:off
trait RedirectService extends Logging {

  //ALL PAGES
  def commonBenefitsRedirects(employmentCYAModel: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  //ALL CAR VAN PAGES
  def commonCarVanFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def carBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def carBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def carFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def carFuelBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def vanBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def vanBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def vanFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def vanFuelBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def mileageBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def mileageBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def accommodationRelocationBenefitsRedirects(employmentCYAModel: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  //ALL ACCOMMODATION PAGES
  def commonAccommodationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def accommodationBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def qualifyingRelocationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def qualifyingRelocationBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def nonQualifyingRelocationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def nonQualifyingRelocationBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def travelEntertainmentBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  //ALL TRAVEL ENTERTAINMENT PAGES
  def commonTravelEntertainmentBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def travelSubsistenceBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def incidentalCostsBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def incidentalCostsBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def entertainmentBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def entertainmentBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def utilitiesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  //ALL Utilities and services PAGES
  def commonUtilitiesAndServicesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def telephoneBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def employerProvidedServicesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def employerProvidedServicesAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def employerProvidedSubscriptionsBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def employerProvidedSubscriptionsBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def servicesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def servicesBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def medicalBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  //ALL Medical, Childcare and Education pages
  def commonMedicalChildcareEducationRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def medicalInsuranceAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def childcareRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def childcareAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def educationalServicesRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def educationalServicesAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def beneficialLoansRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def beneficialLoansAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def incomeTaxAndCostsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  //ALL Income tax and incurred costs pages
  def commonIncomeTaxAndCostsModelRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def incomeTaxPaidByDirectorAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def incurredCostsPaidByEmployerRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def incurredCostsPaidByEmployerAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def reimbursedCostsVouchersAndNonCashRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  //ALL Reimbursed costs and vouchers pages
  def commonReimbursedCostsVouchersAndNonCashModelRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def expensesAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def taxableExpensesRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def taxableExpensesAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def vouchersAndCreditCardsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def vouchersAndCreditCardsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def nonCashRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def nonCashAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def otherItemsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def otherItemsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def assetsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  //ALL assets pages
  def commonAssetsModelRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def assetsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def assetTransferRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def assetTransferAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect]

  def benefitsSubmitRedirect(cya: EmploymentCYAModel, nextPage: Call)(_taxYear: Int, _employmentId: String): Result

  def getUnfinishedRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[Call]

  def employmentDetailsRedirect(cya: EmploymentCYAModel,
                                taxYear: Int,
                                employmentId: String): Result

  def redirectBasedOnCurrentAnswers(taxYear: Int, employmentId: String, data: Option[EmploymentUserData], employmentType: EmploymentType)
                                   (cyaConditions: EmploymentCYAModel => Seq[ConditionalRedirect])
                                   (block: EmploymentUserData => Future[Result]): Future[Result] = {
    calculateRedirect(taxYear, employmentId, data, employmentType, cyaConditions) match {
      case Left(redirect) => Future.successful(redirect)
      case Right(cya) => block(cya)
    }
  }

  private def calculateRedirect(taxYear: Int, employmentId: String, data: Option[EmploymentUserData], employmentType: EmploymentType,
                                cyaConditions: EmploymentCYAModel => Seq[ConditionalRedirect]): Either[Result, EmploymentUserData] = {
    data match {
      case Some(cya) =>
        val possibleRedirects = cyaConditions(cya.employment)
        val redirect = possibleRedirects.collectFirst {
          case ConditionalRedirect(condition, result, Some(hasPriorBenefits)) if condition && hasPriorBenefits == cya.hasPriorBenefits => Redirect(result)
          case ConditionalRedirect(condition, result, None) if condition => Redirect(result)
        }

        redirect match {
          case Some(redirect) =>
            logger.info(s"[RedirectService][calculateRedirect]" +
              s" Some data is missing / in the wrong state for the requested page. Routing to ${redirect.header.headers.getOrElse("Location", "")}")
            Left(redirect)
          case None => Right(cya)
        }

      case None => employmentTypeRedirect(employmentType, taxYear, employmentId)
    }
  }

  private def employmentTypeRedirect(employmentType: EmploymentType, taxYear: Int, employmentId: String): Either[Result, EmploymentUserData] = {
    employmentType match {
      case EmploymentBenefitsType => Left(Redirect(CheckYourBenefitsController.show(taxYear, employmentId)))
      case EmploymentDetailsType => Left(Redirect(CheckEmploymentDetailsController.show(taxYear, employmentId)))
    }
  }

}
