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

package controllers.benefits.fuel

import forms.YesNoForm
import models.benefits.{BenefitsViewModel, CarVanFuelModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserDataWithBenefits
import utils.PageUrls._
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class ReceivedOwnCarMileageBenefitControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "employmentId"
  private val mileageAmount: Option[BigDecimal] = Some(BigDecimal(4.9))

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val continueButtonFormSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val p1Selector = "#main-content > div > div > p:nth-child(2)"
    val p2Selector = "#main-content > div > div > p:nth-child(3)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedP1: String
    val expectedP2: String
    val expectedErrorTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Employment benefits for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get a mileage benefit for using your own car for work?"
    val expectedH1 = "Did you get a mileage benefit for using your own car for work?"
    val expectedP1 = "We only need to know about payments made above our ‘approved amount’. If you have payments above the ‘approved amount’, they should be recorded in section E of your P11D form."
    val expectedP2 = "Check with your employer if you are unsure."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got a mileage benefit for using your own car for work"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a mileage benefit for using their own car for work?"
    val expectedH1 = "Did your client get a mileage benefit for using their own car for work?"
    val expectedP1: String = "We only need to know about payments made above our ‘approved amount’. " +
      "If your client has payments above the ‘approved amount’, they should be recorded in section E of their P11D form."
    val expectedP2 = "Check with your client’s employer if you are unsure."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got a mileage benefit for using their own car for work"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch fuddiant milltiroedd am ddefnyddio eich car eich hun ar gyfer gwaith?"
    val expectedH1 = "A gawsoch fuddiant milltiroedd am ddefnyddio eich car eich hun ar gyfer gwaith?"
    val expectedP1: String = "Dim ond taliadau syín uwch naín ëswm cymeradwyí y mae angen i ni wybod amdanynt. Os oes gennych daliadau " +
      "syín uwch naír ëswm cymeradwyí, dylid eu cofnodi yn adran E oích ffurflen P11D."
    val expectedP2 = "Gwiriwch ‚ích cyflogwr os nad ydych yn si?r."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ëIawní os cawsoch fuddiant milltiroedd am ddefnyddioích car eich hun ar gyfer gwaith"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient fuddiant milltiroedd am ddefnyddio ei gar ei hun ar gyfer gwaith?"
    val expectedH1 = "A gafodd eich cleient fuddiant milltiroedd am ddefnyddio ei gar ei hun ar gyfer gwaith?"
    val expectedP1: String = "Dim ond taliadau syín uwch naín ëswm cymeradwyí y mae angen i ni wybod amdanynt. Os oes gan eich " +
      "cleient daliadau syín uwch naír ëswm cymeradwyí, dylid eu cofnodi yn adran E oíi ffurflen P11D."
    val expectedP2 = "Gwiriwch ‚ chyflogwr eich cleient os nad ydych yn si?r."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ëIawní os cafodd eich cleient fuddiant milltiroedd am ddefnyddio ei gar ei hun ar gyfer gwaith"
  }

  private val someAmount: Option[BigDecimal] = Some(123.45)

  private val allSectionsFinishedCarVanFuelModel: CarVanFuelModel = CarVanFuelModel(sectionQuestion = Some(true), carQuestion = Some(true), car = someAmount,
    carFuelQuestion = Some(true), carFuel = someAmount, vanQuestion = Some(true), van = someAmount, vanFuelQuestion = Some(true),
    vanFuel = someAmount, mileageQuestion = Some(true), mileage = someAmount)

  private val benefitsWithEmptyMileage = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(mileageQuestion = None, mileage = None)), isUsingCustomerData = true)

  private def benefitsWithMileageYes(mileageAmount: Option[BigDecimal] = mileageAmount) = BenefitsViewModel(isBenefitsReceived = true,
    carVanFuelModel = Some(allSectionsFinishedCarVanFuelModel.copy(mileageQuestion = Some(true), mileage = mileageAmount)), isUsingCustomerData = true)

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

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the 'Did you get a mileage benefit?' page with correct content and no radio buttons selected when no cya data" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyMileage, isPriorSubmission = false, hasPriorBenefits = false))
            urlGet(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(mileageBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get a mileage benefit?' page with correct content and yes button selected when there cya data for the question set as true" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithMileageYes(), isPriorSubmission = false, hasPriorBenefits = false))
            urlGet(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(mileageBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get a mileage benefit?' page with correct content and yes button selected when the user has previously chosen yes" +
          " but has did not enter a mileage amount yet" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            val benefitsViewModel = benefitsWithMileageYes(mileageAmount = None)
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsViewModel, isPriorSubmission = false, hasPriorBenefits = false))
            urlGet(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(mileageBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get a mileage benefit?' page with correct content and no button selected when there cya data for the question set as false" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithMileageNo, isPriorSubmission = false, hasPriorBenefits = false))
            urlGet(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP2, p2Selector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(mileageBenefitsUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "redirect to overview page if the user tries to hit this page with current taxYear" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithMileageNo, isPriorSubmission = false, hasPriorBenefits = false))
        val inYearUrl = s"$appUrl/$taxYear/how-much-pay?employmentId=$employmentId"
        urlGet(inYearUrl, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    // common redirect tests for show
    redirectTests(user = user)
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "display an error when no radio button is selected" which {
          lazy val form: Map[String, String] = Map[String, String]()
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyMileage, isPriorSubmission = false, hasPriorBenefits = false))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          s"has a BAD_REQUEST ($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))

          welshToggleCheck(user.isWelsh)
        }
      }
    }

    val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

    "Update the mileageQuestion to no and wipe out the mileage amount when the user chooses no, redirects to accommodation page when prior benefits exist" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropEmploymentDB()
        insertCyaData(anEmploymentUserDataWithBenefits(benefitsWithEmptyMileage))
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
        authoriseAgentOrIndividual(user.isAgent)
        urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
    redirectTests(isSubmitTest = true, user = user)
  }

  // scalastyle:off method.length
  def redirectTests(isSubmitTest: Boolean = false, user: UserScenario[_, _]): Unit = {
    val getOrPost = if (isSubmitTest) {
      "post"
    } else {
      "get"
    }

    lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

    def url: WSResponse = if (isSubmitTest) {
      urlGet(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
    } else {
      urlPost(fullUrl(mileageBenefitsUrl(taxYearEOY, employmentId)), follow = false,
        welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = form)
    }

    s"redirect to accommodation relocation page when benefits has carVanFuelQuestion set to false " +
      s"for a $getOrPost and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
      s" and language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(user.isAgent)
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
