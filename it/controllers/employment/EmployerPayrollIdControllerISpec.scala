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

package controllers.employment

import models.AuthorisationRequest
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import utils.PageUrls.{checkYourDetailsUrl, fullUrl, overviewUrl, payrollIdUrl}
import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

class EmployerPayrollIdControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

  private val employmentId = "001"

  private implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private val userRequest: AuthorisationRequest[_] = anAuthorisationRequest

  object Selectors {
    val paragraph1Selector = "p.govuk-body:nth-child(2)"
    val paragraph2Selector = "p.govuk-body:nth-child(3)"
    val paragraph3Selector = "p.govuk-body:nth-child(4)"
    val paragraph4Selector = "p.govuk-body:nth-child(5)"
    val hintTextSelector = "#payrollId-hint"
    val inputSelector = "#payrollId"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#payrollId"

    def bulletSelector(bulletNumber: Int): String =
      s"#main-content > div > div > ul > li:nth-child($bulletNumber)"
  }

  val inputName: String = "payrollId"

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val emptyErrorText: String
    val wrongFormatErrorText: String
    val tooLongErrorText: String
    val paragraph1: String
    val paragraph2: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val continueButtonText: String
    val hintText: String
    val bullet1: String
    val bullet2: String
    val bullet3: String
    val previousParagraph: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Employment details for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    val continueButtonText = "Continue"
    val hintText = "For example, 123456"
    val bullet1: String = "upper and lower case letters (a to z)"
    val bullet2: String = "numbers"
    val bullet3: String = "the special characters: .,-()/=!\"%&*;<>'+:\\?"
    val previousParagraph: String = "If the payroll ID is not 123456, tell us the correct ID."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Manylion cyflogaeth ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"
    val continueButtonText = "Yn eich blaen"
    val hintText = "Er enghraifft, 123456"
    val bullet1: String = "llythrennau mawr a bach (a i z)"
    val bullet2: String = "rhifau"
    val bullet3: String = "y cymeriadau arbennig: .,-()/=!\"%&*;<>'+:\\?"
    val previousParagraph: String = "Os nad 123456 ywír ID cyflogres, rhowch wybod i ni beth ywír ID cywir."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your payroll ID for this employment?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "What’s your payroll ID for this employment?"
    val emptyErrorText: String = "Enter your payroll ID"
    val wrongFormatErrorText: String = "Enter your payroll ID in the correct format"
    val tooLongErrorText: String = "Your payroll ID must be 38 characters or fewer"
    val paragraph1: String = "Your payroll ID must be 38 characters or fewer. It can include:"
    val paragraph2: String = "You can find this on your payslip or on your P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your client’s payroll ID for this employment?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "What’s your client’s payroll ID for this employment?"
    val emptyErrorText: String = "Enter your client’s payroll ID"
    val wrongFormatErrorText: String = "Enter your client’s payroll ID in the correct format"
    val tooLongErrorText: String = "Your client’s payroll ID must be 38 characters or fewer"
    val paragraph1: String = "Your client’s payroll ID must be 38 characters or fewer. It can include:"
    val paragraph2: String = "You can find this on your client’s payslip or on their P60. It’s also known as a ‘payroll number’."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Beth ywích ID cyflogres am y gyflogaeth hon?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedH1: String = "Beth ywích ID cyflogres am y gyflogaeth hon?"
    val emptyErrorText: String = "Nodwch eich ID cyflogres"
    val wrongFormatErrorText: String = "Nodwch eich ID cyflogres yn y fformat cywir"
    val tooLongErrorText: String = "Maeín rhaid iích ID cyflogres fod yn 38 o gymeriadau neu lai"
    val paragraph1: String = "Maeín rhaid iích ID cyflogres fod yn 38 o gymeriadau neu lai. Gall gynnwys y canlynol:"
    val paragraph2: String = "Mae hwn iíw weld ar eich slip cyflog neuích P60. Mae hefyd yn cael ei alwín ërhif cyflogresí."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Beth yw ID cyflogres eich cleient ar gyfer y gyflogaeth hon?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedH1: String = "Beth yw ID cyflogres eich cleient ar gyfer y gyflogaeth hon?"
    val emptyErrorText: String = "Nodwch ID cyflogres eich cleient"
    val wrongFormatErrorText: String = "Nodwch ID cyflogres eich cleient yn y fformat cywir"
    val tooLongErrorText: String = "Maeín rhaid i ID cyflogres eich cleient fod yn 38 o gymeriadau neu lai"
    val paragraph1: String = "Maeín rhaid i ID cyflogres eich cleient fod yn 38 o gymeriadau neu lai. Gall gynnwys y canlynol:"
    val paragraph2: String = "Mae hwn iíw weld ar slip cyflog eich cleient neu ar ei P60. Mae hefyd yn cael ei alwín ërhif cyflogresí."
  }

  def cya(isPriorSubmission: Boolean = true): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission, hasPriorBenefits = isPriorSubmission, hasPriorStudentLoans = isPriorSubmission,
      EmploymentCYAModel(
        EmploymentDetails("maggie", currentDataIsHmrcHeld = false),
        None
      )
    )

  def cyaWithPayrollId(isPriorSubmission: Boolean = true): EmploymentUserData =
    EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission, hasPriorBenefits = isPriorSubmission, hasPriorStudentLoans = isPriorSubmission,
      EmploymentCYAModel(
        EmploymentDetails("maggie", payrollId = Some("123456"), currentDataIsHmrcHeld = false),
        None
      )
    )

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
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render the What's your payrollId? page with the correct content when theres no payrollId in cya" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(cya(false))
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.paragraph1, paragraph1Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph3Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(payrollIdUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "should render the What's your payrollId? page with the id pre-filled when theres payrollId data in cya" which {
          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(cyaWithPayrollId())
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlGet(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(get.expectedTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(previousParagraph, paragraph1Selector)
          textOnPageCheck(get.paragraph1, paragraph2Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph4Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "123456")
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(payrollIdUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)
        }

        "redirect to check employment details page when there is no cya data in session" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            urlGet(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
          }
        }

        "redirect to overview page if the user tries to hit this page with current taxYear" when {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            insertCyaData(cya())
            urlGet(fullUrl(payrollIdUrl(taxYear, employmentId)), welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(overviewUrl(taxYear)) shouldBe true
          }
        }
      }
    }
  }

  ".submit" when {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      import user.specificExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "should render the What's your payrollId? page with an error when the payrollId is input as empty" which {
          val payrollId = ""
          val body = Map("payrollId" -> payrollId)

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(cya())
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlPost(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), body, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has a BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.paragraph1, paragraph2Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph4Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, payrollId)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(payrollIdUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(get.emptyErrorText)
        }

        "should render the What's your payrollId? page with an error when the payrollId is input as too long" which {
          val payrollId = "123456789012345678901234567890123456789"
          val body = Map("payrollId" -> payrollId)

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(cya())
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlPost(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), body, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has a BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.paragraph1, paragraph2Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph4Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, payrollId)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(payrollIdUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.tooLongErrorText, expectedErrorHref)
          errorAboveElementCheck(get.tooLongErrorText)
        }

        "should render the What's your payrollId? page with an error when the payrollId is input as the wrong format" which {
          val payrollId = "$11223"
          val body = Map("payrollId" -> payrollId)

          implicit lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(cya())
            userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
            urlPost(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), body, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          lazy val document = Jsoup.parse(result.body)

          implicit def documentSupplier: () => Document = () => document

          "has a BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(get.expectedErrorTitle, user.isWelsh)
          h1Check(get.expectedH1)
          captionCheck(expectedCaption)
          textOnPageCheck(get.paragraph1, paragraph2Selector)
          textOnPageCheck(bullet1, bulletSelector(1))
          textOnPageCheck(bullet2, bulletSelector(2))
          textOnPageCheck(bullet3, bulletSelector(3))
          textOnPageCheck(get.paragraph2, paragraph4Selector)
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, payrollId)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(payrollIdUrl(taxYearEOY, employmentId), continueButtonFormSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(get.wrongFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(get.wrongFormatErrorText)
        }

        "should update the payrollId when a valid payrollId is submitted and redirect to the check your details controller" when {
          val payrollId = "123456"
          val body = Map("payrollId" -> payrollId)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropEmploymentDB()
            val data = EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId, isPriorSubmission = true, hasPriorBenefits = true, hasPriorStudentLoans = true, anEmploymentCYAModel)
            insertCyaData(data)
            urlPost(fullUrl(payrollIdUrl(taxYearEOY, employmentId)), body, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "status SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirect to the Check Employment Details page" in {

            result.header(HeaderNames.LOCATION).contains(checkYourDetailsUrl(taxYearEOY, employmentId)) shouldBe true
          }

          s"update the cya models payroll id to be $payrollId" in {
            lazy val cyaModel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyaModel.employment.employmentDetails.payrollId shouldBe Some(payrollId)
          }
        }
      }
    }
  }
}
