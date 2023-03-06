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

package controllers.benefits.fuel

import forms.YesNoForm
import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithBenefits
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ReceivedOwnCarMileageBenefitControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"

  private val someAmount: Option[BigDecimal] = Some(123.45)

  private val allSectionsFinishedCarVanFuelModel: CarVanFuelModel = CarVanFuelModel(sectionQuestion = Some(true), carQuestion = Some(true), car = someAmount,
    carFuelQuestion = Some(true), carFuel = someAmount, vanQuestion = Some(true), van = someAmount, vanFuelQuestion = Some(true),
    vanFuel = someAmount, mileageQuestion = Some(true), mileage = someAmount)

  private val benefitsWithEmptyMileage = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(mileageQuestion = None, mileage = None)), isUsingCustomerData = true)

  // models for Incomplete sections redirect tests
  private val benefitsWithNoBenefitsReceived = BenefitsViewModel(isUsingCustomerData = true)

  private def benefitsWithNoCarVanFuelQuestion(carVanFuelQuestion: Option[Boolean] = Some(false)) = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(sectionQuestion = carVanFuelQuestion)),
    isUsingCustomerData = true)

  private val benefitsWithEmptyCarQuestion = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(carQuestion = None)),
    isUsingCustomerData = true)

  private val benefitsWithEmptyVanQuestion = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(vanQuestion = None)),
    isUsingCustomerData = true)

  private val benefitsWithEmptyCarFuelQuestion = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(carFuelQuestion = None)),
    isUsingCustomerData = true)

  private val benefitsCarQuestionYesNoAmount = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(car = None)), isUsingCustomerData = true)

  private val benefitsCarFuelQuestionYesNoAmount = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(carFuel = None)), isUsingCustomerData = true)

  private val benefitsVanFuelQuestionYesNoAmount = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(vanFuel = None)), isUsingCustomerData = true)

  private val benefitsWithMileageNo = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(mileageQuestion = Some(false), mileage = None)), isUsingCustomerData = true)

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" when {
    "render the 'Did you get a mileage benefit?' page with correct content and no radio buttons selected when no cya data" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyMileage, isPriorSubmission = false, hasPriorBenefits = false))
        urlGet(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect to overview page if the user tries to hit this page with current taxYear" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithMileageNo, isPriorSubmission = false, hasPriorBenefits = false))
        val inYearUrl = s"$appUrl/$taxYear/details/how-much-pay?employmentId=$employmentId"
        urlGet(inYearUrl, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    // common redirect tests for show
    redirectTests()
  }

  ".submit" should {
    "display an error when no radio button is selected" which {
      lazy val form: Map[String, String] = Map[String, String]()
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyMileage, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "Update the mileageQuestion to no and wipe out the mileage amount when the user chooses no, redirects to accommodation page when prior benefits exist" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyMileage))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check employment benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update the mileageQuestion to false and mileage to none" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe None
      }
    }

    "Update the mileageQuestion to no and wipe out the mileage amount when the user chooses no, redirects to" +
      "accommodation relocation when no prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        val mileage = benefitsWithEmptyMileage
        insertCyaData(anEmploymentUserDataWithBenefits(mileage, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the check employment benefits page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update the mileageQuestion to false and mileage to none" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(false)
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileage)) shouldBe None
      }
    }

    "Update the mileageQuestion to yes when the user chooses yes, redirects to mileage amount page when prior benefits exist" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyMileage))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the mileage amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(mileageBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update the mileageQuestion to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
      }
    }

    "Update the mileageQuestion to yes when the user chooses yes, redirects to mileage amount page when no prior benefits" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyMileage, isPriorSubmission = false, hasPriorBenefits = false))
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "redirects to the mileage amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(mileageBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }

      "update the mileageQuestion to true" in {
        lazy val cyaModel = findCyaData(taxYearEOY, employmentId, anAuthorisationRequest).get
        cyaModel.employment.employmentBenefits.flatMap(_.carVanFuelModel.flatMap(_.mileageQuestion)) shouldBe Some(true)
      }
    }

    // common redirect tests for submit
    redirectTests(isSubmitTest = true)
  }

  // scalastyle:off method.length
  def redirectTests(isSubmitTest: Boolean = false): Unit = {
    val getOrPost = if (isSubmitTest) {
      "post"
    } else {
      "get"
    }

    lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

    def url: WSResponse = if (isSubmitTest) {
      urlGet(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
    } else {
      urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)),
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
    }

    s"redirect to accommodation relocation page when benefits has carVanFuelQuestion set to false " +
      s"for a $getOrPost and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoCarVanFuelQuestion(), isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(accommodationRelocationBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to check employment benefits page when benefits has carVanFuelQuestion is empty for a $getOrPost " +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        val benefitsViewModel = benefitsWithNoCarVanFuelQuestion(None)
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carVanFuelBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to check employment benefits page when benefits has carQuestion is empty for a $getOrPost" +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyCarQuestion, isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to check employment benefits page when benefits has carFuelQuestion is empty for a $getOrPost" +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyCarFuelQuestion, isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carFuelBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to check employment benefits page when benefits has vanQuestion is empty for a $getOrPost" +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyVanQuestion, isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vanBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to car amount page when benefits has carQuestion set to true but no amount for a $getOrPost" +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsCarQuestionYesNoAmount, isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to car amount page when benefits has carFuelQuestion set to true but no amount for a $getOrPost" +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsCarFuelQuestionYesNoAmount, isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(carFuelBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to van fuel amount page when benefits has VanQuestion set to true but no amount for a $getOrPost" +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsVanFuelQuestionYesNoAmount, isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vanFuelBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to van fuel amount page when benefits has VanFuelQuestion set to true but no amount for a $getOrPost" +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsVanFuelQuestionYesNoAmount, isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(vanFuelBenefitsAmountUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to check employment benefits page when benefits has benefitsReceived set to false for a $getOrPost" +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithNoBenefitsReceived, isPriorSubmission = false, hasPriorBenefits = false))
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }

    s"redirect to check employment benefits page when there is no cya data in session for a $getOrPost" +
      s" and language is ${welshTest(isWelsh = false)} and request is from an ${agentTest(isAgent = false)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropEmploymentDB()
        url
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkYourBenefitsUrl(taxYearEOY, employmentId)) shouldBe true
      }
    }
  }
  // scalastyle:on method.length
}
