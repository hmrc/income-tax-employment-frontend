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

package support.services

import models.mongo.EmploymentCYAModel
import models.redirects.ConditionalRedirect
import play.api.mvc.{Call, Result}
import services.RedirectService

import scala.collection.mutable

//scalastyle:off
object RedirectServiceStub extends RedirectService {

  private val _redirects: mutable.Map[(String, EmploymentCYAModel, Int, String), Seq[ConditionalRedirect]] = mutable.Map()

  def clearRedirects(): Unit = _redirects.clear()

  private def redirectsFor(methodName: String, employmentCYAModel: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    _redirects.getOrElse((methodName, employmentCYAModel, taxYear, employmentId), throw new RuntimeException("No redirects found"))
  }

  override def commonBenefitsRedirects(employmentCYAModel: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    redirectsFor("commonBenefitsRedirects", employmentCYAModel, taxYear, employmentId)
  }

  def mockCommonBenefitsRedirects(employmentCYAModel: EmploymentCYAModel, taxYear: Int, employmentId: String, result: Seq[ConditionalRedirect]): Unit = {
    _redirects.put(("commonBenefitsRedirects", employmentCYAModel, taxYear, employmentId), result)
  }

  override def commonCarVanFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def carBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def carBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def carFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def carFuelBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def vanBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def vanBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def vanFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def vanFuelBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def mileageBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def mileageBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def accommodationRelocationBenefitsRedirects(employmentCYAModel: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def commonAccommodationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def accommodationBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def qualifyingRelocationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def qualifyingRelocationBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def nonQualifyingRelocationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def nonQualifyingRelocationBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def travelEntertainmentBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def commonTravelEntertainmentBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def travelSubsistenceBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def incidentalCostsBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def incidentalCostsBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def entertainmentBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def entertainmentBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def utilitiesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def commonUtilitiesAndServicesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def telephoneBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def employerProvidedServicesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def employerProvidedServicesAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def employerProvidedSubscriptionsBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def employerProvidedSubscriptionsBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def servicesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def servicesBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def medicalBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def commonMedicalChildcareEducationRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def medicalInsuranceAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def childcareRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def childcareAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def educationalServicesRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def educationalServicesAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def beneficialLoansRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def beneficialLoansAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def incomeTaxAndCostsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def commonIncomeTaxAndCostsModelRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def incomeTaxPaidByDirectorAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def incurredCostsPaidByEmployerRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def incurredCostsPaidByEmployerAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def reimbursedCostsVouchersAndNonCashRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def commonReimbursedCostsVouchersAndNonCashModelRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def expensesAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def taxableExpensesRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def taxableExpensesAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def vouchersAndCreditCardsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def vouchersAndCreditCardsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def nonCashRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def nonCashAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def otherItemsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def otherItemsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def assetsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def commonAssetsModelRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def assetsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def assetTransferRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def assetTransferAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = throw new NotImplementedError

  override def getUnfinishedRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[Call] = throw new NotImplementedError

  override def employmentDetailsRedirect(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Result = throw new NotImplementedError

  override def benefitsSubmitRedirect(cya: EmploymentCYAModel, nextPage: Call)(_taxYear: Int, _employmentId: String): Result = throw new NotImplementedError
}
