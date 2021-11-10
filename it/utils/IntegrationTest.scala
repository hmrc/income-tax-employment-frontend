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

package utils

import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.SessionValues
import config.AppConfig
import controllers.predicates.AuthorisedAction
import helpers.{PlaySessionCookieBaker, WireMockHelper, WiremockStubHelpers}
import models.IncomeTaxUserData
import models.benefits.{AccommodationRelocationModel, Benefits, CarVanFuelModel, MedicalChildcareEducationModel, TravelEntertainmentModel, UtilitiesAndServicesModel}
import models.employment._
import models.mongo.{EmploymentCYAModel, EmploymentDetails, EmploymentUserData}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.NO_CONTENT
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import play.api.{Application, Environment, Mode}
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import views.html.authErrorPages.AgentAuthErrorPageView

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait IntegrationTest extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with WireMockHelper
  with WiremockStubHelpers with BeforeAndAfterAll {

  val nino = "AA123456A"
  val mtditid = "1234567890"
  val sessionId = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"
  val affinityGroup = "affinityGroup"
  val taxYear = 2022

  val xSessionId: (String, String) = "X-Session-ID" -> sessionId

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid)

  implicit val actorSystem: ActorSystem = ActorSystem()

  implicit val integrationTestClock = IntegrationTestClock

  implicit def wsClient: WSClient = app.injector.instanceOf[WSClient]

  lazy val appUrl = s"http://localhost:$port/income-through-software/return/employment-income"

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  def config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort.toString,
    "microservice.services.income-tax-employment.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-expenses.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-submission.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.view-and-change.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.sign-in.url" -> s"/auth-login-stub/gg-sign-in",
    "taxYearErrorFeatureSwitch" -> "false",
    "useEncryption" -> "true"
  )

  def configWithInvalidEncryptionKey: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort.toString,
    "microservice.services.income-tax-employment.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-expenses.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-submission.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.view-and-change.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.sign-in.url" -> s"/auth-login-stub/gg-sign-in",
    "taxYearErrorFeatureSwitch" -> "false",
    "useEncryption" -> "true",
    "mongodb.encryption.key" -> "key"
  )

  def externalConfig: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission.url" -> s"http://127.0.0.1:$wiremockPort",
    "metrics.enabled" -> "false"
  )

  lazy val agentAuthErrorPage: AgentAuthErrorPageView = app.injector.instanceOf[AgentAuthErrorPageView]

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  lazy val appWithFakeExternalCall: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(externalConfig)
    .build

  lazy val appWithInvalidEncryptionKey: Application = GuiceApplicationBuilder()
    .configure(configWithInvalidEncryptionKey)
    .build()

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]


  val defaultAcceptedConfidenceLevels = Seq(
    ConfidenceLevel.L200,
    ConfidenceLevel.L500
  )

  def authService(stubbedRetrieval: Future[_], acceptedConfidenceLevel: Seq[ConfidenceLevel]): AuthService = new AuthService(
    new MockAuthConnector(stubbedRetrieval, acceptedConfidenceLevel)
  )

  def authAction(
                  stubbedRetrieval: Future[_],
                  acceptedConfidenceLevel: Seq[ConfidenceLevel] = Seq.empty[ConfidenceLevel]
                ): AuthorisedAction = new AuthorisedAction(
    appConfig
  )(
    authService(stubbedRetrieval, if (acceptedConfidenceLevel.nonEmpty) {
      acceptedConfidenceLevel
    } else {
      defaultAcceptedConfidenceLevels
    }),
    mcc
  )

  def successfulRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L200
  )

  def insufficientConfidenceRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L50
  )

  def incorrectCredsRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L200
  )

  def playSessionCookies(taxYear: Int, extraData: Map[String, String] = Map.empty): String = PlaySessionCookieBaker.bakeSessionCookie(Map(
    SessionValues.TAX_YEAR -> taxYear.toString,
    SessionKeys.sessionId -> sessionId,
    SessionValues.CLIENT_NINO -> nino,
    SessionValues.CLIENT_MTDITID -> mtditid
  ) ++ extraData)


  def userData(allData: AllEmploymentData): IncomeTaxUserData = IncomeTaxUserData(Some(allData))

  def userDataStub(userData: IncomeTaxUserData, nino: String, taxYear: Int): StubMapping = {

    stubGetWithHeadersCheck(
      s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", OK,
      Json.toJson(userData).toString(), ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))
  }

  def userDataStubDeleteOrIgnoreEmployment(userData: IncomeTaxUserData, nino: String, taxYear: Int, employmentId: String, sourceType: String): StubMapping = {

    stubDeleteWithHeadersCheck(
      s"/income-tax-employment/income-tax/nino/$nino/sources/$employmentId/$sourceType\\?taxYear=$taxYear", NO_CONTENT,
      Json.toJson(userData).toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)
  }

  def noUserDataStub(nino: String, taxYear: Int): StubMapping = {

    stubGetWithHeadersCheck(
      s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT,
      "{}", ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))
  }

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  def fullEmploymentsModel(hmrcEmployment: Seq[EmploymentSource] = Seq(employmentDetailsAndBenefits()),
                           hmrcExpenses: Option[EmploymentExpenses] = Some(employmentExpenses),
                           customerEmployment: Seq[EmploymentSource] = Seq(),
                           customerExpenses: Option[EmploymentExpenses] = None): AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = hmrcEmployment,
    hmrcExpenses = hmrcExpenses,
    customerEmploymentData = customerEmployment,
    customerExpenses = customerExpenses)

  def employmentDetailsAndBenefits(benefits: Option[EmploymentBenefits] = None,
                                   employmentId: String = "001",
                                   employerName: String = "maggie",
                                   employerRef: Option[String] = Some("223/AB12399"),
                                   startDate: Option[String] = Some("2019-04-21"),
                                   dateIgnored: Option[String] = None,
                                   submittedOn: Option[String] = Some("2020-01-04T05:01:01Z"),
                                   taxablePayToDate: Option[BigDecimal] = Some(34234.15),
                                   totalTaxToDate: Option[BigDecimal] = Some(6782.92)
                                  ): EmploymentSource = {
    EmploymentSource(
      employmentId = employmentId,
      employerName = employerName,
      employerRef = employerRef,
      payrollId = Some("12345678"),
      startDate = startDate,
      cessationDate = Some("2020-03-11"),
      dateIgnored = dateIgnored,
      submittedOn = submittedOn,
      employmentData = Some(EmploymentData(
        submittedOn = "2020-02-12",
        employmentSequenceNumber = Some("123456789999"),
        companyDirector = Some(true),
        closeCompany = Some(false),
        directorshipCeasedDate = Some("2020-02-12"),
        occPen = Some(false),
        disguisedRemuneration = Some(false),
        pay = Some(Pay(taxablePayToDate, totalTaxToDate, Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
        Some(Deductions(
          studentLoans = Some(StudentLoans(
            uglDeductionAmount = Some(100.00),
            pglDeductionAmount = Some(100.00)
          ))
        ))
      )),
      employmentBenefits = benefits
    )
  }


  lazy val expenses: Expenses = Expenses(
    Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)
  )
  lazy val employmentExpenses: EmploymentExpenses = EmploymentExpenses(
    submittedOn = None,
    dateIgnored = None,
    totalExpenses = None,
    expenses = Some(expenses)
  )

  lazy val filteredBenefits: Some[EmploymentBenefits] = Some(EmploymentBenefits(
    submittedOn = "2020-02-12",
    benefits = Some(Benefits(
      van = Some(3.00),
      vanFuel = Some(4.00),
      mileage = Some(5.00),
    ))
  )
  )

  lazy val fullBenefits: Some[EmploymentBenefits] = Some(EmploymentBenefits(
    submittedOn = "2020-02-12",
    benefits = Some(Benefits(
      car = Some(1.23),
      carFuel = Some(2.00),
      van = Some(3.00),
      vanFuel = Some(4.00),
      mileage = Some(5.00),
      accommodation = Some(6.00),
      qualifyingRelocationExpenses = Some(7.00),
      nonQualifyingRelocationExpenses = Some(8.00),
      travelAndSubsistence = Some(9.00),
      personalIncidentalExpenses = Some(10.00),
      entertaining = Some(11.00),
      telephone = Some(12.00),
      employerProvidedServices = Some(13.00),
      employerProvidedProfessionalSubscriptions = Some(14.00),
      service = Some(15.00),
      medicalInsurance = Some(16.00),
      nurseryPlaces = Some(17.00),
      beneficialLoan = Some(18.00),
      educationalServices = Some(19.00),
      incomeTaxPaidByDirector = Some(20.00),
      paymentsOnEmployeesBehalf = Some(21.00),
      expenses = Some(22.00),
      taxableExpenses = Some(23.00),
      vouchersAndCreditCards = Some(24.00),
      nonCash = Some(25.00),
      otherItems = Some(26.00),
      assets = Some(27.00),
      assetTransfer = Some(280000.00)
    )
    )
  ))

  def employmentUserData: EmploymentUserData = EmploymentUserData(
    sessionId,
    mtditid,
    nino,
    2021,
    "employmentId",
    isPriorSubmission = true,
    hasPriorBenefits =  true,
    EmploymentCYAModel(
      EmploymentDetails("Employer Name", currentDataIsHmrcHeld = true),
      None
    )
  )

  def fullCarVanFuelModel: CarVanFuelModel =
    CarVanFuelModel(
      carVanFuelQuestion = Some(true),
      carQuestion = Some(true),
      car = Some(100),
      carFuelQuestion = Some(true),
      carFuel = Some(200),
      vanQuestion = Some(true),
      van = Some(300),
      vanFuelQuestion = Some(true),
      vanFuel = Some(400),
      mileageQuestion = Some(true),
      mileage = Some(400)
    )

  def emptyCarVanFuelModel: CarVanFuelModel =
    CarVanFuelModel(
      carVanFuelQuestion = Some(false)
    )

  def fullAccommodationRelocationModel: AccommodationRelocationModel =
    AccommodationRelocationModel(
      accommodationRelocationQuestion = Some(true),
      accommodationQuestion = Some(true),
      accommodation = Some(100.00),
      qualifyingRelocationExpensesQuestion = Some(true),
      qualifyingRelocationExpenses = Some(200.00),
      nonQualifyingRelocationExpensesQuestion = Some(true),
      nonQualifyingRelocationExpenses = Some(300.00)
    )

  def emptyAccommodationRelocationModel: AccommodationRelocationModel =
    AccommodationRelocationModel(
      accommodationRelocationQuestion = Some(false)
    )

  def fullTravelOrEntertainmentModel: TravelEntertainmentModel =
    TravelEntertainmentModel(
      travelEntertainmentQuestion = Some(true),
      travelAndSubsistenceQuestion = Some(true),
      travelAndSubsistence = Some(100.00),
      personalIncidentalExpensesQuestion = Some(true),
      personalIncidentalExpenses = Some(200.00),
      entertainingQuestion = Some(true),
      entertaining = Some(300.00),
    )

  def emptyTravelOrEntertainmentModel: TravelEntertainmentModel =
    TravelEntertainmentModel(
      travelEntertainmentQuestion = Some(false)
    )

  def fullUtilitiesAndServicesModel: UtilitiesAndServicesModel =
    UtilitiesAndServicesModel(
      utilitiesAndServicesQuestion = Some(true),
      telephoneQuestion = Some(true),
      telephone = Some(100.00),
      employerProvidedServicesQuestion = Some(true),
      employerProvidedServices = Some(200.00),
      employerProvidedProfessionalSubscriptionsQuestion = Some(true),
      employerProvidedProfessionalSubscriptions = Some(300.00),
      serviceQuestion = Some(true),
      service = Some(400.00)
    )

  def emptyUtilitiesAndServicesModel: UtilitiesAndServicesModel =
    UtilitiesAndServicesModel(utilitiesAndServicesQuestion = Some(false))

  def fullMedicalChildcareEducationModel: MedicalChildcareEducationModel =
    MedicalChildcareEducationModel(
      medicalChildcareEducationQuestion = Some(true),
      medicalInsuranceQuestion = Some(true),
      medicalInsurance = Some(100.00),
      nurseryPlacesQuestion = Some(true),
      nurseryPlaces = Some(200.00),
      educationalServicesQuestion = Some(true),
      educationalServices = Some(300.00),
      beneficialLoanQuestion = Some(true),
      beneficialLoan = Some(400.00),
    )

  def emptyMedicalChildcareEducationModel: MedicalChildcareEducationModel =
    MedicalChildcareEducationModel(medicalChildcareEducationQuestion = Some(false))
}
