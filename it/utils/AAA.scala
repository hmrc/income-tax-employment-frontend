/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import controllers.benefits.accommodation.{AccommodationRelocationBenefitsControllerISpec, LivingAccommodationBenefitAmountControllerISpec,
  LivingAccommodationBenefitsControllerISpec, NonQualifyingRelocationBenefitsAmountControllerISpec, NonQualifyingRelocationBenefitsControllerISpec,
  QualifyingRelocationBenefitsAmountControllerISpec, QualifyingRelocationBenefitsControllerISpec}
import controllers.benefits.assets._
import controllers.benefits.fuel._
import controllers.benefits.income._
import controllers.benefits.medical._
import controllers.benefits.reimbursed._
import controllers.benefits.travel._
import controllers.benefits.utilities._
import controllers.benefits._

import org.scalatest.{Suites, TestSuite}

class AAA extends Suites(AAA.all_suites_that_need_postgress: _*) with TestSuite

object AAA {

  lazy val all_suites_that_need_postgress: Seq[TestSuite] = {
    postgress_suites_with_do_not_discover
  }

  private lazy val postgress_suites_with_do_not_discover = Seq(
    new AccommodationRelocationBenefitsControllerISpec,
    new LivingAccommodationBenefitAmountControllerISpec,
    new LivingAccommodationBenefitsControllerISpec,
    new NonQualifyingRelocationBenefitsAmountControllerISpec,
    new NonQualifyingRelocationBenefitsControllerISpec,
    new QualifyingRelocationBenefitsAmountControllerISpec,
    new QualifyingRelocationBenefitsControllerISpec,
    new AssetsBenefitsAmountControllerISpec,
    new AssetsBenefitsControllerISpec,
    new AssetsOrAssetTransfersBenefitsControllerISpec,
    new AssetsTransfersBenefitsAmountControllerISpec,
    new AssetTransfersBenefitsControllerISpec,
    new CarFuelBenefitsAmountControllerISpec,
    new CarVanFuelBenefitsControllerISpec,
    new CompanyCarBenefitsAmountControllerISpec,
    new CompanyCarBenefitsControllerISpec,
    new CompanyCarFuelBenefitsControllerISpec,
    new CompanyVanBenefitsAmountControllerISpec,
    new CompanyVanBenefitsControllerISpec,
    new CompanyVanFuelBenefitsAmountControllerISpec,
    new CompanyVanFuelBenefitsControllerISpec,
    new MileageBenefitAmountControllerISpec,
    new ReceivedOwnCarMileageBenefitControllerISpec,
    new IncomeTaxBenefitsAmountControllerISpec,
    new IncomeTaxBenefitsControllerISpec,
    new IncomeTaxOrIncurredCostsBenefitsControllerISpec,
    new IncurredCostsBenefitsAmountControllerISpec,
    new IncurredCostsBenefitsControllerISpec,
    new BeneficialLoansAmountControllerISpec,
    new BeneficialLoansBenefitsControllerISpec,
    new ChildcareBenefitsAmountControllerISpec,
    new ChildcareBenefitsControllerISpec,
    new EducationalServicesBenefitsAmountControllerISpec,
    new EducationalServicesBenefitsControllerISpec,
    new MedicalDentalBenefitsControllerISpec,
    new MedicalDentalChildcareBenefitsControllerISpec,
    new MedicalOrDentalBenefitsAmountControllerISpec,
    new NonCashBenefitsAmountControllerISpec,
    new NonCashBenefitsControllerISpec,
    new NonTaxableCostsBenefitsAmountControllerISpec,
    new NonTaxableCostsBenefitsControllerISpec,
    new OtherBenefitsAmountControllerISpec,
    new OtherBenefitsControllerISpec,
    new ReimbursedCostsVouchersAndNonCashBenefitsControllerISpec,
    new TaxableCostsBenefitsAmountControllerISpec,
    new TaxableCostsBenefitsControllerISpec,
    new VouchersBenefitsAmountControllerISpec,
    new VouchersBenefitsControllerISpec,
    new EntertainingBenefitsControllerISpec,
    new EntertainmentBenefitsAmountControllerISpec,
    new IncidentalCostsBenefitsAmountControllerISpec,
    new IncidentalOvernightCostEmploymentBenefitsControllerISpec,
    new TravelAndSubsistenceBenefitsControllerISpec,
    new TravelOrEntertainmentBenefitsControllerISpec,
    new TravelOrSubsistenceBenefitsAmountControllerISpec,
    new EmployerProvidedServicesBenefitsAmountControllerISpec,
    new EmployerProvidedServicesBenefitsControllerISpec,
    new OtherServicesBenefitsAmountControllerISpec,
    new OtherServicesBenefitsControllerISpec,
    new ProfessionalSubscriptionsBenefitsAmountControllerISpec,
    new ProfessionalSubscriptionsBenefitsControllerISpec,
    new TelephoneBenefitsAmountControllerISpec,
    new TelephoneBenefitsControllerISpec,
    new UtilitiesOrGeneralServicesBenefitsControllerISpec
  )
}