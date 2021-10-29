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

package services

import controllers.benefits.routes._
import controllers.employment.routes._
import models.User
import models.employment.{AccommodationRelocationModel, CarVanFuelModel}
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

import scala.concurrent.Future

//scalastyle:off
object RedirectService extends Logging {

  trait EmploymentType
  case object EmploymentDetailsType extends EmploymentType
  case object EmploymentBenefitsType extends EmploymentType

  case class ConditionalRedirect(condition: Boolean, redirect: Call, hasPriorBenefits: Option[Boolean] = None)

  def toConditionalRedirect(call: Option[Call]): Option[ConditionalRedirect] = {
    call.map(ConditionalRedirect(true, _))
  }

  //ALL PAGES
  def commonBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val benefitsReceived = cya.employmentBenefits.map(_.isBenefitsReceived)

    Seq(
      ConditionalRedirect(benefitsReceived.isEmpty, ReceiveAnyBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
      ConditionalRedirect(benefitsReceived.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true)),
      ConditionalRedirect(benefitsReceived.contains(false), CheckYourBenefitsController.show(taxYear, employmentId))
    )
  }

  //ALL CAR VAN PAGES
  def commonCarVanFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carVanFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carVanFuelQuestion))

    commonBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(carVanFuelQuestion.isEmpty, CarVanFuelBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(carVanFuelQuestion.contains(false), AccommodationRelocationBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(carVanFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def carBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId)
  }

  def carBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carQuestion))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(carQuestion.isEmpty, CompanyCarBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(carQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(carQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def carFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carSectionFinished(taxYear, employmentId))))

    carBenefitsAmountRedirects(cya, taxYear, employmentId) ++ Seq(carSectionFinished).flatten
  }

  def carFuelBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val carFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.carFuelQuestion))

    carFuelBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(carFuelQuestion.isEmpty, CompanyCarFuelBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(carFuelQuestion.contains(false), CompanyVanBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(carFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def vanBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val fullCarSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.fullCarSectionFinished(taxYear, employmentId))))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(fullCarSectionFinished).flatten
  }

  def vanBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanQuestion))

    vanBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(vanQuestion.isEmpty, CompanyVanBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(vanQuestion.contains(false), ReceiveOwnCarMileageBenefitController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(vanQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def vanFuelBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanSectionFinished(taxYear, employmentId))))

    vanBenefitsAmountRedirects(cya, taxYear, employmentId) ++ Seq(vanSectionFinished).flatten
  }

  def vanFuelBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val vanFuelQuestion = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.vanFuelQuestion))

    vanFuelBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(vanFuelQuestion.isEmpty, CompanyVanFuelBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(vanFuelQuestion.contains(false), ReceiveOwnCarMileageBenefitController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(vanFuelQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def mileageBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val fullCarSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.fullCarSectionFinished(taxYear, employmentId))))

    val fullVanSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.fullVanSectionFinished(taxYear, employmentId))))

    commonCarVanFuelBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(fullCarSectionFinished, fullVanSectionFinished).flatten
  }

  def mileageBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val cyaMileageQuestion: Option[Boolean] = cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion))

    mileageBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(cyaMileageQuestion.isEmpty, ReceiveOwnCarMileageBenefitController.show(taxYear, employmentId)),
        ConditionalRedirect(cyaMileageQuestion.contains(false), AccommodationRelocationBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(cyaMileageQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def accommodationRelocationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val fullCarVanFuelFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.isFinished(taxYear, employmentId))))

    commonBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(fullCarVanFuelFinished).flatten
  }

  //ALL ACCOMMODATION PAGES
  def commonAccommodationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val accommodationRelocationQuestion = cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationRelocationQuestion))

    accommodationRelocationBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(accommodationRelocationQuestion.isEmpty, AccommodationRelocationBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(accommodationRelocationQuestion.contains(false), TravelOrEntertainmentBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(accommodationRelocationQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def accommodationBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val accommodationQuestion = cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationQuestion))

    commonAccommodationBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(accommodationQuestion.isEmpty, LivingAccommodationBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(accommodationQuestion.contains(false), QualifyingRelocationBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(accommodationQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def qualifyingRelocationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val accommodationSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationSectionFinished(taxYear, employmentId))))

    commonAccommodationBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(accommodationSectionFinished).flatten
  }

  def qualifyingRelocationBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val relocationQuestion = cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationExpensesQuestion))

    qualifyingRelocationBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(relocationQuestion.isEmpty, QualifyingRelocationBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(relocationQuestion.contains(false), NonQualifyingRelocationBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(relocationQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def nonQualifyingRelocationBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val accommodationSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.accommodationSectionFinished(taxYear, employmentId))))

    val qualifyingRelocationSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.qualifyingRelocationSectionFinished(taxYear, employmentId))))

    commonAccommodationBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(accommodationSectionFinished, qualifyingRelocationSectionFinished).flatten
  }

  def nonQualifyingRelocationBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val relocationQuestion = cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.nonQualifyingRelocationExpensesQuestion))

    nonQualifyingRelocationBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(relocationQuestion.isEmpty, NonQualifyingRelocationBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(relocationQuestion.contains(false), TravelOrEntertainmentBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(relocationQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def travelEntertainmentBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val fullCarVanFuelFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.isFinished(taxYear, employmentId))))
    val accommodationRelocationFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.isFinished(taxYear, employmentId))))

    commonBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(fullCarVanFuelFinished, accommodationRelocationFinished).flatten
  }

  //ALL TRAVEL ENTERTAINMENT PAGES
  def commonTravelEntertainmentBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    val travelEntertainmentQuestion = cya.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelEntertainmentQuestion))

    travelEntertainmentBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(travelEntertainmentQuestion.isEmpty, TravelOrEntertainmentBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(travelEntertainmentQuestion.contains(false), UtilitiesOrGeneralServicesBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(travelEntertainmentQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def travelSubsistenceBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val travelSubsistenceQuestion = cya.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelAndSubsistenceQuestion))

    commonTravelEntertainmentBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(travelSubsistenceQuestion.isEmpty, TravelAndSubsistenceBenefitsController.show(taxYear, employmentId)),
        //TODO go to incidental costs yes no page
        ConditionalRedirect(travelSubsistenceQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(travelSubsistenceQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def incidentalCostsBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val travelSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelSectionFinished(taxYear, employmentId))))

    commonTravelEntertainmentBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(travelSectionFinished).flatten
  }

  def incidentalCostsBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val incidentalCostsQuestion = cya.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalExpensesQuestion))

    incidentalCostsBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO go to incidental costs yes no page
        ConditionalRedirect(incidentalCostsQuestion.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId)),
        //TODO go to entertainment yes no page
        ConditionalRedirect(incidentalCostsQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(incidentalCostsQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def entertainmentBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val travelSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.travelSectionFinished(taxYear, employmentId))))

    val incidentalCostsSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.personalIncidentalSectionFinished(taxYear, employmentId))))

    commonTravelEntertainmentBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(travelSectionFinished, incidentalCostsSectionFinished).flatten
  }

  def entertainmentBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val entertainingQuestion = cya.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.entertainingQuestion))

    entertainmentBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO go to entertaining yes no page
        ConditionalRedirect(entertainingQuestion.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(entertainingQuestion.contains(false), UtilitiesOrGeneralServicesBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(entertainingQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def utilitiesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val fullCarVanFuelFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.isFinished(taxYear, employmentId))))
    val accommodationRelocationFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.isFinished(taxYear, employmentId))))
    val travelEntertainmentFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.isFinished(taxYear, employmentId))))

    commonBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(fullCarVanFuelFinished, accommodationRelocationFinished, travelEntertainmentFinished).flatten
  }

  //ALL Utilities and services PAGES
  def commonUtilitiesAndServicesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val utilitiesAndServicesQuestion = cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.utilitiesAndServicesQuestion))

    utilitiesBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(utilitiesAndServicesQuestion.isEmpty, UtilitiesOrGeneralServicesBenefitsController.show(taxYear, employmentId)),
        //TODO go to medical benefits section
        ConditionalRedirect(utilitiesAndServicesQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(utilitiesAndServicesQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def telephoneBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val telephoneQuestion = cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.telephoneQuestion))

    commonUtilitiesAndServicesBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO go to telephone yes no page
        ConditionalRedirect(telephoneQuestion.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId)),
        ConditionalRedirect(telephoneQuestion.contains(false), EmployerProvidedServicesBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(telephoneQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def employerProvidedServicesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val telephoneSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.telephoneSectionFinished(taxYear, employmentId))))

    commonUtilitiesAndServicesBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(telephoneSectionFinished).flatten
  }

  def employerProvidedServicesAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {
    val employerProvidedServicesQuestion = cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServicesQuestion))

    employerProvidedServicesBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        ConditionalRedirect(employerProvidedServicesQuestion.isEmpty, EmployerProvidedServicesBenefitsController.show(taxYear, employmentId)),
        //TODO go to employer provided subscriptions yes no page
        ConditionalRedirect(employerProvidedServicesQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(employerProvidedServicesQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def employerProvidedSubscriptionsBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val telephoneSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.telephoneSectionFinished(taxYear, employmentId))))

    val employerProvidedServicesSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServicesSectionFinished(taxYear, employmentId))))

    commonUtilitiesAndServicesBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(telephoneSectionFinished, employerProvidedServicesSectionFinished).flatten
  }

  def employerProvidedSubscriptionsBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val employerProvidedProfessionalSubscriptionsQuestion = cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptionsQuestion))

    employerProvidedSubscriptionsBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO go to employerProvidedProfessionalSubscriptions yes no page
        ConditionalRedirect(employerProvidedProfessionalSubscriptionsQuestion.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId)),
        //TODO go to services yes no page
        ConditionalRedirect(employerProvidedProfessionalSubscriptionsQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(employerProvidedProfessionalSubscriptionsQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def servicesBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val telephoneSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.telephoneSectionFinished(taxYear, employmentId))))

    val employerProvidedServicesSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedServicesSectionFinished(taxYear, employmentId))))

    val employerProvidedProfessionalSubscriptionsSectionFinished = toConditionalRedirect(
      cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.employerProvidedProfessionalSubscriptionsSectionFinished(taxYear, employmentId))))

    commonUtilitiesAndServicesBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(telephoneSectionFinished, employerProvidedServicesSectionFinished, employerProvidedProfessionalSubscriptionsSectionFinished).flatten
  }

  def servicesBenefitsAmountRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val serviceQuestion = cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.serviceQuestion))

    servicesBenefitsRedirects(cya, taxYear, employmentId) ++
      Seq(
        //TODO go to service yes no page
        ConditionalRedirect(serviceQuestion.isEmpty, CheckYourBenefitsController.show(taxYear, employmentId)),
        //TODO go to medical benefits yes no page
        ConditionalRedirect(serviceQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(false)),
        ConditionalRedirect(serviceQuestion.contains(false), CheckYourBenefitsController.show(taxYear, employmentId), hasPriorBenefits = Some(true))
      )
  }

  def medicalBenefitsRedirects(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Seq[ConditionalRedirect] = {

    val fullCarVanFuelFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.isFinished(taxYear, employmentId))))
    val accommodationRelocationFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.accommodationRelocationModel.flatMap(_.isFinished(taxYear, employmentId))))
    val travelEntertainmentFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.travelEntertainmentModel.flatMap(_.isFinished(taxYear, employmentId))))
    val utilitiesAndServicesFinished = toConditionalRedirect(cya.employmentBenefits.flatMap(_.utilitiesAndServicesModel.flatMap(_.isFinished(taxYear, employmentId))))

    commonBenefitsRedirects(cya, taxYear, employmentId) ++ Seq(fullCarVanFuelFinished, accommodationRelocationFinished, travelEntertainmentFinished, utilitiesAndServicesFinished).flatten
  }

  def redirectBasedOnCurrentAnswers(taxYear: Int, employmentId: String, data: Option[EmploymentUserData], employmentType: EmploymentType)
                                   (cyaConditions: EmploymentCYAModel => Seq[ConditionalRedirect])
                                   (block: EmploymentUserData => Future[Result]): Future[Result] = {
    val redirect = calculateRedirect(taxYear, employmentId, data, employmentType, cyaConditions)

    redirect match {
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

  def benefitsSubmitRedirect(cya: EmploymentCYAModel, nextPage: Call)(_taxYear: Int, _employmentId: String)(implicit user: User[_]): Result ={

    implicit val taxYear: Int = _taxYear
    implicit val employmentId: String = _employmentId

    val carVanFuelSection: CarVanFuelModel = cya.employmentBenefits.flatMap(_.carVanFuelModel).getOrElse(CarVanFuelModel())
    val accommodationRelocationSection: AccommodationRelocationModel = cya.employmentBenefits.flatMap(_.accommodationRelocationModel).getOrElse(AccommodationRelocationModel())

    val carVanFuelSectionFinished = carVanFuelSection.isFinished
    val accommodationRelocationSectionFinished = accommodationRelocationSection.isFinished

    val unfinishedRedirects: Seq[Call] = Seq(carVanFuelSectionFinished,accommodationRelocationSectionFinished).flatten

    unfinishedRedirects match {

      case calls if calls.isEmpty =>
        logger.info("[RedirectService][benefitsSubmitRedirect] User has completed all sections - Routing to benefits CYA page")
        Redirect(CheckYourBenefitsController.show(taxYear,employmentId))

      case calls =>

        logger.info(s"[RedirectService][benefitsSubmitRedirect] User has not yet completed all sections - Routing to next page: ${nextPage.url}")
        Redirect(nextPage)
    }
  }

  def employmentDetailsRedirect(cya: EmploymentCYAModel, taxYear: Int, employmentId: String,
                                isPriorSubmission: Boolean, isStandaloneQuestion: Boolean = true): Result = {
    Redirect(if (isPriorSubmission && isStandaloneQuestion) {
      CheckEmploymentDetailsController.show(taxYear, employmentId)
    } else {
      questionRouting(cya, taxYear, employmentId)
    })
  }

  def questionRouting(cya: EmploymentCYAModel, taxYear: Int, employmentId: String): Call = {
    cya match {
      case EmploymentCYAModel(EmploymentDetails(_,employerRef@None,_,_,_,_,_,_,_,_,_,_),_) => PayeRefController.show(taxYear,employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,startDate@None,_,_,_,_,_,_,_,_,_),_) => EmployerStartDateController.show(taxYear,employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,_,cessationDateQuestion@None,_,_,_,_,_,_,_),_) =>
        StillWorkingForEmployerController.show(taxYear, employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,_,Some(false),cessationDate@None,_,_,_,_,_,_),_) =>
        EmployerLeaveDateController.show(taxYear, employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,payrollId@None,_,_,_,_,_,_,_,_),_) =>
        EmployerPayrollIdController.show(taxYear,employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,_,_,_,_,_,_,taxablePayToDate@None,_,_),_) => EmployerPayAmountController.show(taxYear,employmentId)
      case EmploymentCYAModel(EmploymentDetails(_,_,_,_,_,_,_,_,_,_,totalTaxToDate@None,_),_) => EmploymentTaxController.show(taxYear,employmentId)
      case _ => CheckEmploymentDetailsController.show(taxYear, employmentId)
    }
  }
}