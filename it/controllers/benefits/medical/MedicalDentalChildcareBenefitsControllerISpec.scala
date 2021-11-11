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

  package controllers.benefits.medicalChildcareEducation

  import forms.YesNoForm
  import models.User
  import models.benefits.{BenefitsViewModel, MedicalChildcareEducationModel}
  import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
  import org.jsoup.Jsoup
  import org.jsoup.nodes.Document
  import play.api.http.HeaderNames
  import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
  import play.api.libs.ws.WSResponse
  import utils.{EmploymentDatabaseHelper, IntegrationTest, ViewHelpers}

  class MedicalDentalChildcareBenefitsControllerISpec extends IntegrationTest with ViewHelpers with EmploymentDatabaseHelper {

    val taxYearEOY: Int = taxYear - 1
    val employmentId: String = "001"

    private val userRequest = User(mtditid, None, nino, sessionId, affinityGroup)(fakeRequest)

    private def employmentUserData(isPrior: Boolean, employmentCyaModel: EmploymentCYAModel): EmploymentUserData =
      EmploymentUserData(sessionId, mtditid, nino, taxYearEOY, employmentId,
        isPriorSubmission = isPrior, hasPriorBenefits = isPrior, employmentCyaModel)

    def cyaModel(employerName: String, hmrc: Boolean, benefits: Option[BenefitsViewModel] = None): EmploymentCYAModel =
      EmploymentCYAModel(EmploymentDetails(employerName, currentDataIsHmrcHeld = hmrc), benefits)

    def benefits(medicalChildcareEducationModel: MedicalChildcareEducationModel): BenefitsViewModel =
      BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true, medicalChildcareEducationModel = Some(medicalChildcareEducationModel))

    private def medicalDentalChildcareQuestionPageUrl(taxYear: Int) = s"$appUrl/$taxYear/benefits/medical-dental-childcare-education-loans?employmentId=$employmentId"

    val continueLink = s"/income-through-software/return/employment-income/$taxYearEOY/benefits/medical-dental-childcare-education-loans?employmentId=$employmentId"

    object Selectors {
      val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
      val paragraphSelector: String = "#main-content > div > div > form > div > fieldset > legend > p"
      val continueButtonSelector: String = "#continue"
      val continueButtonFormSelector: String = "#main-content > div > div > form"
      val yesSelector = "#value"
      val noSelector = "#value-no"
    }

    trait SpecificExpectedResults {
      val expectedTitle: String
      val expectedHeading: String
      val expectedErrorTitle: String
      val expectedErrorText: String
    }

    trait CommonExpectedResults {
      val expectedCaption: String
      val expectedButtonText: String
      val yesText: String
      val noText: String
    }

    object ExpectedIndividualEN extends SpecificExpectedResults {
      val expectedTitle = "Did you get any medical, dental, childcare, education benefits or loans from this company?"
      val expectedHeading = "Did you get any medical, dental, childcare, education benefits or loans from this company?"
      val expectedErrorTitle = s"Error: $expectedTitle"
      val expectedErrorText = "Select yes if you got medical, dental, childcare, education benefits or loans"
    }

    object ExpectedIndividualCY extends SpecificExpectedResults {
      val expectedTitle = "Did you get any medical, dental, childcare, education benefits or loans from this company?"
      val expectedHeading = "Did you get any medical, dental, childcare, education benefits or loans from this company?"
      val expectedErrorTitle = s"Error: $expectedTitle"
      val expectedErrorText = "Select yes if you got medical, dental, childcare, education benefits or loans"
    }

    object ExpectedAgentEN extends SpecificExpectedResults {
      val expectedTitle = "Did your client get any medical, dental, childcare, education benefits or loans from this company?"
      val expectedHeading = "Did your client get any medical, dental, childcare, education benefits or loans from this company?"
      val expectedErrorTitle = s"Error: $expectedTitle"
      val expectedErrorText = "Select yes if your client got medical, dental, childcare, education benefits or loans"
    }

    object ExpectedAgentCY extends SpecificExpectedResults {
      val expectedTitle = "Did your client get any medical, dental, childcare, education benefits or loans from this company?"
      val expectedHeading = "Did your client get any medical, dental, childcare, education benefits or loans from this company?"
      val expectedErrorTitle = s"Error: $expectedTitle"
      val expectedErrorText = "Select yes if your client got medical, dental, childcare, education benefits or loans"
    }

    object CommonExpectedEN extends CommonExpectedResults {
      val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
      val expectedButtonText = "Continue"
      val yesText = "Yes"
      val noText = "No"
    }

    object CommonExpectedCY extends CommonExpectedResults {
      val expectedCaption = s"Employment for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
      val expectedButtonText = "Continue"
      val yesText = "Yes"
      val noText = "No"
    }

    val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
      Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
        UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
        UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
        UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
    }

    ".show" should {

      userScenarios.foreach { user =>
        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

          "render 'Did you get any medical, dental, childcare, education benefits or loans from this company?' " +
            "page with the correct content with no pre-filling" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              userDataStub(userData(fullEmploymentsModel()), nino, taxYearEOY)
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
                benefits = Some(benefits(fullMedicalChildcareEducationModel.copy(medicalChildcareEducationQuestion = None))))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(medicalDentalChildcareQuestionPageUrl(taxYearEOY), welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(yesText, 1, Some(false))
            radioButtonCheck(noText, 2, Some(false))
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)
          }

          "render 'Did you get any medical, dental, childcare, education benefits or loans from this company?' " +
            "page with the correct content with cya data and the yes value pre-filled" which {
            lazy val result: WSResponse = {
              dropEmploymentDB()
              insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
                Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(medicalDentalChildcareQuestionPageUrl(taxYearEOY), welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(yesText, 1, Some(true))
            radioButtonCheck(noText, 2, Some(false))
            buttonCheck(expectedButtonText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
            welshToggleCheck(user.isWelsh)
          }
        }
      }

      "redirect to another page when the request is valid but they aren't allowed to view the page and" should {

        val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN))

        "redirect the user to the check employment benefits page when theres no benefits and prior submission" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(medicalDentalChildcareQuestionPageUrl(taxYearEOY), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")

          }
        }

        "redirect the user to the benefits received page when theres no benefits and not prior submission" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(medicalDentalChildcareQuestionPageUrl(taxYearEOY), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/benefits/company-benefits?employmentId=$employmentId")

          }
        }

        "redirect the user to the check employment benefits page when theres no session data for that user" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(medicalDentalChildcareQuestionPageUrl(taxYearEOY), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")

          }
        }

        "redirect the user to the overview page when the request is in year" which {
          lazy val result: WSResponse = {
            dropEmploymentDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              benefits = Some(BenefitsViewModel(isUsingCustomerData = true, isBenefitsReceived = true)))), userRequest)
            urlGet(medicalDentalChildcareQuestionPageUrl(taxYear), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")

          }
        }

        "redirect to the check employment benefits page when theres no CYA data" which {

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(medicalDentalChildcareQuestionPageUrl(taxYearEOY), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the check your details page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
          }

          "doesn't create any benefits data" in {
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyamodel.employment.employmentBenefits shouldBe None
          }
        }

      }

    }


    ".submit" should {

      userScenarios.foreach { user =>
        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

          s"return a BAD_REQUEST($BAD_REQUEST) status" when {

            "the value is empty" which {
              lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

              lazy val result: WSResponse = {
                dropEmploymentDB()
                insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
                  Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
                authoriseAgentOrIndividual(user.isAgent)
                urlPost(medicalDentalChildcareQuestionPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
                  headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
              }

              "has the correct status" in {
                result.status shouldBe BAD_REQUEST
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              import Selectors._
              import user.commonExpectedResults._

              titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
              h1Check(user.specificExpectedResults.get.expectedHeading)
              textOnPageCheck(expectedCaption, captionSelector)
              radioButtonCheck(yesText, 1, Some(false))
              radioButtonCheck(noText, 2, Some(false))
              buttonCheck(expectedButtonText, continueButtonSelector)
              formPostLinkCheck(continueLink, continueButtonFormSelector)
              welshToggleCheck(user.isWelsh)

              errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, Selectors.yesSelector)
              errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText, Some("value"))
            }
          }
        }
      }

      "redirect to another page when a valid request is made and then" should {

        val user = UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedAgentEN))

        "redirect to check your benefits, update the medicalChildcareEducationQuestion to no and wipe the medical, child care and" +
          " education data when the user chooses no" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel)))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(medicalDentalChildcareQuestionPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the check your details page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get

            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalChildcareEducationQuestion)) shouldBe Some(false)
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsuranceQuestion)) shouldBe None
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance)) shouldBe None
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlacesQuestion)) shouldBe None
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlaces)) shouldBe None
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.educationalServicesQuestion)) shouldBe None
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.educationalServices)) shouldBe None
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoanQuestion)) shouldBe None
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoan)) shouldBe None
          }

        }

        "redirect to check your benefits and update the medicalChildcareEducationQuestion to yes when the user chooses yes" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = false, cyaModel("employerName", hmrc = true,
              Some(benefits(fullMedicalChildcareEducationModel.copy(medicalChildcareEducationQuestion = Some(false)))))), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(medicalDentalChildcareQuestionPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the check your details page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalChildcareEducationQuestion)) shouldBe Some(true)
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsuranceQuestion)) shouldBe Some(true)
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.medicalInsurance)) shouldBe Some(100)
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlacesQuestion)) shouldBe Some(true)
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.nurseryPlaces)) shouldBe Some(200)
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.educationalServicesQuestion)) shouldBe Some(true)
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.educationalServices)) shouldBe Some(300)
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoanQuestion)) shouldBe Some(true)
            cyamodel.employment.employmentBenefits.flatMap(_.medicalChildcareEducationModel.flatMap(_.beneficialLoan)) shouldBe Some(400)
          }

        }

        "redirect the user to the overview page when it is in year" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(medicalDentalChildcareQuestionPageUrl(taxYear), body = "", user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(s"http://localhost:11111/income-through-software/return/$taxYear/view")
          }
        }

        "redirect to the check employment benefits page when theres no CYA data" which {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

          lazy val result: WSResponse = {
            dropEmploymentDB()
            insertCyaData(employmentUserData(isPrior = true, cyaModel("employerName", hmrc = true)), userRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(medicalDentalChildcareQuestionPageUrl(taxYearEOY), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "redirects to the check your details page" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe
              Some(s"/income-through-software/return/employment-income/$taxYearEOY/check-employment-benefits?employmentId=$employmentId")
          }

          "doesn't create any benefits data" in {
            lazy val cyamodel = findCyaData(taxYearEOY, employmentId, userRequest).get
            cyamodel.employment.employmentBenefits shouldBe None
          }
        }


      }
    }

  }